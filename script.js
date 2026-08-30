document.addEventListener('DOMContentLoaded', () => {
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    initNavigation();
    initScrollProgress();
    initLibrarySwitcher();
    initFaq();
    initDownloadTelemetry();
    if (!reduceMotion) {
        initReveal();
        initCounters();
    } else {
        document.querySelectorAll('.reveal').forEach(element => element.classList.add('is-visible'));
        document.querySelectorAll('.count-up').forEach(element => { element.textContent = Number(element.dataset.target || 0).toLocaleString(); });
    }
});

function initNavigation() {
    const header = document.getElementById('navbar');
    const toggle = document.getElementById('navToggle');
    const links = document.getElementById('navLinks');
    const updateHeader = () => header?.classList.toggle('is-scrolled', window.scrollY > 12);
    updateHeader();
    window.addEventListener('scroll', updateHeader, { passive: true });

    toggle?.addEventListener('click', () => {
        const open = links?.classList.toggle('is-open') || false;
        toggle.classList.toggle('is-open', open);
        toggle.setAttribute('aria-expanded', String(open));
        toggle.setAttribute('aria-label', open ? 'Close navigation' : 'Open navigation');
    });
    links?.querySelectorAll('a').forEach(link => link.addEventListener('click', () => {
        links.classList.remove('is-open');
        toggle?.classList.remove('is-open');
        toggle?.setAttribute('aria-expanded', 'false');
    }));
}

function initScrollProgress() {
    const indicator = document.getElementById('scrollProgress');
    const update = () => {
        const available = document.documentElement.scrollHeight - window.innerHeight;
        indicator.style.width = `${available > 0 ? (window.scrollY / available) * 100 : 0}%`;
    };
    update();
    window.addEventListener('scroll', update, { passive: true });
}

function initReveal() {
    const observer = new IntersectionObserver(entries => {
        entries.forEach(entry => {
            if (!entry.isIntersecting) return;
            entry.target.classList.add('is-visible');
            observer.unobserve(entry.target);
        });
    }, { threshold: 0.12, rootMargin: '0px 0px -30px' });
    document.querySelectorAll('.reveal').forEach(element => observer.observe(element));
}

function initCounters() {
    const observer = new IntersectionObserver(entries => {
        entries.forEach(entry => {
            if (!entry.isIntersecting) return;
            animateCounter(entry.target);
            observer.unobserve(entry.target);
        });
    }, { threshold: 0.75 });
    document.querySelectorAll('.count-up').forEach(element => observer.observe(element));
}

function animateCounter(element) {
    const target = Number(element.dataset.target || 0);
    const startedAt = performance.now();
    const duration = 850;
    const tick = now => {
        const progress = Math.min((now - startedAt) / duration, 1);
        const eased = 1 - Math.pow(1 - progress, 3);
        element.textContent = Math.round(target * eased).toLocaleString();
        if (progress < 1) requestAnimationFrame(tick);
    };
    requestAnimationFrame(tick);
}

function initLibrarySwitcher() {
    const buttons = document.querySelectorAll('[data-library-filter]');
    const panels = document.querySelectorAll('[data-library-panel]');
    buttons.forEach(button => button.addEventListener('click', () => {
        const selected = button.dataset.libraryFilter;
        buttons.forEach(item => {
            const active = item === button;
            item.classList.toggle('is-active', active);
            item.setAttribute('aria-selected', String(active));
        });
        panels.forEach(panel => panel.classList.toggle('is-hidden', panel.dataset.libraryPanel !== selected));
    }));
}

function initFaq() {
    document.querySelectorAll('.faq-item').forEach(item => {
        const question = item.querySelector('.faq-question');
        question?.addEventListener('click', () => {
            const open = !item.classList.contains('is-open');
            document.querySelectorAll('.faq-item').forEach(other => {
                other.classList.remove('is-open');
                other.querySelector('.faq-question')?.setAttribute('aria-expanded', 'false');
            });
            item.classList.toggle('is-open', open);
            question.setAttribute('aria-expanded', String(open));
        });
    });
}

function initDownloadTelemetry() {
    document.querySelectorAll('[data-download]').forEach(link => link.addEventListener('click', () => {
        navigator.sendBeacon?.('/api/app/update-status', JSON.stringify({
            status: 'download_started',
            targetVersion: 'website',
            source: 'nous-ai-academy-landing'
        }));
    }));
}
