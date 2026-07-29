const http = require('http');
const fs = require('fs');
const path = require('path');
const url = require('url');
const crypto = require('crypto');

const PORT = process.env.PORT || 3000;
const ROOT_DIR = path.resolve(__dirname, '..');
const RELEASES_DIR = path.resolve(ROOT_DIR, 'releases');
const DOWNLOADS_DIR = path.resolve(ROOT_DIR, 'downloads');
const PUBLIC_INSTALLERS_DIR = path.resolve(ROOT_DIR, 'public', 'installers');
const LOGS_DIR = path.resolve(ROOT_DIR, 'logs');

// Ensure directories exist
[RELEASES_DIR, DOWNLOADS_DIR, PUBLIC_INSTALLERS_DIR, LOGS_DIR].forEach(dir => {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
});

// Initial releases manifest
const LATEST_RELEASE = {
  version: "1.2.0",
  releaseDate: new Date().toISOString(),
  releaseName: "Version 1.2.0 - Single EXE Installer Setup",
  releaseNotes: "• Single EXE Installer (codequest-academy-setup.exe) with automatic setup wizard\n• Bundled Java JVM runtime included\n• Real-Time Auto-Update System with WebSocket notification\n• Offline data sync & local cache management",
  downloadUrl: "/api/download",
  isRequired: false,
  minimumVersion: "1.0.0",
  files: {
    windows: {
      fileName: "codequest-academy-setup.exe",
      url: "/api/download?os=windows",
      sha256: "60C39A9AB1E721BA077038473A03DD99FB273F6F7478B2FA44EFBB1C4BA18C70",
      sizeBytes: 76430848
    },
    macos: {
      fileName: "CodeQuest-Academy-1.2.0.dmg",
      url: "/api/download?os=macos",
      sha256: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      sizeBytes: 81200300
    },
    linux: {
      fileName: "CodeQuest-Academy-1.2.0.AppImage",
      url: "/api/download?os=linux",
      sha256: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      sizeBytes: 79400100
    }
  }
};

// In-memory data store for analytics & user telemetry
const downloadStats = { totalDownloads: 1420, windows: 980, macos: 290, linux: 150 };
const updateLogs = [];
const errorLogs = [];
const connectedWsClients = new Set();

// Write latest.yml manifest
const latestYmlContent = `version: ${LATEST_RELEASE.version}
releaseDate: '${LATEST_RELEASE.releaseDate}'
releaseName: '${LATEST_RELEASE.releaseName}'
path: ${LATEST_RELEASE.files.windows.fileName}
sha512: ${LATEST_RELEASE.files.windows.sha256}
files:
  - url: ${LATEST_RELEASE.files.windows.fileName}
    sha512: ${LATEST_RELEASE.files.windows.sha256}
    size: ${LATEST_RELEASE.files.windows.sizeBytes}
`;
fs.writeFileSync(path.join(RELEASES_DIR, 'latest.yml'), latestYmlContent, 'utf8');
fs.writeFileSync(path.join(RELEASES_DIR, 'releases.json'), JSON.stringify(LATEST_RELEASE, null, 2), 'utf8');

// Helper to parse JSON body
function getJsonBody(req) {
  return new Promise((resolve, reject) => {
    let body = '';
    req.on('data', chunk => body += chunk.toString());
    req.on('end', () => {
      try {
        resolve(body ? JSON.parse(body) : {});
      } catch (err) {
        resolve({});
      }
    });
    req.on('error', reject);
  });
}

// Helper to send JSON response
function sendJson(res, statusCode, data) {
  res.writeHead(statusCode, {
    'Content-Type': 'application/json',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS'
  });
  res.end(JSON.stringify(data));
}

// Helper to serve installer file directly with HEAD & Range (IDM/Multi-threaded download) support
function serveInstallerFile(req, res, filePath, downloadName) {
  let targetFile = path.resolve(filePath);

  if (!fs.existsSync(targetFile)) {
    const altPath1 = path.resolve(PUBLIC_INSTALLERS_DIR, downloadName);
    const altPath2 = path.resolve(RELEASES_DIR, downloadName);
    const altPath3 = path.resolve(DOWNLOADS_DIR, downloadName);

    if (fs.existsSync(altPath1)) targetFile = altPath1;
    else if (fs.existsSync(altPath2)) targetFile = altPath2;
    else if (fs.existsSync(altPath3)) targetFile = altPath3;
    else  if (!fs.existsSync(targetFile)) {
    // When hosted on cloud / Vercel where .exe binaries are published via GitHub Releases:
    console.log(`[Installer Server 302] File not local (${downloadName}). Redirecting to GitHub Release.`);
    res.writeHead(302, {
      'Location': 'https://github.com/yrfavone-droid/CodeQuest-/releases/download/v1.2.0/codequest-academy-setup.exe',
      'Access-Control-Allow-Origin': '*'
    });
    return res.end();
  }
  }

  const stat = fs.statSync(targetFile);
  const range = req.headers.range;

  const headers = {
    'Content-Type': 'application/octet-stream',
    'Content-Disposition': `attachment; filename="${downloadName}"`,
    'Accept-Ranges': 'bytes',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization, Range',
    'Access-Control-Expose-Headers': 'Content-Disposition, Content-Length, Content-Range'
  };

  // Support HEAD requests for IDM and browser download pre-flight checks
  if (req.method === 'HEAD') {
    headers['Content-Length'] = stat.size;
    res.writeHead(200, headers);
    return res.end();
  }

  // Support HTTP Range requests (IDM / Multi-threaded downloads / Pause & Resume)
  if (range) {
    const parts = range.replace(/bytes=/, "").split("-");
    const start = parseInt(parts[0], 10);
    const end = parts[1] ? parseInt(parts[1], 10) : stat.size - 1;
    const chunkSize = (end - start) + 1;

    headers['Content-Range'] = `bytes ${start}-${end}/${stat.size}`;
    headers['Content-Length'] = chunkSize;
    res.writeHead(206, headers);

    console.log(`[Installer Server Range 206] ${downloadName} bytes ${start}-${end}/${stat.size}`);
    const stream = fs.createReadStream(targetFile, { start, end });
    stream.pipe(res);
  } else {
    headers['Content-Length'] = stat.size;
    res.writeHead(200, headers);
    console.log(`[Installer Server 200] Serving Download: ${downloadName} (${stat.size} bytes)`);
    fs.createReadStream(targetFile).pipe(res);
  }
}

// Simple MIME types map
const MIME_TYPES = {
  '.html': 'text/html',
  '.css': 'text/css',
  '.js': 'application/javascript',
  '.json': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.zip': 'application/zip',
  '.exe': 'application/octet-stream',
  '.dmg': 'application/octet-stream',
  '.AppImage': 'application/octet-stream',
  '.yml': 'text/yaml'
};

const server = http.createServer(async (req, res) => {
  const parsedUrl = url.parse(req.url, true);
  const pathname = parsedUrl.pathname;

  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    res.writeHead(204, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization, Range',
      'Access-Control-Allow-Methods': 'GET, HEAD, POST, OPTIONS'
    });
    return res.end();
  }

  // ============ SINGLE EXE DOWNLOAD ENDPOINT ============
  if (pathname === '/api/download' || pathname === '/download/win' || pathname === '/installers/codequest-academy-setup.exe') {
    const userAgent = (req.headers['user-agent'] || '').toLowerCase();
    const osQuery = (parsedUrl.query.os || '').toLowerCase();

    let targetOS = 'windows';
    if (osQuery === 'macos' || userAgent.includes('mac') || userAgent.includes('darwin')) {
      targetOS = 'macos';
    } else if (osQuery === 'linux' || userAgent.includes('linux')) {
      targetOS = 'linux';
    }

    if (targetOS === 'windows') {
      downloadStats.windows++;
      downloadStats.totalDownloads++;
      const exePath = path.join(PUBLIC_INSTALLERS_DIR, 'codequest-academy-setup.exe');
      return serveInstallerFile(req, res, exePath, 'codequest-academy-setup.exe');
    } else if (targetOS === 'macos') {
      downloadStats.macos++;
      downloadStats.totalDownloads++;
      const dmgPath = path.join(RELEASES_DIR, 'CodeQuest-Academy-1.2.0.dmg');
      return serveInstallerFile(req, res, dmgPath, 'CodeQuest-Academy-1.2.0.dmg');
    } else {
      downloadStats.linux++;
      downloadStats.totalDownloads++;
      const appImagePath = path.join(RELEASES_DIR, 'CodeQuest-Academy-1.2.0.AppImage');
      return serveInstallerFile(req, res, appImagePath, 'CodeQuest-Academy-1.2.0.AppImage');
    }
  }

  // 1. Backend API Endpoints
  if (pathname === '/api/app/latest-version') {
    return sendJson(res, 200, {
      version: LATEST_RELEASE.version,
      downloadUrl: `http://localhost:${PORT}/api/download?os=windows`,
      releaseNotes: LATEST_RELEASE.releaseNotes,
      isRequired: LATEST_RELEASE.isRequired,
      minimumVersion: LATEST_RELEASE.minimumVersion,
      sha256: LATEST_RELEASE.files.windows.sha256,
      releaseDate: LATEST_RELEASE.releaseDate
    });
  }

  if (pathname === '/api/app/check-updates') {
    const currentVersion = parsedUrl.query.version || '1.0.0';
    const os = (parsedUrl.query.os || 'windows').toLowerCase();
    const updateAvailable = currentVersion !== LATEST_RELEASE.version;
    const fileInfo = LATEST_RELEASE.files[os] || LATEST_RELEASE.files.windows;

    return sendJson(res, 200, {
      updateAvailable,
      currentVersion,
      latestVersion: LATEST_RELEASE.version,
      downloadUrl: `http://localhost:${PORT}/api/download?os=${os}`,
      fileName: fileInfo.fileName,
      sha256: fileInfo.sha256,
      sizeBytes: fileInfo.sizeBytes,
      releaseNotes: LATEST_RELEASE.releaseNotes,
      isRequired: LATEST_RELEASE.isRequired,
      minimumVersion: LATEST_RELEASE.minimumVersion
    });
  }

  if (pathname === '/api/auth/login' && req.method === 'POST') {
    const body = await getJsonBody(req);
    return sendJson(res, 200, {
      success: true,
      token: `jwt_token_${crypto.randomBytes(16).toString('hex')}`,
      refreshToken: `refresh_token_${crypto.randomBytes(16).toString('hex')}`,
      user: {
        id: "usr_101",
        email: body.email || "learner@codequest.org",
        name: body.name || "Math Coding Explorer",
        createdVersion: "1.0.0"
      }
    });
  }

  if (pathname === '/api/auth/signup' && req.method === 'POST') {
    const body = await getJsonBody(req);
    return sendJson(res, 201, {
      success: true,
      token: `jwt_token_${crypto.randomBytes(16).toString('hex')}`,
      user: {
        id: "usr_" + Date.now(),
        email: body.email,
        name: body.name || "New Learner"
      }
    });
  }

  if (pathname === '/api/auth/token-refresh' && req.method === 'POST') {
    return sendJson(res, 200, {
      success: true,
      token: `jwt_token_${crypto.randomBytes(16).toString('hex')}`
    });
  }

  if (pathname === '/api/content/lessons') {
    return sendJson(res, 200, {
      version: LATEST_RELEASE.version,
      ttlSeconds: 86400,
      tracks: [
        {
          id: "track_math_foundations",
          title: "Math Structures & Logic Foundations",
          description: "Explore boolean algebra, set theory, graph theory, and matrix transformations in code.",
          lessonsCount: 12
        },
        {
          id: "track_algorithmic_math",
          title: "Algorithmic Complexity & Discrete Math",
          description: "Master Big-O analysis, recurrence relations, dynamic programming, and numerical algorithms.",
          lessonsCount: 15
        }
      ]
    });
  }

  if (pathname.startsWith('/api/content/challenges/')) {
    const challengeId = pathname.replace('/api/content/challenges/', '');
    return sendJson(res, 200, {
      id: challengeId,
      title: "Matrix Transformation Pipeline",
      mathConcept: "Linear Algebra & 2D Vector Rotation",
      difficulty: "Intermediate",
      instructions: "Implement a 2D rotation matrix function using theta angle in radians.",
      starterCode: "function rotateVector(x, y, theta) {\n  // Implement rotation math here\n  return { x: 0, y: 0 };\n}"
    });
  }

  if (pathname === '/api/user/progress') {
    if (req.method === 'POST') {
      const body = await getJsonBody(req);
      return sendJson(res, 200, { status: "synced", timestamp: new Date().toISOString(), progress: body });
    }
    return sendJson(res, 200, {
      completedLessons: 18,
      totalXP: 2450,
      currentLevelId: "level_math_matrix_01",
      achievements: ["MATH_MASTER_I", "OFFLINE_WARRIOR", "AUTO_UPDATED"]
    });
  }

  if (pathname === '/api/app/update-status' && req.method === 'POST') {
    const body = await getJsonBody(req);
    const entry = { timestamp: new Date().toISOString(), ...body };
    updateLogs.push(entry);
    console.log(`[Update Telemetry] Status: ${body.status} (v${body.targetVersion}) - Client: ${body.userId || 'anonymous'}`);
    return sendJson(res, 200, { status: "logged", entry });
  }

  if (pathname === '/api/app/report-error' && req.method === 'POST') {
    const body = await getJsonBody(req);
    const entry = { timestamp: new Date().toISOString(), ...body };
    errorLogs.push(entry);
    fs.appendFileSync(path.join(LOGS_DIR, 'crash-reports.log'), JSON.stringify(entry) + '\n', 'utf8');
    console.log(`[Crash Reporter] Captured ${body.errorType || 'Error'}: ${body.message}`);
    return sendJson(res, 200, { status: "received", errorId: "err_" + Date.now() });
  }

  if (pathname === '/api/app/feature-flags') {
    return sendJson(res, 200, {
      newMathModule: true,
      betaEditor: false,
      darkThemeV2: true,
      realTimeNotifications: true,
      offlineAutoSync: true,
      systemTrayEnabled: true
    });
  }

  if (pathname === '/api/analytics/dashboard') {
    return sendJson(res, 200, {
      latestVersion: LATEST_RELEASE.version,
      downloads: downloadStats,
      recentUpdates: updateLogs.slice(-10),
      recentErrors: errorLogs.slice(-10),
      connectedWsClients: connectedWsClients.size
    });
  }

  if (pathname === '/api/admin/broadcast-update' && req.method === 'POST') {
    const body = await getJsonBody(req);
    const updatePayload = JSON.stringify({
      type: "UPDATE_AVAILABLE",
      version: body.version || LATEST_RELEASE.version,
      releaseNotes: body.releaseNotes || LATEST_RELEASE.releaseNotes,
      downloadUrl: `http://localhost:${PORT}/api/download?os=windows`,
      timestamp: new Date().toISOString()
    });

    let count = 0;
    connectedWsClients.forEach(client => {
      try {
        client.write(encodeWsFrame(updatePayload));
        count++;
      } catch (err) {
        connectedWsClients.delete(client);
      }
    });
    return sendJson(res, 200, { status: "broadcasted", clientCount: count });
  }

  // 2. Direct Release File Endpoint
  if (pathname.startsWith('/releases/') || pathname.startsWith('/downloads/')) {
    const filename = path.basename(pathname);
    let filePath = path.join(RELEASES_DIR, filename);
    if (!fs.existsSync(filePath)) {
      filePath = path.join(DOWNLOADS_DIR, filename);
    }
    if (!fs.existsSync(filePath)) {
      filePath = path.join(PUBLIC_INSTALLERS_DIR, filename);
    }

    if (fs.existsSync(filePath)) {
      const ext = path.extname(filename).toLowerCase();
      if (ext === '.exe') {
        return serveInstallerFile(req, res, filePath, filename);
      }
      res.writeHead(200, {
        'Content-Type': MIME_TYPES[ext] || 'application/octet-stream',
        'Content-Length': fs.statSync(filePath).size,
        'Access-Control-Allow-Origin': '*'
      });
      return fs.createReadStream(filePath).pipe(res);
    }
  }

  // 3. Static Web Content Serving (index.html, styles.css, script.js, download page)
  let staticFile = pathname === '/' || pathname === '/download' ? 'index.html' : pathname.replace(/^\//, '');
  let fullPath = path.join(ROOT_DIR, staticFile);

  if (fs.existsSync(fullPath) && fs.statSync(fullPath).isFile()) {
    const ext = path.extname(fullPath).toLowerCase();
    res.writeHead(200, {
      'Content-Type': MIME_TYPES[ext] || 'text/plain',
      'Access-Control-Allow-Origin': '*'
    });
    return fs.createReadStream(fullPath).pipe(res);
  }

  // Fallback 404
  res.writeHead(404, { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' });
  res.end(JSON.stringify({ error: '404 Not Found', path: pathname }));
});

// Simple native WebSocket upgrade handler
server.on('upgrade', (req, socket, head) => {
  if (req.url === '/ws/notifications') {
    const key = req.headers['sec-websocket-key'];
    const acceptKey = crypto.createHash('sha1')
      .update(key + '258EAFA5-E914-47DA-95CA-C5AB0DC85B11')
      .digest('base64');

    const headers = [
      'HTTP/1.1 101 Switching Protocols',
      'Upgrade: websocket',
      'Connection: Upgrade',
      `Sec-WebSocket-Accept: ${acceptKey}`
    ];
    socket.write(headers.join('\r\n') + '\r\n\r\n');

    connectedWsClients.add(socket);
    console.log(`[WebSocket] Client connected. Total active clients: ${connectedWsClients.size}`);

    const welcomeMsg = JSON.stringify({
      type: "CONNECTED",
      serverVersion: LATEST_RELEASE.version,
      message: "Connected to CodeQuest Academy Real-Time Update Stream"
    });
    socket.write(encodeWsFrame(welcomeMsg));

    socket.on('close', () => {
      connectedWsClients.delete(socket);
      console.log(`[WebSocket] Client disconnected. Total active clients: ${connectedWsClients.size}`);
    });
    socket.on('error', () => {
      connectedWsClients.delete(socket);
    });
  } else {
    socket.destroy();
  }
});

function encodeWsFrame(data) {
  const payload = Buffer.from(data);
  const length = payload.length;
  let header;

  if (length <= 125) {
    header = Buffer.from([0x81, length]);
  } else if (length <= 65535) {
    header = Buffer.alloc(4);
    header[0] = 0x81;
    header[1] = 126;
    header.writeUInt16BE(length, 2);
  } else {
    header = Buffer.alloc(10);
    header[0] = 0x81;
    header[1] = 127;
    header.writeBigUInt64BE(BigInt(length), 2);
  }
  return Buffer.concat([header, payload]);
}

server.listen(PORT, () => {
  console.log(`===================================================`);
  console.log(`🚀 CodeQuest Academy Server & API running at: http://localhost:${PORT}`);
  console.log(`📥 Single EXE Download Endpoint: http://localhost:${PORT}/api/download?os=windows`);
  console.log(`📡 WebSocket Notification Stream at: ws://localhost:${PORT}/ws/notifications`);
  console.log(`📦 Release Manifest API: http://localhost:${PORT}/api/app/latest-version`);
  console.log(`===================================================`);
});
