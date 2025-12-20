# 🗺️ Roadmap de Desarrollo - Plugin MMORPG PaperMC + Panel Web

## 1. Análisis y objetivos
- Integrar un sistema RPG modular en PaperMC, gestionando mundos independientes (flag `isRPG`).
- Sincronizar datos RPG con el panel web (Flask + JS) para administración y visualización por mundo.
- Persistencia robusta por mundo (SQLite).
- Soporte para NPCs, quests, clases, ítems y economía.

---

## 2. Estructura técnica
- **Plugin Java (PaperMC):**
  - Carpeta principal: `mmorpg-rpg-plugin/`
  - Subcarpetas: `core/`, `npc/`, `world/`, `web/`, `database/`, `events/`, `utils/`, `config/`, `docs/`
  - Archivos clave: `plugin.yml`, `config.yml`, `classes.yml`, `quests.yml`, `items.yml`, `lang/`, `ROADMAP_MMRPG.md`
- **Panel Web (Flask + JS):**
  - Integración con endpoints/archivos del plugin para mostrar y administrar RPG por mundo.

---

## 3. Roadmap de desarrollo

### Fase 1: Base y arquitectura
- [x] Crear estructura base del plugin PaperMC (Java, Maven/Gradle) ✅ COMPLETADO
- [x] Definir `plugin.yml` con permisos y dependencias (Vault) ✅ COMPLETADO
- [x] Implementar sistema de configuración YAML/JSON editable ✅ COMPLETADO
- [x] Integrar flag `isRPG` en metadata de mundos y lógica de activación ✅ COMPLETADO
- [x] Crear endpoints/archivos para comunicación con el panel web ✅ COMPLETADO

### Fase 2: Sistemas RPG principales
- [x] Sistema de clases (6 clases completas con 18 habilidades) ✅ COMPLETADO
- [x] Sistema de NPCs (4 tipos: comerciantes, entrenadores, guardias, quest givers) ✅ COMPLETADO
- [x] Sistema de quests (objetivos múltiples, recompensas, dificultades) ✅ COMPLETADO
- [x] Sistema de economía (integración Vault + monedas RPG internas) ✅ COMPLETADO
- [x] Sistema de tiendas (4 shops NPCs con items especializados) ✅ COMPLETADO
- [x] Persistencia completa (SQLite con 8 tablas relacionales) ✅ COMPLETADO
- [x] Integración con Vault para economía ✅ COMPLETADO
- [x] Sistema de Mobs custom (CustomMob + MobManager con stats y drops) ✅ COMPLETADO

### Fase 3: Integración y panel web
- [x] API REST completa (6 endpoints para quests y NPCs) ✅ COMPLETADO
- [x] Endpoints API para mobs (GET /api/rpg/mobs, POST /api/rpg/mob/create, DELETE /api/rpg/mob/<id>) ✅ COMPLETADO
- [x] Panel web con interfaz de administración RPG ✅ COMPLETADO
- [x] Administración de quests y NPCs desde el panel ✅ COMPLETADO
- [x] Administración de mobs custom desde el panel web ✅ COMPLETADO
- [x] Tab completo de Mobs con tabla CRUD ✅ COMPLETADO
- [x] Formularios con tooltips de ayuda para NPCs, Quests y Mobs ✅ COMPLETADO
- [x] Sincronización bidireccional plugin-panel web ✅ COMPLETADO

### Fase 4: Extensibilidad y extras
- [x] Sistema de eventos personalizados (6 eventos implementados) ✅ COMPLETADO
- [x] Sistema completo de auditoría (AuditLogger con 7 categorías) ✅ COMPLETADO
- [x] Soporte multilenguaje (Español + Inglés) ✅ COMPLETADO
- [x] Migración a persistencia SQLite ✅ COMPLETADO
- [x] Documentación técnica completa (API Reference + Developer Guide) ✅ COMPLETADO

---

## 📊 Estado del Proyecto: 100% ETAPA 4 COMPLETADA ✅

### Resumen de Implementación - ETAPA 4

**Subsistemas Principales (Completados):**
1. ✅ **Spawn Command** - `/rpg mob spawn <id> [mundo] [x] [y] [z]`
2. ✅ **Loot System** - 16 items RPG, 4 raridades (Common, Rare, Epic, Legendary)
3. ✅ **Kill Tracking** - Dashboard web con estadísticas en tiempo real
4. ✅ **Respawn System** - Respawn automático por zonas (Farmeo, Dungeon, Boss Arena)
5. ✅ **ItemManager** - Sistema completo de items con atributos RPG
6. ✅ **MobDeathListener** - Auto-drops al matar mobs
7. ✅ **RespawnManager** - Gestión de zonas de respawn
8. ✅ **RespawnZone** - Modelo inmutable con ZoneType enum
9. ✅ **Web API Endpoints** - 10 endpoints REST funcionales
10. ✅ **Web UI Panels** - Dashboard de kills y panel de respawn

**Archivos Creados/Modificados (Etapa 4):**
- ✅ `/mmorpg-plugin/src/main/java/com/nightslayer/mmorpg/items/ItemManager.java` - Gestor de items RPG
- ✅ `/mmorpg-plugin/src/main/java/com/nightslayer/mmorpg/items/RPGItem.java` - Modelo de item RPG
- ✅ `/mmorpg-plugin/src/main/java/com/nightslayer/mmorpg/items/Rarity.java` - Enum de raridades
- ✅ `/mmorpg-plugin/src/main/java/com/nightslayer/mmorpg/mobs/MobDeathListener.java` - Listener de drops
- ✅ `/mmorpg-plugin/src/main/java/com/nightslayer/mmorpg/respawn/RespawnZone.java` - Modelo de zona
- ✅ `/mmorpg-plugin/src/main/java/com/nightslayer/mmorpg/respawn/RespawnManager.java` - Gestor de respawn
- ✅ `/plugins/MMORPGPlugin/data/items.json` - Configuración de items
- ✅ `/plugins/MMORPGPlugin/data/respawn_config.json` - Configuración de zonas
- ✅ `/web/templates/kills_dashboard.html` - Panel de kills
- ✅ `/web/templates/respawn_panel.html` - Panel de respawn
- ✅ `/web/static/kills-tracking.js` - JavaScript para kills
- ✅ `/web/static/kills-tracking.css` - Estilos para kills
- ✅ `/web/app.py` - 10 endpoints API (3 nuevos para respawn)
- ✅ `/docs/ETAPA_4.md` - Documentación completa (600+ líneas)
- ✅ `/mmorpg-plugin/pom.xml` - Dependencias Maven

**Estadísticas:**
- **Total LOC Etapa 4:** ~2,500 líneas de código
- **Java Classes:** 8 nuevas clases
- **API Endpoints:** 10 (3 nuevos para respawn)
- **Configuration Files:** 2 (items.json, respawn_config.json)
- **Web Templates:** 2 (kills_dashboard, respawn_panel)
- **JavaScript:** 1 módulo completo (kills-tracking.js)
- **CSS:** 1 stylesheet completo (kills-tracking.css)
- **Maven Build:** ✅ BUILD SUCCESS

### Resumen de Implementación Original

**Total de archivos Java**: 31 clases
**Líneas de código**: ~9,200 LOC (pre-Etapa 4)
**Dependencias**: 
- PaperMC 1.21.1
- Vault API 1.7
- Gson 2.10.1
- SQLite JDBC 3.44.1.0

**Sistemas implementados**:
1. ✅ **Sistema de Clases**: 6 clases (Warrior, Mage, Archer, Assassin, Cleric, Paladin) con 18 habilidades únicas
2. ✅ **Sistema de Quests**: Quest manager con objetivos múltiples, dificultades, recompensas
3. ✅ **Sistema de NPCs**: 4 tipos de NPCs con diálogos, comercio, quests
4. ✅ **Sistema de Economía**: Dual (Vault + RPG Coins)
5. ✅ **Sistema de Tiendas**: 4 shops especializados (General, Weapons, Armor, Potions)
6. ✅ **Base de Datos SQLite**: 8 tablas con índices optimizados
7. ✅ **Sistema de Eventos**: 6 eventos personalizados para extensibilidad
8. ✅ **Sistema de Auditoría**: Logging completo con 7 categorías y 4 niveles de severidad
9. ✅ **Multilenguaje**: Español e Inglés con +150 mensajes traducidos
10. ✅ **API REST**: 10 endpoints para administración web (NPCs, Quests, Mobs, Kills, Respawn)
11. ✅ **Panel Web**: Interfaz completa de administración Flask + JavaScript con tabs
12. ✅ **Sistema de Mobs Custom**: CustomMob + MobManager con stats (health, damage, defense, level, XP, boss flag, drops)
13. ✅ **Gestión de Mobs en Panel**: Tab completo con CRUD (crear, listar, editar, eliminar)
14. ✅ **Tooltips de Ayuda**: Viñetas informativas en todos los formularios (NPCs, Quests, Mobs)
15. ✅ **ETAPA 4 - Sistema Integrado**: Spawn, Loot, Kill Tracking, Respawn (NUEVO - 100% COMPLETADO)

**Archivos de documentación**:
- ✅ `API_REFERENCE.md` - Documentación completa de API
- ✅ `DEVELOPER_GUIDE.md` - Guía para desarrolladores
- ✅ `ROADMAP_MMORPG.md` - Este roadmap (actualizado)
- ✅ `ETAPA_4.md` - Documentación completa de Etapa 4

---

## 📊 Estado del Proyecto: 90% COMPLETADO ✅

---

## 🚀 Próximos Pasos Opcionales (Fase 5+)

### Mejoras de Contenido
- [ ] Sistema de Dungeons con generación procedural
- [ ] Sistema de Raids para 10+ jugadores
- [ ] Boss fights con mecánicas especiales
- [x] Sistema de crafting avanzado de items RPG (Módulo 3.1 – crafteo web/API ✅)
- [x] Sistema de encantamientos personalizados (Módulo 3.2 – panel y API ✅)
- [x] Mascotas y monturas (Módulo 3.3 – panel, API y config ✅)

### Optimización y Escalado
- [ ] Cache distribuido con Redis
- [ ] Sharding de base de datos
- [ ] Clustering multi-servidor
- [ ] Balanceo de carga

### Integración
- [ ] Integración profunda con Citizens para NPCs avanzados
- [ ] Compatibilidad completa con Geyser/Floodgate (Bedrock)
- [ ] Integración con PlaceholderAPI
- [ ] Hooks para WorldGuard y otros plugins de protección

---

## 📋 TODO: Tareas Completadas y Pendientes por Etapa

### ✅ COMPLETADO: ETAPA 1 al 5 (100% de Coding)

**Subsistemas Implementados:**
1. ✅ **Spawn Command** - `/rpg mob spawn <id> [mundo] [x] [y] [z]`
2. ✅ **Loot System** - 16 items RPG con 4 raridades y auto-drops
3. ✅ **Kill Tracking** - Dashboard web con estadísticas en tiempo real
4. ✅ **Respawn System** - Zonas automáticas (Farmeo/Dungeon/Boss Arena)
5. ✅ **UI Integrada** - Panel web interactivo para administración
6. ✅ **API REST** - 10 endpoints funcionales
7. ✅ **Documentación** - ETAPA_4.md completo con ejemplos

---

### **ETAPA 1: Testing y Validación** (Crítica) ✅ COMPLETADO
- [x] **Testing local del panel RPG** ✅ COMPLETADO
  - [x] Ejecutar `./web/start-panel.sh` y validar autenticación ✅
  - [x] Verificar tab RPG carga correctamente con mundo mmorpg activo ✅
  - [x] Testar tab NPCs: listar, crear, editar, eliminar ✅
  - [x] Testar tab Quests: listar, crear, editar, eliminar ✅
  - [x] Testar tab Mobs: listar, crear con drops, editar, eliminar ✅
  - [x] Validar tooltips de ayuda aparecen en todos los formularios ✅
  - [x] Verificar sincronización bidireccional plugin ↔ panel ✅

- [x] **Testing de integración backend** ✅ COMPLETADO
  - [x] Verificar endpoints API retornan datos correctos ✅
  - [x] Validar creación de mobs.json en plugins/MMORPGPlugin/data/ ✅
  - [x] Testar spawning de mobs custom en servidor ✅
  - [x] Validar persistencia de mobs en JSON ✅

### **ETAPA 2: Sistema de Mobs Avanzado** ✅ COMPLETADO
- [x] **Persistencia de mobs en SQLite** (opcional, si se requiere) ✅
  - [x] Crear tabla `custom_mobs` en BD ✅
  - [x] Migrar datos de mobs.json a SQLite ✅
  - [x] Actualizar MobManager para usar BD en lugar de JSON ✅

- [x] **Spawning avanzado de mobs** ✅ COMPLETADO
  - [x] Comando `/rpg mob spawn <mob_id> <world> <x> <y> <z>` para spawnerlos manualmente ✅
  - [x] Sistema de puntos de spawn predefinidos ✅
  - [x] Respawn automático de mobs en intervalos ✅

- [x] **Comportamiento inteligente de mobs** ✅ COMPLETADO
  - [x] Sistema de pathfinding hacia jugadores ✅
  - [x] Agrupa mobs en el servidor (limiares de rendimiento) ✅
  - [x] Drop de items en death con probabilidades ✅
  - [x] Aplicar modifiers de daño/defensa basados en level ✅

- [x] **Evento custom de muerte de mob** ✅ COMPLETADO
  - [x] Trigger al matar mob: `CustomMobDeathEvent` ✅
  - [x] Registrar kill en BD/auditoría ✅
  - [x] Notificar recompensas al jugador (XP, dinero) ✅

- [x] **Sistema de respawn automático de mobs/NPCs** ✅ COMPLETADO

#### Respawn por zona ✅
  - [x] Definir zonas de farmeo y dungeons (por coordenadas o regiones) ✅
  - [x] Configurar tipo de mobs, cantidad máxima y tiempo de respawn por zona ✅
  - [x] Implementar lógica para que, al morir un mob en zona especial, se respawnee automáticamente tras X segundos ✅
  - [x] Panel web: administración visual de zonas y mobs activos ✅

#### Respawn individual por mob/NPC ✅
  - [x] Agregar opción `autoRespawn` y `respawnDelay` en la definición de mobs/NPCs ✅
  - [x] Permitir que ciertos mobs/bosses/NPCs reaparezcan automáticamente tras morir, independiente de la zona ✅
  - [x] Configuración avanzada por mob (ideal para bosses, NPCs clave, mobs raros) ✅

#### Combinación de ambos sistemas ✅
  - [x] Permitir coexistencia de respawn por zona y por mob individual ✅
  - [x] Documentar best practices y ejemplos de uso ✅

---

### **ETAPA 3: Contenido y Expansión de Mobs** ✅ COMPLETADO
- [x] **Crear librería estándar de mobs** ✅ COMPLETADO
  - [x] 10 mobs básicos (Zombie Guerrero, Skeleton Arquero, Spider Cazadora, etc.) ✅ COMPLETADO
  - [x] 5 mobs de élite (Ravager, Vindicator, Evoker, Guardian, Shulker) ✅ COMPLETADO
  - [x] 3 bosses de mundo (Dragón Corrupto, Rey Necrómante, Titán Ártico) ✅ COMPLETADO

- [x] **Sistema de oleadas (Waves)** ✅ COMPLETADO
  - [x] Spawnear múltiples mobs en progresión ✅
  - [x] Dificultad escalada ✅
  - [x] Recompensas acumulativas ✅

- [x] **Mobs temáticos por bioma** ✅ COMPLETADO
  - [x] Mobs desérticos ✅
  - [x] Mobs de nieve ✅
  - [x] Mobs de jungla ✅
  - [x] Mobs de nether/end ✅

- [x] **Documentación de mobs** ✅ COMPLETADO
  - [x] Crear `MOBS_GUIDE.md` con librería estándar (21 mobs documentados) ✅
  - [x] Ejemplos de configuración de custom mobs ✅
  - [x] Best practices para balanceo de dificultad ✅

### **ETAPA 4: Integración con Sistema RPG Existente** ✅ 100% COMPLETADO
- [x] **Quests relacionadas con mobs** ✅ COMPLETADO
  - [x] Tipos de objetivos: "Matar X de mob Y" ✅
  - [x] Sistema de seguimiento de kills ✅
  - [x] Recompensas por cumplir objetivos de mobs ✅

- [x] **Drops relacionados con quests** ✅ COMPLETADO
  - [x] Items necesarios para completar quests pueden ser drops de mobs ✅
  - [x] Sistema de "tasa de drop" por tipo de quest ✅

- [x] **Loot personalizado** ✅ COMPLETADO
  - [x] Items con atributos RPG (+daño, +defensa, bonificadores) ✅
  - [x] Rarezas de items (Común, Raro, Épico, Legendario) ✅

### **ETAPA 5: Optimizaciones y Polish** ✅ COMPLETADO
- [x] **Performance de mobs** ✅ COMPLETADO
  - [x] Optimizar pathfinding ✅
  - [x] Culling de mobs fuera de vista ✅
  - [x] Limitadores de spawns concurrentes ✅

- [x] **Mejorar UI del panel** ✅ COMPLETADO
  - [x] Previsualizaciones de mobs (icono/textura) ✅
  - [x] Gráficos estadísticos de kills vs tiempo ✅
  - [x] Filtros avanzados en tabla de mobs ✅

- [x] **Documentación de mobs** ✅ COMPLETADO
  - [x] Crear `MOBS_GUIDE.md` con librería estándar ✅
  - [x] Ejemplos de configuración de custom mobs ✅
  - [x] Best practices para balanceo de dificultad ✅

---

## 🚀 ROADMAP FUTURO - Características Pendientes por Módulos

### **MÓDULO 1: Sistema de Progresión Avanzada** 🎯 ✅ COMPLETADO

#### **Etapa 1.1: Bestiario y Enciclopedia** ✅ COMPLETADO
- [x] **Modelo de datos del bestiario** ✅
  - [x] Crear clase `Bestiary.java` con registro de mobs descubiertos ✅
  - [x] Tabla SQLite `player_bestiary` (player_uuid, mob_id, kills, first_kill_date, last_kill_date) ✅
  - [x] Sistema de progresión por mob con tiers (0%, 25%, 50%, 75%, 100%, 500, 1000) ✅
  - [x] Estadísticas detalladas por mob (total kills, first/last kill dates, tier actual) ✅

- [x] **Recompensas por descubrimientos** ✅
  - [x] XP bonus al descubrir nuevo mob (firstKill reward) ✅
  - [x] Sistema de categorías (Undead, Beasts) con completion rewards ✅
  - [x] Items exclusivos al completar categorías ✅
  - [x] Sistema de tiers con recompensas progresivas ✅

- [x] **UI del bestiario en panel web** ✅
  - [x] Página `/bestiary` con grid de categorías y mobs ✅
  - [x] Detalles de mob: stats, kills, tier, descubierto ✅
  - [x] Progress bars por categoría (Undead, Beasts, etc.) ✅
  - [x] Filtros y búsqueda por jugador ✅
  - [x] Panel de administración para config y categorías ✅

- [x] **Backend completo** ✅
  - [x] BestiaryManager con carga/guardado automático ✅
  - [x] BestiaryEntry, BestiaryCategory, BestiaryReward models ✅
  - [x] MobDeathListener integrado con BestiaryManager ✅
  - [x] Configuración JSON (bestiary_config.json) ✅

#### **Etapa 1.2: Sistema de Achievements/Trofeos** ✅ COMPLETADO
- [x] **Categorías de achievements** ✅
  - [x] Achievements de kills (KILL_ANY, KILL_MOB triggers) ✅
  - [x] Sistema configurable por tipo de mob ✅
  - [x] Target configurable (matar X mobs) ✅

- [x] **Sistema de recompensas** ✅
  - [x] XP, coins, títulos configurables ✅
  - [x] Broadcast opcional para logros destacados ✅
  - [x] Items como recompensa ✅
  - [x] Integración con EconomyManager ✅

- [x] **UI de achievements** ✅
  - [x] Panel web `/achievements` con tabs (Config, List, Stats, Player) ✅
  - [x] Progress tracking en tiempo real ✅
  - [x] Estadísticas globales (completions, leaderboard) ✅
  - [x] Endpoint API completo (config, stats, player progress, CRUD) ✅

- [x] **Backend completo** ✅
  - [x] AchievementManager con persistencia SQLite ✅
  - [x] Achievement, AchievementProgress, AchievementReward models ✅
  - [x] Tabla player_achievements con tracking de progreso ✅
  - [x] Configuración JSON (achievements_config.json) ✅

#### **Etapa 1.3: Sistema de Rangos y Títulos** ✅ COMPLETADO
- [x] **Rangos configurables** ✅
  - [x] Sistema de rangos con orden y requisitos ✅
  - [x] Requisitos: nivel mínimo, logros completados ✅
  - [x] Recompensas: XP, coins, título, permisos, items ✅
  - [x] Broadcast opcional al ascender ✅

- [x] **Títulos personalizados** ✅
  - [x] Sistema de títulos asociados a rangos ✅
  - [x] Asignación manual vía panel web ✅
  - [x] Evaluación automática de promoción ✅
  - [x] Persistencia en tabla player_ranks ✅

- [x] **Panel web y API** ✅
  - [x] Panel `/ranks` con tabs (Config, Rangos, Stats, Jugador) ✅
  - [x] CRUD completo de rangos ✅
  - [x] Asignación manual de rangos a jugadores ✅
  - [x] Estadísticas y leaderboard de rangos ✅
  - [x] Endpoints API completos ✅

- [x] **Backend completo** ✅
  - [x] RankManager con carga/guardado automático ✅
  - [x] Rank, RankRequirement, RankReward models ✅
  - [x] Integración con AchievementManager para requisitos ✅
  - [x] Configuración JSON (ranks_config.json) ✅

---

### **MÓDULO 2: Contenido Dinámico y Eventos** 🎪

#### **Etapa 2.1: Sistema de Invasiones** ✅ COMPLETADO
- [x] **Mecánica de invasiones** ✅
  - [x] Invasiones programadas (FIXED, RANDOM schedules) ✅
  - [x] Tipos configurables en JSON (Zombie Horde, etc.) ✅
  - [x] Sistema de oleadas con delays configurables ✅
  - [x] Escalado por nivel de mob configurado ✅
  - [x] Broadcast de notificaciones al servidor ✅

- [x] **Backend completo** ✅
  - [x] InvasionManager con carga/guardado automático ✅
  - [x] InvasionConfig, InvasionSession, InvasionWaveConfig ✅
  - [x] Persistencia SQLite (invasion_history, invasion_participants) ✅
  - [x] Integración con MobManager para spawns ✅
  - [x] Detección automática de muerte de mobs de invasión ✅

- [x] **Oleadas progresivas** ✅
  - [x] 5-10 oleadas configurables por invasión ✅
  - [x] Boss final en última oleada con HP multiplicado ✅
  - [x] Spawns dinámicos cerca de jugadores ✅
  - [x] Sistema de puntos por kills (tracking por jugador) ✅
  - [x] Auto-progresión de oleadas al eliminar todos los mobs ✅

- [x] **Recompensas de invasión** ✅
  - [x] XP y monedas por oleada completada ✅
  - [x] Bonus XP y monedas por invasión completa ✅
  - [x] Items especiales configurables ✅
  - [x] Integración con EconomyManager ✅
  - [x] Broadcast de recompensas a participantes ✅

- [x] **UI y tracking** ✅
  - [x] Panel web `/invasions` completo ✅
  - [x] Tab Activas: invasiones en curso con progreso en tiempo real ✅
  - [x] Tab Configuración: JSON editable con validación ✅
  - [x] Tab Historial: últimas 50 invasiones con detalles ✅
  - [x] Tab Estadísticas: totales, tasa éxito, leaderboard top 10 ✅
  - [x] Inicio/detención manual desde panel ✅
  - [x] Integración completa en panel RPG principal ✅

#### **Etapa 2.2: Eventos Temáticos** ✅ COMPLETADO
- [x] **Eventos por temporada** ✅
  - [x] Halloween: Mobs espectrales, calabazas drop, boss Headless Horseman ✅
  - [x] Navidad: Mobs de nieve, regalos drop, boss Santa Corrupto ✅
  - [x] Sistema de eventos configurables (JSON) ✅
  - [x] Eventos personalizados (Aniversario servidor, etc.) ✅

- [x] **Mecánicas especiales de eventos** ✅
  - [x] Drops exclusivos durante evento (limitado tiempo) ✅
  - [x] Monedas de evento acumulativas por jugador ✅
  - [x] Sistema de recompensas configurables ✅
  - [x] Bonus XP multiplicado durante eventos ✅

- [x] **Configuración de eventos** ✅
  - [x] JSON editable: `events_config.json` ✅
  - [x] Programar eventos con fecha inicio/fin ✅
  - [x] Panel admin para activar/desactivar eventos manualmente ✅
  - [x] Preview de eventos próximos en panel web ✅

- [x] **Backend completo** ✅
  - [x] EventManager con auto-checker de eventos ✅
  - [x] EventConfig, EventSession con tracking de participantes ✅
  - [x] Persistencia SQLite (event_history, event_participants) ✅
  - [x] Integración con MobDeathListener ✅
  - [x] Sistema de monedas de evento por jugador ✅

- [x] **UI y tracking** ✅
  - [x] Panel web `/events` completo ✅
  - [x] Tab Activos: eventos en curso con detalles ✅
  - [x] Tab Configuración: JSON editable con validación ✅
  - [x] Tab Historial: últimas 50 eventos con detalles ✅
  - [x] Tab Estadísticas: totales, leaderboard top 10 ✅
  - [x] Inicio/detención manual desde panel ✅

#### **Etapa 2.3: Mazmorras Dinámicas**
- [ ] **Sistema de coordinación**
  - [ ] Grupos de 3-5 mobs que pelean juntos
  - [ ] IA cooperativa: un tank, dos DPS, un healer
  - [ ] Buffs compartidos entre miembros (si uno muere, otros se enfurecen)
  - [ ] Estrategias de grupo (flanking, focus fire, protect healer)

- [ ] **Tipos de cuadrillas**
  - [ ] Cuadrilla de Bandidos (Archer, Warrior, Rogue)
  - [ ] Cuadrilla de No-muertos (Zombie Tank, Skeleton Sniper, Necromancer)
  - [ ] Cuadrilla de Bestias (Alpha Wolf + 4 wolves)
  - [ ] Cuadrilla de Guardianes (Knight Captain + 3 guards)

- [ ] **Recompensas de cuadrilla**
  - [ ] Drop garantizado de set de items (completa armadura o arma set)
  - [ ] XP bonus por eliminar cuadrilla completa
  - [ ] Logro especial: "Squad Wiper"
  - [ ] Probabilidad de invocar boss si eliminas 10 cuadrillas

- [ ] **Spawning inteligente**
  - [ ] Spawn en dungeons y zonas de alto nivel
  - [ ] Patrullas móviles (se mueven por área)
  - [ ] No respawn automático (spawn manual o evento)
  - [ ] Configuración en `squad_config.json`

---

### **MÓDULO 3: Integración y Expansión** 🔗

#### **Etapa 3.1: Sistema de Crafting de Items RPG**
- [ ] **Crafteo avanzado**
  - [ ] Recetas personalizadas con drops de mobs
  - [ ] Estación de crafteo especial (Altar Encantado, Forja Mágica)
  - [ ] Items con atributos aleatorios (RNG stats dentro de rango)
  - [ ] Sistema de mejora (upgrade items con materiales)

- [ ] **Materiales de mobs**
  - [ ] Cada mob dropea material específico (Zombie flesh, Skeleton bone, Dragon scale)
  - [ ] Rareza de materiales (común, raro, épico, legendario)
  - [ ] Stack de materiales hasta 64
  - [ ] Shop NPCs que compran/venden materiales

- [ ] **Recetas configurables**
  - [ ] JSON editable: `crafting_recipes.json`
  - [ ] UI en panel web para crear/editar recetas
  - [ ] Validación de recetas (evitar recetas rotas)
  - [ ] Sistema de descubrimiento (recetas desbloquean al obtener material)

#### **Etapa 3.2: Encantamientos Personalizados**
- [ ] **Encantamientos RPG**
  - [ ] 20+ encantamientos custom (Life Steal, Critical Strike, Fire Aura)
  - [ ] Niveles de encantamiento (I, II, III, IV, V)
  - [ ] Incompatibilidades configurables
  - [ ] Aplicación mediante Enchanting Table especial o NPCs

- [ ] **Drop de libros encantados**
  - [ ] Bosses dropean libros con encantamientos raros
  - [ ] Probabilidad basada en dificultad del mob
  - [ ] Libros combinables (anvil para fusionar)
  - [ ] Librería de encantamientos en panel web

#### **Etapa 3.3: Mascotas y Monturas**
- [ ] **Sistema de mascotas**
  - [ ] Mascotas invocables (perros, gatos, lobos, dragones bebé)
  - [ ] Drops de bosses (huevos de mascota 1% drop)
  - [ ] Stats de mascota (daño, vida, velocidad)
  - [ ] Niveles de mascota (XP compartido con jugador)

- [ ] **Monturas**
  - [ ] Caballos RPG con stats custom
  - [ ] Monturas voladoras (Dragón, Grifo) desbloqueables
  - [ ] Velocidad, salto, resistencia configurables
  - [ ] Cosméticos para monturas (armaduras, sillas)

- [ ] **Gestión de mascotas**
  - [ ] Comando `/rpg pet summon/dismiss`
  - [ ] Inventario de mascotas (hasta 10 guardadas)
  - [ ] Alimentación para curar/buffear mascota
  - [ ] Panel web con colección de mascotas

#### **Etapa 3.4: Dungeons Procedurales**
- [ ] **Generación de dungeons**
  - [ ] Algoritmo de generación procedural (rooms + corridors)
  - [ ] 3 niveles de dificultad (Normal, Heroico, Mítico)
  - [ ] Temas: Cripta, Cueva, Castillo, Templo
  - [ ] Boss room al final con cofre de loot

- [ ] **Mecánicas de dungeon**
  - [ ] Entrada requiere key (drop de mobs)
  - [ ] Instanciado por grupo (solo tu party ve tu dungeon)
  - [ ] Timer de 30-60 min (falla si no completas)
  - [ ] Checkpoints (respawn dentro del dungeon si mueres)

- [ ] **Loot de dungeon**
  - [ ] Cofres con loot garantizado
  - [ ] Tabla de loot por dificultad
  - [ ] Items exclusivos de dungeon
  - [ ] Leaderboard de tiempo de completado

#### **Etapa 3.5: Integración con Discord**
- [ ] **Webhooks de eventos**
  - [ ] Notificar en Discord al matar boss
  - [ ] Anunciar logros raros desbloqueados
  - [ ] Alertas de invasiones activas
  - [ ] Leaderboards semanales automáticos

- [ ] **Bot de estadísticas**
  - [ ] Comando `!stats <jugador>` en Discord
  - [ ] Comando `!bestiary <jugador>` ver progreso
  - [ ] Comando `!leaderboard` ver top kills
  - [ ] Comando `!events` ver eventos activos/próximos

---

### **MÓDULO 4: Optimización y Producción** ⚙️

#### **Etapa 4.1: Testing Completo**
- [ ] **Testing unitario Java**
  - [ ] Tests para MobManager, ItemManager, RespawnManager
  - [ ] Tests de integración con Bukkit API
  - [ ] Coverage mínimo 60%
  - [ ] CI/CD con GitHub Actions

- [ ] **Testing del panel web**
  - [ ] Tests de endpoints API (200, 400, 500 responses)
  - [ ] Tests de UI (Selenium o Playwright)
  - [ ] Validación de formularios
  - [ ] Performance testing (load test con 100+ requests)

- [ ] **Testing en servidor real**
  - [ ] Deploy en staging environment
  - [ ] 10+ jugadores testeando simultáneamente
  - [ ] Reporte de bugs y fixes
  - [ ] Balanceo de dificultad

#### **Etapa 4.2: Optimización de Performance**
- [ ] **Optimización de spawns**
  - [ ] Pooling de entities (reusar en vez de crear)
  - [ ] Batching de spawns (spawn múltiples en 1 tick)
  - [ ] Despawn de mobs muy lejos de jugadores
  - [ ] Limitador de mobs por chunk

- [ ] **Optimización de BD**
  - [ ] Índices en tablas críticas
  - [ ] Query optimization (evitar N+1)
  - [ ] Connection pooling
  - [ ] Caché de queries frecuentes (Redis opcional)

- [ ] **Optimización de panel web**
  - [ ] Compresión gzip
  - [ ] Minificación de JS/CSS
  - [ ] Lazy loading de imágenes
  - [ ] Paginación en tablas grandes

#### **Etapa 4.3: Monitoreo y Logging**
- [ ] **Métricas en tiempo real**
  - [ ] Dashboard de performance (TPS, RAM, CPU)
  - [ ] Gráficos de mobs spawneados vs tiempo
  - [ ] Estadísticas de API calls
  - [ ] Alertas si TPS < 15 o RAM > 90%

- [ ] **Logging avanzado**
  - [ ] Rotación de logs (1 archivo por día)
  - [ ] Niveles de log configurables (DEBUG, INFO, WARN, ERROR)
  - [ ] Logs estructurados (JSON format)
  - [ ] Integración con Grafana/Prometheus (opcional)

#### **Etapa 4.4: Documentación de Producción**
- [ ] **Guías de deployment**
  - [ ] Instalación paso a paso
  - [ ] Configuración recomendada
  - [ ] Troubleshooting común
  - [ ] Rollback procedure

- [ ] **Documentación de admin**
  - [ ] Manual de comandos admin
  - [ ] Guía de configuración de eventos
  - [ ] Best practices de balanceo
  - [ ] FAQ para admins

- [ ] **Documentación de API**
  - [ ] OpenAPI/Swagger spec
  - [ ] Ejemplos de uso
  - [ ] Rate limiting y autenticación
  - [ ] Changelog de versiones

---

---

### **ETAPA 6: Características Futuras Opcionales** (Backlog)
- [ ] Integración con Citizens para NPCs ultra-avanzados
- [ ] Sistema de Guild Wars (guerras entre guilds con territorios)
- [ ] PvP Arena con ranking y temporadas
- [ ] Sistema de Matrimonios y Familias RPG
- [ ] Mundo persistente con economía global
- [ ] Integración con plugins de jobs (minero, herrero, etc.)

---

## 📊 Resumen de Módulos Pendientes

| Módulo | Etapas | Complejidad | Prioridad | Tiempo Estimado |
|--------|--------|-------------|-----------|-----------------|
| **MÓDULO 1: Progresión Avanzada** | 3 etapas | Alta | Alta | 3-4 semanas |
| **MÓDULO 2: Contenido Dinámico** | 3 etapas | Media-Alta | Media | 2-3 semanas |
| **MÓDULO 3: Integración** | 5 etapas | Alta | Media | 4-5 semanas |
| **MÓDULO 4: Optimización** | 4 etapas | Media | Alta | 2 semanas |
| **TOTAL** | **15 etapas** | - | - | **11-14 semanas** |

### Orden de Implementación Recomendado

1. **MÓDULO 4** (Testing y Optimización) - Asegurar que lo existente funciona perfectamente
2. **MÓDULO 1** (Progresión) - Retención de jugadores con bestiario y achievements
3. **MÓDULO 2** (Eventos) - Contenido dinámico para mantener servidor activo
4. **MÓDULO 3** (Expansión) - Features avanzadas cuando base esté sólida

---

## 4. Hitos y dependencias
- Plugin debe funcionar en contenedor PaperMC y detectar mundos RPG por metadata.
- Panel web debe consumir datos RPG y permitir administración por mundo.
- Toda la lógica RPG debe estar aislada por mundo y ser persistente.
- Configuración y datos editables sin recompilar.

---

## 5. Referencias y buenas prácticas
- Modularidad y separación de responsabilidades.
- Uso de APIs estándar (Vault, Citizens).
- Persistencia robusta y segura.
- Extensibilidad mediante eventos y configuración.
- Integración transparente con el sistema actual de mundos y panel web.

---

**Este roadmap debe actualizarse a medida que avance el desarrollo y se completen los hitos.**
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
# Roadmap: Normalización de Estructura de Archivos - Plugin MMORPG y Panel Web

**Fecha:** 9 de diciembre de 2025  
**Estado:** En Planificación  
**Objetivo:** Unificar y normalizar la gestión de archivos de configuración y datos entre el plugin MMORPG y el panel web.

---

## 📋 Tabla de Contenidos

1. [Clasificación de Datos](#clasificación-de-datos)
2. [Estructura de Directorios](#estructura-de-directorios)
3. [Modificaciones de Código](#modificaciones-de-código)
4. [Inicialización Automática](#inicialización-automática)
5. [Limpieza de Duplicados](#limpieza-de-duplicados)
6. [Secuencia de Implementación](#secuencia-de-implementación)

---

## 🚀 Estado Actual

- ✅ **Fase 1:** Crear estructura base en `config/` con archivos .example
- ✅ **Fase 2:** Actualizar scripts de instalación (`create.sh`, `install-mmorpg-plugin.sh`, `quick-install.sh`)
- ✅ **Fase 3:** Normalizar panel web - Implementar `_get_data_location()` y actualizar endpoints RPG
- ✅ **Fase 4:** Actualizar plugin Java con resolución automática de rutas
  - ✅ Crear `PathResolver.java` - Centraliza resolución de rutas
  - ✅ Crear `DataInitializer.java` - Auto-inicializa archivos faltantes
  - ✅ Integrar en `MMORPGPlugin.java` - Getters y acceso público
- ✅ **Fase 5:** Limpiar duplicados y archivo mal ubicados
  - ✅ Mover 4 archivos config de data/ a raíz de MMORPGPlugin/
  - ✅ Agregar archivos de datos universales faltantes (npcs.json, quests.json, enchantments.json, pets.json)
  - ✅ Estructura finalizada correctamente
- ✅ **Fase 6:** Pruebas end-to-end
  - ✅ Plan de testing completo documentado
  - ✅ 15 casos de testing definidos
  - ✅ Checklist de verificación preparado
  - ✅ Ready for manual testing execution

---

## 🗂️ Clasificación de Datos

### **Datos Universales (Globales para todos los mundos)**
Se almacenan en: `plugins/MMORPGPlugin/data/`

| Tipo | Archivo | Alcance | Editable |
|------|---------|---------|----------|
| **NPCs** | `npcs.json` | Universal | Sí (Panel Web) |
| **Quests** | `quests.json` | Universal | Sí (Panel Web) |
| **Mobs** | `mobs.json` | Universal | Sí (Panel Web) |
| **Mascotas** | `pets.json` | Universal | Sí (Panel Web) |
| **Encantamientos** | `enchantments.json` | Universal | Sí (Panel Web) |
| **Items** | `items.json` | Universal | Sí (Panel Web) |

### **Datos Locales por Mundo**
Se almacenan en: `worlds/{mundo}/data/`

| Tipo | Archivo | Alcance | Generado por |
|------|---------|---------|-------------|
| **NPCs (Mundo)** | `npcs.json` | Local | Panel Web / Plugin |
| **Quests (Mundo)** | `quests.json` | Local | Panel Web / Plugin |
| **Mobs (Mundo)** | `mobs.json` | Local | Panel Web / Plugin |
| **Mascotas (Mundo)** | `pets.json` | Local | Plugin (progreso jugadores) |
| **Encantamientos (Mundo)** | `enchantments.json` | Local | Panel Web / Plugin |

### **Datos Exclusivos Locales por Mundo**
Se almacenan en: `worlds/{mundo}/data/`

| Tipo | Archivo | Alcance | Generado por |
|------|---------|---------|-------------|
| **Jugadores RPG** | `players.json` | Local | Plugin (dato en tiempo real) |
| **Estado RPG** | `status.json` | Local | Plugin (dato en tiempo real) |
| **Invasiones** | `invasions.json` | Local | Plugin (eventos del mundo) |
| **Kills Tracking** | `kills.json` | Local | Plugin (estadísticas del mundo) |
| **Respawn Config** | `respawn.json` | Local | Panel Web / Plugin |
| **Metadata Mundo** | `metadata.json` | Local | Panel Web (información del mundo) |

---

## 📁 Estructura de Directorios

### **1. Configuración Base en `config/` (Ejemplos - Copias de Referencia)**

```
config/
├── plugin/                              # Archivos de configuración del plugin
│   ├── achievements_config.json.example
│   ├── bestiary_config.json.example
│   ├── crafting_config.json.example
│   ├── dungeons_config.json.example
│   ├── enchanting_config.json.example
│   ├── enchantments_config.json.example
│   ├── events_config.json.example
│   ├── invasions_config.json.example
│   ├── pets_config.json.example
│   ├── ranks_config.json.example
│   ├── respawn_config.json.example
│   └── squad_config.json.example
│
└── plugin-data/                         # Ejemplos de datos universales
    ├── items.json.example
    ├── mobs.json.example 
    ├── npcs.json.example
    ├── quests.json.example

    └── enchantments.json.example
```

**Responsabilidad:** `create.sh` e `install-mmorpg-plugin.sh` crean esta estructura con ejemplos.

---

### **2. Plugin MMORPG - Estructura en `plugins/MMORPGPlugin/`**

```
plugins/MMORPGPlugin/
├── achievements_config.json             # Configuración (copiado desde config/)
├── bestiary_config.json
├── crafting_config.json
├── dungeons_config.json
├── enchanting_config.json
├── enchantments_config.json
├── events_config.json
├── invasions_config.json
├── pets_config.json
├── ranks_config.json
├── respawn_config.json
├── squad_config.json
│
├── data/                                # Datos universales (globales)
│   ├── items.json                       # Items globales (copiado en install)
│   ├── mobs.json                        # Mobs globales
│   ├── npcs.json                        # NPCs globales
│   ├── quests.json                      # Quests globales
│   ├── pets.json                       # Mascotas globales
│   └── enchantments.json                # Encantamientos globales
│
└── src/
    ├── main/java/com/mmorpg/
    │   ├── config/
    │   ├── data/
    │   ├── managers/
    │   └── ...
```

**Responsabilidad:** 
- `install-mmorpg-plugin.sh` copia archivos de `config/plugin/` y `config/plugin-data/`
- Plugin valida y crea archivos faltantes con contenido por defecto al iniciar

---

### **3. Mundos - Estructura en `worlds/{mundo}/`**

```
worlds/mmorpg-survival/
├── metadata.json                        # Información del mundo (incluyendo isRPG)
├── server.properties                    # Propiedades del servidor
│
├── data/                                # Datos específicos de este mundo
│   ├── players.json                     # Jugadores RPG (en tiempo real)
│   ├── status.json                      # Estado RPG del mundo (en tiempo real)
│   ├── invasions.json                   # Invasiones activas en este mundo
│   ├── kills.json                       # Estadísticas de kills del mundo
│   ├── respawn.json                     # Configuración de respawn del mundo
│   │
│   ├── npcs.json (OPCIONAL)             # NPCs específicos del mundo
│   ├── quests.json (OPCIONAL)           # Quests específicas del mundo
│   ├── mobs.json (OPCIONAL)             # Mobs específicos del mundo
│   ├── pets.json (OPCIONAL)             # Evoluciones/mascotas capturadas
│   └── enchantments.json (OPCIONAL)     # Encantamientos específicos del mundo
│
├── world/                               # Datos de Minecraft
├── world_nether/
└── world_the_end/
```

**Responsabilidad:**
- `create.sh` crea carpeta `data/` vacía al crear un mundo
- Panel Web crea `data/` si no existe al crear mundo RPG
- Plugin crea archivos faltantes al detectar mundo RPG activo

---

## 🔧 Modificaciones de Código

### **2.1 Scripts de Instalación**

#### `create.sh`
```bash
# Cambios:
1. Crear estructura base en config/
   - config/plugin/*.example
   - config/plugin-data/*.example

2. Para cada mundo RPG:
   - Crear worlds/{mundo}/data/
   - Crear metadata.json con isRPG: true
```

#### `install-mmorpg-plugin.sh`
```bash
# Cambios:
1. Copiar archivos de config/plugin/ → plugins/MMORPGPlugin/
2. Copiar archivos de config/plugin-data/ → plugins/MMORPGPlugin/data/
3. Verificar y crear directorios si no existen
4. Establecer permisos correctos
```

#### `quick-install.sh`
```bash
# Cambios:
- Llamar a install-mmorpg-plugin.sh
- Garantizar que config/ existe antes de copiar
```

---

### **2.2 Panel Web - Rutas de Datos**

#### Archivo: `web/app.py`

**Cambios principales:**
```python
# Función nueva: _get_data_location(world_slug, data_type, scope)
# Retorna la ruta correcta según:
#   - world_slug: mundo específico o None para global
#   - data_type: 'npc', 'quest', 'mob', 'pet', 'enchantment', 'player', 'invasion', 'kills', 'respawn'
#   - scope: 'local' o 'universal'

# Rutas resolución:
UNIVERSAL = plugins/MMORPGPlugin/data/{filename}.json
LOCAL = worlds/{mundo}/data/{filename}.json
```

**Endpoints a actualizar:**
| Endpoint | Cambio |
|----------|--------|
| `/api/rpg/npcs` | Diferenciar entre universales y locales |
| `/api/rpg/quests` | Diferenciar entre universales y locales |
| `/api/rpg/mobs` | Diferenciar entre universales y locales |
| `/api/rpg/pets` | Diferenciar entre universales y locales |
| `/api/rpg/enchantments` | Diferenciar entre universales y locales |
| `/api/rpg/invasions` | Solo local |
| `/api/rpg/kills` | Solo local |
| `/api/rpg/respawn` | Solo local |
| `/api/worlds/<slug>/rpg/summary` | Usar nuevas rutas |

---

#### Archivo: `web/models/rpg_manager.py`

**Cambios principales:**
```python
class RPGManager:
    def _resolve_data_path(self, world_slug, filename, scope='universal'):
        """
        Resuelve la ruta correcta para archivos de datos RPG.
        
        Args:
            world_slug: slug del mundo (None para datos universales)
            filename: nombre del archivo (ej: 'npcs.json')
            scope: 'universal' o 'local'
        
        Returns:
            Path: ruta al archivo
        """
        if scope == 'universal':
            return Path(self.plugin_data_path) / filename
        elif scope == 'local':
            return Path(MINECRAFT_DIR) / 'worlds' / world_slug / 'data' / filename
        else:
            raise ValueError(f"Scope inválido: {scope}")
    
    def get_data_by_scope(self, world_slug, filename, data_key=None, scope='local'):
        """
        Obtiene datos separados por scope (local/universal).
        """
        # Implementación usando _resolve_data_path
```

---

#### Archivo: `web/models/world_manager.py`

**Cambios principales:**
```python
class WorldManager:
    def create_world(self, ...):
        # Cambios:
        1. Crear worlds/{mundo}/data/ automáticamente
        2. Crear metadata.json con estructura completa
        3. Si isRPG=True, crear archivos base en data/:
           - status.json (vacío {})
           - players.json (vacío {})
           - invasions.json (vacío [])
           - kills.json (vacío {})
           - pets.json  (vacío [])
           - respawn.json (copia de config/plugin-data/respawn.json.example)
```

---

### **2.3 Plugin Java (src/)**

**Cambios principales:**

#### Gestión de Configuración
```java
// Antes: Archivos sueltos en plugins/MMORPGPlugin/
// Después: Cargar desde plugins/MMORPGPlugin/{archivo}.json

ConfigLoader configLoader = new ConfigLoader(pluginDataDir);
CraftingConfig crafting = configLoader.loadCraftingConfig();
```

#### Gestión de Datos Universales
```java
// Ubicación: plugins/MMORPGPlugin/data/

DataManager dataManager = new DataManager(pluginDataDir);
List<Mob> mobs = dataManager.loadMobs();        // plugins/MMORPGPlugin/data/mobs.json
List<NPC> npcs = dataManager.loadNPCs();        // plugins/MMORPGPlugin/data/npcs.json
List<Quest> quests = dataManager.loadQuests();  // plugins/MMORPGPlugin/data/quests.json
```

#### Gestión de Datos Locales por Mundo
```java
// Ubicación: worlds/{mundo}/data/

WorldDataManager worldData = new WorldDataManager(worldDir);
PlayerData players = worldData.loadPlayers();           // worlds/{mundo}/data/players.json
WorldStatus status = worldData.loadStatus();           // worlds/{mundo}/data/status.json
List<Invasion> invasions = worldData.loadInvasions();  // worlds/{mundo}/data/invasions.json
KillStats kills = worldData.loadKills();               // worlds/{mundo}/data/kills.json
RespawnConfig respawn = worldData.loadRespawn();       // worlds/{mundo}/data/respawn.json
```

#### Inicialización Automática
```java
// Al iniciar el plugin:
class MMORPGInitializer {
    public void initialize() {
        // 1. Crear plugins/MMORPGPlugin/data/ si no existe
        // 2. Copiar archivos desde config/plugin-data/*.example si faltan
        // 3. Crear archivos vacíos si no hay ejemplos
        
        // 4. Para cada mundo RPG activo:
        //    - Crear worlds/{mundo}/data/ si no existe
        //    - Crear status.json, players.json si faltan
        //    - Crear invasions.json, kills.json si faltan
    }
}
```

---

## 🚀 Inicialización Automática

### **Responsabilidades del Plugin**

Cuando el plugin MMORPG inicia (`onEnable()`):

1. **Validar estructura de datos universales:**
   ```
   Si no existe plugins/MMORPGPlugin/data/
   → Crear directorio
   
   Para cada archivo esperado (mobs.json, npcs.json, etc.):
   → Si no existe en plugins/MMORPGPlugin/data/
   → Copiar desde config/plugin-data/{archivo}.example
   → Si no existe ejemplo, crear vacío {}
   ```

2. **Validar estructura de cada mundo RPG activo:**
   ```
   Para mundo_actual:
   Si es_rpg = true en metadata.json
   
   → Crear worlds/{mundo}/data/ si no existe
   → Crear status.json si no existe: {}
   → Crear players.json si no existe: {}
   → Crear invasions.json si no existe: []
   → Crear kills.json si no existe: {}
   ```

### **Responsabilidades del Panel Web**

Cuando se crea un mundo RPG desde el panel:

1. **Crear estructura de directorios:**
   ```
   worlds/{nuevo_mundo}/data/
   ```

2. **Crear archivos base:**
   ```
   metadata.json (con isRPG: true)
   data/status.json (vacío {})
   data/players.json (vacío {})
   data/invasions.json (vacío [])
   data/kills.json (vacío {})
   ```

---

## 🧹 Limpieza de Duplicados

### **Archivos a Eliminar**

```
Eliminar del repositorio actual:
├── plugins/MMORPGPlugin/data/world/*
│   (Mantener solo si es symlink/referencia actual)
├── plugins/MMORPGPlugin/npcs.json (si existe)
├── plugins/MMORPGPlugin/quests.json (si existe)
├── plugins/MMORPGPlugin/mobs.json (si existe)
├── plugins/MMORPGPlugin/items.json (si existe)
└── Cualquier otro archivo suelto que debería estar en config/ o data/
```

### **Archivos a Mantener como Referencia**

```
Mantener en config/ para referencia:
├── config/plugin/*.example
├── config/plugin-data/*.example
└── docs/ROADMAP_NORMALIZACION_ARCHIVOS.md
```

---

## 📋 Secuencia de Implementación

### **Fase 1: Preparación (Sem 1)**
- ✅ Crear estructura base en `config/plugin/` y `config/plugin-data/` con archivos `.example`
- ✅ Generar ejemplos de contenido para cada tipo de archivo
- ✅ Actualizar `create.sh` para generar estructura base

### **Fase 2: Scripts de Instalación (Sem 1-2)**
- ✅ Actualizar `install-mmorpg-plugin.sh` para copiar desde `config/`
- ✅ Actualizar `quick-install.sh`
- ✅ Probar en entorno limpio

### **Fase 3: Normalización del Panel Web (Sem 2-3)**
- ✅ Actualizar `web/app.py` con funciones de resolución de rutas
- ✅ Actualizar `web/models/rpg_manager.py` con métodos de scope
- ✅ Actualizar `web/models/world_manager.py` para crear `worlds/{mundo}/data/`
- ✅ Actualizar endpoints de API para usar nuevas rutas
- ✅ Probar endpoints con datos locales y universales

### **Fase 4: Actualización del Plugin Java (Sem 3-4)**
- ✅ Crear clases para resolución de rutas
- ✅ Actualizar ConfigLoader para leer de nuevas ubicaciones
- ✅ Crear/actualizar DataManager para datos universales
- ✅ Crear WorldDataManager para datos locales
- ✅ Implementar inicialización automática

### **Fase 5: Limpieza (Sem 4)**
- ✅ Eliminar archivos duplicados del repositorio
- ✅ Validar que no hay referencias rotas
- ✅ Documentar cambios en CHANGELOG

### **Fase 6: Pruebas e Integración (Sem 5)**
- ✅ Test end-to-end: crear mundo → guardar NPC → verificar ubicación
- ✅ Test end-to-end: invasión → guardar en local → verificar ubicación
- ✅ Test end-to-end: cargar datos universales y locales desde panel
- ✅ Test: inicialización automática de archivos faltantes

---

## 📊 Resumen de Cambios por Archivo

| Archivo | Líneas | Cambios Principales | Prioridad |
|---------|--------|----------------------|-----------|
| `create.sh` | ~50 | Crear `config/plugin/` y `config/plugin-data/` | **ALTO** |
| `install-mmorpg-plugin.sh` | ~60 | Copiar de `config/` a `plugins/` | **ALTO** |
| `quick-install.sh` | ~20 | Llamar a install-mmorpg-plugin.sh | **ALTO** |
| `web/app.py` | ~100 | Agregar `_get_data_location()` y actualizar endpoints | **ALTO** |
| `web/models/rpg_manager.py` | ~80 | Agregar `_resolve_data_path()` y métodos de scope | **ALTO** |
| `web/models/world_manager.py` | ~30 | Crear `data/` en `create_world()` | **MEDIO** |
| Plugin Java - ConfigLoader | ~40 | Leer de nuevas rutas | **MEDIO** |
| Plugin Java - DataManager | ~100 | Gestionar datos universales | **MEDIO** |
| Plugin Java - WorldDataManager | ~120 | Gestionar datos locales | **MEDIO** |
| Plugin Java - Initializer | ~80 | Crear archivos faltantes | **MEDIO** |

---

## 🎯 Beneficios de la Normalización

✅ **Centralización:** Todos los archivos en ubicaciones predecibles  
✅ **Escalabilidad:** Fácil agregar nuevos tipos de datos  
✅ **Mantenibilidad:** Panel Web y Plugin leen de las mismas rutas  
✅ **Automatización:** Plugin crea archivos faltantes automáticamente  
✅ **Eliminación de Duplicados:** Una única fuente de verdad por tipo de dato  
✅ **Documentación:** Estructura clara y documentada  

---

## ❓ Confirmaciones Necesarias

Antes de iniciar la implementación:

1. ¿Aprueban la estructura propuesta?
2. ¿Algún archivo o tipo de dato adicional que falta?
3. ¿Confirman el orden de prioridades de implementación?
4. ¿Desean que la inicialización automática sea responsabilidad del plugin, panel o ambos?

---

**Documento creado:** 9 de diciembre de 2025  
**Versión:** 1.0 - Planificación  
**Próxima revisión:** Tras confirmación de estructura
