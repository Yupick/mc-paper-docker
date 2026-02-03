// Sistema de Crafteo - JavaScript
let currentPlayer = null;
let selectedRecipeId = null;

// Inicializar al cargar la página
document.addEventListener('DOMContentLoaded', function() {
    currentPlayer = localStorage.getItem('username') || 'Jugador';
    document.getElementById('username').textContent = currentPlayer;
    
    // Cargar datos iniciales
    loadRecipes();
    loadActiveSessions();
    loadCraftingStats();
    loadCraftingHistory();
    
    // Auto-refrescar cada 3 segundos
    setInterval(() => {
        loadActiveSessions();
        loadCraftingStats();
    }, 3000);
});

// Cargar lista de recetas
async function loadRecipes() {
    try {
        const response = await fetch('/api/rpg/crafting/recipes', {
            headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
        });
        
        if (!response.ok) throw new Error('Error al cargar recetas');
        
        const recipes = await response.json();
        displayRecipes(recipes);
    } catch (error) {
        console.error('Error:', error);
        document.getElementById('recipes-list').innerHTML = 
            '<div class="col-12 alert alert-danger">Error al cargar recetas</div>';
    }
}

// Mostrar recetas en tarjetas
function displayRecipes(recipes) {
    const container = document.getElementById('recipes-list');
    container.innerHTML = '';
    
    if (!recipes || recipes.length === 0) {
        container.innerHTML = '<div class="col-12 text-center text-muted">No hay recetas disponibles</div>';
        return;
    }
    
    recipes.forEach(recipe => {
        const tierClass = recipe.tier.toLowerCase();
        const card = document.createElement('div');
        card.className = 'col-md-6 col-lg-4';
        card.innerHTML = `
            <div class="recipe-card" onclick="showRecipeDetails('${recipe.id}')">
                <div class="recipe-header">
                    <div>
                        <div class="recipe-title">${recipe.name}</div>
                        <span class="recipe-tier ${tierClass}">${recipe.tier}</span>
                    </div>
                </div>
                <div class="recipe-description">${recipe.description}</div>
                <div class="recipe-stats">
                    <div class="recipe-stat">
                        <div class="recipe-stat-label">XP</div>
                        <div class="recipe-stat-value">+${recipe.experience_reward}</div>
                    </div>
                    <div class="recipe-stat">
                        <div class="recipe-stat-label">Monedas</div>
                        <div class="recipe-stat-value">+${recipe.coin_reward}</div>
                    </div>
                    <div class="recipe-stat">
                        <div class="recipe-stat-label">Tiempo</div>
                        <div class="recipe-stat-value">${recipe.crafting_time_seconds}s</div>
                    </div>
                </div>
            </div>
        `;
        container.appendChild(card);
    });
}

// Mostrar detalles de receta
async function showRecipeDetails(recipeId) {
    try {
        const response = await fetch(`/api/rpg/crafting/recipe/${recipeId}`, {
            headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
        });
        
        if (!response.ok) throw new Error('Error al cargar receta');
        
        const recipe = await response.json();
        selectedRecipeId = recipe.id;
        
        // Llenar modal
        document.getElementById('recipeTitle').textContent = recipe.name;
        document.getElementById('recipeDescription').textContent = recipe.description;
        document.getElementById('recipeTier').textContent = recipe.tier;
        document.getElementById('recipeTier').className = `badge bg-secondary recipe-tier ${recipe.tier.toLowerCase()}`;
        document.getElementById('recipeCraftingTime').textContent = `${recipe.crafting_time_seconds} segundos`;
        
        // Ingredientes
        const ingredientsList = document.getElementById('recipeIngredients');
        ingredientsList.innerHTML = '';
        if (recipe.ingredients && recipe.ingredients.length > 0) {
            recipe.ingredients.forEach(ingredient => {
                const item = document.createElement('li');
                item.className = 'list-group-item bg-dark';
                item.innerHTML = `
                    <i class="fas fa-box"></i> ${ingredient.material}
                    <span class="badge bg-info float-end">x${ingredient.amount}</span>
                `;
                ingredientsList.appendChild(item);
            });
        }
        
        // Recompensas
        const rewardsList = document.getElementById('recipeRewards');
        rewardsList.innerHTML = '';
        
        const resultItem = document.createElement('li');
        resultItem.className = 'list-group-item bg-dark text-success';
        resultItem.innerHTML = `
            <i class="fas fa-gift"></i> ${recipe.result_item}
            <span class="badge bg-success float-end">x${recipe.result_amount}</span>
        `;
        rewardsList.appendChild(resultItem);
        
        const xpItem = document.createElement('li');
        xpItem.className = 'list-group-item bg-dark text-warning';
        xpItem.innerHTML = `<i class="fas fa-star"></i> XP: +${recipe.experience_reward}`;
        rewardsList.appendChild(xpItem);
        
        const coinItem = document.createElement('li');
        coinItem.className = 'list-group-item bg-dark text-info';
        coinItem.innerHTML = `<i class="fas fa-coins"></i> Monedas: +${recipe.coin_reward}`;
        rewardsList.appendChild(coinItem);
        
        // Mostrar modal
        const modal = new bootstrap.Modal(document.getElementById('recipeModal'));
        modal.show();
    } catch (error) {
        console.error('Error:', error);
        alert('Error al cargar los detalles de la receta');
    }
}

// Iniciar crafteo
async function startCrafting() {
    if (!selectedRecipeId) return;
    
    try {
        const response = await fetch('/api/rpg/crafting/start', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ recipe_id: selectedRecipeId })
        });
        
        if (!response.ok) throw new Error('Error al iniciar crafteo');
        
        const result = await response.json();
        
        // Cerrar modal y actualizar vistas
        bootstrap.Modal.getInstance(document.getElementById('recipeModal')).hide();
        loadActiveSessions();
        
        // Mostrar notificación
        showNotification('Crafteo iniciado', 'Puedes ver el progreso en la sección de Crafteo en Curso', 'success');
    } catch (error) {
        console.error('Error:', error);
        showNotification('Error', error.message, 'danger');
    }
}

// Cargar crafteos en curso
async function loadActiveSessions() {
    try {
        const response = await fetch('/api/rpg/crafting/active', {
            headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
        });
        
        if (!response.ok) throw new Error('Error al cargar crafteos');
        
        const sessions = await response.json();
        displayActiveSessions(sessions);
    } catch (error) {
        console.error('Error:', error);
    }
}

// Mostrar crafteos en curso
function displayActiveSessions(sessions) {
    const container = document.getElementById('crafting-list');
    container.innerHTML = '';
    
    if (!sessions || sessions.length === 0) {
        container.innerHTML = '<div class="col-12 text-center text-muted"><p><i class="fas fa-inbox"></i> No hay crafteos en curso</p></div>';
        return;
    }
    
    sessions.forEach(session => {
        const progress = (session.elapsed_time / (session.total_time * 1000)) * 100;
        const remaining = Math.max(0, session.remaining_time / 1000);
        
        const sessionDiv = document.createElement('div');
        sessionDiv.className = 'col-12';
        sessionDiv.innerHTML = `
            <div class="crafting-session">
                <div class="crafting-session-title">
                    <i class="fas fa-hammer"></i> ${session.recipe_name}
                </div>
                <div class="crafting-session-progress">
                    <div class="progress">
                        <div class="progress-bar" style="width: ${Math.min(100, progress)}%"></div>
                    </div>
                </div>
                <div class="crafting-session-info">
                    <span><i class="fas fa-hourglass-end"></i> Tiempo restante: ${Math.ceil(remaining)}s</span>
                    <span>${Math.round(progress)}% completado</span>
                    ${session.is_complete ? '<span class="badge bg-success"><i class="fas fa-check"></i> ¡Listo!</span>' : ''}
                </div>
                ${session.is_complete ? `
                    <button class="btn btn-sm btn-success mt-2 w-100" onclick="completeCrafting('${session.session_id}')">
                        <i class="fas fa-check"></i> Recoger Item
                    </button>
                ` : ''}
            </div>
        `;
        container.appendChild(sessionDiv);
    });
}

// Completar crafteo
async function completeCrafting(sessionId) {
    try {
        const response = await fetch('/api/rpg/crafting/complete', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ session_id: sessionId })
        });
        
        if (!response.ok) throw new Error('Error al completar crafteo');
        
        loadActiveSessions();
        loadCraftingStats();
        loadCraftingHistory();
        
        showNotification('¡Crafteo completado!', 'Has obtenido tu item', 'success');
    } catch (error) {
        console.error('Error:', error);
        showNotification('Error', error.message, 'danger');
    }
}

// Cargar estadísticas de crafteo
async function loadCraftingStats() {
    try {
        const response = await fetch('/api/rpg/crafting/stats', {
            headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
        });
        
        if (!response.ok) throw new Error('Error al cargar estadísticas');
        
        const stats = await response.json();
        
        document.getElementById('stat-recipes').textContent = 
            `${stats.recipes_unlocked}/${stats.total_recipes}`;
        document.getElementById('stat-crafted').textContent = stats.completed_crafts;
        document.getElementById('stat-xp').textContent = stats.total_experience.toLocaleString();
        document.getElementById('stat-coins').textContent = stats.total_coins.toLocaleString();
    } catch (error) {
        console.error('Error:', error);
    }
}

// Cargar historial de crafteos
async function loadCraftingHistory() {
    try {
        const response = await fetch('/api/rpg/crafting/history?limit=10', {
            headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
        });
        
        if (!response.ok) throw new Error('Error al cargar historial');
        
        const history = await response.json();
        displayCraftingHistory(history);
    } catch (error) {
        console.error('Error:', error);
    }
}

// Mostrar historial de crafteos
function displayCraftingHistory(history) {
    const tbody = document.getElementById('history-list');
    tbody.innerHTML = '';
    
    if (!history || history.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">Sin historial de crafteos</td></tr>';
        return;
    }
    
    history.forEach(entry => {
        const row = document.createElement('tr');
        const completed = entry.completed_at ? 'table-success' : '';
        const status = entry.completed_at ? '<span class="badge bg-success">Completado</span>' : 
                      '<span class="badge bg-warning">En progreso</span>';
        
        row.className = completed;
        row.innerHTML = `
            <td>${entry.recipe_name || entry.recipe_id}</td>
            <td>${new Date(entry.started_at).toLocaleString()}</td>
            <td><i class="fas fa-star text-warning"></i> ${entry.experience_earned || 0}</td>
            <td><i class="fas fa-coins text-info"></i> ${entry.coins_earned || 0}</td>
            <td>${status}</td>
        `;
        tbody.appendChild(row);
    });
}

// Mostrar notificación
function showNotification(title, message, type = 'info') {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show fixed-top mt-3`;
    alertDiv.style.maxWidth = '500px';
    alertDiv.style.margin = '15px auto';
    alertDiv.style.zIndex = '9999';
    alertDiv.innerHTML = `
        <strong>${title}</strong> ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    document.body.appendChild(alertDiv);
    
    setTimeout(() => {
        alertDiv.remove();
    }, 4000);
}
