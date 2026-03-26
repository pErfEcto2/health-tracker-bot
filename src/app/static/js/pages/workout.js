// Workout page
const WorkoutPage = {
    currentDate: new Date().toISOString().split('T')[0],
    exercises: [],
    chartPeriod: 'month',

    async render() {
        const page = document.getElementById('page-workout');
        page.innerHTML = '<div class="loading">Загрузка...</div>';

        try {
            const [workouts, exercises] = await Promise.all([
                API.get(`/workouts?date=${this.currentDate}`),
                API.get('/exercises'),
            ]);
            this.exercises = exercises;

            page.innerHTML = `
                <div class="section-header">Тренировки — ${this.formatDate(this.currentDate)}</div>

                <div class="card">
                    <div class="card-header">
                        <span class="card-title">Сегодня</span>
                        <button class="btn btn-secondary btn-sm" onclick="WorkoutPage.showNewWorkoutModal()">+ Новая</button>
                    </div>
                    ${workouts.length ? workouts.map(w => `
                        <div class="workout-item">
                            <div class="workout-item-header">
                                <h4>${w.notes || 'Тренировка'} <span class="card-subtitle">${this.formatTime(w.started_at)}</span></h4>
                                <div>
                                    <button class="btn btn-secondary btn-sm" onclick="WorkoutPage.showAddSetModal('${w.id}')">+ Подход</button>
                                    <button class="btn-danger" onclick="WorkoutPage.deleteWorkout('${w.id}')">&times;</button>
                                </div>
                            </div>
                            ${w.sets.length ? w.sets.map(s => `
                                <div class="workout-set">
                                    <span>${s.exercise_name || 'Упражнение'}</span>
                                    <span>${s.set_number} подход</span>
                                    <span>${s.reps} повт × ${s.weight_kg} кг</span>
                                </div>
                            `).join('') : '<p class="card-subtitle" style="padding:8px 0">Нет подходов</p>'}
                        </div>
                    `).join('') : '<div class="empty-state"><p>Нет тренировок за этот день</p></div>'}
                </div>

                <div class="section-header">Статистика</div>
                <div class="period-selector">
                    ${['week', 'month', '3months'].map(p => `
                        <button class="period-btn ${this.chartPeriod === p ? 'active' : ''}" onclick="WorkoutPage.setChartPeriod('${p}')">${this.periodLabel(p)}</button>
                    `).join('')}
                </div>
                <div class="card">
                    <div class="chart-container"><canvas id="workout-freq-chart"></canvas></div>
                </div>
                ${exercises.length ? `
                <div class="card">
                    <div class="card-header">
                        <span class="card-title">Прогресс по упражнению</span>
                    </div>
                    <div class="form-group">
                        <select id="exercise-progress-select" onchange="WorkoutPage.loadExerciseProgress()">
                            ${exercises.map(e => `<option value="${e.id}">${e.name}</option>`).join('')}
                        </select>
                    </div>
                    <div class="chart-container"><canvas id="exercise-progress-chart"></canvas></div>
                </div>` : ''}
            `;

            this.loadCharts();
        } catch (e) {
            page.innerHTML = '<div class="empty-state"><p>Ошибка загрузки</p></div>';
        }
    },

    async loadCharts() {
        const freq = await API.get(`/stats/workouts?period=${this.chartPeriod}`);
        if (freq.length) Charts.workoutFrequencyBar('workout-freq-chart', freq);
        this.loadExerciseProgress();
    },

    async loadExerciseProgress() {
        const sel = document.getElementById('exercise-progress-select');
        if (!sel) return;
        const data = await API.get(`/stats/exercise/${sel.value}?period=${this.chartPeriod}`);
        if (data.length) Charts.exerciseProgressLine('exercise-progress-chart', data);
    },

    setChartPeriod(p) {
        this.chartPeriod = p;
        TG.haptic('selection');
        App.refresh();
    },

    showNewWorkoutModal() {
        TG.haptic('impact', 'light');
        App.openModal('Новая тренировка', `
            <div class="form-group"><label>Заметка (опционально)</label><input id="workout-notes" placeholder="Ноги, кардио..."></div>
            <button class="btn btn-primary" onclick="WorkoutPage.submitNewWorkout()">Создать</button>
        `);
    },

    async submitNewWorkout() {
        const notes = document.getElementById('workout-notes').value.trim() || null;
        try {
            await API.post('/workouts', { notes });
            TG.haptic('notification', 'success');
            App.closeModal();
            App.showToast('Тренировка создана');
            App.refresh();
        } catch (e) {
            App.showToast('Ошибка: ' + e.message);
        }
    },

    showAddSetModal(workoutId) {
        TG.haptic('impact', 'light');
        if (!this.exercises.length) return;
        App.openModal('Добавить подход', `
            <div class="form-group"><label>Упражнение</label>
                <select id="set-exercise">${this.exercises.map(e => `<option value="${e.id}">${e.name} (${e.muscle_group})</option>`).join('')}</select>
            </div>
            <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:8px">
                <div class="form-group"><label>Подход №</label><input id="set-num" type="number" value="1" min="1"></div>
                <div class="form-group"><label>Повторы</label><input id="set-reps" type="number" placeholder="10"></div>
                <div class="form-group"><label>Вес (кг)</label><input id="set-weight" type="number" placeholder="0"></div>
            </div>
            <button class="btn btn-primary" onclick="WorkoutPage.submitAddSet('${workoutId}')">Добавить</button>
        `);
    },

    async submitAddSet(workoutId) {
        const data = {
            exercise_id: document.getElementById('set-exercise').value,
            set_number: parseInt(document.getElementById('set-num').value) || 1,
            reps: parseInt(document.getElementById('set-reps').value) || 0,
            weight_kg: parseFloat(document.getElementById('set-weight').value) || 0,
        };
        try {
            await API.post(`/workouts/${workoutId}/sets`, data);
            TG.haptic('notification', 'success');
            App.closeModal();
            App.showToast('Подход добавлен');
            App.refresh();
        } catch (e) {
            App.showToast('Ошибка: ' + e.message);
        }
    },

    async deleteWorkout(id) {
        TG.haptic('impact', 'medium');
        try {
            await API.del(`/workouts/${id}`);
            App.showToast('Удалено');
            App.refresh();
        } catch (e) {
            App.showToast('Ошибка: ' + e.message);
        }
    },

    formatDate(d) {
        return new Date(d).toLocaleDateString('ru-RU', { day: 'numeric', month: 'long' });
    },
    formatTime(dt) {
        return new Date(dt).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
    },
    periodLabel(p) {
        return { week: 'Неделя', month: 'Месяц', '3months': '3 мес' }[p] || p;
    },
};
