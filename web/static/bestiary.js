// Variables globales
let bestiaryConfig = {};
let categoriesData = {};

// Inicialización
document.addEventListener('DOMContentLoaded', function() {
    initTabs();
    loadBestiaryConfig();
    loadCategories();
    loadStats();
});

// Sistema de tabs
function initTabs() {
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const tabName = btn.getAttribute('data-tab');

            // Remover active de todos
            tabBtns.forEach(b => b.classList.remove('active'));
            tabContents.forEach(c => c.classList.remove('active'));

            // Activar el clickeado
            btn.classList.add('active');
            document.getElementById(tabName).classList.add('active');

            // Cargar datos del tab si es necesario
            if (tabName === 'stats') {
                loadStats();
            }
        });
    });
}

// ============================================
// TAB 1: Configuración Global
// ============================================

function loadBestiaryConfig() {
    fetch('/api/rpg/bestiary/config')
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                bestiaryConfig = data.config;
                populateGlobalConfig();
            } else {
                showNotification('Error al cargar configuración: ' + data.message, 'error');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showNotification('Error de conexión', 'error');
        });
}

function populateGlobalConfig() {
    document.getElementById('bestiaryEnabled').checked = bestiaryConfig.enabled !== false;
    
    if (bestiaryConfig.progressThresholds) {
        document.getElementById('progressThresholds').value = bestiaryConfig.progressThresholds.join(', ');
    }

    if (bestiaryConfig.discoveryRewards) {
        const rewards = bestiaryConfig.discoveryRewards;
        if (rewards.firstKill) document.getElementById('firstKillXP').value = rewards.firstKill;
        if (rewards.tier1) document.getElementById('tier1XP').value = rewards.tier1;
        if (rewards.tier2) document.getElementById('tier2XP').value = rewards.tier2;
        if (rewards.tier3) document.getElementById('tier3XP').value = rewards.tier3;
        if (rewards.tier4) document.getElementById('tier4XP').value = rewards.tier4;
        if (rewards.tier5) document.getElementById('tier5XP').value = rewards.tier5;
    }
}

function saveGlobalConfig() {
    const enabled = document.getElementById('bestiaryEnabled').checked;
    const thresholdsStr = document.getElementById('progressThresholds').value;
    const thresholds = thresholdsStr.split(',').map(s => parseInt(s.trim())).filter(n => !isNaN(n));

    const discoveryRewards = {
        firstKill: parseInt(document.getElementById('firstKillXP').value) || 0,
        tier1: parseInt(document.getElementById('tier1XP').value) || 0,
        tier2: parseInt(document.getElementById('tier2XP').value) || 0,
        tier3: parseInt(document.getElementById('tier3XP').value) || 0,
        tier4: parseInt(document.getElementById('tier4XP').value) || 0,
        tier5: parseInt(document.getElementById('tier5XP').value) || 0
    };

    const payload = {
        enabled: enabled,
        progressThresholds: thresholds,
        discoveryRewards: discoveryRewards
    };

    fetch('/api/rpg/bestiary/config', {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(payload)
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showNotification('Configuración guardada correctamente', 'success');
            bestiaryConfig = data.config;
        } else {
            showNotification('Error: ' + data.message, 'error');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showNotification('Error al guardar configuración', 'error');
    });
}

// ============================================
// TAB 2: Categorías
// ============================================

function loadCategories() {
    fetch('/api/rpg/bestiary/config')
        .then(response => response.json())
        .then(data => {
            if (data.success && data.config.categories) {
                categoriesData = data.config.categories;
                renderCategories();
            }
        })
        .catch(error => console.error('Error:', error));
}

function renderCategories() {
    const grid = document.getElementById('categoriesGrid');
    grid.innerHTML = '';

    Object.entries(categoriesData).forEach(([id, category]) => {
        const card = document.createElement('div');
        card.className = 'category-card';
        
        const mobsCount = category.mobs ? category.mobs.length : 0;
        const reward = category.completionReward;
        
        card.innerHTML = `
            <div class="category-header">
                <h3>${category.name}</h3>
                <div class="category-actions">
                    <button class="btn-icon" onclick="editCategory('${id}')" title="Editar">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn-icon btn-danger" onclick="deleteCategory('${id}')" title="Eliminar">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </div>
            <p class="category-description">${category.description || 'Sin descripción'}</p>
            <div class="category-stats">
                <div class="stat-item">
                    <i class="fas fa-paw"></i>
                    <span>${mobsCount} mobs</span>
                </div>
                ${reward ? `
                    <div class="stat-item">
                        <i class="fas fa-trophy"></i>
                        <span>${reward.xp || 0} XP</span>
                    </div>
                ` : ''}
            </div>
            <div class="category-mobs">
                <strong>Mobs:</strong>
                <div class="mobs-list">
                    ${category.mobs ? category.mobs.map(m => `<span class="mob-tag">${m}</span>`).join('') : ''}
                </div>
            </div>
            ${reward && reward.title ? `
                <div class="category-reward">
                    <i class="fas fa-award"></i>
                    <span>Título: ${reward.title}</span>
                </div>
            ` : ''}
        `;
        
        grid.appendChild(card);
    });
}

function openCategoryModal(categoryId = null) {
    const modal = document.getElementById('categoryModal');
    const title = document.getElementById('modalTitle');
    
    if (categoryId) {
        title.textContent = 'Editar Categoría';
        const category = categoriesData[categoryId];
        
        document.getElementById('categoryId').value = categoryId;
        document.getElementById('categoryId').disabled = true;
        document.getElementById('categoryName').value = category.name;
        document.getElementById('categoryDescription').value = category.description || '';
        document.getElementById('categoryMobs').value = category.mobs ? category.mobs.join(', ') : '';
        
        if (category.completionReward) {
            const r = category.completionReward;
            document.getElementById('rewardTitle').value = r.title || '';
            document.getElementById('rewardXP').value = r.xp || 0;
            document.getElementById('rewardCoins').value = r.coins || 0;
            document.getElementById('rewardItem').value = r.item || '';
            document.getElementById('rewardBroadcast').checked = r.broadcast || false;
        }
    } else {
        title.textContent = 'Nueva Categoría';
        document.getElementById('categoryId').disabled = false;
        // Limpiar campos
        document.getElementById('categoryId').value = '';
        document.getElementById('categoryName').value = '';
        document.getElementById('categoryDescription').value = '';
        document.getElementById('categoryMobs').value = '';
        document.getElementById('rewardTitle').value = '';
        document.getElementById('rewardXP').value = '';
        document.getElementById('rewardCoins').value = '';
        document.getElementById('rewardItem').value = '';
        document.getElementById('rewardBroadcast').checked = false;
    }
    
    modal.style.display = 'flex';
}

function closeCategoryModal() {
    document.getElementById('categoryModal').style.display = 'none';
}

function saveCategory() {
    const id = document.getElementById('categoryId').value.trim();
    const name = document.getElementById('categoryName').value.trim();
    const description = document.getElementById('categoryDescription').value.trim();
    const mobsStr = document.getElementById('categoryMobs').value.trim();
    
    if (!id || !name || !mobsStr) {
        showNotification('Complete los campos requeridos (ID, Nombre, Mobs)', 'error');
        return;
    }
    
    const mobs = mobsStr.split(',').map(s => s.trim()).filter(s => s.length > 0);
    
    const payload = {
        id: id,
        name: name,
        description: description,
        mobs: mobs
    };
    
    // Agregar recompensa si hay datos
    const rewardTitle = document.getElementById('rewardTitle').value.trim();
    const rewardXP = parseInt(document.getElementById('rewardXP').value) || 0;
    const rewardCoins = parseInt(document.getElementById('rewardCoins').value) || 0;
    const rewardItem = document.getElementById('rewardItem').value.trim();
    const rewardBroadcast = document.getElementById('rewardBroadcast').checked;
    
    if (rewardTitle || rewardXP || rewardCoins || rewardItem) {
        payload.completionReward = {
            title: rewardTitle,
            xp: rewardXP,
            coins: rewardCoins,
            item: rewardItem,
            broadcast: rewardBroadcast
        };
    }
    
    fetch('/api/rpg/bestiary/category', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(payload)
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showNotification('Categoría guardada correctamente', 'success');
            closeCategoryModal();
            loadCategories();
        } else {
            showNotification('Error: ' + data.message, 'error');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showNotification('Error al guardar categoría', 'error');
    });
}

function editCategory(categoryId) {
    openCategoryModal(categoryId);
}

function deleteCategory(categoryId) {
    if (!confirm(`¿Eliminar la categoría "${categoryId}"?`)) return;
    
    fetch(`/api/rpg/bestiary/category/${categoryId}`, {
        method: 'DELETE'
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showNotification('Categoría eliminada', 'success');
            loadCategories();
        } else {
            showNotification('Error: ' + data.message, 'error');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showNotification('Error al eliminar categoría', 'error');
    });
}

// ============================================
// TAB 3: Estadísticas
// ============================================

function loadStats() {
    fetch('/api/rpg/bestiary/stats')
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                document.getElementById('totalGlobalKills').textContent = data.totalGlobalKills.toLocaleString();
                document.getElementById('totalUniqueDiscoveries').textContent = data.totalUniqueDiscoveries;
                
                if (data.mostKilledMob) {
                    document.getElementById('mostKilledMob').textContent = 
                        `${data.mostKilledMob.mobId} (${data.mostKilledMob.kills})`;
                } else {
                    document.getElementById('mostKilledMob').textContent = '-';
                }
                
                renderLeaderboard(data.topPlayers);
            }
        })
        .catch(error => console.error('Error:', error));
}

function renderLeaderboard(topPlayers) {
    const tbody = document.getElementById('leaderboardBody');
    tbody.innerHTML = '';
    
    if (!topPlayers || topPlayers.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4">No hay datos</td></tr>';
        return;
    }
    
    topPlayers.forEach((player, index) => {
        const row = document.createElement('tr');
        const percentage = Math.round((player.discoveries / 50) * 100); // Asumiendo 50 mobs totales
        
        row.innerHTML = `
            <td>${index + 1}</td>
            <td>${player.player}</td>
            <td>${player.discoveries}</td>
            <td>
                <div class="progress-bar-container">
                    <div class="progress-bar" style="width: ${percentage}%"></div>
                    <span>${percentage}%</span>
                </div>
            </td>
        `;
        
        tbody.appendChild(row);
    });
}

// ============================================
// TAB 4: Vista de Jugador
// ============================================

function loadPlayerBestiary() {
    const playerName = document.getElementById('playerNameInput').value.trim();
    
    if (!playerName) {
        showNotification('Ingrese un nombre de jugador', 'error');
        return;
    }
    
    const content = document.getElementById('playerBestiaryContent');
    content.innerHTML = '<div class="loading"><i class="fas fa-spinner fa-spin"></i> Cargando...</div>';
    
    fetch(`/api/rpg/bestiary/${playerName}`)
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                renderPlayerBestiary(data);
            } else {
                content.innerHTML = `<div class="error-message">${data.message}</div>`;
            }
        })
        .catch(error => {
            console.error('Error:', error);
            content.innerHTML = '<div class="error-message">Error al cargar bestiario</div>';
        });
}

function renderPlayerBestiary(data) {
    const content = document.getElementById('playerBestiaryContent');
    
    const html = `
        <div class="player-header">
            <h3><i class="fas fa-user"></i> ${data.player}</h3>
            <div class="player-stats">
                <div class="stat-badge">
                    <i class="fas fa-eye"></i>
                    <span>${data.totalDiscoveries} Descubrimientos</span>
                </div>
                <div class="stat-badge">
                    <i class="fas fa-crosshairs"></i>
                    <span>${data.totalKills} Kills Totales</span>
                </div>
            </div>
        </div>
        
        <div class="entries-grid">
            ${data.entries.map(entry => `
                <div class="entry-card ${entry.discovered ? 'discovered' : 'undiscovered'}">
                    <div class="entry-header">
                        <h4>${entry.mobId}</h4>
                        ${entry.discovered ? '<i class="fas fa-check-circle discovered-icon"></i>' : '<i class="fas fa-question-circle undiscovered-icon"></i>'}
                    </div>
                    <div class="entry-stats">
                        <div class="stat-row">
                            <span>Kills:</span>
                            <strong>${entry.kills}</strong>
                        </div>
                        <div class="stat-row">
                            <span>Tier:</span>
                            <strong>${entry.currentTier}</strong>
                        </div>
                        ${entry.firstKillDate ? `
                            <div class="stat-row">
                                <span>Primera kill:</span>
                                <small>${new Date(entry.firstKillDate).toLocaleDateString()}</small>
                            </div>
                        ` : ''}
                    </div>
                </div>
            `).join('')}
        </div>
    `;
    
    content.innerHTML = html;
}

// ============================================
// Utilidades
// ============================================

function refreshData() {
    loadBestiaryConfig();
    loadCategories();
    loadStats();
    showNotification('Datos actualizados', 'success');
}

function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.innerHTML = `
        <i class="fas fa-${type === 'success' ? 'check-circle' : type === 'error' ? 'exclamation-circle' : 'info-circle'}"></i>
        <span>${message}</span>
    `;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.classList.add('show');
    }, 10);
    
    setTimeout(() => {
        notification.classList.remove('show');
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

// Cerrar modal al hacer click fuera
window.onclick = function(event) {
    const modal = document.getElementById('categoryModal');
    if (event.target === modal) {
        closeCategoryModal();
    }
}
