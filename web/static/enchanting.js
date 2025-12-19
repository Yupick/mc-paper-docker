// Sistema de Encantamientos - JavaScript
let currentPlayer = null;
let selectedEnchantment = null;
let selectedItem = null;
let allEnchantments = [];

// Inicializar al cargar la página
document.addEventListener('DOMContentLoaded', function() {
    currentPlayer = localStorage.getItem('username') || 'Jugador';
    document.getElementById('username').textContent = currentPlayer;
    
    // Cargar datos iniciales
    loadEnchantments();
    loadEnchantedItems();
    loadEnchantingStats();
    loadEnchantingHistory();
    
    // Event listeners
    document.getElementById('filterType').addEventListener('change', filterEnchantments);
    document.getElementById('filterTier').addEventListener('change', filterEnchantments);
    document.getElementById('itemType').addEventListener('change', onItemTypeChange);
    document.getElementById('enchantmentSelect').addEventListener('change', onEnchantmentChange);
    document.getElementById('enchantmentLevel').addEventListener('input', onLevelChange);
    
    // Auto-refrescar cada 5 segundos
    setInterval(() => {
        loadEnchantedItems();
        loadEnchantingStats();
    }, 5000);
});

// Cargar lista de encantamientos
async function loadEnchantments() {
    try {
        const response = await fetch('/api/rpg/enchanting/list', {
            headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
        });
        
        if (!response.ok) throw new Error('Error al cargar encantamientos');
        
        allEnchantments = await response.json();
        displayEnchantments(allEnchantments);
    } catch (error) {
        console.error('Error:', error);
        document.getElementById('enchantments-list').innerHTML = 
            '<div class="col-12 alert alert-danger">Error al cargar encantamientos</div>';
    }
}

// Mostrar encantamientos en tarjetas
function displayEnchantments(enchantments) {
    const container = document.getElementById('enchantments-list');
    container.innerHTML = '';
    
    if (!enchantments || enchantments.length === 0) {
        container.innerHTML = '<div class="col-12 text-center text-muted">No hay encantamientos disponibles</div>';
        return;
    }
    
    enchantments.forEach(ench => {
        const tierClass = ench.tier.toLowerCase();
        const typeClass = ench.type.toLowerCase();
        const card = document.createElement('div');
        card.className = 'col-md-6 col-lg-4';
        card.innerHTML = `
            <div class="enchantment-card" onclick="showEnchantmentDetails('${ench.id}')">
                <div class="enchantment-header">
                    <div>
                        <div class="enchantment-title">
                            <i class="fas fa-wand-magic-sparkles"></i> ${ench.name}
                        </div>
                        <span class="enchantment-type ${typeClass}">${ench.type}</span>
                    </div>
                    <span class="enchantment-tier ${tierClass}">${ench.tier}</span>
                </div>
                <div class="enchantment-description">${ench.description}</div>
                <div class="enchantment-stats">
                    <div class="enchantment-stat">
                        <span class="enchantment-stat-label">Nivel Máx</span>
                        <span class="enchantment-stat-value">${ench.max_level}</span>
                    </div>
                    <div class="enchantment-stat">
                        <span class="enchantment-stat-label">Costo/Nivel</span>
                        <span class="enchantment-stat-value">${ench.cost_per_level}</span>
                    </div>
                    <div class="enchantment-stat">
                        <span class="enchantment-stat-label">XP</span>
                        <span class="enchantment-stat-value">${ench.experience_cost}</span>
                    </div>
                </div>
            </div>
        `;
        container.appendChild(card);
    });
}

// Filtrar encantamientos
function filterEnchantments() {
    const typeFilter = document.getElementById('filterType').value;
    const tierFilter = document.getElementById('filterTier').value;
    
    let filtered = allEnchantments;
    
    if (typeFilter) {
        filtered = filtered.filter(e => e.type === typeFilter);
    }
    
    if (tierFilter) {
        filtered = filtered.filter(e => e.tier === tierFilter);
    }
    
    displayEnchantments(filtered);
}

// Mostrar detalles de encantamiento
async function showEnchantmentDetails(enchantmentId) {
    try {
        const response = await fetch(`/api/rpg/enchanting/details/${enchantmentId}`, {
            headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
        });
        
        if (!response.ok) throw new Error('Error al cargar detalles');
        
        const ench = await response.json();
        
        // Llenar modal
        document.getElementById('enchantmentTitle').textContent = ench.name;
        document.getElementById('enchantmentDescription').textContent = ench.description;
        document.getElementById('enchantmentType').textContent = ench.type;
        document.getElementById('enchantmentTier').textContent = ench.tier;
        document.getElementById('enchantmentTier').className = `badge bg-secondary enchantment-tier ${ench.tier.toLowerCase()}`;
        document.getElementById('enchantmentMaxLevel').textContent = ench.max_level;
        
        // Items aplicables
        const applicableDiv = document.getElementById('enchantmentApplicable');
        applicableDiv.innerHTML = ench.applicable_items.map(item => 
            `<span class="badge bg-info me-1">${item}</span>`
        ).join('');
        
        // Efectos
        const effectsList = document.getElementById('enchantmentEffects');
        effectsList.innerHTML = '';
        if (ench.effects && ench.effects.length > 0) {
            ench.effects.forEach(effect => {
                const item = document.createElement('li');
                item.className = 'list-group-item bg-dark';
                item.innerHTML = `
                    <strong>${effect.type}</strong>: ${effect.base_value} 
                    ${effect.per_level > 0 ? `+ ${effect.per_level} por nivel` : ''}
                    ${effect.duration_seconds ? `(${effect.duration_seconds}s)` : ''}
                `;
                effectsList.appendChild(item);
            });
        }
        
        // Incompatibles
        const incompSection = document.getElementById('incompatibleSection');
        const incompDiv = document.getElementById('enchantmentIncompatible');
        if (ench.incompatible_with && ench.incompatible_with.length > 0) {
            incompSection.style.display = 'block';
            incompDiv.textContent = ench.incompatible_with.join(', ');
        } else {
            incompSection.style.display = 'none';
        }
        
        // Mostrar modal
        const modal = new bootstrap.Modal(document.getElementById('enchantmentModal'));
        modal.show();
    } catch (error) {
        console.error('Error:', error);
        alert('Error al cargar los detalles del encantamiento');
    }
}

// Cambio de tipo de item
function onItemTypeChange() {
    const itemType = document.getElementById('itemType').value;
    const preview = document.getElementById('itemPreview');
    const enchSelect = document.getElementById('enchantmentSelect');
    const enchBtn = document.getElementById('enchantBtn');
    
    if (!itemType) {
        preview.innerHTML = '<i class="fas fa-question-circle fa-5x text-muted"></i><p class="text-muted mt-3">Selecciona un item para comenzar</p>';
        preview.classList.remove('active');
        enchSelect.disabled = true;
        enchBtn.disabled = true;
        return;
    }
    
    selectedItem = itemType;
    
    // Actualizar preview
    const icons = {
        'SWORD': 'fa-sword',
        'AXE': 'fa-axe',
        'BOW': 'fa-bow-arrow',
        'CROSSBOW': 'fa-crosshairs',
        'TRIDENT': 'fa-anchor',
        'HELMET': 'fa-helmet-battle',
        'CHESTPLATE': 'fa-vest',
        'LEGGINGS': 'fa-socks',
        'BOOTS': 'fa-boot',
        'SHIELD': 'fa-shield'
    };
    
    preview.innerHTML = `
        <i class="fas ${icons[itemType] || 'fa-gem'} item-icon"></i>
        <div class="item-name">${itemType}</div>
    `;
    preview.classList.add('active');
    
    // Cargar encantamientos compatibles
    const compatible = allEnchantments.filter(e => 
        e.applicable_items.includes(itemType)
    );
    
    enchSelect.innerHTML = '<option value="">Selecciona un encantamiento...</option>';
    compatible.forEach(e => {
        const option = document.createElement('option');
        option.value = e.id;
        option.textContent = `${e.name} (${e.tier})`;
        enchSelect.appendChild(option);
    });
    
    enchSelect.disabled = false;
}

// Cambio de encantamiento seleccionado
function onEnchantmentChange() {
    const enchId = document.getElementById('enchantmentSelect').value;
    const levelSlider = document.getElementById('enchantmentLevel');
    const enchBtn = document.getElementById('enchantBtn');
    
    if (!enchId) {
        levelSlider.disabled = true;
        enchBtn.disabled = true;
        return;
    }
    
    selectedEnchantment = allEnchantments.find(e => e.id === enchId);
    
    if (selectedEnchantment) {
        levelSlider.max = selectedEnchantment.max_level;
        levelSlider.value = 1;
        levelSlider.disabled = false;
        enchBtn.disabled = false;
        
        updateCostPreview();
    }
}

// Cambio de nivel
function onLevelChange() {
    const level = parseInt(document.getElementById('enchantmentLevel').value);
    document.getElementById('levelDisplay').textContent = `Nivel ${level}`;
    updateCostPreview();
}

// Actualizar preview de costo
function updateCostPreview() {
    if (!selectedEnchantment) return;
    
    const level = parseInt(document.getElementById('enchantmentLevel').value);
    const baseCost = selectedEnchantment.cost_per_level * level;
    const baseXP = selectedEnchantment.experience_cost * level;
    
    // Cálculo de probabilidad de éxito
    const baseRate = 0.70;
    const tierModifiers = {
        'UNCOMMON': 1.0,
        'RARE': 0.85,
        'EPIC': 0.70,
        'LEGENDARY': 0.50
    };
    const tierMod = tierModifiers[selectedEnchantment.tier] || 1.0;
    const successRate = Math.round(baseRate * tierMod * 100);
    
    document.getElementById('costCoins').textContent = baseCost.toLocaleString();
    document.getElementById('costXP').textContent = baseXP;
    document.getElementById('successRate').textContent = `${successRate}%`;
}

// Encantar item
async function enchantItem() {
    if (!selectedItem || !selectedEnchantment) {
        alert('Selecciona un item y un encantamiento');
        return;
    }
    
    const level = parseInt(document.getElementById('enchantmentLevel').value);
    
    try {
        const response = await fetch('/api/rpg/enchanting/apply', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                item_type: selectedItem,
                enchantment_id: selectedEnchantment.id,
                level: level
            })
        });
        
        if (!response.ok) throw new Error('Error al encantar item');
        
        const result = await response.json();
        
        if (result.success) {
            showNotification('¡Encantamiento exitoso!', 
                `Has encantado tu ${selectedItem} con ${selectedEnchantment.name} nivel ${level}`, 
                'success');
            
            // Resetear formulario
            document.getElementById('itemType').value = '';
            onItemTypeChange();
            
            // Actualizar vistas
            loadEnchantedItems();
            loadEnchantingStats();
            loadEnchantingHistory();
        } else {
            showNotification('Encantamiento fallido', 
                result.message || 'El encantamiento no tuvo éxito. Inténtalo de nuevo.', 
                'warning');
        }
    } catch (error) {
        console.error('Error:', error);
        showNotification('Error', error.message, 'danger');
    }
}

// Cargar items encantados
async function loadEnchantedItems() {
    try {
        const response = await fetch('/api/rpg/enchanting/items', {
            headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
        });
        
        if (!response.ok) throw new Error('Error al cargar items');
        
        const items = await response.json();
        displayEnchantedItems(items);
    } catch (error) {
        console.error('Error:', error);
    }
}

// Mostrar items encantados
function displayEnchantedItems(items) {
    const container = document.getElementById('enchanted-items-list');
    container.innerHTML = '';
    
    if (!items || items.length === 0) {
        container.innerHTML = '<div class="col-12 text-center text-muted"><p><i class="fas fa-inbox"></i> No tienes items encantados</p></div>';
        return;
    }
    
    items.forEach(item => {
        const itemDiv = document.createElement('div');
        itemDiv.className = 'col-12';
        itemDiv.innerHTML = `
            <div class="enchanted-item-card">
                <div class="enchanted-item-header">
                    <div>
                        <div class="enchanted-item-name">${item.item_name}</div>
                        <div class="enchanted-item-type">${item.item_type}</div>
                    </div>
                    <div>
                        <span class="badge bg-info">${item.enchantments.length} encantamiento(s)</span>
                    </div>
                </div>
                <div class="enchanted-item-enchantments">
                    ${item.enchantments.map(e => 
                        `<span class="enchantment-badge">
                            <i class="fas fa-magic"></i> ${e.name} ${e.level}
                        </span>`
                    ).join('')}
                </div>
            </div>
        `;
        container.appendChild(itemDiv);
    });
}

// Cargar estadísticas
async function loadEnchantingStats() {
    try {
        const response = await fetch('/api/rpg/enchanting/stats', {
            headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
        });
        
        if (!response.ok) throw new Error('Error al cargar estadísticas');
        
        const stats = await response.json();
        
        document.getElementById('stat-items').textContent = stats.enchanted_items;
        document.getElementById('stat-enchants').textContent = stats.total_enchantments;
        document.getElementById('stat-xp').textContent = stats.total_xp.toLocaleString();
        document.getElementById('stat-coins').textContent = stats.total_coins.toLocaleString();
    } catch (error) {
        console.error('Error:', error);
    }
}

// Cargar historial
async function loadEnchantingHistory() {
    try {
        const response = await fetch('/api/rpg/enchanting/history?limit=10', {
            headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
        });
        
        if (!response.ok) throw new Error('Error al cargar historial');
        
        const history = await response.json();
        displayEnchantingHistory(history);
    } catch (error) {
        console.error('Error:', error);
    }
}

// Mostrar historial
function displayEnchantingHistory(history) {
    const tbody = document.getElementById('history-list');
    tbody.innerHTML = '';
    
    if (!history || history.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">Sin historial de encantamientos</td></tr>';
        return;
    }
    
    history.forEach(entry => {
        const row = document.createElement('tr');
        const statusClass = entry.success ? 'table-success' : 'table-danger';
        const statusBadge = entry.success ? 
            '<span class="badge bg-success">Exitoso</span>' : 
            '<span class="badge bg-danger">Fallido</span>';
        
        row.className = statusClass;
        row.innerHTML = `
            <td>${new Date(entry.timestamp).toLocaleString()}</td>
            <td>${entry.item_type}</td>
            <td>${entry.enchantment_name}</td>
            <td><span class="badge bg-info">Nivel ${entry.level}</span></td>
            <td>
                <i class="fas fa-coins text-warning"></i> ${entry.cost_coins}
                <i class="fas fa-star text-info ms-2"></i> ${entry.cost_xp}
            </td>
            <td>${statusBadge}</td>
        `;
        tbody.appendChild(row);
    });
}

// Mostrar notificación
function showNotification(title, message, type = 'info') {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show fixed-top mt-3`;
    alertDiv.style.maxWidth = '500px';
    alertDiv.style.margin = '15px auto';
    alertDiv.style.zIndex = '9999';
    alertDiv.innerHTML = `
        <strong>${title}</strong> ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    document.body.appendChild(alertDiv);
    
    setTimeout(() => {
        alertDiv.remove();
    }, 4000);
}
