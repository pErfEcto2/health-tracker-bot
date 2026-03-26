// Profile page
const ProfilePage = {
    chartPeriod: 'month',

    async render() {
        const page = document.getElementById('page-profile');
        page.innerHTML = '<div class="loading">Загрузка...</div>';

        try {
            const [profile, tdee] = await Promise.all([
                API.get('/profile'),
                API.get('/profile/tdee'),
            ]);

            page.innerHTML = `
                <div class="section-header">Профиль</div>
                <div class="card">
                    <div class="card-header">
                        <span class="card-title">${this.escape(profile.first_name)} ${this.escape(profile.last_name || '')}</span>
                        <button class="btn btn-secondary btn-sm" onclick="ProfilePage.showEditModal()">Изменить</button>
                    </div>
                    <div class="stats-grid">
                        <div class="stat-item">
                            <div class="stat-value">${profile.weight_kg || '—'}</div>
                            <div class="stat-label">Вес (кг)</div>
                        </div>
                        <div class="stat-item">
                            <div class="stat-value">${profile.height_cm || '—'}</div>
                            <div class="stat-label">Рост (см)</div>
                        </div>
                        <div class="stat-item">
                            <div class="stat-value">${profile.birth_date ? this.calcAge(profile.birth_date) : '—'}</div>
                            <div class="stat-label">Возраст</div>
                        </div>
                        <div class="stat-item">
                            <div class="stat-value">${this.genderLabel(profile.gender)}</div>
                            <div class="stat-label">Пол</div>
                        </div>
                    </div>
                </div>

                ${tdee.tdee > 0 ? `
                <div class="card">
                    <div class="card-title">Энергия</div>
                    <div class="stats-grid" style="margin-top:8px">
                        <div class="stat-item">
                            <div class="stat-value">${Math.round(tdee.bmr)}</div>
                            <div class="stat-label">BMR (ккал)</div>
                        </div>
                        <div class="stat-item">
                            <div class="stat-value">${Math.round(tdee.tdee)}</div>
                            <div class="stat-label">TDEE (ккал)</div>
                        </div>
                    </div>
                    <div class="card-subtitle">Активность: ${this.activityLabel(tdee.activity_level)}</div>
                </div>` : ''}

                <div class="section-header">Замеры тела</div>
                <button class="btn btn-primary" style="margin-bottom:12px" onclick="ProfilePage.showMeasurementModal()">+ Добавить замер</button>

                <div class="period-selector">
                    ${['month', '3months', 'year'].map(p => `
                        <button class="period-btn ${this.chartPeriod === p ? 'active' : ''}" onclick="ProfilePage.setChartPeriod('${p}')">${this.periodLabel(p)}</button>
                    `).join('')}
                </div>

                <div class="card">
                    <div class="chart-container"><canvas id="profile-weight-chart"></canvas></div>
                </div>
                <div class="card">
                    <div class="chart-container"><canvas id="profile-measurements-chart"></canvas></div>
                </div>
            `;

            this.loadCharts();
        } catch (e) {
            page.innerHTML = '<div class="empty-state"><p>Заполните профиль, чтобы начать</p></div>';
            this.showEditModal();
        }
    },

    async loadCharts() {
        const [weight, measurements] = await Promise.all([
            API.get(`/stats/weight?period=${this.chartPeriod}`),
            API.get(`/stats/measurements?period=${this.chartPeriod}`),
        ]);
        if (weight.length) Charts.weightLine('profile-weight-chart', weight);
        if (measurements.length) Charts.measurementsMultiLine('profile-measurements-chart', measurements);
    },

    setChartPeriod(p) {
        this.chartPeriod = p;
        TG.haptic('selection');
        App.refresh();
    },

    showEditModal() {
        TG.haptic('impact', 'light');
        App.openModal('Редактировать профиль', `
            <div class="form-group"><label>Пол</label>
                <select id="prof-gender"><option value="">—</option><option value="male">Мужской</option><option value="female">Женский</option></select>
            </div>
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px">
                <div class="form-group"><label>Вес (кг)</label><input id="prof-weight" type="number" step="0.1"></div>
                <div class="form-group"><label>Рост (см)</label><input id="prof-height" type="number" step="0.1"></div>
            </div>
            <div class="form-group"><label>Дата рождения (ДД.ММ.ГГГГ)</label><input id="prof-birth" type="text" inputmode="numeric" placeholder="01.01.1990" maxlength="10" oninput="ProfilePage.maskDate(this)"></div>
            <div class="form-group"><label>Уровень активности</label>
                <select id="prof-activity">
                    <option value="">—</option>
                    <option value="sedentary">Сидячий — офис, мало движения</option>
                    <option value="light">Лёгкий — прогулки 1-3 раза/нед</option>
                    <option value="moderate">Умеренный — спорт 3-5 раз/нед</option>
                    <option value="active">Активный — спорт 6-7 раз/нед</option>
                    <option value="very_active">Очень активный — 2 тренировки/день</option>
                </select>
            </div>
            <button class="btn btn-primary" onclick="ProfilePage.submitEdit()">Сохранить</button>
        `);
    },

    async submitEdit() {
        const data = {};
        const gender = document.getElementById('prof-gender').value;
        const weight = document.getElementById('prof-weight').value;
        const height = document.getElementById('prof-height').value;
        const birth = document.getElementById('prof-birth').value;
        const activity = document.getElementById('prof-activity').value;

        if (gender) data.gender = gender;
        if (weight) data.weight_kg = parseFloat(weight);
        if (height) data.height_cm = parseFloat(height);
        if (birth) {
            const parts = birth.split('.');
            if (parts.length === 3) data.birth_date = `${parts[2]}-${parts[1]}-${parts[0]}`;
        }
        if (activity) data.activity_level = activity;

        try {
            await API.put('/profile', data);
            TG.haptic('notification', 'success');
            App.closeModal();
            App.showToast('Профиль сохранён');
            App.refresh();
        } catch (e) {
            App.showToast('Ошибка: ' + e.message);
        }
    },

    showMeasurementModal() {
        TG.haptic('impact', 'light');
        App.openModal('Новый замер', `
            <div class="form-group"><label>Вес (кг)</label><input id="meas-weight" type="number" step="0.1"></div>
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px">
                <div class="form-group"><label>Талия (см)</label><input id="meas-waist" type="number" step="0.1"></div>
                <div class="form-group"><label>Бицепс (см)</label><input id="meas-bicep" type="number" step="0.1"></div>
                <div class="form-group"><label>Бёдра (см)</label><input id="meas-hip" type="number" step="0.1"></div>
                <div class="form-group"><label>Грудь (см)</label><input id="meas-chest" type="number" step="0.1"></div>
            </div>
            <button class="btn btn-primary" onclick="ProfilePage.submitMeasurement()">Сохранить</button>
        `);
    },

    async submitMeasurement() {
        const data = {};
        const w = document.getElementById('meas-weight').value;
        const waist = document.getElementById('meas-waist').value;
        const bicep = document.getElementById('meas-bicep').value;
        const hip = document.getElementById('meas-hip').value;
        const chest = document.getElementById('meas-chest').value;

        if (w) data.weight_kg = parseFloat(w);
        if (waist) data.waist_cm = parseFloat(waist);
        if (bicep) data.bicep_cm = parseFloat(bicep);
        if (hip) data.hip_cm = parseFloat(hip);
        if (chest) data.chest_cm = parseFloat(chest);

        try {
            await API.post('/measurements', data);
            TG.haptic('notification', 'success');
            App.closeModal();
            App.showToast('Замер сохранён');
            App.refresh();
        } catch (e) {
            App.showToast('Ошибка: ' + e.message);
        }
    },

    calcAge(birthDate) {
        const today = new Date();
        const birth = new Date(birthDate);
        let age = today.getFullYear() - birth.getFullYear();
        if (today.getMonth() < birth.getMonth() || (today.getMonth() === birth.getMonth() && today.getDate() < birth.getDate())) age--;
        return age;
    },

    maskDate(input) {
        let v = input.value.replace(/\D/g, '');
        if (v.length > 8) v = v.slice(0, 8);
        if (v.length > 4) v = v.slice(0, 2) + '.' + v.slice(2, 4) + '.' + v.slice(4);
        else if (v.length > 2) v = v.slice(0, 2) + '.' + v.slice(2);
        input.value = v;
    },

    genderLabel(g) { return { male: 'М', female: 'Ж' }[g] || '—'; },
    activityLabel(a) {
        return { sedentary: 'Сидячий', light: 'Лёгкий', moderate: 'Умеренный', active: 'Активный', very_active: 'Очень активный' }[a] || '—';
    },
    periodLabel(p) { return { month: 'Месяц', '3months': '3 мес', year: 'Год' }[p] || p; },
    escape(s) { const d = document.createElement('div'); d.textContent = s || ''; return d.innerHTML; },
};
