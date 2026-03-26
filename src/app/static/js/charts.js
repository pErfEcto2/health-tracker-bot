// Chart.js helper — uses Telegram theme colors
const Charts = {
    instances: {},

    getColors() {
        const s = getComputedStyle(document.documentElement);
        return {
            text: s.getPropertyValue('--text').trim() || '#000',
            hint: s.getPropertyValue('--hint').trim() || '#999',
            grid: s.getPropertyValue('--secondary-bg').trim() || '#f0f0f0',
            calories: s.getPropertyValue('--chart-calories').trim() || '#ff6b6b',
            protein: s.getPropertyValue('--chart-protein').trim() || '#4ecdc4',
            fat: s.getPropertyValue('--chart-fat').trim() || '#ffe66d',
            carbs: s.getPropertyValue('--chart-carbs').trim() || '#95e1d3',
            weight: s.getPropertyValue('--chart-weight').trim() || '#2678b6',
            workout: s.getPropertyValue('--chart-workout').trim() || '#2678b6',
        };
    },

    baseOptions(title = '') {
        const c = this.getColors();
        return {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                title: title ? {
                    display: true, text: title,
                    color: c.text, font: { size: 13, weight: '600' },
                    padding: { bottom: 8 },
                } : { display: false },
            },
            scales: {
                x: { ticks: { color: c.hint, font: { size: 10 } }, grid: { display: false } },
                y: { ticks: { color: c.hint, font: { size: 10 } }, grid: { color: c.grid + '40' }, beginAtZero: true },
            },
        };
    },

    /** Default bar dataset config for fixed-width bars */
    barDefaults() {
        return { maxBarThickness: 28, borderRadius: 4 };
    },

    destroy(id) {
        if (this.instances[id]) {
            this.instances[id].destroy();
            delete this.instances[id];
        }
    },

    create(canvasId, config) {
        this.destroy(canvasId);
        const ctx = document.getElementById(canvasId);
        if (!ctx) return null;
        this.instances[canvasId] = new Chart(ctx, config);
        return this.instances[canvasId];
    },

    // --- Prebuilt chart factories ---

    caloriesWithGoals(canvasId, data, bmr, tdee) {
        const c = this.getColors();
        const labels = data.map(d => this.shortDate(d.date));
        const values = data.map(d => d.calories);
        const len = labels.length || 1;

        const datasets = [
            {
                type: 'bar',
                label: 'Калории',
                data: values,
                backgroundColor: values.map(v => v > tdee ? c.calories + 'cc' : c.calories + '80'),
                ...this.barDefaults(),
                order: 2,
            },
        ];

        if (tdee > 0) {
            datasets.push({
                type: 'line',
                label: 'TDEE',
                data: Array(len).fill(tdee),
                borderColor: c.weight,
                borderWidth: 2,
                borderDash: [6, 3],
                pointRadius: 0,
                fill: false,
                order: 1,
            });
        }
        if (bmr > 0) {
            datasets.push({
                type: 'line',
                label: 'BMR',
                data: Array(len).fill(bmr),
                borderColor: c.hint,
                borderWidth: 1.5,
                borderDash: [3, 3],
                pointRadius: 0,
                fill: false,
                order: 0,
            });
        }

        return this.create(canvasId, {
            type: 'bar',
            data: { labels, datasets },
            options: {
                ...this.baseOptions('Калории за неделю'),
                plugins: {
                    ...this.baseOptions().plugins,
                    legend: {
                        display: true,
                        position: 'bottom',
                        labels: { color: c.text, font: { size: 10 }, boxWidth: 14, padding: 8 },
                    },
                },
            },
        });
    },

    caloriesLine(canvasId, data) {
        const c = this.getColors();
        return this.create(canvasId, {
            type: 'line',
            data: {
                labels: data.map(d => this.shortDate(d.date)),
                datasets: [{
                    data: data.map(d => d.calories),
                    borderColor: c.calories,
                    backgroundColor: c.calories + '20',
                    fill: true, tension: 0.3, pointRadius: 2,
                }],
            },
            options: this.baseOptions('Калории за период'),
        });
    },

    macrosDoughnut(canvasId, protein, fat, carbs) {
        const c = this.getColors();
        return this.create(canvasId, {
            type: 'doughnut',
            data: {
                labels: ['Белки', 'Жиры', 'Углеводы'],
                datasets: [{
                    data: [protein, fat, carbs],
                    backgroundColor: [c.protein, c.fat, c.carbs],
                    borderWidth: 0,
                }],
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: 'bottom', labels: { color: this.getColors().text, font: { size: 11 } } } },
                cutout: '65%',
            },
        });
    },

    macrosStackedBar(canvasId, data) {
        const c = this.getColors();
        return this.create(canvasId, {
            type: 'bar',
            data: {
                labels: data.map(d => this.shortDate(d.date)),
                datasets: [
                    { label: 'Белки', data: data.map(d => d.protein), backgroundColor: c.protein, ...this.barDefaults() },
                    { label: 'Жиры', data: data.map(d => d.fat), backgroundColor: c.fat, ...this.barDefaults() },
                    { label: 'Углеводы', data: data.map(d => d.carbs), backgroundColor: c.carbs, ...this.barDefaults() },
                ],
            },
            options: {
                ...this.baseOptions('БЖУ за период'),
                plugins: {
                    ...this.baseOptions().plugins,
                    legend: { display: true, position: 'bottom', labels: { color: c.text, font: { size: 10 }, boxWidth: 12 } },
                },
                scales: {
                    ...this.baseOptions().scales,
                    x: { ...this.baseOptions().scales.x, stacked: true },
                    y: { ...this.baseOptions().scales.y, stacked: true },
                },
            },
        });
    },

    workoutFrequencyBar(canvasId, data) {
        const c = this.getColors();
        return this.create(canvasId, {
            type: 'bar',
            data: {
                labels: data.map(d => this.shortDate(d.date)),
                datasets: [{
                    data: data.map(d => d.count),
                    backgroundColor: c.workout + '80',
                    ...this.barDefaults(),
                }],
            },
            options: this.baseOptions('Тренировки'),
        });
    },

    exerciseProgressLine(canvasId, data) {
        const c = this.getColors();
        return this.create(canvasId, {
            type: 'line',
            data: {
                labels: data.map(d => this.shortDate(d.date)),
                datasets: [
                    { label: 'Макс вес', data: data.map(d => d.max_weight), borderColor: c.weight, tension: 0.3, pointRadius: 3, yAxisID: 'y' },
                    { label: 'Объём', data: data.map(d => d.volume), borderColor: c.carbs, tension: 0.3, pointRadius: 3, yAxisID: 'y1' },
                ],
            },
            options: {
                ...this.baseOptions('Прогресс'),
                plugins: {
                    ...this.baseOptions().plugins,
                    legend: { display: true, position: 'bottom', labels: { color: c.text, font: { size: 10 }, boxWidth: 12 } },
                },
                scales: {
                    x: this.baseOptions().scales.x,
                    y: { ...this.baseOptions().scales.y, position: 'left', title: { display: true, text: 'кг', color: c.hint } },
                    y1: { ...this.baseOptions().scales.y, position: 'right', grid: { drawOnChartArea: false }, title: { display: true, text: 'объём', color: c.hint } },
                },
            },
        });
    },

    weightLine(canvasId, data) {
        const c = this.getColors();
        return this.create(canvasId, {
            type: 'line',
            data: {
                labels: data.map(d => this.shortDate(d.date)),
                datasets: [{
                    data: data.map(d => d.weight),
                    borderColor: c.weight,
                    backgroundColor: c.weight + '15',
                    fill: true, tension: 0.3, pointRadius: 3,
                }],
            },
            options: {
                ...this.baseOptions('Вес (кг)'),
                scales: {
                    ...this.baseOptions().scales,
                    y: { ...this.baseOptions().scales.y, beginAtZero: false },
                },
            },
        });
    },

    measurementsMultiLine(canvasId, data) {
        const c = this.getColors();
        const fields = [
            { key: 'waist_cm', label: 'Талия', color: c.calories },
            { key: 'bicep_cm', label: 'Бицепс', color: c.protein },
            { key: 'hip_cm', label: 'Бёдра', color: c.carbs },
            { key: 'chest_cm', label: 'Грудь', color: c.fat },
        ];

        const datasets = fields
            .filter(f => data.some(d => d[f.key] != null))
            .map(f => ({
                label: f.label,
                data: data.map(d => d[f.key]),
                borderColor: f.color,
                tension: 0.3, pointRadius: 3, spanGaps: true,
            }));

        return this.create(canvasId, {
            type: 'line',
            data: { labels: data.map(d => this.shortDate(d.date)), datasets },
            options: {
                ...this.baseOptions('Замеры (см)'),
                plugins: {
                    ...this.baseOptions().plugins,
                    legend: { display: true, position: 'bottom', labels: { color: c.text, font: { size: 10 }, boxWidth: 12 } },
                },
                scales: {
                    ...this.baseOptions().scales,
                    y: { ...this.baseOptions().scales.y, beginAtZero: false },
                },
            },
        });
    },

    waterBar(canvasId, data, goalMl = 2000) {
        const c = this.getColors();
        const labels = data.map(d => this.shortDate(d.date));
        const values = data.map(d => d.total_ml);
        const len = labels.length || 1;

        return this.create(canvasId, {
            type: 'bar',
            data: {
                labels,
                datasets: [
                    {
                        label: 'Вода (мл)',
                        data: values,
                        backgroundColor: '#64b5f6' + '80',
                        ...this.barDefaults(),
                        order: 1,
                    },
                    {
                        type: 'line',
                        label: 'Норма',
                        data: Array(len).fill(goalMl),
                        borderColor: '#1565c0',
                        borderWidth: 1.5,
                        borderDash: [4, 4],
                        pointRadius: 0,
                        fill: false,
                        order: 0,
                    },
                ],
            },
            options: {
                ...this.baseOptions('Вода за неделю'),
                plugins: {
                    ...this.baseOptions().plugins,
                    legend: {
                        display: true,
                        position: 'bottom',
                        labels: { color: c.text, font: { size: 10 }, boxWidth: 14, padding: 8 },
                    },
                },
            },
        });
    },

    shortDate(dateStr) {
        const d = new Date(dateStr);
        return `${d.getDate()}.${String(d.getMonth() + 1).padStart(2, '0')}`;
    },
};
