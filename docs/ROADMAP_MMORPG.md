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
