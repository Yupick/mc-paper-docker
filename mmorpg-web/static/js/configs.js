/**
 * Sprint 5: Configs Manager JavaScript
 * Gestión universal de configuraciones via API REST
 */

let currentConfigId = null;
let currentConfigType = 'mobs';
let allConfigs = [];

// ===================== CARGA DE CONFIGURACIONES =====================

function loadConfigs() {
    currentConfigType = $('#config-type-selector').val();
    $('#active-config-type').text(currentConfigType.toUpperCase());
    $('#config-type-label').text(currentConfigType.charAt(0).toUpperCase() + currentConfigType.slice(1));
    
    $.ajax({
        url: `/api/config/${currentConfigType}`,
        method: 'GET',
        success: function(response) {
            if (response.success) {
                allConfigs = response.config;
                displayConfigs(allConfigs);
                $('#total-configs-count').text(Object.keys(allConfigs).length);
                $('#last-update-time').text(new Date().toLocaleString('es-ES'));
            } else {
                showError('Error al cargar configuraciones: ' + response.error);
            }
        },
        error: function(xhr) {
            showError('Error de conexión al cargar configuraciones');
            $('#configs-tbody').html('<tr><td colspan="5" class="text-center text-danger">Error al cargar configuraciones</td></tr>');
        }
    });
}

function displayConfigs(configs) {
    const tbody = $('#configs-tbody');
    tbody.empty();
    
    const configArray = Array.isArray(configs) ? configs : Object.values(configs);
    
    if (configArray.length === 0) {
        tbody.html('<tr><td colspan="5" class="text-center">No hay configuraciones</td></tr>');
        return;
    }
    
    configArray.forEach(function(config) {
        const row = buildConfigRow(config);
        tbody.append(row);
    });
}

function buildConfigRow(config) {
    const configId = getConfigId(config);
    const configName = getConfigName(config);
    const configWorld = getConfigWorld(config);
    const configDetails = getConfigDetails(config);
    
    return `
        <tr>
            <td><code>${configId}</code></td>
            <td><strong>${configName}</strong></td>
            <td>${configWorld ? `<span class="badge badge-info">${configWorld}</span>` : '<span class="badge badge-secondary">Global</span>'}</td>
            <td><small>${configDetails}</small></td>
            <td>
                <div class="btn-group btn-group-sm">
                    <button class="btn btn-info" onclick="viewConfig('${configId}')" title="Ver JSON">
                        <i class="fas fa-eye"></i>
                    </button>
                    <button class="btn btn-primary" onclick="editConfig('${configId}')" title="Editar">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-success" onclick="duplicateConfig('${configId}')" title="Duplicar">
                        <i class="fas fa-copy"></i>
                    </button>
                    <button class="btn btn-danger" onclick="deleteConfig('${configId}')" title="Eliminar">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </td>
        </tr>
    `;
}

// ===================== EXTRACCIÓN DE DATOS SEGÚN TIPO =====================

function getConfigId(config) {
    return config.mobId || config.itemId || config.npcId || config.petId || 
           config.dungeonId || config.recipeId || config.enchantmentId || 
           config.classId || config.squadId || config.id || 'unknown';
}

function getConfigName(config) {
    return config.displayName || config.name || config.description || 
           config.mobName || config.itemName || config.npcName || 
           getConfigId(config);
}

function getConfigWorld(config) {
    return config.world || config.worldName || '';
}

function getConfigDetails(config) {
    switch(currentConfigType) {
        case 'mobs':
            return `Nivel: ${config.level || '-'} | HP: ${config.health || '-'} | Daño: ${config.damage || '-'}`;
        case 'items':
            return `Rareza: ${config.rarity || '-'} | Tipo: ${config.itemType || '-'}`;
        case 'npcs':
            return `Tipo: ${config.npcType || '-'} | Diálogos: ${config.dialogues?.length || 0}`;
        case 'pets':
            return `Tipo: ${config.type || '-'} | Habilidades: ${config.abilities?.length || 0}`;
        case 'dungeons':
            return `Oleadas: ${config.waves?.length || 0} | Jugadores: ${config.minPlayers || 1}-${config.maxPlayers || 4}`;
        case 'crafting':
            return `Materiales: ${config.materials?.length || 0}`;
        case 'enchanting':
            return `Nivel Max: ${config.maxLevel || '-'}`;
        case 'classes':
            return `Tipo: ${config.type || '-'} | Habilidades: ${config.skills?.length || 0}`;
        case 'squads':
            return `Max Miembros: ${config.maxMembers || '-'}`;
        default:
            return '-';
    }
}

// ===================== CRUD CONFIGURACIONES =====================

function openCreateConfigModal() {
    currentConfigId = null;
    $('#configModalTitle').text(`Crear ${currentConfigType.charAt(0).toUpperCase() + currentConfigType.slice(1)}`);
    $('#config-form')[0].reset();
    $('#config-id').prop('disabled', false);
    $('#config-world').val('');
    $('#advanced-mode-toggle').prop('checked', false);
    $('#config-json-editor').hide();
    loadSpecificFields();
    $('#configModal').modal('show');
}

function editConfig(configId) {
    currentConfigId = configId;
    $('#configModalTitle').text(`Editar ${currentConfigType}`);
    
    const config = findConfigById(configId);
    if (!config) {
        showError('Configuración no encontrada');
        return;
    }
    
    // Llenar formulario básico
    $('#config-id').val(configId).prop('disabled', true);
    $('#config-name').val(getConfigName(config));
    $('#config-world').val(getConfigWorld(config));
    
    // Llenar JSON editor
    $('#config-json-editor').val(JSON.stringify(config, null, 2));
    
    // Llenar campos específicos
    loadSpecificFields(config);
    
    $('#configModal').modal('show');
}

function viewConfig(configId) {
    const config = findConfigById(configId);
    if (!config) {
        showError('Configuración no encontrada');
        return;
    }
    
    const jsonStr = JSON.stringify(config, null, 2);
    const modalContent = `
        <div class="modal fade" id="viewConfigModal" tabindex="-1">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">Ver Configuración: ${configId}</h5>
                        <button type="button" class="close" data-dismiss="modal">
                            <span>&times;</span>
                        </button>
                    </div>
                    <div class="modal-body">
                        <pre style="background: #f4f4f4; padding: 15px; border-radius: 5px; max-height: 500px; overflow-y: auto;">${jsonStr}</pre>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary" onclick="copyToClipboard('${configId}')">
                            <i class="fas fa-copy"></i> Copiar JSON
                        </button>
                        <button class="btn btn-primary" data-dismiss="modal">Cerrar</button>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    $('body').append(modalContent);
    $('#viewConfigModal').modal('show');
    $('#viewConfigModal').on('hidden.bs.modal', function() {
        $(this).remove();
    });
}

function duplicateConfig(configId) {
    const config = findConfigById(configId);
    if (!config) return;
    
    const newId = prompt(`Duplicar ${configId} con nuevo ID:`, `${configId}_copy`);
    if (!newId) return;
    
    const newConfig = JSON.parse(JSON.stringify(config));
    
    // Actualizar ID según tipo
    if (newConfig.mobId) newConfig.mobId = newId;
    else if (newConfig.itemId) newConfig.itemId = newId;
    else if (newConfig.npcId) newConfig.npcId = newId;
    else if (newConfig.petId) newConfig.petId = newId;
    else if (newConfig.dungeonId) newConfig.dungeonId = newId;
    else if (newConfig.id) newConfig.id = newId;
    
    // Enviar al servidor
    $.ajax({
        url: `/api/config/${currentConfigType}`,
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(newConfig),
        success: function(response) {
            if (response.success) {
                showSuccess(`Configuración duplicada: ${newId}`);
                loadConfigs();
            } else {
                showError('Error al duplicar: ' + response.error);
            }
        }
    });
}

function saveConfig() {
    let configData;
    
    // Modo avanzado: usar JSON directo
    if ($('#advanced-mode-toggle').is(':checked')) {
        try {
            configData = JSON.parse($('#config-json-editor').val());
        } catch (e) {
            showError('JSON inválido: ' + e.message);
            return;
        }
    } else {
        // Modo formulario: construir objeto
        configData = buildConfigFromForm();
    }
    
    // Asegurar que tenga ID
    const configId = $('#config-id').val();
    if (!configId) {
        showError('ID es obligatorio');
        return;
    }
    
    const url = currentConfigId ? 
        `/api/config/${currentConfigType}/${currentConfigId}` : 
        `/api/config/${currentConfigType}`;
    const method = currentConfigId ? 'PUT' : 'POST';
    
    $.ajax({
        url: url,
        method: method,
        contentType: 'application/json',
        data: JSON.stringify(configData),
        success: function(response) {
            if (response.success) {
                showSuccess(currentConfigId ? 'Configuración actualizada' : 'Configuración creada');
                $('#configModal').modal('hide');
                loadConfigs();
            } else {
                showError('Error: ' + response.error);
            }
        },
        error: function(xhr) {
            showError('Error al guardar configuración');
        }
    });
}

function deleteConfig(configId) {
    if (!confirm(`¿Eliminar configuración ${configId}?`)) return;
    
    $.ajax({
        url: `/api/config/${currentConfigType}/${configId}`,
        method: 'DELETE',
        success: function(response) {
            if (response.success) {
                showSuccess('Configuración eliminada');
                loadConfigs();
            } else {
                showError('Error: ' + response.error);
            }
        },
        error: function() {
            showError('Error al eliminar configuración');
        }
    });
}

// ===================== CAMPOS ESPECÍFICOS POR TIPO =====================

function loadSpecificFields(config = null) {
    const container = $('#config-specific-fields');
    container.empty();
    
    let fieldsHtml = '';
    
    switch(currentConfigType) {
        case 'mobs':
            fieldsHtml = buildMobFields(config);
            break;
        case 'items':
            fieldsHtml = buildItemFields(config);
            break;
        case 'npcs':
            fieldsHtml = buildNPCFields(config);
            break;
        case 'pets':
            fieldsHtml = buildPetFields(config);
            break;
        case 'dungeons':
            fieldsHtml = buildDungeonFields(config);
            break;
        default:
            fieldsHtml = '<div class="alert alert-info">Use el modo avanzado para editar este tipo de configuración.</div>';
    }
    
    container.html(fieldsHtml);
}

function buildMobFields(config) {
    return `
        <h5>Propiedades del Mob</h5>
        <div class="form-row">
            <div class="form-group col-md-3">
                <label>Nivel</label>
                <input type="number" class="form-control" id="mob-level" value="${config?.level || 1}">
            </div>
            <div class="form-group col-md-3">
                <label>Salud</label>
                <input type="number" class="form-control" id="mob-health" value="${config?.health || 100}">
            </div>
            <div class="form-group col-md-3">
                <label>Daño</label>
                <input type="number" class="form-control" id="mob-damage" value="${config?.damage || 10}">
            </div>
            <div class="form-group col-md-3">
                <label>Experiencia</label>
                <input type="number" class="form-control" id="mob-xp" value="${config?.experience || 50}">
            </div>
        </div>
    `;
}

function buildItemFields(config) {
    return `
        <h5>Propiedades del Item</h5>
        <div class="form-row">
            <div class="form-group col-md-4">
                <label>Tipo de Item</label>
                <input type="text" class="form-control" id="item-type" value="${config?.itemType || 'CUSTOM'}">
            </div>
            <div class="form-group col-md-4">
                <label>Rareza</label>
                <select class="form-control" id="item-rarity">
                    <option value="common" ${config?.rarity === 'common' ? 'selected' : ''}>Común</option>
                    <option value="uncommon" ${config?.rarity === 'uncommon' ? 'selected' : ''}>Poco Común</option>
                    <option value="rare" ${config?.rarity === 'rare' ? 'selected' : ''}>Raro</option>
                    <option value="epic" ${config?.rarity === 'epic' ? 'selected' : ''}>Épico</option>
                    <option value="legendary" ${config?.rarity === 'legendary' ? 'selected' : ''}>Legendario</option>
                </select>
            </div>
            <div class="form-group col-md-4">
                <label>Nivel Requerido</label>
                <input type="number" class="form-control" id="item-level-req" value="${config?.requiredLevel || 1}">
            </div>
        </div>
    `;
}

function buildNPCFields(config) {
    return `
        <h5>Propiedades del NPC</h5>
        <div class="form-row">
            <div class="form-group col-md-6">
                <label>Tipo de NPC</label>
                <select class="form-control" id="npc-type">
                    <option value="merchant" ${config?.npcType === 'merchant' ? 'selected' : ''}>Comerciante</option>
                    <option value="quest_giver" ${config?.npcType === 'quest_giver' ? 'selected' : ''}>Dador de Misiones</option>
                    <option value="trainer" ${config?.npcType === 'trainer' ? 'selected' : ''}>Entrenador</option>
                    <option value="banker" ${config?.npcType === 'banker' ? 'selected' : ''}>Banquero</option>
                    <option value="dialogue" ${config?.npcType === 'dialogue' ? 'selected' : ''}>Diálogo</option>
                </select>
            </div>
            <div class="form-group col-md-6">
                <label>Skin (URL o ID)</label>
                <input type="text" class="form-control" id="npc-skin" value="${config?.skin || ''}">
            </div>
        </div>
    `;
}

function buildPetFields(config) {
    return `
        <h5>Propiedades de la Mascota</h5>
        <div class="form-row">
            <div class="form-group col-md-4">
                <label>Tipo de Mascota</label>
                <input type="text" class="form-control" id="pet-type" value="${config?.type || 'WOLF'}">
            </div>
            <div class="form-group col-md-4">
                <label>Velocidad</label>
                <input type="number" step="0.1" class="form-control" id="pet-speed" value="${config?.speed || 1.0}">
            </div>
            <div class="form-group col-md-4">
                <label>Salud</label>
                <input type="number" class="form-control" id="pet-health" value="${config?.health || 20}">
            </div>
        </div>
    `;
}

function buildDungeonFields(config) {
    return `
        <h5>Propiedades de la Dungeon</h5>
        <div class="form-row">
            <div class="form-group col-md-3">
                <label>Min Jugadores</label>
                <input type="number" class="form-control" id="dungeon-min-players" value="${config?.minPlayers || 1}">
            </div>
            <div class="form-group col-md-3">
                <label>Max Jugadores</label>
                <input type="number" class="form-control" id="dungeon-max-players" value="${config?.maxPlayers || 4}">
            </div>
            <div class="form-group col-md-3">
                <label>Tiempo Límite (min)</label>
                <input type="number" class="form-control" id="dungeon-time-limit" value="${config?.timeLimit || 30}">
            </div>
            <div class="form-group col-md-3">
                <label>Nivel Requerido</label>
                <input type="number" class="form-control" id="dungeon-level-req" value="${config?.requiredLevel || 1}">
            </div>
        </div>
    `;
}

function buildConfigFromForm() {
    const configData = {
        id: $('#config-id').val(),
        name: $('#config-name').val(),
        world: $('#config-world').val() || null
    };
    
    // Agregar campos específicos según tipo
    switch(currentConfigType) {
        case 'mobs':
            configData.mobId = configData.id;
            configData.level = parseInt($('#mob-level').val());
            configData.health = parseInt($('#mob-health').val());
            configData.damage = parseInt($('#mob-damage').val());
            configData.experience = parseInt($('#mob-xp').val());
            break;
        case 'items':
            configData.itemId = configData.id;
            configData.itemType = $('#item-type').val();
            configData.rarity = $('#item-rarity').val();
            configData.requiredLevel = parseInt($('#item-level-req').val());
            break;
        case 'npcs':
            configData.npcId = configData.id;
            configData.npcType = $('#npc-type').val();
            configData.skin = $('#npc-skin').val();
            break;
        case 'pets':
            configData.petId = configData.id;
            configData.type = $('#pet-type').val();
            configData.speed = parseFloat($('#pet-speed').val());
            configData.health = parseInt($('#pet-health').val());
            break;
        case 'dungeons':
            configData.dungeonId = configData.id;
            configData.minPlayers = parseInt($('#dungeon-min-players').val());
            configData.maxPlayers = parseInt($('#dungeon-max-players').val());
            configData.timeLimit = parseInt($('#dungeon-time-limit').val());
            configData.requiredLevel = parseInt($('#dungeon-level-req').val());
            break;
    }
    
    return configData;
}

// ===================== FILTROS Y BÚSQUEDA =====================

function filterByWorld() {
    const selectedWorld = $('#world-filter').val();
    if (!selectedWorld) {
        displayConfigs(allConfigs);
        return;
    }
    
    $.ajax({
        url: `/api/config/${currentConfigType}/${selectedWorld}`,
        method: 'GET',
        success: function(response) {
            if (response.success) {
                displayConfigs(response.config);
            }
        }
    });
}

function loadWorlds() {
    // Cargar mundos disponibles (simplificado)
    const worlds = ['world', 'world_nether', 'world_the_end', 'rpg_world'];
    const select = $('#world-filter');
    select.empty();
    select.append('<option value="">Todos los mundos</option>');
    worlds.forEach(w => select.append(`<option value="${w}">${w}</option>`));
    $('#worlds-count').text(worlds.length);
}

// ===================== IMPORTACIÓN/EXPORTACIÓN =====================

function exportConfigs() {
    const dataStr = JSON.stringify(allConfigs, null, 2);
    const dataBlob = new Blob([dataStr], {type: 'application/json'});
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${currentConfigType}_config_${Date.now()}.json`;
    link.click();
    showSuccess('Configuración exportada');
}

function importConfigs() {
    $('#importModal').modal('show');
}

function processImport() {
    const fileInput = $('#import-file-input')[0];
    const textInput = $('#import-json-text').val();
    
    if (fileInput.files.length > 0) {
        const file = fileInput.files[0];
        const reader = new FileReader();
        reader.onload = function(e) {
            try {
                const imported = JSON.parse(e.target.result);
                sendImportData(imported);
            } catch (err) {
                showError('Error al parsear JSON: ' + err.message);
            }
        };
        reader.readAsText(file);
    } else if (textInput) {
        try {
            const imported = JSON.parse(textInput);
            sendImportData(imported);
        } catch (err) {
            showError('Error al parsear JSON: ' + err.message);
        }
    } else {
        showError('Seleccione un archivo o pegue JSON');
    }
}

function sendImportData(data) {
    $.ajax({
        url: `/api/config/${currentConfigType}`,
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function(response) {
            if (response.success) {
                showSuccess('Configuraciones importadas correctamente');
                $('#importModal').modal('hide');
                loadConfigs();
            } else {
                showError('Error al importar: ' + response.error);
            }
        },
        error: function() {
            showError('Error al importar configuraciones');
        }
    });
}

// ===================== UTILIDADES =====================

function toggleAdvancedMode() {
    const advanced = $('#advanced-mode-toggle').is(':checked');
    if (advanced) {
        const currentData = buildConfigFromForm();
        $('#config-json-editor').val(JSON.stringify(currentData, null, 2)).show();
        $('#config-specific-fields').hide();
    } else {
        $('#config-json-editor').hide();
        $('#config-specific-fields').show();
    }
}

function reloadConfigs() {
    showSuccess('Recargando configuraciones...');
    loadConfigs();
}

function findConfigById(configId) {
    if (Array.isArray(allConfigs)) {
        return allConfigs.find(c => getConfigId(c) === configId);
    } else {
        return allConfigs[configId] || Object.values(allConfigs).find(c => getConfigId(c) === configId);
    }
}

function copyToClipboard(configId) {
    const config = findConfigById(configId);
    if (!config) return;
    
    const jsonStr = JSON.stringify(config, null, 2);
    navigator.clipboard.writeText(jsonStr).then(() => {
        showSuccess('JSON copiado al portapapeles');
    });
}

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
