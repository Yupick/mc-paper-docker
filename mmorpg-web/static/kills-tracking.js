/**
 * Módulo de Tracking de Kills - Panel RPG Web
 * Gestiona UI y visualización de estadísticas de kills
 */

class KillsTracker {
    constructor() {
        this.kills = [];
        this.playerStats = {};
        this.mobStats = {};
        this.timeline = [];
        this.currentFilter = {
            player: '',
            mob: ''
        };
    }

    /**
     * Carga estadísticas de kills desde el backend
     */
    async loadKillsStatistics() {
        try {
            const params = new URLSearchParams();
            if (this.currentFilter.player) params.append('player', this.currentFilter.player);
            if (this.currentFilter.mob) params.append('mob', this.currentFilter.mob);

            const response = await fetch(`/api/rpg/stats/kills?${params}`);
            const data = await response.json();

            if (data.success) {
                this.kills = data.kills || [];
                this.playerStats = data.summary?.playerStats || {};
                this.renderKillsTable();
                this.renderPlayerStats();
            }
        } catch (error) {
            console.error('Error cargando estadísticas:', error);
        }
    }

    /**
     * Carga estadísticas por mob
     */
    async loadMobStatistics() {
        try {
            const response = await fetch('/api/rpg/stats/mobs');
            const data = await response.json();

            if (data.success) {
                this.mobStats = data.mobStats || {};
                this.renderMobStats();
            }
        } catch (error) {
            console.error('Error cargando estadísticas de mobs:', error);
        }
    }

    /**
     * Carga timeline para gráficos
     */
    async loadTimeline() {
        try {
            const params = new URLSearchParams();
            if (this.currentFilter.player) params.append('player', this.currentFilter.player);

            const response = await fetch(`/api/rpg/stats/timeline?${params}`);
            const data = await response.json();

            if (data.success) {
                this.timeline = data.timeline || [];
                this.renderTimeline();
            }
        } catch (error) {
            console.error('Error cargando timeline:', error);
        }
    }

    /**
     * Renderiza tabla de kills filtrada
     */
    renderKillsTable() {
        const container = document.getElementById('kills-table-container');
        if (!container) return;

        let html = `
            <div class="kills-controls">
                <input type="text" id="player-filter" placeholder="Filtrar por jugador" class="filter-input">
                <input type="text" id="mob-filter" placeholder="Filtrar por mob" class="filter-input">
                <button onclick="killsTracker.applyFilters()" class="btn btn-primary">Filtrar</button>
            </div>

            <table class="kills-table">
                <thead>
                    <tr>
                        <th>Jugador</th>
                        <th>Mob</th>
                        <th>XP</th>
                        <th>Mundo</th>
                        <th>Fecha</th>
                    </tr>
                </thead>
                <tbody>
        `;

        for (const kill of this.kills) {
            html += `
                <tr>
                    <td>${kill.playerName || 'Unknown'}</td>
                    <td>${kill.mobName || kill.mobId || 'Unknown'}</td>
                    <td><span class="xp-badge">${kill.xpReward || 0}</span></td>
                    <td>${kill.world || 'Unknown'}</td>
                    <td>${new Date(kill.timestamp).toLocaleString('es-ES')}</td>
                </tr>
            `;
        }

        html += `
                </tbody>
            </table>
        `;

        container.innerHTML = html;
    }

    /**
     * Renderiza estadísticas por jugador
     */
    renderPlayerStats() {
        const container = document.getElementById('player-stats-container');
        if (!container) return;

        let html = '<div class="player-stats-grid">';

        for (const [playerName, stats] of Object.entries(this.playerStats)) {
            const killsByMob = stats.killsByMob || {};
            const mobsList = Object.entries(killsByMob)
                .map(([mobId, count]) => `<li>${mobId}: ${count}</li>`)
                .join('');

            html += `
                <div class="player-stat-card">
                    <h3>${playerName}</h3>
                    <div class="stat-row">
                        <span>Kills totales:</span>
                        <strong>${stats.totalKills || 0}</strong>
                    </div>
                    <div class="stat-row">
                        <span>XP ganado:</span>
                        <strong class="xp-value">${stats.totalXpGained || 0}</strong>
                    </div>
                    <div class="stat-row">
                        <span>Último kill:</span>
                        <span>${stats.lastKillTime ? new Date(stats.lastKillTime).toLocaleString('es-ES') : 'Nunca'}</span>
                    </div>
                    <div class="kills-by-mob">
                        <h4>Kills por mob:</h4>
                        <ul>
                            ${mobsList || '<li>Sin kills</li>'}
                        </ul>
                    </div>
                </div>
            `;
        }

        html += '</div>';
        container.innerHTML = html;
    }

    /**
     * Renderiza estadísticas por mob
     */
    renderMobStats() {
        const container = document.getElementById('mob-stats-container');
        if (!container) return;

        let html = `
            <table class="mob-stats-table">
                <thead>
                    <tr>
                        <th>Mob</th>
                        <th>Total Kills</th>
                        <th>XP Total</th>
                        <th>Jugadores</th>
                        <th>XP Promedio</th>
                    </tr>
                </thead>
                <tbody>
        `;

        // Ordenar por kills descendente
        const sortedMobs = Object.entries(this.mobStats).sort((a, b) => 
            b[1].totalKills - a[1].totalKills
        );

        for (const [mobId, stats] of sortedMobs) {
            const playersStr = stats.playersKilled?.join(', ') || 'Ninguno';

            html += `
                <tr>
                    <td><strong>${stats.name}</strong></td>
                    <td class="text-center">${stats.totalKills}</td>
                    <td class="xp-value">${stats.totalXpDropped}</td>
                    <td>${playersStr}</td>
                    <td class="text-center">${stats.averageXpPerKill.toFixed(1)}</td>
                </tr>
            `;
        }

        html += `
                </tbody>
            </table>
        `;

        container.innerHTML = html;
    }

    /**
     * Renderiza timeline para gráficos
     */
    renderTimeline() {
        const container = document.getElementById('timeline-container');
        if (!container) return;

        let html = '<div class="timeline-chart">';

        if (this.timeline.length === 0) {
            html += '<p>Sin datos de timeline</p>';
        } else {
            // Crear tabla simple de timeline
            html += `
                <table class="timeline-table">
                    <thead>
                        <tr>
                            <th>Fecha</th>
                            <th>Kills</th>
                            <th>XP Ganado</th>
                        </tr>
                    </thead>
                    <tbody>
            `;

            for (const data of this.timeline) {
                const barWidth = (data.kills / Math.max(...this.timeline.map(t => t.kills)) * 100);
                html += `
                    <tr>
                        <td>${data.date}</td>
                        <td>
                            <div class="progress-bar">
                                <div class="progress-fill" style="width: ${barWidth}%"></div>
                                <span class="progress-label">${data.kills}</span>
                            </div>
                        </td>
                        <td><span class="xp-value">${data.xp}</span></td>
                    </tr>
                `;
            }

            html += `
                    </tbody>
                </table>
            `;
        }

        html += '</div>';
        container.innerHTML = html;
    }

    /**
     * Aplica filtros a la tabla de kills
     */
    applyFilters() {
        const playerInput = document.getElementById('player-filter');
        const mobInput = document.getElementById('mob-filter');

        this.currentFilter.player = playerInput?.value || '';
        this.currentFilter.mob = mobInput?.value || '';

        this.loadKillsStatistics();
    }

    /**
     * Inicializa el tracker
     */
    async init() {
        await this.loadKillsStatistics();
        await this.loadMobStatistics();
        await this.loadTimeline();

        // Auto-refresh cada 10 segundos
        setInterval(() => this.loadKillsStatistics(), 10000);
        setInterval(() => this.loadMobStatistics(), 30000);
        setInterval(() => this.loadTimeline(), 15000);
    }
}

// Instancia global
const killsTracker = new KillsTracker();

// Inicializar cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', () => {
    killsTracker.init();
});
