# 📖 LEER PRIMERO: Índice de Documentos de Migración a SQLite

**Fecha:** 27 de diciembre de 2025  
**Status:** PLANIFICACIÓN - Pendiente tu aprobación

---

## 🚦 ORDEN DE LECTURA RECOMENDADO

### 1️⃣ **CHECKLIST_DECISIONES.md** ← EMPIEZA AQUÍ
   - 5 preguntas para tomar decisiones
   - Opciones claras para cada una
   - Mis recomendaciones
   - **TIEMPO:** 10-15 minutos
   
   **→ Responde las 5 preguntas antes de continuar**

---

### 2️⃣ **00_RESUMEN_PLAN_MIGRACION.md** ← LUEGO ESTO
   - Resumen ejecutivo del plan
   - Objetivos y cambios principales
   - Timeline estimado
   - Próximos pasos
   - **TIEMPO:** 5 minutos

---

### 3️⃣ **ROADMAP_MIGRACION_SQLITE.md** ← PLANIFICACIÓN DETALLADA
   - 6 fases completas
   - Estimación por fase
   - Estructura de datos propuesta
   - Pasos de implementación
   - **TIEMPO:** 30-40 minutos

---

### 4️⃣ **ANALISIS_CONFIG_FOLDER.md** ← SI QUIERES DETALLES DE /config/
   - Desglose de CADA archivo en /config/
   - Qué mantener ✅ / Qué eliminar ❌ / Qué migrar ⏳
   - Tamaños actuales
   - Plan de limpieza
   - **TIEMPO:** 20-30 minutos
   - **IMPORTANTE PARA:** Tomar decisión sobre qué borrar

---

### 5️⃣ **ARQUITECTURA_SQLITE_PROPUESTA.md** ← DIAGRAMAS Y FLUJOS
   - Arquitectura antes/después
   - 5 escenarios de flujo de datos
   - Manejo de concurrencia
   - Estructura final de carpetas
   - Ciclo de vida de un dato
   - **TIEMPO:** 30 minutos
   - **IMPORTANTE PARA:** Entender cómo funcionará

---

## 📊 RESUMEN EJECUTIVO (2 minutos)

### El Problema
```
Plugin escribe en SQLite  ✅
Web lee JSON             ❌ Desactualizado
= Panel web nunca está sincronizado
```

### La Solución
```
Plugin → SQLite ← Web
= Ambos leen/escriben en la misma base de datos
= Panel siempre sincronizado
```

### Lo que vas a cambiar
```
1. Eliminar config/data/ (300 KB de datos viejos)
2. Crear universal.db (datos globales)
3. Crear {world}.db por cada mundo (datos locales)
4. Refactorizar plugin (8 cambios)
5. Refactorizar web (3 cambios)
6. Testing y validación
```

### Tiempo estimado
```
20-26 horas de trabajo
```

---

## 🎯 LO QUE NECESITO DE TI

### OPCIÓN A: Rápido (Confía en mis recomendaciones)
```
1. Lee CHECKLIST_DECISIONES.md (10 min)
2. Responde "SÍ" a todas mis recomendaciones
3. Decimos: "Adelante, Fase 1"
4. TOTAL: 10 minutos
```

### OPCIÓN B: Detallado (Revisa todo antes)
```
1. Lee todos los documentos en orden (2 horas)
2. Haz preguntas si tienes dudas
3. Aprobamos arquitectura
4. Decimos: "Adelante, Fase 1"
5. TOTAL: 2+ horas
```

### OPCIÓN C: Selectivo (Equilibrio)
```
1. Lee CHECKLIST_DECISIONES.md (10 min)
2. Lee 00_RESUMEN_PLAN_MIGRACION.md (5 min)
3. Si tienes dudas, lee los documentos específicos
4. Responde las 5 preguntas
5. TOTAL: 15-45 minutos
```

---

## 📁 UBICACIÓN DE DOCUMENTOS

```
/home/mkd/contenedores/mc-paper-docker/
├── LEER_PRIMERO.md                     ← TÚ ESTÁS AQUÍ
├── CHECKLIST_DECISIONES.md             ← EMPIEZA AQUÍ
├── 00_RESUMEN_PLAN_MIGRACION.md        ← SEGUNDO
├── ROADMAP_MIGRACION_SQLITE.md         ← TERCERO
├── ANALISIS_CONFIG_FOLDER.md           ← CUARTO
└── ARQUITECTURA_SQLITE_PROPUESTA.md    ← QUINTO
```

---

## ✅ CHECKLIST RÁPIDO

```
[ ] Entiendo que hay un problema con JSON vs SQLite
[ ] Entiendo que la solución es usar SOLO SQLite
[ ] He leído CHECKLIST_DECISIONES.md
[ ] Tengo respuestas para las 5 preguntas
[ ] Estoy listo para aprobar el plan
```

---

## ❓ ¿QUÉ HAGO AHORA?

### PASO 1: Lee CHECKLIST_DECISIONES.md

No leas nada más hasta no responder esas 5 preguntas.

### PASO 2: Dame tus respuestas

Cuando termines CHECKLIST_DECISIONES.md, dime:
```
Decisión 1: [ ] A [ ] B [X] C
Decisión 2: [X] A [ ] B
Decisión 3: [ ] A [ ] B [X] C
Decisión 4: [X] A [ ] B
Decisión 5: [X] A [ ] B [ ] C
```

(Usa [X] para tu opción elegida)

### PASO 3: Yo confirmo el plan

Si tus respuestas son iguales a mis recomendaciones:
- ✅ Plan aprobado
- ✅ Listo para Fase 1

Si son diferentes:
- ⚠️ Discutimos alternativa
- ⚠️ Ajustamos arquitectura
- ✅ Plan revisado y aprobado

### PASO 4: Comenzamos Fase 1

Una vez aprobado:
```
Fase 1A: Eliminar archivos obsoletos de /config/
Fase 1B: Migrar datos JSON → SQLite
Fase 2-6: Modificaciones de código
```

---

## ⏱️ TIMELINE TOTAL

```
Decisión + Aprobación:     1-2 horas
Fase 1 (Limpieza):         1 hora
Fase 2-6 (Desarrollo):     19-25 horas
Testing final:             1-2 horas
TOTAL:                     22-30 horas
```

---

## 🔒 GARANTÍAS

1. **Backup antes de todo**
   - Hacemos `cp -r config config.backup`
   - Todos los cambios son reversibles

2. **Testing completo**
   - Verificamos que nada se rompe
   - Validamos integridad de datos
   - Probamos con mundo existente

3. **Sin downtime para jugadores**
   - Cambios se hacen "detrás de escenas"
   - Mundo sigue funcionando normalmente
   - Solo requiere reinicio del plugin al final

---

## 🎯 META FINAL

```
✅ config/ limpio (sin datos obsoletos)
✅ Datos en SQLite (una fuente única)
✅ Panel web sincronizado (siempre actualizado)
✅ Plugin y web sincronizados (no hay conflictos)
✅ Performance mejorado (sin leer JSON)
✅ Código más limpio (sin duplicación)
✅ Fácil de mantener (todo en DB)
```

---

## 🚀 VAMOS

**¿Listo?**

→ Abre **CHECKLIST_DECISIONES.md**

→ Responde las 5 preguntas

→ Espero tus respuestas

**Después decidimos si empezamos con Fase 1.**

---

*Documentos preparados para revisión y aprobación.*
