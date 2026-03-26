// Telegram WebApp SDK wrapper
const TG = {
    app: window.Telegram?.WebApp,

    init() {
        if (this.app) {
            this.app.ready();
            this.app.expand();
            this.app.enableClosingConfirmation();
        }
    },

    get initData() {
        return this.app?.initData || '';
    },

    get user() {
        return this.app?.initDataUnsafe?.user || null;
    },

    get colorScheme() {
        return this.app?.colorScheme || 'light';
    },

    haptic(type = 'impact', style = 'light') {
        try {
            if (type === 'impact') {
                this.app?.HapticFeedback?.impactOccurred(style);
            } else if (type === 'notification') {
                this.app?.HapticFeedback?.notificationOccurred(style);
            } else if (type === 'selection') {
                this.app?.HapticFeedback?.selectionChanged();
            }
        } catch {}
    },
};

TG.init();
