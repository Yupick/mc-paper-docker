/**
 * Sprint 5: Events Manager JavaScript
 * Manejo de eventos via API REST
 */

let currentEventId = null;
let eventMobsCount = 0;
let eventDropsCount = 0;

// ===================== CARGA DE EVENTOS =====================

function loadEvents() {
    $.ajax({
        url: '/api/events',
        method: 'GET',
        success: function(response) {
            if (response.success) {
                displayEvents(response.events);
                $('#total-events-count').text(response.events.length);
            } else {
                showError('Error al cargar eventos: ' + response.error);
            }
        },
        error: function(xhr) {
            showError('Error de conexión al cargar eventos');
            $('#events-tbody').html('<tr><td colspan="7" class="text-center text-danger">Error al cargar eventos</td></tr>');
        }
    });
}

function displayEvents(events) {
    const tbody = $('#events-tbody');
    tbody.empty();
    
    if (events.length === 0) {
        tbody.html('<tr><td colspan="7" class="text-center">No hay eventos configurados</td></tr>');
        return;
    }
    
    events.forEach(function(event) {
        const statusBadge = event.enabled ? 
            '<span class="badge badge-success">Habilitado</span>' : 
            '<span class="badge badge-secondary">Deshabilitado</span>';
        
        const dates = `${formatDate(event.startDate)} - ${formatDate(event.endDate)}`;
        const mobsCount = event.customMobs ? event.customMobs.length : 0;
        
        const row = `
            <tr>
                <td>${event.id}</td>
                <td><strong>${event.name}</strong></td>
                <td>${event.description || '-'}</td>
                <td>${statusBadge}</td>
                <td><small>${dates}</small></td>
                <td><span class="badge badge-info">${mobsCount} mobs</span></td>
                <td>
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-info" onclick="viewEvent('${event.id}')" title="Ver">
                            <i class="fas fa-eye"></i>
                        </button>
                        <button class="btn btn-primary" onclick="editEvent('${event.id}')" title="Editar">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn btn-success" onclick="startEvent('${event.id}')" title="Iniciar">
                            <i class="fas fa-play"></i>
                        </button>
                        <button class="btn btn-warning" onclick="stopEvent('${event.id}')" title="Detener">
                            <i class="fas fa-stop"></i>
                        </button>
                        <button class="btn btn-secondary" onclick="validateEvent('${event.id}')" title="Validar">
                            <i class="fas fa-check-circle"></i>
                        </button>
                        <button class="btn btn-danger" onclick="deleteEvent('${event.id}')" title="Eliminar">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
        tbody.append(row);
    });
}

function loadActiveEvents() {
    $.ajax({
        url: '/api/events/active',
        method: 'GET',
        success: function(response) {
            if (response.success) {
                $('#active-events-count').text(response.active_events.length);
            }
        }
    });
}

// ===================== CRUD EVENTOS =====================

function openCreateEventModal() {
    currentEventId = null;
    $('#eventModalTitle').text('Crear Evento');
    $('#event-form')[0].reset();
    $('#event-id').prop('disabled', false);
    $('#event-enabled').prop('checked', true);
    $('#event-autostart').prop('checked', false);
    $('#event-mobs-container').empty();
    $('#event-drops-container').empty();
    eventMobsCount = 0;
    eventDropsCount = 0;
    $('#eventModal').modal('show');
}

function editEvent(eventId) {
    currentEventId = eventId;
    $('#eventModalTitle').text('Editar Evento');
    
    $.ajax({
        url: `/api/events/${eventId}`,
        method: 'GET',
        success: function(response) {
            if (response.success) {
                const event = response.event;
                
                // Llenar formulario
                $('#event-id').val(event.id).prop('disabled', true);
                $('#event-name').val(event.name);
                $('#event-description').val(event.description);
                $('#event-start-date').val(formatDateTimeLocal(event.startDate));
                $('#event-end-date').val(formatDateTimeLocal(event.endDate));
                $('#event-enabled').prop('checked', event.enabled);
                $('#event-autostart').prop('checked', event.autoStart);
                
                // Recompensas
                if (event.rewards) {
                    $('#event-coins-kill').val(event.rewards.eventCoinsPerKill || 10);
                    $('#event-xp-multiplier').val(event.rewards.bonusXPMultiplier || 1.5);
                    $('#event-coins-boss').val(event.rewards.bossEventCoins || 100);
                }
                
                // Mobs
                $('#event-mobs-container').empty();
                eventMobsCount = 0;
                if (event.customMobs) {
                    event.customMobs.forEach(mob => addEventMob(mob));
                }
                
                // Drops
                $('#event-drops-container').empty();
                eventDropsCount = 0;
                if (event.exclusiveDrops) {
                    event.exclusiveDrops.forEach(drop => addEventDrop(drop));
                }
                
                $('#eventModal').modal('show');
            }
        },
        error: function() {
            showError('Error al cargar evento');
        }
    });
}

function saveEvent() {
    // Recopilar datos del formulario
    const eventData = {
        id: $('#event-id').val(),
        name: $('#event-name').val(),
        description: $('#event-description').val(),
        startDate: $('#event-start-date').val(),
        endDate: $('#event-end-date').val(),
        enabled: $('#event-enabled').is(':checked'),
        autoStart: $('#event-autostart').is(':checked'),
        customMobs: collectEventMobs(),
        exclusiveDrops: collectEventDrops(),
        rewards: {
            eventCoinsPerKill: parseInt($('#event-coins-kill').val()),
            bonusXPMultiplier: parseFloat($('#event-xp-multiplier').val()),
            bossEventCoins: parseInt($('#event-coins-boss').val())
        }
    };
    
    // Validación básica
    if (!eventData.id || !eventData.name) {
        showError('ID y Nombre son obligatorios');
        return;
    }
    
    const url = currentEventId ? `/api/events/${currentEventId}` : '/api/events';
    const method = currentEventId ? 'PUT' : 'POST';
    
    $.ajax({
        url: url,
        method: method,
        contentType: 'application/json',
        data: JSON.stringify(eventData),
        success: function(response) {
            if (response.success) {
                showSuccess(currentEventId ? 'Evento actualizado' : 'Evento creado');
                $('#eventModal').modal('hide');
                loadEvents();
            } else {
                showError('Error: ' + response.error);
            }
        },
        error: function(xhr) {
            showError('Error al guardar evento');
        }
    });
}

function deleteEvent(eventId) {
    if (!confirm(`¿Eliminar evento ${eventId}?`)) return;
    
    $.ajax({
        url: `/api/events/${eventId}`,
        method: 'DELETE',
        success: function(response) {
            if (response.success) {
                showSuccess('Evento eliminado');
                loadEvents();
            } else {
                showError('Error: ' + response.error);
            }
        },
        error: function() {
            showError('Error al eliminar evento');
        }
    });
}

// ===================== CONTROL DE EVENTOS =====================

function startEvent(eventId) {
    if (!confirm(`¿Iniciar evento ${eventId}?`)) return;
    
    $.ajax({
        url: `/api/events/${eventId}/start`,
        method: 'POST',
        success: function(response) {
            if (response.success) {
                showSuccess(`Evento ${eventId} iniciado`);
                loadEvents();
                loadActiveEvents();
            } else {
                showError('Error: ' + response.error);
            }
        },
        error: function() {
            showError('Error al iniciar evento');
        }
    });
}

function stopEvent(eventId) {
    if (!confirm(`¿Detener evento ${eventId}?`)) return;
    
    $.ajax({
        url: `/api/events/${eventId}/stop`,
        method: 'POST',
        success: function(response) {
            if (response.success) {
                showSuccess(`Evento ${eventId} detenido`);
                loadEvents();
                loadActiveEvents();
            } else {
                showError('Error: ' + response.error);
            }
        },
        error: function() {
            showError('Error al detener evento');
        }
    });
}

function validateEvent(eventId) {
    $.ajax({
        url: `/api/events/${eventId}/validate`,
        method: 'GET',
        success: function(response) {
            if (response.success) {
                const validation = response.validation;
                if (validation.valid) {
                    showSuccess(`Evento ${eventId} validado correctamente. ${validation.mobs_existing}/${validation.mobs_configured} mobs encontrados.`);
                } else {
                    showWarning(`Validación fallida. Mobs faltantes: ${validation.missing_mobs.join(', ')}`);
                }
            }
        },
        error: function() {
            showError('Error al validar evento');
        }
    });
}

function reloadEvents() {
    $.ajax({
        url: '/api/events/reload',
        method: 'POST',
        success: function(response) {
            if (response.success) {
                showSuccess('Configuración de eventos recargada');
                loadEvents();
            }
        },
        error: function() {
            showError('Error al recargar configuración');
        }
    });
}

// ===================== GESTIÓN DE MOBS =====================

function addEventMob(mobData = null) {
    eventMobsCount++;
    const mobId = `mob-${eventMobsCount}`;
    
    const mobHtml = `
        <div class="card mb-2" id="${mobId}">
            <div class="card-body">
                <div class="row">
                    <div class="col-md-4">
                        <input type="text" class="form-control mob-id" placeholder="ID del Mob" value="${mobData?.mobId || ''}">
                    </div>
                    <div class="col-md-3">
                        <input type="number" class="form-control mob-spawn-chance" placeholder="Chance (%)" step="0.01" value="${mobData?.spawnChance || 1.0}">
                    </div>
                    <div class="col-md-3">
                        <input type="number" class="form-control mob-spawn-radius" placeholder="Radio" value="${mobData?.spawnRadius || 50}">
                    </div>
                    <div class="col-md-2">
                        <button type="button" class="btn btn-danger btn-block" onclick="$('#${mobId}').remove()">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    $('#event-mobs-container').append(mobHtml);
}

function collectEventMobs() {
    const mobs = [];
    $('#event-mobs-container .card').each(function() {
        mobs.push({
            mobId: $(this).find('.mob-id').val(),
            spawnChance: parseFloat($(this).find('.mob-spawn-chance').val()),
            spawnRadius: parseInt($(this).find('.mob-spawn-radius').val())
        });
    });
    return mobs;
}

// ===================== GESTIÓN DE DROPS =====================

function addEventDrop(dropData = null) {
    eventDropsCount++;
    const dropId = `drop-${eventDropsCount}`;
    
    const dropHtml = `
        <div class="card mb-2" id="${dropId}">
            <div class="card-body">
                <div class="row">
                    <div class="col-md-3">
                        <input type="text" class="form-control drop-item" placeholder="Nombre del Item" value="${dropData?.itemName || ''}">
                    </div>
                    <div class="col-md-3">
                        <input type="number" class="form-control drop-chance" placeholder="Chance (%)" step="0.01" value="${dropData?.dropChance || 0.1}">
                    </div>
                    <div class="col-md-2">
                        <input type="number" class="form-control drop-min" placeholder="Min" value="${dropData?.minAmount || 1}">
                    </div>
                    <div class="col-md-2">
                        <input type="number" class="form-control drop-max" placeholder="Max" value="${dropData?.maxAmount || 1}">
                    </div>
                    <div class="col-md-2">
                        <button type="button" class="btn btn-danger btn-block" onclick="$('#${dropId}').remove()">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    $('#event-drops-container').append(dropHtml);
}

function collectEventDrops() {
    const drops = [];
    $('#event-drops-container .card').each(function() {
        drops.push({
            itemName: $(this).find('.drop-item').val(),
            dropChance: parseFloat($(this).find('.drop-chance').val()),
            minAmount: parseInt($(this).find('.drop-min').val()),
            maxAmount: parseInt($(this).find('.drop-max').val())
        });
    });
    return drops;
}

// ===================== UTILIDADES =====================

function formatDate(dateStr) {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleDateString('es-ES');
}

function formatDateTimeLocal(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toISOString().slice(0, 16);
}

function showSuccess(message) {
    // Usar sistema de notificaciones existente o alert
    if (typeof toastr !== 'undefined') {
        toastr.success(message);
    } else {
        alert(message);
    }
}

function showError(message) {
    if (typeof toastr !== 'undefined') {
        toastr.error(message);
    } else {
        alert('Error: ' + message);
    }
}

function showWarning(message) {
    if (typeof toastr !== 'undefined') {
        toastr.warning(message);
    } else {
        alert('Advertencia: ' + message);
    }
}

function viewEvent(eventId) {
    window.location.href = `/event-details/${eventId}`;
}
