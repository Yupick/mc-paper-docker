/**
 * rpg.js - Gestión de interfaz RPG en el panel web
 */

// Estado global RPG
const rpgState = {
    currentWorldSlug: null,
    summary: null,
    refreshInterval: null
};

/**
 * Inicializa la sección RPG para un mundo
 */
async function initRPGSection(worldSlug) {
    rpgState.currentWorldSlug = worldSlug;
    
    // Cargar datos iniciales
    await refreshRPGData();
    
    // Configurar actualización automática cada 10 segundos
    if (rpgState.refreshInterval) {
        clearInterval(rpgState.refreshInterval);
    }
    
    rpgState.refreshInterval = setInterval(async () => {
        await refreshRPGData();
    }, 10000);
}

/**
 * Detiene la actualización automática de RPG
 */
function stopRPGRefresh() {
    if (rpgState.refreshInterval) {
        clearInterval(rpgState.refreshInterval);
        rpgState.refreshInterval = null;
    }
}

/**
 * Refresca todos los datos RPG del mundo actual
 */
async function refreshRPGData() {
    if (!rpgState.currentWorldSlug) {
        return;
    }
    
    try {
        const response = await fetch(`/api/worlds/${rpgState.currentWorldSlug}/rpg/summary`);
        const data = await response.json();
        
        if (data.success) {
            rpgState.summary = data.summary;
            renderRPGDashboard(data.summary);
        } else {
            showRPGError(data.message || 'Error al cargar datos RPG');
        }
    } catch (error) {
        console.error('Error al refrescar datos RPG:', error);
        showRPGError('Error de conexión al cargar datos RPG');
    }
}

/**
 * Renderiza el dashboard RPG con los datos del servidor
 */
function renderRPGDashboard(summary) {
    const container = document.getElementById('rpg-content');
    
    if (!container) {
        return;
    }
    
    const { config, status, players, isActive } = summary;
    
    // Si el plugin no está activo, mostrar mensaje
    if (!isActive) {
        container.innerHTML = `
            <div class="alert alert-warning">
                <i class="bi bi-exclamation-triangle-fill me-2"></i>
                El plugin MMORPG está instalado pero el servidor no está iniciado.
                <br>Inicia el servidor para ver las estadísticas RPG.
            </div>
        `;
        return;
    }
    
    // Renderizar dashboard completo
    container.innerHTML = `
        <div class="row g-4">
            <!-- Configuración RPG -->
            <div class="col-md-6">
                <div class="card border-primary h-100">
                    <div class="card-header bg-primary text-white">
                        <h5 class="mb-0">
                            <i class="bi bi-gear-fill me-2"></i>
                            Configuración RPG
                        </h5>
                    </div>
                    <div class="card-body">
                        ${renderRPGConfig(config)}
                    </div>
                </div>
            </div>
            
            <!-- Estado del Servidor -->
            <div class="col-md-6">
                <div class="card border-success h-100">
                    <div class="card-header bg-success text-white">
                        <h5 class="mb-0">
                            <i class="bi bi-activity me-2"></i>
                            Estado del Servidor
                        </h5>
                    </div>
                    <div class="card-body">
                        ${renderRPGStatus(status)}
                    </div>
                </div>
            </div>
            
            <!-- Jugadores RPG -->
            <div class="col-12">
                <div class="card border-info">
                    <div class="card-header bg-info text-white">
                        <h5 class="mb-0">
                            <i class="bi bi-people-fill me-2"></i>
                            Jugadores Activos
                        </h5>
                    </div>
                    <div class="card-body">
                        ${renderRPGPlayers(players)}
                    </div>
                </div>
            </div>
        </div>
    `;
}

/**
 * Renderiza la configuración RPG
 */
function renderRPGConfig(config) {
    const features = [
        { key: 'classesEnabled', label: 'Sistema de Clases', icon: 'shield-fill' },
        { key: 'questsEnabled', label: 'Sistema de Quests', icon: 'journal-text' },
        { key: 'npcsEnabled', label: 'NPCs', icon: 'person-badge-fill' },
        { key: 'economyEnabled', label: 'Economía', icon: 'coin' }
    ];
    
    return `
        <div class="list-group list-group-flush">
            ${features.map(f => `
                <div class="list-group-item d-flex justify-content-between align-items-center">
                    <span>
                        <i class="bi bi-${f.icon} me-2"></i>
                        ${f.label}
                    </span>
                    <span class="badge ${config[f.key] ? 'bg-success' : 'bg-secondary'}">
                        ${config[f.key] ? '✓ Activado' : '✗ Desactivado'}
                    </span>
                </div>
            `).join('')}
        </div>
        <div class="mt-3">
            <small class="text-muted">
                <i class="bi bi-info-circle me-1"></i>
                Configuración definida en el archivo metadata.json del mundo
            </small>
        </div>
    `;
}

/**
 * Renderiza el estado del servidor RPG
 */
function renderRPGStatus(status) {
    if (!status || Object.keys(status).length === 0) {
        return `
            <div class="alert alert-info mb-0">
                <i class="bi bi-hourglass-split me-2"></i>
                Esperando datos del servidor...
            </div>
        `;
    }
    
    return `
        <div class="row g-3">
            <div class="col-md-6">
                <div class="d-flex align-items-center">
                    <div class="flex-shrink-0">
                        <div class="bg-success bg-opacity-10 text-success rounded-3 p-3">
                            <i class="bi bi-clock-fill fs-4"></i>
                        </div>
                    </div>
                    <div class="flex-grow-1 ms-3">
                        <div class="text-muted small">Última actualización</div>
                        <div class="fw-bold">${status.lastUpdate || 'N/A'}</div>
                    </div>
                </div>
            </div>
            <div class="col-md-6">
                <div class="d-flex align-items-center">
                    <div class="flex-shrink-0">
                        <div class="bg-primary bg-opacity-10 text-primary rounded-3 p-3">
                            <i class="bi bi-globe fs-4"></i>
                        </div>
                    </div>
                    <div class="flex-grow-1 ms-3">
                        <div class="text-muted small">Mundo RPG</div>
                        <div class="fw-bold">${status.worldName || rpgState.currentWorldSlug}</div>
                    </div>
                </div>
            </div>
        </div>
        <hr>
        <div class="text-center text-muted">
            <small>Sistema de estadísticas avanzadas disponible en futuras versiones</small>
        </div>
    `;
}

/**
 * Renderiza la lista de jugadores RPG
 */
function renderRPGPlayers(players) {
    if (!players || Object.keys(players).length === 0) {
        return `
            <div class="alert alert-info mb-0">
                <i class="bi bi-info-circle me-2"></i>
                No hay jugadores conectados en este momento
            </div>
        `;
    }
    
    // Convertir objeto de jugadores a array
    const playersList = Object.entries(players).map(([uuid, data]) => ({
        uuid,
        ...data
    }));
    
    return `
        <div class="table-responsive">
            <table class="table table-hover">
                <thead>
                    <tr>
                        <th>Jugador</th>
                        <th>Nivel</th>
                        <th>Clase</th>
                        <th>Última conexión</th>
                    </tr>
                </thead>
                <tbody>
                    ${playersList.map(p => `
                        <tr>
                            <td>
                                <i class="bi bi-person-circle me-2"></i>
                                ${p.name || p.uuid}
                            </td>
                            <td>
                                <span class="badge bg-primary">
                                    Nivel ${p.level || 1}
                                </span>
                            </td>
                            <td>
                                ${p.class || '<span class="text-muted">Sin clase</span>'}
                            </td>
                            <td>
                                <small class="text-muted">
                                    ${p.lastSeen || 'Ahora'}
                                </small>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
        <div class="text-muted small mt-2">
            <i class="bi bi-info-circle me-1"></i>
            Total de jugadores registrados: ${playersList.length}
        </div>
    `;
}

/**
 * Muestra un mensaje de error en la sección RPG
 */
function showRPGError(message) {
    const container = document.getElementById('rpg-content');
    
    if (!container) {
        return;
    }
    
    container.innerHTML = `
        <div class="alert alert-danger">
            <i class="bi bi-exclamation-triangle-fill me-2"></i>
            ${message}
        </div>
    `;
}

/**
 * Verifica si un mundo tiene modo RPG activado
 */
async function checkIfRPGWorld(worldSlug) {
    try {
        const response = await fetch(`/api/worlds/${worldSlug}/rpg/summary`);
        const data = await response.json();
        
        return data.success;
    } catch (error) {
        console.error('Error al verificar mundo RPG:', error);
        return false;
    }
}
