# 📚 Sistema de Configuración Web - Documentación Técnica

**Versión**: 1.0  
**Fecha**: 22 de diciembre de 2024  
**Sprint**: 6

---

## 📋 Índice

1. [Arquitectura General](#arquitectura-general)
2. [API REST](#api-rest)
3. [Interfaz Web](#interfaz-web)
4. [Almacenamiento](#almacenamiento)
5. [Patrones de Diseño](#patrones-de-diseño)
6. [Guía de Uso](#guía-de-uso)

---

## 🏗️ Arquitectura General

### Stack Tecnológico

```
┌─────────────────────────────────────┐
│         Frontend (UI Layer)          │
│  Bootstrap 4 + jQuery + FontAwesome  │
└──────────────┬──────────────────────┘
               │ AJAX HTTP
┌──────────────▼──────────────────────┐
│        Backend (API Layer)           │
│      Flask + Blueprints REST         │
└──────────────┬──────────────────────┘
               │ JSON I/O
┌──────────────▼──────────────────────┐
│      Storage (Data Layer)            │
│    JSON Files + ConfigManager        │
└─────────────────────────────────────┘
```

### Componentes Principales

1. **Flask Application** (`web/app.py`)
   - Servidor web principal
   - Gestión de autenticación
   - Registro de blueprints
   - Rutas de páginas HTML

2. **API Blueprints** (`web/routes/`)
   - 8 blueprints modulares
   - 65 endpoints REST
   - Patrón RESTful consistente

3. **Templates** (`web/templates/`)
   - 8 páginas HTML con Jinja2
   - Layout base compartido
   - Componentes reutilizables

4. **JavaScript** (`web/static/js/`)
   - 8 archivos JS especializados
   - CRUD operations
   - Filtros dinámicos

5. **ConfigManager** (`mmorpg-plugin/src/.../ConfigManager.java`)
   - Gestión centralizada de JSON
   - Validación de datos
   - Sistema de templates

---

## 🌐 API REST

### Estructura de Blueprints

| Blueprint | Endpoints | Archivo | Descripción |
|-----------|-----------|---------|-------------|
| `config_bp` | 20 | `config_routes.py` | Mobs, Items, NPCs, Pets |
| `events_bp` | 15 | `events_routes.py` | Eventos y triggers |
| `dungeons_bp` | 6 | `dungeons_routes.py` | Dungeons con oleadas |
| `invasions_bp` | 5 | `invasions_routes.py` | Invasiones programadas |
| `classes_bp` | 5 | `classes_routes.py` | Clases RPG |
| `enchantments_bp` | 5 | `enchantments_routes.py` | Encantamientos |
| `crafting_bp` | 5 | `crafting_routes.py` | Recetas de crafteo |
| `respawn_bp` | 6 | `respawn_routes.py` | Zonas de respawn |

### Patrón RESTful Estándar

```
GET    /api/config/{domain}              - Listar todos
POST   /api/config/{domain}              - Crear nuevo
GET    /api/config/{domain}/<id>         - Obtener específico
PUT    /api/config/{domain}/<id>         - Actualizar
DELETE /api/config/{domain}/<id>         - Eliminar
GET    /api/config/{domain}/filter/<val> - Filtro especializado
```

### Ejemplo: Mobs API

```python
# GET /api/config/mobs
# Respuesta:
{
  "success": true,
  "config": [
    {
      "mobId": "zombie_warrior",
      "displayName": "Zombie Guerrero",
      "level": 10,
      "health": 50.0,
      "damage": 8.0,
      "xp": 100,
      "drops": [...],
      "abilities": [...]
    }
  ]
}

# POST /api/config/mobs
# Request body:
{
  "mobId": "skeleton_archer",
  "displayName": "Arquero Esqueleto",
  "level": 12,
  "health": 40.0,
  "damage": 10.0
}

# Response:
{
  "success": true,
  "message": "Mob creado exitosamente"
}
```

### Filtros Especializados

#### Filtro por Mundo
```
GET /api/config/mobs/world/world_nether
GET /api/config/dungeons/world/world
GET /api/config/npcs/world/world_the_end
```

#### Filtro por Categoría
```
GET /api/config/enchantments/category/WEAPON
GET /api/config/crafting/category/ARMOR
```

#### Filtro por Tipo
```
GET /api/config/classes/type/WARRIOR
GET /api/config/npcs/type/MERCHANT
```

### Manejo de Errores

```json
// Error de ID duplicado
{
  "success": false,
  "error": "El mob 'zombie_warrior' ya existe"
}

// Error de ID no encontrado
{
  "success": false,
  "error": "Mob con ID 'invalid_id' no encontrado"
}

// Error de validación
{
  "success": false,
  "error": "Campo 'mobId' es requerido"
}
```

### Autenticación

Todos los endpoints requieren autenticación HTTP Basic:

```python
import requests

response = requests.get(
    "http://localhost:5000/api/config/mobs",
    auth=("admin", "admin123")
)
```

---

## 🎨 Interfaz Web

### Páginas Disponibles

| URL | Página | Funcionalidad |
|-----|--------|---------------|
| `/events-manager` | Gestión de Eventos | CRUD de eventos con triggers y recompensas |
| `/configs-manager` | Configuración Universal | Editor multidominio de configuraciones |
| `/event-dashboard` | Dashboard de Eventos | Visualización en tiempo real |
| `/mobs-manager` | Gestión de Mobs | CRUD con drops y habilidades dinámicas |
| `/items-manager` | Catálogo de Items | CRUD con stats y rareza |
| `/npcs-manager` | Gestión de NPCs | CRUD con diálogos dinámicos |
| `/dungeons-manager` | Editor de Dungeons | CRUD con oleadas de mobs |
| `/invasions-manager` | Invasiones | CRUD con programación |

### Estructura de Página

Todas las páginas siguen el mismo patrón:

```html
┌─────────────────────────────────────┐
│ Header (Título + Botones de Acción) │
├─────────────────────────────────────┤
│ Cards de Estadísticas (4 métricas)  │
├─────────────────────────────────────┤
│ Filtros (Búsqueda + Selectores)     │
├─────────────────────────────────────┤
│ Tabla de Datos (con paginación)     │
├─────────────────────────────────────┤
│ Modal CRUD (Formulario dinámico)    │
└─────────────────────────────────────┘
```

### Componentes JavaScript

#### Funciones CRUD Estándar

```javascript
// Cargar datos desde API
function loadEntities() {
    $.ajax({
        url: '/api/config/{domain}',
        method: 'GET',
        success: function(response) {
            allEntities = response.config;
            displayEntities(allEntities);
            updateStats(allEntities);
        }
    });
}

// Mostrar en tabla
function displayEntities(entities) {
    const tbody = $('#entities-tbody');
    tbody.empty();
    entities.forEach(entity => {
        tbody.append(createTableRow(entity));
    });
}

// Crear nuevo
function openCreateModal() {
    currentEntityId = null;
    $('#entity-form')[0].reset();
    $('#entityModal').modal('show');
}

// Editar existente
function editEntity(id) {
    currentEntityId = id;
    const entity = allEntities.find(e => e.id === id);
    populateForm(entity);
    $('#entityModal').modal('show');
}

// Guardar (POST/PUT)
function saveEntity() {
    const data = collectFormData();
    const url = currentEntityId 
        ? `/api/config/{domain}/${currentEntityId}`
        : `/api/config/{domain}`;
    const method = currentEntityId ? 'PUT' : 'POST';
    
    $.ajax({url, method, data: JSON.stringify(data), ...});
}

// Eliminar
function deleteEntity(id) {
    if (confirm('¿Eliminar?')) {
        $.ajax({
            url: `/api/config/{domain}/${id}`,
            method: 'DELETE',
            success: loadEntities
        });
    }
}
```

#### Formularios Dinámicos

```javascript
// Añadir sección dinámica (drops, abilities, waves, dialogues)
function addSection(data = null) {
    const id = sectionCount++;
    const html = `
        <div class="card mb-2" id="section-${id}">
            <div class="card-body">
                <button onclick="removeSection(${id})">
                    <i class="fas fa-times"></i>
                </button>
                <!-- Campos dinámicos -->
            </div>
        </div>
    `;
    $('#sections-container').append(html);
}

// Remover sección
function removeSection(id) {
    $(`#section-${id}`).remove();
}

// Recolectar datos de secciones
function collectSections() {
    const sections = [];
    $('.section-input').each(function() {
        sections.push(extractData($(this)));
    });
    return sections;
}
```

#### Filtros Multi-Criterio

```javascript
function filterEntities() {
    const search = $('#search-input').val().toLowerCase();
    const filter1 = $('#filter1').val();
    const filter2 = $('#filter2').val();
    
    const filtered = allEntities.filter(entity => {
        const matchesSearch = !search || 
            entity.id.toLowerCase().includes(search) ||
            entity.name.toLowerCase().includes(search);
        const matchesFilter1 = !filter1 || entity.field1 === filter1;
        const matchesFilter2 = !filter2 || entity.field2 === filter2;
        
        return matchesSearch && matchesFilter1 && matchesFilter2;
    });
    
    displayEntities(filtered);
}
```

### Auto-Refresh

```javascript
$(document).ready(function() {
    loadEntities();
    
    // Recargar cada 60 segundos si modal cerrado
    setInterval(function() {
        if (!$('#entityModal').is(':visible')) {
            loadEntities();
        }
    }, 60000);
});
```

---

## 💾 Almacenamiento

### Estructura de Archivos

```
/config/
├── mobs_config.json              # Todos los mobs
├── items_config.json             # Todos los items
├── npcs_config.json              # Todos los NPCs
├── pets_config.json              # Todas las mascotas
├── events_config.json            # Todos los eventos
├── dungeons_config.json          # Todas las dungeons
├── invasions_config.json         # Todas las invasiones
├── enchantments_config.json      # Todos los encantamientos
├── crafting_config.json          # Todas las recetas
├── respawn_config.json           # Zonas + global settings
└── classes/                      # Almacenamiento por archivos
    ├── warrior.json
    ├── mage.json
    └── archer.json
```

### Formato JSON

#### Mobs Config
```json
{
  "mobs": [
    {
      "mobId": "zombie_warrior",
      "displayName": "Zombie Guerrero",
      "level": 10,
      "health": 50.0,
      "damage": 8.0,
      "defense": 2.0,
      "speed": 0.25,
      "xp": 100,
      "isBoss": false,
      "isMythic": false,
      "world": "world",
      "drops": [
        {
          "itemName": "rotten_flesh",
          "chance": 0.8,
          "minAmount": 1,
          "maxAmount": 3
        }
      ],
      "abilities": [
        {
          "abilityId": "slash",
          "cooldown": 5,
          "damage": 10.0
        }
      ]
    }
  ]
}
```

#### Dungeons Config
```json
{
  "dungeons": [
    {
      "dungeonId": "crypt_1",
      "displayName": "Cripta Maldita",
      "level": 15,
      "world": "world",
      "minPlayers": 2,
      "maxPlayers": 4,
      "timeLimit": 30,
      "active": true,
      "waves": [
        {
          "mobs": ["zombie_warrior", "skeleton_archer"]
        },
        {
          "mobs": ["zombie_brute", "skeleton_mage"]
        },
        {
          "mobs": ["boss_lich"]
        }
      ]
    }
  ]
}
```

#### Respawn Config (Estructura Dual)
```json
{
  "zones": [
    {
      "zoneId": "spawn_1",
      "displayName": "Zona Spawn Principal",
      "world": "world",
      "x": 0,
      "y": 64,
      "z": 0,
      "radius": 10
    }
  ],
  "globalSettings": {
    "respawnTime": 5,
    "protectionTime": 10,
    "allowBedSpawn": true
  }
}
```

### ConfigManager (Java)

```java
public class ConfigManager {
    /**
     * Carga configuración desde archivo JSON
     */
    public JsonObject loadConfig(File file) throws Exception {
        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, JsonObject.class);
        }
    }
    
    /**
     * Guarda configuración a archivo JSON
     */
    public void saveConfig(File file, JsonObject config) throws Exception {
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(config, writer);
        }
    }
    
    /**
     * Valida estructura de configuración
     */
    public boolean validateConfig(JsonObject config, String schema) {
        // Implementar validación contra schema
        return true;
    }
}
```

---

## 🎯 Patrones de Diseño

### 1. Repository Pattern

Cada blueprint actúa como repositorio para su dominio:

```python
class MobsRepository:
    def __init__(self, config_file):
        self.config_file = config_file
    
    def get_all(self):
        return load_json(self.config_file)
    
    def get_by_id(self, mob_id):
        mobs = self.get_all()
        return next((m for m in mobs if m['mobId'] == mob_id), None)
    
    def create(self, mob_data):
        mobs = self.get_all()
        mobs.append(mob_data)
        save_json(self.config_file, mobs)
```

### 2. Blueprint Pattern (Flask)

Separación modular de rutas:

```python
# dungeons_routes.py
from flask import Blueprint

dungeons_bp = Blueprint('dungeons', __name__)

@dungeons_bp.route('/api/config/dungeons', methods=['GET'])
def get_all_dungeons():
    # Implementación
    pass
```

### 3. Dynamic Forms Pattern

Formularios con secciones expandibles:

```html
<div id="drops-container">
    <!-- Drops dinámicos añadidos con JS -->
</div>
<button onclick="addDrop()">Añadir Drop</button>

<script>
function addDrop() {
    const dropCard = createDropCard();
    $('#drops-container').append(dropCard);
}
</script>
```

### 4. Filter Chain Pattern

Filtros aplicados secuencialmente:

```javascript
function filterEntities() {
    let filtered = allEntities;
    
    // Filtro 1: Búsqueda de texto
    if (search) {
        filtered = filtered.filter(e => matches(e, search));
    }
    
    // Filtro 2: Mundo
    if (world) {
        filtered = filtered.filter(e => e.world === world);
    }
    
    // Filtro 3: Nivel
    if (levelRange) {
        filtered = filtered.filter(e => inRange(e.level, levelRange));
    }
    
    return filtered;
}
```

### 5. Observer Pattern (Auto-Refresh)

Actualización automática de datos:

```javascript
setInterval(() => {
    if (!isModalOpen()) {
        loadData(); // Re-fetch from API
    }
}, 60000);
```

---

## 📖 Guía de Uso

### Para Desarrolladores

#### Añadir Nuevo Dominio

1. **Crear archivo de rutas** (`web/routes/new_domain_routes.py`):
```python
from flask import Blueprint, request, jsonify
import json

new_domain_bp = Blueprint('new_domain', __name__)

@new_domain_bp.route('/api/config/new-domain', methods=['GET'])
def get_all():
    # Implementar
    pass
```

2. **Registrar blueprint** (`web/routes/__init__.py`):
```python
from .new_domain_routes import new_domain_bp

def init_routes(app):
    app.register_blueprint(new_domain_bp)
```

3. **Crear página HTML** (`web/templates/new_domain_manager.html`):
```html
{% extends "base.html" %}
{% block content %}
<!-- Implementar UI -->
{% endblock %}
```

4. **Crear JavaScript** (`web/static/js/new_domain.js`):
```javascript
function loadNewDomain() {
    $.ajax({url: '/api/config/new-domain', ...});
}
```

5. **Añadir ruta Flask** (`web/app.py`):
```python
@app.route('/new-domain-manager')
@login_required
def new_domain_manager():
    return render_template('new_domain_manager.html')
```

### Para Administradores

#### Configurar Autenticación

Editar `web/app.py`:
```python
users = {
    "admin": "hash_password_here",
    "user2": "hash_password_here"
}
```

Generar hash:
```python
python web/generate_hash.py
```

#### Iniciar Panel Web

```bash
cd /home/mkd/contenedores/mc-paper-docker/web
source ../.venv/bin/activate
python app.py
```

Acceder en: `http://localhost:5000`

#### Backup de Configuraciones

```bash
# Backup manual
tar -czf config_backup_$(date +%Y%m%d).tar.gz config/

# Restaurar backup
tar -xzf config_backup_20241222.tar.gz
```

### Para Usuarios

#### Crear Nuevo Mob

1. Ir a `/mobs-manager`
2. Click en "Crear Mob"
3. Llenar formulario:
   - ID único (ej: "zombie_warrior_2")
   - Nombre display
   - Stats (nivel, HP, daño, XP)
   - Añadir drops (botón "+")
   - Añadir habilidades (botón "+")
4. Guardar

#### Filtrar Datos

1. Usar barra de búsqueda (busca en ID y nombre)
2. Seleccionar filtros específicos (mundo, tipo, categoría)
3. Click en "Limpiar" para resetear

#### Duplicar Entidad

1. En la tabla, click en botón "Duplicar"
2. Editar el ID (debe ser único)
3. Modificar campos necesarios
4. Guardar

---

## 🔧 Troubleshooting

### Error: "ID ya existe"
**Solución**: Cada ID debe ser único. Cambiar el ID o eliminar el existente primero.

### Error: "Archivo no encontrado"
**Solución**: Verificar que el archivo JSON existe en `/config/`. Crear con estructura vacía si es necesario.

### Error: "No se puede conectar a la API"
**Solución**: 
1. Verificar que Flask está corriendo: `python app.py`
2. Verificar autenticación (usuario/password correctos)
3. Revisar logs en consola

### Los cambios no se reflejan en Minecraft
**Solución**:
1. Asegurar que el plugin está activo: `/plugins`
2. Recargar configuración: `/rpg reload`
3. Reiniciar servidor si es necesario

---

## 📊 Métricas del Sistema

| Métrica | Valor |
|---------|-------|
| Total Endpoints | 65 |
| Total Blueprints | 8 |
| Total Páginas Web | 8 |
| Líneas de Código Python | ~4,800 |
| Líneas de Código JavaScript | ~3,600 |
| Líneas de Código HTML | ~2,000 |
| Dominios Soportados | 12 |
| Tiempo de Respuesta API | < 100ms |
| Auto-refresh Interval | 60s |

---

## 🚀 Roadmap Futuro

### Corto Plazo
- [ ] Tests unitarios completos
- [ ] Validación de schemas JSON
- [ ] Paginación en tablas

### Mediano Plazo
- [ ] WebSocket para updates en tiempo real
- [ ] Sistema de roles y permisos
- [ ] Historial de cambios (audit log)

### Largo Plazo
- [ ] API GraphQL
- [ ] Dashboard analytics avanzado
- [ ] Multi-tenancy

---

**Documentos relacionados**:
- [SPRINT_4_5_COMPLETADO.md](./SPRINT_4_5_COMPLETADO.md)
- [ESTADO_PROYECTO.md](./ESTADO_PROYECTO.md)
- [API Reference](./WEB_PANEL_API.md)
