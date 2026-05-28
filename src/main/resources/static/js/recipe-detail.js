(function () {
    const container = document.getElementById('recipeDetail');
    const id = container.dataset.id;

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

    fetch('/api/recipe/' + id)
        .then(r => r.json())
        .then(json => {
            if (!json.success || !json.data) {
                container.innerHTML = '<p class="hint">菜谱不存在</p>';
                return;
            }
            const recipe = json.data;
            const nutrition = recipe.nutrition || {};
            container.innerHTML = `
                <section class="card">
                    <div class="card-body recipe-result">
                        <div class="recipe-header">
                            <h2>${recipe.title}</h2>
                            <div class="meta-tags">
                                <span class="meta-tag">${recipe.cuisineName} 风味</span>
                                <span class="meta-tag">${recipe.difficulty}</span>
                                <span class="meta-tag">${recipe.cookingTime}分钟</span>
                            </div>
                        </div>
                        ${dishImageHtml(recipe.imageUrl)}
                        <p class="hint">${recipe.summary || ''}</p>
                        <div class="section-block">
                            <h3>🥬 所需食材</h3>
                            <div class="ingredient-tags">
                                ${(recipe.ingredients || []).map(i =>
                                    `<span class="ingredient-tag">${i.category} ${i.name} ${i.amount || ''}</span>`
                                ).join('')}
                            </div>
                        </div>
                        <div class="section-block">
                            <h3>📝 制作步骤</h3>
                            <ol class="step-list">
                                ${(recipe.steps || []).map((s, i) =>
                                    `<li class="step-item"><span class="step-num">${i + 1}</span><span>${s}</span></li>`
                                ).join('')}
                            </ol>
                        </div>
                        <div class="section-block">
                            <h3>🍎 营养分析</h3>
                            <div class="nutrition-grid">
                                <div class="nutrition-item">热量：${nutrition.calories || '-'}</div>
                                <div class="nutrition-item">蛋白质：${nutrition.protein || '-'}</div>
                                <div class="nutrition-item">脂肪：${nutrition.fat || '-'}</div>
                                <div class="nutrition-item">碳水：${nutrition.carbs || '-'}</div>
                            </div>
                        </div>
                    </div>
                </section>`;
        });
})();
