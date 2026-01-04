# 📋 RESUMEN EJECUTIVO: Plan de Migración a SQLite

## 🎯 OBJETIVO
Migrar toda la arquitectura de datos del MMORPG Plugin de una mezcla JSON/SQLite a **SQLite 100%**, eliminando la duplicación y desincronización de datos.

**Resultado esperado:** Panel web siempre sincronizado, mejor performance, sin confusiones de dónde están los datos.

---

## 📊 DOCUMENTOS GENERADOS (Revisar en orden)

### 1. **ROADMAP_MIGRACION_SQLITE.md** 
   - Visión general del proyecto
   - 6 fases de implementación
   - Estimación de tiempo (20-26 horas)
   - **5 preguntas para confirmar decisiones**

### 2. **ANALISIS_CONFIG_FOLDER.md**
   - Desglose de cada archivo en `/config/`
   - Qué mantener ✅ / Qué eliminar ❌ / Qué migrar ⏳
   - Plan de limpieza de 350 KB
   - **Decisión principal:** Eliminar config/data/ (308 KB de datos obsoletos)

### 3. **ARQUITECTURA_SQLITE_PROPUESTA.md**
   - Diagrama antes/después
   - 5 escenarios de flujo de datos
   - Solución a race conditions
   - Estructura final de carpetas
   - **Checklist pre-implementación**

---

## 🗂️ ESTRUCTURA ACTUAL (Problemática)

```
Plugin usa SQLite (rpgdata.db)  ←→  Web usa JSON
   └─ Cambios NO se sincronizan
   └─ Panel web siempre desactualizado
   └─ Datos duplicados en múltiples formatos
```

---

## 🗂️ ESTRUCTURA PROPUESTA (Solución)

```
Dos bases de datos SQLite:

1. universal.db (Global, compartido)
   └─ Items, mobs, encantamientos, achievements
   
2. {world_slug}.db (Local, por mundo)
   └─ Jugadores, quests, NPCs, economía
   
Plugin ↔ SQLite ↔ Web Panel
   ✅ Sincronización automática
   ✅ Una fuente de verdad
   ✅ Sin JSON (excepto config crítica)
```

---

## 📝 DECISIONES CLAVE A TOMAR

### 1. ¿Mantener templates JSON en config/?
- [ ] **SÍ:** Guardar en config/templates/ (fácil personalización)
- [ ] **NO:** Eliminar, usar scripts de setup
- [ ] **MIGRAR:** Pasar todo a base de datos

### 2. ¿Una DB universal o dos bases separadas?
- [ ] **RECOMENDADO:** Universal + {world}.db (mejor organización)
- [ ] **SIMPLE:** Una sola DB para todo

### 3. ¿Sincronización web-plugin en tiempo real?
- [ ] **SÍ:** Cada X segundos (mejor UX)
- [ ] **NO:** Solo lectura desde web (más simple)

### 4. ¿Migrar datos existentes?
- [ ] **SÍ:** Convertir JSON → SQLite (preservar datos)
- [ ] **NO:** Empezar de cero (más limpio, pero pierde data)

### 5. ¿Mantener backups JSON periodicamente?
- [ ] **SÍ:** Exportar DB a JSON cada X horas
- [ ] **NO:** SQLite como única fuente

---

## 🔴 CAMBIOS PRINCIPALES

### Eliminar AHORA (Sin código afectado):
```
✂️ config/data/              (308 KB de datos obsoletos)
✂️ config/MMORPGPlugin/      (duplicado)
✂️ config/api/               (datos viejos)
✂️ config/npcs/, pets/, etc. (múltiples carpetas obsoletas)
```

### Crear:
```
📁 plugins/MMORPGPlugin/universal.db
📁 plugins/MMORPGPlugin/data/{world_slug}/{world_slug}.db
```

### Modificar Plugin (8 archivos Java):
```java
DatabaseManager          → 2 conexiones (universal + world)
EnchantmentManager       → Lee universal.db
CraftingManager          → Lee universal.db
QuestManager             → Lee {world}.db
NPCManager               → Lee {world}.db
ClassManager             → Lee {world}.db
EconomyManager           → Lee {world}.db
EventManager             → Lee universal.db + {world}.db
```

### Modificar Web (3 archivos Python):
```python
rpg_manager.py          → Queries SQLite en lugar de JSON
app.py                  → Endpoints usan DB
world_manager.py        → Crea {world}.db al crear mundo RPG
```

---

## ⏱️ ESTIMACIÓN DE TIEMPO

| Tarea | Tiempo |
|-------|--------|
| Fase 1: Limpieza config/ | 1h |
| Fase 2: Diseño DB | 2h |
| Fase 3: Modificar Plugin | 8-10h |
| Fase 4: Modificar Web | 4-6h |
| Fase 5: Reorganizar carpetas | 1h |
| Fase 6: Testing integral | 4-6h |
| **TOTAL** | **20-26 horas** |

---

## ✅ PRÓXIMOS PASOS

### OPCIÓN A: Aprobación Rápida
```
1. Revisar los 3 documentos generados
2. Responder las 5 preguntas clave
3. Dar el OK para Fase 1 (limpieza)
4. Proceder con implementación
```

### OPCIÓN B: Discusión Detallada
```
1. Revisar documentos
2. Hacer preguntas sobre detalles específicos
3. Ajustar arquitectura si es necesario
4. Consenso antes de empezar
```

---

## 📌 ARCHIVOS GENERADOS PARA REVISAR

```
/home/mkd/contenedores/mc-paper-docker/
├── ROADMAP_MIGRACION_SQLITE.md          ← Planificación general
├── ANALISIS_CONFIG_FOLDER.md            ← Análisis de /config/
└── ARQUITECTURA_SQLITE_PROPUESTA.md     ← Diagramas y flujos
```

---

## 🎯 RESUMEN EN UNA FRASE

> **"Eliminar mezcla JSON/SQLite, usar SQLite para todo, panel web siempre sincronizado, mejor performance, código más limpio."**

---

## ❓ ¿QUÉ HACER AHORA?

1. **Lee los 3 documentos** (orden recomendado):
   - Primero: ROADMAP
   - Segundo: ANALISIS_CONFIG_FOLDER
   - Tercero: ARQUITECTURA_SQLITE_PROPUESTA

2. **Responde las 5 preguntas** de decisión

3. **Confirma:**
   - ¿Aprobamos este plan?
   - ¿Algún cambio?
   - ¿Listo para Fase 1?

4. **Una vez aprobado:**
   - Comenzaremos con **Fase 1: Limpieza de `/config/`**
   - Luego procederemos con modificación del código

---

## 📞 NOTAS

- Todos los cambios son reversibles (tenemos backup)
- No afecta a Minecraft data (level.dat, region files, etc)
- No afecta a jugadores en línea
- Cambio gradual (puede hacerse paso a paso)

---

**¿Listo para revisar y tomar decisiones?**
