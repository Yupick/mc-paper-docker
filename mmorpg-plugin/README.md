# MMORPG Plugin - Fase 1

Plugin de Minecraft Paper 1.21.1 que añade características MMORPG a mundos específicos.

## 📋 Descripción

Este plugin permite activar el modo RPG en mundos individuales, añadiendo características como:

- ✅ Sistema de clases (Fase 1: Básico)
- ✅ Sistema de quests (Fase 1: Básico)
- ✅ NPCs (Fase 1: Básico)
- ✅ Economía (Fase 1: Básico)

## 🏗️ Estructura del Proyecto

```
mmorpg-plugin/                    # Código fuente (fuera de plugins/)
├── src/
│   └── main/
│       ├── java/
│       │   └── com/nightslayer/mmorpg/
│       │       ├── MMORPGPlugin.java       # Clase principal
│       │       ├── RPGCommand.java         # Comandos /rpg
│       │       ├── WorldMetadata.java      # POJO para metadata
│       │       ├── WorldRPGManager.java    # Gestión de mundos RPG
│       │       └── DataManager.java        # Persistencia de datos
│       └── resources/
│           ├── plugin.yml                  # Descriptor del plugin
│           └── config.yml                  # Configuración
├── pom.xml                                 # Maven build
└── target/                                 # Salida de compilación
    └── MMORPGPlugin.jar                   # JAR compilado

plugins/                                    # Destino del plugin
└── MMORPGPlugin.jar                       # JAR copiado (se despliega aquí)
    └── data/                              # Datos generados por el plugin
        └── {world-slug}/
            ├── status.json                # Estado del mundo RPG
            └── players.json               # Datos de jugadores
```

## 🚀 Compilación

### Requisitos

- Java 17 o superior
- Maven 3.6+
- Docker con contenedor PaperMC corriendo

### Compilar y desplegar

```bash
# Desde la raíz del proyecto mc-paper
./scripts/build-mmorpg-plugin.sh
```

Este script:
1. Compila el plugin con Maven
2. Copia el JAR a `plugins/MMORPGPlugin.jar`
3. Muestra mensaje para reiniciar el servidor

### Compilación manual

```bash
cd mmorpg-plugin
mvn clean package
cp target/MMORPGPlugin.jar ../plugins/MMORPGPlugin.jar
```

## 🎮 Uso

### Activar RPG en un mundo

1. En el panel web, ir a **Mundos** → **Crear Mundo**
2. Marcar la opción "Activar modo MMORPG"
3. Seleccionar las características RPG deseadas
4. Crear el mundo

### Comandos disponibles

- `/rpg help` - Muestra ayuda
- `/rpg status` - Muestra tu estado RPG
- `/rpg info` - Información del mundo RPG actual

### Verificar que el plugin funciona

1. Iniciar el servidor: `docker-compose up -d`
2. Ver logs: `docker-compose logs -f`
3. Buscar: `[MMORPGPlugin] MMORPGPlugin habilitado correctamente!`
4. Buscar: `Detectados X mundos con modo RPG activado`

## 📊 Integración con Panel Web

El plugin sincroniza datos con el panel web cada 30 segundos (configurable):

- `/server/plugins/MMORPGPlugin/data/{world-slug}/status.json` - Estado del mundo
- `/server/plugins/MMORPGPlugin/data/{world-slug}/players.json` - Datos de jugadores

### Endpoints API

- `GET /api/worlds/<slug>/rpg/status` - Estado RPG del mundo
- `GET /api/worlds/<slug>/rpg/players` - Jugadores RPG
- `GET /api/worlds/<slug>/rpg/summary` - Resumen completo
- `GET /api/rpg/worlds` - Lista de mundos con RPG activado

### Pestaña RPG

El panel web muestra automáticamente una pestaña "RPG" cuando el mundo activo tiene `isRPG: true`.

## 🔧 Configuración

### config.yml (plugin)

```yaml
plugin:
  debug: false  # Modo debug (logs adicionales)

worlds:
  base-path: /server/worlds  # Ruta base de mundos

web-panel:
  enabled: true          # Sincronización con panel web
  sync-interval: 30      # Intervalo de sincronización (segundos)
```

### metadata.json (por mundo)

```json
{
  "name": "Mundo Aventura",
  "slug": "mundo-aventura",
  "isRPG": true,
  "rpgConfig": {
    "classesEnabled": true,
    "questsEnabled": true,
    "npcsEnabled": true,
    "economyEnabled": true
  }
}
```

## 🗺️ Roadmap

### ✅ Fase 1: Base (Actual)
- [x] Detección de mundos RPG
- [x] Comandos básicos
- [x] Sincronización con panel web
- [x] UI en panel web

### 🔄 Fase 2: Clases, NPCs y Quests
- [ ] Sistema de clases completo (Guerrero, Mago, Arquero)
- [ ] NPCs con diálogos
- [ ] Sistema de quests funcional
- [ ] Inventario de quest items

### 🔄 Fase 3: Economía y Skills
- [ ] Economía con monedas
- [ ] Sistema de skills y niveles
- [ ] Tiendas y comercio

### 🔄 Fase 4: Pulido
- [ ] Partículas y efectos
- [ ] Sonidos personalizados
- [ ] Optimizaciones
- [ ] Testing completo

## 📝 Notas Técnicas

- **Paper API 1.21.1**: Compatible con Minecraft 1.21.1
- **Gson**: Para serialización JSON
- **Maven Shade**: Para empaquetar dependencias
- **Async sync**: La sincronización con panel web corre en thread asíncrono

## 🐛 Debugging

### Ver logs del plugin

```bash
docker-compose logs -f | grep MMORPG
```

### Activar debug

En `plugins/MMORPGPlugin/config.yml`:

```yaml
plugin:
  debug: true
```

### Verificar mundos detectados

```bash
docker-compose logs | grep "Mundo RPG detectado"
```

## 🤝 Contribución

Este plugin es parte del sistema multi-mundo de PaperMC con panel web.

Ver `ROADMAP_MMORPG.md` para detalles completos de las fases de desarrollo.
