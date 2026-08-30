document.querySelectorAll('[data-download]').forEach(link => {
  link.addEventListener('click', () => link.setAttribute('aria-busy', 'true'), { once: true });
});
