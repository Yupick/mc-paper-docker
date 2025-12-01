#!/bin/bash

echo "========================================="
echo "Creando servidor Minecraft Paper"
echo "========================================="

echo ""
echo "[1/5] Creando directorios necesarios..."
mkdir -p worlds plugins resourcepacks config logs backups/worlds

# Crear archivos de configuración del panel si no existen
if [ ! -f config/backup_config.json ]; then
    cat > config/backup_config.json << 'EOF'
{
  "auto_backup_enabled": true,
  "retention_count": 5
}
EOF
    echo "✅ Configuración de backups creada"
fi

if [ ! -f config/panel_config.json ]; then
    cat > config/panel_config.json << 'EOF'
{
  "refresh_interval": 5000,
  "logs_interval": 10000,
  "tps_interval": 10000,
  "pause_when_hidden": true,
  "enable_cache": true,
  "cache_ttl": 3000
}
EOF
    echo "✅ Configuración del panel creada"
fi

# Crear server.properties por defecto si no existe
if [ ! -f config/server.properties ]; then
    cat > config/server.properties << 'EOF'
#Minecraft server properties
enable-jmx-monitoring=false
rcon.port=25575
level-seed=
gamemode=survival
enable-command-block=false
enable-query=false
enable-rcon=true
generator-settings={}
enforce-secure-profile=true
level-name=worlds/world
motd=A Minecraft Server - Java & Bedrock
query.port=25565
pvp=true
generate-structures=true
max-chained-neighbor-updates=1000000
difficulty=easy
network-compression-threshold=256
max-tick-time=60000
require-resource-pack=false
use-native-transport=true
max-players=20
online-mode=true
enable-status=true
allow-flight=false
initial-enabled-packs=vanilla
broadcast-rcon-to-ops=true
view-distance=10
server-ip=
resource-pack-prompt=
allow-nether=true
server-port=25565
enable-rcon=false
sync-chunk-writes=true
op-permission-level=4
prevent-proxy-connections=false
hide-online-players=false
resource-pack=
entity-broadcast-range-percentage=100
simulation-distance=10
rcon.password=minecraft123
player-idle-timeout=0
force-gamemode=false
rate-limit=0
hardcore=false
white-list=false
broadcast-console-to-ops=true
spawn-npcs=true
spawn-animals=true
function-permission-level=2
initial-disabled-packs=
level-type=minecraft\:normal
text-filtering-config=
spawn-monsters=true
enforce-whitelist=false
spawn-protection=16
resource-pack-sha1=
max-world-size=29999984
EOF
    echo "✅ Archivo server.properties creado en config/"
fi

echo ""
echo "[2/5] Descargando plugins..."
echo "Descargando últimas versiones de: GeyserMC, Floodgate, ViaVersion, ViaBackwards, ViaRewind"

# Descargar GeyserMC
echo "  → Descargando GeyserMC..."
curl -L -o plugins/Geyser-Spigot.jar "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot" 2>/dev/null
if [ -f plugins/Geyser-Spigot.jar ]; then
    GEYSER_SIZE=$(ls -lh plugins/Geyser-Spigot.jar 2>/dev/null | awk '{print $5}')
    if [ "$GEYSER_SIZE" != "31" ]; then
        echo "    ✅ GeyserMC descargado ($GEYSER_SIZE)"
    else
        echo "    ❌ Error descargando GeyserMC (archivo corrupto)"
        rm -f plugins/Geyser-Spigot.jar
    fi
fi

# Descargar Floodgate
echo "  → Descargando Floodgate..."
curl -L -o plugins/floodgate-spigot.jar "https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot" 2>/dev/null
if [ -f plugins/floodgate-spigot.jar ]; then
    FLOODGATE_SIZE=$(ls -lh plugins/floodgate-spigot.jar 2>/dev/null | awk '{print $5}')
    if [ "$FLOODGATE_SIZE" != "31" ]; then
        echo "    ✅ Floodgate descargado ($FLOODGATE_SIZE)"
    else
        echo "    ❌ Error descargando Floodgate (archivo corrupto)"
        rm -f plugins/floodgate-spigot.jar
    fi
fi

# Descargar ViaVersion
echo "  → Descargando ViaVersion..."
VIAVERSION_URL=$(curl -s https://api.github.com/repos/ViaVersion/ViaVersion/releases/latest | grep "browser_download_url.*ViaVersion-.*\.jar" | cut -d '"' -f 4)
curl -L -o plugins/ViaVersion.jar "$VIAVERSION_URL" 2>/dev/null
echo "    ✅ ViaVersion descargado ($(ls -lh plugins/ViaVersion.jar 2>/dev/null | awk '{print $5}'))"

# Descargar ViaBackwards
echo "  → Descargando ViaBackwards..."
VIABACKWARDS_URL=$(curl -s https://api.github.com/repos/ViaVersion/ViaBackwards/releases/latest | grep "browser_download_url.*ViaBackwards-.*\.jar" | cut -d '"' -f 4)
curl -L -o plugins/ViaBackwards.jar "$VIABACKWARDS_URL" 2>/dev/null
echo "    ✅ ViaBackwards descargado ($(ls -lh plugins/ViaBackwards.jar 2>/dev/null | awk '{print $5}'))"

# Descargar ViaRewind
echo "  → Descargando ViaRewind..."
VIAREWIND_URL=$(curl -s https://api.github.com/repos/ViaVersion/ViaRewind/releases/latest | grep "browser_download_url.*ViaRewind-.*\.jar" | cut -d '"' -f 4)
curl -L -o plugins/ViaRewind.jar "$VIAREWIND_URL" 2>/dev/null
echo "    ✅ ViaRewind descargado ($(ls -lh plugins/ViaRewind.jar 2>/dev/null | awk '{print $5}'))"

echo ""
echo "[3/5] Construyendo imagen Docker..."
sudo docker-compose build

if [ $? -ne 0 ]; then
    echo "❌ Error al construir la imagen"
    exit 1
fi

echo ""
echo "[4/5] Levantando contenedor..."
sudo docker-compose up -d

if [ $? -ne 0 ]; then
    echo "❌ Error al levantar el contenedor"
    exit 1
fi

echo ""
echo "[5/5] Esperando a que el servidor inicie..."
sleep 5

echo ""
echo "========================================="
echo "✅ Servidor creado exitosamente"
echo "========================================="
echo ""
echo "Plugins instalados:"
echo "  - GeyserMC (soporte Bedrock)"
echo "  - Floodgate (autenticación Bedrock)"
echo "  - ViaVersion (soporte multi-versión)"
echo "  - ViaBackwards (versiones antiguas)"
echo "  - ViaRewind (versiones muy antiguas)"
echo ""
echo "Puertos:"
echo "  - Java Edition: 25565"
echo "  - Bedrock Edition: 19132"
echo "  - RCON (Panel Web): 25575"
echo ""
echo "RCON:"
echo "  - Puerto: 25575"
echo "  - Contraseña: minecraft123"
echo "  - Estado: Habilitado"
echo ""
echo "Archivos de configuración en:"
echo "  - ./config/server.properties"
echo "  - ./config/backup_config.json (backups automáticos)"
echo "  - ./config/panel_config.json (rendimiento del panel)"
echo "  - ./worlds/ (mundos y datos del servidor)"
echo "  - ./backups/worlds/ (backups de mundos)"
echo "  - ./plugins/ (addons)"
echo "  - ./resourcepacks/ (texturas)"
echo ""
echo "Sistema Multi-Mundos:"
echo "  - Crear/gestionar mundos desde el panel web"
echo "  - Cambiar entre mundos sin reiniciar"
echo "  - Backups automáticos configurables"
echo "  - Ver documentación: cat BACKUP_SYSTEM.md"
echo ""
echo "Comandos disponibles:"
echo "  - Ver logs: ./run.sh"
echo "  - Detener: ./stop.sh"
echo "  - Actualizar servidor: ./update.sh"
echo "  - Actualizar plugins: ./update.sh --plugins"
echo "  - Migrar a multi-mundos: ./migrate-to-multiworld.sh"
echo "  - Tests del sistema: ./run-tests.sh"
