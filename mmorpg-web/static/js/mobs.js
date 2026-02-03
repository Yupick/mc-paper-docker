/**
 * Sprint 5: Mobs Manager JavaScript
 * Gestión de mobs con stats, drops y habilidades
 */

let currentMobId = null;
let allMobs = [];
let dropsCount = 0;
let abilitiesCount = 0;

// ===================== CARGA DE MOBS =====================

function loadMobs() {
    $.ajax({
        url: '/api/config/mobs',
        method: 'GET',
        success: function(response) {
            if (response.success) {
                allMobs = Array.isArray(response.config) ? response.config : Object.values(response.config);
                displayMobs(allMobs);
                updateStatistics(allMobs);
            } else {
                showError('Error al cargar mobs: ' + response.error);
            }
        },
        error: function() {
            showError('Error de conexión al cargar mobs');
            $('#mobs-tbody').html('<tr><td colspan="9" class="text-center text-danger">Error al cargar mobs</td></tr>');
        }
    });
}

function displayMobs(mobs) {
    const tbody = $('#mobs-tbody');
    tbody.empty();
    
    if (mobs.length === 0) {
        tbody.html('<tr><td colspan="9" class="text-center">No hay mobs configurados</td></tr>');
        return;
    }
    
    mobs.forEach(function(mob) {
        const isBoss = mob.isBoss || mob.boss || false;
        const typeBadge = isBoss ? 
            '<span class="badge badge-danger">JEFE</span>' : 
            '<span class="badge badge-secondary">Normal</span>';
        
        const row = `
            <tr>
                <td><code>${mob.mobId}</code></td>
                <td><strong>${mob.displayName || mob.mobName || mob.mobId}</strong></td>
                <td><span class="badge badge-primary">Nv ${mob.level || 1}</span></td>
                <td><span class="badge badge-danger">${mob.health || 100} HP</span></td>
                <td><span class="badge badge-warning">${mob.damage || 10} DMG</span></td>
                <td><span class="badge badge-success">${mob.experience || 0} XP</span></td>
                <td>${typeTag}</td>
                <td>${mob.world ? `<span class="badge badge-info">${mob.world}</span>` : '<span class="badge badge-secondary">Global</span>'}</td>
                <td>
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-info" onclick="viewMob('${mob.mobId}')" title="Ver">
                            <i class="fas fa-eye"></i>
                        </button>
                        <button class="btn btn-primary" onclick="editMob('${mob.mobId}')" title="Editar">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn btn-success" onclick="duplicateMob('${mob.mobId}')" title="Duplicar">
                            <i class="fas fa-copy"></i>
                        </button>
                        <button class="btn btn-danger" onclick="deleteMob('${mob.mobId}')" title="Eliminar">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
        tbody.append(row);
    });
}

function updateStatistics(mobs) {
    const totalMobs = mobs.length;
    const bossMobs = mobs.filter(m => m.isBoss || m.boss).length;
    const avgLevel = mobs.length > 0 ? 
        Math.round(mobs.reduce((sum, m) => sum + (m.level || 1), 0) / mobs.length) : 0;
    const totalXP = mobs.reduce((sum, m) => sum + (m.experience || 0), 0);
    
    $('#total-mobs-count').text(totalMobs);
    $('#boss-mobs-count').text(bossMobs);
    $('#avg-level').text(avgLevel);
    $('#total-xp').text(totalXP.toLocaleString());
}

// ===================== CRUD MOBS =====================

function openCreateMobModal() {
    currentMobId = null;
    $('#mobModalTitle').text('Crear Mob');
    $('#mob-form')[0].reset();
    $('#mob-id').prop('disabled', false);
    $('#drops-container').empty();
    $('#abilities-container').empty();
    dropsCount = 0;
    abilitiesCount = 0;
    $('#mobModal').modal('show');
}

function editMob(mobId) {
    currentMobId = mobId;
    $('#mobModalTitle').text('Editar Mob');
    
    const mob = allMobs.find(m => m.mobId === mobId);
    if (!mob) {
        showError('Mob no encontrado');
        return;
    }
    
    // Llenar datos básicos
    $('#mob-id').val(mob.mobId).prop('disabled', true);
    $('#mob-name').val(mob.displayName || mob.mobName || '');
    $('#mob-world').val(mob.world || '');
    
    // Stats
    $('#mob-level').val(mob.level || 1);
    $('#mob-health').val(mob.health || 100);
    $('#mob-damage').val(mob.damage || 10);
    $('#mob-xp').val(mob.experience || 0);
    $('#mob-defense').val(mob.defense || 0);
    $('#mob-speed').val(mob.speed || 1.0);
    $('#mob-is-boss').prop('checked', mob.isBoss || mob.boss || false);
    $('#mob-is-mythic').prop('checked', mob.isMythic || false);
    
    // Drops
    $('#drops-container').empty();
    dropsCount = 0;
    if (mob.drops && Array.isArray(mob.drops)) {
        mob.drops.forEach(drop => addDrop(drop));
    }
    
    // Habilidades
    $('#abilities-container').empty();
    abilitiesCount = 0;
    if (mob.abilities && Array.isArray(mob.abilities)) {
        mob.abilities.forEach(ability => addAbility(ability));
    }
    
    $('#mobModal').modal('show');
}

function saveMob() {
    const mobData = {
        mobId: $('#mob-id').val(),
        displayName: $('#mob-name').val(),
        world: $('#mob-world').val() || null,
        level: parseInt($('#mob-level').val()),
        health: parseInt($('#mob-health').val()),
        damage: parseFloat($('#mob-damage').val()),
        experience: parseInt($('#mob-xp').val()),
        defense: parseFloat($('#mob-defense').val()),
        speed: parseFloat($('#mob-speed').val()),
        isBoss: $('#mob-is-boss').is(':checked'),
        isMythic: $('#mob-is-mythic').is(':checked'),
        drops: collectDrops(),
        abilities: collectAbilities()
    };
    
    if (!mobData.mobId) {
        showError('ID del mob es obligatorio');
        return;
    }
    
    const url = currentMobId ? 
        `/api/config/mobs/${currentMobId}` : 
        '/api/config/mobs';
    const method = currentMobId ? 'PUT' : 'POST';
    
    $.ajax({
        url: url,
        method: method,
        contentType: 'application/json',
        data: JSON.stringify(mobData),
        success: function(response) {
            if (response.success) {
                showSuccess(currentMobId ? 'Mob actualizado' : 'Mob creado');
                $('#mobModal').modal('hide');
                loadMobs();
            } else {
                showError('Error: ' + response.error);
            }
        },
        error: function() {
            showError('Error al guardar mob');
        }
    });
}

function deleteMob(mobId) {
    if (!confirm(`¿Eliminar mob ${mobId}?`)) return;
    
    $.ajax({
        url: `/api/config/mobs/${mobId}`,
        method: 'DELETE',
        success: function(response) {
            if (response.success) {
                showSuccess('Mob eliminado');
                loadMobs();
            } else {
                showError('Error: ' + response.error);
            }
        },
        error: function() {
            showError('Error al eliminar mob');
        }
    });
}

function duplicateMob(mobId) {
    const mob = allMobs.find(m => m.mobId === mobId);
    if (!mob) return;
    
    const newId = prompt(`Duplicar ${mobId} con nuevo ID:`, `${mobId}_copy`);
    if (!newId) return;
    
    const newMob = JSON.parse(JSON.stringify(mob));
    newMob.mobId = newId;
    
    $.ajax({
        url: '/api/config/mobs',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(newMob),
        success: function(response) {
            if (response.success) {
                showSuccess(`Mob duplicado: ${newId}`);
                loadMobs();
            } else {
                showError('Error: ' + response.error);
            }
        }
    });
}

function viewMob(mobId) {
    const mob = allMobs.find(m => m.mobId === mobId);
    if (!mob) return;
    
    const jsonStr = JSON.stringify(mob, null, 2);
    const modalContent = `
        <div class="modal fade" id="viewMobModal" tabindex="-1">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">Ver Mob: ${mobId}</h5>
                        <button type="button" class="close" data-dismiss="modal">
                            <span>&times;</span>
                        </button>
                    </div>
                    <div class="modal-body">
                        <pre style="background: #f4f4f4; padding: 15px; border-radius: 5px; max-height: 500px; overflow-y: auto;">${jsonStr}</pre>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-primary" data-dismiss="modal">Cerrar</button>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    $('body').append(modalContent);
    $('#viewMobModal').modal('show');
    $('#viewMobModal').on('hidden.bs.modal', function() {
        $(this).remove();
    });
}

// ===================== GESTIÓN DE DROPS =====================

function addDrop(dropData = null) {
    dropsCount++;
    const dropId = `drop-${dropsCount}`;
    
    const dropHtml = `
        <div class="card mb-2" id="${dropId}">
            <div class="card-body">
                <div class="form-row">
                    <div class="col-md-4">
                        <input type="text" class="form-control drop-item" placeholder="Nombre del Item" value="${dropData?.itemName || dropData?.item || ''}">
                    </div>
                    <div class="col-md-2">
                        <input type="number" class="form-control drop-chance" placeholder="Chance (%)" step="0.01" value="${dropData?.dropChance || dropData?.chance || 1.0}">
                    </div>
                    <div class="col-md-2">
                        <input type="number" class="form-control drop-min" placeholder="Min" value="${dropData?.minAmount || dropData?.min || 1}">
                    </div>
                    <div class="col-md-2">
                        <input type="number" class="form-control drop-max" placeholder="Max" value="${dropData?.maxAmount || dropData?.max || 1}">
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
    
    $('#drops-container').append(dropHtml);
}

function collectDrops() {
    const drops = [];
    $('#drops-container .card').each(function() {
        drops.push({
            itemName: $(this).find('.drop-item').val(),
            dropChance: parseFloat($(this).find('.drop-chance').val()),
            minAmount: parseInt($(this).find('.drop-min').val()),
            maxAmount: parseInt($(this).find('.drop-max').val())
        });
    });
    return drops;
}

// ===================== GESTIÓN DE HABILIDADES =====================

function addAbility(abilityData = null) {
    abilitiesCount++;
    const abilityId = `ability-${abilitiesCount}`;
    
    const abilityHtml = `
        <div class="card mb-2" id="${abilityId}">
            <div class="card-body">
                <div class="form-row">
                    <div class="col-md-4">
                        <input type="text" class="form-control ability-id" placeholder="ID de Habilidad" value="${abilityData?.abilityId || abilityData?.id || ''}">
                    </div>
                    <div class="col-md-3">
                        <input type="number" class="form-control ability-cooldown" placeholder="Cooldown (s)" value="${abilityData?.cooldown || 10}">
                    </div>
                    <div class="col-md-3">
                        <input type="number" class="form-control ability-damage" placeholder="Daño" step="0.1" value="${abilityData?.damage || 0}">
                    </div>
                    <div class="col-md-2">
                        <button type="button" class="btn btn-danger btn-block" onclick="$('#${abilityId}').remove()">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    $('#abilities-container').append(abilityHtml);
}

function collectAbilities() {
    const abilities = [];
    $('#abilities-container .card').each(function() {
        abilities.push({
            abilityId: $(this).find('.ability-id').val(),
            cooldown: parseInt($(this).find('.ability-cooldown').val()),
            damage: parseFloat($(this).find('.ability-damage').val())
        });
    });
    return abilities;
}

// ===================== FILTROS =====================

function filterMobs() {
    const searchText = $('#search-input').val().toLowerCase();
    const worldFilter = $('#world-filter').val();
    const levelFilter = $('#level-filter').val();
    const bossFilter = $('#boss-filter').val();
    
    let filtered = allMobs.filter(mob => {
        // Búsqueda por texto
        const matchesSearch = !searchText || 
            mob.mobId.toLowerCase().includes(searchText) ||
            (mob.displayName && mob.displayName.toLowerCase().includes(searchText));
        
        // Filtro de mundo
        const matchesWorld = !worldFilter || mob.world === worldFilter;
        
        // Filtro de nivel
        let matchesLevel = true;
        if (levelFilter) {
            const level = mob.level || 1;
            if (levelFilter === '1-10') matchesLevel = level >= 1 && level <= 10;
            else if (levelFilter === '11-25') matchesLevel = level >= 11 && level <= 25;
            else if (levelFilter === '26-50') matchesLevel = level >= 26 && level <= 50;
            else if (levelFilter === '51+') matchesLevel = level >= 51;
        }
        
        // Filtro de jefe
        let matchesBoss = true;
        if (bossFilter) {
            const isBoss = mob.isBoss || mob.boss || false;
            if (bossFilter === 'boss') matchesBoss = isBoss;
            else if (bossFilter === 'normal') matchesBoss = !isBoss;
        }
        
        return matchesSearch && matchesWorld && matchesLevel && matchesBoss;
    });
    
    displayMobs(filtered);
}

function clearFilters() {
    $('#search-input').val('');
    $('#world-filter').val('');
    $('#level-filter').val('');
    $('#boss-filter').val('');
    displayMobs(allMobs);
}

// ===================== EXPORTACIÓN =====================

function exportMobs() {
    const dataStr = JSON.stringify(allMobs, null, 2);
    const dataBlob = new Blob([dataStr], {type: 'application/json'});
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `mobs_config_${Date.now()}.json`;
    link.click();
    showSuccess('Configuración de mobs exportada');
}

// ===================== UTILIDADES =====================

function showSuccess(message) {
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
