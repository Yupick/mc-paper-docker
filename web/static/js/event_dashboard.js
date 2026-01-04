/**
 * Sprint 5: Event Dashboard JavaScript
 * Dashboard en tiempo real con Chart.js
 */

let participationChart = null;
let killsChart = null;

// ===================== CARGA PRINCIPAL =====================

function loadDashboard() {
    loadActiveEventsStatus();
    loadStatistics();
    loadLeaderboard();
    loadHistory();
    loadCharts();
}

// ===================== ESTADÍSTICAS =====================

function loadActiveEventsStatus() {
    $.ajax({
        url: '/api/events/active',
        method: 'GET',
        success: function(response) {
            if (response.success) {
                displayActiveEvents(response.active_events);
                $('#active-events-count').text(response.active_events.length);
            }
        },
        error: function() {
            $('#active-events-container').html('<div class="alert alert-danger">Error al cargar eventos activos</div>');
        }
    });
}

function displayActiveEvents(events) {
    const container = $('#active-events-container');
    container.empty();
    
    if (events.length === 0) {
        container.html('<div class="alert alert-info"><i class="fas fa-info-circle"></i> No hay eventos activos en este momento</div>');
        return;
    }
    
    events.forEach(function(event) {
        const eventCard = `
            <div class="card mb-3 border-success">
                <div class="card-body">
                    <div class="row align-items-center">
                        <div class="col-md-6">
                            <h5 class="card-title mb-1">
                                <i class="fas fa-star text-warning"></i> ${event.name}
                                <span class="badge badge-success ml-2">ACTIVO</span>
                            </h5>
                            <p class="text-muted mb-2">${event.description || 'Sin descripción'}</p>
                            <small>
                                <i class="fas fa-calendar"></i> ${formatDateTime(event.startDate)} - ${formatDateTime(event.endDate)}
                            </small>
                        </div>
                        <div class="col-md-3 text-center">
                            <h6 class="text-muted mb-1">Mobs del Evento</h6>
                            <h3>${event.customMobs ? event.customMobs.length : 0}</h3>
                            <small class="text-muted">tipos configurados</small>
                        </div>
                        <div class="col-md-3 text-center">
                            <h6 class="text-muted mb-1">Recompensas</h6>
                            <p class="mb-0">
                                <i class="fas fa-coins text-warning"></i> ${event.rewards?.eventCoinsPerKill || 10} por kill<br>
                                <i class="fas fa-star text-info"></i> ${event.rewards?.bonusXPMultiplier || 1}x XP
                            </p>
                        </div>
                    </div>
                    <div class="progress mt-3" style="height: 5px;">
                        <div class="progress-bar bg-success progress-bar-striped progress-bar-animated" 
                             role="progressbar" style="width: ${getEventProgress(event)}%"></div>
                    </div>
                </div>
            </div>
        `;
        container.append(eventCard);
    });
}

function loadStatistics() {
    let totalParticipants = 0;
    let totalKills = 0;
    let totalCoins = 0;
    
    // Obtener estadísticas de todos los eventos activos
    $.ajax({
        url: '/api/events',
        method: 'GET',
        success: function(response) {
            if (response.success && response.events) {
                const promises = response.events.map(event => 
                    $.ajax({url: `/api/events/${event.id}/stats`, method: 'GET'})
                );
                
                Promise.all(promises).then(results => {
                    results.forEach(result => {
                        if (result.success && result.stats) {
                            totalParticipants += result.stats.unique_participants || 0;
                            totalKills += result.stats.total_kills || 0;
                            totalCoins += result.stats.total_coins_distributed || 0;
                        }
                    });
                    
                    $('#total-participants-count').text(totalParticipants);
                    $('#total-kills-count').text(totalKills);
                    $('#total-coins-count').text(totalCoins.toLocaleString());
                });
            }
        }
    });
}

function loadLeaderboard() {
    $.ajax({
        url: '/api/events/leaderboard',
        method: 'GET',
        success: function(response) {
            if (response.success) {
                displayLeaderboard(response.leaderboard);
            }
        },
        error: function() {
            $('#leaderboard-tbody').html('<tr><td colspan="5" class="text-center text-danger">Error al cargar leaderboard</td></tr>');
        }
    });
}

function displayLeaderboard(leaderboard) {
    const tbody = $('#leaderboard-tbody');
    tbody.empty();
    
    if (!leaderboard || leaderboard.length === 0) {
        tbody.html('<tr><td colspan="5" class="text-center text-muted">No hay datos de leaderboard</td></tr>');
        return;
    }
    
    leaderboard.forEach(function(entry, index) {
        const position = index + 1;
        const medal = position === 1 ? '<i class="fas fa-trophy text-warning"></i>' :
                     position === 2 ? '<i class="fas fa-medal" style="color: silver;"></i>' :
                     position === 3 ? '<i class="fas fa-medal" style="color: #cd7f32;"></i>' :
                     position;
        
        const row = `
            <tr ${position <= 3 ? 'class="font-weight-bold"' : ''}>
                <td class="text-center">${medal}</td>
                <td>${entry.player_name}</td>
                <td><span class="badge badge-warning">${entry.event_coins || 0}</span></td>
                <td><span class="badge badge-info">${entry.total_kills || 0}</span></td>
                <td><small>${entry.last_event || '-'}</small></td>
            </tr>
        `;
        tbody.append(row);
    });
}

function loadHistory() {
    // Cargar historial de eventos (últimos 10)
    $.ajax({
        url: '/api/events',
        method: 'GET',
        success: function(response) {
            if (response.success && response.events) {
                const promises = response.events.slice(0, 10).map(event => 
                    $.ajax({url: `/api/events/${event.id}/history`, method: 'GET'})
                        .then(historyRes => ({event, history: historyRes}))
                );
                
                Promise.all(promises).then(results => {
                    displayHistory(results);
                });
            }
        },
        error: function() {
            $('#history-tbody').html('<tr><td colspan="7" class="text-center text-danger">Error al cargar historial</td></tr>');
        }
    });
}

function displayHistory(historyData) {
    const tbody = $('#history-tbody');
    tbody.empty();
    
    if (!historyData || historyData.length === 0) {
        tbody.html('<tr><td colspan="7" class="text-center text-muted">No hay historial reciente</td></tr>');
        return;
    }
    
    historyData.forEach(function(data) {
        const event = data.event;
        const history = data.history.success ? data.history.history : [];
        
        if (history.length > 0) {
            history.forEach(h => {
                const statusBadge = h.status === 'completed' ? 
                    '<span class="badge badge-success">Completado</span>' : 
                    h.status === 'started' ? 
                    '<span class="badge badge-info">Iniciado</span>' :
                    '<span class="badge badge-secondary">Detenido</span>';
                
                const duration = h.end_time ? 
                    calculateDuration(h.start_time, h.end_time) : 
                    'En curso';
                
                const row = `
                    <tr>
                        <td>${event.name}</td>
                        <td>${statusBadge}</td>
                        <td><small>${formatDateTime(h.start_time)}</small></td>
                        <td><small>${h.end_time ? formatDateTime(h.end_time) : '-'}</small></td>
                        <td><span class="badge badge-info">${h.participants || 0}</span></td>
                        <td><span class="badge badge-danger">${h.total_kills || 0}</span></td>
                        <td><small>${duration}</small></td>
                    </tr>
                `;
                tbody.append(row);
            });
        }
    });
    
    if (tbody.children().length === 0) {
        tbody.html('<tr><td colspan="7" class="text-center text-muted">No hay eventos en el historial</td></tr>');
    }
}

// ===================== GRÁFICOS =====================

function loadCharts() {
    $.ajax({
        url: '/api/events',
        method: 'GET',
        success: function(response) {
            if (response.success && response.events) {
                const eventNames = [];
                const participationData = [];
                const killsData = [];
                
                const promises = response.events.slice(0, 8).map(event => 
                    $.ajax({url: `/api/events/${event.id}/stats`, method: 'GET'})
                        .then(statsRes => ({event, stats: statsRes}))
                );
                
                Promise.all(promises).then(results => {
                    results.forEach(result => {
                        eventNames.push(result.event.name);
                        participationData.push(result.stats.success ? result.stats.stats.unique_participants || 0 : 0);
                        killsData.push(result.stats.success ? result.stats.stats.total_kills || 0 : 0);
                    });
                    
                    renderParticipationChart(eventNames, participationData);
                    renderKillsChart(eventNames, killsData);
                });
            }
        }
    });
}

function renderParticipationChart(labels, data) {
    const ctx = document.getElementById('participationChart');
    
    if (participationChart) {
        participationChart.destroy();
    }
    
    participationChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: 'Jugadores Únicos',
                data: data,
                backgroundColor: 'rgba(54, 162, 235, 0.2)',
                borderColor: 'rgba(54, 162, 235, 1)',
                borderWidth: 2,
                fill: true,
                tension: 0.4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: {
                    display: true,
                    position: 'top'
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        precision: 0
                    }
                }
            }
        }
    });
}

function renderKillsChart(labels, data) {
    const ctx = document.getElementById('killsChart');
    
    if (killsChart) {
        killsChart.destroy();
    }
    
    killsChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Total de Kills',
                data: data,
                backgroundColor: [
                    'rgba(255, 99, 132, 0.7)',
                    'rgba(54, 162, 235, 0.7)',
                    'rgba(255, 206, 86, 0.7)',
                    'rgba(75, 192, 192, 0.7)',
                    'rgba(153, 102, 255, 0.7)',
                    'rgba(255, 159, 64, 0.7)',
                    'rgba(199, 199, 199, 0.7)',
                    'rgba(83, 102, 255, 0.7)'
                ],
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: {
                    display: true,
                    position: 'top'
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        precision: 0
                    }
                }
            }
        }
    });
}

// ===================== UTILIDADES =====================

function getEventProgress(event) {
    if (!event.startDate || !event.endDate) return 0;
    
    const start = new Date(event.startDate).getTime();
    const end = new Date(event.endDate).getTime();
    const now = Date.now();
    
    if (now < start) return 0;
    if (now > end) return 100;
    
    const total = end - start;
    const elapsed = now - start;
    return Math.round((elapsed / total) * 100);
}

function formatDateTime(dateStr) {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleString('es-ES', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function calculateDuration(startStr, endStr) {
    const start = new Date(startStr).getTime();
    const end = new Date(endStr).getTime();
    const diff = end - start;
    
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    
    if (hours > 0) {
        return `${hours}h ${minutes}m`;
    }
    return `${minutes}m`;
}
