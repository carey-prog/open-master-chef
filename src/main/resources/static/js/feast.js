(function () {
    const state = {
        mode: 'fixed',
        dishCount: 6,
        smartDishes: [],
        fixedDishes: [],
        tastePrefs: [],
        cuisineStyle: '混合菜系',
        diningScene: '家庭聚餐',
        nutritionPref: '营养均衡',
        specialRequire: ''
    };

    const loadingOverlay = document.getElementById('loadingOverlay');
    const fixedPanel = document.getElementById('fixedCountPanel');
    const smartPanel = document.getElementById('smartDishPanel');
    const fixedDishPanel = document.getElementById('fixedDishPanel');
    const feastWarn = document.getElementById('feastWarn');
    const generateBtn = document.getElementById('generateFeastBtn');

    // 模式切换
    document.querySelectorAll('.mode-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.mode-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            state.mode = btn.dataset.mode;
            const isSmart = state.mode === 'smart';
            fixedPanel.hidden = isSmart;
            fixedDishPanel.hidden = isSmart;
            smartPanel.hidden = !isSmart;
            updateSummary();
            updateGenerateBtn();
        });
    });

    // 数量选择
    document.querySelectorAll('.count-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.count-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            state.dishCount = parseInt(btn.dataset.count, 10);
            document.getElementById('customCount').value = state.dishCount;
            updateSummary();
        });
    });
    document.getElementById('customCount').addEventListener('change', e => {
        state.dishCount = Math.min(12, Math.max(2, parseInt(e.target.value, 10) || 6));
        e.target.value = state.dishCount;
        document.querySelectorAll('.count-btn').forEach(b => b.classList.remove('active'));
        updateSummary();
    });

    // 智能模式菜品
    setupDishInput('smartDishInput', 'addSmartDishBtn', 'smartDishTags', 'smartDishCount', state.smartDishes);
    setupDishInput('fixedDishInput', 'addFixedDishBtn', 'fixedDishTags', null, state.fixedDishes);

    // 多选/单选标签
    setupMultiSelect('tastePrefs', state.tastePrefs);
    setupSingleSelect('cuisineStyle', v => { state.cuisineStyle = v; updateSummary(); });
    setupSingleSelect('diningScene', v => { state.diningScene = v; updateSummary(); });
    setupSingleSelect('nutritionPref', v => { state.nutritionPref = v; updateSummary(); });

    const specialInput = document.getElementById('specialRequire');
    specialInput.addEventListener('input', () => {
        state.specialRequire = specialInput.value;
        document.getElementById('specialCount').textContent = specialInput.value.length;
    });

    generateBtn.addEventListener('click', generateFeast);

    function setupDishInput(inputId, btnId, tagsId, countId, list) {
        const input = document.getElementById(inputId);
        const add = () => {
            const name = input.value.trim();
            if (!name || list.includes(name) || list.length >= 10) return;
            list.push(name);
            input.value = '';
            renderTags(tagsId, list, countId);
            updateGenerateBtn();
        };
        document.getElementById(btnId).addEventListener('click', add);
        input.addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); add(); } });
    }

    function renderTags(tagsId, list, countId) {
        const el = document.getElementById(tagsId);
        el.innerHTML = list.map((name, i) =>
            `<span class="selected-tag">${name}<button type="button" data-idx="${i}">×</button></span>`
        ).join('');
        el.querySelectorAll('button').forEach(btn => {
            btn.addEventListener('click', () => {
                list.splice(Number(btn.dataset.idx), 1);
                renderTags(tagsId, list, countId);
                updateGenerateBtn();
            });
        });
        if (countId) document.getElementById(countId).textContent = list.length + '/10';
    }

    function setupMultiSelect(containerId, list) {
        document.querySelectorAll('#' + containerId + ' .tag-select-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                btn.classList.toggle('active');
                const val = btn.dataset.value;
                if (btn.classList.contains('active')) {
                    if (!list.includes(val)) list.push(val);
                } else {
                    const idx = list.indexOf(val);
                    if (idx >= 0) list.splice(idx, 1);
                }
                updateSummary();
            });
        });
    }

    function setupSingleSelect(containerId, setter) {
        document.querySelectorAll('#' + containerId + ' .tag-select-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                document.querySelectorAll('#' + containerId + ' .tag-select-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                setter(btn.dataset.value);
            });
        });
    }

    function updateSummary() {
        const modeText = state.mode === 'smart' ? '✨ 智能搭配' : '🎯 固定数量';
        document.getElementById('feastConfigSummary').textContent =
            `生成模式：${modeText} · 菜品数量：${state.dishCount} 道`;
    }

    function updateGenerateBtn() {
        if (state.mode === 'smart' && state.smartDishes.length === 0) {
            generateBtn.disabled = true;
            feastWarn.hidden = false;
        } else {
            generateBtn.disabled = false;
            feastWarn.hidden = true;
        }
    }

    async function generateFeast() {
        showLoading('AI 正在设计菜单…');
        try {
            const body = {
                mode: state.mode,
                dishCount: state.dishCount,
                specifiedDishes: state.mode === 'smart' ? state.smartDishes : state.fixedDishes,
                tastePrefs: state.tastePrefs,
                cuisineStyle: state.cuisineStyle,
                diningScene: state.diningScene,
                nutritionPref: state.nutritionPref,
                specialRequire: state.specialRequire
            };
            const res = await fetch('/api/modules/feast/generate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });
            const json = await res.json();
            if (!json.success || !json.data) {
                alert(json.message || '生成失败');
                return;
            }
            renderFeastResult(json.data);
        } catch (e) {
            alert(e.message || '请求失败');
        } finally {
            hideLoading();
        }
    }

    function renderFeastResult(menu) {
        const panel = document.getElementById('feastResultPanel');
        const body = document.getElementById('feastResultBody');
        const dishesHtml = (menu.dishes || []).map((d, i) => `
            <li class="feast-dish-item">
                <span class="feast-dish-num">${i + 1}</span>
                <div>
                    <strong>${d.name}</strong>
                    <span class="meta-tag" style="margin-left:8px">${d.type || '菜品'}</span>
                    <span class="meta-tag">${d.difficulty || '中等'}</span>
                    <p class="sub-hint">${d.brief || ''}</p>
                </div>
            </li>`).join('');
        body.innerHTML = `
            <h2>🍽️ ${menu.menuTitle || '您的专属菜单'}</h2>
            <p class="hint">${menu.summary || ''}</p>
            <ol class="feast-dish-list">${dishesHtml}</ol>`;
        panel.hidden = false;
        panel.scrollIntoView({ behavior: 'smooth' });
    }

    function showLoading(text) {
        document.getElementById('loadingText').textContent = text;
        loadingOverlay.hidden = false;
    }
    function hideLoading() { loadingOverlay.hidden = true; }

    updateSummary();
    updateGenerateBtn();
})();
