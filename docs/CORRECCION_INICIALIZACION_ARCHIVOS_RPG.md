# Corrección: Inicialización Automática de Archivos RPG

## Problema Identificado

Cuando se ejecutaba `create.sh` para crear el servidor:
1. El plugin se compilaba correctamente
2. El servidor se iniciaba sin errores
3. **PERO: Los archivos de configuración RPG no se creaban**

Esto causaba que:
- El panel web mostrara el menú RPG vacío (sin opciones)
- No existieran los archivos JSON necesarios en `plugins/MMORPGPlugin/data/`
- No hubiera archivos en las carpetas de los mundos RPG

## Causa Raíz

El plugin tenía la clase `DataInitializer.java` con el método `initializeWorldData()` que podía crear todos los archivos necesarios, **pero este método nunca se ejecutaba**.

El `DataInitializer` se instanciaba en el `onEnable()` del plugin, pero no se llamaba a ninguno de sus métodos de inicialización.

## Solución Implementada

### 1. Modificación en `MMORPGPlugin.java`

**Inicialización de datos universales al inicio:**
```java
// Inicializar resolvedores de rutas
pathResolver = new PathResolver(this);
dataInitializer = new DataInitializer(this, pathResolver);

// Inicializar datos universales (items, mobs globales, etc.)
getLogger().info("Inicializando archivos de configuración RPG globales...");
dataInitializer.initializeWorldData("_universal_");
```

**Inicialización de datos por mundo detectado:**
```java
if (metadata != null && metadata.isRPG()) {
    worldRPGManager.registerRPGWorld(worldFolder.getName(), metadata);
    rpgWorldsCount++;
    
    // Inicializar archivos de datos del mundo RPG
    dataInitializer.initializeWorldData(worldFolder.getName());
    
    if (getConfig().getBoolean("plugin.debug", false)) {
        getLogger().info("Mundo RPG detectado: " + worldFolder.getName());
    }
}
```

### 2. Modificación en `DataInitializer.java`

Soporte para inicializar solo datos universales:
```java
public void initializeWorldData(String worldSlug) {
    if ("_universal_".equals(worldSlug)) {
        plugin.getLogger().info("Inicializando datos RPG universales (globales)...");
        initializeUniversalData();
        return;
    }
    
    plugin.getLogger().info("Inicializando datos RPG para mundo: " + worldSlug);
    
    // Datos universal (solo si no existen)
    initializeUniversalData();
    
    // Datos locales del mundo
    initializeWorldDataFiles(worldSlug);
    
    plugin.getLogger().info("Datos RPG inicializados para: " + worldSlug);
}
```

## Archivos que Ahora se Crean Automáticamente

### Archivos Universales (en `plugins/MMORPGPlugin/data/`)
- `items.json` - Ítems globales
- `mobs.json` - Mobs globales
- `npcs.json` - NPCs globales
- `quests.json` - Misiones globales
- `enchantments.json` - Encantamientos globales
- `pets.json` - Mascotas globales

### Archivos Locales por Mundo (en `plugins/MMORPGPlugin/data/<mundo>/`)
- `npcs.json` - NPCs específicos del mundo
- `quests.json` - Misiones específicas del mundo
- `mobs.json` - Mobs específicos del mundo
- `pets.json` - Mascotas específicas del mundo
- `enchantments.json` - Encantamientos específicos del mundo
- `players.json` - Datos de jugadores
- `status.json` - Estado del mundo RPG
- `invasions.json` - Invasiones activas
- `kills.json` - Estadísticas de muertes
- `respawn.json` - Configuración de respawn
- `squads.json` - Escuadrones/grupos

## Flujo de Inicialización

1. **Al iniciar el servidor:**
   - Se crea la carpeta `plugins/MMORPGPlugin/data/`
   - Se inicializan archivos universales (si no existen)

2. **Al detectar mundos RPG:**
   - Se lee `worlds/<mundo>/metadata.json`
   - Si `isRPG: true`, se registra el mundo
   - Se inicializan todos los archivos locales del mundo

3. **Creación de archivos:**
   - Primero intenta copiar desde `.example` si existe
   - Si no hay ejemplo, genera estructura JSON por defecto
   - Solo crea archivos que no existen (no sobrescribe)

## Beneficios

✅ El panel web ahora puede cargar opciones RPG inmediatamente  
✅ Los archivos se crean con estructura JSON válida  
✅ No se requiere intervención manual  
✅ Los ejemplos personalizados se respetan si existen  
✅ Funciona tanto en instalación nueva como en reinicio  

## Testing

Para probar la corrección:
```bash
# 1. Reconstruir el plugin
./scripts/build-mmorpg-plugin.sh

# 2. Reiniciar el servidor
./stop.sh
./run.sh

# 3. Verificar archivos creados
ls -la plugins/MMORPGPlugin/data/
ls -la plugins/MMORPGPlugin/data/world/  # (si 'world' es RPG)

# 4. Verificar panel web
# Acceder a http://localhost:5000 y navegar a la sección RPG
```

## Archivos Modificados

- `mmorpg-plugin/src/main/java/com/nightslayer/mmorpg/MMORPGPlugin.java`
- `mmorpg-plugin/src/main/java/com/nightslayer/mmorpg/DataInitializer.java`

## Fecha de Corrección

17 de diciembre de 2025
