/**
 * 「厨神 AI」首页前端逻辑（纯原生 JavaScript，无框架依赖）
 *
 * 四步交互流程：
 *   1. 选择/输入食材（最多 10 种，支持 MiMo 拍照识别）
 *   2. 选择菜系大师（中餐 / 国际料理）
 *   3. 点击生成 → POST /api/recipe/generate → 拿到 sessionId
 *   4. 轮询 GET /api/agent/session/{sessionId} → 完成后 GET /api/recipe/{id} 展示结果
 *
 * 后端 Graph 工作流是异步执行的，前端通过轮询感知各节点进度（rag_done → web_search_done → ... → completed）
 */
(function () {
    const MAX = 10;              // 最多选择食材数量
    let selected = [];           // 当前已选食材列表
    let selectedCuisine = null;  // 当前选中的菜系 { code, name }

    const input = document.getElementById('ingredientInput');
    const cameraInput = document.getElementById('cameraInput');
    const selectedTags = document.getElementById('selectedTags');
    const selectedCount = document.getElementById('selectedCount');
    const configIngredients = document.getElementById('configIngredients');
    const configCuisine = document.getElementById('configCuisine');
    const generateBtn = document.getElementById('generateBtn');
    const resultArea = document.getElementById('resultArea');
    const loadingOverlay = document.getElementById('loadingOverlay');
    const customRequire = document.getElementById('customRequire');

    // Graph 各节点完成后写入 Redis 的 status 值 → 前端 loading 文案映射
    const STATUS_TEXT = {
        running: 'AI 厨神正在准备…',
        rag_done: '知识库检索完成，正在联网搜索…',
        web_search_done: '联网搜索完成，正在生成菜谱…',
        recipe_generated: '菜谱已生成，正在分析营养…',
        nutrition_done: '营养分析完成，正在生成配图…',
        image_done: '即将完成，正在保存…'
    };

    function renderSelected() {
        selectedTags.innerHTML = selected.map((name, idx) =>
            `<span class="selected-tag">${name}<button type="button" data-idx="${idx}">×</button></span>`
        ).join('');
        selectedCount.textContent = `已选 ${selected.length}/${MAX}`;
        configIngredients.textContent = selected.length
            ? `🥗 食材：${selected.join('、')}`
            : '🥗 食材：尚未添加';
        updateGenerateBtn();
        syncTagButtons();
    }

    function syncTagButtons() {
        document.querySelectorAll('.tag-btn').forEach(btn => {
            btn.classList.toggle('active', selected.includes(btn.dataset.name));
        });
    }

    function addIngredient(name) {
        name = (name || '').trim();
        if (!name || selected.includes(name)) return;
        if (selected.length >= MAX) {
            alert(`最多添加 ${MAX} 种食材`);
            return;
        }
        selected.push(name);
        renderSelected();
    }

    function removeIngredient(idx) {
        selected.splice(idx, 1);
        renderSelected();
    }

    function updateGenerateBtn() {
        generateBtn.disabled = !(selected.length > 0 && selectedCuisine);
    }

    input.addEventListener('keydown', e => {
        if (e.key === 'Enter') {
            e.preventDefault();
            addIngredient(input.value);
            input.value = '';
        }
    });

    selectedTags.addEventListener('click', e => {
        if (e.target.tagName === 'BUTTON') {
            removeIngredient(Number(e.target.dataset.idx));
        }
    });

    document.querySelectorAll('.tag-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const name = btn.dataset.name;
            if (selected.includes(name)) {
                selected = selected.filter(n => n !== name);
                renderSelected();
            } else {
                addIngredient(name);
            }
        });
    });

    document.querySelectorAll('.cuisine-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.cuisine-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            selectedCuisine = { code: btn.dataset.code, name: btn.textContent.trim() };
            configCuisine.textContent = `👨‍🍳 菜系：${selectedCuisine.name}`;
            updateGenerateBtn();
        });
    });

    // MiMo 拍照识食材：上传图片到 /api/ingredients/recognize，自动添加到已选列表
    cameraInput.addEventListener('change', async () => {
        const file = cameraInput.files[0];
        if (!file) return;
        showLoading('AI 正在识别食材…');
        try {
            const form = new FormData();
            form.append('file', file);
            const res = await fetch('/api/ingredients/recognize', { method: 'POST', body: form });
            const json = await res.json();
            if (json.success && json.data) {
                json.data.forEach(addIngredient);
            } else {
                alert(json.message || '识别失败');
            }
        } catch (e) {
            alert('识别请求失败');
        } finally {
            hideLoading();
            cameraInput.value = '';
        }
    });

    // 核心：发起异步菜谱生成，然后轮询等待结果
    generateBtn.addEventListener('click', async () => {
        showLoading('AI 厨神正在创作…');
        generateBtn.disabled = true;
        try {
            const res = await fetch('/api/recipe/generate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    ingredients: selected,
                    cuisineCode: selectedCuisine.code,
                    customRequire: customRequire.value
                })
            });
            const json = await res.json();
            if (!json.success || !json.data?.sessionId) {
                alert(json.message || '启动生成失败');
                return;
            }
            const recipe = await pollRecipeResult(json.data.sessionId);
            if (recipe) {
                renderResult(recipe);
            }
        } catch (e) {
            alert(e.message || '生成失败，请检查 API Key 配置或查看后端日志');
        } finally {
            hideLoading();
            generateBtn.disabled = !(selected.length > 0 && selectedCuisine);
        }
    });

    /**
     * 轮询 Agent 会话状态，直到 completed / failed / 超时。
     * 每 2 秒请求一次 /api/agent/session/{sessionId}，根据 status 更新 loading 文案。
     */
    async function pollRecipeResult(sessionId) {
        const maxWaitMs = 10 * 60 * 1000; // 最长等待 10 分钟
        const intervalMs = 2000;          // 轮询间隔 2 秒
        const start = Date.now();

        while (Date.now() - start < maxWaitMs) {
            const res = await fetch('/api/agent/session/' + sessionId);
            const json = await res.json();
            const state = json.data || {};
            const status = state.status;

            if (status && STATUS_TEXT[status]) {
                updateLoadingStatus(STATUS_TEXT[status]);
            }

            if (status === 'completed') {
                const recipeId = state.recipeId;
                if (!recipeId) {
                    throw new Error('生成完成但未返回菜谱 ID');
                }
                const recipeRes = await fetch('/api/recipe/' + recipeId);
                const recipeJson = await recipeRes.json();
                if (recipeJson.success && recipeJson.data) {
                    recipeJson.data.sessionId = sessionId;
                    return recipeJson.data;
                }
                throw new Error(recipeJson.message || '获取菜谱详情失败');
            }

            if (status === 'failed') {
                throw new Error(state.error || '菜谱生成失败');
            }

            await sleep(intervalMs);
        }
        throw new Error('生成超时（超过10分钟），请查看后端日志后重试');
    }

    function sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    function updateLoadingStatus(text) {
        document.getElementById('loadingText').textContent = text;
    }

    function normalizeImageUrl(url) {
        if (!url) return '';
        if (url.startsWith('/static/images/')) return url.replace('/static', '');
        return url;
    }

    function dishImageHtml(imageUrl) {
        const src = normalizeImageUrl(imageUrl);
        if (!src) return '';
        return `<img class="dish-image" src="${src}" alt="效果图" onerror="this.onerror=null;this.src='/images/placeholder-dish.svg?v=2';">`;
    }

    function renderResult(recipe) {
        const nutrition = recipe.nutrition || {};
        const stepsHtml = (recipe.steps || []).slice(0, 3).map((s, i) =>
            `<li class="step-item"><span class="step-num">${i + 1}</span><span>${s}</span></li>`
        ).join('');
        const moreSteps = (recipe.steps || []).length > 3
            ? `<p class="sub-hint">还有 ${recipe.steps.length - 3} 个步骤...</p>` : '';
        const ingHtml = (recipe.ingredients || []).map(i =>
            `<span class="ingredient-tag">${i.category} ${i.name} ${i.amount || ''}</span>`
        ).join('');

        resultArea.innerHTML = `
            <div class="recipe-result">
                <div class="recipe-header">
                    <h2>${recipe.title || '美味佳肴'}</h2>
                    <div class="meta-tags">
                        <span class="meta-tag">${recipe.cuisineName || ''} 风味</span>
                        <span class="meta-tag">${recipe.difficulty || '简单'}</span>
                        <span class="meta-tag">${recipe.cookingTime || 30}分钟</span>
                    </div>
                </div>
                ${dishImageHtml(recipe.imageUrl)}
                <p class="hint">${recipe.summary || ''}</p>
                <div class="section-block">
                    <h3>🥬 所需食材</h3>
                    <div class="ingredient-tags">${ingHtml}</div>
                </div>
                <div class="section-block">
                    <h3>📝 制作步骤</h3>
                    <ol class="step-list">${stepsHtml}</ol>
                    ${moreSteps}
                </div>
                <div class="section-block">
                    <h3>🍎 营养分析</h3>
                    <div class="nutrition-grid">
                        <div class="nutrition-item">热量：${nutrition.calories || '-'}</div>
                        <div class="nutrition-item">蛋白质：${nutrition.protein || '-'}</div>
                        <div class="nutrition-item">脂肪：${nutrition.fat || '-'}</div>
                        <div class="nutrition-item">碳水：${nutrition.carbs || '-'}</div>
                    </div>
                    ${nutrition.healthTips ? `<p class="sub-hint" style="margin-top:8px">${(nutrition.healthTips || []).join('；')}</p>` : ''}
                </div>
                <a class="detail-link" href="/recipe/${recipe.id}">查看完整步骤 →</a>
            </div>`;
        resultArea.scrollIntoView({ behavior: 'smooth' });
    }

    function showLoading(text) {
        document.getElementById('loadingText').textContent = text;
        loadingOverlay.hidden = false;
    }

    function hideLoading() {
        loadingOverlay.hidden = true;
    }

    renderSelected();
})();
