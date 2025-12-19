// ========================================
// PETS & MOUNTS PANEL - JavaScript
// ========================================

let allPets = [];
let myPets = [];
let allMounts = [];
let myMounts = [];
let currentMountId = null;

// ========================================
// INITIALIZATION
// ========================================

document.addEventListener('DOMContentLoaded', function() {
    loadAllPets();
    loadMyPets();
    loadMounts();
    loadStats();

    // Auto-refresh every 10 seconds
    setInterval(() => {
        if (document.getElementById('my-pets-tab').classList.contains('active')) {
            loadMyPets();
        } else if (document.getElementById('stats-tab').classList.contains('active')) {
            loadStats();
        }
    }, 10000);

    // Tab change listeners
    document.getElementById('shop-tab').addEventListener('shown.bs.tab', loadAllPets);
    document.getElementById('mounts-tab').addEventListener('shown.bs.tab', loadMounts);
});

// ========================================
// TAB 1: MY PETS
// ========================================

function loadMyPets() {
    fetch('/api/rpg/pets/my-pets')
        .then(response => response.json())
        .then(data => {
            myPets = data;
            renderMyPets();
        })
        .catch(error => {
            console.error('Error loading my pets:', error);
            document.getElementById('my-pets-list').innerHTML = `
                <div class="col-12">
                    <div class="alert alert-danger">
                        <i class="fas fa-exclamation-triangle me-2"></i>
                        Error al cargar mascotas
                    </div>
                </div>
            `;
        });
}

function renderMyPets() {
    const container = document.getElementById('my-pets-list');
    const countBadge = document.getElementById('pet-count');
    
    if (myPets.length === 0) {
        container.innerHTML = `
            <div class="col-12">
                <div class="empty-state">
                    <i class="fas fa-paw"></i>
                    <h5>No tienes mascotas</h5>
                    <p>Ve a la tienda para adoptar tu primera mascota</p>
                </div>
            </div>
        `;
        countBadge.textContent = '0 / 10 mascotas';
        return;
    }

    countBadge.textContent = `${myPets.length} / 10 mascotas`;

    container.innerHTML = myPets.map(pet => {
        const petInfo = allPets.find(p => p.id === pet.pet_id) || {};
        const evolution = petInfo.evolution_levels ? petInfo.evolution_levels.find(e => e.level === pet.level) : null;
        const healthPercent = (pet.current_health / 100) * 100;
        const hungerPercent = (pet.hunger_level / 100) * 100;

        return `
            <div class="col-md-6 col-lg-4">
                <div class="pet-card">
                    <span class="pet-rarity ${petInfo.rarity || 'COMMON'}">${petInfo.rarity || 'COMMON'}</span>
                    <div class="pet-icon">
                        <i class="fas fa-${getPetIcon(petInfo.type)}"></i>
                    </div>
                    <h5 class="pet-name">${pet.custom_name || petInfo.name}</h5>
                    <span class="pet-type ${petInfo.type}">${petInfo.type}</span>
                    
                    <div class="evolution-level">
                        <span class="level-name">${evolution ? evolution.name : 'Nivel 1'}</span>
                        <span class="level-number">Lv. ${pet.level}</span>
                    </div>

                    <div class="evolution-progress">
                        <div class="progress">
                            <div class="progress-bar" style="width: ${getEvolutionProgress(pet, petInfo)}%">
                                ${pet.experience} XP
                            </div>
                        </div>
                    </div>

                    <div class="health-bar">
                        <div class="health-bar-fill" style="width: ${healthPercent}%">
                            ${pet.current_health.toFixed(0)} HP
                        </div>
                    </div>

                    <div class="hunger-bar">
                        <div class="hunger-bar-fill" style="width: ${hungerPercent}%"></div>
                    </div>

                    <div class="pet-actions">
                        <button class="btn btn-feed" onclick="feedPet('${pet.pet_id}')">
                            <i class="fas fa-drumstick-bite me-1"></i> Alimentar
                        </button>
                        ${canEvolve(pet, petInfo) ? `
                            <button class="btn btn-evolve" onclick="evolvePet('${pet.pet_id}')">
                                <i class="fas fa-star me-1"></i> Evolucionar
                            </button>
                        ` : ''}
                        <button class="btn btn-equip" onclick="equipPet('${pet.pet_id}')">
                            <i class="fas fa-check me-1"></i> Equipar
                        </button>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function getEvolutionProgress(pet, petInfo) {
    if (!petInfo.evolution_levels || pet.level >= petInfo.evolution_levels.length) {
        return 100;
    }
    const nextLevel = petInfo.evolution_levels.find(e => e.level === pet.level + 1);
    if (!nextLevel) return 100;
    return (pet.experience / nextLevel.required_xp) * 100;
}

function canEvolve(pet, petInfo) {
    if (!petInfo.evolution_levels || pet.level >= petInfo.evolution_levels.length) {
        return false;
    }
    const nextLevel = petInfo.evolution_levels.find(e => e.level === pet.level + 1);
    return nextLevel && pet.experience >= nextLevel.required_xp;
}

function feedPet(petId) {
    fetch('/api/rpg/pets/feed', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pet_id: petId })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('¡Mascota alimentada!', 'success');
            loadMyPets();
        } else {
            showToast(data.message || 'Error al alimentar mascota', 'error');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('Error al alimentar mascota', 'error');
    });
}

function evolvePet(petId) {
    fetch('/api/rpg/pets/evolve', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pet_id: petId })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('¡Mascota evolucionada! 🌟', 'success');
            loadMyPets();
            loadStats();
        } else {
            showToast(data.message || 'Error al evolucionar mascota', 'error');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('Error al evolucionar mascota', 'error');
    });
}

function equipPet(petId) {
    fetch('/api/rpg/pets/equip', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pet_id: petId })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('¡Mascota equipada!', 'success');
            loadMyPets();
        } else {
            showToast(data.message || 'Error al equipar mascota', 'error');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('Error al equipar mascota', 'error');
    });
}

// ========================================
// TAB 2: SHOP
// ========================================

function loadAllPets() {
    fetch('/api/rpg/pets/list')
        .then(response => response.json())
        .then(data => {
            allPets = data;
            filterShopPets();
        })
        .catch(error => {
            console.error('Error loading pets:', error);
            document.getElementById('shop-pets-list').innerHTML = `
                <div class="col-12">
                    <div class="alert alert-danger">
                        <i class="fas fa-exclamation-triangle me-2"></i>
                        Error al cargar tienda
                    </div>
                </div>
            `;
        });

    // Add filter listener
    document.getElementById('type-filter').addEventListener('change', filterShopPets);
}

function filterShopPets() {
    const typeFilter = document.getElementById('type-filter').value;
    const filteredPets = typeFilter 
        ? allPets.filter(p => p.type === typeFilter)
        : allPets;

    renderShopPets(filteredPets);
}

function renderShopPets(pets) {
    const container = document.getElementById('shop-pets-list');

    if (pets.length === 0) {
        container.innerHTML = `
            <div class="col-12">
                <div class="empty-state">
                    <i class="fas fa-search"></i>
                    <h5>No se encontraron mascotas</h5>
                    <p>Intenta con otro filtro</p>
                </div>
            </div>
        `;
        return;
    }

    container.innerHTML = pets.map(pet => {
        const owned = myPets.some(p => p.pet_id === pet.id);

        return `
            <div class="col-md-6 col-lg-4">
                <div class="pet-card ${owned ? 'pet-owned' : ''}">
                    <span class="pet-rarity ${pet.rarity}">${pet.rarity}</span>
                    <div class="pet-icon">
                        <i class="fas fa-${getPetIcon(pet.type)}"></i>
                    </div>
                    <h5 class="pet-name">${pet.name}</h5>
                    <span class="pet-type ${pet.type}">${pet.type}</span>
                    <p class="text-muted mt-2">${pet.description}</p>

                    <div class="pet-stats">
                        <div class="stat-item">
                            <i class="fas fa-heart"></i>
                            <span class="stat-label">Salud:</span>
                            <span class="stat-value">${pet.base_stats.health}</span>
                        </div>
                        <div class="stat-item">
                            <i class="fas fa-fist-raised"></i>
                            <span class="stat-label">Daño:</span>
                            <span class="stat-value">${pet.base_stats.damage}</span>
                        </div>
                        <div class="stat-item">
                            <i class="fas fa-running"></i>
                            <span class="stat-label">Velocidad:</span>
                            <span class="stat-value">${pet.base_stats.speed}</span>
                        </div>
                        <div class="stat-item">
                            <i class="fas fa-shield-alt"></i>
                            <span class="stat-label">Defensa:</span>
                            <span class="stat-value">${pet.base_stats.defense}</span>
                        </div>
                    </div>

                    ${owned ? `
                        <div class="alert alert-success text-center mt-3">
                            <i class="fas fa-check-circle me-2"></i>Ya adoptada
                        </div>
                    ` : `
                        <div class="text-center mt-3">
                            <button class="btn btn-adopt w-100" onclick="adoptPet('${pet.id}')">
                                <i class="fas fa-heart me-2"></i>Adoptar (${pet.adoption_cost} monedas)
                            </button>
                        </div>
                    `}

                    <button class="btn btn-link w-100 mt-2" onclick="showPetDetails('${pet.id}')">
                        Ver detalles completos
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

function adoptPet(petId) {
    fetch('/api/rpg/pets/adopt', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pet_id: petId })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('¡Mascota adoptada exitosamente! 🎉', 'success');
            loadMyPets();
            loadAllPets();
            loadStats();
        } else {
            showToast(data.message || 'Error al adoptar mascota', 'error');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('Error al adoptar mascota', 'error');
    });
}

function showPetDetails(petId) {
    const pet = allPets.find(p => p.id === petId);
    if (!pet) return;

    const modal = new bootstrap.Modal(document.getElementById('petDetailsModal'));
    document.getElementById('petDetailsTitle').textContent = pet.name;

    const evolutionsHTML = pet.evolution_levels.map(evol => `
        <div class="evolution-item mb-3">
            <h6>Nivel ${evol.level}: ${evol.name}</h6>
            <p class="text-muted small">Requiere ${evol.required_xp} XP</p>
            <p class="text-muted small">Multiplicador: x${evol.stats_multiplier}</p>
            <div class="abilities-list">
                ${evol.abilities.map(a => `<span class="ability-badge">${a}</span>`).join('')}
            </div>
        </div>
    `).join('');

    document.getElementById('petDetailsBody').innerHTML = `
        <div class="row">
            <div class="col-md-6">
                <h6>Estadísticas Base</h6>
                <ul class="list-group">
                    <li class="list-group-item d-flex justify-content-between">
                        <span><i class="fas fa-heart me-2"></i>Salud</span>
                        <strong>${pet.base_stats.health}</strong>
                    </li>
                    <li class="list-group-item d-flex justify-content-between">
                        <span><i class="fas fa-fist-raised me-2"></i>Daño</span>
                        <strong>${pet.base_stats.damage}</strong>
                    </li>
                    <li class="list-group-item d-flex justify-content-between">
                        <span><i class="fas fa-running me-2"></i>Velocidad</span>
                        <strong>${pet.base_stats.speed}</strong>
                    </li>
                    <li class="list-group-item d-flex justify-content-between">
                        <span><i class="fas fa-shield-alt me-2"></i>Defensa</span>
                        <strong>${pet.base_stats.defense}</strong>
                    </li>
                </ul>
            </div>
            <div class="col-md-6">
                <h6>Evoluciones</h6>
                ${evolutionsHTML}
            </div>
        </div>
        <div class="mt-3">
            <h6>Comida Preferida</h6>
            <p>${pet.food_preferences.join(', ')}</p>
        </div>
    `;

    modal.show();
}

// ========================================
// TAB 3: MOUNTS
// ========================================

function loadMounts() {
    fetch('/api/rpg/pets/mounts')
        .then(response => response.json())
        .then(data => {
            allMounts = data.all_mounts || [];
            myMounts = data.my_mounts || [];
            renderMounts();
        })
        .catch(error => {
            console.error('Error loading mounts:', error);
            document.getElementById('mounts-list').innerHTML = `
                <div class="col-12">
                    <div class="alert alert-danger">
                        <i class="fas fa-exclamation-triangle me-2"></i>
                        Error al cargar monturas
                    </div>
                </div>
            `;
        });
}

function renderMounts() {
    const container = document.getElementById('mounts-list');
    const countBadge = document.getElementById('mount-count');
    
    countBadge.textContent = `${myMounts.length} / ${allMounts.length} monturas`;

    if (allMounts.length === 0) {
        container.innerHTML = `
            <div class="col-12">
                <div class="empty-state">
                    <i class="fas fa-horse"></i>
                    <h5>No hay monturas disponibles</h5>
                </div>
            </div>
        `;
        return;
    }

    container.innerHTML = allMounts.map(mount => {
        const unlocked = myMounts.includes(mount.id);

        return `
            <div class="col-md-6 col-lg-4">
                <div class="mount-card ${!unlocked ? 'mount-locked' : ''}">
                    <span class="pet-rarity ${mount.rarity}">${mount.rarity}</span>
                    <div class="mount-icon">
                        <i class="fas fa-horse"></i>
                    </div>
                    <h5 class="mount-name">${mount.name}</h5>
                    <p class="text-muted">${mount.description}</p>

                    <div class="mount-specs">
                        <div class="spec-item">
                            <div class="spec-icon"><i class="fas fa-tachometer-alt"></i></div>
                            <div class="spec-label">Velocidad</div>
                            <div class="spec-value">x${mount.speed}</div>
                        </div>
                        <div class="spec-item">
                            <div class="spec-icon"><i class="fas fa-arrow-up"></i></div>
                            <div class="spec-label">Salto</div>
                            <div class="spec-value">x${mount.jump_strength}</div>
                        </div>
                        <div class="spec-item">
                            <div class="spec-icon"><i class="fas fa-heart"></i></div>
                            <div class="spec-label">Salud</div>
                            <div class="spec-value">${mount.health}</div>
                        </div>
                    </div>

                    ${mount.special_ability ? `
                        <div class="text-center mb-3">
                            <span class="ability-badge">
                                <i class="fas fa-magic"></i> ${mount.special_ability}
                            </span>
                        </div>
                    ` : ''}

                    ${!unlocked ? `
                        <div class="unlock-requirements">
                            <i class="fas fa-info-circle"></i>
                            <strong>Requisitos:</strong> Nivel ${mount.unlock_level}, ${mount.unlock_cost} monedas
                        </div>
                        <button class="btn btn-primary w-100" onclick="showMountDetails('${mount.id}')">
                            <i class="fas fa-unlock me-2"></i>Desbloquear
                        </button>
                    ` : `
                        <div class="alert alert-success text-center">
                            <i class="fas fa-check-circle me-2"></i>Desbloqueada
                        </div>
                    `}
                </div>
            </div>
        `;
    }).join('');
}

function showMountDetails(mountId) {
    currentMountId = mountId;
    const mount = allMounts.find(m => m.id === mountId);
    if (!mount) return;

    const modal = new bootstrap.Modal(document.getElementById('mountDetailsModal'));
    document.getElementById('mountDetailsTitle').textContent = mount.name;
    document.getElementById('mountDetailsBody').innerHTML = `
        <p>${mount.description}</p>
        <h6>Especificaciones</h6>
        <ul>
            <li><strong>Velocidad:</strong> x${mount.speed}</li>
            <li><strong>Fuerza de Salto:</strong> x${mount.jump_strength}</li>
            <li><strong>Salud:</strong> ${mount.health} HP</li>
            ${mount.special_ability ? `<li><strong>Habilidad Especial:</strong> ${mount.special_ability}</li>` : ''}
        </ul>
        <h6>Requisitos</h6>
        <ul>
            <li><strong>Nivel:</strong> ${mount.unlock_level}</li>
            <li><strong>Costo:</strong> ${mount.unlock_cost} monedas</li>
        </ul>
    `;

    modal.show();
}

function unlockMount() {
    if (!currentMountId) return;

    fetch('/api/rpg/pets/unlock-mount', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ mount_id: currentMountId })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('¡Montura desbloqueada! 🐴', 'success');
            bootstrap.Modal.getInstance(document.getElementById('mountDetailsModal')).hide();
            loadMounts();
            loadStats();
        } else {
            showToast(data.message || 'Error al desbloquear montura', 'error');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('Error al desbloquear montura', 'error');
    });
}

// ========================================
// TAB 4: STATS
// ========================================

function loadStats() {
    fetch('/api/rpg/pets/stats')
        .then(response => response.json())
        .then(data => {
            document.getElementById('stat-total-pets').textContent = data.total_pets || 0;
            document.getElementById('stat-total-mounts').textContent = data.total_mounts || 0;
            document.getElementById('stat-evolutions').textContent = data.total_evolutions || 0;
            document.getElementById('stat-coins-spent').textContent = (data.coins_spent || 0).toLocaleString();

            renderActivityHistory(data.activity || []);
        })
        .catch(error => {
            console.error('Error loading stats:', error);
        });
}

function renderActivityHistory(activities) {
    const tbody = document.getElementById('activity-history');
    
    if (activities.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted">No hay actividad registrada</td></tr>';
        return;
    }

    tbody.innerHTML = activities.map(activity => `
        <tr>
            <td><span class="badge bg-${getActionBadge(activity.action)}">${activity.action}</span></td>
            <td>${activity.target_name}</td>
            <td>${activity.cost || 0} monedas</td>
            <td>${new Date(activity.timestamp).toLocaleString()}</td>
        </tr>
    `).join('');
}

// ========================================
// UTILITY FUNCTIONS
// ========================================

function getPetIcon(type) {
    const icons = {
        'COMBAT': 'dragon',
        'SUPPORT': 'magic',
        'GATHERING': 'box'
    };
    return icons[type] || 'paw';
}

function getActionBadge(action) {
    const badges = {
        'ADOPT': 'success',
        'EVOLVE': 'warning',
        'UNLOCK_MOUNT': 'info',
        'FEED': 'secondary'
    };
    return badges[action] || 'primary';
}

function showToast(message, type) {
    const toastHtml = `
        <div class="toast align-items-center text-white bg-${type === 'success' ? 'success' : 'danger'} border-0" role="alert">
            <div class="d-flex">
                <div class="toast-body">
                    <i class="fas fa-${type === 'success' ? 'check-circle' : 'exclamation-circle'} me-2"></i>
                    ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        </div>
    `;

    const toastContainer = document.querySelector('.toast-container') || (() => {
        const container = document.createElement('div');
        container.className = 'toast-container position-fixed top-0 end-0 p-3';
        document.body.appendChild(container);
        return container;
    })();

    toastContainer.insertAdjacentHTML('beforeend', toastHtml);
    const toastElement = toastContainer.lastElementChild;
    const toast = new bootstrap.Toast(toastElement);
    toast.show();

    toastElement.addEventListener('hidden.bs.toast', () => toastElement.remove());
}
