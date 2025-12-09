# MMORPG Plugin - Desarrollo Activo 🚀

Plugin de Minecraft Paper 1.21.1 que añade características MMORPG completas.

## 📊 Progreso del Desarrollo

### ✅ FASE 1: SISTEMAS FUNDAMENTALES (100%)
- ✅ Módulo 1.1: Sistema de Clases y Habilidades
- ✅ Módulo 1.2: Sistema de Niveles y Progresión
- ✅ Módulo 1.3: Sistema de Estadísticas
- ✅ Módulo 1.4: Sistema de Quests
- ✅ Módulo 1.5: NPCs y Diálogos

### ✅ FASE 2: CONTENIDO Y EVENTOS (100%)
- ✅ Módulo 2.1: Sistema de Invasiones
- ✅ Módulo 2.2: Sistema de Eventos Mundiales
- ✅ Módulo 2.3: Sistema de Mazmorras
- ✅ Módulo 2.4: Sistema de Escuadras

### 🔄 FASE 3: ECONOMÍA Y CRAFTEO (40%)
- ✅ Módulo 3.1: Sistema de Crafteo (COMPLETADO)
- ✅ Módulo 3.2: Encantamientos Personalizados (COMPLETADO) ✨
- ⏳ Módulo 3.3: Mascotas y Monturas (SIGUIENTE)
- ⏳ Módulo 3.4: Dungeons Procedurales
- ⏳ Módulo 3.5: Integración Discord

**Último módulo completado**: Encantamientos Personalizados (12 encantamientos únicos, 4 estaciones, panel web completo)

## 📋 Descripción

Este plugin añade un sistema MMORPG completo con:

- ✅ **9 Clases Jugables**: Guerrero, Mago, Arquero, Asesino, Paladín, Nigromante, Druida, Monje, Berserker
- ✅ **Sistema de Habilidades**: 5 habilidades únicas por clase
- ✅ **Sistema de Niveles**: Progresión hasta nivel 100 con curva exponencial
- ✅ **Estadísticas RPG**: STR, AGI, INT, VIT, DEX, LUK
- ✅ **Sistema de Quests**: 50+ misiones con recompensas
- ✅ **NPCs Interactivos**: Sistema de diálogos y comercio
- ✅ **Invasiones**: Oleadas de enemigos con recompensas
- ✅ **Eventos Mundiales**: Boss raids, tesoros, meteoritos
- ✅ **Mazmorras**: Instancias con dificultades
- ✅ **Escuadras**: Sistema de guilds con niveles
- ✅ **Crafteo Avanzado**: 15 recetas RPG con estaciones especializadas
- ✅ **Encantamientos**: 12 encantamientos únicos en 4 niveles de rareza
- ✅ **Panel Web**: Interfaz completa con 45+ endpoints API
- ✅ **Economía**: Sistema de monedas y comercio

## 🎯 Características del Módulo 3.2: Encantamientos

### 12 Encantamientos Únicos
- **UNCOMMON (2)**: Experience Boost, Coin Finder
- **RARE (4)**: Flame Burst, Frost Touch, Venom Strike, Shield Bash
- **EPIC (4)**: Life Steal, Auto-Repair, Critical Master, Thorns Aura
- **LEGENDARY (2)**: Thunder Strike, Soul Bound

### 4 Estaciones de Encantamiento
- **Altar Básico**: Hasta UNCOMMON (90% éxito)
- **Altar Avanzado**: Hasta RARE (80% éxito)
- **Altar Maestro**: Hasta EPIC (70% éxito)
- **Altar Legendario**: Todos los niveles (60% éxito)

### Sistema de Mecánicas
- Tasa de éxito dinámica según rareza
- Máximo 3 encantamientos por item
- Costos en monedas y XP escalables
- Sistema de incompatibilidades
- Panel web con tema mágico (púrpura/dorado)

Ver documentación completa: `docs/MODULO_3_2_ENCHANTING_COMPLETADO.md`

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
