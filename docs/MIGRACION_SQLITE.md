# Migración a SQLite - Proyecto MMORPG Plugin

## Resumen Ejecutivo

Este documento consolida toda la información sobre la migración del sistema de almacenamiento del plugin MMORPG de archivos JSON a bases de datos SQLite.

**Estado:** ✅ **COMPLETADA** (15 de enero de 2026)

---

## 1. Arquitectura Implementada

### 1.1 Modelo Dual de Bases de Datos

El sistema implementa un modelo de dos bases de datos SQLite:

#### **Base de Datos Universal** (`config/data/universal.db`)
- **Ubicación:** `/server/config/data/universal.db`
- **Propósito:** Datos compartidos entre todos los mundos
- **Gestión:** Creada automáticamente por el plugin al iniciar
- **30 Tablas principales:**
  - `players`, `player_abilities`, `player_quests`, `player_quest_progress`
  - `quests`, `quest_objectives`, `npcs`, `economy_transactions`
  - `crafting_recipes`, `crafting_history`, `crafting_stations`
  - `enchantments`, `enchantment_requirements`
  - `squad_members`, `squad_invites`, `squad_permissions`
  - `invasion_definitions`, `invasion_participants`, `invasion_rewards`
  - `dungeon_definitions`, `dungeon_history`, `dungeon_rewards`
  - `rpg_items`, `custom_mobs`, `spawn_points`, `game_events`
  - `player_classes`, `player_ranks`, `shop_items`
  - `player_pets`, `pet_abilities`

#### **Base de Datos Local por Mundo** (`worlds/{world_name}/data/{world_name}-rpg.db`)
- **Ubicación:** `/server/worlds/active/data/mmorpg-survival-rpg.db` (ejemplo)
- **Propósito:** Datos específicos de cada mundo RPG
- **Gestión:** Creada automáticamente al registrar un mundo como RPG
- **5 Tablas principales:**
  - `player_world_data` - Estado del jugador en el mundo
  - `world_player_achievements` - Logros del mundo
  - `world_inventory_items` - Inventario local
  - `world_quest_progress` - Progreso de misiones locales
  - `world_dungeons_completed` - Mazmorras completadas

### 1.2 Componentes Principales

```
mmorpg-plugin/src/main/java/com/nightslayer/mmorpg/
├── database/
│   ├── DatabaseManager.java           # Gestiona universal.db
│   └── WorldDatabaseManager.java      # Gestiona BDs locales
├── WorldRPGManager.java                # Registra mundos RPG
└── MMORPGPlugin.java                   # Inicializa todo
```

---

## 2. Managers Migrados a SQLite

### ✅ Completamente Migrados (17 managers)

1. **PlayerManager** - Datos de jugadores (players, player_abilities)
2. **QuestManager** - Sistema de misiones (quests, quest_objectives, player_quests)
3. **NPCManager** - NPCs y diálogos (npcs)
4. **CraftingManager** - Crafteo personalizado (crafting_recipes, crafting_history, crafting_stations)
5. **EnchantingManager** - Encantamientos custom (enchantments, enchantment_requirements)
6. **SquadManager** - Sistema de escuadras (squad_members, squad_invites, squad_permissions)
7. **InvasionManager** - Sistema de invasiones (invasion_definitions, invasion_participants, invasion_rewards)
8. **DungeonManager** - Mazmorras (dungeon_definitions, dungeon_history, dungeon_rewards)
9. **AchievementManager** - Logros (world_player_achievements)
10. **EconomyManager** - Economía (economy_transactions)
11. **ItemManager** - Items RPG (rpg_items)
12. **MobManager** - Mobs personalizados (custom_mobs)
13. **SpawnManager** - Puntos de spawn (spawn_points)
14. **EventManager** - Eventos del juego (game_events)
15. **ClassManager** - Clases de jugador (player_classes)
16. **RankManager** - Sistema de rangos (player_ranks)
17. **ShopManager** - Tiendas (shop_items)
18. **PetManager** - Mascotas (player_pets, pet_abilities)

### 📊 Estado de Datos JSON Restantes

Los siguientes archivos JSON se mantienen solo para **configuración**, no para datos dinámicos:
- `config/*.json` - Configuración de sistemas (raids, pets, eventos, etc.)
- `worlds/*/data/*.json` - Archivos de status y configuración local

---

## 3. Panel Web de Administración

### 3.1 Nuevas Funcionalidades

**Visor de Bases de Datos** (`/database`)
- Visualización en tiempo real de ambas BDs (universal y local)
- Filtrado por tablas
- Consultas SQL directas (solo lectura)
- Estadísticas de tablas (número de registros, tamaño)

**Endpoints de API:**
- `GET /api/database/universal/tables` - Lista tablas de universal.db
- `GET /api/database/universal/table/<name>` - Datos de tabla específica
- `GET /api/database/universal/stats` - Estadísticas de BD universal
- `GET /api/database/world/tables` - Lista tablas de BD local del mundo activo
- `GET /api/database/world/table/<name>` - Datos de tabla local
- `GET /api/database/world/stats` - Estadísticas de BD local

### 3.2 Requisitos de Acceso

- **Autenticación requerida:** Sí (credenciales admin)
- **Plugin activo requerido:** Sí (verifica MMORPGPlugin habilitado)
- **Ubicación:** `http://localhost:5000/database`

---

## 4. Decisiones Técnicas Clave

### 4.1 Estructura de Rutas

```
/server/
├── config/
│   ├── data/
│   │   └── universal.db          # BD universal (compartida)
│   └── *.json                     # Configs estáticas
├── plugins/
│   └── MMORPGPlugin/
│       └── *.json                 # Configs del plugin
└── worlds/
    ├── active -> world            # Symlink al mundo activo
    ├── world/
    │   └── data/
    │       └── world-rpg.db       # BD local mundo principal
    └── mmorpg-survival/
        └── data/
            ├── mmorpg-survival-rpg.db  # BD local mundo RPG
            └── *.json             # Configs locales
```

### 4.2 Inicialización de BDs

**Universal.db:**
```java
// En MMORPGPlugin.onEnable()
databaseManager = new DatabaseManager(this);
databaseManager.connect();
databaseManager.createTables();
```

**BD Local del Mundo:**
```java
// En WorldRPGManager.registerRPGWorld()
WorldDatabaseManager worldDb = new WorldDatabaseManager(plugin, worldName);
worldDb.connect();
worldDb.createTables();
```

### 4.3 Migración de Datos Existentes

Los datos JSON existentes se mantienen como **respaldo**. La migración automática se realiza:
1. Plugin detecta archivos JSON antiguos
2. Lee datos JSON
3. Inserta en SQLite
4. Renombra JSON a `.json.backup`

---

## 5. Testing y Validación

### 5.1 Tests Implementados

**Ubicación:** `test/`
- `test_backup_service.py` - Validación de backups
- `test_api_endpoints.py` - Tests de endpoints RPG
- `test_rpg_api.py` - Tests de API con autenticación
- `run-tests.sh` - Suite de pruebas completa

**Ejecución:**
```bash
cd /home/mkd/contenedores/mc-paper-docker
test/run-tests.sh
```

### 5.2 Verificación de BDs

**Verificar tablas en universal.db:**
```bash
docker exec minecraft-paper sqlite3 /server/config/data/universal.db ".tables"
```

**Verificar tablas en BD local:**
```bash
docker exec minecraft-paper sqlite3 /server/worlds/active/data/mmorpg-survival-rpg.db ".tables"
```

**Verificar datos desde panel web:**
```bash
# Iniciar panel
cd web && python3 app.py

# Acceder en navegador:
http://localhost:5000/database
```

---

## 6. Rendimiento y Optimización

### 6.1 Índices Implementados

```sql
-- Índices para queries frecuentes
CREATE INDEX idx_player_class ON players(player_class);
CREATE INDEX idx_player_level ON players(level);
CREATE INDEX idx_quest_difficulty ON quests(difficulty);
CREATE INDEX idx_player_quests_status ON player_quests(status);
CREATE INDEX idx_transactions_player ON economy_transactions(player_uuid);
```

### 6.2 Connection Pooling

- **Pool Size:** 10 conexiones simultáneas
- **Timeout:** 30 segundos
- **WAL Mode:** Habilitado para mejor concurrencia

---

## 7. Documentación de Referencia

### Documentos Históricos (docs/migracion-sqlite/)
- `LEER_PRIMERO.md` - Introducción a la migración
- `ARQUITECTURA_SQLITE_PROPUESTA.md` - Diseño técnico
- `PLAN_IMPLEMENTACION_FINAL.md` - Plan de ejecución
- `MIGRACION_COMPLETADA.md` - Reporte de completitud
- Otros 7 documentos de análisis

### Documentación del Proyecto (docs/)
- `ARQUITECTURA_MMORPG.md` - Arquitectura general
- `GUIA_MULTIMUNDOS.md` - Sistema multimundo
- `BACKUP_SYSTEM.md` - Sistema de backups
- `CONFIG_SYSTEM.md` - Sistema de configuración
- Ver `INDICE_DOCUMENTACION.md` para lista completa

---

## 8. Próximos Pasos

### Mantenimiento
- ✅ Sistema de backups automáticos configurado
- ✅ Logs de BD habilitados para debugging
- ✅ Panel web para monitoreo

### Optimizaciones Futuras
- [ ] Implementar cache en memoria para queries frecuentes
- [ ] Agregar más índices según patrones de uso
- [ ] Migrar archivos JSON de configuración a tablas `config`

---

## 9. Contacto y Soporte

**Desarrollador:** mkd  
**Fecha Migración:** 4-15 de enero de 2026  
**Versión Plugin:** 1.0-SNAPSHOT  
**Servidor:** Paper 1.21.4

---

## 10. Comandos Útiles

```bash
# Compilar plugin
cd mmorpg-plugin && mvn clean package

# Reiniciar servidor
docker restart minecraft-paper

# Ver logs
docker logs -f minecraft-paper

# Iniciar panel web
cd web && python3 app.py

# Ejecutar tests
test/run-tests.sh

# Backup manual
docker exec minecraft-paper sqlite3 /server/config/data/universal.db ".backup /server/backups/universal-$(date +%Y%m%d).db"
```

---

**Última actualización:** 15 de enero de 2026
