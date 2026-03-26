// Main SPA controller
const App = {
    currentPage: 'home',
    pages: { home: HomePage, food: FoodPage, workout: WorkoutPage, profile: ProfilePage },

    init() {
        // Auth on load
        API.post('/auth/validate').catch(() => {});

        // Navigation
        document.querySelectorAll('.nav-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                TG.haptic('selection');
                this.navigate(btn.dataset.page);
            });
        });

        // Modal
        document.getElementById('modal-overlay').addEventListener('click', (e) => {
            if (e.target === e.currentTarget) this.closeModal();
        });
        document.getElementById('modal-close').addEventListener('click', () => this.closeModal());

        // Load home
        this.navigate('home');
    },

    navigate(page) {
        // Destroy all charts before re-render to avoid stale references
        Object.keys(Charts.instances).forEach(id => Charts.destroy(id));

        this.currentPage = page;

        // Toggle page visibility
        document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
        document.getElementById(`page-${page}`)?.classList.add('active');

        // Toggle nav active
        document.querySelectorAll('.nav-btn').forEach(b => {
            b.classList.toggle('active', b.dataset.page === page);
        });

        // Render page
        this.pages[page]?.render();
    },

    /** Re-render current page (call after data changes) */
    refresh() {
        Object.keys(Charts.instances).forEach(id => Charts.destroy(id));
        this.pages[this.currentPage]?.render();
    },

    openModal(title, bodyHtml) {
        document.getElementById('modal-title').textContent = title;
        document.getElementById('modal-body').innerHTML = bodyHtml;
        document.getElementById('modal-overlay').classList.remove('hidden');
    },

    closeModal() {
        document.getElementById('modal-overlay').classList.add('hidden');
    },

    showToast(message, duration = 2000) {
        const toast = document.getElementById('toast');
        toast.textContent = message;
        toast.classList.add('show');
        setTimeout(() => toast.classList.remove('show'), duration);
    },
};

// Start
document.addEventListener('DOMContentLoaded', () => App.init());
