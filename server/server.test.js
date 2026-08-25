const assert = require('node:assert/strict');
const fs = require('node:fs');
const http = require('node:http');
const path = require('node:path');
const test = require('node:test');
const server = require('./server');

let baseUrl;
const expectedReleaseUrl = JSON.parse(fs.readFileSync(path.join(__dirname, '..', 'downloads.json'), 'utf8').replace(/^\uFEFF/, '')).windows.downloadUrl;

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
  assert.equal((await request('/codequest_progress.db')).statusCode, 404);
  assert.equal((await request('/..%2Fserver%2Fserver.js')).statusCode, 404);
});

test('uses semantic versions and publishes only supported platforms', async () => {
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

test('Vercel downloads redirect to the verified hosted installer', async () => {
  process.env.VERCEL = '1';
  try {
    const response = await request('/api/download?os=windows');
    assert.equal(response.statusCode, 302);
    assert.equal(response.headers.location, expectedReleaseUrl);
  } finally {
    delete process.env.VERCEL;
  }
});

test('legacy installer URLs redirect to the hosted release instead of serving an LFS pointer', async () => {
  process.env.VERCEL = '1';
  try {
    const response = await request('/installers/codequest-academy-setup.exe');
    assert.equal(response.statusCode, 302);
    assert.equal(response.headers.location, expectedReleaseUrl);
  } finally {
    delete process.env.VERCEL;
  }
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
