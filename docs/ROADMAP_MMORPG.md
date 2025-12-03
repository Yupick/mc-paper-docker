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
- [ ] Crear estructura base del plugin PaperMC (Java, Maven/Gradle)
- [ ] Definir `plugin.yml` con permisos y dependencias (Vault, Citizens)
- [ ] Implementar sistema de configuración YAML/JSON editable
- [ ] Integrar flag `isRPG` en metadata de mundos y lógica de activación
- [ ] Crear endpoints/archivos para comunicación con el panel web

### Fase 2: Sistemas RPG principales
- [ ] Sistema de clases (mínimo 5 clases básicas, habilidades, asignación por NPC)
- [ ] Sistema de NPCs (entrenadores, comerciantes, misiones; integración Citizens)
- [ ] Sistema de quests (objetivos, recompensas, desbloqueo de habilidades)
- [ ] Sistema de ítems RPG (creación, estadísticas, recompensas, comercio)
- [ ] Persistencia por mundo (SQLite/MySQL, datos RPG por jugador y mundo)
- [ ] Integración con Vault para economía

### Fase 3: Integración y panel web
- [ ] Exponer endpoints/archivos JSON/YAML con datos RPG por mundo
- [ ] Adaptar panel web para mostrar solapa RPG solo en mundos con `isRPG: true`
- [ ] Administración de clases, quests, NPCs e ítems desde el panel
- [ ] Sincronización de datos entre plugin y panel web

### Fase 4: Extensibilidad y extras
- [ ] Eventos personalizados (ej: RPGClassAssignedEvent, QuestCompletedEvent)
- [ ] Sistema de logs y auditoría
- [ ] Soporte multilenguaje (archivos YAML)
- [ ] Compatibilidad opcional con Bedrock (Geyser/Floodgate)
- [ ] Documentación técnica y de usuario

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
