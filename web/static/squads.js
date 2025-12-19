// Estado Global
let autoRefreshInterval = null;

function setupAutoRefresh() {
    autoRefreshInterval = setInterval(() => {
        const activeTab = document.querySelector('.nav-link.active');
        if (activeTab) {
            const tabName = activeTab.getAttribute('data-bs-target').substring(1);
            switch (tabName) {
                case 'directory':
                    loadSquadsDirectory();
                    break;
                case 'history':
                    loadSquadsHistory();
                    break;
                case 'stats':
                    loadSquadsStats();
                    break;
            }
        }
    }, 10000);
}

function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleString('es-ES');
}

function formatCurrency(value) {
    return value.toLocaleString('es-ES');
}

function showNotification(message, type = 'info') {
    const alertClass = `alert-${type}`;
    const icon = type === 'success' ? 'fa-check-circle' : 
                 type === 'danger' ? 'fa-exclamation-circle' : 
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

// ===== TAB: DIRECTORIO =====
async function loadSquadsDirectory() {
    try {
        const response = await fetch('/api/rpg/squads/list');
        const data = await response.json();

        if (!response.ok) {
            showNotification('Error al cargar escuadras', 'danger');
            return;
        }

        let html = '';
        if (data.squads && data.squads.length > 0) {
            data.squads.forEach(squad => {
                html += `
                    <div class="squad-card">
                        <h5>
                            <i class="fas fa-users"></i>
                            ${squad.name}
                        </h5>
                        <p class="squad-description">${squad.description || 'Sin descripción'}</p>
                        <div class="squad-level">
                            <i class="fas fa-chart-line"></i> Nivel ${squad.level}
                        </div>
                        <div class="squad-stats">
                            <div class="squad-stat-item">
                                <div class="squad-stat-label">Miembros</div>
                                <div class="squad-stat-value">${squad.member_count}</div>
                            </div>
                            <div class="squad-stat-item">
                                <div class="squad-stat-label">Tesorería</div>
                                <div class="squad-stat-value">${formatCurrency(squad.treasury_coins)}</div>
                            </div>
                            <div class="squad-stat-item">
                                <div class="squad-stat-label">XP Tesoro</div>
                                <div class="squad-stat-value">${formatCurrency(squad.treasury_xp)}</div>
                            </div>
                            <div class="squad-stat-item">
                                <div class="squad-stat-label">Contribuciones</div>
                                <div class="squad-stat-value">${formatCurrency(squad.total_contributions)}</div>
                            </div>
                        </div>
                        <div class="squad-buttons">
                            <button class="btn btn-primary" onclick="openSquadDetails('${squad.id}')">
                                <i class="fas fa-eye"></i> Ver Detalles
                            </button>
                        </div>
                    </div>
                `;
            });
        } else {
            html = '<div class="alert alert-info"><i class="fas fa-info-circle"></i> No hay escuadras activas</div>';
        }

        document.getElementById('squadsList').innerHTML = html;
    } catch (error) {
        console.error('Error:', error);
        showNotification('Error al cargar directorio', 'danger');
    }
}

function openSquadDetails(squadId) {
    const modal = new bootstrap.Modal(document.getElementById('squadDetailsModal'));
    document.getElementById('squadDetailsContent').innerHTML = '<div class="text-center"><i class="fas fa-spinner fa-spin"></i> Cargando...</div>';
    modal.show();

    fetch(`/api/rpg/squads/${squadId}/details`)
        .then(r => r.json())
        .then(data => {
            if (data.squad) {
                let html = `
                    <h6><i class="fas fa-info-circle"></i> Información General</h6>
                    <div class="row mb-3">
                        <div class="col-md-6">
                            <strong>Nombre:</strong> ${data.squad.name}<br>
                            <strong>Descripción:</strong> ${data.squad.description || 'N/A'}<br>
                            <strong>Nivel:</strong> ${data.squad.level}/5
                        </div>
                        <div class="col-md-6">
                            <strong>Capitán UUID:</strong> <code>${data.squad.captain_uuid}</code><br>
                            <strong>Tesorería:</strong> ${formatCurrency(data.squad.treasury_coins)} monedas<br>
                            <strong>XP Tesoro:</strong> ${formatCurrency(data.squad.treasury_xp)}
                        </div>
                    </div>

                    <h6><i class="fas fa-users"></i> Miembros (${data.members.length})</h6>
                    <div class="table-responsive mb-3">
                        <table class="table table-dark table-sm">
                            <thead>
                                <tr>
                                    <th>Nombre</th>
                                    <th>Rol</th>
                                    <th>Contribuciones</th>
                                    <th>Se Unió</th>
                                </tr>
                            </thead>
                            <tbody>
                `;

                data.members.forEach(member => {
                    html += `
                        <tr>
                            <td>${member.player_name}</td>
                            <td><span class="badge badge-primary">${member.role}</span></td>
                            <td>${formatCurrency(member.contributions_coins)} monedas</td>
                            <td>${formatDate(member.joined_at)}</td>
                        </tr>
                    `;
                });

                html += `
                            </tbody>
                        </table>
                    </div>

                    <h6><i class="fas fa-history"></i> Historial de Eventos (Últimos 10)</h6>
                    <div class="small" style="max-height: 200px; overflow-y: auto;">
                `;

                data.logs.forEach(log => {
                    html += `
                        <div class="mb-2 pb-2" style="border-bottom: 1px solid rgba(255,255,255,0.1);">
                            <strong>${log.event_type}:</strong> ${log.description}<br>
                            <small class="text-muted">${formatDate(log.timestamp)}</small>
                        </div>
                    `;
                });

                html += `
                    </div>
                `;

                document.getElementById('squadDetailsContent').innerHTML = html;
            }
        })
        .catch(error => {
            console.error(error);
            document.getElementById('squadDetailsContent').innerHTML = '<div class="alert alert-danger">Error al cargar detalles</div>';
        });
}

// ===== TAB: CONFIGURACIÓN =====
async function loadSquadsConfig() {
    try {
        const response = await fetch('/api/rpg/squads/config');
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

async function saveSquadsConfig() {
    try {
        const configText = document.getElementById('configEditor').value;
        const config = JSON.parse(configText);

        const response = await fetch('/api/rpg/squads/config', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(config)
        });

        if (response.ok) {
            showNotification('Configuración guardada correctamente', 'success');
            loadSquadsDirectory();
        } else {
            showNotification('Error al guardar configuración', 'danger');
        }
    } catch (error) {
        console.error(error);
        showNotification('Error: JSON inválido', 'danger');
    }
}

// ===== TAB: HISTORIAL =====
async function loadSquadsHistory() {
    try {
        const response = await fetch('/api/rpg/squads/history');
        const data = await response.json();

        if (!response.ok) {
            showNotification('Error al cargar historial', 'danger');
            return;
        }

        let html = '';
        if (data.history && data.history.length > 0) {
            data.history.forEach(squad => {
                html += `
                    <tr>
                        <td><strong>${squad.name}</strong></td>
                        <td><code>${squad.captain_uuid.substring(0, 8)}...</code></td>
                        <td><span class="badge badge-primary">Nivel ${squad.level}</span></td>
                        <td>${formatDate(squad.created_at)}</td>
                        <td>${formatDate(squad.disbanded_at)}</td>
                        <td>${formatCurrency(squad.treasury_coins)}</td>
                    </tr>
                `;
            });
        } else {
            html = '<tr><td colspan="6" class="text-center text-muted">No hay historial</td></tr>';
        }

        document.getElementById('historyTableBody').innerHTML = html;
    } catch (error) {
        console.error(error);
        showNotification('Error al cargar historial', 'danger');
    }
}

// ===== TAB: ESTADÍSTICAS =====
async function loadSquadsStats() {
    try {
        const response = await fetch('/api/rpg/squads/stats');
        const data = await response.json();

        if (!response.ok) {
            showNotification('Error al cargar estadísticas', 'danger');
            return;
        }

        document.getElementById('statTotalSquads').textContent = data.stats.total_squads || 0;
        document.getElementById('statActiveSquads').textContent = data.stats.active_squads || 0;
        document.getElementById('statTotalMembers').textContent = data.stats.total_members || 0;
        document.getElementById('statTotalTreasury').textContent = formatCurrency(data.stats.total_treasury || 0);

        let html = '';
        if (data.top_squads && data.top_squads.length > 0) {
            data.top_squads.forEach((squad, index) => {
                html += `
                    <tr>
                        <td><strong>#${index + 1}</strong></td>
                        <td>${squad.name}</td>
                        <td><span class="badge badge-primary">Nivel ${squad.level}</span></td>
                        <td><span class="badge badge-info">${squad.member_count} miembros</span></td>
                        <td>${formatCurrency(squad.treasury_coins)}</td>
                    </tr>
                `;
            });
        } else {
            html = '<tr><td colspan="5" class="text-center text-muted">Sin datos</td></tr>';
        }

        document.getElementById('topSquadsTable').innerHTML = html;
    } catch (error) {
        console.error(error);
        showNotification('Error al cargar estadísticas', 'danger');
    }
}

// ===== INICIALIZACIÓN =====
document.addEventListener('DOMContentLoaded', () => {
    loadSquadsDirectory();
    loadSquadsConfig();
    loadSquadsHistory();
    loadSquadsStats();

    document.getElementById('refreshAllBtn').addEventListener('click', () => {
        loadSquadsDirectory();
        loadSquadsConfig();
        loadSquadsHistory();
        loadSquadsStats();
        showNotification('Datos actualizados', 'success');
    });

    document.getElementById('saveConfigBtn').addEventListener('click', saveSquadsConfig);
    document.getElementById('loadConfigBtn').addEventListener('click', loadSquadsConfig);

    document.getElementById('directory-tab').addEventListener('click', loadSquadsDirectory);
    document.getElementById('history-tab').addEventListener('click', loadSquadsHistory);
    document.getElementById('stats-tab').addEventListener('click', loadSquadsStats);

    setupAutoRefresh();
});

window.addEventListener('beforeunload', () => {
    if (autoRefreshInterval) {
        clearInterval(autoRefreshInterval);
    }
});
