/**
 * rpg-layout.js - Editor de coordenadas del mundo RPG
 */

let layoutData = null;

// Cargar datos al inicializar
document.addEventListener('DOMContentLoaded', async () => {
    await loadLayout();
    renderAll();
});

async function loadLayout() {
    try {
        const response = await fetch('/api/rpg/layout');
        const data = await response.json();
        if (data.success) {
            layoutData = data.layout;
        } else {
            showToast('Error al cargar layout: ' + data.message, 'error');
        }
    } catch (error) {
        console.error('Error loading layout:', error);
        showToast('Error de conexión al cargar layout', 'error');
    }
}

function renderAll() {
    if (!layoutData) return;
    renderZones();
    renderNPCs();
    renderStations();
    renderMobSpawns();
    renderWaypoints();
}

function renderZones() {
    const container = document.getElementById('zones-list');
    if (!layoutData || !layoutData.zones) {
        container.innerHTML = '<p class="text-muted">No hay zonas configuradas</p>';
        return;
    }

    let html = '';
    for (const [zoneId, zone] of Object.entries(layoutData.zones)) {
        html += `
            <div class="card zone-card mb-3">
                <div class="card-body">
                    <div class="row align-items-center">
                        <div class="col-md-3">
                            <strong>${zone.name}</strong>
                            <br><small class="text-muted">${zone.type}</small>
                        </div>
                        <div class="col-md-7">
                            <div class="row g-2">
                                <div class="col-auto">
                                    <label class="form-label small mb-0">Esquina 1</label>
                                    <div class="input-group input-group-sm">
                                        <span class="input-group-text">X</span>
                                        <input type="number" class="form-control coord-input" value="${zone.corners[0].x}" 
                                               onchange="updateZoneCorner('${zoneId}', 0, 'x', this.value)">
                                        <span class="input-group-text">Z</span>
                                        <input type="number" class="form-control coord-input" value="${zone.corners[0].z}" 
                                               onchange="updateZoneCorner('${zoneId}', 0, 'z', this.value)">
                                    </div>
                                </div>
                                <div class="col-auto">
                                    <label class="form-label small mb-0">Esquina 2</label>
                                    <div class="input-group input-group-sm">
                                        <span class="input-group-text">X</span>
                                        <input type="number" class="form-control coord-input" value="${zone.corners[1].x}" 
                                               onchange="updateZoneCorner('${zoneId}', 1, 'x', this.value)">
                                        <span class="input-group-text">Z</span>
                                        <input type="number" class="form-control coord-input" value="${zone.corners[1].z}" 
                                               onchange="updateZoneCorner('${zoneId}', 1, 'z', this.value)">
                                    </div>
                                </div>
                                <div class="col-auto">
                                    <label class="form-label small mb-0">Altura Y</label>
                                    <div class="input-group input-group-sm">
                                        <input type="number" class="form-control coord-input" value="${zone.y_range.min}" 
                                               onchange="updateZoneY('${zoneId}', 'min', this.value)">
                                        <span class="input-group-text">-</span>
                                        <input type="number" class="form-control coord-input" value="${zone.y_range.max}" 
                                               onchange="updateZoneY('${zoneId}', 'max', this.value)">
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="col-md-2 text-end">
                            <button class="btn btn-sm btn-danger" onclick="deleteZone('${zoneId}')">
                                <i class="bi bi-trash"></i>
                            </button>
                        </div>
                    </div>
                    <small class="text-muted d-block mt-2">${zone.description}</small>
                </div>
            </div>
        `;
    }
    container.innerHTML = html;
}

function renderNPCs() {
    const container = document.getElementById('npcs-list');
    if (!layoutData || !layoutData.npcs) {
        container.innerHTML = '<p class="text-muted">No hay NPCs configurados</p>';
        return;
    }

    let html = '<div class="table-responsive"><table class="table table-hover"><thead><tr><th>Nombre</th><th>Tipo</th><th>Coordenadas</th><th>Acciones</th></tr></thead><tbody>';
    
    layoutData.npcs.forEach((npc, index) => {
        html += `
            <tr>
                <td>
                    <span class="npc-marker">${npc.name}</span>
                </td>
                <td>${npc.type}</td>
                <td>
                    <div class="input-group input-group-sm">
                        <span class="input-group-text">X</span>
                        <input type="number" class="form-control coord-input" value="${npc.location.x}" 
                               onchange="updateNPCCoord(${index}, 'x', this.value)">
                        <span class="input-group-text">Y</span>
                        <input type="number" class="form-control coord-input" value="${npc.location.y}" 
                               onchange="updateNPCCoord(${index}, 'y', this.value)">
                        <span class="input-group-text">Z</span>
                        <input type="number" class="form-control coord-input" value="${npc.location.z}" 
                               onchange="updateNPCCoord(${index}, 'z', this.value)">
                    </div>
                </td>
                <td>
                    <button class="btn btn-sm btn-danger" onclick="deleteNPC(${index})">
                        <i class="bi bi-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    });
    
    html += '</tbody></table></div>';
    container.innerHTML = html;
}

function renderStations() {
    const container = document.getElementById('stations-list');
    if (!layoutData || !layoutData.crafting_stations) {
        container.innerHTML = '<p class="text-muted">No hay estaciones configuradas</p>';
        return;
    }

    let html = '<div class="table-responsive"><table class="table table-hover"><thead><tr><th>Nombre</th><th>Tipo</th><th>Coordenadas</th><th>Acciones</th></tr></thead><tbody>';
    
    layoutData.crafting_stations.forEach((station, index) => {
        html += `
            <tr>
                <td>
                    <span class="station-marker">${station.name}</span>
                </td>
                <td>${station.type}</td>
                <td>
                    <div class="input-group input-group-sm">
                        <span class="input-group-text">X</span>
                        <input type="number" class="form-control coord-input" value="${station.location.x}" 
                               onchange="updateStationCoord(${index}, 'x', this.value)">
                        <span class="input-group-text">Y</span>
                        <input type="number" class="form-control coord-input" value="${station.location.y}" 
                               onchange="updateStationCoord(${index}, 'y', this.value)">
                        <span class="input-group-text">Z</span>
                        <input type="number" class="form-control coord-input" value="${station.location.z}" 
                               onchange="updateStationCoord(${index}, 'z', this.value)">
                    </div>
                </td>
                <td>
                    <button class="btn btn-sm btn-danger" onclick="deleteStation(${index})">
                        <i class="bi bi-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    });
    
    html += '</tbody></table></div>';
    container.innerHTML = html;
}

function renderMobSpawns() {
    const container = document.getElementById('mobs-list');
    if (!layoutData || !layoutData.mob_spawn_points) {
        container.innerHTML = '<p class="text-muted">No hay spawns configurados</p>';
        return;
    }

    let html = '<div class="table-responsive"><table class="table table-hover"><thead><tr><th>Mob ID</th><th>Coordenadas</th><th>Respawn (seg)</th><th>Acciones</th></tr></thead><tbody>';
    
    layoutData.mob_spawn_points.forEach((spawn, index) => {
        html += `
            <tr>
                <td>
                    <span class="mob-marker">${spawn.mob_id}</span>
                </td>
                <td>
                    <div class="input-group input-group-sm">
                        <span class="input-group-text">X</span>
                        <input type="number" class="form-control coord-input" value="${spawn.location.x}" 
                               onchange="updateMobSpawnCoord(${index}, 'x', this.value)">
                        <span class="input-group-text">Y</span>
                        <input type="number" class="form-control coord-input" value="${spawn.location.y}" 
                               onchange="updateMobSpawnCoord(${index}, 'y', this.value)">
                        <span class="input-group-text">Z</span>
                        <input type="number" class="form-control coord-input" value="${spawn.location.z}" 
                               onchange="updateMobSpawnCoord(${index}, 'z', this.value)">
                    </div>
                </td>
                <td>
                    <input type="number" class="form-control form-control-sm" style="width: 80px;" 
                           value="${spawn.respawn_time_seconds}" 
                           onchange="updateMobSpawnTime(${index}, this.value)">
                </td>
                <td>
                    <button class="btn btn-sm btn-danger" onclick="deleteMobSpawn(${index})">
                        <i class="bi bi-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    });
    
    html += '</tbody></table></div>';
    container.innerHTML = html;
}

function renderWaypoints() {
    const container = document.getElementById('waypoints-list');
    if (!layoutData || !layoutData.teleport_waypoints) {
        container.innerHTML = '<p class="text-muted">No hay waypoints configurados</p>';
        return;
    }

    let html = '<div class="table-responsive"><table class="table table-hover"><thead><tr><th>Nombre</th><th>Coordenadas</th><th>Nivel Req.</th><th>Acciones</th></tr></thead><tbody>';
    
    layoutData.teleport_waypoints.forEach((waypoint, index) => {
        html += `
            <tr>
                <td>${waypoint.name}</td>
                <td>
                    <div class="input-group input-group-sm">
                        <span class="input-group-text">X</span>
                        <input type="number" class="form-control coord-input" value="${waypoint.location.x}" 
                               onchange="updateWaypointCoord(${index}, 'x', this.value)">
                        <span class="input-group-text">Y</span>
                        <input type="number" class="form-control coord-input" value="${waypoint.location.y}" 
                               onchange="updateWaypointCoord(${index}, 'y', this.value)">
                        <span class="input-group-text">Z</span>
                        <input type="number" class="form-control coord-input" value="${waypoint.location.z}" 
                               onchange="updateWaypointCoord(${index}, 'z', this.value)">
                    </div>
                </td>
                <td>${waypoint.unlock_level}</td>
                <td>
                    <button class="btn btn-sm btn-danger" onclick="deleteWaypoint(${index})">
                        <i class="bi bi-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    });
    
    html += '</tbody></table></div>';
    container.innerHTML = html;
}

// Update functions
function updateZoneCorner(zoneId, cornerIndex, axis, value) {
    layoutData.zones[zoneId].corners[cornerIndex][axis] = parseInt(value);
}

function updateZoneY(zoneId, minOrMax, value) {
    layoutData.zones[zoneId].y_range[minOrMax] = parseInt(value);
}

function updateNPCCoord(index, axis, value) {
    layoutData.npcs[index].location[axis] = parseInt(value);
}

function updateStationCoord(index, axis, value) {
    layoutData.crafting_stations[index].location[axis] = parseInt(value);
}

function updateMobSpawnCoord(index, axis, value) {
    layoutData.mob_spawn_points[index].location[axis] = parseInt(value);
}

function updateMobSpawnTime(index, value) {
    layoutData.mob_spawn_points[index].respawn_time_seconds = parseInt(value);
}

function updateWaypointCoord(index, axis, value) {
    layoutData.teleport_waypoints[index].location[axis] = parseInt(value);
}

// Save layout
async function saveLayout() {
    try {
        const response = await fetch('/api/rpg/layout', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(layoutData)
        });
        const data = await response.json();
        if (data.success) {
            showToast('Layout guardado correctamente', 'success');
        } else {
            showToast('Error al guardar: ' + data.message, 'error');
        }
    } catch (error) {
        console.error('Error saving layout:', error);
        showToast('Error de conexión al guardar', 'error');
    }
}

// Generate spawn script
async function generateSpawnScript() {
    try {
        const response = await fetch('/api/rpg/layout/generate-script');
        const data = await response.json();
        if (data.success) {
            // Download script
            const blob = new Blob([data.script], { type: 'text/plain' });
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'spawn_rpg_world.sh';
            a.click();
            showToast('Script generado y descargado', 'success');
        } else {
            showToast('Error al generar script: ' + data.message, 'error');
        }
    } catch (error) {
        console.error('Error generating script:', error);
        showToast('Error al generar script', 'error');
    }
}

function showToast(message, type) {
    // Simple toast notification
    const alertClass = type === 'success' ? 'alert-success' : 'alert-danger';
    const toast = document.createElement('div');
    toast.className = `alert ${alertClass} position-fixed top-0 end-0 m-3`;
    toast.style.zIndex = '9999';
    toast.textContent = message;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
}
