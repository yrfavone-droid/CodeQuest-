document.querySelectorAll('[data-download]').forEach(link => {
  link.addEventListener('click', () => link.setAttribute('aria-busy', 'true'), { once: true });
});

// Keep release copy accurate when a cached page is opened after an update.
fetch('/downloads.json', { cache: 'no-store' })
  .then(response => response.ok ? response.json() : null)
  .then(release => {
    const version = release?.latestVersion;
    const fileName = release?.windows?.fileName;
    if (!version || !fileName) return;
    const sizeMb = release.windows.sizeBytes
      ? (release.windows.sizeBytes / 1024 / 1024).toFixed(1)
      : null;
    document.querySelectorAll('[data-version]').forEach(node => { node.textContent = version; });
    document.querySelectorAll('[data-installer-name]').forEach(node => { node.textContent = fileName; });
    document.querySelectorAll('[data-installer-size]').forEach(node => {
      if (sizeMb) node.textContent = `${sizeMb} MB`;
    });
    document.querySelectorAll('[data-installer-sha256]').forEach(node => {
      if (release.windows.sha256) node.textContent = release.windows.sha256.toUpperCase();
    });
    document.querySelectorAll('[data-download]').forEach(link => {
      link.textContent = `Download version ${version}`;
      link.setAttribute('href', `/api/download?os=windows`);
      link.setAttribute('download', fileName);
    });
  })
  .catch(() => { /* static fallback text remains usable offline */ });
