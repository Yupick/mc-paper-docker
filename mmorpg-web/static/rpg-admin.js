// Administrador de Quests para el panel web
const RPGQuestAdmin = {
    quests: [],
    npcs: [],
    
    init() {
        this.loadQuests();
        this.loadNPCs();
        this.setupEventListeners();
    },
    
    async loadQuests() {
        try {
            const response = await fetch('/api/rpg/quests');
            const data = await response.json();
            this.quests = data.quests || [];
            this.renderQuestsList();
        } catch (error) {
            console.error('Error cargando quests:', error);
        }
    },
    
    async loadNPCs() {
        try {
            const response = await fetch('/api/rpg/npcs');
            const data = await response.json();
            this.npcs = data.npcs || [];
        } catch (error) {
            console.error('Error cargando NPCs:', error);
        }
    },
    
    renderQuestsList() {
        const container = document.getElementById('quests-list');
        if (!container) return;
        
        container.innerHTML = this.quests.map(quest => `
            <div class="quest-card" data-quest-id="${quest.id}">
                <div class="quest-header">
                    <h3>${quest.name}</h3>
                    <span class="difficulty-badge ${quest.difficulty.toLowerCase()}">${quest.difficulty}</span>
                </div>
                <p class="quest-description">${quest.description}</p>
                <div class="quest-info">
                    <span>Nivel Req: ${quest.requiredLevel}</span>
                    <span>Objetivos: ${quest.objectives ? quest.objectives.length : 0}</span>
                    <span>Recompensas: ${quest.rewards ? quest.rewards.length : 0}</span>
                </div>
                <div class="quest-actions">
                    <button class="btn-edit" onclick="RPGQuestAdmin.editQuest('${quest.id}')">
                        <i class="fas fa-edit"></i> Editar
                    </button>
                    <button class="btn-delete" onclick="RPGQuestAdmin.deleteQuest('${quest.id}')">
                        <i class="fas fa-trash"></i> Eliminar
                    </button>
                </div>
            </div>
        `).join('');
    },
    
    setupEventListeners() {
        const createBtn = document.getElementById('btn-create-quest');
        if (createBtn) {
            createBtn.addEventListener('click', () => this.showCreateModal());
        }
    },
    
    showCreateModal() {
        const modal = this.buildQuestModal();
        document.body.insertAdjacentHTML('beforeend', modal);
        this.initializeModalEvents();
    },
    
    buildQuestModal(quest = null) {
        const isEdit = quest !== null;
        const title = isEdit ? 'Editar Quest' : 'Crear Nueva Quest';
        
        return `
            <div class="modal" id="quest-modal">
                <div class="modal-content">
                    <div class="modal-header">
                        <h2>${title}</h2>
                        <button class="close-modal" onclick="RPGQuestAdmin.closeModal()">&times;</button>
                    </div>
                    <div class="modal-body">
                        <form id="quest-form">
                            <div class="form-group">
                                <label>ID de la Quest</label>
                                <input type="text" name="id" value="${quest ? quest.id : ''}" 
                                       ${isEdit ? 'readonly' : ''} required>
                            </div>
                            
                            <div class="form-group">
                                <label>Nombre</label>
                                <input type="text" name="name" value="${quest ? quest.name : ''}" required>
                            </div>
                            
                            <div class="form-group">
                                <label>Descripción</label>
                                <textarea name="description" required>${quest ? quest.description : ''}</textarea>
                            </div>
                            
                            <div class="form-row">
                                <div class="form-group">
                                    <label>Nivel Mínimo</label>
                                    <input type="number" name="minLevel" value="${quest ? quest.requiredLevel : 1}" 
                                           min="1" required>
                                </div>
                                
                                <div class="form-group">
                                    <label>Dificultad</label>
                                    <select name="difficulty" required>
                                        <option value="EASY" ${quest && quest.difficulty === 'EASY' ? 'selected' : ''}>Fácil</option>
                                        <option value="NORMAL" ${quest && quest.difficulty === 'NORMAL' ? 'selected' : ''}>Normal</option>
                                        <option value="HARD" ${quest && quest.difficulty === 'HARD' ? 'selected' : ''}>Difícil</option>
                                        <option value="EPIC" ${quest && quest.difficulty === 'EPIC' ? 'selected' : ''}>Épica</option>
                                        <option value="LEGENDARY" ${quest && quest.difficulty === 'LEGENDARY' ? 'selected' : ''}>Legendaria</option>
                                    </select>
                                </div>
                            </div>
                            
                            <div class="form-group">
                                <label>NPC que da la quest</label>
                                <select name="npcGiverId">
                                    <option value="">Ninguno</option>
                                    ${this.npcs.map(npc => `
                                        <option value="${npc.id}" ${quest && quest.npcGiver === npc.id ? 'selected' : ''}>
                                            ${npc.name}
                                        </option>
                                    `).join('')}
                                </select>
                            </div>
                            
                            <div class="form-group">
                                <label>
                                    <input type="checkbox" name="repeatable" 
                                           ${quest && quest.repeatable ? 'checked' : ''}>
                                    Quest Repetible
                                </label>
                            </div>
                            
                            <div class="form-group" id="cooldown-group" style="display:${quest && quest.repeatable ? 'block' : 'none'}">
                                <label>Cooldown (segundos)</label>
                                <input type="number" name="cooldown" value="${quest ? (quest.cooldown || 0) / 1000 : 0}" min="0">
                            </div>
                            
                            <h3>Objetivos</h3>
                            <div id="objectives-container">
                                <!-- Objetivos dinámicos -->
                            </div>
                            <button type="button" class="btn-add" onclick="RPGQuestAdmin.addObjective()">
                                <i class="fas fa-plus"></i> Añadir Objetivo
                            </button>
                            
                            <h3>Recompensas</h3>
                            <div id="rewards-container">
                                <!-- Recompensas dinámicas -->
                            </div>
                            <button type="button" class="btn-add" onclick="RPGQuestAdmin.addReward()">
                                <i class="fas fa-plus"></i> Añadir Recompensa
                            </button>
                            
                            <div class="form-actions">
                                <button type="submit" class="btn-primary">
                                    <i class="fas fa-save"></i> ${isEdit ? 'Actualizar' : 'Crear'}
                                </button>
                                <button type="button" class="btn-secondary" onclick="RPGQuestAdmin.closeModal()">
                                    Cancelar
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        `;
    },
    
    initializeModalEvents() {
        const form = document.getElementById('quest-form');
        form.addEventListener('submit', (e) => this.handleSubmit(e));
        
        const repeatableCheckbox = form.querySelector('[name="repeatable"]');
        repeatableCheckbox.addEventListener('change', (e) => {
            document.getElementById('cooldown-group').style.display = e.target.checked ? 'block' : 'none';
        });
    },
    
    addObjective() {
        const container = document.getElementById('objectives-container');
        const index = container.children.length;
        
        container.insertAdjacentHTML('beforeend', `
            <div class="objective-item">
                <select name="objective_type_${index}" required>
                    <option value="KILL">Matar</option>
                    <option value="COLLECT">Recolectar</option>
                    <option value="TALK">Hablar</option>
                    <option value="REACH">Llegar</option>
                    <option value="USE">Usar</option>
                    <option value="DELIVER">Entregar</option>
                </select>
                <input type="text" name="objective_target_${index}" placeholder="Objetivo (ej: ZOMBIE)" required>
                <input type="number" name="objective_amount_${index}" placeholder="Cantidad" min="1" value="1" required>
                <button type="button" class="btn-remove" onclick="this.parentElement.remove()">
                    <i class="fas fa-times"></i>
                </button>
            </div>
        `);
    },
    
    addReward() {
        const container = document.getElementById('rewards-container');
        const index = container.children.length;
        
        container.insertAdjacentHTML('beforeend', `
            <div class="reward-item">
                <select name="reward_type_${index}" required>
                    <option value="EXPERIENCE">Experiencia</option>
                    <option value="MONEY">Dinero</option>
                    <option value="ITEM">Item</option>
                    <option value="CLASS_SKILL_POINT">Punto de Habilidad</option>
                </select>
                <input type="number" name="reward_value_${index}" placeholder="Valor/Cantidad" min="0" required>
                <button type="button" class="btn-remove" onclick="this.parentElement.remove()">
                    <i class="fas fa-times"></i>
                </button>
            </div>
        `);
    },
    
    async handleSubmit(e) {
        e.preventDefault();
        
        const formData = new FormData(e.target);
        const questData = {
            id: formData.get('id'),
            name: formData.get('name'),
            description: formData.get('description'),
            minLevel: parseInt(formData.get('minLevel')),
            difficulty: formData.get('difficulty'),
            npcGiverId: formData.get('npcGiverId') || '',
            repeatable: formData.get('repeatable') === 'on',
            cooldown: parseInt(formData.get('cooldown') || 0) * 1000,
            objectives: [],
            rewards: []
        };
        
        // Recopilar objetivos
        let index = 0;
        while (formData.has(`objective_type_${index}`)) {
            questData.objectives.push({
                type: formData.get(`objective_type_${index}`),
                target: formData.get(`objective_target_${index}`),
                amount: parseInt(formData.get(`objective_amount_${index}`))
            });
            index++;
        }
        
        // Recopilar recompensas
        index = 0;
        while (formData.has(`reward_type_${index}`)) {
            questData.rewards.push({
                type: formData.get(`reward_type_${index}`),
                value: parseFloat(formData.get(`reward_value_${index}`))
            });
            index++;
        }
        
        try {
            const response = await fetch('/api/rpg/quest/create', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(questData)
            });
            
            if (response.ok) {
                alert('Quest creada exitosamente');
                this.closeModal();
                this.loadQuests();
            } else {
                alert('Error creando quest');
            }
        } catch (error) {
            console.error('Error:', error);
            alert('Error de conexión');
        }
    },
    
    async deleteQuest(questId) {
        if (!confirm(`¿Eliminar la quest ${questId}?`)) return;
        
        try {
            const response = await fetch(`/api/rpg/quest/${questId}`, {method: 'DELETE'});
            if (response.ok) {
                alert('Quest eliminada');
                this.loadQuests();
            }
        } catch (error) {
            console.error('Error:', error);
        }
    },
    
    editQuest(questId) {
        const quest = this.quests.find(q => q.id === questId);
        if (quest) {
            const modal = this.buildQuestModal(quest);
            document.body.insertAdjacentHTML('beforeend', modal);
            this.initializeModalEvents();
        }
    },
    
    closeModal() {
        const modal = document.getElementById('quest-modal');
        if (modal) modal.remove();
    }
};

// Inicializar cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('rpg-admin-quests')) {
        RPGQuestAdmin.init();
    }
});
