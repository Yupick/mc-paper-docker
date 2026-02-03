let ranksCache = {};
let editingRankId = null;

document.addEventListener('DOMContentLoaded', () => {
    setupTabs();
    refreshRanks();
});

function setupTabs() {
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
            btn.classList.add('active');
            const tab = btn.getAttribute('data-tab');
            document.getElementById(tab).classList.add('active');
        });
    });
}

function showToast(msg, type = 'info') {
    const el = document.getElementById('toast');
    el.textContent = msg;
    el.className = `toast show ${type}`;
    setTimeout(() => el.classList.remove('show'), 2500);
}

async function refreshRanks() {
    await loadConfig();
    await loadRanks();
    await loadStats();
}

async function loadConfig() {
    try {
        const res = await fetch('/api/rpg/ranks/config');
        const data = await res.json();
        if (data.success) {
            document.getElementById('rkEnabled').checked = data.config.enabled !== false;
        }
    } catch (e) {
        console.error(e);
        showToast('No se pudo cargar config', 'error');
    }
}

async function saveConfig() {
    try {
        const payload = { enabled: document.getElementById('rkEnabled').checked };
        const res = await fetch('/api/rpg/ranks/config', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await res.json();
        if (data.success) {
            showToast('Configuración guardada', 'success');
        } else {
            showToast(data.message || 'Error al guardar', 'error');
        }
    } catch (e) {
        console.error(e);
        showToast('Error al guardar', 'error');
    }
}

async function loadRanks() {
    try {
        const res = await fetch('/api/rpg/ranks');
        const data = await res.json();
        if (!data.success) {
            showToast(data.message || 'Error al cargar rangos', 'error');
            return;
        }
        ranksCache = data.ranks || {};
        renderRanks(ranksCache, data.counts || {});
    } catch (e) {
        console.error(e);
        showToast('Error al cargar rangos', 'error');
    }
}

function renderRanks(ranks, counts) {
    const grid = document.getElementById('ranksGrid');
    if (!grid) return;

    const entries = Object.entries(ranks);
    if (entries.length === 0) {
        grid.innerHTML = '<p class="muted">No hay rangos configurados.</p>';
        return;
    }

    grid.innerHTML = entries.map(([id, rk]) => {
        const req = rk.requirements || {};
        const reward = rk.reward || {};
        const count = counts && counts[id] ? counts[id] : 0;
        return `
            <div class="rank-card">
                <div class="rank-card-header">
                    <div>
                        <div class="rank-id">${id}</div>
                        <h4>${rk.name || ''}</h4>
                    </div>
                    <div class="actions">
                        <button class="btn-icon" title="Editar" onclick="openRankModal('${id}')"><i class="fas fa-pen"></i></button>
                        <button class="btn-icon danger" title="Eliminar" onclick="deleteRank('${id}')"><i class="fas fa-trash"></i></button>
                    </div>
                </div>
                <p class="muted">${rk.description || ''}</p>
                <div class="rank-meta">
                    <span class="tag order">Orden ${rk.order || 0}</span>
                    <span class="tag req">Nivel ${req.level || 0}</span>
                    <span class="tag req">Logros ${req.achievementsCompleted || 0}</span>
                    <span class="tag">Asignaciones: ${count}</span>
                </div>
                <div class="rank-meta">
                    <span class="tag">XP ${reward.xp || 0}</span>
                    <span class="tag">Coins ${reward.coins || 0}</span>
                    ${reward.title ? `<span class="tag">Título: ${reward.title}</span>` : ''}
                    ${reward.item ? `<span class="tag">Item: ${reward.item}</span>` : ''}
                </div>
                ${reward.permissions ? `<div class="rank-meta"><span class="tag">Permisos: ${reward.permissions.join(', ')}</span></div>` : ''}
            </div>
        `;
    }).join('');
}

function openRankModal(id = null) {
    const modal = document.getElementById('rankModal');
    modal.style.display = 'flex';
    editingRankId = id;

    if (id && ranksCache[id]) {
        const rk = ranksCache[id];
        document.getElementById('modalTitle').innerText = 'Editar Rango';
        document.getElementById('rankId').value = id;
        document.getElementById('rankId').disabled = true;
        document.getElementById('rankName').value = rk.name || '';
        document.getElementById('rankDescription').value = rk.description || '';
        document.getElementById('rankOrder').value = rk.order || 0;
        document.getElementById('reqLevel').value = (rk.requirements && rk.requirements.level) || 0;
        document.getElementById('reqAchievements').value = (rk.requirements && rk.requirements.achievementsCompleted) || 0;
        document.getElementById('rewardXp').value = (rk.reward && rk.reward.xp) || 0;
        document.getElementById('rewardCoins').value = (rk.reward && rk.reward.coins) || 0;
        document.getElementById('rewardTitle').value = (rk.reward && rk.reward.title) || '';
        document.getElementById('rewardItem').value = (rk.reward && rk.reward.item) || '';
        document.getElementById('rewardBroadcast').checked = !!(rk.reward && rk.reward.broadcast);
        document.getElementById('rewardPermissions').value = rk.reward && rk.reward.permissions ? rk.reward.permissions.join(', ') : '';
    } else {
        document.getElementById('modalTitle').innerText = 'Nuevo Rango';
        document.querySelectorAll('#rankModal input, #rankModal textarea').forEach(el => { if (el.type !== 'checkbox') el.value = ''; });
        document.getElementById('rankOrder').value = 1;
        document.getElementById('reqLevel').value = 1;
        document.getElementById('reqAchievements').value = 0;
        document.getElementById('rewardXp').value = 0;
        document.getElementById('rewardCoins').value = 0;
        document.getElementById('rewardBroadcast').checked = false;
        document.getElementById('rankId').disabled = false;
    }
}

function closeRankModal() {
    const modal = document.getElementById('rankModal');
    modal.style.display = 'none';
    editingRankId = null;
}

window.onclick = function(event) {
    const modal = document.getElementById('rankModal');
    if (event.target === modal) {
        closeRankModal();
    }
};

async function saveRank() {
    const payload = {
        id: document.getElementById('rankId').value.trim(),
        name: document.getElementById('rankName').value.trim(),
        description: document.getElementById('rankDescription').value.trim(),
        order: parseInt(document.getElementById('rankOrder').value || '0', 10),
        requirements: {
            level: parseInt(document.getElementById('reqLevel').value || '0', 10),
            achievementsCompleted: parseInt(document.getElementById('reqAchievements').value || '0', 10)
        },
        reward: {
            xp: parseInt(document.getElementById('rewardXp').value || '0', 10),
            coins: parseInt(document.getElementById('rewardCoins').value || '0', 10),
            title: document.getElementById('rewardTitle').value.trim(),
            item: document.getElementById('rewardItem').value.trim(),
            broadcast: document.getElementById('rewardBroadcast').checked,
            permissions: document.getElementById('rewardPermissions').value
                .split(',')
                .map(p => p.trim())
                .filter(p => p.length > 0)
        }
    };

    if (!payload.id || !payload.name) {
        showToast('ID y Nombre son requeridos', 'error');
        return;
    }

    try {
        if (editingRankId) {
            const newRanks = { ...ranksCache, [editingRankId]: payload };
            const res = await fetch('/api/rpg/ranks/config', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ ranks: newRanks })
            });
            const data = await res.json();
            if (!data.success) {
                showToast(data.message || 'Error al actualizar', 'error');
                return;
            }
        } else {
            const res = await fetch('/api/rpg/ranks/rank', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const data = await res.json();
            if (!data.success) {
                showToast(data.message || 'Error al crear', 'error');
                return;
            }
        }

        showToast('Rango guardado', 'success');
        closeRankModal();
        await refreshRanks();
    } catch (e) {
        console.error(e);
        showToast('Error al guardar rango', 'error');
    }
}

async function deleteRank(id) {
    if (!confirm(`¿Eliminar el rango "${id}"?`)) return;
    try {
        const res = await fetch(`/api/rpg/ranks/rank/${id}`, { method: 'DELETE' });
        const data = await res.json();
        if (data.success) {
            showToast('Rango eliminado', 'success');
            refreshRanks();
        } else {
            showToast(data.message || 'Error al eliminar', 'error');
        }
    } catch (e) {
        console.error(e);
        showToast('Error al eliminar rango', 'error');
    }
}

async function loadStats() {
    try {
        const res = await fetch('/api/rpg/ranks/stats');
        const data = await res.json();
        if (!data.success) return;
        document.getElementById('totalAssignments').innerText = data.totalAssignments || 0;
        document.getElementById('totalPlayers').innerText = data.totalPlayers || 0;

        const tbody = document.querySelector('#rankLeaderboard tbody');
        tbody.innerHTML = '';
        (data.leaderboard || []).forEach((row, idx) => {
            const tr = document.createElement('tr');
            tr.innerHTML = `<td>${idx + 1}</td><td>${row.rankId}</td><td>${row.total}</td>`;
            tbody.appendChild(tr);
        });
    } catch (e) {
        console.error(e);
    }
}

async function loadPlayerRank() {
    const player = document.getElementById('playerSearch').value.trim();
    if (!player) {
        showToast('Ingresa un jugador', 'error');
        return;
    }
    try {
        const res = await fetch(`/api/rpg/ranks/player/${encodeURIComponent(player)}`);
        const data = await res.json();
        const content = document.querySelector('#playerContent .card-body');
        if (!data.success) {
            content.innerHTML = `<p class="error">${data.message || 'No encontrado'}</p>`;
            return;
        }

        const defs = data.definitions || {};
        const rank = data.rank;

        const selectOptions = Object.entries(defs).map(([id, rk]) => `<option value="${id}" ${rank && rank.rankId === id ? 'selected' : ''}>${rk.name || id}</option>`).join('');

        content.innerHTML = `
            <div class="form-group">
                <label>Jugador</label>
                <div class="tag">${data.player}</div>
            </div>
            <div class="form-group">
                <label>Rango actual</label>
                <div>${rank ? `<span class="tag order">${rank.rankId}</span> <small class="muted">${rank.title || ''}</small>` : '<span class="muted">Sin rango</span>'}</div>
            </div>
            <div class="form-group">
                <label>Asignar nuevo rango</label>
                <select id="assignRankSelect">${selectOptions}</select>
            </div>
            <button class="btn btn-primary" onclick="assignRank('${data.player}')"><i class="fas fa-check"></i> Asignar</button>
        `;
    } catch (e) {
        console.error(e);
        showToast('Error al cargar jugador', 'error');
    }
}

async function assignRank(player) {
    const select = document.getElementById('assignRankSelect');
    if (!select) return;
    const rankId = select.value;
    try {
        const res = await fetch('/api/rpg/ranks/assign', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ player, rankId })
        });
        const data = await res.json();
        if (data.success) {
            showToast('Rango asignado', 'success');
            loadPlayerRank();
        } else {
            showToast(data.message || 'Error al asignar', 'error');
        }
    } catch (e) {
        console.error(e);
        showToast('Error al asignar', 'error');
    }
}
