// invasions.js - Gestión del panel de invasiones

let currentConfig = null;
let refreshInterval = null;

// Inicialización
document.addEventListener('DOMContentLoaded', function() {
    loadInvasionsConfig();
    loadActiveInvasions();
    loadInvasionsHistory();
    loadInvasionsStats();
    
    // Auto-refresh para invasiones activas cada 5 segundos
    refreshInterval = setInterval(() => {
        const activeTab = document.querySelector('#active-tab');
        if (activeTab && activeTab.classList.contains('active')) {
            loadActiveInvasions();
        }
    }, 5000);
    
    // Search en historial
    document.getElementById('historySearch')?.addEventListener('input', filterHistory);
});

// =====================================================
// INVASIONES ACTIVAS
// =====================================================

async function loadActiveInvasions() {
    try {
        const response = await fetch('/api/rpg/invasions/active');
        const data = await response.json();
        
        if (!data.success) {
            throw new Error(data.message);
        }
        
        renderActiveInvasions(data.invasions);
    } catch (error) {
        console.error('Error loading active invasions:', error);
        document.getElementById('activeInvasionsContainer').innerHTML = `
            <div class="alert alert-danger">
                <i class="bi bi-exclamation-circle"></i> Error al cargar invasiones activas: ${error.message}
            </div>
        `;
    }
}

function renderActiveInvasions(invasions) {
    const container = document.getElementById('activeInvasionsContainer');
    
    if (invasions.length === 0) {
        container.innerHTML = `
            <div class="alert alert-secondary text-center">
                <i class="bi bi-shield-check" style="font-size: 3rem;"></i>
                <h5 class="mt-3">No hay invasiones activas</h5>
                <p class="mb-0">Todas las invasiones están completadas o inactivas. Puedes iniciar una manualmente o esperar a la próxima programada.</p>
            </div>
        `;
        return;
    }
    
    let html = '<div class="row">';
    
    invasions.forEach(invasion => {
        const progress = (invasion.currentWave / invasion.totalWaves * 100).toFixed(0);
        const mobsProgress = (invasion.totalMobsKilled / invasion.totalMobsSpawned * 100).toFixed(0);
        
        html += `
            <div class="col-md-6 mb-3">
                <div class="card invasion-card status-${invasion.status.toLowerCase()}">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <div>
                            <i class="bi bi-exclamation-triangle-fill text-danger"></i>
                            <strong>${invasion.displayName || invasion.invasionId}</strong>
                        </div>
                        <span class="badge status-${invasion.status.toLowerCase()}">${invasion.status}</span>
                    </div>
                    <div class="card-body">
                        <div class="mb-3">
                            <small class="text-muted">Mundo: ${invasion.worldName}</small><br>
                            <small class="text-muted">Oleada: ${invasion.currentWave}/${invasion.totalWaves}</small>
                        </div>
                        
                        <div class="mb-3">
                            <div class="d-flex justify-content-between mb-1">
                                <small>Progreso de oleadas</small>
                                <small>${progress}%</small>
                            </div>
                            <div class="progress">
                                <div class="progress-bar bg-danger" style="width: ${progress}%"></div>
                            </div>
                        </div>
                        
                        <div class="mb-3">
                            <div class="d-flex justify-content-between mb-1">
                                <small>Mobs eliminados</small>
                                <small>${invasion.totalMobsKilled}/${invasion.totalMobsSpawned}</small>
                            </div>
                            <div class="progress">
                                <div class="progress-bar bg-success" style="width: ${mobsProgress}%"></div>
                            </div>
                        </div>
                        
                        <div class="d-flex justify-content-between align-items-center">
                            <small class="text-muted">
                                <i class="bi bi-people-fill"></i> ${Object.keys(invasion.playerKills || {}).length} participantes
                            </small>
                            <div>
                                <button class="btn btn-sm btn-outline-info" onclick="showInvasionDetails('${invasion.sessionId}')">
                                    <i class="bi bi-info-circle"></i> Detalles
                                </button>
                                <button class="btn btn-sm btn-outline-danger" onclick="stopInvasionConfirm('${invasion.sessionId}')">
                                    <i class="bi bi-stop-fill"></i> Detener
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    });
    
    html += '</div>';
    container.innerHTML = html;
}

async function stopInvasionConfirm(sessionId) {
    if (!confirm('¿Estás seguro de que quieres detener esta invasión?')) {
        return;
    }
    
    try {
        const response = await fetch('/api/rpg/invasions/stop', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId })
        });
        
        const data = await response.json();
        
        if (data.success) {
            alert('Invasión detenida correctamente');
            loadActiveInvasions();
        } else {
            alert('Error: ' + data.message);
        }
    } catch (error) {
        alert('Error al detener invasión: ' + error.message);
    }
}

// =====================================================
// CONFIGURACIÓN
// =====================================================

async function loadInvasionsConfig() {
    try {
        const response = await fetch('/api/rpg/invasions/config');
        const data = await response.json();
        
        if (!data.success) {
            throw new Error(data.message);
        }
        
        currentConfig = data.config;
        document.getElementById('configTextarea').value = JSON.stringify(data.config, null, 2);
        
        // Actualizar select de invasiones en modal
        updateInvasionSelect(data.config.invasions);
    } catch (error) {
        console.error('Error loading config:', error);
        document.getElementById('configTextarea').value = `Error: ${error.message}`;
    }
}

function updateInvasionSelect(invasions) {
    const select = document.getElementById('invasionSelect');
    if (!select) return;
    
    select.innerHTML = '<option value="">Seleccionar invasión...</option>';
    
    invasions.forEach(invasion => {
        if (invasion.enabled) {
            const option = document.createElement('option');
            option.value = invasion.invasionId;
            option.textContent = invasion.displayName || invasion.invasionId;
            option.dataset.invasion = JSON.stringify(invasion);
            select.appendChild(option);
        }
    });
}

async function saveInvasionsConfig() {
    try {
        const configText = document.getElementById('configTextarea').value;
        const config = JSON.parse(configText);
        
        const response = await fetch('/api/rpg/invasions/config', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(config)
        });
        
        const data = await response.json();
        
        if (data.success) {
            alert('Configuración guardada. Recarga el plugin o reinicia el servidor para aplicar cambios.');
            currentConfig = config;
        } else {
            alert('Error: ' + data.message);
        }
    } catch (error) {
        alert('Error al guardar configuración: ' + error.message);
    }
}

// =====================================================
// HISTORIAL
// =====================================================

async function loadInvasionsHistory() {
    try {
        const response = await fetch('/api/rpg/invasions/history');
        const data = await response.json();
        
        if (!data.success) {
            throw new Error(data.message);
        }
        
        renderHistory(data.history);
    } catch (error) {
        console.error('Error loading history:', error);
        document.getElementById('historyContainer').innerHTML = `
            <div class="alert alert-danger">
                <i class="bi bi-exclamation-circle"></i> Error: ${error.message}
            </div>
        `;
    }
}

function renderHistory(history) {
    const container = document.getElementById('historyContainer');
    
    if (history.length === 0) {
        container.innerHTML = `
            <div class="alert alert-secondary text-center">
                <i class="bi bi-inbox" style="font-size: 3rem;"></i>
                <h5 class="mt-3">Sin historial</h5>
                <p class="mb-0">No hay invasiones registradas aún.</p>
            </div>
        `;
        return;
    }
    
    let html = '<div class="table-responsive"><table class="table table-hover">';
    html += `
        <thead>
            <tr>
                <th>Invasión</th>
                <th>Mundo</th>
                <th>Inicio</th>
                <th>Duración</th>
                <th>Oleadas</th>
                <th>Mobs</th>
                <th>Estado</th>
                <th>Acciones</th>
            </tr>
        </thead>
        <tbody>
    `;
    
    history.forEach(invasion => {
        const startTime = new Date(invasion.startTime).toLocaleString();
        const duration = formatDuration(invasion.durationSeconds);
        const statusIcon = invasion.success ? 
            '<i class="bi bi-check-circle-fill text-success"></i>' :
            '<i class="bi bi-x-circle-fill text-danger"></i>';
        
        html += `
            <tr class="history-row" data-session="${invasion.sessionId}" data-search="${invasion.invasionId} ${invasion.worldName} ${invasion.status}">
                <td><strong>${invasion.invasionId}</strong></td>
                <td>${invasion.worldName}</td>
                <td><small>${startTime}</small></td>
                <td>${duration}</td>
                <td>${invasion.completedWaves}/${invasion.totalWaves}</td>
                <td>${invasion.totalMobsKilled}/${invasion.totalMobsSpawned}</td>
                <td>
                    ${statusIcon}
                    <span class="badge status-${invasion.status.toLowerCase()}">${invasion.status}</span>
                </td>
                <td>
                    <button class="btn btn-sm btn-outline-info" onclick='showHistoryDetails(${JSON.stringify(invasion)})'>
                        <i class="bi bi-eye"></i>
                    </button>
                </td>
            </tr>
        `;
    });
    
    html += '</tbody></table></div>';
    container.innerHTML = html;
}

function filterHistory() {
    const search = document.getElementById('historySearch').value.toLowerCase();
    const rows = document.querySelectorAll('.history-row');
    
    rows.forEach(row => {
        const searchText = row.dataset.search.toLowerCase();
        row.style.display = searchText.includes(search) ? '' : 'none';
    });
}

function showHistoryDetails(invasion) {
    const modal = new bootstrap.Modal(document.getElementById('invasionDetailsModal'));
    const content = document.getElementById('invasionDetailsContent');
    
    let html = `
        <div class="mb-3">
            <h6>Información General</h6>
            <table class="table table-sm">
                <tr><th>ID de Sesión:</th><td><code>${invasion.sessionId}</code></td></tr>
                <tr><th>Invasión:</th><td>${invasion.invasionId}</td></tr>
                <tr><th>Mundo:</th><td>${invasion.worldName}</td></tr>
                <tr><th>Inicio:</th><td>${new Date(invasion.startTime).toLocaleString()}</td></tr>
                <tr><th>Fin:</th><td>${invasion.endTime ? new Date(invasion.endTime).toLocaleString() : 'En curso'}</td></tr>
                <tr><th>Duración:</th><td>${formatDuration(invasion.durationSeconds)}</td></tr>
                <tr><th>Estado:</th><td><span class="badge status-${invasion.status.toLowerCase()}">${invasion.status}</span></td></tr>
            </table>
        </div>
        
        <div class="mb-3">
            <h6>Progreso</h6>
            <table class="table table-sm">
                <tr><th>Oleadas:</th><td>${invasion.completedWaves}/${invasion.totalWaves} (${(invasion.completedWaves/invasion.totalWaves*100).toFixed(0)}%)</td></tr>
                <tr><th>Mobs Eliminados:</th><td>${invasion.totalMobsKilled}/${invasion.totalMobsSpawned} (${(invasion.totalMobsKilled/invasion.totalMobsSpawned*100).toFixed(0)}%)</td></tr>
                <tr><th>Éxito:</th><td>${invasion.success ? '✅ Sí' : '❌ No'}</td></tr>
            </table>
        </div>
    `;
    
    if (invasion.participants && invasion.participants.length > 0) {
        html += `
            <div class="mb-3">
                <h6>Participantes (${invasion.participants.length})</h6>
                <div style="max-height: 200px; overflow-y: auto;">
        `;
        
        invasion.participants.forEach((participant, index) => {
            html += `
                <div class="participant-item">
                    <div>
                        <strong>${index + 1}. ${participant.name}</strong>
                        <br><small class="text-muted">${participant.uuid}</small>
                    </div>
                    <div>
                        <span class="badge bg-danger">${participant.kills} kills</span>
                    </div>
                </div>
            `;
        });
        
        html += '</div></div>';
    }
    
    content.innerHTML = html;
    modal.show();
}

// =====================================================
// ESTADÍSTICAS
// =====================================================

async function loadInvasionsStats() {
    try {
        const response = await fetch('/api/rpg/invasions/stats');
        const data = await response.json();
        
        if (!data.success) {
            throw new Error(data.message);
        }
        
        renderStats(data.stats);
    } catch (error) {
        console.error('Error loading stats:', error);
        document.getElementById('statsContainer').innerHTML = `
            <div class="alert alert-danger">
                <i class="bi bi-exclamation-circle"></i> Error: ${error.message}
            </div>
        `;
    }
}

function renderStats(stats) {
    const container = document.getElementById('statsContainer');
    
    let html = `
        <div class="row mb-4">
            <div class="col-md-3">
                <div class="stat-card">
                    <div class="stat-icon"><i class="bi bi-shield-exclamation text-danger"></i></div>
                    <div class="stat-value">${stats.totalInvasions}</div>
                    <div class="stat-label">Total Invasiones</div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card">
                    <div class="stat-icon"><i class="bi bi-check-circle text-success"></i></div>
                    <div class="stat-value">${stats.successRate}%</div>
                    <div class="stat-label">Tasa de Éxito</div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card">
                    <div class="stat-icon"><i class="bi bi-crosshair text-warning"></i></div>
                    <div class="stat-value">${stats.totalMobsKilled.toLocaleString()}</div>
                    <div class="stat-label">Mobs Eliminados</div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card">
                    <div class="stat-icon"><i class="bi bi-clock text-info"></i></div>
                    <div class="stat-value">${formatDuration(stats.avgDuration)}</div>
                    <div class="stat-label">Duración Promedio</div>
                </div>
            </div>
        </div>
    `;
    
    if (stats.topPlayers && stats.topPlayers.length > 0) {
        html += `
            <div class="card">
                <div class="card-header">
                    <i class="bi bi-trophy-fill text-warning"></i> Top 10 Cazadores de Invasiones
                </div>
                <div class="card-body">
        `;
        
        stats.topPlayers.forEach((player, index) => {
            const rank = index + 1;
            const rankClass = rank <= 3 ? `rank-${rank}` : 'rank-other';
            
            html += `
                <div class="leaderboard-item">
                    <div class="d-flex align-items-center">
                        <div class="leaderboard-rank ${rankClass}">${rank}</div>
                        <div class="ms-3">
                            <strong>${player.name}</strong>
                            <br><small class="text-muted">${player.uuid}</small>
                        </div>
                    </div>
                    <div>
                        <span class="badge bg-danger">${player.totalKills.toLocaleString()} kills</span>
                    </div>
                </div>
            `;
        });
        
        html += '</div></div>';
    }
    
    container.innerHTML = html;
}

// =====================================================
// MODAL INICIAR INVASIÓN
// =====================================================

function showStartInvasionModal() {
    const modal = new bootstrap.Modal(document.getElementById('startInvasionModal'));
    
    // Listener para cambio de selección
    document.getElementById('invasionSelect').onchange = function() {
        const invasionData = this.options[this.selectedIndex].dataset.invasion;
        if (invasionData) {
            showInvasionPreview(JSON.parse(invasionData));
        }
    };
    
    modal.show();
}

function showInvasionPreview(invasion) {
    const preview = document.getElementById('invasionPreview');
    const content = document.getElementById('invasionPreviewContent');
    
    let html = `
        <p><strong>Nombre:</strong> ${invasion.displayName}</p>
        <p><strong>Descripción:</strong> ${invasion.description}</p>
        <p><strong>Mundos:</strong> ${invasion.targetWorlds.join(', ')}</p>
        <p><strong>Oleadas:</strong> ${invasion.waves.length}</p>
        <p><strong>Duración estimada:</strong> ${invasion.schedule.durationMinutes} minutos</p>
    `;
    
    content.innerHTML = html;
    preview.classList.remove('d-none');
}

async function startInvasion() {
    const invasionId = document.getElementById('invasionSelect').value;
    
    if (!invasionId) {
        alert('Selecciona una invasión');
        return;
    }
    
    try {
        const response = await fetch('/api/rpg/invasions/start', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ invasionId })
        });
        
        const data = await response.json();
        
        if (data.success) {
            alert('Invasión iniciada correctamente');
            bootstrap.Modal.getInstance(document.getElementById('startInvasionModal')).hide();
            
            // Cambiar a tab activas
            document.getElementById('active-tab').click();
            loadActiveInvasions();
        } else {
            alert('Error: ' + data.message);
        }
    } catch (error) {
        alert('Error al iniciar invasión: ' + error.message);
    }
}

function showInvasionDetails(sessionId) {
    // Esta función buscaría los detalles de la invasión activa
    // Por simplicidad, mostraremos un placeholder
    alert('Detalles de invasión activa: ' + sessionId);
}

// =====================================================
// UTILIDADES
// =====================================================

function formatDuration(seconds) {
    if (!seconds) return '0s';
    
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = Math.floor(seconds % 60);
    
    let parts = [];
    if (hours > 0) parts.push(`${hours}h`);
    if (minutes > 0) parts.push(`${minutes}m`);
    if (secs > 0 || parts.length === 0) parts.push(`${secs}s`);
    
    return parts.join(' ');
}

function refreshAllData() {
    loadInvasionsConfig();
    loadActiveInvasions();
    loadInvasionsHistory();
    loadInvasionsStats();
}

// Limpiar interval al salir
window.addEventListener('beforeunload', () => {
    if (refreshInterval) {
        clearInterval(refreshInterval);
    }
});
