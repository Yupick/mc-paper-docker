let achConfig = {};
let achDefinitions = {};

document.addEventListener('DOMContentLoaded', () => {
    initTabs();
    loadConfig();
    loadAchievements();
    loadStats();
});

function initTabs() {
    const tabs = document.querySelectorAll('.tab-btn');
    const contents = document.querySelectorAll('.tab-content');

    tabs.forEach(btn => {
        btn.addEventListener('click', () => {
            tabs.forEach(b => b.classList.remove('active'));
            contents.forEach(c => c.classList.remove('active'));
            btn.classList.add('active');
            document.getElementById(btn.dataset.tab).classList.add('active');
        });
    });
}

// CONFIG
function loadConfig() {
    fetch('/api/rpg/achievements/config')
        .then(r => r.json())
        .then(data => {
            if (!data.success) {
                notify(data.message, 'error');
                return;
            }
            achConfig = data.config;
            document.getElementById('achEnabled').checked = data.config.enabled !== false;
        })
        .catch(() => notify('Error de conexión', 'error'));
}

function saveConfig() {
    const payload = {
        enabled: document.getElementById('achEnabled').checked,
        achievements: achConfig.achievements || {}
    };
    fetch('/api/rpg/achievements/config', {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(payload)
    })
        .then(r => r.json())
        .then(data => {
            if (data.success) {
                notify('Configuración guardada', 'success');
                achConfig = data.config;
            } else {
                notify(data.message, 'error');
            }
        })
        .catch(() => notify('Error de conexión', 'error'));
}

// LIST
function loadAchievements() {
    fetch('/api/rpg/achievements/config')
        .then(r => r.json())
        .then(data => {
            if (!data.success) {
                notify(data.message, 'error');
                return;
            }
            achDefinitions = data.config.achievements || {};
            renderAchievements();
        })
        .catch(() => notify('Error al cargar logros', 'error'));
}

function renderAchievements() {
    const grid = document.getElementById('achievementsGrid');
    grid.innerHTML = '';
    const entries = Object.entries(achDefinitions);
    if (entries.length === 0) {
        grid.innerHTML = '<p class="muted">No hay logros configurados.</p>';
        return;
    }

    entries.forEach(([id, ach]) => {
        const card = document.createElement('div');
        card.className = 'ach-card';
        const trigger = ach.trigger || {};
        const reward = ach.reward || {};
        card.innerHTML = `
            <div class="ach-card-header">
                <div>
                    <span class="ach-id">${id}</span>
                    <h4>${ach.name}</h4>
                    <p class="muted">${ach.description || ''}</p>
                </div>
                <div class="actions">
                    <button class="btn-icon" onclick="editAchievement('${id}')"><i class="fas fa-edit"></i></button>
                    <button class="btn-icon danger" onclick="deleteAchievement('${id}')"><i class="fas fa-trash"></i></button>
                </div>
            </div>
            <div class="ach-meta">
                <span><i class="fas fa-bullseye"></i> ${trigger.type} ${trigger.mobId ? ' (' + trigger.mobId + ')' : ''}</span>
                <span><i class="fas fa-flag"></i> Objetivo: ${trigger.target}</span>
            </div>
            <div class="ach-reward">
                <strong>Recompensas:</strong>
                <div class="reward-tags">
                    ${reward.xp ? `<span class="tag">${reward.xp} XP</span>` : ''}
                    ${reward.coins ? `<span class="tag">${reward.coins} coins</span>` : ''}
                    ${reward.title ? `<span class="tag">Título: ${reward.title}</span>` : ''}
                    ${reward.item ? `<span class="tag">Item: ${reward.item}</span>` : ''}
                    ${reward.broadcast ? `<span class="tag warn">Broadcast</span>` : ''}
                </div>
            </div>
        `;
        grid.appendChild(card);
    });
}

function openAchievementModal(id = null) {
    const modal = document.getElementById('achievementModal');
    const title = document.getElementById('modalTitle');
    if (id) {
        const ach = achDefinitions[id];
        title.textContent = 'Editar Logro';
        document.getElementById('achId').value = id;
        document.getElementById('achId').disabled = true;
        document.getElementById('achName').value = ach.name || '';
        document.getElementById('achDescription').value = ach.description || '';
        document.getElementById('triggerType').value = ach.trigger?.type || 'KILL_ANY';
        document.getElementById('mobId').value = ach.trigger?.mobId || '';
        document.getElementById('target').value = ach.trigger?.target || 1;
        document.getElementById('rewardXP').value = ach.reward?.xp || 0;
        document.getElementById('rewardCoins').value = ach.reward?.coins || 0;
        document.getElementById('rewardTitle').value = ach.reward?.title || '';
        document.getElementById('rewardItem').value = ach.reward?.item || '';
        document.getElementById('rewardBroadcast').checked = ach.reward?.broadcast || false;
        toggleMobField();
    } else {
        title.textContent = 'Nuevo Logro';
        document.getElementById('achId').disabled = false;
        ['achId','achName','achDescription','mobId','rewardTitle','rewardItem'].forEach(id => document.getElementById(id).value='');
        document.getElementById('triggerType').value = 'KILL_ANY';
        document.getElementById('target').value = 1;
        document.getElementById('rewardXP').value = 0;
        document.getElementById('rewardCoins').value = 0;
        document.getElementById('rewardBroadcast').checked = false;
        toggleMobField();
    }
    modal.style.display = 'flex';
}

function closeAchievementModal() {
    document.getElementById('achievementModal').style.display = 'none';
}

function toggleMobField() {
    const type = document.getElementById('triggerType').value;
    document.getElementById('mobIdGroup').style.display = type === 'KILL_MOB' ? 'block' : 'none';
}

document.getElementById('triggerType').addEventListener('change', toggleMobField);

function saveAchievement() {
    const id = document.getElementById('achId').value.trim();
    const name = document.getElementById('achName').value.trim();
    const triggerType = document.getElementById('triggerType').value;
    const target = parseInt(document.getElementById('target').value) || 1;
    const mobId = document.getElementById('mobId').value.trim();

    if (!id || !name) {
        notify('Complete ID y Nombre', 'error');
        return;
    }

    const payload = {
        id,
        name,
        description: document.getElementById('achDescription').value.trim(),
        trigger: {
            type: triggerType,
            target: target,
            ...(triggerType === 'KILL_MOB' ? {mobId: mobId} : {})
        },
        reward: {
            xp: parseInt(document.getElementById('rewardXP').value) || 0,
            coins: parseInt(document.getElementById('rewardCoins').value) || 0,
            title: document.getElementById('rewardTitle').value.trim(),
            item: document.getElementById('rewardItem').value.trim(),
            broadcast: document.getElementById('rewardBroadcast').checked
        }
    };

    fetch('/api/rpg/achievements/achievement', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(payload)
    })
        .then(r => r.json())
        .then(data => {
            if (data.success) {
                notify('Logro guardado', 'success');
                closeAchievementModal();
                loadAchievements();
            } else {
                notify(data.message, 'error');
            }
        })
        .catch(() => notify('Error al guardar logro', 'error'));
}

function editAchievement(id) {
    openAchievementModal(id);
}

function deleteAchievement(id) {
    if (!confirm(`¿Eliminar el logro "${id}"?`)) return;
    fetch(`/api/rpg/achievements/achievement/${id}`, {method: 'DELETE'})
        .then(r => r.json())
        .then(data => {
            if (data.success) {
                notify('Logro eliminado', 'success');
                loadAchievements();
            } else {
                notify(data.message, 'error');
            }
        })
        .catch(() => notify('Error al eliminar', 'error'));
}

// STATS
function loadStats() {
    fetch('/api/rpg/achievements/stats')
        .then(r => r.json())
        .then(data => {
            if (!data.success) return;
            document.getElementById('totalCompletions').textContent = data.totalCompletions;
            document.getElementById('totalPlayers').textContent = data.totalPlayers;
            renderLeaderboard(data.leaderboard);
        })
        .catch(() => {});
}

function renderLeaderboard(list) {
    const tbody = document.querySelector('#leaderboardTable tbody');
    tbody.innerHTML = '';
    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="3">Sin datos</td></tr>';
        return;
    }
    list.forEach((row, idx) => {
        const tr = document.createElement('tr');
        tr.innerHTML = `<td>${idx + 1}</td><td>${row.id}</td><td>${row.completions}</td>`;
        tbody.appendChild(tr);
    });
}

// PLAYER
function loadPlayerAchievements() {
    const player = document.getElementById('playerSearch').value.trim();
    if (!player) {
        notify('Ingrese un nombre de jugador', 'error');
        return;
    }
    const container = document.getElementById('playerContent');
    container.innerHTML = '<div class="card-body"><p class="muted">Cargando...</p></div>';

    fetch(`/api/rpg/achievements/${player}`)
        .then(r => r.json())
        .then(data => {
            if (!data.success) {
                container.innerHTML = `<div class="card-body"><p class="error">${data.message}</p></div>`;
                return;
            }
            renderPlayerAchievements(container, data);
        })
        .catch(() => container.innerHTML = '<div class="card-body"><p class="error">Error de conexión</p></div>');
}

function renderPlayerAchievements(container, data) {
    const defs = data.definitions || {};
    const progressMap = {};
    (data.progress || []).forEach(p => progressMap[p.id] = p);

    const items = Object.entries(defs).map(([id, ach]) => {
        const prog = progressMap[id] || {progress: 0, completed: false};
        const trigger = ach.trigger || {};
        const percent = Math.min(100, Math.round((prog.progress / (trigger.target || 1)) * 100));
        return `
            <div class="ach-progress ${prog.completed ? 'done' : ''}">
                <div class="header">
                    <div>
                        <span class="ach-id">${id}</span>
                        <h4>${ach.name}</h4>
                        <p class="muted">${ach.description || ''}</p>
                    </div>
                    <span class="status ${prog.completed ? 'success' : ''}">${prog.completed ? 'Completado' : 'En progreso'}</span>
                </div>
                <div class="bar"><div style="width:${percent}%"></div></div>
                <div class="footer">
                    <span>Progreso: ${prog.progress}/${trigger.target || 1}</span>
                    ${prog.completedAt ? `<span class="muted">${prog.completedAt}</span>` : ''}
                </div>
            </div>
        `;
    }).join('');

    container.innerHTML = `<div class="card-body">${items || '<p class="muted">Sin logros configurados.</p>'}</div>`;
}

function refreshAchievements() {
    loadConfig();
    loadAchievements();
    loadStats();
    notify('Actualizado', 'success');
}

// UI helper
function notify(msg, type='info') {
    const n = document.createElement('div');
    n.className = `toast ${type}`;
    n.textContent = msg;
    document.body.appendChild(n);
    setTimeout(() => n.classList.add('show'), 10);
    setTimeout(() => {
        n.classList.remove('show');
        setTimeout(() => n.remove(), 300);
    }, 2500);
}

// Cerrar modal al click afuera
window.onclick = function(e) {
    const modal = document.getElementById('achievementModal');
    if (e.target === modal) {
        closeAchievementModal();
    }
};
