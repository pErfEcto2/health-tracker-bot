// Home page — daily summary + calories chart with BMR/TDEE
const HomePage = {
    async render() {
        const page = document.getElementById('page-home');
        page.innerHTML = '<div class="loading">Загрузка...</div>';

        try {
            const profile = await API.get('/profile');
            const hasProfile = profile.weight_kg && profile.height_cm && profile.birth_date;

            if (!hasProfile) {
                page.innerHTML = `
                    <div class="empty-state" style="padding-top:80px">
                        <p style="font-size:18px;font-weight:600;margin-bottom:8px">Добро пожаловать в TrackHub!</p>
                        <p style="font-size:14px;color:var(--hint);margin-bottom:24px">Для начала заполните свой профиль — это нужно для расчёта нормы калорий</p>
                        <button class="btn btn-primary" style="max-width:280px;margin:0 auto" onclick="App.navigate('profile'); setTimeout(() => ProfilePage.showEditModal(), 300)">Заполнить профиль</button>
                    </div>
                `;
                return;
            }

            const today = _localDate();
            const [summary, caloriesData, tdeeData, waterSummary, waterStats] = await Promise.all([
                API.get(`/food/summary?date=${today}`),
                API.get('/stats/calories?period=week'),
                API.get('/profile/tdee'),
                API.get(`/water/summary?date=${today}`),
                API.get('/stats/water?period=week'),
            ]);

            const bmr = tdeeData.bmr > 0 ? Math.round(tdeeData.bmr) : 0;
            const tdee = tdeeData.tdee > 0 ? Math.round(tdeeData.tdee) : 2000;
            const eaten = Math.round(summary.total_calories);
            const remaining = Math.max(0, tdee - eaten);
            const pctTdee = Math.min(100, (eaten / tdee) * 100);
            const pctBmr = bmr > 0 ? Math.min(100, (eaten / bmr) * 100) : 0;

            page.innerHTML = `
                <div class="section-header">Сегодня</div>
                <div class="card">
                    <div class="macros-row" style="margin-top:0;margin-bottom:12px">
                        <div class="macro-chip protein">
                            <div class="value">${Math.round(summary.total_protein_g)}г</div>
                            <div class="label">Белки</div>
                        </div>
                        <div class="macro-chip fat">
                            <div class="value">${Math.round(summary.total_fat_g)}г</div>
                            <div class="label">Жиры</div>
                        </div>
                        <div class="macro-chip carbs">
                            <div class="value">${Math.round(summary.total_carbs_g)}г</div>
                            <div class="label">Углеводы</div>
                        </div>
                    </div>

                    ${bmr > 0 ? `
                    <div class="progress-container">
                        <div class="progress-label">
                            <span>BMR (базовый обмен)</span>
                            <span>${eaten} / ${bmr} ккал</span>
                        </div>
                        <div class="progress-bar">
                            <div class="progress-fill" style="width:${pctBmr}%;background:var(--hint)"></div>
                        </div>
                    </div>` : ''}

                    <div class="progress-container">
                        <div class="progress-label">
                            <span>TDEE (дневная норма)</span>
                            <span>${eaten} / ${tdee} ккал</span>
                        </div>
                        <div class="progress-bar">
                            <div class="progress-fill calories" style="width:${pctTdee}%"></div>
                        </div>
                    </div>

                    <p style="font-size:13px;color:var(--hint);margin-top:4px;text-align:center">
                        ${remaining > 0 ? `Осталось <b style="color:var(--text)">${remaining}</b> ккал` : `<span style="color:var(--destructive)">Превышение на ${Math.abs(tdee - eaten)} ккал</span>`}
                    </p>
                </div>

                <div class="section-header">Вода</div>
                <div class="card">
                    <div class="card-header">
                        <span class="card-title">💧 ${Math.round(waterSummary.total_ml)} мл</span>
                        <span class="card-subtitle">${waterSummary.glasses} стаканов / 8</span>
                    </div>
                    <div class="progress-container">
                        <div class="progress-bar">
                            <div class="progress-fill" style="width:${Math.min(100, (waterSummary.total_ml / 2000) * 100)}%;background:#64b5f6"></div>
                        </div>
                    </div>
                    <div style="display:flex;gap:6px;margin-top:10px;flex-wrap:wrap">
                        <button class="btn btn-secondary btn-sm" onclick="HomePage.addWater(150)">150 мл</button>
                        <button class="btn btn-secondary btn-sm" onclick="HomePage.addWater(250)">250 мл</button>
                        <button class="btn btn-secondary btn-sm" onclick="HomePage.addWater(330)">330 мл</button>
                        <button class="btn btn-secondary btn-sm" onclick="HomePage.addWater(500)">500 мл</button>
                    </div>
                </div>

                <div class="section-header">Калории за неделю</div>
                <div class="card">
                    <div class="chart-container" style="height:220px">
                        <canvas id="home-calories-chart"></canvas>
                    </div>
                </div>

                <div class="section-header">Вода за неделю</div>
                <div class="card">
                    <div class="chart-container" style="height:180px">
                        <canvas id="home-water-chart"></canvas>
                    </div>
                </div>
            `;

            Charts.caloriesWithGoals('home-calories-chart', caloriesData, bmr, tdee);
            if (waterStats.length > 0) {
                Charts.waterBar('home-water-chart', waterStats, 2000);
            }
        } catch (e) {
            page.innerHTML = `
                <div class="empty-state" style="padding-top:80px">
                    <p style="font-size:18px;font-weight:600;margin-bottom:8px">Добро пожаловать в TrackHub!</p>
                    <p style="font-size:14px;color:var(--hint);margin-bottom:24px">Для начала заполните свой профиль</p>
                    <button class="btn btn-primary" style="max-width:280px;margin:0 auto" onclick="App.navigate('profile'); setTimeout(() => ProfilePage.showEditModal(), 300)">Заполнить профиль</button>
                </div>
            `;
        }
    },

    async addWater(ml) {
        TG.haptic('impact', 'light');
        try {
            await API.post('/water', { amount_ml: ml });
            App.showToast(`+${ml} мл воды`);
            App.refresh();
        } catch (e) {
            App.showToast('Ошибка: ' + e.message);
        }
    },
};
