/**
 * Sprint 5: Items Manager JavaScript
 */

let currentItemId = null;
let allItems = [];

function loadItems() {
    $.ajax({
        url: '/api/config/items',
        method: 'GET',
        success: function(response) {
            if (response.success) {
                allItems = Array.isArray(response.config) ? response.config : Object.values(response.config);
                displayItems(allItems);
                updateStats(allItems);
            }
        }
    });
}

function displayItems(items) {
    const tbody = $('#items-tbody');
    tbody.empty();
    
    if (items.length === 0) {
        tbody.html('<tr><td colspan="7" class="text-center">No hay items</td></tr>');
        return;
    }
    
    items.forEach(item => {
        const rarityColors = {
            common: 'secondary', uncommon: 'success', rare: 'primary',
            epic: 'danger', legendary: 'warning'
        };
        const rarityBadge = `<span class="badge badge-${rarityColors[item.rarity] || 'secondary'}">${item.rarity || 'común'}</span>`;
        const stats = `${item.damage || 0} DMG / ${item.defense || 0} DEF`;
        
        tbody.append(`
            <tr>
                <td><code>${item.itemId}</code></td>
                <td><strong>${item.displayName || item.itemName || item.itemId}</strong></td>
                <td><span class="badge badge-info">${item.itemType || 'CUSTOM'}</span></td>
                <td>${rarityBadge}</td>
                <td>Nv ${item.requiredLevel || 1}</td>
                <td>${stats}</td>
                <td>
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-primary" onclick="editItem('${item.itemId}')"><i class="fas fa-edit"></i></button>
                        <button class="btn btn-danger" onclick="deleteItem('${item.itemId}')"><i class="fas fa-trash"></i></button>
                    </div>
                </td>
            </tr>
        `);
    });
}

function updateStats(items) {
    $('#total-items-count').text(items.length);
    $('#legendary-count').text(items.filter(i => i.rarity === 'legendary').length);
    $('#epic-count').text(items.filter(i => i.rarity === 'epic').length);
    $('#rare-count').text(items.filter(i => i.rarity === 'rare').length);
}

function openCreateItemModal() {
    currentItemId = null;
    $('#itemModalTitle').text('Crear Item');
    $('#item-form')[0].reset();
    $('#item-id').prop('disabled', false);
    $('#itemModal').modal('show');
}

function editItem(itemId) {
    currentItemId = itemId;
    const item = allItems.find(i => i.itemId === itemId);
    if (!item) return;
    
    $('#itemModalTitle').text('Editar Item');
    $('#item-id').val(item.itemId).prop('disabled', true);
    $('#item-name').val(item.displayName || item.itemName || '');
    $('#item-type').val(item.itemType || 'CUSTOM');
    $('#item-rarity').val(item.rarity || 'common');
    $('#item-level').val(item.requiredLevel || 1);
    $('#item-damage').val(item.damage || 0);
    $('#item-defense').val(item.defense || 0);
    $('#item-durability').val(item.durability || 100);
    $('#item-price').val(item.price || 0);
    $('#item-description').val(item.description || '');
    
    $('#itemModal').modal('show');
}

function saveItem() {
    const itemData = {
        itemId: $('#item-id').val(),
        displayName: $('#item-name').val(),
        itemType: $('#item-type').val(),
        rarity: $('#item-rarity').val(),
        requiredLevel: parseInt($('#item-level').val()),
        damage: parseFloat($('#item-damage').val()),
        defense: parseFloat($('#item-defense').val()),
        durability: parseInt($('#item-durability').val()),
        price: parseInt($('#item-price').val()),
        description: $('#item-description').val()
    };
    
    const url = currentItemId ? `/api/config/items/${currentItemId}` : '/api/config/items';
    const method = currentItemId ? 'PUT' : 'POST';
    
    $.ajax({
        url: url,
        method: method,
        contentType: 'application/json',
        data: JSON.stringify(itemData),
        success: function(response) {
            if (response.success) {
                alert(currentItemId ? 'Item actualizado' : 'Item creado');
                $('#itemModal').modal('hide');
                loadItems();
            }
        }
    });
}

function deleteItem(itemId) {
    if (!confirm(`¿Eliminar item ${itemId}?`)) return;
    
    $.ajax({
        url: `/api/config/items/${itemId}`,
        method: 'DELETE',
        success: function(response) {
            if (response.success) {
                alert('Item eliminado');
                loadItems();
            }
        }
    });
}

function filterItems() {
    const search = $('#search-input').val().toLowerCase();
    const rarity = $('#rarity-filter').val();
    const type = $('#type-filter').val();
    
    const filtered = allItems.filter(item => {
        const matchesSearch = !search || 
            item.itemId.toLowerCase().includes(search) || 
            (item.displayName && item.displayName.toLowerCase().includes(search));
        const matchesRarity = !rarity || item.rarity === rarity;
        const matchesType = !type || item.itemType === type;
        return matchesSearch && matchesRarity && matchesType;
    });
    
    displayItems(filtered);
}

function clearFilters() {
    $('#search-input').val('');
    $('#rarity-filter').val('');
    $('#type-filter').val('');
    displayItems(allItems);
}
