# ✅ CHECKLIST EJECUTIVO: Decisiones Previas a Implementación

**IMPORTANTE:** Antes de cambiar cualquier código, necesito tus respuestas a estas 5 preguntas.

---

## 5️⃣ DECISIONES CLAVE

### ❓ DECISIÓN 1: ¿Qué hacemos con los templates JSON en config/?
**Contexto:** Archivos como `crafting_config.json`, `enchanting_config.json`, etc.  
**Opciones:**
```
[ ] A. MANTENER EN CONFIG/
    - Ubicación: config/templates/
    - Uso: Templates para instalación personalizada
    - Ventaja: Fácil modificar antes de jugar
    - Desventaja: Mantener duplicados

[ ] B. ELIMINAR
    - Solo usar desde SQLite
    - Ventaja: Una fuente única
    - Desventaja: Menos flexible para personalizar

[ ] C. MIGRAR
    - Cargar una sola vez al iniciar plugin
    - Guardar en universal.db
    - Eliminar archivos JSON después
    - Ventaja: Lo mejor de ambos mundos
```
**RECOMENDACIÓN:** Opción C (Migrar)

---

### ❓ DECISIÓN 2: ¿Una o dos bases de datos SQLite?

**Opción A: DOS BASES (RECOMENDADA)**
```
universal.db
├── enchantments
├── crafting_recipes
├── items
├── mobs
└── achievements_def

{world_slug}.db (una por mundo)
├── players
├── quests
├── npcs
└── economy
```
- ✅ Mejor organización
- ✅ Fácil escalar (múltiples mundos)
- ✅ Datos claros (globales vs locales)
- ❌ Dos archivos a gestionar

**Opción B: UNA SOLA BASE**
```
mmorpg.db
├── (TODO mezclado)
```
- ✅ Más simple
- ❌ Difícil escalar
- ❌ Datos confusos
- ❌ Rendimiento si crece mucho

**RECOMENDACIÓN:** Opción A (Dos bases)

---

### ❓ DECISIÓN 3: ¿Sincronización web-plugin?

**Opción A: SINCRONIZACIÓN EN TIEMPO REAL**
```
Plugin escribe en DB
Web lee inmediatamente (< 1 segundo)
Flujo: Plugin → DB → Web
```
- ✅ Panel siempre actualizado
- ✅ Cambios instantáneos
- ❌ Código más complejo
- ❌ Necesita manejo de concurrencia

**Opción B: SIN SINCRONIZACIÓN EN TIEMPO REAL**
```
Web solo LECTURA
Plugin solo LECTURA (de cambios del web)
Flujo: Plugin → DB ← Web
```
- ✅ Más simple
- ✅ Sin race conditions
- ❌ Panel se actualiza cada X segundos
- ❌ Cambios del web demoran en verse en-juego

**Opción C: HIBRID**
```
Plugin → DB (inmediato)
Web lee cada 5-10 segundos
Web → DB (cambios específicos)
Plugin lee cambios cada 30 segundos
```
- ✅ Balance entre simpleza y actualización
- ✅ Evita race conditions
- ✅ Actualización razonablemente rápida

**RECOMENDACIÓN:** Opción C (Híbrido)

---

### ❓ DECISIÓN 4: ¿Migrar datos existentes?

**Opción A: CONVERTIR JSON → SQLite**
```
- Leer config/data/
- Convertir a SQLite
- Preservar todos los datos existentes
```
- ✅ No pierden datos
- ✅ Continuidad
- ❌ Script de migración complejo
- ❌ Riesgo de inconsistencias

**Opción B: EMPEZAR DE CERO**
```
- Eliminar todo
- Nuevas DBs vacías
- Jugadores crean todo de nuevo
```
- ✅ Más limpio
- ✅ Más simple
- ❌ Pierden todos los datos
- ❌ Jugadores se molestan

**RECOMENDACIÓN:** Opción A (Convertir)

---

### ❓ DECISIÓN 5: ¿Mantener backups JSON?

**Opción A: EXPORTAR PERIÓDICAMENTE**
```
- Cada hora/día
- Plugin exporta DB → JSON
- Ubicación: backups/rpg_data_{timestamp}.json
```
- ✅ Respaldo de datos
- ✅ Fácil restaurar
- ❌ Más operaciones de I/O
- ❌ Archivos más grandes

**Opción B: SOLO BACKUPS DE BD**
```
- Respaldar .db files
- Usar herramientas SQLite
```
- ✅ Más eficiente
- ✅ Menos archivos
- ❌ Requiere SQLite para restaurar

**Opción C: AMBOS**
```
- Backups JSON Y BD
- Máxima redundancia
```
- ✅ Máxima seguridad
- ❌ Máximo almacenamiento

**RECOMENDACIÓN:** Opción B (Solo backups DB)

---

## 📋 RESUMEN DE RECOMENDACIONES

Si aceptas TODAS mis recomendaciones:

```
✅ Decisión 1: MIGRAR templates a SQLite
✅ Decisión 2: DOS bases (universal + {world}.db)
✅ Decisión 3: SINCRONIZACIÓN HÍBRIDA (cada 5-10s web, cada 30s plugin)
✅ Decisión 4: CONVERTIR datos existentes
✅ Decisión 5: BACKUPS de .db (no JSON)

RESULTADO:
- Eliminar 350 KB de JSON/carpetas obsoletas
- Código más limpio y mantenible
- Panel web siempre actualizado
- Mejor performance
- Un punto único de verdad (SQLite)
```

---

## 📝 TUS RESPUESTAS

**Por favor, indica tu opción para cada decisión:**

```
DECISIÓN 1: ¿Qué hacemos con templates JSON?
[ ] A. Mantener en config/templates/
[ ] B. Eliminar
[X] C. Migrar a SQLite (RECOMENDADO)

DECISIÓN 2: ¿Una o dos bases de datos?
[X] A. DOS bases - universal.db + {world}.db (RECOMENDADO)
[ ] B. Una sola base

DECISIÓN 3: ¿Sincronización web-plugin?
[ ] A. Tiempo real (complejo)
[ ] B. Sin sincronización (simple pero viejo)
[X] C. Híbrida - cada X segundos (RECOMENDADO)

DECISIÓN 4: ¿Migrar datos existentes?
[X] A. Convertir JSON → SQLite (RECOMENDADO)
[ ] B. Empezar de cero

DECISIÓN 5: ¿Mantener backups JSON?
[X] B. Solo backups de .db (RECOMENDADO)
[ ] A. Exportar JSON periódicamente
[ ] C. Ambos
```

---

## 🚀 PRÓXIMOS PASOS (Una vez aprobado)

### FASE 1A: Limpieza Inmediata
```
1. [ ] Backup completo: cp -r config config.backup
2. [ ] Eliminar config/data/
3. [ ] Eliminar config/MMORPGPlugin/
4. [ ] Eliminar config/api/, npcs/, pets/, etc.
5. [ ] Verificar que create.sh aún funciona
```

### FASE 1B: Después de código
```
6. [ ] Migrar datos JSON → SQLite
7. [ ] Crear universal.db con tablas
8. [ ] Crear {world}.db con tablas
9. [ ] Verificar integridad de datos
```

### FASE 2-6: Cambios de código (Plugin + Web)
```
10. [ ] Refactorizar DatabaseManager
11. [ ] Actualizar todos los managers
12. [ ] Modificar rpg_manager.py
13. [ ] Testing completo
14. [ ] Eliminar datos JSON redundantes
```

---

## ⏱️ TIEMPO ESTIMADO

**Fases 1-6:** 20-26 horas  
**Distribución:**
- Fase 1 (Limpieza): 1 hora
- Fase 2 (Diseño): 2 horas
- Fase 3 (Plugin): 8-10 horas
- Fase 4 (Web): 4-6 horas
- Fase 5 (Directorios): 1 hora
- Fase 6 (Testing): 4-6 horas

---

## ❓ ¿DUDAS?

Si tienes preguntas sobre cualquier decisión:

1. Lee el documento relevante:
   - DECISIÓN 1 → ROADMAP (sección Templates)
   - DECISIÓN 2 → ARQUITECTURA (sección Base de Datos)
   - DECISIÓN 3 → ARQUITECTURA (sección Concurrencia)
   - DECISIÓN 4-5 → ROADMAP (sección Consideraciones)

2. Pregunta lo que necesites antes de empezar

3. Una vez claro, confirmamos y empezamos

---

## ✅ CONFIRMACIÓN FINAL

**Por favor responde:**

```
¿Apruebas las 5 decisiones recomendadas?
[ ] SÍ, empezamos con Fase 1A (Limpieza)
[ ] NO, quiero cambiar algunas decisiones
[ ] PARCIAL, tengo dudas sobre...
```

**Si es NO o PARCIAL:**
```
¿Cuál es tu alternativa preferida para cada decisión?
(Usa el formato de "TUS RESPUESTAS" arriba)
```

---

## 🎯 META FINAL

Una vez aprobado y completado:

```
✅ config/data/ eliminado (300 KB liberados)
✅ Datos en SQLite (una fuente única)
✅ Panel web sincronizado (siempre actualizado)
✅ Código plugin limpio (sin exportar JSON)
✅ Código web limpio (sin leer JSON)
✅ Performance mejorado (queries en lugar de I/O)
✅ Mantenibilidad mejorada (no hay duplicación)
```

---

**¿Listo para responder y comenzar?**

Espero tus respuestas a las 5 preguntas.
