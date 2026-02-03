// Events Panel JavaScript

let eventsConfig = null;
let activeEvents = [];
let autoRefreshInterval = null;

// ==========================================
// INICIALIZACIÓN
// ==========================================

document.addEventListener('DOMContentLoaded', function() {
    console.log('Events Panel cargado');
    
    // Cargar datos iniciales
    loadEventsConfig();
    loadActiveEvents();
    loadScheduledEvents();
    loadEventsHistory();
    loadEventsStats();
    
    // Event listeners
    document.getElementById('refreshAllBtn').addEventListener('click', refreshAll);
    document.getElementById('saveConfigBtn').addEventListener('click', saveEventsConfig);
    document.getElementById('loadConfigBtn').addEventListener('click', loadEventsConfig);
    document.getElementById('confirmStartEvent').addEventListener('click', confirmStartEvent);
    
    // Auto-refresh cada 10 segundos
    autoRefreshInterval = setInterval(() => {
        loadActiveEvents();
        loadScheduledEvents();
    }, 10000);
    
    // Tab changes
    document.querySelectorAll('[data-bs-toggle="tab"]').forEach(tab => {
        tab.addEventListener('shown.bs.tab', function(e) {
            const target = e.target.getAttribute('data-bs-target');
            if (target === '#history') {
                loadEventsHistory();
            } else if (target === '#stats') {
                loadEventsStats();
            }
        });
    });
});

// ==========================================
// FUNCIONES DE CARGA DE DATOS
// ==========================================

async function loadEventsConfig() {
    try {
        const response = await fetch('/api/rpg/events/config');
        const data = await response.json();
        
        if (data.success) {
            eventsConfig = data.config;
            document.getElementById('configEditor').value = JSON.stringify(data.config, null, 2);
            populateEventSelect();
        } else {
            showNotification('Error cargando configuración: ' + data.message, 'error');
        }
    } catch (error) {
        console.error('Error:', error);
        showNotification('Error conectando con el servidor', 'error');
    }
}

async function saveEventsConfig() {
    try {
        const configText = document.getElementById('configEditor').value;
        const config = JSON.parse(configText);
        
        const response = await fetch('/api/rpg/events/config', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(config)
        });
        
        const data = await response.json();
        
        if (data.success) {
            eventsConfig = config;
            populateEventSelect();
            showNotification('Configuración guardada exitosamente', 'success');
        } else {
            showNotification('Error guardando configuración: ' + data.message, 'error');
        }
    } catch (error) {
        console.error('Error:', error);
        if (error instanceof SyntaxError) {
            showNotification('Error: JSON inválido. Verifica la sintaxis.', 'error');
        } else {
            showNotification('Error guardando configuración', 'error');
        }
    }
}

async function loadActiveEvents() {
    try {
        const response = await fetch('/api/rpg/events/active');
        const data = await response.json();
        
        if (data.success) {
            activeEvents = data.events;
            renderActiveEvents(data.events);
        }
    } catch (error) {
        console.error('Error:', error);
    }
}

async function loadScheduledEvents() {
    if (!eventsConfig || !eventsConfig.events) return;
    
    const now = new Date();
    const scheduledEvents = eventsConfig.events.filter(event => {
        const startDate = new Date(event.startDate);
        const endDate = new Date(event.endDate);
        return event.enabled && now < startDate;
    });
    
    renderScheduledEvents(scheduledEvents);
}

async function loadEventsHistory() {
    try {
        const response = await fetch('/api/rpg/events/history');
        const data = await response.json();
        
        if (data.success) {
            renderEventsHistory(data.history);
        }
    } catch (error) {
        console.error('Error:', error);
    }
}

async function loadEventsStats() {
    try {
        const response = await fetch('/api/rpg/events/stats');
        const data = await response.json();
        
        if (data.success) {
            renderEventsStats(data.stats);
        }
    } catch (error) {
        console.error('Error:', error);
    }
}

// ==========================================
// FUNCIONES DE RENDERIZADO
// ==========================================

function renderActiveEvents(events) {
    const container = document.getElementById('activeEventsList');
    
    if (events.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-calendar-times"></i>
                <h5>No hay eventos activos</h5>
                <p>Inicia un evento manualmente o espera a que se active automáticamente.</p>
                <button class="btn btn-primary mt-3" onclick="openStartEventModal()">
                    <i class="fas fa-play"></i> Iniciar Evento
                </button>
            </div>
        `;
        return;
    }
    
    let html = '';
    events.forEach(event => {
        const startTime = new Date(event.startedAt);
        const duration = Math.floor((Date.now() - startTime.getTime()) / 1000 / 60);
        
        html += `
            <div class="event-card active">
                <div class="event-header">
                    <div class="event-title">
                        <i class="fas fa-star me-2"></i>${event.eventName}
                    </div>
                    <span class="event-status-badge active">
                        <i class="fas fa-circle me-1"></i>Activo
                    </span>
                </div>
                <div class="event-info">
                    <div class="event-info-item">
                        <div class="event-info-label">Mundo</div>
                        <div class="event-info-value">${event.world}</div>
                    </div>
                    <div class="event-info-item">
                        <div class="event-info-label">Duración</div>
                        <div class="event-info-value">${duration} min</div>
                    </div>
                    <div class="event-info-item">
                        <div class="event-info-label">Participantes</div>
                        <div class="event-info-value">${event.participants}</div>
                    </div>
                    <div class="event-info-item">
                        <div class="event-info-label">Kills</div>
                        <div class="event-info-value">${event.totalKills}</div>
                    </div>
                </div>
                <div class="mt-3">
                    <button class="btn btn-danger btn-sm" onclick="stopEvent('${event.eventId}')">
                        <i class="fas fa-stop"></i> Detener Evento
                    </button>
                </div>
            </div>
        `;
    });
    
    container.innerHTML = html;
}

function renderScheduledEvents(events) {
    const container = document.getElementById('scheduledEventsList');
    
    if (events.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-calendar-plus"></i>
                <h5>No hay eventos programados</h5>
                <p>Configura eventos con fechas futuras en la sección de Configuración.</p>
            </div>
        `;
        return;
    }
    
    let html = '';
    const now = new Date();
    
    events.forEach(event => {
        const startDate = new Date(event.startDate);
        const endDate = new Date(event.endDate);
        const daysUntil = Math.ceil((startDate - now) / 1000 / 60 / 60 / 24);
        
        html += `
            <div class="event-card upcoming">
                <div class="event-header">
                    <div class="event-title">
                        <i class="fas fa-calendar-check me-2"></i>${event.name}
                    </div>
                    <span class="event-status-badge upcoming">
                        <i class="fas fa-clock me-1"></i>Próximo
                    </span>
                </div>
                <div class="event-description">${event.description}</div>
                <div class="event-info">
                    <div class="event-info-item">
                        <div class="event-info-label">Inicia en</div>
                        <div class="event-info-value">${daysUntil} días</div>
                    </div>
                    <div class="event-info-item">
                        <div class="event-info-label">Fecha Inicio</div>
                        <div class="event-info-value">${formatDate(startDate)}</div>
                    </div>
                    <div class="event-info-item">
                        <div class="event-info-label">Fecha Fin</div>
                        <div class="event-info-value">${formatDate(endDate)}</div>
                    </div>
                    <div class="event-info-item">
                        <div class="event-info-label">Auto-inicio</div>
                        <div class="event-info-value">
                            ${event.autoStart ? '<i class="fas fa-check text-success"></i>' : '<i class="fas fa-times text-danger"></i>'}
                        </div>
                    </div>
                </div>
                <div class="mt-3">
                    <button class="btn btn-primary btn-sm" onclick="startEventNow('${event.id}')">
                        <i class="fas fa-play"></i> Iniciar Ahora
                    </button>
                </div>
            </div>
        `;
    });
    
    container.innerHTML = html;
}

function renderEventsHistory(history) {
    const tbody = document.getElementById('historyTableBody');
    
    if (history.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="9" class="text-center text-muted">
                    <i class="fas fa-inbox fa-2x mb-2"></i>
                    <p>No hay eventos en el historial</p>
                </td>
            </tr>
        `;
        return;
    }
    
    let html = '';
    history.forEach(event => {
        const startTime = new Date(event.startedAt);
        const endTime = event.endedAt ? new Date(event.endedAt) : null;
        const duration = endTime ? Math.floor((endTime - startTime) / 1000 / 60) : 'N/A';
        
        const statusClass = event.status === 'COMPLETED' ? 'success' : 'secondary';
        
        html += `
            <tr>
                <td>
                    <strong>${event.eventName}</strong><br>
                    <small class="text-muted">${event.eventId}</small>
                </td>
                <td>${event.world}</td>
                <td>${formatDateTime(startTime)}</td>
                <td>${endTime ? formatDateTime(endTime) : '-'}</td>
                <td>${duration} min</td>
                <td>${event.participants}</td>
                <td>${event.totalKills}</td>
                <td><span class="badge bg-${statusClass}">${event.status}</span></td>
                <td>
                    <button class="btn btn-sm btn-info" onclick="showEventDetails(${event.historyId})">
                        <i class="fas fa-eye"></i>
                    </button>
                </td>
            </tr>
        `;
    });
    
    tbody.innerHTML = html;
}

function renderEventsStats(stats) {
    document.getElementById('statTotalEvents').textContent = stats.totalEvents.toLocaleString();
    document.getElementById('statCompletedEvents').textContent = stats.completedEvents.toLocaleString();
    document.getElementById('statTotalKills').textContent = stats.totalKills.toLocaleString();
    document.getElementById('statTotalParticipants').textContent = stats.totalParticipants.toLocaleString();
    
    const tbody = document.getElementById('topPlayersTable');
    
    if (stats.topPlayers.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="4" class="text-center text-muted">
                    <i class="fas fa-trophy fa-2x mb-2"></i>
                    <p>No hay datos de jugadores</p>
                </td>
            </tr>
        `;
        return;
    }
    
    let html = '';
    stats.topPlayers.forEach((player, index) => {
        const rank = index + 1;
        let medal = '';
        if (rank === 1) medal = '<i class="fas fa-trophy text-warning"></i>';
        else if (rank === 2) medal = '<i class="fas fa-medal text-secondary"></i>';
        else if (rank === 3) medal = '<i class="fas fa-medal" style="color: #cd7f32;"></i>';
        
        html += `
            <tr>
                <td><strong>${rank}.</strong> ${medal}</td>
                <td>${player.name}</td>
                <td><span class="badge bg-danger">${player.totalKills}</span></td>
                <td><span class="badge bg-warning">${player.totalEventCoins}</span></td>
            </tr>
        `;
    });
    
    tbody.innerHTML = html;
}

// ==========================================
// ACCIONES
// ==========================================

async function startEventNow(eventId) {
    if (!confirm('¿Iniciar este evento ahora?')) return;
    
    try {
        const response = await fetch('/api/rpg/events/start', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ eventId: eventId, world: 'world' })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showNotification('Evento iniciado correctamente', 'success');
            setTimeout(() => {
                loadActiveEvents();
                loadScheduledEvents();
            }, 1000);
        } else {
            showNotification('Error: ' + data.message, 'error');
        }
    } catch (error) {
        console.error('Error:', error);
        showNotification('Error al iniciar evento', 'error');
    }
}

async function stopEvent(eventId) {
    if (!confirm('¿Detener este evento activo?')) return;
    
    try {
        const response = await fetch('/api/rpg/events/stop', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ eventId: eventId })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showNotification('Evento detenido correctamente', 'success');
            setTimeout(() => {
                loadActiveEvents();
            }, 1000);
        } else {
            showNotification('Error: ' + data.message, 'error');
        }
    } catch (error) {
        console.error('Error:', error);
        showNotification('Error al detener evento', 'error');
    }
}

function openStartEventModal() {
    const modal = new bootstrap.Modal(document.getElementById('startEventModal'));
    modal.show();
}

function populateEventSelect() {
    if (!eventsConfig || !eventsConfig.events) return;
    
    const select = document.getElementById('eventSelect');
    select.innerHTML = '';
    
    eventsConfig.events.forEach(event => {
        if (event.enabled) {
            const option = document.createElement('option');
            option.value = event.id;
            option.textContent = `${event.name} (${event.id})`;
            select.appendChild(option);
        }
    });
}

async function confirmStartEvent() {
    const eventId = document.getElementById('eventSelect').value;
    const world = document.getElementById('eventWorldInput').value;
    
    if (!eventId) {
        showNotification('Selecciona un evento', 'error');
        return;
    }
    
    try {
        const response = await fetch('/api/rpg/events/start', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ eventId: eventId, world: world })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showNotification('Evento iniciado correctamente', 'success');
            bootstrap.Modal.getInstance(document.getElementById('startEventModal')).hide();
            setTimeout(() => {
                loadActiveEvents();
                loadScheduledEvents();
            }, 1000);
        } else {
            showNotification('Error: ' + data.message, 'error');
        }
    } catch (error) {
        console.error('Error:', error);
        showNotification('Error al iniciar evento', 'error');
    }
}

function showEventDetails(historyId) {
    // TODO: Implementar modal con detalles completos del evento
    alert('Detalles del evento ID: ' + historyId + '\n\n(Por implementar)');
}

function refreshAll() {
    loadEventsConfig();
    loadActiveEvents();
    loadScheduledEvents();
    loadEventsHistory();
    loadEventsStats();
    showNotification('Datos actualizados', 'success');
}

// ==========================================
// UTILIDADES
// ==========================================

function formatDate(date) {
    return date.toLocaleDateString('es-ES', { 
        day: '2-digit', 
        month: 'short', 
        year: 'numeric' 
    });
}

function formatDateTime(date) {
    return date.toLocaleString('es-ES', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function showNotification(message, type = 'info') {
    const alertClass = type === 'success' ? 'alert-success' : 
                      type === 'error' ? 'alert-danger' : 'alert-info';
    
    const alert = document.createElement('div');
    alert.className = `alert ${alertClass} alert-dismissible fade show position-fixed top-0 end-0 m-3`;
    alert.style.zIndex = '9999';
    alert.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    document.body.appendChild(alert);
    
    setTimeout(() => {
        alert.remove();
    }, 5000);
}

// Cleanup al cerrar
window.addEventListener('beforeunload', () => {
    if (autoRefreshInterval) {
        clearInterval(autoRefreshInterval);
    }
});
