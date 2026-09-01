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
    document.querySelectorAll('[data-version]').forEach(node => { node.textContent = version; });
    document.querySelectorAll('[data-download]').forEach(link => {
      link.textContent = `Download version ${version}`;
      link.setAttribute('href', `/api/download?os=windows`);
      link.setAttribute('download', fileName);
    });
  })
  .catch(() => { /* static fallback text remains usable offline */ });
