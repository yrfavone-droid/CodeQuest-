const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const PORT = Number(process.env.PORT || 3000);
const ROOT_DIR = path.resolve(__dirname, '..');
const PUBLIC_DIR = path.join(ROOT_DIR, 'public');
const INSTALLERS_DIR = path.join(PUBLIC_DIR, 'installers');
const RELEASE_MANIFEST = path.join(ROOT_DIR, 'downloads.json');
const MAX_JSON_BODY_BYTES = 64 * 1024;
const ADMIN_TOKEN = process.env.CODEQUEST_ADMIN_TOKEN || '';
const RELEASE_DOWNLOAD_URL = process.env.CODEQUEST_RELEASE_DOWNLOAD_URL || '';
const PUBLIC_BASE_URL = (process.env.CODEQUEST_PUBLIC_BASE_URL || '').replace(/\/$/, '');
const ALLOWED_ORIGINS = new Set((process.env.CODEQUEST_ALLOWED_ORIGINS || '')
  .split(',').map(value => value.trim()).filter(Boolean));

const MIME_TYPES = {
  '.css': 'text/css; charset=utf-8', '.html': 'text/html; charset=utf-8',
  '.ico': 'image/x-icon', '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8', '.png': 'image/png',
  '.svg': 'image/svg+xml', '.webp': 'image/webp'
};

const downloadStats = { totalDownloads: 0, windows: 0 };
const updateLogs = [];
const errorLogs = [];
const connectedWsClients = new Set();

function loadRelease() {
  const fallback = {
    latestVersion: '1.2.0', releaseDate: '2026-07-31T00:12:59Z',
    releaseName: 'CodeQuest Academy', releaseNotes: 'Download the latest CodeQuest Academy installer.',
    minimumVersion: '1.0.0', windows: { enabled: false }
  };
  try {
    const manifest = JSON.parse(fs.readFileSync(RELEASE_MANIFEST, 'utf8').replace(/^\uFEFF/, ''));
    return { ...fallback, ...manifest, windows: { ...fallback.windows, ...manifest.windows } };
  } catch (error) {
    console.warn(`Release manifest unavailable: ${error.message}`);
    return fallback;
  }
}

function compareVersions(left, right) {
  const parse = value => String(value).split(/[.+-]/).slice(0, 3).map(part => {
    const number = Number.parseInt(part, 10);
    return Number.isSafeInteger(number) && number >= 0 ? number : 0;
  });
  const a = parse(left); const b = parse(right);
  for (let index = 0; index < 3; index += 1) {
    const difference = (a[index] || 0) - (b[index] || 0);
    if (difference !== 0) return Math.sign(difference);
  }
  return 0;
}

function requestBaseUrl(req) {
  if (PUBLIC_BASE_URL) return PUBLIC_BASE_URL;
  const host = req.headers.host;
  return host && !/[\r\n]/.test(host) ? `http://${host}` : `http://localhost:${PORT}`;
}

function corsHeaders(req) {
  const origin = req.headers.origin;
  return origin && ALLOWED_ORIGINS.has(origin) ? { 'Access-Control-Allow-Origin': origin, Vary: 'Origin' } : {};
}

function sendJson(req, res, statusCode, data, extraHeaders = {}) {
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store',
    'X-Content-Type-Options': 'nosniff', ...corsHeaders(req), ...extraHeaders
  });
  res.end(JSON.stringify(data));
}

function getJsonBody(req) {
  return new Promise((resolve, reject) => {
    let totalBytes = 0; let tooLarge = false; const chunks = [];
    req.on('data', chunk => {
      totalBytes += chunk.length;
      if (totalBytes > MAX_JSON_BODY_BYTES) { tooLarge = true; return; }
      if (!tooLarge) chunks.push(chunk);
    });
    req.on('end', () => {
      if (tooLarge) return reject(Object.assign(new Error('Request body is too large.'), { statusCode: 413 }));
      if (totalBytes === 0) return resolve({});
      try { resolve(JSON.parse(Buffer.concat(chunks).toString('utf8'))); }
      catch { reject(Object.assign(new Error('Request body must be valid JSON.'), { statusCode: 400 })); }
    });
    req.on('error', reject);
  });
}

function hasAdminAccess(req) {
  if (!ADMIN_TOKEN) return false;
  const token = req.headers.authorization?.replace(/^Bearer\s+/i, '') || '';
  const expected = Buffer.from(ADMIN_TOKEN); const provided = Buffer.from(token);
  return expected.length === provided.length && crypto.timingSafeEqual(expected, provided);
}

function requireAdmin(req, res) {
  if (!ADMIN_TOKEN) { sendJson(req, res, 503, { error: 'Administrative endpoints are disabled.' }); return false; }
  if (!hasAdminAccess(req)) {
    sendJson(req, res, 401, { error: 'Administrative authorization is required.' }, { 'WWW-Authenticate': 'Bearer' });
    return false;
  }
  return true;
}

function safeFile(root, relativePath) {
  const candidate = path.resolve(root, relativePath);
  return candidate === root || candidate.startsWith(`${root}${path.sep}`) ? candidate : null;
}

function sendNotFound(req, res) { sendJson(req, res, 404, { error: 'Not found.' }); }

function redirectToStaticInstaller(res, fileName) {
  res.writeHead(302, {
    Location: `/installers/${encodeURIComponent(path.basename(fileName))}`,
    'Cache-Control': 'no-store'
  });
  return res.end();
}

function serveInstaller(req, res, release) {
  const file = release.windows;
  if (!file?.enabled || !file.fileName) return sendJson(req, res, 404, { error: 'Windows installer is not published.' });
  if (RELEASE_DOWNLOAD_URL) {
    res.writeHead(302, { Location: RELEASE_DOWNLOAD_URL, 'Cache-Control': 'no-store' });
    return res.end();
  }
  // Serverless functions cannot reliably stream a full desktop installer. On Vercel,
  // let the static-file CDN deliver it instead of returning a truncated error body.
  if (process.env.VERCEL || process.env.CODEQUEST_DIRECT_STATIC_DOWNLOADS === 'true') {
    return redirectToStaticInstaller(res, file.fileName);
  }
  const localFile = safeFile(INSTALLERS_DIR, file.fileName);
  if (!localFile) return sendNotFound(req, res);
  if (!fs.existsSync(localFile)) {
    return sendJson(req, res, 404, { error: 'Windows installer is not available on this server.' });
  }
  const stat = fs.statSync(localFile);
  const headers = {
    'Content-Type': 'application/octet-stream', 'Content-Disposition': `attachment; filename="${path.basename(file.fileName)}"`,
    'Accept-Ranges': 'bytes', 'X-Content-Type-Options': 'nosniff'
  };
  if (req.method === 'HEAD') { res.writeHead(200, { ...headers, 'Content-Length': stat.size }); return res.end(); }
  const range = req.headers.range;
  if (!range) { res.writeHead(200, { ...headers, 'Content-Length': stat.size }); return fs.createReadStream(localFile).pipe(res); }
  const match = /^bytes=(\d*)-(\d*)$/.exec(range);
  if (!match) { res.writeHead(416, { 'Content-Range': `bytes */${stat.size}` }); return res.end(); }
  const start = match[1] ? Number(match[1]) : 0;
  const end = match[2] ? Number(match[2]) : stat.size - 1;
  if (!Number.isSafeInteger(start) || !Number.isSafeInteger(end) || start < 0 || end < start || start >= stat.size) {
    res.writeHead(416, { 'Content-Range': `bytes */${stat.size}` }); return res.end();
  }
  const boundedEnd = Math.min(end, stat.size - 1);
  res.writeHead(206, { ...headers, 'Content-Range': `bytes ${start}-${boundedEnd}/${stat.size}`, 'Content-Length': boundedEnd - start + 1 });
  return fs.createReadStream(localFile, { start, end: boundedEnd }).pipe(res);
}

function serveStatic(req, res, pathname) {
  const rootFiles = new Set(['index.html', 'styles.css', 'script.js']);
  let requested;
  try { requested = pathname === '/' || pathname === '/download' ? 'index.html' : decodeURIComponent(pathname.replace(/^\//, '')); }
  catch { return sendNotFound(req, res); }
  const staticRoot = requested.startsWith('assets/') || requested.startsWith('installers/') || requested === 'favicon.ico' ? PUBLIC_DIR : ROOT_DIR;
  if (staticRoot === ROOT_DIR && !rootFiles.has(requested)) return sendNotFound(req, res);
  const file = safeFile(staticRoot, requested);
  if (!file || !fs.existsSync(file) || !fs.statSync(file).isFile()) return sendNotFound(req, res);
  const ext = path.extname(file).toLowerCase();
  res.writeHead(200, {
    'Content-Type': MIME_TYPES[ext] || 'application/octet-stream', 'X-Content-Type-Options': 'nosniff',
    'Cache-Control': ext === '.html' ? 'no-cache' : 'public, max-age=3600',
    'Referrer-Policy': 'strict-origin-when-cross-origin',
    'Content-Security-Policy': "default-src 'self'; connect-src 'self'; font-src 'self' https://fonts.gstatic.com; img-src 'self' data:; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com"
  });
  fs.createReadStream(file).pipe(res);
}

const server = http.createServer(async (req, res) => {
  try {
    const parsed = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
    const pathname = parsed.pathname; const release = loadRelease();
    if (req.method === 'OPTIONS') {
      res.writeHead(204, { ...corsHeaders(req), 'Access-Control-Allow-Headers': 'Authorization, Content-Type', 'Access-Control-Allow-Methods': 'GET, HEAD, POST, OPTIONS' });
      return res.end();
    }
    if ((pathname === '/api/download' || pathname === '/download/win') && ['GET', 'HEAD'].includes(req.method)) {
      if ((parsed.searchParams.get('os') || 'windows') !== 'windows') return sendJson(req, res, 404, { error: 'That platform is not currently published.' });
      if (req.method === 'GET') { downloadStats.windows += 1; downloadStats.totalDownloads += 1; }
      return serveInstaller(req, res, release);
    }
    if (pathname === '/api/app/latest-version' && req.method === 'GET') {
      const file = release.windows;
      return sendJson(req, res, 200, {
        version: release.latestVersion, downloadUrl: `${requestBaseUrl(req)}/api/download?os=windows`, releaseNotes: release.releaseNotes,
        minimumVersion: release.minimumVersion, releaseDate: release.releaseDate, supportedPlatforms: file?.enabled ? ['windows'] : [], sha256: file?.sha256 || ''
      });
    }
    if (pathname === '/api/app/check-updates' && req.method === 'GET') {
      const currentVersion = parsed.searchParams.get('version') || '0.0.0'; const os = parsed.searchParams.get('os') || 'windows';
      const file = os === 'windows' ? release.windows : null;
      if (!file?.enabled) return sendJson(req, res, 404, { error: `No ${os} update artifact is published.` });
      return sendJson(req, res, 200, {
        updateAvailable: compareVersions(release.latestVersion, currentVersion) > 0, currentVersion, latestVersion: release.latestVersion,
        downloadUrl: `${requestBaseUrl(req)}/api/download?os=windows`, fileName: file.fileName, sha256: file.sha256, sizeBytes: file.sizeBytes,
        releaseNotes: release.releaseNotes, isRequired: Boolean(release.isRequired), minimumVersion: release.minimumVersion
      });
    }
    if (pathname === '/api/app/update-status' && req.method === 'POST') {
      const body = await getJsonBody(req);
      updateLogs.push({ timestamp: new Date().toISOString(), status: String(body.status || ''), targetVersion: String(body.targetVersion || '') });
      updateLogs.splice(0, Math.max(0, updateLogs.length - 100)); return sendJson(req, res, 202, { status: 'accepted' });
    }
    if (pathname === '/api/app/report-error' && req.method === 'POST') {
      const body = await getJsonBody(req);
      errorLogs.push({ timestamp: new Date().toISOString(), errorType: String(body.errorType || ''), message: String(body.message || '').slice(0, 1000) });
      errorLogs.splice(0, Math.max(0, errorLogs.length - 100)); return sendJson(req, res, 202, { status: 'accepted' });
    }
    if (pathname === '/api/analytics/dashboard' && req.method === 'GET') {
      if (!requireAdmin(req, res)) return;
      return sendJson(req, res, 200, { latestVersion: release.latestVersion, downloads: downloadStats, recentUpdates: updateLogs.slice(-10), recentErrors: errorLogs.slice(-10), connectedWsClients: connectedWsClients.size });
    }
    if (pathname === '/api/admin/broadcast-update' && req.method === 'POST') {
      if (!requireAdmin(req, res)) return;
      const body = await getJsonBody(req);
      const payload = JSON.stringify({ type: 'UPDATE_AVAILABLE', version: body.version || release.latestVersion, releaseNotes: body.releaseNotes || release.releaseNotes, timestamp: new Date().toISOString() });
      let count = 0;
      for (const client of connectedWsClients) if (!client.destroyed) { client.write(encodeWsFrame(payload)); count += 1; }
      return sendJson(req, res, 200, { status: 'broadcasted', clientCount: count });
    }
    if (pathname.startsWith('/api/')) return sendNotFound(req, res);
    return serveStatic(req, res, pathname);
  } catch (error) {
    sendJson(req, res, error.statusCode || 500, { error: error.statusCode ? error.message : 'Internal server error.' });
  }
});

server.on('upgrade', (req, socket) => {
  if (req.url !== '/ws/notifications' || !ADMIN_TOKEN) return socket.destroy();
  const key = req.headers['sec-websocket-key'];
  if (typeof key !== 'string') return socket.destroy();
  const accept = crypto.createHash('sha1').update(`${key}258EAFA5-E914-47DA-95CA-C5AB0DC85B11`).digest('base64');
  socket.write(['HTTP/1.1 101 Switching Protocols', 'Upgrade: websocket', 'Connection: Upgrade', `Sec-WebSocket-Accept: ${accept}`, '', ''].join('\r\n'));
  connectedWsClients.add(socket); socket.on('close', () => connectedWsClients.delete(socket)); socket.on('error', () => connectedWsClients.delete(socket));
});

function encodeWsFrame(data) {
  const payload = Buffer.from(data);
  if (payload.length <= 125) return Buffer.concat([Buffer.from([0x81, payload.length]), payload]);
  if (payload.length <= 65535) { const header = Buffer.alloc(4); header[0] = 0x81; header[1] = 126; header.writeUInt16BE(payload.length, 2); return Buffer.concat([header, payload]); }
  const header = Buffer.alloc(10); header[0] = 0x81; header[1] = 127; header.writeBigUInt64BE(BigInt(payload.length), 2); return Buffer.concat([header, payload]);
}

if (require.main === module) server.listen(PORT, () => console.log(`CodeQuest Academy server listening on http://localhost:${PORT}`));
module.exports = server;
