// Estado Global
let dungeonDetailsCache = {};
let currentDungeon = null;
let autoRefreshInterval = null;

// ===== FUNCIONES GENERALES =====
function setupAutoRefresh() {
    autoRefreshInterval = setInterval(() => {
        const activeTab = document.querySelector('.nav-link.active');
        if (activeTab) {
            const tabName = activeTab.getAttribute('data-bs-target').substring(1);
            switch (tabName) {
                case 'explorer':
                    loadDungeonsExplorer();
                    break;
                case 'active':
                    loadActiveDungeons();
                    break;
                case 'history':
                    loadDungeonsHistory();
                    break;
                case 'stats':
                    loadDungeonsStats();
                    break;
            }
        }
    }, 10000); // Actualizar cada 10 segundos
}

function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleString('es-ES');
}

function formatDuration(seconds) {
    if (!seconds) return '-';
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    if (hours > 0) {
        return `${hours}h ${minutes}m ${secs}s`;
    }
    return `${minutes}m ${secs}s`;
}

function showNotification(message, type = 'info') {
    const alertClass = `alert-${type}`;
    const icon = type === 'success' ? 'fa-check-circle' : 
                 type === 'danger' ? 'fa-exclamation-circle' : 
                 type === 'warning' ? 'fa-exclamation-triangle' : 
                 'fa-info-circle';

    const notification = document.createElement('div');
    notification.className = `alert ${alertClass} alert-dismissible fade show position-fixed`;
    notification.style.top = '20px';
    notification.style.right = '20px';
    notification.style.zIndex = '1050';
    notification.innerHTML = `
        <i class="fas ${icon}"></i> ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    document.body.appendChild(notification);
    setTimeout(() => notification.remove(), 5000);
}

// ===== TAB: EXPLORADOR =====
async function loadDungeonsExplorer() {
    try {
        const response = await fetch('/api/rpg/dungeons/config');
        const config = await response.json();

        if (!response.ok) {
            showNotification('Error al cargar mazmorras', 'danger');
            return;
        }

        let html = '';
        if (Array.isArray(config.dungeons) && config.dungeons.length > 0) {
            config.dungeons.forEach(dungeon => {
                const status = dungeon.enabled ? 
                    '<span class="dungeon-status" style="background-color: rgba(6, 214, 160, 0.2); color: #06D6A0;"><i class="fas fa-check"></i> Activa</span>' :
                    '<span class="dungeon-status" style="background-color: rgba(239, 71, 111, 0.2); color: #EF476F;"><i class="fas fa-times"></i> Desactiva</span>';

                html += `
                    <div class="dungeon-card">
                        <h5>
                            <i class="fas fa-dungeon"></i>
                            ${dungeon.name}
                            ${status}
                        </h5>
                        <p class="dungeon-description">${dungeon.description}</p>
                        <div class="dungeon-level">
                            <i class="fas fa-chart-line"></i> Nivel ${dungeon.minLevel} - ${dungeon.maxLevel}
                        </div>
                        <div class="dungeon-stats">
                            <div class="dungeon-stat-item">
                                <div class="dungeon-stat-label">Jugadores</div>
                                <div class="dungeon-stat-value">${dungeon.minPlayers} - ${dungeon.maxPlayers}</div>
                            </div>
                            <div class="dungeon-stat-item">
                                <div class="dungeon-stat-label">Duración Estimada</div>
                                <div class="dungeon-stat-value">${dungeon.estimatedDuration} min</div>
                            </div>
                            <div class="dungeon-stat-item">
                                <div class="dungeon-stat-label">Salas</div>
                                <div class="dungeon-stat-value">${dungeon.roomTemplates ? dungeon.roomTemplates.length : 0}</div>
                            </div>
                            <div class="dungeon-stat-item">
                                <div class="dungeon-stat-label">Recompensas</div>
                                <div class="dungeon-stat-value">${dungeon.rewards.baseXp} XP</div>
                            </div>
                        </div>
                        <div class="dungeon-buttons">
                            <button class="btn btn-primary" onclick="openDungeonDetails('${dungeon.id}', '${dungeon.name}')">
                                <i class="fas fa-eye"></i> Detalles
                            </button>
                            <button class="btn btn-success" onclick="startDungeon('${dungeon.id}', '${dungeon.name}')">
                                <i class="fas fa-play"></i> Iniciar
                            </button>
                        </div>
                    </div>
                `;
            });
        } else {
            html = '<div class="alert alert-info"><i class="fas fa-info-circle"></i> No hay mazmorras configuradas</div>';
        }

        document.getElementById('dungeonsList').innerHTML = html;
    } catch (error) {
        console.error('Error:', error);
        showNotification('Error al cargar explorador', 'danger');
    }
}

function openDungeonDetails(dungeonId, dungeonName) {
    currentDungeon = dungeonId;
    const modal = new bootstrap.Modal(document.getElementById('dungeonDetailsModal'));
    document.getElementById('dungeonDetailsContent').innerHTML = '<div class="text-center"><i class="fas fa-spinner fa-spin"></i> Cargando...</div>';
    modal.show();

    // Cargar detalles desde config
    fetch('/api/rpg/dungeons/config')
        .then(r => r.json())
        .then(config => {
            const dungeon = config.dungeons.find(d => d.id === dungeonId);
            if (dungeon) {
                let html = `
                    <h6><i class="fas fa-info-circle"></i> Información General</h6>
                    <div class="row mb-3">
                        <div class="col-md-6">
                            <strong>Nombre:</strong> ${dungeon.name}<br>
                            <strong>Descripción:</strong> ${dungeon.description}
                        </div>
                        <div class="col-md-6">
                            <strong>Nivel Requerido:</strong> ${dungeon.minLevel} - ${dungeon.maxLevel}<br>
                            <strong>Jugadores:</strong> ${dungeon.minPlayers} - ${dungeon.maxPlayers}
                        </div>
                    </div>

                    <h6><i class="fas fa-door-open"></i> Salas (${dungeon.roomTemplates.length})</h6>
                    <div class="table-responsive mb-3">
                        <table class="table table-dark table-sm">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>Tipo</th>
                                    <th>Mobs</th>
                                    <th>Tesoro</th>
                                    <th>Boss</th>
                                </tr>
                            </thead>
                            <tbody>
                `;

                dungeon.roomTemplates.forEach(room => {
                    html += `
                        <tr>
                            <td>${room.id}</td>
                            <td><span class="badge badge-info">${room.type}</span></td>
                            <td>${room.mobCount} mobs</td>
                            <td>${room.hasTreasure ? '<i class="fas fa-gem" style="color: gold;"></i>' : '-'}</td>
                            <td>${room.hasBoss ? '<i class="fas fa-crown" style="color: #FFD60A;"></i>' : '-'}</td>
                        </tr>
                    `;
                });

                html += `
                            </tbody>
                        </table>
                    </div>

                    <h6><i class="fas fa-gifts"></i> Recompensas</h6>
                    <div class="row">
                        <div class="col-md-6">
                            <strong>XP Base:</strong> ${dungeon.rewards.baseXp}<br>
                            <strong>Monedas Base:</strong> ${dungeon.rewards.baseCoin}
                        </div>
                        <div class="col-md-6">
                            <strong>Bonus Boss XP:</strong> ${dungeon.rewards.bossXpBonus}<br>
                            <strong>Bonus Boss Monedas:</strong> ${dungeon.rewards.bossCoinBonus}
                        </div>
                    </div>
                `;

                document.getElementById('dungeonDetailsContent').innerHTML = html;
            }
        })
        .catch(error => {
            console.error(error);
            document.getElementById('dungeonDetailsContent').innerHTML = '<div class="alert alert-danger">Error al cargar detalles</div>';
        });
}

async function startDungeon(dungeonId, dungeonName) {
    try {
        const response = await fetch('/api/rpg/dungeons/start', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ dungeon_id: dungeonId })
        });

        if (response.ok) {
            showNotification(`Mazmorra "${dungeonName}" iniciada`, 'success');
            bootstrap.Modal.getInstance(document.getElementById('dungeonDetailsModal')).hide();
            loadActiveDungeons();
        } else {
            showNotification('Error al iniciar mazmorra', 'danger');
        }
    } catch (error) {
        console.error(error);
        showNotification('Error al iniciar mazmorra', 'danger');
    }
}

// ===== TAB: ACTIVAS =====
async function loadActiveDungeons() {
    try {
        const response = await fetch('/api/rpg/dungeons/active');
        const data = await response.json();

        if (!response.ok) {
            showNotification('Error al cargar mazmorras activas', 'danger');
            return;
        }

        let html = '';
        if (data.active_dungeons && data.active_dungeons.length > 0) {
            data.active_dungeons.forEach(dungeon => {
                html += `
                    <tr>
                        <td><strong>${dungeon.dungeon_name}</strong></td>
                        <td>${dungeon.world}</td>
                        <td>${formatDate(dungeon.started_at)}</td>
                        <td><span class="badge badge-info">${dungeon.player_count}</span></td>
                        <td><strong>${dungeon.total_mobs_killed}</strong></td>
                        <td><span class="badge badge-success"><i class="fas fa-play-circle"></i> En Progreso</span></td>
                        <td>
                            <button class="btn btn-sm btn-danger" onclick="exitDungeon('${dungeon.session_id}')">
                                <i class="fas fa-sign-out-alt"></i> Salir
                            </button>
                        </td>
                    </tr>
                `;
            });
        } else {
            html = '<tr><td colspan="7" class="text-center text-muted">No hay mazmorras activas</td></tr>';
        }

        document.getElementById('activeTableBody').innerHTML = html;
    } catch (error) {
        console.error(error);
        showNotification('Error al cargar mazmorras activas', 'danger');
    }
}

async function exitDungeon(sessionId) {
    if (!confirm('¿Estás seguro de que deseas salir de la mazmorra?')) return;

    try {
        const response = await fetch('/api/rpg/dungeons/exit', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ session_id: sessionId })
        });

        if (response.ok) {
            showNotification('Salida de mazmorra registrada', 'success');
            loadActiveDungeons();
            loadDungeonsHistory();
        } else {
            showNotification('Error al salir de mazmorra', 'danger');
        }
    } catch (error) {
        console.error(error);
        showNotification('Error al salir de mazmorra', 'danger');
    }
}

// ===== TAB: CONFIGURACIÓN =====
async function loadDungeonsConfig() {
    try {
        const response = await fetch('/api/rpg/dungeons/config');
        const data = await response.json();

        if (response.ok) {
            document.getElementById('configEditor').value = JSON.stringify(data, null, 2);
        } else {
            showNotification('Error al cargar configuración', 'danger');
        }
    } catch (error) {
        console.error(error);
        showNotification('Error al cargar configuración', 'danger');
    }
}

async function saveDungeonsConfig() {
    try {
        const configText = document.getElementById('configEditor').value;
        const config = JSON.parse(configText);

        const response = await fetch('/api/rpg/dungeons/config', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(config)
        });

        if (response.ok) {
            showNotification('Configuración guardada correctamente', 'success');
            loadDungeonsExplorer();
        } else {
            showNotification('Error al guardar configuración', 'danger');
        }
    } catch (error) {
        console.error(error);
        showNotification('Error: JSON inválido', 'danger');
    }
}

// ===== TAB: HISTORIAL =====
async function loadDungeonsHistory() {
    try {
        const response = await fetch('/api/rpg/dungeons/history');
        const data = await response.json();

        if (!response.ok) {
            showNotification('Error al cargar historial', 'danger');
            return;
        }

        let html = '';
        if (data.history && data.history.length > 0) {
            data.history.forEach(dungeon => {
                const completedBadge = dungeon.status === 'COMPLETED' ?
                    '<span class="badge badge-success">✓ Completada</span>' :
                    '<span class="badge badge-danger">✗ Incompleta</span>';

                html += `
                    <tr>
                        <td><strong>${dungeon.dungeon_name}</strong></td>
                        <td>${dungeon.world}</td>
                        <td>${formatDate(dungeon.started_at)}</td>
                        <td>${formatDate(dungeon.completed_at)}</td>
                        <td>${formatDuration(dungeon.duration_seconds)}</td>
                        <td><span class="badge badge-info">${dungeon.player_count}</span></td>
                        <td><strong>${dungeon.total_mobs_killed}</strong></td>
                        <td>${completedBadge}</td>
                        <td>
                            <button class="btn btn-sm btn-primary" onclick="viewDungeonDetails('${dungeon.session_id}')">
                                <i class="fas fa-eye"></i> Ver
                            </button>
                        </td>
                    </tr>
                `;
            });
        } else {
            html = '<tr><td colspan="9" class="text-center text-muted">No hay historial de mazmorras</td></tr>';
        }

        document.getElementById('historyTableBody').innerHTML = html;
    } catch (error) {
        console.error(error);
        showNotification('Error al cargar historial', 'danger');
    }
}

// ===== TAB: ESTADÍSTICAS =====
async function loadDungeonsStats() {
    try {
        const response = await fetch('/api/rpg/dungeons/stats');
        const data = await response.json();

        if (!response.ok) {
            showNotification('Error al cargar estadísticas', 'danger');
            return;
        }

        // Actualizar stat cards
        document.getElementById('statTotalDungeons').textContent = data.stats.total_dungeons || 0;
        document.getElementById('statCompletedDungeons').textContent = data.stats.completed || 0;
        document.getElementById('statTotalMobsKilled').textContent = (data.stats.total_mobs_killed || 0).toLocaleString();
        document.getElementById('statAvgTime').textContent = formatDuration(data.stats.avg_duration || 0);

        // Top 10 jugadores
        let html = '';
        if (data.top_players && data.top_players.length > 0) {
            data.top_players.forEach((player, index) => {
                html += `
                    <tr>
                        <td><strong>#${index + 1}</strong></td>
                        <td><i class="fas fa-user"></i> ${player.player_name}</td>
                        <td>${player.kills}</td>
                        <td><span class="badge badge-info">${player.rewards_xp}</span></td>
                        <td><span class="badge badge-success">${player.rewards_coins}</span></td>
                    </tr>
                `;
            });
        } else {
            html = '<tr><td colspan="5" class="text-center text-muted">Sin datos disponibles</td></tr>';
        }

        document.getElementById('topPlayersTable').innerHTML = html;
    } catch (error) {
        console.error(error);
        showNotification('Error al cargar estadísticas', 'danger');
    }
}

// ===== INICIALIZACIÓN =====
document.addEventListener('DOMContentLoaded', () => {
    // Cargar contenido inicial
    loadDungeonsExplorer();
    loadActiveDungeons();
    loadDungeonsConfig();
    loadDungeonsHistory();
    loadDungeonsStats();

    // Botones de acción
    document.getElementById('refreshAllBtn').addEventListener('click', () => {
        loadDungeonsExplorer();
        loadActiveDungeons();
        loadDungeonsConfig();
        loadDungeonsHistory();
        loadDungeonsStats();
        showNotification('Datos actualizados', 'success');
    });

    document.getElementById('saveConfigBtn').addEventListener('click', saveDungeonsConfig);
    document.getElementById('loadConfigBtn').addEventListener('click', loadDungeonsConfig);

    // Event listeners para los tabs
    document.getElementById('explorer-tab').addEventListener('click', loadDungeonsExplorer);
    document.getElementById('active-tab').addEventListener('click', loadActiveDungeons);
    document.getElementById('config-tab').addEventListener('click', loadDungeonsConfig);
    document.getElementById('history-tab').addEventListener('click', loadDungeonsHistory);
    document.getElementById('stats-tab').addEventListener('click', loadDungeonsStats);

    // Auto-refresh
    setupAutoRefresh();
});

// Limpiar interval al descargar la página
window.addEventListener('beforeunload', () => {
    if (autoRefreshInterval) {
        clearInterval(autoRefreshInterval);
    }
});
