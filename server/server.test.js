const assert = require('node:assert/strict');
const fs = require('node:fs');
const http = require('node:http');
const path = require('node:path');
const test = require('node:test');
const server = require('./server');

let baseUrl;
const release = JSON.parse(fs.readFileSync(path.join(__dirname, '..', 'downloads.json'), 'utf8').replace(/^\uFEFF/, ''));
const expectedReleaseUrl = release.windows.downloadUrl;
const expectedInstallerPath = `/installers/${release.windows.fileName}`;

function request(path, options = {}) {
  return new Promise((resolve, reject) => {
    const request = http.request(`${baseUrl}${path}`, options, response => {
      let body = '';
      response.setEncoding('utf8');
      response.on('data', chunk => { body += chunk; });
      response.on('end', () => resolve({ statusCode: response.statusCode, headers: response.headers, body }));
    });
    request.on('error', reject);
    if (options.body) request.write(options.body);
    request.end();
  });
}

test.before(async () => {
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const address = server.address();
  baseUrl = `http://127.0.0.1:${address.port}`;
});

test.after(async () => new Promise(resolve => server.close(resolve)));

test('serves the landing page but never repository source or database files', async () => {
  assert.equal((await request('/')).statusCode, 200);
  assert.equal((await request('/server/server.js')).statusCode, 404);
  assert.equal((await request('/nous_ai_academy.db')).statusCode, 404);
  assert.equal((await request('/..%2Fserver%2Fserver.js')).statusCode, 404);
});

test('uses semantic versions and only publishes the supported Windows installer', async () => {
  const newerClient = await request('/api/app/check-updates?version=1.10.0&os=windows');
  assert.equal(newerClient.statusCode, 200);
  assert.equal(JSON.parse(newerClient.body).updateAvailable, false);
  assert.equal((await request('/api/app/check-updates?version=1.0.0&os=macos')).statusCode, 404);
});

test('uses the forwarded HTTPS protocol for public update links', async () => {
  const response = await request('/api/app/latest-version', { headers: { 'x-forwarded-proto': 'https' } });
  assert.equal(response.statusCode, 200);
  assert.match(JSON.parse(response.body).downloadUrl, /^https:\/\/127\.0\.0\.1:/);
});

test('redirects public downloads to the verified HTTPS installer release', async () => {
  const response = await request('/api/download?os=windows', { method: 'HEAD' });
  assert.equal(response.statusCode, 302);
  assert.equal(response.headers.location, expectedReleaseUrl);
});

test('the installer alias serves the verified installer directly', async () => {
  const response = await request('/installers/nous-ai-academy-setup.exe', { method: 'HEAD' });
  assert.equal(response.statusCode, 200);
  assert.match(response.headers['content-disposition'], new RegExp(release.windows.fileName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
  assert.equal(Number(response.headers['content-length']), release.windows.sizeBytes);
});

test('the versioned installer URL serves the verified installer directly', async () => {
  const response = await request(expectedInstallerPath, { method: 'HEAD' });
  assert.equal(response.statusCode, 200);
  assert.match(response.headers['content-disposition'], new RegExp(release.windows.fileName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
  assert.equal(Number(response.headers['content-length']), release.windows.sizeBytes);
});

test('keeps administration disabled without an explicit server token', async () => {
  assert.equal((await request('/api/analytics/dashboard')).statusCode, 503);
  assert.equal((await request('/api/admin/broadcast-update', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{}'
  })).statusCode, 503);
});

test('rejects malformed and oversized JSON telemetry', async () => {
  assert.equal((await request('/api/app/update-status', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{'
  })).statusCode, 400);
  assert.equal((await request('/api/app/update-status', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ payload: 'x'.repeat(70 * 1024) })
  })).statusCode, 413);
});
