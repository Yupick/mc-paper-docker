/**
 * Sprint 5: Dungeons Manager JavaScript
 */

let currentDungeonId = null;
let allDungeons = [];
let waveCount = 0;

function loadDungeons() {
    $.ajax({
        url: '/api/config/dungeons',
        method: 'GET',
        success: function(response) {
            if (response.success) {
                allDungeons = Array.isArray(response.config) ? response.config : Object.values(response.config);
                displayDungeons(allDungeons);
                updateStats(allDungeons);
            }
        }
    });
}

function displayDungeons(dungeons) {
    const tbody = $('#dungeons-tbody');
    tbody.empty();
    
    if (dungeons.length === 0) {
        tbody.html('<tr><td colspan="8" class="text-center">No hay dungeons</td></tr>');
        return;
    }
    
    dungeons.forEach(dungeon => {
        const statusBadge = dungeon.active 
            ? '<span class="badge badge-success">Activo</span>' 
            : '<span class="badge badge-secondary">Inactivo</span>';
        const waves = (dungeon.waves || []).length;
        const players = `${dungeon.minPlayers || 1}-${dungeon.maxPlayers || 4}`;
        
        tbody.append(`
            <tr>
                <td><code>${dungeon.dungeonId}</code></td>
                <td><strong>${dungeon.displayName || dungeon.dungeonId}</strong></td>
                <td><span class="badge badge-info">Nv ${dungeon.level || 1}</span></td>
                <td><span class="badge badge-secondary">${dungeon.world || 'world'}</span></td>
                <td><span class="badge badge-pill badge-warning">${waves}</span></td>
                <td>${players}</td>
                <td>${statusBadge}</td>
                <td>
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-primary" onclick="editDungeon('${dungeon.dungeonId}')"><i class="fas fa-edit"></i></button>
                        <button class="btn btn-danger" onclick="deleteDungeon('${dungeon.dungeonId}')"><i class="fas fa-trash"></i></button>
                    </div>
                </td>
            </tr>
        `);
    });
}

function updateStats(dungeons) {
    $('#total-dungeons-count').text(dungeons.length);
    $('#active-dungeons-count').text(dungeons.filter(d => d.active).length);
    
    const totalWaves = dungeons.reduce((sum, d) => sum + ((d.waves || []).length), 0);
    $('#total-waves-count').text(totalWaves);
    
    const avgLevel = dungeons.length > 0 
        ? Math.round(dungeons.reduce((sum, d) => sum + (d.level || 1), 0) / dungeons.length) 
        : 0;
    $('#avg-level').text(avgLevel);
}

function openCreateDungeonModal() {
    currentDungeonId = null;
    waveCount = 0;
    $('#dungeonModalTitle').text('Crear Dungeon');
    $('#dungeon-form')[0].reset();
    $('#dungeon-id').prop('disabled', false);
    $('#waves-container').empty();
    $('#dungeonModal').modal('show');
}

function editDungeon(dungeonId) {
    currentDungeonId = dungeonId;
    const dungeon = allDungeons.find(d => d.dungeonId === dungeonId);
    if (!dungeon) return;
    
    $('#dungeonModalTitle').text('Editar Dungeon');
    $('#dungeon-id').val(dungeon.dungeonId).prop('disabled', true);
    $('#dungeon-name').val(dungeon.displayName || '');
    $('#dungeon-level').val(dungeon.level || 1);
    $('#dungeon-world').val(dungeon.world || 'world');
    $('#dungeon-min-players').val(dungeon.minPlayers || 1);
    $('#dungeon-max-players').val(dungeon.maxPlayers || 4);
    $('#dungeon-time-limit').val(dungeon.timeLimit || 30);
    $('#dungeon-active').prop('checked', dungeon.active || false);
    
    $('#waves-container').empty();
    waveCount = 0;
    if (dungeon.waves && dungeon.waves.length > 0) {
        dungeon.waves.forEach(wave => {
            addWave(wave);
        });
    }
    
    $('#dungeonModal').modal('show');
}

function saveDungeon() {
    const dungeonData = {
        dungeonId: $('#dungeon-id').val(),
        displayName: $('#dungeon-name').val(),
        level: parseInt($('#dungeon-level').val()),
        world: $('#dungeon-world').val(),
        minPlayers: parseInt($('#dungeon-min-players').val()),
        maxPlayers: parseInt($('#dungeon-max-players').val()),
        timeLimit: parseInt($('#dungeon-time-limit').val()),
        active: $('#dungeon-active').is(':checked'),
        waves: collectWaves()
    };
    
    const url = currentDungeonId ? `/api/config/dungeons/${currentDungeonId}` : '/api/config/dungeons';
    const method = currentDungeonId ? 'PUT' : 'POST';
    
    $.ajax({
        url: url,
        method: method,
        contentType: 'application/json',
        data: JSON.stringify(dungeonData),
        success: function(response) {
            if (response.success) {
                alert(currentDungeonId ? 'Dungeon actualizado' : 'Dungeon creado');
                $('#dungeonModal').modal('hide');
                loadDungeons();
            }
        }
    });
}

function deleteDungeon(dungeonId) {
    if (!confirm(`¿Eliminar dungeon ${dungeonId}?`)) return;
    
    $.ajax({
        url: `/api/config/dungeons/${dungeonId}`,
        method: 'DELETE',
        success: function(response) {
            if (response.success) {
                alert('Dungeon eliminado');
                loadDungeons();
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

function filterDungeons() {
    const search = $('#search-input').val().toLowerCase();
    const world = $('#world-filter').val();
    
    const filtered = allDungeons.filter(dungeon => {
        const matchesSearch = !search || 
            dungeon.dungeonId.toLowerCase().includes(search) || 
            (dungeon.displayName && dungeon.displayName.toLowerCase().includes(search));
        const matchesWorld = !world || dungeon.world === world;
        return matchesSearch && matchesWorld;
    });
    
    displayDungeons(filtered);
}

function clearFilters() {
    $('#search-input').val('');
    $('#world-filter').val('');
    displayDungeons(allDungeons);
}
