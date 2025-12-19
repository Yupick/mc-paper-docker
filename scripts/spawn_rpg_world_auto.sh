#!/bin/bash
# Script automatizado de spawn vía RCON
# Requiere rcon-cli instalado: https://github.com/itzg/rcon-cli

RCON_HOST="${RCON_HOST:-localhost}"
RCON_PORT="${RCON_PORT:-25575}"
RCON_PASSWORD="${RCON_PASSWORD:-}"
WORLD="mmorpg"

if [ -z "$RCON_PASSWORD" ]; then
    echo "Error: Define la variable RCON_PASSWORD"
    echo "Ejemplo: RCON_PASSWORD='tu_password' bash spawn_rpg_world_auto.sh"
    exit 1
fi

if ! command -v rcon-cli &> /dev/null; then
    echo "Error: rcon-cli no está instalado"
    echo "Instala con: wget https://github.com/itzg/rcon-cli/releases/download/1.6.0/rcon-cli_1.6.0_linux_amd64.tar.gz"
    exit 1
fi

RCON="rcon-cli --host $RCON_HOST --port $RCON_PORT --password $RCON_PASSWORD"

echo "=== Spawneando mundo RPG automáticamente ==="
echo "Host: $RCON_HOST:$RCON_PORT"
echo "Mundo: $WORLD"
echo ""

# NPCs
echo "Spawneando NPCs..."
$RCON "rpg npc spawn npc_trainer_warrior $WORLD 10 64 10"
$RCON "rpg npc spawn npc_merchant_general $WORLD -10 64 10"
$RCON "rpg npc spawn npc_questgiver_start $WORLD 0 64 20"
$RCON "rpg npc spawn npc_blacksmith $WORLD 20 64 -10"
echo "✓ 4 NPCs spawneados"
echo ""

# Mobs
echo "Spawneando mobs..."
$RCON "rpg mob spawn zombie_warrior $WORLD 150 64 0"
$RCON "rpg mob spawn skeleton_archer $WORLD 200 64 50"
$RCON "rpg mob spawn spider_hunter $WORLD 250 64 -50"
$RCON "rpg mob spawn boss_dragon $WORLD -350 68 -350"
echo "✓ 4 tipos de mobs spawneados"
echo ""

# Zonas
echo "Creando zonas..."
$RCON "rpg zone create safe_zone SAFE $WORLD -50 60 -50 50 100 50"
$RCON "rpg zone create farming_zone FARMEO $WORLD 100 60 -100 300 80 100"
$RCON "rpg zone create dungeon_entrance DUNGEON $WORLD -200 50 100 -100 70 200"
$RCON "rpg zone create boss_arena BOSS_ARENA $WORLD -400 65 -400 -300 75 -300"
echo "✓ 4 zonas creadas"
echo ""

# Waypoints
echo "Creando waypoints..."
$RCON "rpg waypoint create waypoint_spawn \"Plaza Central\" $WORLD 0 64 0 1"
$RCON "rpg waypoint create waypoint_farming \"Campo de Entrenamiento\" $WORLD 200 64 0 5"
$RCON "rpg waypoint create waypoint_dungeon \"Cripta Oscura\" $WORLD -150 64 150 10"
$RCON "rpg waypoint create waypoint_boss \"Coliseo\" $WORLD -350 68 -350 30"
echo "✓ 4 waypoints creados"
echo ""

echo "=== Mundo RPG cargado exitosamente ==="
echo ""
echo "Siguiente paso: Ingresa al servidor en el mundo '$WORLD' y verifica:"
echo "- NPCs en la plaza central"
echo "- Mobs en zonas de farmeo"
echo "- Boss Dragon en el coliseo"
echo "- Waypoints funcionando"
