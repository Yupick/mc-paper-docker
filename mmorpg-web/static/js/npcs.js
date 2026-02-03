/**
 * Sprint 5: NPCs Manager JavaScript
 */

let currentNpcId = null;
let allNpcs = [];
let dialogueCount = 0;

function loadNpcs() {
    $.ajax({
        url: '/api/config/npcs',
        method: 'GET',
        success: function(response) {
            if (response.success) {
                allNpcs = Array.isArray(response.config) ? response.config : Object.values(response.config);
                displayNpcs(allNpcs);
                updateStats(allNpcs);
            }
        }
    });
}

function displayNpcs(npcs) {
    const tbody = $('#npcs-tbody');
    tbody.empty();
    
    if (npcs.length === 0) {
        tbody.html('<tr><td colspan="7" class="text-center">No hay NPCs</td></tr>');
        return;
    }
    
    npcs.forEach(npc => {
        const typeColors = {
            QUEST_GIVER: 'success', MERCHANT: 'warning', 
            TRAINER: 'info', BANKER: 'primary'
        };
        const typeBadge = `<span class="badge badge-${typeColors[npc.npcType] || 'secondary'}">${npc.npcType || 'CUSTOM'}</span>`;
        const location = `${Math.round(npc.x)}, ${Math.round(npc.y)}, ${Math.round(npc.z)}`;
        const dialogues = (npc.dialogues || []).length;
        
        tbody.append(`
            <tr>
                <td><code>${npc.npcId}</code></td>
                <td><strong>${npc.displayName || npc.npcId}</strong></td>
                <td>${typeBadge}</td>
                <td><span class="badge badge-secondary">${npc.world || 'world'}</span></td>
                <td>${location}</td>
                <td><span class="badge badge-pill badge-info">${dialogues}</span></td>
                <td>
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-primary" onclick="editNpc('${npc.npcId}')"><i class="fas fa-edit"></i></button>
                        <button class="btn btn-danger" onclick="deleteNpc('${npc.npcId}')"><i class="fas fa-trash"></i></button>
                    </div>
                </td>
            </tr>
        `);
    });
}

function updateStats(npcs) {
    $('#total-npcs-count').text(npcs.length);
    $('#quest-npcs-count').text(npcs.filter(n => n.npcType === 'QUEST_GIVER').length);
    $('#merchant-npcs-count').text(npcs.filter(n => n.npcType === 'MERCHANT').length);
    $('#trainer-npcs-count').text(npcs.filter(n => n.npcType === 'TRAINER').length);
}

function openCreateNpcModal() {
    currentNpcId = null;
    dialogueCount = 0;
    $('#npcModalTitle').text('Crear NPC');
    $('#npc-form')[0].reset();
    $('#npc-id').prop('disabled', false);
    $('#dialogues-container').empty();
    $('#npcModal').modal('show');
}

function editNpc(npcId) {
    currentNpcId = npcId;
    const npc = allNpcs.find(n => n.npcId === npcId);
    if (!npc) return;
    
    $('#npcModalTitle').text('Editar NPC');
    $('#npc-id').val(npc.npcId).prop('disabled', true);
    $('#npc-name').val(npc.displayName || '');
    $('#npc-type').val(npc.npcType || 'QUEST_GIVER');
    $('#npc-skin').val(npc.skinName || '');
    $('#npc-world').val(npc.world || 'world');
    $('#npc-x').val(npc.x || 0);
    $('#npc-y').val(npc.y || 64);
    $('#npc-z').val(npc.z || 0);
    
    $('#dialogues-container').empty();
    dialogueCount = 0;
    if (npc.dialogues && npc.dialogues.length > 0) {
        npc.dialogues.forEach(dialogue => {
            addDialogue(dialogue);
        });
    }
    
    $('#npcModal').modal('show');
}

function saveNpc() {
    const npcData = {
        npcId: $('#npc-id').val(),
        displayName: $('#npc-name').val(),
        npcType: $('#npc-type').val(),
        skinName: $('#npc-skin').val(),
        world: $('#npc-world').val(),
        x: parseFloat($('#npc-x').val()),
        y: parseFloat($('#npc-y').val()),
        z: parseFloat($('#npc-z').val()),
        dialogues: collectDialogues()
    };
    
    const url = currentNpcId ? `/api/config/npcs/${currentNpcId}` : '/api/config/npcs';
    const method = currentNpcId ? 'PUT' : 'POST';
    
    $.ajax({
        url: url,
        method: method,
        contentType: 'application/json',
        data: JSON.stringify(npcData),
        success: function(response) {
            if (response.success) {
                alert(currentNpcId ? 'NPC actualizado' : 'NPC creado');
                $('#npcModal').modal('hide');
                loadNpcs();
            }
        }
    });
}

function deleteNpc(npcId) {
    if (!confirm(`¿Eliminar NPC ${npcId}?`)) return;
    
    $.ajax({
        url: `/api/config/npcs/${npcId}`,
        method: 'DELETE',
        success: function(response) {
            if (response.success) {
                alert('NPC eliminado');
                loadNpcs();
            }
        }
    });
}

function addDialogue(dialogue = null) {
    const id = dialogueCount++;
    const text = dialogue ? dialogue.text || '' : '';
    
    $('#dialogues-container').append(`
        <div class="card mb-2" id="dialogue-${id}">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <strong>Diálogo ${id + 1}</strong>
                    <button type="button" class="btn btn-sm btn-danger" onclick="removeDialogue(${id})">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <textarea class="form-control dialogue-text" rows="2" placeholder="Texto del diálogo">${text}</textarea>
            </div>
        </div>
    `);
}

function removeDialogue(id) {
    $(`#dialogue-${id}`).remove();
}

function collectDialogues() {
    const dialogues = [];
    $('.dialogue-text').each(function() {
        const text = $(this).val().trim();
        if (text) {
            dialogues.push({ text: text });
        }
    });
    return dialogues;
}

function filterNpcs() {
    const search = $('#search-input').val().toLowerCase();
    const type = $('#type-filter').val();
    const world = $('#world-filter').val();
    
    const filtered = allNpcs.filter(npc => {
        const matchesSearch = !search || 
            npc.npcId.toLowerCase().includes(search) || 
            (npc.displayName && npc.displayName.toLowerCase().includes(search));
        const matchesType = !type || npc.npcType === type;
        const matchesWorld = !world || npc.world === world;
        return matchesSearch && matchesType && matchesWorld;
    });
    
    displayNpcs(filtered);
}

function clearFilters() {
    $('#search-input').val('');
    $('#type-filter').val('');
    $('#world-filter').val('');
    displayNpcs(allNpcs);
}
