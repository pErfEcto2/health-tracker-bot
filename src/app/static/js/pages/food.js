function _localDate() {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
}

// Food page — daily log + charts
const FoodPage = {
    currentDate: _localDate(),
    chartPeriod: 'week',

    async render() {
        const page = document.getElementById('page-food');
        page.innerHTML = '<div class="loading">Загрузка...</div>';

        try {
            const [entries, summary] = await Promise.all([
                API.get(`/food?date=${this.currentDate}`),
                API.get(`/food/summary?date=${this.currentDate}`),
            ]);

            page.innerHTML = `
                <div class="section-header">Питание — ${this.formatDate(this.currentDate)}</div>

                <div class="card">
                    <div class="card-header">
                        <span class="card-title">Итого за день</span>
                        <span class="food-item-calories">${Math.round(summary.total_calories)} ккал</span>
                    </div>
                    <div class="macros-row">
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
                </div>

                ${summary.total_protein_g + summary.total_fat_g + summary.total_carbs_g > 0 ? `
                <div class="card">
                    <div class="chart-container" style="height:160px">
                        <canvas id="food-macros-doughnut"></canvas>
                    </div>
                </div>` : ''}

                <div class="card">
                    <div class="card-header">
                        <span class="card-title">Записи</span>
                        <button class="btn btn-secondary btn-sm" onclick="FoodPage.showAddModal()">+ Добавить</button>
                    </div>
                    ${entries.length ? entries.map(e => `
                        <div class="food-item">
                            <div class="food-item-info">
                                <h4>${this.escape(e.name)}</h4>
                                <p>${this.mealLabel(e.meal_type)} · ${e.amount_g}г · Б${Math.round(e.protein_g)} Ж${Math.round(e.fat_g)} У${Math.round(e.carbs_g)}</p>
                            </div>
                            <div style="display:flex;align-items:center;gap:8px">
                                <span class="food-item-calories">${Math.round(e.calories)}</span>
                                <button class="btn-danger" onclick="FoodPage.deleteEntry('${e.id}')">&times;</button>
                            </div>
                        </div>
                    `).join('') : '<div class="empty-state"><p>Нет записей за этот день</p></div>'}
                </div>

                <div class="section-header">Статистика</div>
                <div class="period-selector">
                    ${['week', 'month', '3months'].map(p => `
                        <button class="period-btn ${this.chartPeriod === p ? 'active' : ''}" onclick="FoodPage.setPeriod('${p}')">${this.periodLabel(p)}</button>
                    `).join('')}
                </div>
                <div class="card">
                    <div class="chart-container"><canvas id="food-calories-line"></canvas></div>
                </div>
                <div class="card">
                    <div class="chart-container"><canvas id="food-macros-stacked"></canvas></div>
                </div>
            `;

            if (summary.total_protein_g + summary.total_fat_g + summary.total_carbs_g > 0) {
                Charts.macrosDoughnut('food-macros-doughnut', summary.total_protein_g, summary.total_fat_g, summary.total_carbs_g);
            }

            this.loadCharts();
        } catch (e) {
            page.innerHTML = '<div class="empty-state"><p>Ошибка загрузки</p></div>';
        }
    },

    async loadCharts() {
        const [cal, macros] = await Promise.all([
            API.get(`/stats/calories?period=${this.chartPeriod}`),
            API.get(`/stats/macros?period=${this.chartPeriod}`),
        ]);
        if (cal.length) Charts.caloriesLine('food-calories-line', cal);
        if (macros.length) Charts.macrosStackedBar('food-macros-stacked', macros);
    },

    setPeriod(p) {
        this.chartPeriod = p;
        TG.haptic('selection');
        App.refresh();
    },

    inputMode: 'search',
    searchTimer: null,

    showAddModal() {
        TG.haptic('impact', 'light');
        this.inputMode = 'search';
        this._renderAddModal();
    },

    _renderAddModal() {
        const modes = [
            { id: 'search', label: 'Поиск' },
            { id: 'full', label: 'Вручную' },
            { id: 'quick', label: 'Быстро' },
        ];

        const modeButtons = modes.map(m =>
            `<button class="period-btn ${this.inputMode === m.id ? 'active' : ''}" onclick="FoodPage.switchMode('${m.id}')">${m.label}</button>`
        ).join('');

        let fields = '';

        if (this.inputMode === 'search') {
            fields = `
                <div class="form-group"><label>Найти продукт</label><input id="food-search-input" placeholder="молоко, рис, курица..." oninput="FoodPage.onSearch(this.value)"></div>
                <div id="food-search-results"></div>
                <div id="food-search-form" style="display:none">
                    <div class="form-group"><label>Название</label><input id="food-name" readonly></div>
                    <div class="card-subtitle" id="food-per100" style="margin-bottom:8px"></div>
                    <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px">
                        <div class="form-group"><label>Масса (г)</label><input id="food-amount" type="number" inputmode="decimal" value="100" oninput="FoodPage.recalc()"></div>
                        <div class="form-group"><label>Приём</label>
                            <select id="food-meal">
                                <option value="breakfast">Завтрак</option><option value="lunch">Обед</option>
                                <option value="dinner">Ужин</option><option value="snack" selected>Перекус</option>
                            </select>
                        </div>
                    </div>
                    <div style="display:grid;grid-template-columns:1fr 1fr 1fr 1fr;gap:6px;margin-bottom:14px">
                        <div class="macro-chip"><div class="value" id="food-calc-cal">0</div><div class="label">ккал</div></div>
                        <div class="macro-chip protein"><div class="value" id="food-calc-p">0</div><div class="label">Б</div></div>
                        <div class="macro-chip fat"><div class="value" id="food-calc-f">0</div><div class="label">Ж</div></div>
                        <div class="macro-chip carbs"><div class="value" id="food-calc-c">0</div><div class="label">У</div></div>
                    </div>
                    <input type="hidden" id="food-cal"><input type="hidden" id="food-protein"><input type="hidden" id="food-fat"><input type="hidden" id="food-carbs">
                    <button class="btn btn-primary" onclick="FoodPage.submitAdd()">Сохранить</button>
                </div>
            `;
        } else if (this.inputMode === 'full') {
            fields = `
                <div class="form-group"><label>Название</label><input id="food-name" placeholder="Куриная грудка"></div>
                <div class="form-group"><label>Калории (ккал)</label><input id="food-cal" type="number" inputmode="decimal" placeholder="165"></div>
                <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:8px">
                    <div class="form-group"><label>Белки (г)</label><input id="food-protein" type="number" inputmode="decimal" placeholder="31"></div>
                    <div class="form-group"><label>Жиры (г)</label><input id="food-fat" type="number" inputmode="decimal" placeholder="3.6"></div>
                    <div class="form-group"><label>Углеводы (г)</label><input id="food-carbs" type="number" inputmode="decimal" placeholder="0"></div>
                </div>
                <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px">
                    <div class="form-group"><label>Масса (г)</label><input id="food-amount" type="number" inputmode="decimal" placeholder="100"></div>
                    <div class="form-group"><label>Приём</label>
                        <select id="food-meal">
                            <option value="breakfast">Завтрак</option><option value="lunch">Обед</option>
                            <option value="dinner">Ужин</option><option value="snack" selected>Перекус</option>
                        </select>
                    </div>
                </div>
                <button class="btn btn-primary" onclick="FoodPage.submitAdd()">Сохранить</button>
            `;
        } else if (this.inputMode === 'quick') {
            fields = `
                <div class="form-group"><label>Название</label><input id="food-name" placeholder="Пицца, кофе с молоком..."></div>
                <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px">
                    <div class="form-group"><label>Калории (ккал)</label><input id="food-cal" type="number" inputmode="decimal" placeholder="350"></div>
                    <div class="form-group"><label>Приём</label>
                        <select id="food-meal">
                            <option value="breakfast">Завтрак</option><option value="lunch">Обед</option>
                            <option value="dinner">Ужин</option><option value="snack" selected>Перекус</option>
                        </select>
                    </div>
                </div>
                <div class="form-group"><label>Масса (г, необязательно)</label><input id="food-amount" type="number" inputmode="decimal" placeholder="100"></div>
                <p style="font-size:12px;color:var(--hint);margin-bottom:12px">Не знаете состав — введите только калории</p>
                <button class="btn btn-primary" onclick="FoodPage.submitAdd()">Сохранить</button>
            `;
        }

        App.openModal('Добавить еду', `
            <div class="period-selector" style="margin-bottom:16px">${modeButtons}</div>
            ${fields}
        `);
    },

    switchMode(mode) {
        this.inputMode = mode;
        TG.haptic('selection');
        this._renderAddModal();
    },

    // --- Search ---
    selectedProduct: null,

    onSearch(query) {
        clearTimeout(this.searchTimer);
        if (query.length < 2) {
            document.getElementById('food-search-results').innerHTML = '';
            document.getElementById('food-search-form').style.display = 'none';
            return;
        }
        this.searchTimer = setTimeout(() => this._doSearch(query), 400);
    },

    _searchResults: [],

    async _doSearch(query) {
        const resultsEl = document.getElementById('food-search-results');
        resultsEl.innerHTML = '<div class="loading">Поиск...</div>';
        try {
            const results = await API.get(`/food-search?q=${encodeURIComponent(query)}`);
            this._searchResults = results;
            if (!results.length) {
                resultsEl.innerHTML = '<p style="font-size:13px;color:var(--hint);padding:8px 0">Ничего не найдено. Попробуйте на английском или используйте ручной ввод.</p>';
                return;
            }
            resultsEl.innerHTML = results.map((r, i) => `
                <div class="food-item" style="cursor:pointer" onclick="FoodPage.selectProduct(${i})">
                    <div class="food-item-info">
                        <h4>${this.escape(r.name)}</h4>
                        <p>на 100г: Б${r.protein_per_100g} Ж${r.fat_per_100g} У${r.carbs_per_100g}</p>
                    </div>
                    <span class="food-item-calories">${Math.round(r.calories_per_100g)}</span>
                </div>
            `).join('');
        } catch {
            resultsEl.innerHTML = '<p style="font-size:13px;color:var(--hint)">Ошибка поиска</p>';
        }
    },

    selectProduct(indexOrProduct) {
        const product = typeof indexOrProduct === 'number' ? this._searchResults[indexOrProduct] : indexOrProduct;
        if (!product) return;
        TG.haptic('selection');
        this.selectedProduct = product;
        document.getElementById('food-search-results').innerHTML = '';
        document.getElementById('food-search-form').style.display = 'block';
        document.getElementById('food-name').value = product.name;
        document.getElementById('food-per100').textContent = `На 100г: ${product.calories_per_100g} ккал · Б${product.protein_per_100g} Ж${product.fat_per_100g} У${product.carbs_per_100g}`;
        this.recalc();
    },

    recalc() {
        const p = this.selectedProduct;
        if (!p) return;
        const amount = parseFloat(document.getElementById('food-amount')?.value) || 0;
        const ratio = amount / 100;
        const cal = Math.round(p.calories_per_100g * ratio);
        const pr = Math.round(p.protein_per_100g * ratio * 10) / 10;
        const fa = Math.round(p.fat_per_100g * ratio * 10) / 10;
        const ca = Math.round(p.carbs_per_100g * ratio * 10) / 10;
        document.getElementById('food-calc-cal').textContent = cal;
        document.getElementById('food-calc-p').textContent = pr;
        document.getElementById('food-calc-f').textContent = fa;
        document.getElementById('food-calc-c').textContent = ca;
        document.getElementById('food-cal').value = cal;
        document.getElementById('food-protein').value = pr;
        document.getElementById('food-fat').value = fa;
        document.getElementById('food-carbs').value = ca;
    },

    async submitAdd() {
        const name = document.getElementById('food-name')?.value.trim();
        if (!name) {
            App.showToast('Введите название');
            return;
        }

        const data = {
            name,
            calories: parseFloat(document.getElementById('food-cal')?.value) || 0,
            protein_g: parseFloat(document.getElementById('food-protein')?.value) || 0,
            fat_g: parseFloat(document.getElementById('food-fat')?.value) || 0,
            carbs_g: parseFloat(document.getElementById('food-carbs')?.value) || 0,
            amount_g: parseFloat(document.getElementById('food-amount')?.value) || 0,
            meal_type: document.getElementById('food-meal')?.value || 'snack',
        };

        if (data.calories === 0 && data.amount_g === 0 && data.protein_g === 0) {
            App.showToast('Укажите хотя бы калории или массу');
            return;
        }

        try {
            await API.post('/food', data);
            TG.haptic('notification', 'success');
            App.closeModal();
            App.showToast('Запись добавлена');
            App.refresh();
        } catch (e) {
            App.showToast('Ошибка: ' + e.message);
        }
    },

    async deleteEntry(id) {
        TG.haptic('impact', 'medium');
        try {
            await API.del(`/food/${id}`);
            App.showToast('Удалено');
            App.refresh();
        } catch (e) {
            App.showToast('Ошибка: ' + e.message);
        }
    },

    mealLabel(t) {
        return { breakfast: 'Завтрак', lunch: 'Обед', dinner: 'Ужин', snack: 'Перекус' }[t] || t;
    },
    periodLabel(p) {
        return { week: 'Неделя', month: 'Месяц', '3months': '3 мес' }[p] || p;
    },
    formatDate(d) {
        const dt = new Date(d);
        return dt.toLocaleDateString('ru-RU', { day: 'numeric', month: 'long' });
    },
    escape(s) {
        const d = document.createElement('div');
        d.textContent = s;
        return d.innerHTML;
    },
};
