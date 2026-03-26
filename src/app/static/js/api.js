// HTTP client with Telegram auth
const API = {
    BASE: '/api/v1',

    async request(method, path, body = null) {
        const opts = {
            method,
            headers: {
                'Content-Type': 'application/json',
                'X-Telegram-Init-Data': TG.initData,
            },
        };
        if (body) opts.body = JSON.stringify(body);

        const res = await fetch(this.BASE + path, opts);
        if (res.status === 204) return null;
        if (!res.ok) {
            const err = await res.json().catch(() => ({ detail: 'Request failed' }));
            throw new Error(err.detail || 'Request failed');
        }
        return res.json();
    },

    get(path) { return this.request('GET', path); },
    post(path, body) { return this.request('POST', path, body); },
    put(path, body) { return this.request('PUT', path, body); },
    del(path) { return this.request('DELETE', path); },
};
