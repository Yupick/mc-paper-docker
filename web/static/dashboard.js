// dashboard.js - Panel de Administración Minecraft Completo

let currentFile = null;
let statsChart = null;
let cpuChart = null;
let memoryChart = null;
let passwordChangeModal = null;

// ==================== SEGURIDAD Y AUTENTICACIÓN ====================

// Verificar si requiere cambio de contraseña al cargar
async function checkPasswordSecurity() {
    try {
        const response = await fetch('/api/auth/check-password-security');
        const data = await response.json();
        
        if (data.requires_change) {
            // Mostrar modal de cambio obligatorio
            if (!passwordChangeModal) {
                passwordChangeModal = new bootstrap.Modal(document.getElementById('passwordChangeModal'));
            }
            passwordChangeModal.show();
        }
    } catch (error) {
        console.error('Error checking password security:', error);
    }
}

// Enviar cambio de contraseña
async function submitPasswordChange() {
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    const email = document.getElementById('adminEmail').value;
    const errorDiv = document.getElementById('passwordChangeError');
    
    // Validaciones client-side
    errorDiv.classList.add('d-none');
    
    if (!newPassword || !confirmPassword) {
        errorDiv.textContent = 'Por favor, completa todos los campos obligatorios';
        errorDiv.classList.remove('d-none');
        return;
    }
    
    if (newPassword !== confirmPassword) {
        errorDiv.textContent = 'Las contraseñas no coinciden';
        errorDiv.classList.remove('d-none');
        return;
    }
    
    if (newPassword.length < 8) {
        errorDiv.textContent = 'La contraseña debe tener al menos 8 caracteres';
        errorDiv.classList.remove('d-none');
        return;
    }
    
    try {
        const response = await fetch('/api/auth/change-password', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                new_password: newPassword,
                confirm_password: confirmPassword,
                email: email || null
            })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showToast(data.message, 'success');
            
            // Cerrar modal y redirigir a login
            setTimeout(() => {
                window.location.href = '/logout';
            }, 2000);
        } else {
            errorDiv.textContent = data.message;
            errorDiv.classList.remove('d-none');
        }
    } catch (error) {
        errorDiv.textContent = 'Error al cambiar contraseña: ' + error.message;
        errorDiv.classList.remove('d-none');
    }
}

// ==================== UTILIDADES ====================

function showToast(message, type = 'success') {
    const toastContainer = document.getElementById('toast-container');
    const toastId = 'toast-' + Date.now();
    const bgClass = type === 'success' ? 'bg-success' : type === 'error' ? 'bg-danger' : 'bg-warning';
    const icon = type === 'success' ? 'check-circle-fill' : type === 'error' ? 'exclamation-triangle-fill' : 'info-circle-fill';
    
    const toastHTML = `
        <div class="toast align-items-center text-white ${bgClass} border-0" id="${toastId}" role="alert">
            <div class="d-flex">
                <div class="toast-body">
                    <i class="bi bi-${icon} me-2"></i>
                    ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        </div>
    `;
    
    toastContainer.insertAdjacentHTML('beforeend', toastHTML);
    const toastElement = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastElement, { delay: 3000 });
    toast.show();
    
    toastElement.addEventListener('hidden.bs.toast', () => toastElement.remove());
}

function toggleTheme() {
    const html = document.documentElement;
    const currentTheme = html.getAttribute('data-bs-theme');
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
    const icon = document.getElementById('theme-icon');
    
    html.setAttribute('data-bs-theme', newTheme);
    localStorage.setItem('theme', newTheme);
    icon.className = newTheme === 'dark' ? 'bi bi-sun-fill' : 'bi bi-moon-stars';
}

function showSection(sectionName) {
    // Ocultar todas las secciones
    document.querySelectorAll('.content-section').forEach(section => {
        section.classList.remove('active');
    });
    
    // Mostrar sección seleccionada
    document.getElementById('section-' + sectionName).classList.add('active');
    
    // Actualizar sidebar
    document.querySelectorAll('.sidebar-item').forEach(item => {
        item.classList.remove('active');
    });
    event.target.closest('.sidebar-item').classList.add('active');
    
    // Cargar datos según la sección
    switch(sectionName) {
        case 'dashboard':
            loadDashboard();
            break;
        case 'config':
            loadServerProperties();
            break;
        case 'players':
            loadPlayersSection();
            break;
        case 'plugins':
            loadPluginsSection();
            break;
        case 'files':
            loadFilesTree();
            break;
        case 'worlds':
            loadWorldsSection();
            break;
        case 'backups':
            loadBackupsSection();
            break;
        case 'whitelist':
            loadWhitelist();
            break;
        case 'ops':
            loadOpsSection();
            break;
        case 'stats':
            loadStatsSection();
            break;
    }
}

// ==================== DASHBOARD ====================

function loadDashboard() {
    loadServerStatus();
    loadLogs();
    loadTPS();
}

function loadServerStatus() {
    fetch('/api/server/status')
        .then(r => r.json())
        .then(data => {
            const statusBadge = document.getElementById('server-status-dash');
            if (data.running) {
                statusBadge.innerHTML = '<span class="badge bg-success">ONLINE</span>';
                document.getElementById('cpu-dash').textContent = Math.round(data.cpu_percent) + '%';
                document.getElementById('cpu-progress-dash').style.width = data.cpu_percent + '%';
                document.getElementById('memory-dash').textContent = Math.round(data.memory_usage_mb) + ' MB';
                document.getElementById('memory-progress-dash').style.width = data.memory_percent + '%';
                document.getElementById('memory-detail-dash').textContent = 
                    Math.round(data.memory_usage_mb) + ' / ' + Math.round(data.memory_limit_mb) + ' MB';
            } else {
                statusBadge.innerHTML = '<span class="badge bg-danger">OFFLINE</span>';
            }
        });
    
    fetch('/api/server/version')
        .then(r => r.json())
        .then(data => {
            document.getElementById('server-version-dash').textContent = 'Versión: ' + data.version;
        });
    
    fetch('/api/server/uptime')
        .then(r => r.json())
        .then(data => {
            document.getElementById('server-uptime-dash').textContent = 'Uptime: ' + data.uptime;
        });
    
    fetch('/api/server/players')
        .then(r => r.json())
        .then(data => {
            document.getElementById('players-count-dash').textContent = data.online + '/' + data.max;
        });
}

function loadLogs() {
    fetch('/api/server/logs')
        .then(r => r.json())
        .then(data => {
            document.getElementById('logs-dash').textContent = data.logs;
        });
}

function loadTPS() {
    fetch('/api/server/tps')
        .then(r => r.json())
        .then(data => {
            if (!data.error) {
                document.getElementById('tps-1m').textContent = data.tps_1m.toFixed(1);
                document.getElementById('tps-5m').textContent = data.tps_5m.toFixed(1);
                document.getElementById('tps-15m').textContent = data.tps_15m.toFixed(1);
            }
        })
        .catch(e => console.error('TPS error:', e));
}

// ==================== SERVER CONTROLS ====================

function startServer() {
    if (confirm('¿Iniciar el servidor?')) {
        showToast('Iniciando servidor...', 'info');
        fetch('/api/server/start', { method: 'POST' })
            .then(r => r.json())
            .then(data => {
                showToast(data.message, data.success ? 'success' : 'error');
                setTimeout(loadServerStatus, 2000);
            });
    }
}

function restartServer() {
    if (confirm('¿Reiniciar el servidor? Los jugadores serán desconectados.')) {
        showToast('Reiniciando servidor...', 'info');
        fetch('/api/server/restart', { method: 'POST' })
            .then(r => r.json())
            .then(data => {
                showToast(data.message, data.success ? 'success' : 'error');
                setTimeout(loadServerStatus, 2000);
            });
    }
}

function stopServer() {
    if (confirm('¿Detener el servidor? Los jugadores serán desconectados.')) {
        showToast('Deteniendo servidor...', 'info');
        fetch('/api/server/stop', { method: 'POST' })
            .then(r => r.json())
            .then(data => {
                showToast(data.message, data.success ? 'success' : 'error');
                setTimeout(loadServerStatus, 2000);
            });
    }
}

function updateServer() {
    if (confirm('¿Actualizar PaperMC a la última versión? El servidor será reiniciado.')) {
        showToast('Actualizando... Esto puede tardar varios minutos.', 'info');
        fetch('/api/server/update', { method: 'POST' })
            .then(r => r.json())
            .then(data => {
                showToast(data.message, data.success ? 'success' : 'error');
            });
    }
}

// ==================== CONFIGURATION ====================

function loadServerProperties() {
    fetch('/api/config/server-properties')
        .then(r => r.json())
        .then(data => {
            document.getElementById('server-properties').value = data.content;
            parseQuickConfig(data.content);
        });
}

function parseQuickConfig(content) {
    const lines = content.split('\n');
    const props = {};
    
    lines.forEach(line => {
        if (line && !line.startsWith('#') && line.includes('=')) {
            const [key, value] = line.split('=');
            props[key.trim()] = value.trim();
        }
    });
    
    if (props.difficulty) document.getElementById('difficulty').value = props.difficulty;
    if (props.gamemode) document.getElementById('gamemode').value = props.gamemode;
    if (props['max-players']) document.getElementById('max-players').value = props['max-players'];
    if (props['view-distance']) document.getElementById('view-distance').value = props['view-distance'];
    document.getElementById('pvp').checked = props.pvp === 'true';
    document.getElementById('whitelist-enabled').checked = props['white-list'] === 'true';
}

function saveQuickConfig() {
    const updates = [
        { key: 'difficulty', value: document.getElementById('difficulty').value },
        { key: 'gamemode', value: document.getElementById('gamemode').value },
        { key: 'max-players', value: document.getElementById('max-players').value },
        { key: 'view-distance', value: document.getElementById('view-distance').value },
        { key: 'pvp', value: document.getElementById('pvp').checked },
        { key: 'white-list', value: document.getElementById('whitelist-enabled').checked }
    ];
    
    Promise.all(updates.map(update => 
        fetch('/api/config/update-property', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(update)
        })
    )).then(() => {
        showToast('Configuración actualizada. Reinicia el servidor para aplicar cambios.', 'success');
        loadServerProperties();
    });
}

function saveServerProperties() {
    const content = document.getElementById('server-properties').value;
    fetch('/api/config/server-properties', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content })
    })
    .then(r => r.json())
    .then(data => {
        showToast(data.message + ' - Reinicia el servidor.', data.success ? 'success' : 'error');
    });
}

// ==================== PLAYERS ====================

function loadPlayersSection() {
    loadPlayersList();
    loadChatMessages();
    setInterval(loadPlayersList, 10000);
    setInterval(loadChatMessages, 5000);
}

function loadPlayersList() {
    fetch('/api/server/players')
        .then(r => r.json())
        .then(data => {
            const list = document.getElementById('players-list-section');
            document.getElementById('online-count').textContent = data.online;
            
            if (data.players && data.players.length > 0) {
                list.innerHTML = '';
                data.players.forEach(player => {
                    const card = document.createElement('div');
                    card.className = 'player-card';
                    card.innerHTML = `
                        <div class="player-info">
                            <div class="player-avatar">${player[0].toUpperCase()}</div>
                            <div>
                                <strong>${player}</strong>
                                <br><small class="text-muted">Conectado</small>
                            </div>
                        </div>
                        <div class="d-flex gap-2">
                            <button class="btn btn-sm btn-warning" onclick="kickPlayer('${player}')">
                                <i class="bi bi-door-open"></i> Kick
                            </button>
                            <button class="btn btn-sm btn-danger" onclick="banPlayer('${player}')">
                                <i class="bi bi-ban"></i> Ban
                            </button>
                            <select class="form-select form-select-sm" onchange="changePlayerGamemode('${player}', this.value)" style="width:auto;">
                                <option value="">Gamemode...</option>
                                <option value="survival">Survival</option>
                                <option value="creative">Creative</option>
                                <option value="adventure">Adventure</option>
                                <option value="spectator">Spectator</option>
                            </select>
                        </div>
                    `;
                    list.appendChild(card);
                });
            } else {
                list.innerHTML = '<p class="text-muted text-center">No hay jugadores conectados</p>';
            }
        });
}

function kickPlayer(player) {
    const reason = prompt('Razón del kick:', 'Kickeado por un administrador');
    if (reason !== null) {
        fetch('/api/players/kick', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ player, reason })
        })
        .then(r => r.json())
        .then(data => {
            showToast(data.message, data.success ? 'success' : 'error');
            loadPlayersList();
        });
    }
}

function banPlayer(player) {
    const reason = prompt('Razón del ban:', 'Baneado por un administrador');
    if (reason !== null) {
        fetch('/api/players/ban', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ player, reason })
        })
        .then(r => r.json())
        .then(data => {
            showToast(data.message, data.success ? 'success' : 'error');
            loadPlayersList();
        });
    }
}

function changePlayerGamemode(player, gamemode) {
    if (gamemode) {
        fetch('/api/players/gamemode', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ player, gamemode })
        })
        .then(r => r.json())
        .then(data => {
            showToast(data.message, data.success ? 'success' : 'error');
        });
    }
}

function loadChatMessages() {
    fetch('/api/server/chat')
        .then(r => r.json())
        .then(data => {
            const chat = document.getElementById('chat-messages');
            if (data.messages && data.messages.length > 0) {
                chat.innerHTML = data.messages.map(msg => 
                    `<div><strong>${msg.player}:</strong> ${msg.message}</div>`
                ).join('\n');
                chat.scrollTop = chat.scrollHeight;
            } else {
                chat.textContent = 'No hay mensajes de chat recientes.';
            }
        });
}

function sendChatMessage() {
    const input = document.getElementById('chat-message');
    const message = input.value.trim();
    
    if (message) {
        fetch('/api/server/say', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message })
        })
        .then(r => r.json())
        .then(data => {
            if (data.success) {
                input.value = '';
                loadChatMessages();
            } else {
                showToast(data.error, 'error');
            }
        });
    }
}

// ==================== CONSOLE ====================

function executeQuickCommand(command) {
    document.getElementById('console-command').value = command;
    executeCommand();
}

function executeCommand() {
    const command = document.getElementById('console-command').value.trim();
    
    if (command) {
        const output = document.getElementById('console-output');
        output.textContent += `\n> ${command}\n`;
        
        fetch('/api/server/command', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ command })
        })
        .then(r => r.json())
        .then(data => {
            if (data.success) {
                output.textContent += data.output + '\n';
            } else {
                output.textContent += `Error: ${data.error || data.message || 'Error desconocido'}\n`;
            }
            output.scrollTop = output.scrollHeight;
            document.getElementById('console-command').value = '';
        })
        .catch(error => {
            output.textContent += `Error de red: ${error.message}\n`;
            output.scrollTop = output.scrollHeight;
        });
    }
}

// ==================== PLUGINS ====================

function loadPluginsSection() {
    fetch('/api/plugins/detailed')
        .then(r => r.json())
        .then(data => {
            const list = document.getElementById('plugins-list-section');
            document.getElementById('plugins-count').textContent = data.plugins.length;
            
            if (data.plugins.length === 0) {
                list.innerHTML = '<p class="text-muted text-center">No hay plugins instalados</p>';
            } else {
                list.innerHTML = '';
                data.plugins.forEach(plugin => {
                    const item = document.createElement('div');
                    item.className = 'player-card';
                    item.innerHTML = `
                        <div class="player-info">
                            <div class="player-avatar"><i class="bi bi-puzzle"></i></div>
                            <div>
                                <strong>${plugin.plugin_name}</strong>
                                <br><small class="text-muted">${plugin.size_mb} MB | ${plugin.status}</small>
                            </div>
                        </div>
                        <div class="d-flex gap-2">
                            <button class="btn btn-sm ${plugin.active ? 'btn-success' : 'btn-secondary'}" 
                                    onclick="togglePlugin('${plugin.name}')">
                                <i class="bi bi-${plugin.active ? 'toggle-on' : 'toggle-off'}"></i>
                                ${plugin.active ? 'Activo' : 'Inactivo'}
                            </button>
                            <button class="btn btn-sm btn-danger" onclick="deletePlugin('${plugin.name}')">
                                <i class="bi bi-trash"></i>
                            </button>
                        </div>
                    `;
                    list.appendChild(item);
                });
            }
        });
}

function uploadPlugin() {
    const fileInput = document.getElementById('plugin-file-upload');
    const file = fileInput.files[0];
    
    if (file && file.name.endsWith('.jar')) {
        const formData = new FormData();
        formData.append('file', file);
        
        showToast('Subiendo plugin...', 'info');
        fetch('/api/plugins/upload', {
            method: 'POST',
            body: formData
        })
        .then(r => r.json())
        .then(data => {
            showToast(data.message, data.success ? 'success' : 'error');
            fileInput.value = '';
            loadPluginsSection();
        });
    } else {
        showToast('Selecciona un archivo .jar', 'error');
    }
}

function togglePlugin(name) {
    fetch('/api/plugins/toggle', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name })
    })
    .then(r => r.json())
    .then(data => {
        showToast(data.message, data.success ? 'success' : 'error');
        loadPluginsSection();
    });
}

function deletePlugin(name) {
    if (confirm(`¿Eliminar ${name}?`)) {
        fetch('/api/plugins/delete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name })
        })
        .then(r => r.json())
        .then(data => {
            showToast(data.message, data.success ? 'success' : 'error');
            loadPluginsSection();
        });
    }
}

function updateAllPlugins() {
    if (confirm('¿Actualizar todos los plugins?')) {
        showToast('Actualizando plugins...', 'info');
        fetch('/api/plugins/update-all', { method: 'POST' })
            .then(r => r.json())
            .then(data => {
                showToast(data.message, data.success ? 'success' : 'error');
                loadPluginsSection();
            });
    }
}

function reloadPlugins() {
    if (confirm('¿Recargar todos los plugins? Esto puede causar problemas si los plugins no soportan reload.')) {
        showToast('Recargando plugins...', 'info');
        fetch('/api/plugins/reload', { method: 'POST' })
            .then(r => r.json())
            .then(data => {
                showToast(data.message, data.success ? 'success' : 'error');
            });
    }
}

// ==================== FILES EDITOR ====================

function loadFilesTree() {
    fetch('/api/files/list')
        .then(r => r.json())
        .then(data => {
            const tree = document.getElementById('files-tree');
            if (data.files.length === 0) {
                tree.innerHTML = '<p class="text-muted">No hay archivos disponibles</p>';
            } else {
                tree.innerHTML = '';
                data.files.forEach(file => {
                    const item = document.createElement('div');
                    item.className = 'file-item';
                    item.innerHTML = `
                        <i class="bi bi-file-earmark-text"></i>
                        ${file.name}
                        <small class="text-muted d-block">${file.path}</small>
                    `;
                    item.onclick = () => loadFileContent(file.path, file.name);
                    tree.appendChild(item);
                });
            }
        });
}

function loadFileContent(path, name) {
    fetch(`/api/files/read?path=${encodeURIComponent(path)}`)
        .then(r => r.json())
        .then(data => {
            document.getElementById('file-content-editor').value = data.content;
            document.getElementById('current-file-name').innerHTML = `<i class="bi bi-file-code"></i> ${name}`;
            document.getElementById('save-file-btn').style.display = 'block';
            currentFile = path;
            
            // Highlight seleccionado
            document.querySelectorAll('.file-item').forEach(item => {
                item.classList.remove('selected');
            });
            event.target.closest('.file-item').classList.add('selected');
        });
}

function saveCurrentFile() {
    if (currentFile) {
        const content = document.getElementById('file-content-editor').value;
        
        fetch('/api/files/save', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ path: currentFile, content })
        })
        .then(r => r.json())
        .then(data => {
            showToast(data.message, data.success ? 'success' : 'error');
        });
    }
}

// ==================== WORLDS ====================

function loadWorldsSection() {
    fetch('/api/worlds/list')
        .then(r => r.json())
        .then(data => {
            const list = document.getElementById('worlds-list');
            if (data.worlds.length === 0) {
                list.innerHTML = '<p class="text-muted text-center">No hay mundos disponibles</p>';
            } else {
                list.innerHTML = '';
                data.worlds.forEach(world => {
                    const card = document.createElement('div');
                    card.className = 'player-card';
                    card.innerHTML = `
                        <div class="player-info">
                            <div class="player-avatar"><i class="bi bi-globe"></i></div>
                            <div>
                                <strong>${world.name}</strong>
                                <br><small class="text-muted">${world.size_mb} MB</small>
                            </div>
                        </div>
                        <button class="btn btn-sm btn-primary" onclick="backupWorld('${world.name}')">
                            <i class="bi bi-archive"></i> Backup
                        </button>
                    `;
                    list.appendChild(card);
                });
            }
        });
}

function backupWorld(worldName) {
    showToast('Creando backup del mundo...', 'info');
    fetch('/api/worlds/backup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ world: worldName })
    })
    .then(r => r.json())
    .then(data => {
        showToast(data.message, data.success ? 'success' : 'error');
    });
}

// ==================== BACKUPS ====================

// Cargar configuración de backups al mostrar la sección
async function loadBackupConfig() {
    try {
        const response = await fetch('/api/backup-config');
        const data = await response.json();
        
        if (data.success) {
            const config = data.config;
            
            // Actualizar toggle
            const toggle = document.getElementById('autoBackupEnabled');
            toggle.checked = config.auto_backup_enabled;
            
            // Actualizar label
            updateAutoBackupLabel(config.auto_backup_enabled);
            
            // Actualizar select de retención
            const retention = document.getElementById('autoBackupRetention');
            if (retention) {
                retention.value = config.retention_count || 5;
            }
        }
    } catch (error) {
        console.error('Error loading backup config:', error);
    }
}

// Actualizar label del toggle
function updateAutoBackupLabel(enabled) {
    const label = document.getElementById('autoBackupLabel');
    if (enabled) {
        label.innerHTML = '<span class=\"badge bg-success\">Activado</span>';
    } else {
        label.innerHTML = '<span class=\"badge bg-secondary\">Desactivado</span>';
    }
}

// Toggle de backups automáticos
async function toggleAutoBackup() {
    const toggle = document.getElementById('autoBackupEnabled');
    const enabled = toggle.checked;
    
    try {
        const response = await fetch('/api/backup-config', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                auto_backup_enabled: enabled
            })
        });
        
        const data = await response.json();
        
        if (data.success) {
            updateAutoBackupLabel(enabled);
            showNotification(
                enabled ? 'Backups automáticos activados' : 'Backups automáticos desactivados',
                'success'
            );
        } else {
            // Revertir toggle en caso de error
            toggle.checked = !enabled;
            showNotification('Error: ' + (data.error || 'Error desconocido'), 'danger');
        }
    } catch (error) {
        console.error('Error toggling auto backup:', error);
        toggle.checked = !enabled;
        showNotification('Error de conexión', 'danger');
    }
}

// Actualizar cantidad de backups a conservar
async function updateBackupRetention() {
    const retention = document.getElementById('autoBackupRetention');
    const retentionCount = parseInt(retention.value);
    
    try {
        const response = await fetch('/api/backup-config', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                retention_count: retentionCount
            })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showNotification(`Se conservarán los últimos ${retentionCount} backups automáticos`, 'success');
        } else {
            showNotification('Error: ' + (data.error || 'Error desconocido'), 'danger');
        }
    } catch (error) {
        console.error('Error updating retention:', error);
        showNotification('Error de conexión', 'danger');
    }
}

function loadBackupsSection() {
    // Cargar configuración
    loadBackupConfig();
    
    fetch('/api/backup/list')
        .then(r => r.json())
        .then(data => {
            const list = document.getElementById('backups-list-section');
            document.getElementById('backups-count').textContent = data.backups.length;
            
            if (data.backups.length === 0) {
                list.innerHTML = '<p class="text-muted text-center">No hay backups disponibles</p>';
            } else {
                list.innerHTML = '';
                data.backups.forEach(backup => {
                    const card = document.createElement('div');
                    card.className = 'player-card';
                    card.innerHTML = `
                        <div class="player-info">
                            <div class="player-avatar"><i class="bi bi-archive"></i></div>
                            <div>
                                <strong>${backup.name}</strong>
                                <br><small class="text-muted">${backup.date} | ${backup.size_mb} MB</small>
                            </div>
                        </div>
                        <div class="d-flex gap-2">
                            <a href="/api/backup/download/${backup.name}" class="btn btn-sm btn-primary">
                                <i class="bi bi-download"></i> Descargar
                            </a>
                            <button class="btn btn-sm btn-warning" onclick="restoreBackup('${backup.name}')">
                                <i class="bi bi-arrow-counterclockwise"></i> Restaurar
                            </button>
                            <button class="btn btn-sm btn-danger" onclick="deleteBackup('${backup.name}')">
                                <i class="bi bi-trash"></i>
                            </button>
                        </div>
                    `;
                    list.appendChild(card);
                });
            }
        });
}

function createBackup() {
    if (confirm('¿Crear backup completo del servidor?')) {
        showToast('Creando backup...', 'info');
        fetch('/api/backup/create', { method: 'POST' })
            .then(r => r.json())
            .then(data => {
                showToast(data.message, data.success ? 'success' : 'error');
                loadBackupsSection();
            });
    }
}

function restoreBackup(name) {
    if (confirm(`¿Restaurar desde ${name}?\n\nADVERTENCIA: Esto reemplazará los mundos actuales. Se creará un backup automático antes de restaurar.`)) {
        showToast('Restaurando backup... El servidor será reiniciado.', 'info');
        fetch('/api/backup/restore', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name })
        })
        .then(r => r.json())
        .then(data => {
            showToast(data.message, data.success ? 'success' : 'error');
            loadBackupsSection();
        });
    }
}

function deleteBackup(name) {
    if (confirm(`¿Eliminar ${name}?`)) {
        fetch('/api/backup/delete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name })
        })
        .then(r => r.json())
        .then(data => {
            showToast(data.message, data.success ? 'success' : 'error');
            loadBackupsSection();
        });
    }
}

// ==================== WHITELIST ====================

function loadWhitelist() {
    fetch('/api/whitelist')
        .then(r => r.json())
        .then(data => {
            document.getElementById('whitelist-content').value = JSON.stringify(data.whitelist, null, 2);
        });
}

function saveWhitelist() {
    const content = document.getElementById('whitelist-content').value;
    try {
        const whitelist = JSON.parse(content);
        fetch('/api/whitelist', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ whitelist })
        })
        .then(r => r.json())
        .then(data => {
            showToast(data.message, data.success ? 'success' : 'error');
        });
    } catch (e) {
        showToast('JSON inválido', 'error');
    }
}

// ==================== OPS ====================

function loadOpsSection() {
    fetch('/api/ops')
        .then(r => r.json())
        .then(data => {
            const list = document.getElementById('ops-list');
            if (data.ops.length === 0) {
                list.innerHTML = '<p class="text-muted text-center">No hay operadores</p>';
            } else {
                list.innerHTML = '';
                data.ops.forEach(op => {
                    const card = document.createElement('div');
                    card.className = 'player-card';
                    card.innerHTML = `
                        <div class="player-info">
                            <div class="player-avatar"><i class="bi bi-shield"></i></div>
                            <div>
                                <strong>${op.name}</strong>
                                <br><small class="text-muted">Nivel: ${op.level || 4}</small>
                            </div>
                        </div>
                        <button class="btn btn-sm btn-danger" onclick="removeOp('${op.name}')">
                            <i class="bi bi-x-circle"></i> Remover
                        </button>
                    `;
                    list.appendChild(card);
                });
            }
        });
}

function addOp() {
    const player = document.getElementById('op-player-name').value.trim();
    if (player) {
        fetch('/api/ops/add', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ player })
        })
        .then(r => r.json())
        .then(data => {
            showToast(data.message, data.success ? 'success' : 'error');
            document.getElementById('op-player-name').value = '';
            loadOpsSection();
        });
    }
}

function removeOp(player) {
    if (confirm(`¿Remover a ${player} de operadores?`)) {
        fetch('/api/ops/remove', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ player })
        })
        .then(r => r.json())
        .then(data => {
            showToast(data.message, data.success ? 'success' : 'error');
            loadOpsSection();
        });
    }
}

// ==================== STATS ====================

function loadStatsSection() {
    fetch('/api/stats/history')
        .then(r => r.json())
        .then(data => {
            initStatsCharts(data.history);
        });
}

function initStatsCharts(history) {
    const labels = history.map(h => new Date(h.timestamp * 1000).toLocaleTimeString());
    const cpuData = history.map(h => h.cpu);
    const memoryData = history.map(h => h.memory);
    const playersData = history.map(h => h.players);
    
    // Chart combinado
    const ctx = document.getElementById('statsChart');
    if (statsChart) statsChart.destroy();
    
    statsChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [
                {
                    label: 'CPU %',
                    data: cpuData,
                    borderColor: '#667eea',
                    backgroundColor: 'rgba(102, 126, 234, 0.1)',
                    tension: 0.4
                },
                {
                    label: 'Memoria %',
                    data: memoryData,
                    borderColor: '#48bb78',
                    backgroundColor: 'rgba(72, 187, 120, 0.1)',
                    tension: 0.4
                },
                {
                    label: 'Jugadores',
                    data: playersData,
                    borderColor: '#ed8936',
                    backgroundColor: 'rgba(237, 137, 54, 0.1)',
                    tension: 0.4
                }
            ]
        },
        options: {
            responsive: true,
            plugins: {
                legend: {
                    position: 'top',
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    max: 100
                }
            }
        }
    });
}

// ==================== INITIALIZATION ====================

document.addEventListener('DOMContentLoaded', function() {
    // Cargar tema guardado
    const savedTheme = localStorage.getItem('theme') || 'light';
    document.documentElement.setAttribute('data-bs-theme', savedTheme);
    document.getElementById('theme-icon').className = savedTheme === 'dark' ? 'bi bi-sun-fill' : 'bi bi-moon-stars';
    
    // Cargar dashboard inicial
    loadDashboard();
    
    // Verificar seguridad de contraseña después de cargar dashboard
    setTimeout(() => checkPasswordSecurity(), 1000);
    
    // Cargar configuración del panel y configurar intervalos dinámicos
    loadPanelConfig();
});

// ==================== CONFIGURACIÓN DE PANEL Y POLLING ====================

// Variables de control de polling
let pollingIntervals = {
    serverStatus: null,
    logs: null,
    tps: null,
    stats: null
};

let pollingConfig = {
    refresh_interval: 5000,
    logs_interval: 10000,
    tps_interval: 10000,
    pause_when_hidden: true,
    enable_cache: true,
    cache_ttl: 3000
};

let isPageVisible = true;

// Cargar configuración del panel
async function loadPanelConfig() {
    try {
        const response = await fetch('/api/panel-config');
        const data = await response.json();
        
        if (data.success) {
            pollingConfig = data.config;
            updatePollingConfigUI();
        }
    } catch (error) {
        console.error('Error loading panel config:', error);
    }
    
    // Iniciar polling con configuración cargada
    startPolling();
}

// Iniciar todos los intervalos de polling
function startPolling() {
    // Limpiar intervalos existentes
    stopPolling();
    
    // Configurar Page Visibility API
    setupPageVisibility();
    
    // Iniciar intervalos
    pollingIntervals.serverStatus = setInterval(() => {
        if (shouldPoll()) loadServerStatus();
    }, pollingConfig.refresh_interval);
    
    pollingIntervals.logs = setInterval(() => {
        if (shouldPoll()) loadLogs();
    }, pollingConfig.logs_interval);
    
    pollingIntervals.tps = setInterval(() => {
        if (shouldPoll()) loadTPS();
    }, pollingConfig.tps_interval);
    
    // Guardar estadísticas cada minuto
    pollingIntervals.stats = setInterval(() => {
        if (shouldPoll()) {
            fetch('/api/server/status')
                .then(r => r.json())
                .then(data => {
                    if (data.running) {
                        fetch('/api/server/players').then(r => r.json()).then(players => {
                            fetch('/api/stats/save', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({
                                    cpu: data.cpu_percent,
                                    memory: data.memory_percent,
                                    players: players.online
                                })
                            });
                        });
                    }
                });
        }
    }, 60000);
}

// Detener todos los intervalos
function stopPolling() {
    Object.keys(pollingIntervals).forEach(key => {
        if (pollingIntervals[key]) {
            clearInterval(pollingIntervals[key]);
            pollingIntervals[key] = null;
        }
    });
}

// Verificar si debe hacer polling
function shouldPoll() {
    if (pollingConfig.pause_when_hidden && !isPageVisible) {
        return false;
    }
    return true;
}

// Configurar Page Visibility API
function setupPageVisibility() {
    // Detectar si la página está visible
    document.addEventListener('visibilitychange', () => {
        isPageVisible = !document.hidden;
        
        // Actualizar UI de estado
        updatePollingStatusUI();
        
        if (isPageVisible && pollingConfig.pause_when_hidden) {
            // Página visible de nuevo, actualizar inmediatamente
            loadServerStatus();
            loadLogs();
            loadTPS();
        }
    });
}

// Actualizar indicador de estado de polling
function updatePollingStatusUI() {
    const statusElement = document.getElementById('pollingStatus');
    if (!statusElement) return;
    
    if (!isPageVisible && pollingConfig.pause_when_hidden) {
        statusElement.innerHTML = '<span class="text-warning">⏸️ Pausado (pestaña oculta)</span>';
    } else {
        statusElement.innerHTML = '<span class="text-success">✓ Activo</span>';
    }
}

// Actualizar UI de configuración
function updatePollingConfigUI() {
    const refreshSelect = document.getElementById('refreshInterval');
    const logsSelect = document.getElementById('logsInterval');
    const tpsSelect = document.getElementById('tpsInterval');
    const pauseToggle = document.getElementById('pauseWhenHidden');
    
    if (refreshSelect) refreshSelect.value = pollingConfig.refresh_interval;
    if (logsSelect) logsSelect.value = pollingConfig.logs_interval;
    if (tpsSelect) tpsSelect.value = pollingConfig.tps_interval;
    if (pauseToggle) pauseToggle.checked = pollingConfig.pause_when_hidden;
    
    // Actualizar estado inicial
    updatePollingStatusUI();
}

// Actualizar configuración de refresco
async function updateRefreshInterval() {
    const interval = parseInt(document.getElementById('refreshInterval').value);
    await updatePanelConfigValue('refresh_interval', interval);
}

async function updateLogsInterval() {
    const interval = parseInt(document.getElementById('logsInterval').value);
    await updatePanelConfigValue('logs_interval', interval);
}

async function updateTpsInterval() {
    const interval = parseInt(document.getElementById('tpsInterval').value);
    await updatePanelConfigValue('tps_interval', interval);
}

async function togglePauseWhenHidden() {
    const enabled = document.getElementById('pauseWhenHidden').checked;
    await updatePanelConfigValue('pause_when_hidden', enabled);
}

// Función auxiliar para actualizar configuración
async function updatePanelConfigValue(key, value) {
    try {
        const response = await fetch('/api/panel-config', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ [key]: value })
        });
        
        const data = await response.json();
        
        if (data.success) {
            pollingConfig = data.config;
            showNotification('Configuración actualizada. Reiniciando polling...', 'success');
            
            // Reiniciar polling con nueva configuración
            startPolling();
        } else {
            showNotification('Error: ' + (data.error || 'Error desconocido'), 'danger');
        }
    } catch (error) {
        console.error('Error updating panel config:', error);
        showNotification('Error de conexión', 'danger');
    }
}

// ==================== GESTIÓN DE MUNDOS ====================

let pendingWorldSwitch = null;
let currentEditingWorld = null;
let currentBackupsWorld = null;

// Cargar lista de mundos
async function loadWorlds() {
    try {
        const response = await fetch('/api/worlds');
        const data = await response.json();
        
        if (data.success) {
            renderWorldsGrid(data.worlds, data.active_world);
        } else {
            showWorldsError('Error al cargar mundos: ' + (data.error || 'Error desconocido'));
        }
    } catch (error) {
        console.error('Error loading worlds:', error);
        showWorldsError('Error de conexión al cargar mundos');
    }
}

// Renderizar grid de mundos
function renderWorldsGrid(worlds, activeWorldSlug) {
    const grid = document.getElementById('worldsGrid');
    
    if (!worlds || worlds.length === 0) {
        grid.innerHTML = `
            <div class="col-12 text-center text-muted">
                <i class="bi bi-inbox" style="font-size: 3rem;"></i>
                <p class="mt-2">No hay mundos creados</p>
                <button class="btn btn-primary" onclick="showCreateWorldModal()">
                    <i class="bi bi-plus-circle"></i> Crear Primer Mundo
                </button>
            </div>
        `;
        return;
    }
    
    grid.innerHTML = worlds.map(world => createWorldCard(world, world.slug === activeWorldSlug)).join('');
}

// Crear tarjeta de mundo
function createWorldCard(world, isActive) {
    const gamemodeBadges = {
        'survival': 'bg-success',
        'creative': 'bg-info',
        'adventure': 'bg-warning',
        'spectator': 'bg-secondary'
    };
    
    const difficultyBadges = {
        'peaceful': 'bg-success',
        'easy': 'bg-info',
        'normal': 'bg-warning',
        'hard': 'bg-danger'
    };
    
    const lastPlayed = world.last_played ? 
        new Date(world.last_played).toLocaleString('es-ES') : 
        'Nunca jugado';
    
    return `
        <div class="col-md-6 col-lg-4">
            <div class="card h-100 ${isActive ? 'border-success border-2' : ''}">
                <div class="card-header d-flex justify-content-between align-items-center ${isActive ? 'bg-success bg-opacity-10' : ''}">
                    <h5 class="mb-0">
                        ${escapeHtml(world.name)}
                        ${isActive ? '<span class="badge bg-success ms-2">Activo</span>' : ''}
                    </h5>
                    <div class="dropdown">
                        <button class="btn btn-sm btn-outline-secondary" data-bs-toggle="dropdown" aria-expanded="false">
                            <i class="bi bi-three-dots-vertical"></i>
                        </button>
                        <ul class="dropdown-menu">
                            ${!isActive ? `
                            <li>
                                <a class="dropdown-item" href="#" onclick="event.preventDefault(); activateWorld('${world.slug}', '${escapeHtml(world.name)}')">
                                    <i class="bi bi-play-circle me-2"></i>Activar
                                </a>
                            </li>` : ''}
                            <li>
                                <a class="dropdown-item" href="#" onclick="event.preventDefault(); editWorld('${world.slug}')">
                                    <i class="bi bi-gear me-2"></i>Configurar
                                </a>
                            </li>
                            <li>
                                <a class="dropdown-item" href="#" onclick="event.preventDefault(); duplicateWorld('${world.slug}', '${escapeHtml(world.name)}')">
                                    <i class="bi bi-files me-2"></i>Duplicar
                                </a>
                            </li>
                            <li>
                                <a class="dropdown-item" href="#" onclick="event.preventDefault(); backupWorld('${world.slug}')">
                                    <i class="bi bi-save me-2"></i>Backup
                                </a>
                            </li>
                            <li><hr class="dropdown-divider"></li>
                            ${!isActive ? `
                            <li>
                                <a class="dropdown-item text-danger" href="#" onclick="event.preventDefault(); deleteWorld('${world.slug}', '${escapeHtml(world.name)}')">
                                    <i class="bi bi-trash me-2"></i>Eliminar
                                </a>
                            </li>` : ''}
                        </ul>
                    </div>
                </div>
                <div class="card-body">
                    <p class="text-muted small mb-3">${escapeHtml(world.description || 'Sin descripción')}</p>
                    
                    <div class="d-flex flex-wrap gap-2 mb-3">
                        <span class="badge ${gamemodeBadges[world.gamemode] || 'bg-secondary'}">
                            <i class="bi bi-controller me-1"></i>${world.gamemode}
                        </span>
                        <span class="badge ${difficultyBadges[world.difficulty] || 'bg-secondary'}">
                            <i class="bi bi-signal me-1"></i>${world.difficulty}
                        </span>
                        <span class="badge bg-secondary">
                            <i class="bi bi-hdd me-1"></i>${world.size_mb} MB
                        </span>
                        ${world.player_count > 0 ? `
                        <span class="badge bg-info">
                            <i class="bi bi-people me-1"></i>${world.player_count} jugadores
                        </span>` : ''}
                    </div>
                    
                    <div class="text-muted small">
                        <i class="bi bi-clock me-1"></i>
                        Última vez: ${lastPlayed}
                    </div>
                </div>
                <div class="card-footer">
                    <button class="btn btn-sm ${isActive ? 'btn-success' : 'btn-primary'} w-100" 
                            onclick="activateWorld('${world.slug}', '${escapeHtml(world.name)}')"
                            ${isActive ? 'disabled' : ''}>
                        <i class="bi ${isActive ? 'bi-check-circle' : 'bi-play-circle'} me-2"></i>
                        ${isActive ? 'Mundo Activo' : 'Activar Mundo'}
                    </button>
                </div>
            </div>
        </div>
    `;
}

// Mostrar modal de crear mundo
function showCreateWorldModal() {
    const modal = new bootstrap.Modal(document.getElementById('createWorldModal'));
    document.getElementById('createWorldForm').reset();
    document.getElementById('createWorldError').classList.add('d-none');
    modal.show();
}

// Enviar formulario de crear mundo
async function submitCreateWorld() {
    const form = document.getElementById('createWorldForm');
    const errorDiv = document.getElementById('createWorldError');
    const formData = new FormData(form);
    
    const data = {
        name: formData.get('name'),
        description: formData.get('description'),
        template: formData.get('template'),
        seed: formData.get('seed'),
        gamemode: formData.get('gamemode'),
        difficulty: formData.get('difficulty')
    };
    
    // Validación
    if (!data.name) {
        errorDiv.textContent = 'El nombre del mundo es requerido';
        errorDiv.classList.remove('d-none');
        return;
    }
    
    try {
        const response = await fetch('/api/worlds', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        
        const result = await response.json();
        
        if (result.success) {
            // Cerrar modal
            bootstrap.Modal.getInstance(document.getElementById('createWorldModal')).hide();
            
            // Mostrar notificación de éxito
            showNotification('Mundo creado correctamente', 'success');
            
            // Recargar lista de mundos
            loadWorlds();
        } else {
            errorDiv.textContent = result.error || 'Error al crear mundo';
            errorDiv.classList.remove('d-none');
        }
    } catch (error) {
        console.error('Error creating world:', error);
        errorDiv.textContent = 'Error de conexión al crear mundo';
        errorDiv.classList.remove('d-none');
    }
}

// Activar mundo
async function activateWorld(slug, name) {
    // Guardar datos para confirmar después
    pendingWorldSwitch = slug;
    
    // Actualizar nombre en el modal
    document.getElementById('targetWorldName').textContent = name;
    
    // Cargar configuración de backups para actualizar el checkbox
    try {
        const response = await fetch('/api/backup-config');
        const data = await response.json();
        
        if (data.success) {
            document.getElementById('createBackupBeforeSwitch').checked = data.config.auto_backup_enabled;
        }
    } catch (error) {
        console.error('Error loading backup config:', error);
    }
    
    // Mostrar modal de confirmación
    const modal = new bootstrap.Modal(document.getElementById('confirmSwitchModal'));
    modal.show();
}

// Confirmar cambio de mundo
async function confirmSwitchWorld() {
    const slug = pendingWorldSwitch;
    const createBackup = document.getElementById('createBackupBeforeSwitch').checked;
    
    if (!slug) return;
    
    try {
        showNotification('Cambiando de mundo...', 'info');
        
        const response = await fetch(`/api/worlds/${slug}/activate`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ create_backup: createBackup })
        });
        
        const data = await response.json();
        
        // Cerrar modal
        bootstrap.Modal.getInstance(document.getElementById('confirmSwitchModal')).hide();
        
        if (data.success) {
            showNotification('Mundo activado correctamente. El servidor se está reiniciando...', 'success');
            
            // Recargar mundos después de 3 segundos
            setTimeout(() => {
                loadWorlds();
                loadServerStatus();
            }, 3000);
        } else {
            showNotification('Error: ' + (data.error || 'Error desconocido'), 'danger');
        }
    } catch (error) {
        console.error('Error activating world:', error);
        showNotification('Error de conexión al activar mundo', 'danger');
    }
}

// Editar configuración del mundo
async function editWorld(slug) {
    currentEditingWorld = slug;
    
    try {
        const response = await fetch(`/api/worlds/${slug}/config`);
        const data = await response.json();
        
        if (data.success) {
            // Renderizar configuración
            const contentDiv = document.getElementById('worldConfigContent');
            const properties = data.properties;
            
            contentDiv.innerHTML = `
                <div class="row">
                    <div class="col-md-6 mb-3">
                        <label class="form-label">Modo de Juego</label>
                        <select class="form-control" name="gamemode">
                            <option value="survival" ${properties.gamemode === 'survival' ? 'selected' : ''}>Survival</option>
                            <option value="creative" ${properties.gamemode === 'creative' ? 'selected' : ''}>Creative</option>
                            <option value="adventure" ${properties.gamemode === 'adventure' ? 'selected' : ''}>Adventure</option>
                            <option value="spectator" ${properties.gamemode === 'spectator' ? 'selected' : ''}>Spectator</option>
                        </select>
                    </div>
                    <div class="col-md-6 mb-3">
                        <label class="form-label">Dificultad</label>
                        <select class="form-control" name="difficulty">
                            <option value="peaceful" ${properties.difficulty === 'peaceful' ? 'selected' : ''}>Peaceful</option>
                            <option value="easy" ${properties.difficulty === 'easy' ? 'selected' : ''}>Easy</option>
                            <option value="normal" ${properties.difficulty === 'normal' ? 'selected' : ''}>Normal</option>
                            <option value="hard" ${properties.difficulty === 'hard' ? 'selected' : ''}>Hard</option>
                        </select>
                    </div>
                    <div class="col-md-6 mb-3">
                        <label class="form-label">PvP</label>
                        <select class="form-control" name="pvp">
                            <option value="true" ${properties.pvp === 'true' ? 'selected' : ''}>Activado</option>
                            <option value="false" ${properties.pvp === 'false' ? 'selected' : ''}>Desactivado</option>
                        </select>
                    </div>
                    <div class="col-md-6 mb-3">
                        <label class="form-label">Máximo de Jugadores</label>
                        <input type="number" class="form-control" name="max-players" value="${properties['max-players'] || 20}">
                    </div>
                </div>
            `;
            
            // Mostrar modal
            const modal = new bootstrap.Modal(document.getElementById('editWorldConfigModal'));
            modal.show();
        } else {
            showNotification('Error al cargar configuración: ' + data.error, 'danger');
        }
    } catch (error) {
        console.error('Error loading world config:', error);
        showNotification('Error de conexión al cargar configuración', 'danger');
    }
}

// Guardar configuración del mundo
async function saveWorldConfig() {
    if (!currentEditingWorld) return;
    
    const form = document.getElementById('editWorldConfigForm');
    const formData = new FormData(form);
    const properties = {};
    
    for (let [key, value] of formData.entries()) {
        properties[key] = value;
    }
    
    try {
        const response = await fetch(`/api/worlds/${currentEditingWorld}/config`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ properties })
        });
        
        const data = await response.json();
        
        if (data.success) {
            bootstrap.Modal.getInstance(document.getElementById('editWorldConfigModal')).hide();
            showNotification('Configuración guardada correctamente', 'success');
            loadWorlds();
        } else {
            showNotification('Error: ' + data.error, 'danger');
        }
    } catch (error) {
        console.error('Error saving world config:', error);
        showNotification('Error de conexión al guardar configuración', 'danger');
    }
}

// Duplicar mundo
async function duplicateWorld(slug, originalName) {
    const newName = prompt(`Ingresa el nombre para el duplicado de "${originalName}":`, `${originalName} (Copia)`);
    
    if (!newName) return;
    
    try {
        showNotification('Duplicando mundo...', 'info');
        
        const response = await fetch(`/api/worlds/${slug}/duplicate`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ new_name: newName })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showNotification('Mundo duplicado correctamente', 'success');
            loadWorlds();
        } else {
            showNotification('Error: ' + (data.error || 'Error desconocido'), 'danger');
        }
    } catch (error) {
        console.error('Error duplicating world:', error);
        showNotification('Error de conexión al duplicar mundo', 'danger');
    }
}

// Crear backup del mundo
async function backupWorld(slug) {
    try {
        // Buscar información del mundo
        const worldsResponse = await fetch('/api/worlds');
        const worldsData = await worldsResponse.json();
        const world = worldsData.worlds.find(w => w.slug === slug);
        
        if (!world) {
            showNotification('Mundo no encontrado', 'danger');
            return;
        }
        
        // Mostrar modal de backups
        showWorldBackupsModal(slug, world.name);
        
    } catch (error) {
        console.error('Error loading backups:', error);
        showNotification('Error al cargar backups', 'danger');
    }
}

// Mostrar modal de backups de un mundo
async function showWorldBackupsModal(slug, worldName) {
    currentBackupsWorld = slug;
    
    // Actualizar título del modal
    document.getElementById('backupsWorldName').textContent = worldName;
    
    // Mostrar modal
    const modal = new bootstrap.Modal(document.getElementById('worldBackupsModal'));
    modal.show();
    
    // Cargar lista de backups
    await loadWorldBackups(slug);
}

// Cargar lista de backups de un mundo
async function loadWorldBackups(slug) {
    try {
        const response = await fetch(`/api/worlds/${slug}/backups`);
        const data = await response.json();
        
        if (data.success) {
            // Actualizar estadísticas
            document.getElementById('totalBackupsCount').textContent = data.total_count;
            document.getElementById('totalBackupsSize').textContent = data.total_size_mb + ' MB';
            
            // Renderizar lista
            renderWorldBackupsList(data.backups);
        } else {
            showBackupsError(data.error || 'Error al cargar backups');
        }
    } catch (error) {
        console.error('Error loading backups:', error);
        showBackupsError('Error de conexión al cargar backups');
    }
}

// Renderizar lista de backups
function renderWorldBackupsList(backups) {
    const container = document.getElementById('worldBackupsList');
    
    if (backups.length === 0) {
        container.innerHTML = `
            <div class="alert alert-info">
                <i class="bi bi-info-circle"></i> No hay backups disponibles para este mundo.
                <br><small>Crea tu primer backup usando el botón de arriba.</small>
            </div>
        `;
        return;
    }
    
    let html = '<div class="list-group">';
    
    backups.forEach(backup => {
        const date = new Date(backup.created_at);
        const dateStr = date.toLocaleString('es-ES', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
        
        const typeClass = backup.type === 'auto' ? 'bg-info' : 'bg-primary';
        const typeText = backup.type === 'auto' ? 'Automático' : 'Manual';
        
        html += `
            <div class="list-group-item bg-dark border-secondary">
                <div class="d-flex justify-content-between align-items-start">
                    <div class="flex-grow-1">
                        <h6 class="mb-1">
                            <i class="bi bi-archive"></i> ${escapeHtml(backup.description || 'Backup')}
                            <span class="badge ${typeClass} ms-2">${typeText}</span>
                        </h6>
                        <p class="mb-1 text-muted small">
                            <i class="bi bi-calendar"></i> ${dateStr}
                            <span class="ms-3"><i class="bi bi-hdd"></i> ${backup.size_mb} MB</span>
                        </p>
                        <small class="text-muted">${escapeHtml(backup.filename)}</small>
                    </div>
                    <div class="btn-group" role="group">
                        <button class="btn btn-sm btn-success" onclick="restoreWorldBackup('${backup.filename}', '${escapeHtml(backup.description || 'este backup')}')">
                            <i class="bi bi-arrow-counterclockwise"></i> Restaurar
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="deleteWorldBackup('${backup.filename}', '${escapeHtml(backup.description || 'este backup')}')">
                            <i class="bi bi-trash"></i>
                        </button>
                    </div>
                </div>
            </div>
        `;
    });
    
    html += '</div>';
    container.innerHTML = html;
}

// Crear backup desde el modal
async function createWorldBackupFromModal() {
    if (!currentBackupsWorld) return;
    
    const description = prompt('Descripción del backup (opcional):');
    if (description === null) return; // Usuario canceló
    
    try {
        showNotification('Creando backup...', 'info');
        
        const response = await fetch(`/api/worlds/${currentBackupsWorld}/backup`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                description: description || ''
            })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showNotification('Backup creado correctamente', 'success');
            // Recargar lista de backups
            await loadWorldBackups(currentBackupsWorld);
        } else {
            showNotification('Error: ' + (data.error || 'Error desconocido'), 'danger');
        }
    } catch (error) {
        console.error('Error creating backup:', error);
        showNotification('Error de conexión al crear backup', 'danger');
    }
}

// Restaurar backup de mundo
async function restoreWorldBackup(filename, description) {
    const confirmed = confirm(`¿Restaurar desde "${description}"?\n\nADVERTENCIA: Esto reemplazará el mundo actual. Se creará un backup de seguridad automáticamente.`);
    if (!confirmed) return;
    
    try {
        showNotification('Restaurando backup...', 'info');
        
        const response = await fetch(`/api/worlds/${currentBackupsWorld}/restore`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                backup_filename: filename
            })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showNotification('Mundo restaurado correctamente', 'success');
            
            // Cerrar modal
            const modal = bootstrap.Modal.getInstance(document.getElementById('worldBackupsModal'));
            if (modal) modal.hide();
            
            // Recargar lista de mundos
            loadWorlds();
        } else {
            showNotification('Error: ' + (data.error || 'Error desconocido'), 'danger');
        }
    } catch (error) {
        console.error('Error restoring backup:', error);
        showNotification('Error de conexión al restaurar backup', 'danger');
    }
}

// Eliminar backup de mundo
async function deleteWorldBackup(filename, description) {
    const confirmed = confirm(`¿Eliminar el backup "${description}"?\n\nEsta acción no se puede deshacer.`);
    if (!confirmed) return;
    
    try {
        showNotification('Eliminando backup...', 'info');
        
        const response = await fetch(`/api/backups/${filename}`, {
            method: 'DELETE'
        });
        
        const data = await response.json();
        
        if (data.success) {
            showNotification('Backup eliminado correctamente', 'success');
            // Recargar lista de backups
            await loadWorldBackups(currentBackupsWorld);
        } else {
            showNotification('Error: ' + (data.error || 'Error desconocido'), 'danger');
        }
    } catch (error) {
        console.error('Error deleting backup:', error);
        showNotification('Error de conexión al eliminar backup', 'danger');
    }
}

// Mostrar error en lista de backups
function showBackupsError(message) {
    const container = document.getElementById('worldBackupsList');
    container.innerHTML = `
        <div class="alert alert-danger">
            <i class="bi bi-exclamation-triangle"></i> ${escapeHtml(message)}
        </div>
    `;
}

// Eliminar mundo
async function deleteWorld(slug, name) {
    const confirmed = confirm(`¿Estás seguro de eliminar el mundo "${name}"?\n\nEsta acción no se puede deshacer.`);
    if (!confirmed) return;
    
    const createBackup = confirm('¿Deseas crear un backup antes de eliminar?');
    
    try {
        showNotification('Eliminando mundo...', 'info');
        
        const response = await fetch(`/api/worlds/${slug}?backup=${createBackup}`, {
            method: 'DELETE'
        });
        
        const data = await response.json();
        
        if (data.success) {
            showNotification('Mundo eliminado correctamente', 'success');
            loadWorlds();
        } else {
            showNotification('Error: ' + (data.error || 'Error desconocido'), 'danger');
        }
    } catch (error) {
        console.error('Error deleting world:', error);
        showNotification('Error de conexión al eliminar mundo', 'danger');
    }
}

// Mostrar error en grid de mundos
function showWorldsError(message) {
    const grid = document.getElementById('worldsGrid');
    grid.innerHTML = `
        <div class="col-12">
            <div class="alert alert-danger">
                <i class="bi bi-exclamation-triangle"></i> ${escapeHtml(message)}
            </div>
        </div>
    `;
}

// Función auxiliar para escapar HTML
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Cargar mundos cuando se abre la sección
const originalShowSection = window.showSection;
window.showSection = function(section) {
    if (originalShowSection) {
        originalShowSection(section);
    }
    
    // Si se abre la sección de mundos, cargarlos
    if (section === 'worlds') {
        loadWorlds();
    }
};

