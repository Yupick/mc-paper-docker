# Fase 4: Actualización del Plugin Java

## Resumen de Cambios

Se han implementado dos nuevas clases de utilidad en el plugin Java:

### 1. `PathResolver.java`
**Propósito:** Centraliza la resolución de rutas de datos RPG según clasificación

**Características:**
- ✅ Resuelve rutas basadas en scope (local, universal, exclusive-local)
- ✅ Cache de level-name para evitar lecturas repetidas
- ✅ Obtiene automáticamente el level-name desde server.properties
- ✅ Retorna rutas pareadas (local + universal) para datos híbridos
- ✅ Métodos de validación y debug
- ✅ Clasifica automáticamente tipos de datos

**Uso Principal:**
```java
PathResolver resolver = plugin.getPathResolver();

// Obtener ruta de NPCs locales
File npcsLocal = resolver.resolvePath("mmorpg", "npcs", "local");

// Obtener ruta de items universales
File itemsUniversal = resolver.resolvePath("survival", "items", "universal");

// Obtener ruta de kills (exclusive-local)
File kills = resolver.resolvePath("mmorpg", "kills", "exclusive-local");
```

**Clasificación Automática:**
- **UNIVERSAL:** items, mobs_global, npcs_global, quests_global, enchantments_global, pets_global
- **HYBRID:** npcs, quests, mobs, pets, enchantments (busca local, fallback universal)
- **EXCLUSIVE-LOCAL:** players, status, invasions, kills, respawn, squads

### 2. `DataInitializer.java`
**Propósito:** Auto-inicializa archivos de datos faltantes

**Características:**
- ✅ Inicializa datos universales al activarse el plugin
- ✅ Inicializa datos locales para cada mundo RPG
- ✅ Intenta copiar desde archivos .example en config/
- ✅ Genera estructuras JSON por defecto si no hay .example
- ✅ Soporta todos los tipos de datos (11 tipos diferentes)
- ✅ Manejo robusto de errores

**Uso Principal:**
```java
DataInitializer init = plugin.getDataInitializer();

// Inicializar mundo completo
init.initializeWorldData("mmorpg");

// Crea automáticamente:
// - plugins/MMORPGPlugin/data/{level-name}/*.json
// - Copia desde config/plugin-data/ si existen .example
// - Genera por defecto si no existen .example
```

**Flujo de Inicialización:**
1. Detectar archivos .example en `config/plugin-data/`
2. Si existen, copiar a `plugins/MMORPGPlugin/data/`
3. Si no existen, generar estructura JSON por defecto
4. Crear directorio de mundo si no existe
5. Log de operaciones completadas

### 3. Integración en `MMORPGPlugin.java`

**Cambios:**
- ✅ Agregadas propiedades `pathResolver` y `dataInitializer`
- ✅ Inicializadas en `onEnable()` después de DataManager
- ✅ Agregados getters públicos para acceso desde otros managers
- ✅ Listo para usar en todos los managers (NPCManager, QuestManager, etc.)

**Integración:**
```java
// En cualquier manager
public class NPCManager {
    private PathResolver pathResolver;
    
    public void loadNPCs(String worldSlug) {
        File npcFile = plugin.getPathResolver().resolvePath(worldSlug, "npcs", "local");
        // Cargar desde npcFile...
    }
}
```

## Ventajas de esta Implementación

1. **Centralización:** Una única fuente de verdad para resolución de rutas
2. **Cache:** Evita lecturas repetidas de server.properties
3. **Escalabilidad:** Fácil agregar nuevos tipos de datos
4. **Auto-Inicialización:** No requiere configuración manual
5. **Compatibilidad:** Soporta archivos .example y generación por defecto
6. **Debug:** Método `getDebugInfo()` para troubleshooting

## Próximos Pasos

Para usar estas clases en otros managers:

1. **NPCManager:** Usar PathResolver para cargar/guardar NPCs locales
2. **QuestManager:** Usar PathResolver para quests locales
3. **MobManager:** Usar PathResolver para mobs locales
4. **Todos los managers:** Inicializar datos con DataInitializer

## Ejemplos de Uso

### Cargar datos locales del mundo activo
```java
String worldSlug = "mmorpg";
File npcPath = plugin.getPathResolver().resolvePath(worldSlug, "npcs", "local");

if (npcPath.exists()) {
    JsonObject data = JsonParser.parseReader(new FileReader(npcPath)).getAsJsonObject();
    JsonArray npcs = data.getAsJsonArray("npcs");
    // Procesar NPCs...
}
```

### Auto-inicializar mundo nuevo
```java
String newWorldSlug = "nuevo-mundo";
plugin.getDataInitializer().initializeWorldData(newWorldSlug);
// Todos los archivos necesarios se crean automáticamente
```

### Debug de rutas
```java
String debugInfo = plugin.getPathResolver().getDebugInfo("mmorpg");
plugin.getLogger().info(debugInfo);
```

## Estructura de Archivos Resultante

```
plugins/MMORPGPlugin/
├── data/
│   ├── items.json              # Universal
│   ├── mobs.json               # Universal
│   ├── npcs.json               # Universal
│   ├── quests.json             # Universal
│   ├── enchantments.json       # Universal
│   ├── pets.json               # Universal
│   ├── mmorpg/                 # Mundo "mmorpg"
│   │   ├── npcs.json           # Local
│   │   ├── quests.json         # Local
│   │   ├── mobs.json           # Local
│   │   ├── players.json        # Exclusive-Local
│   │   ├── status.json         # Exclusive-Local
│   │   ├── invasions.json      # Exclusive-Local
│   │   ├── kills.json          # Exclusive-Local
│   │   ├── respawn.json        # Exclusive-Local
│   │   └── squads.json         # Exclusive-Local
│   └── survival/               # Mundo "survival"
│       └── [misma estructura]
```

## Verificación Post-Instalación

```
✅ PathResolver.java creado
✅ DataInitializer.java creado
✅ MMORPGPlugin.java actualizado
✅ Getters públicos agregados
✅ Integración completa lista
```
