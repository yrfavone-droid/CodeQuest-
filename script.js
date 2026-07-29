/* ===================================
   CodeQuest Academy — Web & Download System
   ===================================*/

document.addEventListener('DOMContentLoaded', () => {
    initParticles();
    initNavbar();
    initTypewriter();
    initScrollReveal();
    initCountUp();
    initProgressBars();
    initSmoothScroll();
    initTiltCards();
    initDownloadButtons();
    detectUserOS();
    fetchLatestVersionInfo();
    initFaqAccordion();
});

/* === Particle System === */
function initParticles() {
    const canvas = document.getElementById('particleCanvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');

    let width = canvas.width = window.innerWidth;
    let height = canvas.height = window.innerHeight;

    const particles = [];
    const particleCount = Math.min(80, Math.floor(width * height / 15000));
    const connectionDistance = 150;
    const mouseRadius = 200;

    let mouse = { x: -1000, y: -1000 };

    class Particle {
        constructor() {
            this.x = Math.random() * width;
            this.y = Math.random() * height;
            this.vx = (Math.random() - 0.5) * 0.4;
            this.vy = (Math.random() - 0.5) * 0.4;
            this.radius = Math.random() * 1.5 + 0.5;
            this.opacity = Math.random() * 0.4 + 0.1;
            this.hue = Math.random() > 0.5 ? 252 : 195;
        }

        update() {
            const dx = mouse.x - this.x;
            const dy = mouse.y - this.y;
            const dist = Math.sqrt(dx * dx + dy * dy);

            if (dist < mouseRadius) {
                const force = (mouseRadius - dist) / mouseRadius;
                this.vx -= (dx / dist) * force * 0.02;
                this.vy -= (dy / dist) * force * 0.02;
            }

            this.x += this.vx;
            this.y += this.vy;
            this.vx *= 0.999;
            this.vy *= 0.999;

            if (this.x < 0) this.x = width;
            if (this.x > width) this.x = 0;
            if (this.y < 0) this.y = height;
            if (this.y > height) this.y = 0;
        }

        draw() {
            ctx.beginPath();
            ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
            ctx.fillStyle = `hsla(${this.hue}, 70%, 70%, ${this.opacity})`;
            ctx.fill();
        }
    }

    for (let i = 0; i < particleCount; i++) {
        particles.push(new Particle());
    }

    function animate() {
        ctx.clearRect(0, 0, width, height);
        particles.forEach(p => {
            p.update();
            p.draw();
        });
        requestAnimationFrame(animate);
    }
    animate();

    window.addEventListener('resize', () => {
        width = canvas.width = window.innerWidth;
        height = canvas.height = window.innerHeight;
    });

    window.addEventListener('mousemove', (e) => {
        mouse.x = e.clientX;
        mouse.y = e.clientY;
    });
}

/* === Navbar === */
function initNavbar() {
    const navbar = document.getElementById('navbar');
    const toggle = document.getElementById('navToggle');
    const links = document.getElementById('navLinks');

    if (navbar) {
        window.addEventListener('scroll', () => {
            if (window.scrollY > 50) {
                navbar.classList.add('scrolled');
            } else {
                navbar.classList.remove('scrolled');
            }
        });
    }

    if (toggle && links) {
        toggle.addEventListener('click', () => {
            toggle.classList.toggle('active');
            links.classList.toggle('active');
        });

        links.querySelectorAll('a').forEach(link => {
            link.addEventListener('click', () => {
                toggle.classList.remove('active');
                links.classList.remove('active');
            });
        });
    }
}

/* === Typewriter Effect === */
function initTypewriter() {
    const codeElement = document.getElementById('typedCode');
    if (!codeElement) return;

    const codeSnippet = `# CodeQuest Academy: Math-Based Code Solver
import math
from codequest import QuestEngine, Vector2D

def solve_matrix_quest(vector, theta_deg):
    """Rotates a vector by theta degrees using linear algebra."""
    radians = math.radians(theta_deg)
    cos_t, sin_t = math.cos(radians), math.sin(radians)
    
    # Apply 2D Rotation Matrix
    x_prime = vector.x * cos_t - vector.y * sin_t
    y_prime = vector.x * sin_t + vector.y * cos_t
    
    return Vector2D(round(x_prime, 4), round(y_prime, 4))

# Initialize Quest Runner & Solve
engine = QuestEngine(track="linear_algebra", level=10)
result = solve_matrix_quest(Vector2D(1, 0), theta_deg=90)
print(f"★ Quest Mastered! Output: {result}")`;

    // Render initial snippet immediately
    codeElement.textContent = codeSnippet;

    let i = 0;
    function type() {
        if (i <= codeSnippet.length) {
            codeElement.textContent = codeSnippet.substring(0, i);
            i++;
            setTimeout(type, 18);
        } else {
            // Loop animation after delay
            setTimeout(() => { i = 0; type(); }, 5000);
        }
    }
    setTimeout(type, 300);
}

/* === Scroll Reveal === */
function initScrollReveal() {
    const elements = document.querySelectorAll('.feature-card, .curriculum-card, .stat-card, .testimonial-card');
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('visible');
                observer.unobserve(entry.target);
            }
        });
    }, { threshold: 0.15 });

    elements.forEach(el => observer.observe(el));
}

/* === Count Up Animation === */
function initCountUp() {
    const statNumbers = document.querySelectorAll('.stat-number');
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const el = entry.target;
                const target = parseInt(el.dataset.target || "10000");
                animateNumber(el, 0, target, 2000);
                observer.unobserve(el);
            }
        });
    }, { threshold: 0.5 });

    statNumbers.forEach(el => observer.observe(el));
}

function animateNumber(el, start, end, duration) {
    const startTime = performance.now();
    function update(currentTime) {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);
        const eased = 1 - Math.pow(1 - progress, 3);
        const current = Math.floor(start + (end - start) * eased);
        el.textContent = current.toLocaleString();
        if (progress < 1) requestAnimationFrame(update);
    }
    requestAnimationFrame(update);
}

/* === Progress Bars === */
function initProgressBars() {
    const progressElements = document.querySelectorAll('.quest-bar, .stat-fill');
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const card = entry.target.closest('.feature-card, .stat-card');
                if (card) card.classList.add('visible');
                observer.unobserve(entry.target);
            }
        });
    }, { threshold: 0.3 });
    progressElements.forEach(el => observer.observe(el));
}

/* === Smooth Scroll === */
function initSmoothScroll() {
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        });
    });
}

/* === Tilt Cards === */
function initTiltCards() {
    const cards = document.querySelectorAll('[data-tilt]');
    cards.forEach(card => {
        card.addEventListener('mousemove', (e) => {
            const rect = card.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;
            const centerX = rect.width / 2;
            const centerY = rect.height / 2;
            const tiltX = (y - centerY) / centerY * 3;
            const tiltY = (centerX - x) / centerX * 3;

            card.style.transform = `perspective(1000px) rotateX(${tiltX}deg) rotateY(${tiltY}deg) translateY(-4px)`;
        });

        card.addEventListener('mouseleave', () => {
            card.style.transform = '';
        });
    });
}

/* === Automatic OS Detection === */
function detectUserOS() {
    const userAgent = navigator.userAgent.toLowerCase();
    let detectedOS = 'windows';

    if (userAgent.includes('mac') || userAgent.includes('darwin')) {
        detectedOS = 'macos';
    } else if (userAgent.includes('linux') || userAgent.includes('x11')) {
        detectedOS = 'linux';
    }

    const btnWin = document.getElementById('downloadWindows');
    const btnMac = document.getElementById('downloadMac');
    const btnLin = document.getElementById('downloadLinux');

    if (detectedOS === 'windows' && btnWin) {
        highlightRecommendedButton(btnWin, 'Windows');
    } else if (detectedOS === 'macos' && btnMac) {
        highlightRecommendedButton(btnMac, 'macOS');
    } else if (detectedOS === 'linux' && btnLin) {
        highlightRecommendedButton(btnLin, 'Linux');
    }
}

function highlightRecommendedButton(btn, osName) {
    btn.classList.add('recommended-os');
    const smallText = btn.querySelector('.download-btn-small');
    if (smallText) {
        smallText.innerHTML = `Recommended for your ${osName} <span class="detected-tag">Auto-Detected</span>`;
    }
}

/* === Fetch Latest Version Info from API === */
async function fetchLatestVersionInfo() {
    try {
        const response = await fetch('/api/app/latest-version');
        if (response.ok) {
            const data = await response.json();
            const versionElements = document.querySelectorAll('.download-version');
            versionElements.forEach(el => {
                el.textContent = `v${data.version} • Latest Build (${data.releaseDate ? new Date(data.releaseDate).toLocaleDateString() : 'Active'})`;
            });
        }
    } catch (e) {
        console.log('Using static version display fallback.');
    }
}

/* === Download Buttons & Telemetry === */
function initDownloadButtons() {
    const downloadBtns = document.querySelectorAll('.download-btn');

    downloadBtns.forEach(btn => {
        btn.addEventListener('click', (e) => {
            const href = btn.getAttribute('href');
            if (href && (href.startsWith('http://') || href.startsWith('https://'))) {
                return; // Allow browser to open direct CDN link directly
            }
            let targetFile = '/api/download?os=windows';
            let downloadFileName = 'codequest-academy-setup.exe';
            let targetOS = 'Windows';

            if (btn.id === 'downloadMac') {
                targetFile = '/api/download?os=macos';
                downloadFileName = 'CodeQuest-Academy-1.2.0.dmg';
                targetOS = 'macOS';
            } else if (btn.id === 'downloadLinux') {
                targetFile = '/api/download?os=linux';
                downloadFileName = 'CodeQuest-Academy-1.2.0.AppImage';
                targetOS = 'Linux';
            }

            // Send telemetry log to backend API
            fetch('/api/app/update-status', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    userId: 'web_visitor_' + Math.random().toString(36).substring(7),
                    currentVersion: 'website',
                    targetVersion: '1.2.0',
                    status: 'download_started',
                    os: targetOS
                })
            }).catch(() => {});
        });
    });
}

/* === FAQ Accordion === */
function initFaqAccordion() {
    const faqItems = document.querySelectorAll('.faq-item');
    faqItems.forEach(item => {
        const question = item.querySelector('.faq-question');
        if (question) {
            question.addEventListener('click', () => {
                const isOpen = item.classList.contains('active');
                faqItems.forEach(i => i.classList.remove('active'));
                if (!isOpen) {
                    item.classList.add('active');
                }
            });
        }
    });
}
