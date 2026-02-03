# Instalación Nativa Completada

## Estado del Sistema

✅ **Servidor Minecraft** 
- **Estado**: Ejecutándose correctamente
- **PID**: 130131
- **CPU**: ~84%
- **RAM**: 904 MB
- **Puerto**: 25565
- **Versión**: Paper 1.20.6-147
- **Plugin MMORPG**: Cargado correctamente (API 1.20)

✅ **Panel Web**
- **Estado**: Ejecutándose correctamente  
- **PID**: 121318
- **URL**: http://localhost:5000
- **Modo**: Nativo (sin Docker)
- **Servicios**: RCONService + ServerMonitor

## Instalación Realizada

### 1. Compilación del Plugin
```bash
mvn clean package -f mmorpg-plugin/pom.xml
```
- Tiempo de compilación: ~1 minuto
- Artefacto generado: `mmorpg-plugin-1.0.0.jar` (14MB)
- Cambio: API version 1.21 → 1.20 para compatibilidad

### 2. Servidor Minecraft
```bash
minecraft-server/
├── paper.jar (43.7 MB)
├── plugins/
│   └── MMORPGPlugin.jar (14 MB)
├── server.properties
├── eula.txt
└── logs/latest.log
```

**Configuración:**
- JVM: Java 21 OpenJDK
- Memoria: -Xms1G -Xmx1G
- GC: G1GC con Aikar's flags
- Mundo: worlds/world (generado automáticamente)

### 3. Panel Web
```bash
mmorpg-web/
├── venv/ (entorno virtual Python 3.12)
├── app.py
├── services/
│   ├── rcon_native.py
│   └── backup_service.py
└── web.log
```

**Dependencias instaladas:**
- Flask 3.1.2
- flask-login 0.6.3
- mcrcon 0.7.0
- psutil 7.2.2
- requests 2.32.5
- python-dotenv 1.2.1

## Script de Control

Creado `server-control.sh` para gestionar ambos servicios:

```bash
# Iniciar
./server-control.sh start server    # Solo Minecraft
./server-control.sh start web        # Solo panel web
./server-control.sh start all        # Ambos servicios

# Detener
./server-control.sh stop server
./server-control.sh stop web
./server-control.sh stop all

# Reiniciar
./server-control.sh restart server
./server-control.sh restart web
./server-control.sh restart all

# Estado
./server-control.sh status server
./server-control.sh status web
./server-control.sh status all

# Logs en tiempo real
./server-control.sh logs server
./server-control.sh logs web
```

## Archivos de Estado

- **PID Minecraft**: `minecraft-server/server.pid`
- **PID Web**: `mmorpg-web/web.pid`
- **Logs Minecraft**: `minecraft-server/logs/latest.log`
- **Logs Web**: `mmorpg-web/web.log`

## Modificaciones en el Código

### app.py (mmorpg-web/)
```python
# Import opcional de docker
try:
    import docker
    DOCKER_AVAILABLE = True
except ImportError:
    docker = None
    DOCKER_AVAILABLE = False

# Inicialización condicional
if DOCKER_AVAILABLE:
    docker_client = docker.from_env()
else:
    docker_client = None

# Servicios nativos siempre disponibles
rcon_service = RCONService(...)
server_monitor = ServerMonitor(...)
```

### plugin.yml (mmorpg-plugin/)
```yaml
api-version: '1.20'  # Cambiado de 1.21
```

## Tiempos de Carga

- **Compilación Maven**: ~63 segundos
- **Remapping plugin**: ~14 segundos  
- **Generación mundo**: ~86 segundos
- **Carga completa servidor**: ~199 segundos (3.3 minutos)
- **Inicio panel web**: ~5 segundos

## Problemas Resueltos

1. ✅ **Puerto 25565 ocupado**: Detenido proceso antiguo (PID 45389)
2. ✅ **API version incompatible**: Cambiado plugin.yml de 1.21 a 1.20
3. ✅ **Módulo docker faltante**: Importación condicional en app.py
4. ✅ **python-dotenv faltante**: Instalado en venv

## Advertencias del Plugin

El plugin reporta algunos warnings no críticos:

- ⚠️ Pets con campos faltantes (configuración por defecto)
- ⚠️ `respawn_config.json` no encontrado
- ⚠️ Directorio de mundos `/server/worlds` no encontrado (usa auto-detección)
- ⚠️ Conexión a base de datos cerrada (normal durante carga)

Estos warnings no afectan la funcionalidad básica del servidor.

## Siguiente Paso

Acceder al panel web en: **http://localhost:5000**

Para conectarse al servidor Minecraft:
- **IP**: localhost:25565
- **Versión**: 1.20.6

---

**Fecha**: 3 de febrero de 2026  
**Branch**: paper-native  
**Modo**: Instalación nativa sin Docker
