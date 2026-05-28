(function () {
    let preference = 'meat_heavy';
    let currentResult = null;

    const startPanel = document.getElementById('blindStartPanel');
    const resultPanel = document.getElementById('blindResultPanel');
    const loadingOverlay = document.getElementById('loadingOverlay');
    const recipeResultArea = document.getElementById('recipeResultArea');

    document.querySelectorAll('.pref-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.pref-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            preference = btn.dataset.pref;
        });
    });

    document.getElementById('startRandomBtn').addEventListener('click', () => randomPick());
    document.getElementById('reselectBtn').addEventListener('click', () => {
        resultPanel.hidden = true;
        startPanel.hidden = false;
        recipeResultArea.innerHTML = '';
    });
    document.getElementById('generateRecipeBtn').addEventListener('click', () => generateRecipe());

    async function randomPick() {
        showLoading('正在随机抽取…');
        try {
            const res = await fetch('/api/modules/blind-box/random?preference=' + preference, { method: 'POST' });
            const json = await res.json();
            if (!json.success || !json.data) {
                alert(json.message || '随机失败');
                return;
            }
            currentResult = json.data;
            renderResult(currentResult);
            startPanel.hidden = true;
            resultPanel.hidden = false;
        } catch (e) {
            alert('请求失败');
        } finally {
            hideLoading();
        }
    }

    function renderResult(data) {
        document.getElementById('ingredientCount').textContent = data.ingredients.length;
        document.getElementById('ingredientResultGrid').innerHTML = data.ingredients
            .map(name => `<div class="item">${name}</div>`).join('');
        document.getElementById('chefEmoji').textContent = data.cuisineEmoji || '👨‍🍳';
        document.getElementById('chefName').textContent = data.chefTitle || (data.cuisineName + '大师');
        document.getElementById('chefDesc').textContent = data.chefDesc || '';
    }

    async function generateRecipe() {
        if (!currentResult) return;
        showLoading('AI 厨神正在创作…');
        try {
            const res = await fetch('/api/recipe/generate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    ingredients: currentResult.ingredients,
                    cuisineCode: currentResult.cuisineCode,
                    customRequire: '美食盲盒推荐组合'
                })
            });
            const json = await res.json();
            if (!json.success || !json.data?.sessionId) {
                alert(json.message || '启动失败');
                return;
            }
            const recipe = await pollRecipe(json.data.sessionId);
            if (recipe) {
                renderRecipe(recipe);
                resultPanel.scrollIntoView({ behavior: 'smooth' });
            }
        } catch (e) {
            alert(e.message || '生成失败');
        } finally {
            hideLoading();
        }
    }

    async function pollRecipe(sessionId) {
        const maxWait = 10 * 60 * 1000;
        const start = Date.now();
        while (Date.now() - start < maxWait) {
            const res = await fetch('/api/agent/session/' + sessionId);
            const json = await res.json();
            const state = json.data || {};
            if (state.status === 'completed') {
                const r = await fetch('/api/recipe/' + state.recipeId);
                const recipeJson = await r.json();
                return recipeJson.success ? recipeJson.data : null;
            }
            if (state.status === 'failed') throw new Error(state.error || '失败');
            await new Promise(r => setTimeout(r, 2000));
        }
        throw new Error('超时');
    }

    function renderRecipe(recipe) {
        const nutrition = recipe.nutrition || {};
        recipeResultArea.innerHTML = `
            <section class="card module-card">
                <div class="card-body recipe-result">
                    <div class="recipe-header">
                        <h2>${recipe.title}</h2>
                        <div class="meta-tags">
                            <span class="meta-tag">${recipe.cuisineName} 风味</span>
                            <span class="meta-tag">${recipe.difficulty}</span>
                            <span class="meta-tag">${recipe.cookingTime}分钟</span>
                        </div>
                    </div>
                    ${recipe.imageUrl ? `<img class="dish-image" src="${recipe.imageUrl}" alt="效果图" onerror="this.src='/images/placeholder-dish.svg?v=2'">` : ''}
                    <p class="hint">${recipe.summary || ''}</p>
                    <a class="detail-link" href="/recipe/${recipe.id}">查看完整菜谱 →</a>
                </div>
            </section>`;
    }

    function showLoading(text) {
        document.getElementById('loadingText').textContent = text;
        loadingOverlay.hidden = false;
    }
    function hideLoading() { loadingOverlay.hidden = true; }
})();
