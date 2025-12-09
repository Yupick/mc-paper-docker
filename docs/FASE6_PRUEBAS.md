# Fase 6: Pruebas End-to-End

## Plan de Testing Comprehensive

### 1. Testing de PathResolver (Java)

**Objetivo:** Verificar que las rutas se resuelven correctamente

```bash
# Compilar el plugin con las nuevas clases
cd /home/mkd/contenedores/mc-paper/mmorpg-plugin
mvn clean package
```

**Esperar:**
- ✅ Compilación exitosa sin errores
- ✅ JAR generado en `target/mmorpg-plugin-1.0.0.jar`

### 2. Testing de DataInitializer (Java)

**Objetivo:** Verificar que se crean automáticamente los archivos de datos

**Manual Test:**
```bash
# Después de compilar, copiamos el JAR
cp mmorpg-plugin/target/mmorpg-plugin-1.0.0.jar plugins/

# Iniciamos el servidor (Docker)
docker-compose up -d
docker-compose logs -f minecraft

# Esperamos a ver el mensaje:
# "Inicializando datos RPG para mundo: mmorpg"
# "✅ Copiado npcs desde .example"
# "✅ Copiado quests desde .example"
```

**Verificación post-inicio:**
```bash
# Comprobar que se crearon automáticamente
ls -la plugins/MMORPGPlugin/data/
ls -la plugins/MMORPGPlugin/data/mmorpg/
```

### 3. Testing de Panel Web - Endpoint GET /api/rpg/npcs

**Objetivo:** Verificar que el panel web lee correctamente los datos con la nueva función

**Request:**
```bash
curl -X GET http://localhost:5000/api/rpg/npcs \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json"
```

**Respuesta esperada:**
```json
{
  "success": true,
  "npcs_local": [...],
  "npcs_universal": [...]
}
```

**Verificación:**
- ✅ Response status 200
- ✅ Arrays de NPCs no están vacíos
- ✅ Datos son válidos JSON

### 4. Testing de Panel Web - Endpoint GET /api/rpg/quests

**Objetivo:** Similar a NPCs, verificar separación local/universal

**Request:**
```bash
curl -X GET http://localhost:5000/api/rpg/quests \
  -H "Authorization: Bearer TOKEN"
```

**Verificación:**
- ✅ Quests locales y universales se retornan correctamente
- ✅ Las quests del .example se cargan correctamente

### 5. Testing de Panel Web - Endpoint GET /api/rpg/items

**Objetivo:** Verificar que items (always universal) se retornan correctamente

**Verificación:**
- ✅ Solo retorna items universales
- ✅ No hay items locales (items.json no en world/)
- ✅ Estructura con rarities si existe

### 6. Testing de Panel Web - Endpoint GET /api/rpg/kills

**Objetivo:** Verificar que kills (exclusive-local) se retornan del mundo correcto

**Request:**
```bash
curl -X GET http://localhost:5000/api/rpg/kills \
  -H "Authorization: Bearer TOKEN"
```

**Verificación:**
- ✅ Status 200
- ✅ Estructura: { kills: [], playerStats: {} }
- ✅ Datos local al mundo actual

### 7. Testing: Crear Nuevo Mundo RPG

**Objetivo:** Verificar que se inicializa correctamente con la nueva estructura

**Script:**
```bash
# 1. Crear mundo vía API
curl -X POST http://localhost:5000/api/worlds \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-mundo",
    "isRPG": true,
    "rpgConfig": {
      "level": 1
    }
  }'

# 2. Esperar respuesta exitosa
# 3. Cambiar a ese mundo
curl -X POST http://localhost:5000/api/worlds/test-mundo/activate \
  -H "Authorization: Bearer TOKEN"

# 4. Reiniciar servidor
docker-compose restart minecraft

# 5. Esperar inicialización
docker-compose logs minecraft | grep "Inicializando"

# 6. Verificar que se crearon archivos
ls -la plugins/MMORPGPlugin/data/test-mundo/
```

**Archivos esperados:**
```
plugins/MMORPGPlugin/data/test-mundo/
├── npcs.json              # Copiado de .example
├── quests.json            # Copiado de .example
├── mobs.json              # Copiado de .example
├── pets.json              # Copiado de .example
├── enchantments.json      # Copiado de .example
├── players.json           # Generado por defecto
├── status.json            # Generado por defecto
├── invasions.json         # Generado por defecto
├── kills.json             # Generado por defecto
├── respawn.json           # Generado por defecto
└── squads.json            # Generado por defecto
```

### 8. Testing: Panel Web lee datos del nuevo mundo

**Objetivo:** Verificar que el panel web puede leer datos del mundo recién creado

**Verificación:**
```bash
# Cambiar a test-mundo y verificar
# GET /api/rpg/npcs debe retornar datos del nuevo mundo
```

### 9. Testing: Crear NPC en mundo y verificar ubicación

**Objetivo:** Verifica que los datos se guardan en la ubicación correcta

**Paso a paso:**
1. Ir al panel web
2. Crear nuevo NPC en mundo "mmorpg"
3. Guardar
4. Verificar que el archivo se guardó en `plugins/MMORPGPlugin/data/mmorpg/npcs.json`

```bash
# Verificar guardar
cat plugins/MMORPGPlugin/data/mmorpg/npcs.json | jq '.npcs | length'
```

### 10. Testing: Verificar NO hay archivos en ubicación vieja

**Objetivo:** Confirmar que no hay datos huérfanos en ubicaciones obsoletas

```bash
# No debe haber datos en worlds/{mundo}/data/
find /home/mkd/contenedores/mc-paper/worlds -name "*.json" -path "*data*" -type f

# No debe haber conflictos
ls plugins/MMORPGPlugin/data/invasions_config.json 2>/dev/null || echo "✅ invasions_config.json no en data/"
```

### 11. Testing: Invasiones (Exclusive-Local)

**Objetivo:** Verificar que invasiones se guardan por mundo

```bash
# Crear invasión en mundo "mmorpg"
# GET /api/rpg/worlds/mmorpg/invasions
# Verificar que está en data/mmorpg/invasions.json
# NO debe estar en data/invasions.json (universal)
```

### 12. Testing: Kills Tracking (Exclusive-Local)

**Objetivo:** Similar a invasiones, kills por mundo

```bash
# Simular kill en plugin (si es posible vía API o comando)
# Verificar que se registró en data/mmorpg/kills.json
# NO debe estar en data/kills.json
```

### 13. Testing: Cambiar entre mundos

**Objetivo:** Verificar que los datos se cargan del mundo correcto al cambiar

**Paso a paso:**
1. Estar en mundo "mmorpg"
2. GET /api/rpg/npcs → debe retornar NPCs de mmorpg
3. Cambiar a mundo "test-mundo" vía /api/worlds/test-mundo/activate
4. GET /api/rpg/npcs → debe retornar NPCs de test-mundo
5. Cambiar de vuelta a "mmorpg"
6. GET /api/rpg/npcs → debe retornar NPCs de mmorpg de nuevo

### 14. Testing: Compatibilidad hacia atrás

**Objetivo:** Verificar que datos existentes no se rompen

```bash
# Si hay mundos existentes con datos viejos:
# 1. No deben generar errores
# 2. Deben ser leídos correctamente
# 3. Nuevos datos se guardan en ubicación correcta
```

### 15. Testing: Performance

**Objetivo:** Verificar que las nuevas funciones no causan lag

**Métricas:**
```bash
# Tiempo de respuesta en endpoints RPG
# GET /api/rpg/npcs → debe ser < 200ms
# GET /api/rpg/kills → debe ser < 200ms
# GET /api/rpg/items → debe ser < 100ms
```

## Checklist de Testing

```
TESTING JAVA:
- [ ] PathResolver compila sin errores
- [ ] DataInitializer compila sin errores
- [ ] Plugin compila correctamente
- [ ] No hay warnings de compilación

TESTING PLUGIN:
- [ ] Plugin se carga en inicio
- [ ] PathResolver se inicializa
- [ ] DataInitializer se ejecuta
- [ ] Archivos de datos se crean automáticamente
- [ ] Logs muestran operaciones correctamente

TESTING PANEL WEB:
- [ ] GET /api/rpg/npcs retorna datos correctos
- [ ] GET /api/rpg/quests retorna datos correctos
- [ ] GET /api/rpg/mobs retorna datos correctos
- [ ] GET /api/rpg/items retorna datos correctos
- [ ] GET /api/rpg/kills retorna datos correctos
- [ ] Separación local/universal funciona

TESTING FLUJO COMPLETO:
- [ ] Crear nuevo mundo RPG
- [ ] Datos se inicializan automáticamente
- [ ] Panel web lee datos correctamente
- [ ] Crear NPC se guarda en ubicación correcta
- [ ] Cambiar mundos carga datos correctos
- [ ] Invasiones en ubicación exclusive-local
- [ ] Kills en ubicación exclusive-local

TESTING LIMPIEZA:
- [ ] No hay duplicados
- [ ] No hay archivos huérfanos
- [ ] Estructura es consistente
- [ ] Scripts instalan correctamente
```

## Resultado Esperado

✅ **Normalización completa:** Todos los datos están organizados correctamente  
✅ **Resolución de rutas centralizada:** Panel web y plugin usan misma lógica  
✅ **Auto-inicialización:** Nuevos mundos se crean automáticamente  
✅ **Sin duplicados:** Estructura limpia y consistente  
✅ **Backward compatible:** Datos existentes no se rompen  
✅ **Performance:** Sin degradación de rendimiento  

## Siguientes Pasos si Testing es Exitoso

1. ✅ Documentar los cambios en CHANGELOG.md
2. ✅ Crear tag de versión en git
3. ✅ Ejecutar `create.sh` en ambiente limpio para verificar
4. ✅ Verificar instalación rápida con `quick-install.sh`
5. ✅ Deployment a producción

## Rollback Plan (si algo falla)

Si algún test falla:
1. Identificar el componente que falló
2. Revisar logs en detail
3. Si es crítico:
   - `git checkout` los archivos problemáticos
   - Restaurar desde backup de plugins/
   - Reiniciar y reverificar

## Documentación Generada

- ✅ `/docs/ROADMAP_NORMALIZACION_ARCHIVOS.md` - Guía principal
- ✅ `/docs/FASE4_PLUGIN_JAVA.md` - Detalles de implementación Java
- ✅ `/docs/FASE5_LIMPIEZA_PLAN.md` - Plan de limpieza ejecutado
- ✅ `/docs/FASE6_PRUEBAS.md` - Este documento
