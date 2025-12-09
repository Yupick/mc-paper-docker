#!/bin/bash

echo "========================================="
echo "Creando servidor Minecraft Paper"
echo "========================================="

echo ""
echo "[1/6] Creando directorios necesarios..."
mkdir -p worlds plugins resourcepacks config logs backups/worlds
mkdir -p config/plugin config/plugin-data

# Verificar si existe estructura multi-mundo
if [ -L "worlds/active" ]; then
    echo "  ✅ Estructura multi-mundo detectada (symlink activo)"
    ACTIVE_WORLD=$(readlink worlds/active)
    echo "     → Mundo activo: $ACTIVE_WORLD"
    
    # Verificar que el symlink apunta a un directorio existente
    if [ ! -d "worlds/active" ]; then
        echo "  ⚠️  El symlink 'worlds/active' apunta a un directorio inexistente: $ACTIVE_WORLD"
        echo "     Creando directorio de mundo por defecto..."
        mkdir -p "worlds/world"
        rm -f "worlds/active"
        ln -sf "world" "worlds/active"
        echo "  ✅ Symlink reparado: worlds/active -> world"
    fi
elif [ -d "worlds/active" ]; then
    echo "  ⚠️  'worlds/active' es un directorio (debería ser symlink)"
    echo "     Convirtiendo a estructura multi-mundo..."
    
    # Respaldar directorio actual
    if [ -n "$(ls -A worlds/active 2>/dev/null)" ]; then
        mv "worlds/active" "worlds/world"
        echo "  ✅ Mundo movido: worlds/active → worlds/world"
    else
        rm -rf "worlds/active"
        mkdir -p "worlds/world"
    fi
    
    # Crear symlink
    ln -sf "world" "worlds/active"
    echo "  ✅ Symlink creado: worlds/active -> world"
else
    echo "  ℹ️  Creando estructura multi-mundo inicial..."
    mkdir -p "worlds/world"
    ln -sf "world" "worlds/active"
    echo "  ✅ Estructura creada: worlds/active -> world"
fi

# Verificar y crear server.properties en el mundo activo
ACTIVE_PROPERTIES="worlds/active/server.properties"
if [ ! -f "$ACTIVE_PROPERTIES" ]; then
    echo "  ℹ️  Creando server.properties para el mundo activo..."
    
    # Crear server.properties con configuración por defecto
    cat > "$ACTIVE_PROPERTIES" << 'EOF'
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
level-name=world
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
    echo "  ✅ server.properties creado en: $ACTIVE_PROPERTIES"
else
    echo "  ✅ server.properties ya existe en: $ACTIVE_PROPERTIES"
fi

# Verificar permisos del archivo server.properties
if [ -f "$ACTIVE_PROPERTIES" ]; then
    # Aplicar permisos de escritura
    chmod 664 "$ACTIVE_PROPERTIES" 2>/dev/null || sudo chmod 664 "$ACTIVE_PROPERTIES"
    
    # Verificar si es escribible
    if [ ! -w "$ACTIVE_PROPERTIES" ]; then
        echo "  ⚠️  Advertencia: server.properties no es escribible"
        echo "     Intentando corregir permisos..."
        sudo chown $(id -u):$(id -g) "$ACTIVE_PROPERTIES" 2>/dev/null
        sudo chmod 664 "$ACTIVE_PROPERTIES" 2>/dev/null
        
        if [ -w "$ACTIVE_PROPERTIES" ]; then
            echo "  ✅ Permisos corregidos"
        else
            echo "  ❌ No se pudieron corregir los permisos automáticamente"
            echo "     Ejecuta: sudo chown $(id -u):$(id -g) $ACTIVE_PROPERTIES"
        fi
    fi
fi

# Verificar directorios de mundos (world, world_nether, world_the_end)
for world_dir in "world" "world_nether" "world_the_end"; do
    WORLD_PATH="worlds/active/$world_dir"
    if [ ! -d "$WORLD_PATH" ]; then
        mkdir -p "$WORLD_PATH"
        echo "  ✅ Directorio creado: $WORLD_PATH"
    fi
done

echo ""
echo "[2/6] Creando archivos de configuración del panel..."
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

# Crear archivos JSON de configuración base si no existen
echo "  Verificando archivos de configuración MMORPG..."
if [ ! -f "config/crafting_config.json" ]; then
    cat > config/crafting_config.json << 'EOF'
{
  "recipes": [],
  "crafting_stations": [
    {
      "id": "workbench",
      "name": "Crafting Table",
      "material": "crafting_table",
      "enabled": true
    }
  ]
}
EOF
    echo "    ✅ crafting_config.json creado"
fi

# Configuración base de encantamientos
if [ ! -f "config/enchanting_config.json" ]; then
        cat > config/enchanting_config.json << 'EOF'
{
    "stations": [
        { "id": "altar_basic", "name": "Altar Básico", "max_tier": "UNCOMMON", "success": 90 }
    ],
    "rules": {
        "base_success": 80,
        "tier_scaling": { "UNCOMMON": 100, "RARE": 90, "EPIC": 80, "LEGENDARY": 70 }
    }
}
EOF
        echo "    ✅ enchanting_config.json creado"
fi

# Configuración base de mascotas
if [ ! -f "config/pets_config.json" ]; then
        cat > config/pets_config.json << 'EOF'
{
    "pet_settings": {
        "max_pets_per_player": 3
    },
    "pets": [
        {
            "id": "wolf_pup",
            "name": "Lobezno",
            "type": "COMBAT",
            "rarity": "COMMON",
            "base_stats": { "health": 20, "damage": 3, "speed": 0.2 },
            "adoption_cost": 100,
            "feed_restore_health": 5,
            "evolution_levels": [ { "stats_multiplier": 1.0 }, { "stats_multiplier": 1.2 } ]
        }
    ],
    "mounts": [
        {
            "id": "horse_brown",
            "name": "Corcel Marrón",
            "speed": 0.25,
            "jump": 0.5,
            "unlock_cost": 250
        }
    ]
}
EOF
        echo "    ✅ pets_config.json creado"
fi

if [ ! -f "config/respawn_config.json" ]; then
    cat > config/respawn_config.json << 'EOF'
{
  "respawn_settings": {
    "enabled": true,
    "cooldown_seconds": 5,
    "message_on_respawn": "Has resucitado"
  },
  "respawn_points": {
    "default": {
      "world": "world",
      "x": 0,
      "y": 64,
      "z": 0
    }
  }
}
EOF
    echo "    ✅ respawn_config.json creado"
fi

echo ""
echo "[3/7] Compilando e instalando plugin MMORPG..."

# Verificar si existe Maven
if ! command -v mvn &> /dev/null; then
    echo "  ⚠️  Maven no está instalado"
    echo "  Instalando Maven..."
    if [ -x "scripts/install-dependencies.sh" ]; then
        bash scripts/install-dependencies.sh
    else
        echo "  ❌ No se pudo instalar Maven automáticamente"
        echo "  Por favor, instala Maven manualmente"
    fi
fi

# Compilar el plugin MMORPG
if [ -d "mmorpg-plugin" ]; then
    echo "  📦 Compilando plugin MMORPG..."
    cd mmorpg-plugin
    mvn clean package -q > /dev/null 2>&1
    COMPILE_STATUS=$?
    cd ..
    
    if [ $COMPILE_STATUS -eq 0 ]; then
        echo "    ✅ Plugin MMORPG compilado exitosamente"
        
        # Copiar JAR al directorio de plugins
        JAR_FILE="mmorpg-plugin/target/mmorpg-plugin-1.0.0.jar"
        if [ -f "$JAR_FILE" ]; then
            cp "$JAR_FILE" plugins/mmorpg-plugin-1.0.0.jar
            echo "    ✅ JAR copiado a directorio de plugins"
        else
            echo "    ❌ No se encontró el archivo JAR compilado"
        fi
        
        # Preparar rutas de configuración y datos en el plugin
        PLUGIN_DIR="plugins/MMORPGPlugin"
        DATA_DIR="$PLUGIN_DIR/data"
        ACTIVE_WORLD=$(readlink worlds/active || echo "world")
        mkdir -p "$PLUGIN_DIR" "$DATA_DIR" "$DATA_DIR/$ACTIVE_WORLD"
        echo "    ✅ Directorios del plugin preparados: $PLUGIN_DIR y $DATA_DIR/$ACTIVE_WORLD"

        # Sincronizar configuraciones universales desde config/plugin/ al directorio del plugin
        UNIVERSAL_CONFIGS=(
          "achievements_config.json"
          "bestiary_config.json"
          "crafting_config.json"
          "dungeons_config.json"
          "enchanting_config.json"
          "enchantments_config.json"
          "events_config.json"
          "invasions_config.json"
          "pets_config.json"
          "ranks_config.json"
          "respawn_config.json"
          "squad_config.json"
        )
        for cfg in "${UNIVERSAL_CONFIGS[@]}"; do
            # Intentar copiar desde config/plugin/ primero (desde .example)
            if [ -f "config/plugin/$cfg.example" ]; then
                cp "config/plugin/$cfg.example" "$PLUGIN_DIR/$cfg" 2>/dev/null
            # Fallback: copiar desde config/ si existe (backwards compatibility)
            elif [ -f "config/$cfg" ]; then
                cp "config/$cfg" "$PLUGIN_DIR/$cfg" 2>/dev/null
            # Último recurso: crear placeholder vacío
            elif [ ! -f "$PLUGIN_DIR/$cfg" ]; then
                echo "{}" > "$PLUGIN_DIR/$cfg"
            fi
        done
        echo "    ✅ Configuraciones universales sincronizadas en $PLUGIN_DIR desde config/plugin/"

        # Copiar datos universales desde config/plugin-data/ (Universal scope)
        GLOBAL_DATA_FILES=(
          "items.json"
          "mobs.json"
          "npcs.json"
          "quests.json"
          "pets.json"
          "enchantments.json"
        )
        for data_file in "${GLOBAL_DATA_FILES[@]}"; do
            TARGET="$DATA_DIR/$data_file"
            if [ ! -f "$TARGET" ]; then
                # Intentar copiar desde config/plugin-data/ primero
                if [ -f "config/plugin-data/$data_file.example" ]; then
                    cp "config/plugin-data/$data_file.example" "$TARGET" 2>/dev/null
                else
                    # Crear vacío como fallback
                    echo "{}" > "$TARGET"
                fi
            fi
        done
        echo "    ✅ Datos universales copiados en $DATA_DIR desde config/plugin-data/"

        # Crear datos locales y exclusive-local del mundo activo
        # Local scope (npcs, quests, mobs, pets, enchantments)
        for world_file in npcs.json quests.json mobs.json pets.json enchantments.json; do
            TARGET="$DATA_DIR/$ACTIVE_WORLD/$world_file"
            if [ ! -f "$TARGET" ]; then
                echo "{}" > "$TARGET"
            fi
        done
        # Exclusive-local scope (players, status, invasions, kills, respawn, squads, metadata)
        for world_file in players.json status.json invasions.json kills.json respawn.json squads.json metadata.json; do
            TARGET="$DATA_DIR/$ACTIVE_WORLD/$world_file"
            if [ ! -f "$TARGET" ]; then
                # Arrays para invasions y squads, objects para el resto
                if [[ "$world_file" =~ ^(invasions|squads)\.json$ ]]; then
                    echo "[]" > "$TARGET"
                else
                    echo "{}" > "$TARGET"
                fi
            fi
        done
        echo "    ✅ Datos locales y exclusive-local del mundo activo inicializados en $DATA_DIR/$ACTIVE_WORLD"
    else
        echo "    ⚠️  Error en compilación del plugin MMORPG"
        echo "    Se continuará con la instalación sin el plugin"
    fi
else
    echo "  ℹ️  Directorio mmorpg-plugin no encontrado, saltando compilación"
fi

echo ""
echo "[4/7] Descargando plugins..."
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
echo "[5/7] Construyendo imagen Docker..."
sudo docker-compose build minecraft-paper

if [ $? -ne 0 ]; then
    echo "❌ Error al construir la imagen"
    exit 1
fi

echo ""
echo "[6/7] Levantando contenedor..."
sudo docker-compose up -d minecraft-paper

if [ $? -ne 0 ]; then
    echo "❌ Error al levantar el contenedor"
    exit 1
fi

echo ""
echo "[7/7] Esperando a que el servidor inicie..."
sleep 5

# Verificar si el plugin MMORPG se cargó correctamente
echo ""
echo "========================================="
echo "Verificando plugin MMORPG..."
echo "========================================="
sleep 2

if docker ps --format '{{.Names}}' | grep -q "^minecraft-paper$" 2>/dev/null; then
    if docker exec minecraft-paper test -f /server/plugins/mmorpg-plugin-1.0.0.jar 2>/dev/null; then
        echo "✅ Plugin MMORPG detectado en el servidor"
        
        # Esperar a que el plugin cree sus archivos de configuración
        echo "   Esperando a que el plugin genere archivos de configuración..."
        sleep 3
        
        if docker exec minecraft-paper test -d /server/plugins/MMORPGPlugin 2>/dev/null; then
            echo "   ✅ Directorio de configuración del plugin detectado"
        fi
    else
        echo "⚠️  Plugin MMORPG no encontrado en el servidor"
        echo "   Puedes instalarlo manualmente ejecutando: bash quick-install.sh"
    fi
fi

echo ""
echo "========================================="
echo "✅ Servidor creado exitosamente"
echo "========================================="
echo ""
echo "Plugins instalados:"
echo "  - MMORPG Plugin (sistema RPG completo)"
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
