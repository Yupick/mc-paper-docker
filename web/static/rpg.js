/**
 * rpg.js - Gestión de interfaz RPG en el panel web
 */

// Estado global RPG con separación local/universal
const rpgState = {
    currentWorldSlug: null,
    summary: null,
    refreshInterval: null,
    npcs_local: [],
    npcs_universal: [],
    quests_local: [],
    quests_universal: [],
    mobs_local: [],
    mobs_universal: [],
    currentTab: 'overview'
};

/**
 * Inicializa los modales en el DOM al cargar la página
 */
document.addEventListener('DOMContentLoaded', function() {
    // Insertar modales de Spawns y Dungeons
    const modalsContainer = document.createElement('div');
    modalsContainer.innerHTML = getSpawnModalsHTML() + getDungeonModalsHTML();
    document.body.appendChild(modalsContainer);
});

/**
 * Inicializa la sección RPG para un mundo
 */
async function initRPGSection(worldSlug) {
    rpgState.currentWorldSlug = worldSlug;
    
    // Cargar datos iniciales
    await refreshRPGData();
    await loadNPCs();
    await loadQuests();
    await loadMobs();
    
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
            // Solo renderizar si no hay tabs creadas aún
            const existingTabs = document.getElementById('rpgTabs');
            if (!existingTabs) {
                renderRPGDashboard(data.summary);
            }
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
    
    // Renderizar dashboard completo con tabs
    container.innerHTML = `
        <!-- Tabs de navegación -->
        <ul class="nav nav-tabs mb-4" id="rpgTabs" role="tablist">
            <li class="nav-item" role="presentation">
                <button class="nav-link active" id="overview-tab" data-bs-toggle="tab" data-bs-target="#overview" 
                        type="button" role="tab" onclick="switchRPGTab('overview')">
                    <i class="bi bi-speedometer2"></i> Resumen
                </button>
            </li>
            <li class="nav-item" role="presentation">
                <button class="nav-link" id="npcs-tab" data-bs-toggle="tab" data-bs-target="#npcs" 
                        type="button" role="tab" onclick="switchRPGTab('npcs')">
                    <i class="bi bi-person-badge"></i> NPCs
                </button>
            </li>
            <li class="nav-item" role="presentation">
                <button class="nav-link" id="quests-tab" data-bs-toggle="tab" data-bs-target="#quests" 
                        type="button" role="tab" onclick="switchRPGTab('quests')">
                    <i class="bi bi-journal-text"></i> Quests
                </button>
            </li>
            <li class="nav-item" role="presentation">
                <button class="nav-link" id="mobs-tab" data-bs-toggle="tab" data-bs-target="#mobs" 
                        type="button" role="tab" onclick="switchRPGTab('mobs')">
                    <i class="bi bi-bug"></i> Mobs Custom
                </button>
            </li>
            <li class="nav-item" role="presentation">
                <button class="nav-link" id="players-tab" data-bs-toggle="tab" data-bs-target="#players-rpg" 
                        type="button" role="tab" onclick="switchRPGTab('players')">
                    <i class="bi bi-people"></i> Jugadores
                </button>
            </li>
            <li class="nav-item" role="presentation">
                <button class="nav-link" id="pets-tab" data-bs-toggle="tab" data-bs-target="#pets" 
                        type="button" role="tab" onclick="switchRPGTab('pets')">
                    <i class="bi bi-egg-fill"></i> Mascotas
                </button>
            </li>
            <li class="nav-item" role="presentation">
                <button class="nav-link" id="bestiary-tab" data-bs-toggle="tab" data-bs-target="#bestiary" 
                        type="button" role="tab" onclick="switchRPGTab('bestiary')">
                    <i class="bi bi-book"></i> Bestiario
                </button>
            </li>
            <li class="nav-item" role="presentation">
                <button class="nav-link" id="achievements-tab" data-bs-toggle="tab" data-bs-target="#achievements" 
                        type="button" role="tab" onclick="switchRPGTab('achievements')">
                    <i class="bi bi-trophy"></i> Logros
                </button>
            </li>
            <li class="nav-item" role="presentation">
                <button class="nav-link" id="ranks-tab" data-bs-toggle="tab" data-bs-target="#ranks" 
                        type="button" role="tab" onclick="switchRPGTab('ranks')">
                    <i class="bi bi-award"></i> Rangos
                </button>
            </li>
            <li class="nav-item" role="presentation">
                <button class="nav-link" id="invasions-tab" data-bs-toggle="tab" data-bs-target="#invasions" 
                        type="button" role="tab" onclick="switchRPGTab('invasions')">
                    <i class="bi bi-shield-exclamation"></i> Invasiones
                </button>
            </li>
            <li class="nav-item" role="presentation">
                <button class="nav-link" id="kills-tab" data-bs-toggle="tab" data-bs-target="#kills" 
                        type="button" role="tab" onclick="switchRPGTab('kills')">
                    <i class="bi bi-crosshair"></i> Kills
                </button>
            </li>
            <li class="nav-item" role="presentation">
                <button class="nav-link" id="respawn-tab" data-bs-toggle="tab" data-bs-target="#respawn" 
                        type="button" role="tab" onclick="switchRPGTab('respawn')">
                    <i class="bi bi-arrow-repeat"></i> Respawn
                </button>
            </li>
            <li class="nav-item" role="presentation">
                <button class="nav-link" id="spawns-tab" data-bs-toggle="tab" data-bs-target="#spawns" 
                        type="button" role="tab" onclick="switchRPGTab('spawns')">
                    <i class="bi bi-pin-map"></i> Spawns
                </button>
            </li>
            <li class="nav-item" role="presentation">
                <button class="nav-link" id="dungeons-tab" data-bs-toggle="tab" data-bs-target="#dungeons" 
                        type="button" role="tab" onclick="switchRPGTab('dungeons')">
                    <i class="bi bi-building"></i> Dungeons
                </button>
            </li>
        </ul>
        
        <!-- Contenido de tabs -->
        <div class="tab-content" id="rpgTabContent">
            <!-- Tab: Resumen -->
            <div class="tab-pane fade show active" id="overview" role="tabpanel">
                <div class="row g-4">
                    <div class="col-md-6">
                        <div class="card border-primary h-100">
                            <div class="card-header bg-primary text-white">
                                <h5 class="mb-0"><i class="bi bi-gear-fill me-2"></i>Configuración RPG</h5>
                            </div>
                            <div class="card-body">
                                ${renderRPGConfig(config)}
                            </div>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="card border-success h-100">
                            <div class="card-header bg-success text-white">
                                <h5 class="mb-0"><i class="bi bi-activity me-2"></i>Estado del Servidor</h5>
                            </div>
                            <div class="card-body">
                                ${renderRPGStatus(status)}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <!-- Tab: Mascotas -->
            <div class="tab-pane fade" id="pets" role="tabpanel">
                <div class="card border-success">
                    <div class="card-header bg-success text-white d-flex align-items-center justify-content-between">
                        <div>
                            <i class="bi bi-egg-fill me-2"></i>
                            <strong>Mascotas y Monturas</strong>
                        </div>
                        <span class="badge bg-light text-success">Nuevo</span>
                    </div>
                    <div class="card-body">
                        <p class="text-muted mb-3">
                            Gestiona tus mascotas, evoluciones y monturas en el panel dedicado.
                        </p>
                        <div class="d-flex flex-wrap gap-2">
                            <a class="btn btn-success" href="/pets">
                                <i class="bi bi-box-arrow-up-right me-1"></i> Abrir panel de Mascotas
                            </a>
                            <a class="btn btn-outline-success" href="/api/rpg/pets/list" target="_blank">
                                <i class="bi bi-list-ul me-1"></i> Ver JSON de mascotas
                            </a>
                        </div>
                    </div>
                </div>
            </div>
            
            <!-- Tab: NPCs -->
            <div class="tab-pane fade" id="npcs" role="tabpanel">
                <div class="card">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5 class="mb-0"><i class="bi bi-person-badge me-2"></i>Gestión de NPCs</h5>
                        <div>
                            <button class="btn btn-success" onclick="showCreateNPCModal()">
                                <i class="bi bi-plus-circle"></i> Crear NPC
                            </button>
                            <button class="btn btn-primary" onclick="loadNPCs()">
                                <i class="bi bi-arrow-clockwise"></i> Refrescar
                            </button>
                        </div>
                    </div>
                    <div class="card-body">
                        <div id="npcs-list">
                            <div class="text-center py-4">
                                <div class="spinner-border text-primary" role="status"></div>
                                <p class="mt-2 text-muted">Cargando NPCs...</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <!-- Tab: Quests -->
            <div class="tab-pane fade" id="quests" role="tabpanel">
                <div class="card">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5 class="mb-0"><i class="bi bi-journal-text me-2"></i>Gestión de Quests</h5>
                        <div>
                            <button class="btn btn-success" onclick="showCreateQuestModal()">
                                <i class="bi bi-plus-circle"></i> Crear Quest
                            </button>
                            <button class="btn btn-primary" onclick="loadQuests()">
                                <i class="bi bi-arrow-clockwise"></i> Refrescar
                            </button>
                        </div>
                    </div>
                    <div class="card-body">
                        <div id="quests-list">
                            <div class="text-center py-4">
                                <div class="spinner-border text-primary" role="status"></div>
                                <p class="mt-2 text-muted">Cargando Quests...</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <!-- Tab: Mobs -->
            <div class="tab-pane fade" id="mobs" role="tabpanel">
                <div class="card">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5 class="mb-0"><i class="bi bi-bug me-2"></i>Mobs Personalizados</h5>
                        <div>
                            <button class="btn btn-success" onclick="showCreateMobModal()">
                                <i class="bi bi-plus-circle"></i> Crear Mob
                            </button>
                            <button class="btn btn-primary" onclick="loadMobs()">
                                <i class="bi bi-arrow-clockwise"></i> Refrescar
                            </button>
                        </div>
                    </div>
                    <div class="card-body">
                        <div id="mobs-list">
                            <div class="text-center py-4">
                                <div class="spinner-border text-primary" role="status"></div>
                                <p class="mt-2 text-muted">Cargando Mobs...</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <!-- Tab: Jugadores -->
            <div class="tab-pane fade" id="players-rpg" role="tabpanel">
                <div class="card border-info">
                    <div class="card-header bg-info text-white">
                        <h5 class="mb-0"><i class="bi bi-people-fill me-2"></i>Jugadores Activos</h5>
                    </div>
                    <div class="card-body">
                        ${renderRPGPlayers(players)}
                    </div>
                </div>
            </div>
            
            <!-- Tab: Bestiario -->
            <div class="tab-pane fade" id="bestiary" role="tabpanel">
                <div class="card">
                    <div class="card-header bg-gradient">
                        <h5 class="mb-0"><i class="bi bi-book me-2"></i>Sistema de Bestiario</h5>
                    </div>
                    <div class="card-body p-0">
                        <iframe src="/bestiary" style="width: 100%; height: 800px; border: none;"></iframe>
                    </div>
                </div>
            </div>

            <!-- Tab: Achievements -->
            <div class="tab-pane fade" id="achievements" role="tabpanel">
                <div class="card">
                    <div class="card-header bg-gradient">
                        <h5 class="mb-0"><i class="bi bi-trophy me-2"></i>Sistema de Logros</h5>
                    </div>
                    <div class="card-body p-0">
                        <iframe src="/achievements" style="width: 100%; height: 900px; border: none;"></iframe>
                    </div>
                </div>
            </div>

            <!-- Tab: Ranks/Titles -->
            <div class="tab-pane fade" id="ranks" role="tabpanel">
                <div class="card">
                    <div class="card-header bg-gradient">
                        <h5 class="mb-0"><i class="bi bi-award me-2"></i>Sistema de Rangos</h5>
                    </div>
                    <div class="card-body p-0">
                        <iframe src="/ranks" style="width: 100%; height: 900px; border: none;"></iframe>
                    </div>
                </div>
            </div>

            <!-- Tab: Invasions -->
            <div class="tab-pane fade" id="invasions" role="tabpanel">
                <div class="card">
                    <div class="card-header bg-gradient">
                        <h5 class="mb-0"><i class="bi bi-shield-exclamation me-2"></i>Sistema de Invasiones</h5>
                    </div>
                    <div class="card-body p-0">
                        <iframe src="/invasions" style="width: 100%; height: 900px; border: none;"></iframe>
                    </div>
                </div>
            </div>
            
            <!-- Tab: Kills -->
            <div class="tab-pane fade" id="kills" role="tabpanel">
                <div class="card">
                    <div class="card-header bg-danger text-white">
                        <h5 class="mb-0"><i class="bi bi-crosshair me-2"></i>Estadísticas de Kills</h5>
                    </div>
                    <div class="card-body">
                        <p class="text-muted">Panel de estadísticas de kills por jugador (próximamente)</p>
                    </div>
                </div>
            </div>
            
            <!-- Tab: Respawn -->
            <div class="tab-pane fade" id="respawn" role="tabpanel">
                <div class="card">
                    <div class="card-header bg-warning text-dark">
                        <h5 class="mb-0"><i class="bi bi-arrow-repeat me-2"></i>Sistema de Respawn</h5>
                    </div>
                    <div class="card-body">
                        <p class="text-muted">Panel de configuración de respawn automático (próximamente)</p>
                    </div>
                </div>
            </div>
            
            <!-- Tab: Spawns -->
            <div class="tab-pane fade" id="spawns" role="tabpanel">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Cargando...</span>
                </div>
            </div>
            
            <!-- Tab: Dungeons -->
            <div class="tab-pane fade" id="dungeons" role="tabpanel">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Cargando...</span>
                </div>
            </div>
        </div>
        
        <!-- Modales para Spawns y Dungeons -->
        ${getSpawnModalsHTML()}
        ${getDungeonModalsHTML()}
    `;
    
    // Cargar listas iniciales
    renderNPCsList();
    renderQuestsList();
    renderMobsList();
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

/**
 * Cambia de tab en el panel RPG
 */
function switchRPGTab(tab) {
    rpgState.currentTab = tab;
    if (tab === 'npcs') {
        loadNPCs();
    } else if (tab === 'quests') {
        loadQuests();
    } else if (tab === 'mobs') {
        loadMobs();
    } else if (tab === 'spawns') {
        loadSpawns();
    } else if (tab === 'dungeons') {
        loadDungeons();
    }
}

// ==================== GESTIÓN DE NPCs ====================

/**
 * Carga la lista de NPCs desde el servidor
 */
async function loadNPCs() {
    try {
        const response = await fetch('/api/rpg/npcs');
        const data = await response.json();
        
        if (data.success) {
            rpgState.npcs_local = data.npcs_local || [];
            rpgState.npcs_universal = data.npcs_universal || [];
            renderNPCsList();
        } else {
            showToast('Error al cargar NPCs: ' + data.message, 'error');
        }
    } catch (error) {
        console.error('Error al cargar NPCs:', error);
        showToast('Error de conexión al cargar NPCs', 'error');
    }
}

/**
 * Renderiza la lista de NPCs (separados por scope: local y universal)
 */
function renderNPCsList() {
    const container = document.getElementById('npcs-list');
    if (!container) return;
    
    const totalNPCs = rpgState.npcs_local.length + rpgState.npcs_universal.length;
    
    if (totalNPCs === 0) {
        container.innerHTML = `
            <div class="alert alert-info">
                <i class="bi bi-info-circle me-2"></i>
                No hay NPCs registrados. Crea uno con el botón "Crear NPC".
            </div>
        `;
        return;
    }
    
    let html = '';
    
    // Sección de NPCs locales (del mundo actual)
    if (rpgState.npcs_local.length > 0) {
        html += `
            <div class="mb-4">
                <h5 class="border-bottom pb-2 mb-3">
                    <i class="bi bi-pin-map"></i> NPCs Locales (Este Mundo)
                    <span class="badge bg-info">${rpgState.npcs_local.length}</span>
                </h5>
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Nombre</th>
                                <th>Tipo</th>
                                <th>Ubicación</th>
                                <th>Acciones</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${rpgState.npcs_local.map(npc => `
                                <tr>
                                    <td><code>${npc.id || 'N/A'}</code></td>
                                    <td><strong>${npc.name || 'Sin nombre'}</strong></td>
                                    <td>
                                        <span class="badge bg-primary">${npc.type || 'Unknown'}</span>
                                    </td>
                                    <td>
                                        <small class="text-muted">
                                            ${npc.location ? `X: ${npc.location.x}, Y: ${npc.location.y}, Z: ${npc.location.z}` : 'Sin ubicación'}
                                        </small>
                                    </td>
                                    <td>
                                        <button class="btn btn-sm btn-warning" onclick='editNPC(${JSON.stringify(npc)}, "local")'>
                                            <i class="bi bi-pencil"></i> Editar
                                        </button>
                                        <button class="btn btn-sm btn-danger" onclick="deleteNPC('${npc.id}', 'local')">
                                            <i class="bi bi-trash"></i> Borrar
                                        </button>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    }
    
    // Sección de NPCs universales (compartidos)
    if (rpgState.npcs_universal.length > 0) {
        html += `
            <div class="mb-4">
                <h5 class="border-bottom pb-2 mb-3">
                    <i class="bi bi-globe"></i> NPCs Globales (Compartidos)
                    <span class="badge bg-success">${rpgState.npcs_universal.length}</span>
                </h5>
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Nombre</th>
                                <th>Tipo</th>
                                <th>Ubicación</th>
                                <th>Acciones</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${rpgState.npcs_universal.map(npc => `
                                <tr>
                                    <td><code>${npc.id || 'N/A'}</code></td>
                                    <td><strong>${npc.name || 'Sin nombre'}</strong></td>
                                    <td>
                                        <span class="badge bg-success">${npc.type || 'Unknown'}</span>
                                    </td>
                                    <td>
                                        <small class="text-muted">
                                            ${npc.location ? `X: ${npc.location.x}, Y: ${npc.location.y}, Z: ${npc.location.z}` : 'Sin ubicación'}
                                        </small>
                                    </td>
                                    <td>
                                        <button class="btn btn-sm btn-warning" onclick='editNPC(${JSON.stringify(npc)}, "universal")'>
                                            <i class="bi bi-pencil"></i> Editar
                                        </button>
                                        <button class="btn btn-sm btn-danger" onclick="deleteNPC('${npc.id}', 'universal')">
                                            <i class="bi bi-trash"></i> Borrar
                                        </button>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    }
    
    container.innerHTML = html;
}

/**
 * Muestra el modal para crear un NPC
 */
function showCreateNPCModal() {
    // Crear modal dinámicamente
    const modalHTML = `
        <div class="modal fade" id="createNPCModal" tabindex="-1">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title"><i class="bi bi-plus-circle"></i> Crear Nuevo NPC</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <form id="createNPCForm">
                            <div class="mb-3">
                                <label class="form-label">Alcance *</label>
                                <select class="form-select" id="npc-scope" required>
                                    <option value="local" selected>Local - Solo para este mundo</option>
                                    <option value="universal">Universal - Compartido en todos los mundos</option>
                                </select>
                                <small class="text-muted">
                                    <i class="bi bi-info-circle"></i> Elige dónde guardar este NPC
                                </small>
                            </div>
                            <hr>
                            <div class="mb-3">
                                <label class="form-label">ID del NPC *</label>
                                <input type="text" class="form-control" id="npc-id" required placeholder="ej: blacksmith_1">
                                <small class="text-muted">
                                    <i class="bi bi-info-circle"></i> Identificador único en minúsculas, sin espacios (usar guión bajo)
                                </small>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Nombre *</label>
                                <input type="text" class="form-control" id="npc-name" required placeholder="ej: Herrero Aldeano">
                                <small class="text-muted">
                                    <i class="bi bi-info-circle"></i> Nombre que verán los jugadores sobre el NPC
                                </small>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Tipo *</label>
                                <select class="form-select" id="npc-type" required>
                                    <option value="MERCHANT">Comerciante - Vende/Compra items</option>
                                    <option value="TRAINER">Entrenador - Enseña habilidades</option>
                                    <option value="QUEST_GIVER">Quest Giver - Da misiones</option>
                                    <option value="GUARD">Guardia - Protector de zona</option>
                                </select>
                                <small class="text-muted">
                                    <i class="bi bi-info-circle"></i> Define el rol y comportamiento del NPC
                                </small>
                            </div>
                            <div class="row">
                                <div class="col-md-4 mb-3">
                                    <label class="form-label">X</label>
                                    <input type="number" class="form-control" id="npc-x" value="0" step="0.1">
                                </div>
                                <div class="col-md-4 mb-3">
                                    <label class="form-label">Y</label>
                                    <input type="number" class="form-control" id="npc-y" value="64" step="0.1">
                                </div>
                                <div class="col-md-4 mb-3">
                                    <label class="form-label">Z</label>
                                    <input type="number" class="form-control" id="npc-z" value="0" step="0.1">
                                </div>
                            </div>
                            <small class="text-muted mb-3 d-block">
                                <i class="bi bi-info-circle"></i> Coordenadas del spawn del NPC en el mundo
                            </small>
                            <div class="mb-3">
                                <label class="form-label">Diálogo</label>
                                <textarea class="form-control" id="npc-dialogue" rows="3" 
                                          placeholder="¡Bienvenido, aventurero!"></textarea>
                                <small class="text-muted">
                                    <i class="bi bi-info-circle"></i> Mensaje que mostrará el NPC al interactuar con él
                                </small>
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                        <button type="button" class="btn btn-primary" onclick="submitCreateNPC()">
                            <i class="bi bi-check-circle"></i> Crear NPC
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    // Remover modal anterior si existe
    const oldModal = document.getElementById('createNPCModal');
    if (oldModal) oldModal.remove();
    
    // Añadir y mostrar nuevo modal
    document.body.insertAdjacentHTML('beforeend', modalHTML);
    const modal = new bootstrap.Modal(document.getElementById('createNPCModal'));
    modal.show();
}

/**
 * Envía el formulario de creación de NPC
 */
async function submitCreateNPC() {
    const npcData = {
        id: document.getElementById('npc-id').value,
        name: document.getElementById('npc-name').value,
        type: document.getElementById('npc-type').value,
        scope: document.getElementById('npc-scope').value,
        location: {
            x: parseFloat(document.getElementById('npc-x').value),
            y: parseFloat(document.getElementById('npc-y').value),
            z: parseFloat(document.getElementById('npc-z').value)
        },
        dialogue: document.getElementById('npc-dialogue').value || '¡Hola!'
    };
    
    try {
        const response = await fetch('/api/rpg/npc/save', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(npcData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            showToast('NPC guardado correctamente', 'success');
            bootstrap.Modal.getInstance(document.getElementById('createNPCModal')).hide();
            setTimeout(() => loadNPCs(), 500);
        } else {
            showToast('Error: ' + data.message, 'error');
        }
    } catch (error) {
        console.error('Error al guardar NPC:', error);
        showToast('Error de conexión', 'error');
    }
}

/**
 * Edita un NPC existente
 */
function editNPC(npc, scope = 'local') {
    // Similar a crear pero con datos precargados
    showCreateNPCModal();
    setTimeout(() => {
        document.getElementById('npc-id').value = npc.id;
        document.getElementById('npc-id').disabled = true;
        document.getElementById('npc-name').value = npc.name;
        document.getElementById('npc-type').value = npc.type;
        document.getElementById('npc-scope').value = scope;
        if (npc.location) {
            document.getElementById('npc-x').value = npc.location.x;
            document.getElementById('npc-y').value = npc.location.y;
            document.getElementById('npc-z').value = npc.location.z;
        }
        document.getElementById('npc-dialogue').value = npc.dialogue || '';
        
        // Cambiar título del modal
        document.querySelector('#createNPCModal .modal-title').innerHTML = 
            '<i class="bi bi-pencil"></i> Editar NPC';
    }, 100);
}

/**
 * Elimina un NPC
 */
async function deleteNPC(npcId, scope = 'local') {
    if (!confirm(`¿Eliminar el NPC "${npcId}"?`)) return;
    
    try {
        const response = await fetch(`/api/rpg/npc/${npcId}`, {
            method: 'DELETE'
        });
        
        const data = await response.json();
        
        if (data.success) {
            showToast('NPC eliminado correctamente', 'success');
            setTimeout(() => loadNPCs(), 500);
        } else {
            showToast('Error: ' + data.message, 'error');
        }
    } catch (error) {
        console.error('Error al eliminar NPC:', error);
        showToast('Error de conexión', 'error');
    }
}

// ==================== GESTIÓN DE QUESTS ====================

/**
 * Carga la lista de quests desde el servidor
 */
async function loadQuests() {
    try {
        const response = await fetch('/api/rpg/quests');
        const data = await response.json();
        
        if (data.success) {
            rpgState.quests_local = data.quests_local || [];
            rpgState.quests_universal = data.quests_universal || [];
            renderQuestsList();
        } else {
            showToast('Error al cargar Quests: ' + data.message, 'error');
        }
    } catch (error) {
        console.error('Error al cargar Quests:', error);
        showToast('Error de conexión al cargar Quests', 'error');
    }
}

/**
 * Renderiza la lista de quests (separadas por scope: local y universal)
 */
function renderQuestsList() {
    const container = document.getElementById('quests-list');
    if (!container) return;
    
    const totalQuests = rpgState.quests_local.length + rpgState.quests_universal.length;
    
    if (totalQuests === 0) {
        container.innerHTML = `
            <div class="alert alert-info">
                <i class="bi bi-info-circle me-2"></i>
                No hay Quests registradas. Crea una con el botón "Crear Quest".
            </div>
        `;
        return;
    }
    
    const difficultyColors = {
        'EASY': 'success',
        'MEDIUM': 'warning',
        'HARD': 'danger',
        'LEGENDARY': 'dark'
    };
    
    let html = '';
    
    // Sección de Quests locales
    if (rpgState.quests_local.length > 0) {
        html += `
            <div class="mb-4">
                <h5 class="border-bottom pb-2 mb-3">
                    <i class="bi bi-pin-map"></i> Quests Locales (Este Mundo)
                    <span class="badge bg-info">${rpgState.quests_local.length}</span>
                </h5>
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Nombre</th>
                                <th>Dificultad</th>
                                <th>Recompensas</th>
                                <th>Acciones</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${rpgState.quests_local.map(quest => `
                                <tr>
                                    <td><code>${quest.id || 'N/A'}</code></td>
                                    <td><strong>${quest.name || 'Sin nombre'}</strong></td>
                                    <td>
                                        <span class="badge bg-${difficultyColors[quest.difficulty] || 'secondary'}">
                                            ${quest.difficulty || 'Unknown'}
                                        </span>
                                    </td>
                                    <td>
                                        <small class="text-muted">
                                            ${quest.rewards ? 
                                                `${quest.rewards.xp || 0} XP, ${quest.rewards.money || 0} monedas` : 
                                                'Sin recompensas'}
                                        </small>
                                    </td>
                                    <td>
                                        <button class="btn btn-sm btn-warning" onclick='editQuest(${JSON.stringify(quest)}, "local")'>
                                            <i class="bi bi-pencil"></i> Editar
                                        </button>
                                        <button class="btn btn-sm btn-danger" onclick="deleteQuest('${quest.id}', 'local')">
                                            <i class="bi bi-trash"></i> Borrar
                                        </button>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    }
    
    // Sección de Quests universales
    if (rpgState.quests_universal.length > 0) {
        html += `
            <div class="mb-4">
                <h5 class="border-bottom pb-2 mb-3">
                    <i class="bi bi-globe"></i> Quests Globales (Compartidas)
                    <span class="badge bg-success">${rpgState.quests_universal.length}</span>
                </h5>
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Nombre</th>
                                <th>Dificultad</th>
                                <th>Recompensas</th>
                                <th>Acciones</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${rpgState.quests_universal.map(quest => `
                                <tr>
                                    <td><code>${quest.id || 'N/A'}</code></td>
                                    <td><strong>${quest.name || 'Sin nombre'}</strong></td>
                                    <td>
                                        <span class="badge bg-${difficultyColors[quest.difficulty] || 'secondary'}">
                                            ${quest.difficulty || 'Unknown'}
                                        </span>
                                    </td>
                                    <td>
                                        <small class="text-muted">
                                            ${quest.rewards ? 
                                                `${quest.rewards.xp || 0} XP, ${quest.rewards.money || 0} monedas` : 
                                                'Sin recompensas'}
                                        </small>
                                    </td>
                                    <td>
                                        <button class="btn btn-sm btn-warning" onclick='editQuest(${JSON.stringify(quest)}, "universal")'>
                                            <i class="bi bi-pencil"></i> Editar
                                        </button>
                                        <button class="btn btn-sm btn-danger" onclick="deleteQuest('${quest.id}', 'universal')">
                                            <i class="bi bi-trash"></i> Borrar
                                        </button>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    }
    
    container.innerHTML = html;
}

/**
 * Muestra el modal para crear una quest
 */
function showCreateQuestModal() {
    const modalHTML = `
        <div class="modal fade" id="createQuestModal" tabindex="-1">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title"><i class="bi bi-plus-circle"></i> Crear Nueva Quest</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <form id="createQuestForm">
                            <div class="mb-3">
                                <label class="form-label">Alcance *</label>
                                <select class="form-select" id="quest-scope" required>
                                    <option value="local" selected>Local - Solo para este mundo</option>
                                    <option value="universal">Universal - Compartida en todos los mundos</option>
                                </select>
                                <small class="text-muted">
                                    <i class="bi bi-info-circle"></i> Elige dónde guardar esta quest
                                </small>
                            </div>
                            <hr>
                            <div class="mb-3">
                                <label class="form-label">ID de la Quest *</label>
                                <input type="text" class="form-control" id="quest-id" required 
                                       placeholder="ej: fetch_water">
                                <small class="text-muted">
                                    <i class="bi bi-info-circle"></i> Identificador único en minúsculas, sin espacios (usar guión bajo)
                                </small>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Nombre *</label>
                                <input type="text" class="form-control" id="quest-name" required 
                                       placeholder="ej: Busca Agua para el Pueblo">
                                <small class="text-muted">
                                    <i class="bi bi-info-circle"></i> Título de la quest que verán los jugadores
                                </small>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Descripción</label>
                                <textarea class="form-control" id="quest-description" rows="3"
                                          placeholder="El pueblo necesita agua fresca..."></textarea>
                                <small class="text-muted">
                                    <i class="bi bi-info-circle"></i> Descripción detallada de la misión y su historia
                                </small>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Dificultad *</label>
                                <select class="form-select" id="quest-difficulty" required>
                                    <option value="EASY">Fácil - Para principiantes</option>
                                    <option value="MEDIUM">Media - Requiere experiencia</option>
                                    <option value="HARD">Difícil - Desafío avanzado</option>
                                    <option value="LEGENDARY">Legendaria - Extrema dificultad</option>
                                </select>
                                <small class="text-muted">
                                    <i class="bi bi-info-circle"></i> Define el nivel de dificultad y recompensas recomendadas
                                </small>
                            </div>
                            <div class="row">
                                <div class="col-md-6 mb-3">
                                    <label class="form-label">XP Recompensa</label>
                                    <input type="number" class="form-control" id="quest-xp" value="100">
                                    <small class="text-muted">
                                        <i class="bi bi-info-circle"></i> Experiencia otorgada al completar
                                    </small>
                                </div>
                                <div class="col-md-6 mb-3">
                                    <label class="form-label">Dinero Recompensa</label>
                                    <input type="number" class="form-control" id="quest-money" value="50">
                                    <small class="text-muted">
                                        <i class="bi bi-info-circle"></i> Monedas otorgadas al completar
                                    </small>
                                </div>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">NPC Asignado (ID)</label>
                                <input type="text" class="form-control" id="quest-npc" 
                                       placeholder="ej: villager_chief">
                                <small class="text-muted">
                                    <i class="bi bi-info-circle"></i> ID del NPC que da esta quest (opcional, debe existir previamente)
                                </small>
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                        <button type="button" class="btn btn-primary" onclick="submitCreateQuest()">
                            <i class="bi bi-check-circle"></i> Crear Quest
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    const oldModal = document.getElementById('createQuestModal');
    if (oldModal) oldModal.remove();
    
    document.body.insertAdjacentHTML('beforeend', modalHTML);
    const modal = new bootstrap.Modal(document.getElementById('createQuestModal'));
    modal.show();
}

/**
 * Envía el formulario de creación de quest
 */
async function submitCreateQuest() {
    const questData = {
        id: document.getElementById('quest-id').value,
        name: document.getElementById('quest-name').value,
        description: document.getElementById('quest-description').value || '',
        difficulty: document.getElementById('quest-difficulty').value,
        scope: document.getElementById('quest-scope').value,
        rewards: {
            xp: parseInt(document.getElementById('quest-xp').value) || 0,
            money: parseInt(document.getElementById('quest-money').value) || 0
        },
        npcId: document.getElementById('quest-npc').value || null,
        objectives: [
            {
                type: 'COLLECT',
                target: 'WATER_BUCKET',
                amount: 1,
                description: 'Recoge 1 cubo de agua'
            }
        ]
    };
    
    try {
        const response = await fetch('/api/rpg/quest/save', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(questData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            showToast('Quest guardada correctamente', 'success');
            bootstrap.Modal.getInstance(document.getElementById('createQuestModal')).hide();
            setTimeout(() => loadQuests(), 500);
        } else {
            showToast('Error: ' + data.message, 'error');
        }
    } catch (error) {
        console.error('Error al guardar Quest:', error);
        showToast('Error de conexión', 'error');
    }
}

/**
 * Edita una quest existente
 */
function editQuest(quest, scope = 'local') {
    showCreateQuestModal();
    setTimeout(() => {
        document.getElementById('quest-id').value = quest.id;
        document.getElementById('quest-id').disabled = true;
        document.getElementById('quest-name').value = quest.name;
        document.getElementById('quest-description').value = quest.description || '';
        document.getElementById('quest-difficulty').value = quest.difficulty;
        document.getElementById('quest-scope').value = scope;
        if (quest.rewards) {
            document.getElementById('quest-xp').value = quest.rewards.xp || 0;
            document.getElementById('quest-money').value = quest.rewards.money || 0;
        }
        document.getElementById('quest-npc').value = quest.npcId || '';
        
        document.querySelector('#createQuestModal .modal-title').innerHTML = 
            '<i class="bi bi-pencil"></i> Editar Quest';
    }, 100);
}

/**
 * Elimina una quest
 */
async function deleteQuest(questId, scope = 'local') {
    if (!confirm(`¿Eliminar la quest "${questId}"?`)) return;
    
    try {
        const response = await fetch(`/api/rpg/quest/${questId}`, {
            method: 'DELETE'
        });
        
        const data = await response.json();
        
        if (data.success) {
            showToast('Quest eliminada correctamente', 'success');
            setTimeout(() => loadQuests(), 500);
        } else {
            showToast('Error: ' + data.message, 'error');
        }
    } catch (error) {
        console.error('Error al eliminar Quest:', error);
        showToast('Error de conexión', 'error');
    }
}

// ==================== GESTIÓN DE MOBS ====================

/**
 * Carga la lista de Mobs desde el servidor
 */
async function loadMobs() {
    try {
        const response = await fetch('/api/rpg/mobs');
        const data = await response.json();
        
        if (data.success) {
            rpgState.mobs_local = data.mobs_local || [];
            rpgState.mobs_universal = data.mobs_universal || [];
            renderMobsList();
        } else {
            showToast('Error al cargar mobs: ' + data.message, 'error');
        }
    } catch (error) {
        console.error('Error al cargar Mobs:', error);
        showToast('Error de conexión al cargar mobs', 'error');
    }
}

/**
 * Renderiza la lista de mobs en el panel (separados por scope: local y universal)
 */
function renderMobsList() {
    const container = document.getElementById('mobs-list');
    
    const totalMobs = (rpgState.mobs_local ? rpgState.mobs_local.length : 0) + (rpgState.mobs_universal ? rpgState.mobs_universal.length : 0);
    
    if (totalMobs === 0) {
        container.innerHTML = `
            <div class="alert alert-info">
                <i class="bi bi-info-circle"></i> No hay mobs personalizados creados.
                <br>Crea tu primer mob usando el botón "Crear Mob".
            </div>
        `;
        return;
    }
    
    let html = '';
    
    // Sección de mobs locales
    if (rpgState.mobs_local && rpgState.mobs_local.length > 0) {
        html += `
            <div class="mb-4">
                <h5 class="border-bottom pb-2 mb-3">
                    <i class="bi bi-pin-map"></i> Mobs Locales (Este Mundo)
                    <span class="badge bg-info">${rpgState.mobs_local.length}</span>
                </h5>
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Nombre</th>
                                <th>Tipo</th>
                                <th>Level</th>
                                <th>Health</th>
                                <th>Damage</th>
                                <th>Boss</th>
                                <th>Acciones</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${rpgState.mobs_local.map(mob => `
                                <tr>
                                    <td><code>${mob.id}</code></td>
                                    <td><strong>${mob.displayName}</strong></td>
                                    <td><span class="badge bg-secondary">${mob.entityType}</span></td>
                                    <td><span class="badge bg-primary">Lv ${mob.level}</span></td>
                                    <td><span class="text-danger">${mob.health} HP</span></td>
                                    <td><span class="text-warning">${mob.damage} DMG</span></td>
                                    <td>${mob.isBoss ? '<span class="badge bg-danger">BOSS</span>' : '<span class="badge bg-success">Normal</span>'}</td>
                                    <td>
                                        <button class="btn btn-sm btn-warning" onclick='editMob(${JSON.stringify(mob).replace(/'/g, "&#39;")}, "local")'>
                                            <i class="bi bi-pencil"></i>
                                        </button>
                                        <button class="btn btn-sm btn-danger" onclick="deleteMob('${mob.id}', 'local')">
                                            <i class="bi bi-trash"></i>
                                        </button>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    }
    
    // Sección de mobs universales
    if (rpgState.mobs_universal && rpgState.mobs_universal.length > 0) {
        html += `
            <div class="mb-4">
                <h5 class="border-bottom pb-2 mb-3">
                    <i class="bi bi-globe"></i> Mobs Globales (Compartidos)
                    <span class="badge bg-success">${rpgState.mobs_universal.length}</span>
                </h5>
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Nombre</th>
                                <th>Tipo</th>
                                <th>Level</th>
                                <th>Health</th>
                                <th>Damage</th>
                                <th>Boss</th>
                                <th>Acciones</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${rpgState.mobs_universal.map(mob => `
                                <tr>
                                    <td><code>${mob.id}</code></td>
                                    <td><strong>${mob.displayName}</strong></td>
                                    <td><span class="badge bg-secondary">${mob.entityType}</span></td>
                                    <td><span class="badge bg-primary">Lv ${mob.level}</span></td>
                                    <td><span class="text-danger">${mob.health} HP</span></td>
                                    <td><span class="text-warning">${mob.damage} DMG</span></td>
                                    <td>${mob.isBoss ? '<span class="badge bg-danger">BOSS</span>' : '<span class="badge bg-success">Normal</span>'}</td>
                                    <td>
                                        <button class="btn btn-sm btn-warning" onclick='editMob(${JSON.stringify(mob).replace(/'/g, "&#39;")}, "universal")'>
                                            <i class="bi bi-pencil"></i>
                                        </button>
                                        <button class="btn btn-sm btn-danger" onclick="deleteMob('${mob.id}', 'universal')">
                                            <i class="bi bi-trash"></i>
                                        </button>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    }
    
    container.innerHTML = html;
}

/**
 * Muestra el modal para crear un nuevo mob
 */
function showCreateMobModal() {
    const modalHtml = `
        <div class="modal fade" id="createMobModal" tabindex="-1">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header bg-primary text-white">
                        <h5 class="modal-title" id="mob-form-title">
                            <i class="bi bi-bug"></i> Crear Mob Personalizado
                        </h5>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <form id="createMobForm" onsubmit="submitCreateMob(event)">
                            <input type="hidden" id="mob-edit-id">
                            
                            <div class="row">
                                <div class="col-md-6 mb-3">
                                    <label for="mob-id" class="form-label">ID del Mob *</label>
                                    <input type="text" class="form-control" id="mob-id" required 
                                           pattern="[a-z0-9_]+" placeholder="ej: boss_dragon">
                                    <small class="text-muted">
                                        <i class="bi bi-info-circle"></i> Identificador único en minúsculas, sin espacios (usar guión bajo)
                                    </small>
                                </div>
                                
                                <div class="col-md-6 mb-3">
                                    <label for="mob-name" class="form-label">Nombre Visible *</label>
                                    <input type="text" class="form-control" id="mob-name" required 
                                           placeholder="ej: Dragón Ancestral">
                                    <small class="text-muted">
                                        <i class="bi bi-info-circle"></i> Nombre que aparecerá sobre el mob en el juego
                                    </small>
                                </div>
                            </div>
                            
                            <div class="row">
                                <div class="col-md-6 mb-3">
                                    <label for="mob-type" class="form-label">Tipo de Entidad *</label>
                                    <select class="form-select" id="mob-type" required>
                                        <option value="">Seleccionar tipo...</option>
                                        <option value="ZOMBIE">ZOMBIE - Muerto viviente</option>
                                        <option value="SKELETON">SKELETON - Esqueleto</option>
                                        <option value="SPIDER">SPIDER - Araña</option>
                                        <option value="CREEPER">CREEPER - Creeper</option>
                                        <option value="ENDERMAN">ENDERMAN - Enderman</option>
                                        <option value="BLAZE">BLAZE - Blaze</option>
                                        <option value="WITHER_SKELETON">WITHER_SKELETON - Esqueleto Wither</option>
                                        <option value="PIGLIN">PIGLIN - Piglin</option>
                                        <option value="HOGLIN">HOGLIN - Hoglin</option>
                                        <option value="ENDER_DRAGON">ENDER_DRAGON - Dragón del End</option>
                                        <option value="WITHER">WITHER - Wither</option>
                                    </select>
                                    <small class="text-muted">
                                        <i class="bi bi-info-circle"></i> Tipo base de entidad de Minecraft que será personalizado
                                    </small>
                                </div>
                                
                                <div class="col-md-6 mb-3">
                                    <label for="mob-level" class="form-label">Nivel *</label>
                                    <input type="number" class="form-control" id="mob-level" required 
                                           min="1" max="100" value="1">
                                    <small class="text-muted">
                                        <i class="bi bi-info-circle"></i> Nivel del mob (1-100), afecta dificultad y recompensas
                                    </small>
                                </div>
                            </div>
                            
                            <div class="row">
                                <div class="col-md-4 mb-3">
                                    <label for="mob-health" class="form-label">Salud (HP) *</label>
                                    <input type="number" class="form-control" id="mob-health" required 
                                           min="1" step="0.5" value="20">
                                    <small class="text-muted">
                                        <i class="bi bi-info-circle"></i> Puntos de vida totales (1 corazón = 2 HP)
                                    </small>
                                </div>
                                
                                <div class="col-md-4 mb-3">
                                    <label for="mob-damage" class="form-label">Daño *</label>
                                    <input type="number" class="form-control" id="mob-damage" required 
                                           min="0" step="0.5" value="2">
                                    <small class="text-muted">
                                        <i class="bi bi-info-circle"></i> Daño por ataque (1 corazón = 2 puntos)
                                    </small>
                                </div>
                                
                                <div class="col-md-4 mb-3">
                                    <label for="mob-defense" class="form-label">Defensa *</label>
                                    <input type="number" class="form-control" id="mob-defense" required 
                                           min="0" step="0.5" value="0">
                                    <small class="text-muted">
                                        <i class="bi bi-info-circle"></i> Reducción de daño recibido (armadura)
                                    </small>
                                </div>
                            </div>
                            
                            <div class="row">
                                <div class="col-md-6 mb-3">
                                    <label for="mob-xp" class="form-label">XP Recompensa *</label>
                                    <input type="number" class="form-control" id="mob-xp" required 
                                           min="0" value="10">
                                    <small class="text-muted">
                                        <i class="bi bi-info-circle"></i> Experiencia otorgada al jugador al derrotarlo
                                    </small>
                                </div>
                                
                                <div class="col-md-6 mb-3">
                                    <label for="mob-boss" class="form-label">¿Es un Boss?</label>
                                    <select class="form-select" id="mob-boss">
                                        <option value="false">No - Mob Normal</option>
                                        <option value="true">Sí - Boss (barra de salud superior)</option>
                                    </select>
                                    <small class="text-muted">
                                        <i class="bi bi-info-circle"></i> Los bosses muestran barra de salud en la parte superior
                                    </small>
                                </div>
                            </div>
                            
                            <div class="mb-3">
                                <label for="mob-spawn" class="form-label">Ubicación de Spawn</label>
                                <input type="text" class="form-control" id="mob-spawn" 
                                       placeholder="ej: world,100,64,200 o dejar vacío">
                                <small class="text-muted">
                                    <i class="bi bi-info-circle"></i> Formato: nombreMundo,X,Y,Z (opcional, se puede spawnear con comando)
                                </small>
                            </div>
                            
                            <div class="mb-3">
                                <label class="form-label">Scope - ¿Dónde guardar este Mob?</label>
                                <div class="btn-group w-100" role="group">
                                    <input type="radio" class="btn-check" name="mob-scope" id="mob-scope-local" value="local" checked>
                                    <label class="btn btn-outline-info" for="mob-scope-local">
                                        <i class="bi bi-pin-map"></i> Local (Este Mundo)
                                    </label>
                                    
                                    <input type="radio" class="btn-check" name="mob-scope" id="mob-scope-universal" value="universal">
                                    <label class="btn btn-outline-success" for="mob-scope-universal">
                                        <i class="bi bi-globe"></i> Global (Compartido)
                                    </label>
                                </div>
                                <small class="text-muted d-block mt-2">
                                    <i class="bi bi-info-circle"></i> 
                                    <strong>Local:</strong> Solo en este mundo | 
                                    <strong>Global:</strong> Disponible en todos los mundos
                                </small>
                            </div>
                            
                            <div class="mb-3">
                                <label class="form-label">Drops (Botín) - Formato: ITEM,minCant,maxCant,probabilidad%</label>
                                <div id="mob-drops-container">
                                    <div class="input-group mb-2">
                                        <input type="text" class="form-control mob-drop" 
                                               placeholder="ej: DIAMOND,1,3,50">
                                        <button type="button" class="btn btn-success" onclick="addMobDrop()">
                                            <i class="bi bi-plus"></i>
                                        </button>
                                    </div>
                                </div>
                                <small class="text-muted">
                                    <i class="bi bi-info-circle"></i> Items que puede soltar. Probabilidad en % (0-100). Añade múltiples drops con el botón +
                                </small>
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                        <button type="submit" form="createMobForm" class="btn btn-primary">
                            <i class="bi bi-check-lg"></i> Crear Mob
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    // Eliminar modal previo si existe
    const existingModal = document.getElementById('createMobModal');
    if (existingModal) existingModal.remove();
    
    // Añadir nuevo modal
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    
    // Resetear formulario y establecer valores por defecto
    document.getElementById('mob-form-title').textContent = 'Crear Nuevo Mob';
    document.getElementById('mob-id').value = '';
    document.getElementById('mob-id').disabled = false;
    document.getElementById('mob-edit-id').value = '';
    document.getElementById('mob-name').value = '';
    document.getElementById('mob-type').value = '';
    document.getElementById('mob-level').value = '1';
    document.getElementById('mob-health').value = '20';
    document.getElementById('mob-damage').value = '2';
    document.getElementById('mob-defense').value = '0';
    document.getElementById('mob-xp').value = '10';
    document.getElementById('mob-boss').value = 'false';
    document.getElementById('mob-spawn').value = '';
    
    // Resetear scope a local por defecto
    const scopeLocal = document.getElementById('mob-scope-local');
    if (scopeLocal) scopeLocal.checked = true;
    
    // Resetear drops
    const dropsContainer = document.getElementById('mob-drops-container');
    dropsContainer.innerHTML = `
        <div class="input-group mb-2">
            <input type="text" class="form-control mob-drop" 
                   placeholder="ej: DIAMOND,1,3,50">
            <button type="button" class="btn btn-success" onclick="addMobDrop()">
                <i class="bi bi-plus"></i>
            </button>
        </div>
    `;
    
    // Mostrar modal
    const modal = new bootstrap.Modal(document.getElementById('createMobModal'));
    modal.show();
}

/**
 * Añade un campo adicional de drop
 */
function addMobDrop() {
    const container = document.getElementById('mob-drops-container');
    const newDrop = `
        <div class="input-group mb-2">
            <input type="text" class="form-control mob-drop" 
                   placeholder="ej: EMERALD,2,5,30">
            <button type="button" class="btn btn-danger" onclick="this.parentElement.remove()">
                <i class="bi bi-trash"></i>
            </button>
        </div>
    `;
    container.insertAdjacentHTML('beforeend', newDrop);
}

/**
 * Envía el formulario de creación de mob
 */
async function submitCreateMob(event) {
    event.preventDefault();
    
    // Recopilar drops
    const dropInputs = document.querySelectorAll('.mob-drop');
    const drops = [];
    dropInputs.forEach(input => {
        if (input.value.trim()) {
            drops.push(input.value.trim());
        }
    });
    
    // Determinar scope seleccionado
    const scopeLocal = document.getElementById('mob-scope-local');
    const scopeUniversal = document.getElementById('mob-scope-universal');
    let scope = 'local';
    if (scopeUniversal && scopeUniversal.checked) {
        scope = 'universal';
    }
    
    const mobData = {
        id: document.getElementById('mob-id').value,
        displayName: document.getElementById('mob-name').value,
        entityType: document.getElementById('mob-type').value,
        level: parseInt(document.getElementById('mob-level').value),
        health: parseFloat(document.getElementById('mob-health').value),
        damage: parseFloat(document.getElementById('mob-damage').value),
        defense: parseFloat(document.getElementById('mob-defense').value),
        xpReward: parseInt(document.getElementById('mob-xp').value),
        isBoss: document.getElementById('mob-boss').value === 'true',
        spawnLocation: document.getElementById('mob-spawn').value || null,
        drops: drops,
        scope: scope
    };
    
    console.log('Creando mob:', mobData);
    
    try {
        const response = await fetch('/api/rpg/mob/save', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(mobData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            showToast('Mob creado correctamente', 'success');
            bootstrap.Modal.getInstance(document.getElementById('createMobModal')).hide();
            setTimeout(() => loadMobs(), 1000);
        } else {
            showToast('Error: ' + data.message, 'error');
        }
    } catch (error) {
        console.error('Error al crear Mob:', error);
        showToast('Error de conexión', 'error');
    }
}

/**
 * Edita un mob existente
 */
function editMob(mob, scope) {
    showCreateMobModal();
    setTimeout(() => {
        document.getElementById('mob-id').value = mob.id;
        document.getElementById('mob-id').disabled = true;
        document.getElementById('mob-edit-id').value = mob.id;
        document.getElementById('mob-name').value = mob.displayName;
        document.getElementById('mob-type').value = mob.entityType;
        document.getElementById('mob-level').value = mob.level;
        document.getElementById('mob-health').value = mob.health;
        document.getElementById('mob-damage').value = mob.damage;
        document.getElementById('mob-defense').value = mob.defense;
        document.getElementById('mob-xp').value = mob.xpReward;
        document.getElementById('mob-boss').value = mob.isBoss.toString();
        document.getElementById('mob-spawn').value = mob.spawnLocation || '';
        
        // Establecer el scope correcto
        const scopeLocal = document.getElementById('mob-scope-local');
        const scopeUniversal = document.getElementById('mob-scope-universal');
        if (scope === 'universal') {
            if (scopeUniversal) scopeUniversal.checked = true;
        } else {
            if (scopeLocal) scopeLocal.checked = true;
        }
        
        // Cargar drops
        const dropsContainer = document.getElementById('mob-drops-container');
        dropsContainer.innerHTML = '';
        if (mob.drops && mob.drops.length > 0) {
            mob.drops.forEach((drop, index) => {
                const dropHtml = `
                    <div class="input-group mb-2">
                        <input type="text" class="form-control mob-drop" value="${drop}">
                        <button type="button" class="btn btn-danger" onclick="this.parentElement.remove()">
                            <i class="bi bi-trash"></i>
                        </button>
                    </div>
                `;
                dropsContainer.insertAdjacentHTML('beforeend', dropHtml);
            });
        } else {
            addMobDrop();
        }
        
        document.querySelector('#createMobModal .modal-title').innerHTML = 
            '<i class="bi bi-pencil"></i> Editar Mob';
    }, 100);
}

/**
 * Elimina un mob
 */
async function deleteMob(mobId, scope) {
    if (!confirm(`¿Eliminar el mob "${mobId}"?`)) return;
    
    try {
        const response = await fetch(`/api/rpg/mob/${mobId}?scope=${scope}`, {
            method: 'DELETE'
        });
        
        const data = await response.json();
        
        if (data.success) {
            showToast('Mob eliminado correctamente', 'success');
            setTimeout(() => loadMobs(), 1000);
        } else {
            showToast('Error: ' + data.message, 'error');
        }
    } catch (error) {
        console.error('Error al eliminar Mob:', error);
        showToast('Error de conexión', 'error');
    }
}

// ============================================================================
// GESTIÓN DE SPAWNS
// ============================================================================

/**
 * Carga los spawns del mundo actual
 */
async function loadSpawns() {
    if (!rpgState.currentWorldSlug) return;
    
    try {
        const response = await fetch(`/api/worlds/${rpgState.currentWorldSlug}/rpg/spawns`);
        const data = await response.json();
        
        if (data.success) {
            renderSpawnsTable(data.spawns);
        } else {
            document.getElementById('spawns').innerHTML = `
                <div class="alert alert-warning">
                    <i class="bi bi-exclamation-triangle me-2"></i> ${data.message}
                </div>
            `;
        }
    } catch (error) {
        console.error('Error al cargar spawns:', error);
        document.getElementById('spawns').innerHTML = `
            <div class="alert alert-danger">
                <i class="bi bi-x-circle me-2"></i> Error de conexión al cargar spawns
            </div>
        `;
    }
}

/**
 * Renderiza la tabla de spawns
 */
function renderSpawnsTable(spawns) {
    const container = document.getElementById('spawns');
    
    if (!spawns || spawns.length === 0) {
        container.innerHTML = `
            <div class="card">
                <div class="card-body text-center py-5">
                    <i class="bi bi-pin-map text-muted" style="font-size: 3rem;"></i>
                    <p class="mt-3 text-muted">No hay spawns configurados</p>
                    <button class="btn btn-primary" onclick="showCreateSpawnModal()">
                        <i class="bi bi-plus-circle"></i> Crear Primer Spawn
                    </button>
                </div>
            </div>
        `;
        return;
    }
    
    let html = `
        <div class="d-flex justify-content-between align-items-center mb-3">
            <h4><i class="bi bi-pin-map"></i> Puntos de Spawn (${spawns.length})</h4>
            <button class="btn btn-primary" onclick="showCreateSpawnModal()">
                <i class="bi bi-plus-circle"></i> Nuevo Spawn
            </button>
        </div>
        <div class="table-responsive">
            <table class="table table-hover">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Tipo</th>
                        <th>Item/Entidad</th>
                        <th>Coordenadas</th>
                        <th>Respawn</th>
                        <th>Tiempo</th>
                        <th>Estado</th>
                        <th>Acciones</th>
                    </tr>
                </thead>
                <tbody>
    `;
    
    spawns.forEach(spawn => {
        const coords = `${spawn.x}, ${spawn.y}, ${spawn.z}`;
        const respawnInfo = spawn.respawn_enabled 
            ? `${spawn.respawn_time_seconds}s` 
            : 'No';
        const statusBadge = spawn.enabled 
            ? '<span class="badge bg-success">Activo</span>' 
            : '<span class="badge bg-secondary">Inactivo</span>';
        
        html += `
            <tr>
                <td><code>${spawn.id}</code></td>
                <td><span class="badge bg-info">${spawn.type}</span></td>
                <td>${spawn.item || spawn.entity_type || '-'}</td>
                <td><small>${coords}</small></td>
                <td>${respawnInfo}</td>
                <td>
                    ${spawn.respawn_on_death ? '<span class="badge bg-warning">Muerte</span> ' : ''}
                    ${spawn.respawn_on_use ? '<span class="badge bg-primary">Uso</span>' : ''}
                </td>
                <td>${statusBadge}</td>
                <td>
                    <button class="btn btn-sm btn-outline-primary" onclick='editSpawn(${JSON.stringify(spawn)})'>
                        <i class="bi bi-pencil"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-danger" onclick="deleteSpawn('${spawn.id}')">
                        <i class="bi bi-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    });
    
    html += `
                </tbody>
            </table>
        </div>
    `;
    
    container.innerHTML = html;
}

/**
 * Muestra el modal para crear spawn
 */
function showCreateSpawnModal() {
    // TODO: Crear modal de spawn
    alert('Funcionalidad en desarrollo. Por ahora puedes usar la API directamente:\nPOST /api/worlds/' + rpgState.currentWorldSlug + '/rpg/spawns');
}

/**
 * Edita un spawn
 */
function editSpawn(spawn) {
    // TODO: Mostrar modal de edición
    alert('Edición de spawn: ' + spawn.id);
}

/**
 * Elimina un spawn
 */
async function deleteSpawn(spawnId) {
    if (!confirm(`¿Eliminar el spawn "${spawnId}"?`)) return;
    
    try {
        const response = await fetch(`/api/worlds/${rpgState.currentWorldSlug}/rpg/spawns/${spawnId}`, {
            method: 'DELETE'
        });
        
        const data = await response.json();
        
        if (data.success) {
            showToast('Spawn eliminado correctamente', 'success');
            setTimeout(() => loadSpawns(), 500);
        } else {
            showToast('Error: ' + data.message, 'error');
        }
    } catch (error) {
        console.error('Error al eliminar spawn:', error);
        showToast('Error de conexión', 'error');
    }
}

// ============================================================================
// GESTIÓN DE DUNGEONS
// ============================================================================

/**
 * Carga las dungeons del mundo actual
 */
async function loadDungeons() {
    if (!rpgState.currentWorldSlug) return;
    
    try {
        const response = await fetch(`/api/worlds/${rpgState.currentWorldSlug}/rpg/dungeons`);
        const data = await response.json();
        
        if (data.success) {
            renderDungeonsTable(data.dungeons);
        } else {
            document.getElementById('dungeons').innerHTML = `
                <div class="alert alert-warning">
                    <i class="bi bi-exclamation-triangle me-2"></i> ${data.message}
                </div>
            `;
        }
    } catch (error) {
        console.error('Error al cargar dungeons:', error);
        document.getElementById('dungeons').innerHTML = `
            <div class="alert alert-danger">
                <i class="bi bi-x-circle me-2"></i> Error de conexión al cargar dungeons
            </div>
        `;
    }
}

/**
 * Renderiza la tabla de dungeons
 */
function renderDungeonsTable(dungeons) {
    const container = document.getElementById('dungeons');
    
    if (!dungeons || dungeons.length === 0) {
        container.innerHTML = `
            <div class="card">
                <div class="card-body text-center py-5">
                    <i class="bi bi-building text-muted" style="font-size: 3rem;"></i>
                    <p class="mt-3 text-muted">No hay dungeons configuradas</p>
                    <button class="btn btn-primary" onclick="showCreateDungeonModal()">
                        <i class="bi bi-plus-circle"></i> Crear Primera Dungeon
                    </button>
                </div>
            </div>
        `;
        return;
    }
    
    let html = `
        <div class="d-flex justify-content-between align-items-center mb-3">
            <h4><i class="bi bi-building"></i> Dungeons (${dungeons.length})</h4>
            <button class="btn btn-primary" onclick="showCreateDungeonModal()">
                <i class="bi bi-plus-circle"></i> Nueva Dungeon
            </button>
        </div>
        <div class="row g-3">
    `;
    
    dungeons.forEach(dungeon => {
        const difficultyColors = {
            'easy': 'success',
            'normal': 'primary',
            'hard': 'warning',
            'extreme': 'danger'
        };
        const diffColor = difficultyColors[dungeon.difficulty] || 'secondary';
        const statusBadge = dungeon.enabled 
            ? '<span class="badge bg-success">Activa</span>' 
            : '<span class="badge bg-secondary">Inactiva</span>';
        
        const location = dungeon.location && dungeon.location.x !== undefined
            ? `${dungeon.location.x}, ${dungeon.location.y}, ${dungeon.location.z}`
            : 'No definida';
        
        html += `
            <div class="col-md-6 col-lg-4">
                <div class="card h-100">
                    <div class="card-header bg-${diffColor} text-white">
                        <h5 class="mb-0">${dungeon.name}</h5>
                        <small>${statusBadge}</small>
                    </div>
                    <div class="card-body">
                        <p class="text-muted small">${dungeon.description || 'Sin descripción'}</p>
                        <hr>
                        <div class="mb-2">
                            <strong>ID:</strong> <code>${dungeon.id}</code>
                        </div>
                        <div class="mb-2">
                            <strong>Nivel:</strong> ${dungeon.min_level} - ${dungeon.max_level}
                        </div>
                        <div class="mb-2">
                            <strong>Dificultad:</strong> 
                            <span class="badge bg-${diffColor}">${dungeon.difficulty}</span>
                        </div>
                        <div class="mb-2">
                            <strong>Ubicación:</strong><br>
                            <small class="text-muted">${location}</small>
                        </div>
                        <div class="mb-2">
                            <strong>Salas:</strong> ${dungeon.rooms ? dungeon.rooms.length : 0}
                        </div>
                        <div class="mb-2">
                            <strong>Recompensas:</strong> ${dungeon.rewards ? dungeon.rewards.length : 0}
                        </div>
                    </div>
                    <div class="card-footer">
                        <button class="btn btn-sm btn-outline-primary" onclick='editDungeon(${JSON.stringify(dungeon).replace(/'/g, "\\'")})'> 
                            <i class="bi bi-pencil"></i> Editar
                        </button>
                        <button class="btn btn-sm btn-outline-danger" onclick="deleteDungeon('${dungeon.id}')">
                            <i class="bi bi-trash"></i> Eliminar
                        </button>
                    </div>
                </div>
            </div>
        `;
    });
    
    html += `
        </div>
    `;
    
    container.innerHTML = html;
}

/**
 * Muestra el modal para crear dungeon
 */
function showCreateDungeonModal() {
    // TODO: Crear modal de dungeon
    alert('Funcionalidad en desarrollo. Por ahora puedes usar la API directamente:\nPOST /api/worlds/' + rpgState.currentWorldSlug + '/rpg/dungeons');
}

/**
 * Edita una dungeon
 */
function editDungeon(dungeon) {
    // TODO: Mostrar modal de edición
    alert('Edición de dungeon: ' + dungeon.name);
}

/**
 * Elimina una dungeon
 */
async function deleteDungeon(dungeonId) {
    if (!confirm(`¿Eliminar la dungeon "${dungeonId}"?`)) return;
    
    try {
        const response = await fetch(`/api/worlds/${rpgState.currentWorldSlug}/rpg/dungeons/${dungeonId}`, {
            method: 'DELETE'
        });
        
        const data = await response.json();
        
        if (data.success) {
            showToast('Dungeon eliminada correctamente', 'success');
            setTimeout(() => loadDungeons(), 500);
        } else {
            showToast('Error: ' + data.message, 'error');
        }
    } catch (error) {
        console.error('Error al eliminar dungeon:', error);
        showToast('Error de conexión', 'error');
    }
}

// ============================================================================
// MODALES HTML
// ============================================================================

/**
 * Genera el HTML de los modales para Spawns
 */
function getSpawnModalsHTML() {
    return `
        <!-- Modal: Crear/Editar Spawn -->
        <div class="modal fade" id="spawnModal" tabindex="-1">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="spawnModalTitle">
                            <i class="bi bi-pin-map"></i> Crear Spawn
                        </h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <form id="spawnForm">
                            <input type="hidden" id="spawn-edit-mode" value="create">
                            <input type="hidden" id="spawn-original-id">
                            
                            <div class="mb-3">
                                <label for="spawn-id" class="form-label">ID del Spawn *</label>
                                <input type="text" class="form-control" id="spawn-id" required 
                                       placeholder="ej: spawn_chest_1">
                                <small class="text-muted">Identificador único del spawn</small>
                            </div>
                            
                            <div class="mb-3">
                                <label for="spawn-type" class="form-label">Tipo *</label>
                                <select class="form-select" id="spawn-type" required onchange="updateSpawnTypeFields()">
                                    <option value="item">Item</option>
                                    <option value="mob">Mob</option>
                                    <option value="npc">NPC</option>
                                </select>
                            </div>
                            
                            <div class="mb-3" id="spawn-item-group">
                                <label for="spawn-item" class="form-label">Item/Material *</label>
                                <input type="text" class="form-control" id="spawn-item" 
                                       placeholder="ej: DIAMOND, IRON_SWORD">
                                <small class="text-muted">Nombre del material de Minecraft</small>
                            </div>
                            
                            <div class="mb-3 d-none" id="spawn-entity-group">
                                <label for="spawn-entity" class="form-label">Tipo de Entidad *</label>
                                <input type="text" class="form-control" id="spawn-entity" 
                                       placeholder="ej: ZOMBIE, SKELETON">
                                <small class="text-muted">EntityType de Minecraft</small>
                            </div>
                            
                            <div class="row">
                                <div class="col-md-4 mb-3">
                                    <label for="spawn-x" class="form-label">X *</label>
                                    <input type="number" step="0.5" class="form-control" id="spawn-x" required>
                                </div>
                                <div class="col-md-4 mb-3">
                                    <label for="spawn-y" class="form-label">Y *</label>
                                    <input type="number" step="0.5" class="form-control" id="spawn-y" required>
                                </div>
                                <div class="col-md-4 mb-3">
                                    <label for="spawn-z" class="form-label">Z *</label>
                                    <input type="number" step="0.5" class="form-control" id="spawn-z" required>
                                </div>
                            </div>
                            
                            <div class="form-check mb-3">
                                <input class="form-check-input" type="checkbox" id="spawn-respawn-enabled" checked>
                                <label class="form-check-label" for="spawn-respawn-enabled">
                                    <strong>Habilitar Respawn</strong>
                                </label>
                            </div>
                            
                            <div id="spawn-respawn-config">
                                <div class="mb-3">
                                    <label for="spawn-respawn-time" class="form-label">Tiempo de Respawn (segundos)</label>
                                    <input type="number" class="form-control" id="spawn-respawn-time" value="300" min="1">
                                </div>
                                
                                <div class="form-check mb-2">
                                    <input class="form-check-input" type="checkbox" id="spawn-respawn-on-death" checked>
                                    <label class="form-check-label" for="spawn-respawn-on-death">
                                        Respawnear al morir
                                    </label>
                                </div>
                                
                                <div class="form-check mb-3">
                                    <input class="form-check-input" type="checkbox" id="spawn-respawn-on-use">
                                    <label class="form-check-label" for="spawn-respawn-on-use">
                                        Respawnear al usar/recoger
                                    </label>
                                </div>
                            </div>
                            
                            <div class="form-check mb-3">
                                <input class="form-check-input" type="checkbox" id="spawn-enabled" checked>
                                <label class="form-check-label" for="spawn-enabled">
                                    <strong>Spawn Activo</strong>
                                </label>
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                        <button type="button" class="btn btn-primary" onclick="saveSpawn()">
                            <i class="bi bi-save"></i> Guardar
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;
}

/**
 * Genera el HTML de los modales para Dungeons
 */
function getDungeonModalsHTML() {
    return `
        <!-- Modal: Crear/Editar Dungeon -->
        <div class="modal fade" id="dungeonModal" tabindex="-1">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="dungeonModalTitle">
                            <i class="bi bi-building"></i> Crear Dungeon
                        </h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <form id="dungeonForm">
                            <input type="hidden" id="dungeon-edit-mode" value="create">
                            <input type="hidden" id="dungeon-original-id">
                            
                            <div class="mb-3">
                                <label for="dungeon-id" class="form-label">ID de la Dungeon *</label>
                                <input type="text" class="form-control" id="dungeon-id" required 
                                       placeholder="ej: dungeon_ice_cave">
                                <small class="text-muted">Identificador único de la dungeon</small>
                            </div>
                            
                            <div class="mb-3">
                                <label for="dungeon-name" class="form-label">Nombre *</label>
                                <input type="text" class="form-control" id="dungeon-name" required 
                                       placeholder="ej: Cueva de Hielo">
                            </div>
                            
                            <div class="mb-3">
                                <label for="dungeon-description" class="form-label">Descripción</label>
                                <textarea class="form-control" id="dungeon-description" rows="2" 
                                          placeholder="Descripción breve de la dungeon"></textarea>
                            </div>
                            
                            <h6 class="mb-3">Ubicación</h6>
                            <div class="row">
                                <div class="col-md-4 mb-3">
                                    <label for="dungeon-x" class="form-label">X</label>
                                    <input type="number" step="1" class="form-control" id="dungeon-x" value="0">
                                </div>
                                <div class="col-md-4 mb-3">
                                    <label for="dungeon-y" class="form-label">Y</label>
                                    <input type="number" step="1" class="form-control" id="dungeon-y" value="64">
                                </div>
                                <div class="col-md-4 mb-3">
                                    <label for="dungeon-z" class="form-label">Z</label>
                                    <input type="number" step="1" class="form-control" id="dungeon-z" value="0">
                                </div>
                            </div>
                            
                            <div class="row">
                                <div class="col-md-6 mb-3">
                                    <label for="dungeon-min-level" class="form-label">Nivel Mínimo</label>
                                    <input type="number" class="form-control" id="dungeon-min-level" value="1" min="1">
                                </div>
                                <div class="col-md-6 mb-3">
                                    <label for="dungeon-max-level" class="form-label">Nivel Máximo</label>
                                    <input type="number" class="form-control" id="dungeon-max-level" value="100" min="1">
                                </div>
                            </div>
                            
                            <div class="mb-3">
                                <label for="dungeon-difficulty" class="form-label">Dificultad</label>
                                <select class="form-select" id="dungeon-difficulty">
                                    <option value="easy">Fácil</option>
                                    <option value="normal" selected>Normal</option>
                                    <option value="hard">Difícil</option>
                                    <option value="extreme">Extrema</option>
                                </select>
                            </div>
                            
                            <div class="form-check mb-3">
                                <input class="form-check-input" type="checkbox" id="dungeon-enabled" checked>
                                <label class="form-check-label" for="dungeon-enabled">
                                    <strong>Dungeon Activa</strong>
                                </label>
                            </div>
                            
                            <div class="alert alert-info">
                                <i class="bi bi-info-circle"></i>
                                <small>Las salas, boss y recompensas se pueden configurar más tarde editando el archivo JSON directamente.</small>
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                        <button type="button" class="btn btn-primary" onclick="saveDungeon()">
                            <i class="bi bi-save"></i> Guardar
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;
}

// ============================================================================
// FUNCIONES CRUD COMPLETAS
// ============================================================================

/**
 * Muestra el modal para crear spawn
 */
function showCreateSpawnModal() {
    document.getElementById('spawn-edit-mode').value = 'create';
    document.getElementById('spawnModalTitle').innerHTML = '<i class="bi bi-pin-map"></i> Crear Spawn';
    
    // Limpiar formulario
    document.getElementById('spawnForm').reset();
    document.getElementById('spawn-id').disabled = false;
    document.getElementById('spawn-respawn-enabled').checked = true;
    document.getElementById('spawn-enabled').checked = true;
    document.getElementById('spawn-respawn-on-death').checked = true;
    document.getElementById('spawn-respawn-time').value = 300;
    
    // Mostrar campos correctos según tipo
    updateSpawnTypeFields();
    
    // Mostrar modal
    const modal = new bootstrap.Modal(document.getElementById('spawnModal'));
    modal.show();
}

/**
 * Edita un spawn existente
 */
function editSpawn(spawn) {
    document.getElementById('spawn-edit-mode').value = 'edit';
    document.getElementById('spawn-original-id').value = spawn.id;
    document.getElementById('spawnModalTitle').innerHTML = '<i class="bi bi-pencil"></i> Editar Spawn';
    
    // Llenar formulario
    document.getElementById('spawn-id').value = spawn.id;
    document.getElementById('spawn-id').disabled = true;
    document.getElementById('spawn-type').value = spawn.type;
    document.getElementById('spawn-item').value = spawn.item || '';
    document.getElementById('spawn-entity').value = spawn.entity_type || '';
    document.getElementById('spawn-x').value = spawn.x;
    document.getElementById('spawn-y').value = spawn.y;
    document.getElementById('spawn-z').value = spawn.z;
    document.getElementById('spawn-respawn-enabled').checked = spawn.respawn_enabled;
    document.getElementById('spawn-respawn-time').value = spawn.respawn_time_seconds;
    document.getElementById('spawn-respawn-on-death').checked = spawn.respawn_on_death;
    document.getElementById('spawn-respawn-on-use').checked = spawn.respawn_on_use;
    document.getElementById('spawn-enabled').checked = spawn.enabled;
    
    // Actualizar campos visibles
    updateSpawnTypeFields();
    
    // Mostrar modal
    const modal = new bootstrap.Modal(document.getElementById('spawnModal'));
    modal.show();
}

/**
 * Actualiza los campos visibles según el tipo de spawn
 */
function updateSpawnTypeFields() {
    const type = document.getElementById('spawn-type').value;
    const itemGroup = document.getElementById('spawn-item-group');
    const entityGroup = document.getElementById('spawn-entity-group');
    
    if (type === 'item') {
        itemGroup.classList.remove('d-none');
        entityGroup.classList.add('d-none');
        document.getElementById('spawn-item').required = true;
        document.getElementById('spawn-entity').required = false;
    } else {
        itemGroup.classList.add('d-none');
        entityGroup.classList.remove('d-none');
        document.getElementById('spawn-item').required = false;
        document.getElementById('spawn-entity').required = true;
    }
}

/**
 * Guarda un spawn (crear o editar)
 */
async function saveSpawn() {
    const form = document.getElementById('spawnForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    
    const editMode = document.getElementById('spawn-edit-mode').value;
    const originalId = document.getElementById('spawn-original-id').value;
    
    const spawnData = {
        id: document.getElementById('spawn-id').value,
        type: document.getElementById('spawn-type').value,
        item: document.getElementById('spawn-item').value,
        entity_type: document.getElementById('spawn-entity').value,
        x: parseFloat(document.getElementById('spawn-x').value),
        y: parseFloat(document.getElementById('spawn-y').value),
        z: parseFloat(document.getElementById('spawn-z').value),
        respawn_enabled: document.getElementById('spawn-respawn-enabled').checked,
        respawn_time_seconds: parseInt(document.getElementById('spawn-respawn-time').value),
        respawn_on_death: document.getElementById('spawn-respawn-on-death').checked,
        respawn_on_use: document.getElementById('spawn-respawn-on-use').checked,
        enabled: document.getElementById('spawn-enabled').checked
    };
    
    try {
        let response;
        if (editMode === 'create') {
            response = await fetch(`/api/worlds/${rpgState.currentWorldSlug}/rpg/spawns`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(spawnData)
            });
        } else {
            response = await fetch(`/api/worlds/${rpgState.currentWorldSlug}/rpg/spawns/${originalId}`, {
                method: 'PUT',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(spawnData)
            });
        }
        
        const data = await response.json();
        
        if (data.success) {
            showToast(data.message || 'Spawn guardado correctamente', 'success');
            bootstrap.Modal.getInstance(document.getElementById('spawnModal')).hide();
            setTimeout(() => loadSpawns(), 500);
        } else {
            showToast('Error: ' + data.error, 'error');
        }
    } catch (error) {
        console.error('Error al guardar spawn:', error);
        showToast('Error de conexión', 'error');
    }
}

/**
 * Muestra el modal para crear dungeon
 */
function showCreateDungeonModal() {
    document.getElementById('dungeon-edit-mode').value = 'create';
    document.getElementById('dungeonModalTitle').innerHTML = '<i class="bi bi-building"></i> Crear Dungeon';
    
    // Limpiar formulario
    document.getElementById('dungeonForm').reset();
    document.getElementById('dungeon-id').disabled = false;
    document.getElementById('dungeon-enabled').checked = true;
    document.getElementById('dungeon-min-level').value = 1;
    document.getElementById('dungeon-max-level').value = 100;
    document.getElementById('dungeon-difficulty').value = 'normal';
    document.getElementById('dungeon-x').value = 0;
    document.getElementById('dungeon-y').value = 64;
    document.getElementById('dungeon-z').value = 0;
    
    // Mostrar modal
    const modal = new bootstrap.Modal(document.getElementById('dungeonModal'));
    modal.show();
}

/**
 * Edita una dungeon existente
 */
function editDungeon(dungeon) {
    document.getElementById('dungeon-edit-mode').value = 'edit';
    document.getElementById('dungeon-original-id').value = dungeon.id;
    document.getElementById('dungeonModalTitle').innerHTML = '<i class="bi bi-pencil"></i> Editar Dungeon';
    
    // Llenar formulario
    document.getElementById('dungeon-id').value = dungeon.id;
    document.getElementById('dungeon-id').disabled = true;
    document.getElementById('dungeon-name').value = dungeon.name;
    document.getElementById('dungeon-description').value = dungeon.description || '';
    document.getElementById('dungeon-min-level').value = dungeon.min_level;
    document.getElementById('dungeon-max-level').value = dungeon.max_level;
    document.getElementById('dungeon-difficulty').value = dungeon.difficulty;
    document.getElementById('dungeon-enabled').checked = dungeon.enabled;
    
    if (dungeon.location) {
        document.getElementById('dungeon-x').value = dungeon.location.x || 0;
        document.getElementById('dungeon-y').value = dungeon.location.y || 64;
        document.getElementById('dungeon-z').value = dungeon.location.z || 0;
    }
    
    // Mostrar modal
    const modal = new bootstrap.Modal(document.getElementById('dungeonModal'));
    modal.show();
}

/**
 * Guarda una dungeon (crear o editar)
 */
async function saveDungeon() {
    const form = document.getElementById('dungeonForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    
    const editMode = document.getElementById('dungeon-edit-mode').value;
    const originalId = document.getElementById('dungeon-original-id').value;
    
    const dungeonData = {
        id: document.getElementById('dungeon-id').value,
        name: document.getElementById('dungeon-name').value,
        description: document.getElementById('dungeon-description').value,
        location: {
            x: parseInt(document.getElementById('dungeon-x').value),
            y: parseInt(document.getElementById('dungeon-y').value),
            z: parseInt(document.getElementById('dungeon-z').value)
        },
        min_level: parseInt(document.getElementById('dungeon-min-level').value),
        max_level: parseInt(document.getElementById('dungeon-max-level').value),
        difficulty: document.getElementById('dungeon-difficulty').value,
        enabled: document.getElementById('dungeon-enabled').checked,
        rooms: [],
        boss: {},
        rewards: []
    };
    
    try {
        let response;
        if (editMode === 'create') {
            response = await fetch(`/api/worlds/${rpgState.currentWorldSlug}/rpg/dungeons`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(dungeonData)
            });
        } else {
            response = await fetch(`/api/worlds/${rpgState.currentWorldSlug}/rpg/dungeons/${originalId}`, {
                method: 'PUT',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(dungeonData)
            });
        }
        
        const data = await response.json();
        
        if (data.success) {
            showToast(data.message || 'Dungeon guardada correctamente', 'success');
            bootstrap.Modal.getInstance(document.getElementById('dungeonModal')).hide();
            setTimeout(() => loadDungeons(), 500);
        } else {
            showToast('Error: ' + data.error, 'error');
        }
    } catch (error) {
        console.error('Error al guardar dungeon:', error);
        showToast('Error de conexión', 'error');
    }
}
