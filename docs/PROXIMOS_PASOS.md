# 🚀 SIGUIENTES PASOS: Guía de Ejecución

## Estado Actual
✅ **Todas las Fases 1-6 completadas automáticamente**

Las siguientes tareas están listas para ejecución manual:

---

## 1️⃣ Compilar el Plugin Java

```bash
cd /home/mkd/contenedores/mc-paper/mmorpg-plugin
mvn clean package
```

**Esperar:**
- ✅ Compilación sin errores
- ✅ JAR generado en `target/mmorpg-plugin-1.0.0.jar`

**Si hay errores:**
- Revisar imports en `PathResolver.java` y `DataInitializer.java`
- Asegurar que pom.xml tiene Gson como dependencia

---

## 2️⃣ Copiar JAR al Plugin

```bash
cp /home/mkd/contenedores/mc-paper/mmorpg-plugin/target/mmorpg-plugin-1.0.0.jar \
   /home/mkd/contenedores/mc-paper/plugins/MMORPGPlugin.jar
```

---

## 3️⃣ Reiniciar Servidor

```bash
cd /home/mkd/contenedores/mc-paper
docker-compose stop minecraft
docker-compose start minecraft
```

---

## 4️⃣ Verificar Logs

```bash
docker-compose logs -f minecraft | grep -E "(Inicializando|PathResolver|DataInitializer|✅|Error)"
```

**Buscar mensajes como:**
```
[INFO] Inicializando datos RPG para mundo: mmorpg
[INFO] ✅ Copiado npcs desde .example
[INFO] ✅ Copiado quests desde .example
```

---

## 5️⃣ Testing del Panel Web

### Test 1: GET /api/rpg/npcs

```bash
curl -X GET http://localhost:5000/api/rpg/npcs \
  -H "Content-Type: application/json" 2>/dev/null | jq .
```

**Resultado esperado:**
```json
{
  "success": true,
  "npcs_local": [...],
  "npcs_universal": [...]
}
```

### Test 2: GET /api/rpg/items

```bash
curl -X GET http://localhost:5000/api/rpg/items \
  -H "Content-Type: application/json" 2>/dev/null | jq .
```

### Test 3: GET /api/rpg/kills

```bash
curl -X GET http://localhost:5000/api/rpg/kills \
  -H "Content-Type: application/json" 2>/dev/null | jq .
```

---

## 6️⃣ Verificar Estructura de Archivos

```bash
# Ver estructura universal
ls -lah /home/mkd/contenedores/mc-paper/plugins/MMORPGPlugin/data/

# Ver estructura local (mundo "world")
ls -lah /home/mkd/contenedores/mc-paper/plugins/MMORPGPlugin/data/world/

# Verificar NO hay duplicados en data/
ls /home/mkd/contenedores/mc-paper/plugins/MMORPGPlugin/data/ | grep -E "config"
# ^ Debería estar VACÍO
```

---

## 7️⃣ Testing Crear Nuevo Mundo

```bash
# Crear mundo vía API
curl -X POST http://localhost:5000/api/worlds \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-rpg",
    "isRPG": true
  }' 2>/dev/null | jq .
```

**Esperar mensaje de éxito**

---

## 8️⃣ Cambiar a Nuevo Mundo

```bash
curl -X POST http://localhost:5000/api/worlds/test-rpg/activate \
  -H "Content-Type: application/json" 2>/dev/null | jq .
```

---

## 9️⃣ Verificar Datos Auto-Creados

```bash
# Después de cambiar al mundo, esperar logs de inicialización
docker-compose logs minecraft | grep -A5 "Inicializando datos RPG para mundo: test-rpg"

# Verificar archivos creados
ls -lah /home/mkd/contenedores/mc-paper/plugins/MMORPGPlugin/data/test-rpg/
```

**Archivos esperados:**
```
total 36K
drwxr-xr-x  test-rpg/
-rw-r--r--  npcs.json              (de .example)
-rw-r--r--  quests.json            (de .example)
-rw-r--r--  mobs.json              (de .example)
-rw-r--r--  players.json           (generado)
-rw-r--r--  status.json            (generado)
-rw-r--r--  invasions.json         (generado)
-rw-r--r--  kills.json             (generado)
-rw-r--r--  respawn.json           (generado)
-rw-r--r--  squads.json            (generado)
```

---

## 🔟 Checklist de Verificación

```
COMPILACIÓN:
- [ ] mvn clean package ejecutó sin errores
- [ ] JAR generado correctamente
- [ ] No hay warnings de compilación

PLUGIN:
- [ ] Plugin cargó en inicio
- [ ] Logs muestran "Inicializando datos RPG"
- [ ] Archivos se crearon automáticamente
- [ ] No hay errores en logs

PANEL WEB:
- [ ] GET /api/rpg/npcs retorna datos
- [ ] GET /api/rpg/items retorna datos
- [ ] GET /api/rpg/kills retorna datos
- [ ] Endpoints retornan status 200

FLUJO COMPLETO:
- [ ] Crear nuevo mundo funciona
- [ ] Archivos se auto-crean
- [ ] Panel web lee datos correctamente
- [ ] Estructura es consistente

LIMPIEZA:
- [ ] No hay archivos en data/ con "config" en nombre
- [ ] Todos los archivos están en ubicación correcta
- [ ] No hay duplicados
```

---

## 🐛 Troubleshooting

### Error: "PathResolver compila pero falta Gson"
```bash
# Verificar que pom.xml incluye Gson
grep -A2 "com.google.code.gson" mmorpg-plugin/pom.xml
```

### Error: "DataInitializer no puede crear archivos"
```bash
# Verificar permisos
ls -la plugins/MMORPGPlugin/
# Debe ser 755 o 775
chmod -R 755 plugins/MMORPGPlugin/
```

### Error: "Archivos no se crean automáticamente"
```bash
# Verificar que config/ .example files existen
ls config/plugin-data/*.example

# Si faltan, recrear
# Ver: /docs/ROADMAP_NORMALIZACION_ARCHIVOS.md Fase 1
```

### Error: "Panel web retorna 404"
```bash
# Verificar que endpoint está actualizado
grep "_get_data_location" web/app.py

# Si no está, revisar la actualización de Fase 3
```

---

## 📊 Verificación de Datos

### Contar NPCs universales
```bash
cat /home/mkd/contenedores/mc-paper/plugins/MMORPGPlugin/data/npcs.json | jq '.npcs | length'
```

### Contar NPCs locales (mundo "world")
```bash
cat /home/mkd/contenedores/mc-paper/plugins/MMORPGPlugin/data/world/npcs.json 2>/dev/null | jq '.npcs | length'
# Si no existe, mostrará error (esperado si no hay datos locales)
```

### Ver estructura de kill tracking
```bash
cat /home/mkd/contenedores/mc-paper/plugins/MMORPGPlugin/data/world/kills.json 2>/dev/null | jq 'keys'
```

---

## 📚 Documentos de Referencia

Para detalles completos, consultar:

1. **Visión General:** `/docs/ROADMAP_NORMALIZACION_ARCHIVOS.md`
2. **Implementación Java:** `/docs/FASE4_PLUGIN_JAVA.md`
3. **Plan de Limpieza:** `/docs/FASE5_LIMPIEZA_PLAN.md`
4. **Plan de Testing:** `/docs/FASE6_PRUEBAS.md`
5. **Resumen Final:** `/docs/RESUMEN_FINAL.md`

---

## ✨ Resultado Esperado Final

Cuando todo esté completado:

✅ **Plugin compila sin errores**  
✅ **PathResolver y DataInitializer funcionan**  
✅ **Datos se crean automáticamente**  
✅ **Panel web lee datos correctamente**  
✅ **Estructura es limpia y consistente**  
✅ **Sin duplicados o archivos huérfanos**  

---

## 🚀 Siguiente: Deployment a Producción

Si todo el testing es exitoso:

1. Crear tag de versión en git
2. Documentar cambios en CHANGELOG.md
3. Ejecutar test final en ambiente limpio
4. Deploy a producción

---

**Generado:** 9 de diciembre de 2025  
**Estado:** ✅ Listo para ejecución  
**Duración estimada:** 30-45 minutos para todo el proceso
