# 🗺️ Roadmap: Sistema Multi-Mundos

## 📋 Visión General

Implementar un sistema completo de gestión de múltiples mundos desde el panel web, permitiendo crear, eliminar, alternar y configurar diferentes mundos de Minecraft sin necesidad de acceder al servidor directamente.

---

## 🎯 Objetivos

1. **Gestión de Mundos**: Crear, eliminar, renombrar y duplicar mundos
2. **Alternancia Dinámica**: Cambiar el mundo activo sin reconstruir el contenedor
3. **Configuración Individual**: Cada mundo con su propia configuración (server.properties)
4. **Backups Inteligentes**: Sistema de respaldo automático antes de cambios críticos
5. **Interfaz Intuitiva**: Panel web con vista de tarjetas y gestión visual

---

## 🏗️ Arquitectura Propuesta

### Estructura de Directorios

```
mc-paper/
├── worlds/                          # Directorio raíz de mundos
│   ├── active -> survival-1/        # Symlink al mundo activo
│   ├── survival-1/                  # Mundo 1
│   │   ├── world/                   # Dimensión Overworld
│   │   ├── world_nether/            # Dimensión Nether
│   │   ├── world_the_end/           # Dimensión End
│   │   ├── server.properties        # Configuración específica
│   │   └── metadata.json            # Metadata del mundo
│   ├── creative-lobby/              # Mundo 2
│   │   ├── world/
│   │   ├── server.properties
│   │   └── metadata.json
│   └── templates/                   # Plantillas de mundos
│       ├── survival/
│       ├── creative/
│       └── skyblock/
├── config/
│   └── worlds.json                  # Base de datos de mundos
└── backups/
    └── worlds/                      # Backups de mundos
```

### Archivo de Metadata (metadata.json)

```json
{
  "name": "Survival Principal",
  "slug": "survival-1",
  "description": "Mundo principal de supervivencia",
  "gamemode": "survival",
  "difficulty": "hard",
  "created_at": "2025-11-30T23:00:00Z",
  "last_played": "2025-11-30T23:45:00Z",
  "size_mb": 256,
  "seed": "-123456789",
  "version": "1.21.1",
  "spawn": {
    "x": 0,
    "y": 64,
    "z": 0
  },
  "settings": {
    "pvp": true,
    "spawn_monsters": true,
    "spawn_animals": true,
    "view_distance": 10,
    "max_players": 20
  },
  "tags": ["survival", "hard", "principal"]
}
```

### Base de Datos de Mundos (worlds.json)

```json
{
  "active_world": "survival-1",
  "worlds": [
    {
      "slug": "survival-1",
      "status": "active",
      "auto_backup": true,
      "backup_interval": "6h"
    },
    {
      "slug": "creative-lobby",
      "status": "inactive",
      "auto_backup": false
    }
  ],
  "settings": {
    "max_worlds": 10,
    "auto_backup_before_switch": true,
    "keep_backups": 5
  }
}
```

---

## 🔧 Fases de Implementación

### **Fase 1: Infraestructura Base** (Semana 1-2)

#### 1.1 Reestructuración de Volumes Docker

**Cambios en docker-compose.yml:**

```yaml
volumes:
  - ./worlds:/server/worlds                    # Directorio de mundos
  - ./worlds/active/world:/server/world        # Symlink al mundo activo
  - ./worlds/active/world_nether:/server/world_nether
  - ./worlds/active/world_the_end:/server/world_the_end
  - ./worlds/active/server.properties:/server/server.properties
```

**Script de migración:** `migrate-to-multiworld.sh`
- Mover mundo actual a `worlds/world-default/`
- Crear symlink `worlds/active -> world-default`
- Generar metadata.json inicial
- Actualizar docker-compose.yml

#### 1.2 Sistema de Metadata

**Crear:** `web/models/world.py`

```python
class World:
    def __init__(self, slug):
        self.slug = slug
        self.path = f"/server/worlds/{slug}"
        self.metadata = self._load_metadata()
    
    def _load_metadata(self):
        """Cargar metadata.json del mundo"""
        pass
    
    def get_size(self):
        """Calcular tamaño del mundo en MB"""
        pass
    
    def get_player_count(self):
        """Contar jugadores que han jugado"""
        pass
    
    def update_last_played(self):
        """Actualizar timestamp de última vez jugado"""
        pass
```

**Crear:** `web/models/world_manager.py`

```python
class WorldManager:
    def __init__(self):
        self.worlds_path = "/server/worlds"
        self.config = self._load_config()
    
    def list_worlds(self):
        """Listar todos los mundos disponibles"""
        pass
    
    def get_active_world(self):
        """Obtener el mundo actualmente activo"""
        pass
    
    def create_world(self, name, template="vanilla"):
        """Crear nuevo mundo desde plantilla"""
        pass
    
    def delete_world(self, slug):
        """Eliminar mundo (con backup opcional)"""
        pass
    
    def switch_world(self, slug):
        """Cambiar al mundo especificado"""
        pass
```

---

### **Fase 2: Backend API** (Semana 2-3)

#### 2.1 Endpoints REST API

**En `web/app.py`:**

```python
# ========== GESTIÓN DE MUNDOS ==========

@app.route('/api/worlds', methods=['GET'])
@login_required
def list_worlds():
    """Listar todos los mundos"""
    # Retornar: [{slug, name, status, size_mb, last_played}, ...]
    pass

@app.route('/api/worlds/<slug>', methods=['GET'])
@login_required
def get_world(slug):
    """Obtener detalles de un mundo específico"""
    # Retornar: metadata completa + estadísticas
    pass

@app.route('/api/worlds', methods=['POST'])
@login_required
def create_world():
    """Crear nuevo mundo"""
    # Parámetros: name, template, seed (opcional), gamemode, difficulty
    # 1. Validar nombre único
    # 2. Crear directorio
    # 3. Copiar template o generar nuevo
    # 4. Crear metadata.json
    # 5. Actualizar worlds.json
    pass

@app.route('/api/worlds/<slug>', methods=['DELETE'])
@login_required
def delete_world(slug):
    """Eliminar mundo"""
    # Parámetros: create_backup (bool)
    # 1. Verificar que no sea el mundo activo
    # 2. Crear backup si se solicita
    # 3. Eliminar directorio
    # 4. Actualizar worlds.json
    pass

@app.route('/api/worlds/<slug>/activate', methods=['POST'])
@login_required
def activate_world(slug):
    """Activar mundo (cambiar symlink)"""
    # 1. Detener servidor si está corriendo
    # 2. Backup del mundo activo (opcional)
    # 3. Cambiar symlink 'active'
    # 4. Actualizar worlds.json
    # 5. Iniciar servidor
    pass

@app.route('/api/worlds/<slug>/duplicate', methods=['POST'])
@login_required
def duplicate_world(slug):
    """Duplicar mundo existente"""
    # Parámetros: new_name
    # 1. Copiar directorio completo
    # 2. Actualizar metadata.json
    # 3. Generar nuevo seed (opcional)
    pass

@app.route('/api/worlds/<slug>/config', methods=['GET', 'PUT'])
@login_required
def world_config(slug):
    """Obtener/actualizar configuración del mundo"""
    # GET: Retornar server.properties parseado
    # PUT: Actualizar server.properties
    pass

@app.route('/api/worlds/<slug>/backup', methods=['POST'])
@login_required
def backup_world(slug):
    """Crear backup manual de un mundo"""
    # 1. Comprimir mundo completo
    # 2. Guardar en backups/worlds/
    # 3. Retornar URL de descarga
    pass

@app.route('/api/worlds/<slug>/restore', methods=['POST'])
@login_required
def restore_world(slug):
    """Restaurar mundo desde backup"""
    # Parámetros: backup_file
    # 1. Detener servidor si mundo está activo
    # 2. Eliminar mundo actual
    # 3. Extraer backup
    # 4. Iniciar servidor si corresponde
    pass
```

#### 2.2 Lógica de Cambio de Mundo

**Algoritmo de `switch_world()`:**

```python
def switch_world(new_slug):
    """
    Proceso para cambiar de mundo activo
    """
    # 1. Validaciones
    if not world_exists(new_slug):
        return {"error": "Mundo no encontrado"}
    
    if new_slug == get_active_world():
        return {"error": "Este mundo ya está activo"}
    
    # 2. Detener servidor
    server_was_running = is_server_running()
    if server_was_running:
        stop_server()
        wait_for_shutdown(timeout=60)
    
    # 3. Backup automático del mundo actual (opcional)
    if config.get('auto_backup_before_switch'):
        current_world = get_active_world()
        backup_world(current_world, auto=True)
    
    # 4. Cambiar symlink
    os.unlink('/server/worlds/active')
    os.symlink(f'/server/worlds/{new_slug}', '/server/worlds/active')
    
    # 5. Actualizar configuración
    update_worlds_json({'active_world': new_slug})
    
    # 6. Reiniciar servidor si estaba corriendo
    if server_was_running:
        start_server()
    
    # 7. Actualizar metadata
    update_last_played(new_slug)
    
    return {"success": True, "active_world": new_slug}
```

---

### **Fase 3: Frontend UI** (Semana 3-4)

#### 3.1 Nueva Sección en Dashboard

**IMPORTANTE:** Mantener el diseño actual con tema oscuro y esquema de colores existente:
- **Fondo oscuro:** `#1a1d29` (actual del panel)
- **Tarjetas:** `#242837` con bordes sutiles
- **Colores de acento:** Verde `#28a745` para éxito, Azul `#0d6efd` para acciones
- **Tipografía:** Mantener fuentes actuales (Segoe UI / System)
- **Iconos:** Font Awesome 6 (ya implementado)

**En `dashboard_v2.html`:**

```html
<!-- Nueva tab en el menú (usar estilo actual de tabs) -->
<li class="nav-item">
    <a class="nav-link" href="#worlds" data-bs-toggle="tab">
        <i class="fas fa-globe"></i> Mundos
    </a>
</li>

<!-- Contenido de la tab (mantener estructura de grid actual) -->
<div class="tab-pane fade" id="worlds">
    <!-- Header con botón crear (estilo coherente con dashboard actual) -->
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h3 class="text-white">Gestión de Mundos</h3>
        <button class="btn btn-primary" onclick="showCreateWorldModal()">
            <i class="fas fa-plus"></i> Crear Mundo
        </button>
    </div>
    
    <!-- Grid de mundos (usar grid-cols actual del dashboard) -->
    <div class="row g-4" id="worldsGrid">
        <!-- Tarjetas de mundos generadas dinámicamente -->
    </div>
</div>
```

#### 3.2 Tarjetas de Mundo

**DISEÑO:** Mantener coherencia visual con tarjetas actuales del dashboard (mismos estilos de las tarjetas de estado del servidor).

```html
<!-- Plantilla de tarjeta de mundo (tema oscuro actual) -->
<div class="col-md-4 mb-4">
    <div class="card world-card ${isActive ? 'border-success' : ''}" 
         style="background-color: #242837; border-color: ${isActive ? '#28a745' : '#2d3142'};">
        <div class="card-header d-flex justify-content-between align-items-center" 
             style="background-color: ${isActive ? '#28a74520' : 'transparent'}; border-bottom: 1px solid #2d3142;">
            <h5 class="mb-0 text-white">
                ${world.name}
                ${isActive ? '<span class="badge bg-success ms-2">Activo</span>' : ''}
            </h5>
            <div class="dropdown">
                <button class="btn btn-sm btn-outline-secondary" data-bs-toggle="dropdown">
                    <i class="fas fa-ellipsis-v"></i>
                </button>
                <ul class="dropdown-menu dropdown-menu-dark">
                    <li><a class="dropdown-item" onclick="activateWorld('${slug}')">
                        <i class="fas fa-play-circle me-2"></i>Activar
                    </a></li>
                    <li><a class="dropdown-item" onclick="editWorld('${slug}')">
                        <i class="fas fa-cog me-2"></i>Configurar
                    </a></li>
                    <li><a class="dropdown-item" onclick="duplicateWorld('${slug}')">
                        <i class="fas fa-copy me-2"></i>Duplicar
                    </a></li>
                    <li><a class="dropdown-item" onclick="backupWorld('${slug}')">
                        <i class="fas fa-save me-2"></i>Backup
                    </a></li>
                    <li><hr class="dropdown-divider"></li>
                    <li><a class="dropdown-item text-danger" onclick="deleteWorld('${slug}')">
                        <i class="fas fa-trash me-2"></i>Eliminar
                    </a></li>
                </ul>
            </div>
        </div>
        <div class="card-body">
            <p class="text-muted small mb-3">${world.description}</p>
            <div class="world-stats d-flex justify-content-around mb-3" style="gap: 10px;">
                <span class="badge bg-secondary">
                    <i class="fas fa-gamepad me-1"></i> ${world.gamemode}
                </span>
                <span class="badge bg-secondary">
                    <i class="fas fa-signal me-1"></i> ${world.difficulty}
                </span>
                <span class="badge bg-secondary">
                    <i class="fas fa-hdd me-1"></i> ${world.size_mb} MB
                </span>
            </div>
            <div class="world-meta">
                <small class="text-muted">
                    <i class="fas fa-clock me-1"></i>
                    Última vez jugado: ${formatDate(world.last_played)}
                </small>
            </div>
        </div>
        <div class="card-footer" style="background-color: transparent; border-top: 1px solid #2d3142;">
            <button class="btn btn-sm ${isActive ? 'btn-success' : 'btn-primary'} w-100" 
                    onclick="activateWorld('${slug}')"
                    ${isActive ? 'disabled' : ''}>
                <i class="fas ${isActive ? 'fa-check-circle' : 'fa-play-circle'} me-2"></i>
                ${isActive ? 'Mundo Activo' : 'Activar Mundo'}
            </button>
        </div>
    </div>
</div>
```

#### 3.3 Modales

**Modal: Crear Mundo** (tema oscuro coherente con modal de cambio de contraseña)

```html
<div class="modal fade" id="createWorldModal">
    <div class="modal-dialog modal-lg">
        <div class="modal-content" style="background-color: #242837; color: #fff;">
            <div class="modal-header" style="border-bottom: 1px solid #2d3142;">
                <h5 class="modal-title">
                    <i class="fas fa-plus-circle me-2"></i>Crear Nuevo Mundo
                </h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <form id="createWorldForm">
                    <div class="mb-3">
                        <label>Nombre del Mundo</label>
                        <input type="text" class="form-control" name="name" required>
                    </div>
                    <div class="mb-3">
                        <label>Descripción</label>
                        <textarea class="form-control" name="description"></textarea>
                    </div>
                    <div class="mb-3">
                        <label>Plantilla</label>
                        <select class="form-select" name="template">
                            <option value="vanilla">Vanilla (Generación Normal)</option>
                            <option value="flat">Flat (Mundo Plano)</option>
                            <option value="amplified">Amplified</option>
                            <option value="large_biomes">Large Biomes</option>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label>Seed (Opcional)</label>
                        <input type="text" class="form-control" name="seed">
                    </div>
                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label>Modo de Juego</label>
                            <select class="form-select" name="gamemode">
                                <option value="survival">Survival</option>
                                <option value="creative">Creative</option>
                                <option value="adventure">Adventure</option>
                            </select>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label>Dificultad</label>
                            <select class="form-select" name="difficulty">
                                <option value="peaceful">Peaceful</option>
                                <option value="easy">Easy</option>
                                <option value="normal">Normal</option>
                                <option value="hard">Hard</option>
                            </select>
                        </div>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                <button class="btn btn-primary" onclick="submitCreateWorld()">Crear Mundo</button>
            </div>
        </div>
    </div>
</div>
```

**Modal: Confirmar Cambio de Mundo** (mantener estilo de alertas del panel actual)

```html
<div class="modal fade" id="confirmSwitchModal">
    <div class="modal-dialog">
        <div class="modal-content" style="background-color: #242837; color: #fff;">
            <div class="modal-header" style="background-color: #ffc10720; border-bottom: 1px solid #ffc107;">
                <h5 class="modal-title text-warning">
                    <i class="fas fa-exclamation-triangle me-2"></i>Confirmar Cambio de Mundo
                </h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <p>Estás a punto de cambiar al mundo: <strong id="targetWorldName"></strong></p>
                <p>Esto detendrá el servidor actual y todos los jugadores serán desconectados.</p>
                <div class="form-check">
                    <input type="checkbox" class="form-check-input" id="createBackupBeforeSwitch" checked>
                    <label class="form-check-label">Crear backup del mundo actual</label>
                </div>
            </div>
            <div class="modal-footer">
                <button class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                <button class="btn btn-warning" onclick="confirmSwitchWorld()">Cambiar Mundo</button>
            </div>
        </div>
    </div>
</div>
```

#### 3.4 JavaScript (dashboard.js)

```javascript
// ========== GESTIÓN DE MUNDOS ==========

async function loadWorlds() {
    try {
        const response = await fetch('/api/worlds');
        const data = await response.json();
        
        if (data.success) {
            renderWorldsGrid(data.worlds);
        }
    } catch (error) {
        showError('Error al cargar mundos');
    }
}

function renderWorldsGrid(worlds) {
    const grid = document.getElementById('worldsGrid');
    grid.innerHTML = '';
    
    worlds.forEach(world => {
        const card = createWorldCard(world);
        grid.appendChild(card);
    });
}

function createWorldCard(world) {
    const isActive = world.status === 'active';
    // ... (código del template HTML de tarjeta)
}

async function activateWorld(slug) {
    // Mostrar modal de confirmación
    const modal = new bootstrap.Modal(document.getElementById('confirmSwitchModal'));
    document.getElementById('targetWorldName').textContent = slug;
    modal.show();
    
    // Guardar slug para confirmar después
    window.pendingWorldSwitch = slug;
}

async function confirmSwitchWorld() {
    const slug = window.pendingWorldSwitch;
    const createBackup = document.getElementById('createBackupBeforeSwitch').checked;
    
    showLoading('Cambiando de mundo...');
    
    try {
        const response = await fetch(`/api/worlds/${slug}/activate`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ create_backup: createBackup })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showSuccess('Mundo cambiado correctamente');
            loadWorlds(); // Recargar lista
            loadServerStats(); // Actualizar stats
        } else {
            showError(data.error);
        }
    } catch (error) {
        showError('Error al cambiar de mundo');
    }
}

async function submitCreateWorld() {
    const form = document.getElementById('createWorldForm');
    const formData = new FormData(form);
    
    showLoading('Creando mundo...');
    
    try {
        const response = await fetch('/api/worlds', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(Object.fromEntries(formData))
        });
        
        const data = await response.json();
        
        if (data.success) {
            showSuccess('Mundo creado correctamente');
            bootstrap.Modal.getInstance(document.getElementById('createWorldModal')).hide();
            loadWorlds();
        } else {
            showError(data.error);
        }
    } catch (error) {
        showError('Error al crear mundo');
    }
}

async function deleteWorld(slug) {
    if (!confirm(`¿Estás seguro de eliminar el mundo "${slug}"?\nEsta acción no se puede deshacer.`)) {
        return;
    }
    
    const createBackup = confirm('¿Deseas crear un backup antes de eliminar?');
    
    try {
        const response = await fetch(`/api/worlds/${slug}?backup=${createBackup}`, {
            method: 'DELETE'
        });
        
        const data = await response.json();
        
        if (data.success) {
            showSuccess('Mundo eliminado');
            loadWorlds();
        } else {
            showError(data.error);
        }
    } catch (error) {
        showError('Error al eliminar mundo');
    }
}

// Cargar mundos al iniciar
document.addEventListener('DOMContentLoaded', function() {
    loadWorlds();
    // Recargar cada 30 segundos
    setInterval(loadWorlds, 30000);
});
```

---

### **Fase 4: Sistema de Backups** (Semana 4)

#### 4.1 Backup Automático

**Crear:** `web/services/backup_service.py`

```python
class BackupService:
    def __init__(self):
        self.backup_path = "/backups/worlds"
    
    def create_backup(self, world_slug, auto=False):
        """
        Crear backup comprimido de un mundo
        """
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        backup_name = f"{world_slug}_{timestamp}.tar.gz"
        
        # Comprimir mundo
        subprocess.run([
            'tar', '-czf', 
            f'{self.backup_path}/{backup_name}',
            f'/server/worlds/{world_slug}'
        ])
        
        # Guardar metadata del backup
        self._save_backup_metadata(backup_name, world_slug, auto)
        
        # Limpiar backups antiguos
        self._cleanup_old_backups(world_slug)
    
    def restore_backup(self, backup_file, world_slug):
        """
        Restaurar mundo desde backup
        """
        # Extraer backup
        subprocess.run([
            'tar', '-xzf',
            f'{self.backup_path}/{backup_file}',
            '-C', '/server/worlds/'
        ])
    
    def _cleanup_old_backups(self, world_slug):
        """
        Eliminar backups antiguos (mantener solo los últimos N)
        """
        pass
```

#### 4.2 Cronjob para Backups Automáticos

**En el contenedor Docker:**

```bash
# Backup diario del mundo activo a las 3 AM
0 3 * * * /usr/local/bin/backup-active-world.sh
```

---

### **Fase 5: Funciones Avanzadas** (Semana 5+)

#### 5.1 Plantillas de Mundo

**Crear plantillas predefinidas:**

- **Survival Vanilla**: Generación normal
- **Creative Flat**: Mundo plano para construcción
- **Skyblock**: Isla flotante
- **Minigames**: Arena PvP preconstruida
- **RPG**: Mundo con estructuras custom

**Sistema de importación:**
- Subir archivo `.zip` de mundo
- Importar desde URL
- Clonar desde otro servidor

#### 5.2 Configuración Avanzada

**Editor de server.properties por mundo:**

```javascript
// Vista de configuración del mundo
{
    "general": {
        "max_players": 20,
        "view_distance": 10,
        "simulation_distance": 10
    },
    "gameplay": {
        "pvp": true,
        "difficulty": "hard",
        "spawn_monsters": true,
        "spawn_animals": true,
        "spawn_npcs": true
    },
    "world_generation": {
        "generate_structures": true,
        "level_type": "default",
        "generator_settings": ""
    },
    "advanced": {
        "enable_command_block": false,
        "spawn_protection": 16,
        "max_world_size": 29999984
    }
}
```

#### 5.3 Estadísticas del Mundo

**Mostrar en la tarjeta:**
- Total de jugadores únicos
- Chunks cargados
- Entidades totales
- Tiempo de juego total
- Muertes/kills
- Bloques minados/colocados

#### 5.4 Migración entre Mundos

**Herramientas:**
- Exportar estructura/región
- Copiar inventarios de jugadores
- Migrar datapack/plugins entre mundos

---

## 📊 Estimación de Tiempo

| Fase | Duración | Complejidad |
|------|----------|-------------|
| Fase 1: Infraestructura | 1-2 semanas | Media |
| Fase 2: Backend API | 1-2 semanas | Alta |
| Fase 3: Frontend UI | 1-2 semanas | Media |
| Fase 4: Backups | 1 semana | Baja |
| Fase 5: Avanzado | 2-3 semanas | Alta |
| **Total** | **6-10 semanas** | - |

---

## 🔒 Consideraciones de Seguridad

1. **Validación de Nombres**: Prevenir path traversal (`../`, `/etc/`)
2. **Límite de Mundos**: Configurar máximo de mundos para evitar saturación de disco
3. **Permisos**: Verificar que solo admin pueda crear/eliminar mundos
4. **Backups Obligatorios**: Forzar backup antes de eliminar mundos
5. **Cuotas de Espacio**: Limitar tamaño máximo por mundo

---

## 🚀 Mejoras Futuras (v3.0+)

1. **Multiverse Core**: Integración con plugin para múltiples mundos simultáneos
2. **World Portals**: Portales entre mundos desde el juego
3. **Scheduled Worlds**: Mundos que se activan en horarios específicos
4. **World Sync**: Sincronizar mundos entre múltiples servidores
5. **Cloud Storage**: Almacenar mundos en S3/Google Cloud
6. **Live World Preview**: Vista previa 3D del mundo antes de activar
7. **World Templates Marketplace**: Descargar mundos de comunidad

---

## ✅ Checklist de Implementación

### Fase 1
- [ ] Crear estructura de directorios `worlds/`
- [ ] Implementar sistema de symlinks
- [ ] Crear modelo `World` con metadata
- [ ] Script de migración desde estructura actual
- [ ] Actualizar docker-compose.yml

### Fase 2
- [ ] API: List worlds
- [ ] API: Create world
- [ ] API: Delete world
- [ ] API: Activate world
- [ ] API: Duplicate world
- [ ] API: World configuration
- [ ] Lógica de cambio de mundo
- [ ] Validaciones y error handling

### Fase 3
- [ ] Tab "Mundos" en dashboard
- [ ] Grid de tarjetas de mundos
- [ ] Modal crear mundo
- [ ] Modal confirmar cambio
- [ ] Modal editar configuración
- [ ] JavaScript para gestión
- [ ] Estilos CSS

### Fase 4
- [ ] Sistema de backups manuales
- [ ] Backups automáticos antes de cambios
- [ ] Restauración de backups
- [ ] Limpieza de backups antiguos
- [ ] API de gestión de backups

### Fase 5
- [ ] Plantillas de mundos
- [ ] Importación de mundos
- [ ] Editor avanzado de configuración
- [ ] Estadísticas de mundos
- [ ] Sistema de migración

---

## 🎯 Resultado Final

Al completar este roadmap, el panel web permitirá:

✅ **Crear mundos** con un click desde templates  
✅ **Cambiar entre mundos** dinámicamente sin reconstruir contenedor  
✅ **Configurar cada mundo** independientemente  
✅ **Backups automáticos** antes de cambios críticos  
✅ **Gestión visual** con tarjetas e información en tiempo real  
✅ **Importar/Exportar** mundos fácilmente  
✅ **Estadísticas detalladas** por mundo  

**Experiencia de usuario:** Panel profesional tipo Pterodactyl/AMP pero enfocado específicamente en Minecraft Paper.
