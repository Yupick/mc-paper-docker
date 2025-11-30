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

function loadBackupsSection() {
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
    
    // Actualizar cada 5 segundos
    setInterval(loadServerStatus, 5000);
    setInterval(loadLogs, 10000);
    setInterval(loadTPS, 10000);
    
    // Guardar estadísticas cada minuto
    setInterval(() => {
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
    }, 60000);
});
