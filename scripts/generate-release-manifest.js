const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const versionMatch = fs.readFileSync(path.join(root, 'gradle.properties'), 'utf8').match(/^codequest\.version=(.+)$/m);
if (!versionMatch) throw new Error('codequest.version is missing from gradle.properties');
const version = versionMatch[1].trim();
const fileName = 'codequest-academy-setup.exe';
const packageMetadata = JSON.parse(fs.readFileSync(path.join(root, 'package.json'), 'utf8'));
const repositoryUrl = String(packageMetadata.repository?.url || '').replace(/^git\+/, '').replace(/\.git$/, '');
const hostedInstallerUrl = process.env.CODEQUEST_PUBLIC_INSTALLER_URL ||
  (repositoryUrl ? `${repositoryUrl}/raw/main/public/installers/${encodeURIComponent(fileName)}` : '');
if (!hostedInstallerUrl.startsWith('https://')) throw new Error('A public HTTPS installer URL is required.');
const installer = path.join(root, 'public', 'installers', fileName);
if (!fs.existsSync(installer)) throw new Error(`Installer not found: ${installer}`);
const bytes = fs.readFileSync(installer);
const manifest = {
  latestVersion: version,
  releaseDate: new Date().toISOString(),
  releaseName: `CodeQuest Academy ${version}`,
  releaseNotes: 'Windows installer release with bundled JVM runtime.',
  minimumVersion: '1.0.0',
  windows: {
    enabled: true,
    label: 'Download Single EXE Installer for Windows',
    url: 'api/download?os=windows',
    downloadUrl: hostedInstallerUrl,
    fileName,
    minimumVersion: 'Windows 10',
    architecture: 'x64',
    sizeBytes: bytes.length,
    sha256: crypto.createHash('sha256').update(bytes).digest('hex'),
    sha512: crypto.createHash('sha512').update(bytes).digest('hex'),
    distribution: 'installer'
  }
};
fs.writeFileSync(path.join(root, 'downloads.json'), `${JSON.stringify(manifest, null, 2)}\n`);
const releases = path.join(root, 'releases');
fs.mkdirSync(releases, { recursive: true });
fs.writeFileSync(path.join(releases, 'releases.json'), `${JSON.stringify(manifest, null, 2)}\n`);
fs.writeFileSync(path.join(releases, 'latest.yml'), [
  `version: ${version}`,
  `releaseDate: '${manifest.releaseDate}'`,
  `releaseName: '${manifest.releaseName}'`,
  `path: ${fileName}`,
  `sha512: ${manifest.windows.sha512}`,
  'files:',
  `  - url: ${fileName}`,
  `    sha512: ${manifest.windows.sha512}`,
  `    size: ${manifest.windows.sizeBytes}`,
  ''
].join('\n'));
console.log(`Generated release manifest for ${fileName} v${version}`);
