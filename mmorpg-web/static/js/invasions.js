/**
 * Sprint 5: Invasions Manager JavaScript
 */

let currentInvasionId = null;
let allInvasions = [];
let waveCount = 0;

function loadInvasions() {
    $.ajax({
        url: '/api/config/invasions',
        method: 'GET',
        success: function(response) {
            if (response.success) {
                allInvasions = Array.isArray(response.config) ? response.config : Object.values(response.config);
                displayInvasions(allInvasions);
                updateStats(allInvasions);
            }
        }
    });
}

function displayInvasions(invasions) {
    const tbody = $('#invasions-tbody');
    tbody.empty();
    
    if (invasions.length === 0) {
        tbody.html('<tr><td colspan="8" class="text-center">No hay invasiones</td></tr>');
        return;
    }
    
    invasions.forEach(invasion => {
        const statusBadge = invasion.active 
            ? '<span class="badge badge-success">Activa</span>' 
            : '<span class="badge badge-secondary">Inactiva</span>';
        const waves = (invasion.waves || []).length;
        
        tbody.append(`
            <tr>
                <td><code>${invasion.invasionId}</code></td>
                <td><strong>${invasion.displayName || invasion.invasionId}</strong></td>
                <td><span class="badge badge-danger">Nv ${invasion.level || 1}</span></td>
                <td><span class="badge badge-secondary">${invasion.world || 'world'}</span></td>
                <td><span class="badge badge-pill badge-warning">${waves}</span></td>
                <td>${invasion.duration || 15} min</td>
                <td>${statusBadge}</td>
                <td>
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-primary" onclick="editInvasion('${invasion.invasionId}')"><i class="fas fa-edit"></i></button>
                        <button class="btn btn-danger" onclick="deleteInvasion('${invasion.invasionId}')"><i class="fas fa-trash"></i></button>
                    </div>
                </td>
            </tr>
        `);
    });
}

function updateStats(invasions) {
    $('#total-invasions-count').text(invasions.length);
    $('#active-invasions-count').text(invasions.filter(i => i.active).length);
    $('#scheduled-invasions-count').text(invasions.filter(i => !i.active).length);
    
    const avgLevel = invasions.length > 0 
        ? Math.round(invasions.reduce((sum, i) => sum + (i.level || 1), 0) / invasions.length) 
        : 0;
    $('#avg-level').text(avgLevel);
}

function openCreateInvasionModal() {
    currentInvasionId = null;
    waveCount = 0;
    $('#invasionModalTitle').text('Crear Invasión');
    $('#invasion-form')[0].reset();
    $('#invasion-id').prop('disabled', false);
    $('#waves-container').empty();
    $('#invasionModal').modal('show');
}

function editInvasion(invasionId) {
    currentInvasionId = invasionId;
    const invasion = allInvasions.find(i => i.invasionId === invasionId);
    if (!invasion) return;
    
    $('#invasionModalTitle').text('Editar Invasión');
    $('#invasion-id').val(invasion.invasionId).prop('disabled', true);
    $('#invasion-name').val(invasion.displayName || '');
    $('#invasion-level').val(invasion.level || 1);
    $('#invasion-world').val(invasion.world || 'world');
    $('#invasion-duration').val(invasion.duration || 15);
    $('#invasion-interval').val(invasion.interval || 60);
    $('#invasion-active').prop('checked', invasion.active || false);
    
    $('#waves-container').empty();
    waveCount = 0;
    if (invasion.waves && invasion.waves.length > 0) {
        invasion.waves.forEach(wave => {
            addWave(wave);
        });
    }
    
    $('#invasionModal').modal('show');
}

function saveInvasion() {
    const invasionData = {
        invasionId: $('#invasion-id').val(),
        displayName: $('#invasion-name').val(),
        level: parseInt($('#invasion-level').val()),
        world: $('#invasion-world').val(),
        duration: parseInt($('#invasion-duration').val()),
        interval: parseInt($('#invasion-interval').val()),
        active: $('#invasion-active').is(':checked'),
        waves: collectWaves()
    };
    
    const url = currentInvasionId ? `/api/config/invasions/${currentInvasionId}` : '/api/config/invasions';
    const method = currentInvasionId ? 'PUT' : 'POST';
    
    $.ajax({
        url: url,
        method: method,
        contentType: 'application/json',
        data: JSON.stringify(invasionData),
        success: function(response) {
            if (response.success) {
                alert(currentInvasionId ? 'Invasión actualizada' : 'Invasión creada');
                $('#invasionModal').modal('hide');
                loadInvasions();
            }
        }
    });
}

function deleteInvasion(invasionId) {
    if (!confirm(`¿Eliminar invasión ${invasionId}?`)) return;
    
    $.ajax({
        url: `/api/config/invasions/${invasionId}`,
        method: 'DELETE',
        success: function(response) {
            if (response.success) {
                alert('Invasión eliminada');
                loadInvasions();
            }
        }
    });
}

function addWave(wave = null) {
    const id = waveCount++;
    const mobs = wave && wave.mobs ? wave.mobs.join(', ') : '';
    
    $('#waves-container').append(`
        <div class="card mb-2" id="wave-${id}">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <strong>Oleada ${id + 1}</strong>
                    <button type="button" class="btn btn-sm btn-danger" onclick="removeWave(${id})">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <input type="text" class="form-control wave-mobs" placeholder="IDs de mobs (separados por comas)" value="${mobs}">
            </div>
        </div>
    `);
}

function removeWave(id) {
    $(`#wave-${id}`).remove();
}

function collectWaves() {
    const waves = [];
    $('.wave-mobs').each(function() {
        const mobs = $(this).val().trim();
        if (mobs) {
            waves.push({ 
                mobs: mobs.split(',').map(m => m.trim()).filter(m => m) 
            });
        }
    });
    return waves;
}

function filterInvasions() {
    const search = $('#search-input').val().toLowerCase();
    const world = $('#world-filter').val();
    
    const filtered = allInvasions.filter(invasion => {
        const matchesSearch = !search || 
            invasion.invasionId.toLowerCase().includes(search) || 
            (invasion.displayName && invasion.displayName.toLowerCase().includes(search));
        const matchesWorld = !world || invasion.world === world;
        return matchesSearch && matchesWorld;
    });
    
    displayInvasions(filtered);
}

function clearFilters() {
    $('#search-input').val('');
    $('#world-filter').val('');
    displayInvasions(allInvasions);
}
