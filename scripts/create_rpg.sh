#!/bin/bash
# Script para crear archivos de configuración RPG con datos de ejemplo
# Uso: ./scripts/create_rpg.sh
# Nota: Los archivos se crean en plugins/MMORPGPlugin/ para coherencia con el servidor en ejecución

set -e

MINECRAFT_DIR="$(dirname "$0")/.."
PLUGIN_CONFIG_DIR="$MINECRAFT_DIR/plugins/MMORPGPlugin"

# Crear directorio del plugin si no existe
mkdir -p "$PLUGIN_CONFIG_DIR"

# Crear ejemplo de enchanting_config.json si no existe
if [ ! -f "$PLUGIN_CONFIG_DIR/enchanting_config.json" ]; then
    cat > "$PLUGIN_CONFIG_DIR/enchanting_config.json" << 'EOF'
{
    "stations": [
        { "id": "altar_basic", "name": "Altar Básico", "max_tier": "UNCOMMON", "success": 90 },
        { "id": "altar_rare", "name": "Altar Raro", "max_tier": "RARE", "success": 80 },
        { "id": "altar_epic", "name": "Altar Épico", "max_tier": "EPIC", "success": 70 },
        { "id": "altar_legendary", "name": "Altar Legendario", "max_tier": "LEGENDARY", "success": 60 },
        { "id": "altar_mythic", "name": "Altar Mítico", "max_tier": "MYTHIC", "success": 50 }
    ],
    "rules": {
        "base_success": 80,
        "tier_scaling": { "UNCOMMON": 100, "RARE": 90, "EPIC": 80, "LEGENDARY": 70, "MYTHIC": 60 }
    }
}
EOF
    echo "    ✅ enchanting_config.json creado con ejemplos"
fi

# Crear ejemplo de pets_config.json si no existe
if [ ! -f "$PLUGIN_CONFIG_DIR/pets_config.json" ]; then
    cat > "$PLUGIN_CONFIG_DIR/pets_config.json" << 'EOF'
{
    "pet_settings": {
        "max_pets_per_player": 3
    },
    "pets": [
        { "id": "wolf_pup", "name": "Lobezno", "type": "COMBAT", "rarity": "COMMON", "base_stats": { "health": 20, "damage": 3, "speed": 0.2 }, "adoption_cost": 100, "feed_restore_health": 5, "evolution_levels": [ { "stats_multiplier": 1.0 }, { "stats_multiplier": 1.2 } ] },
        { "id": "cat_kitten", "name": "Gatito", "type": "UTILITY", "rarity": "UNCOMMON", "base_stats": { "health": 15, "damage": 1, "speed": 0.3 }, "adoption_cost": 120, "feed_restore_health": 4, "evolution_levels": [ { "stats_multiplier": 1.0 }, { "stats_multiplier": 1.3 } ] },
        { "id": "dragon_baby", "name": "Dragón Bebé", "type": "COMBAT", "rarity": "EPIC", "base_stats": { "health": 50, "damage": 10, "speed": 0.4 }, "adoption_cost": 1000, "feed_restore_health": 20, "evolution_levels": [ { "stats_multiplier": 1.0 }, { "stats_multiplier": 1.5 } ] },
        { "id": "slime_pet", "name": "Slime", "type": "UTILITY", "rarity": "RARE", "base_stats": { "health": 25, "damage": 2, "speed": 0.15 }, "adoption_cost": 200, "feed_restore_health": 6, "evolution_levels": [ { "stats_multiplier": 1.0 }, { "stats_multiplier": 1.2 } ] },
        { "id": "parrot", "name": "Loro", "type": "UTILITY", "rarity": "UNCOMMON", "base_stats": { "health": 10, "damage": 0, "speed": 0.5 }, "adoption_cost": 80, "feed_restore_health": 3, "evolution_levels": [ { "stats_multiplier": 1.0 }, { "stats_multiplier": 1.1 } ] }
    ],
    "mounts": [
        { "id": "horse_brown", "name": "Corcel Marrón", "speed": 0.25, "jump": 0.5, "unlock_cost": 250 },
        { "id": "horse_white", "name": "Corcel Blanco", "speed": 0.28, "jump": 0.6, "unlock_cost": 350 },
        { "id": "pig_mount", "name": "Cerdo Montura", "speed": 0.18, "jump": 0.3, "unlock_cost": 100 },
        { "id": "llama_mount", "name": "Llama Montura", "speed": 0.22, "jump": 0.4, "unlock_cost": 200 },
        { "id": "strider_mount", "name": "Strider", "speed": 0.30, "jump": 0.2, "unlock_cost": 400 }
    ]
}
EOF
    echo "    ✅ pets_config.json creado con ejemplos"
fi

# Crear ejemplo de respawn_config.json si no existe
if [ ! -f "$PLUGIN_CONFIG_DIR/respawn_config.json" ]; then
    cat > "$PLUGIN_CONFIG_DIR/respawn_config.json" << 'EOF'
{
  "respawn_settings": {
    "enabled": true,
    "cooldown_seconds": 5,
    "message_on_respawn": "Has resucitado"
  },
  "respawn_points": {
    "default": { "world": "world", "x": 0, "y": 64, "z": 0 },
    "ciudad": { "world": "world", "x": 100, "y": 70, "z": -50 },
    "mazmorra": { "world": "dungeon", "x": -200, "y": 40, "z": 300 },
    "castillo": { "world": "castle", "x": 500, "y": 80, "z": 100 },
    "aldea": { "world": "village", "x": -75, "y": 65, "z": 220 }
  }
}
EOF
    echo "    ✅ respawn_config.json creado con ejemplos"
fi


# Directorio correcto para datos del plugin en el servidor en ejecución
PLUGIN_DATA_DIR="$PLUGIN_CONFIG_DIR/data"
NPC_DIR="$PLUGIN_CONFIG_DIR/npcs"
QUEST_DIR="$PLUGIN_CONFIG_DIR/quests"
MOB_DIR="$PLUGIN_CONFIG_DIR/data/mobs"

mkdir -p "$NPC_DIR" "$QUEST_DIR" "$MOB_DIR"


# Crear ejemplo universal de mobs según MOBS_GUIDE.md
if [ ! -f "$MOB_DIR/mobs_example.json" ]; then
        cat > "$MOB_DIR/mobs_example.json" << 'EOF'
{
    "zombie_warrior": {
        "displayName": "Guerrero Zombie",
        "entityType": "ZOMBIE",
        "level": 5,
        "health": 30,
        "damage": 4.5,
        "defense": 2.0,
        "xpReward": 150,
        "isBoss": false,
        "spawnLocation": null,
        "drops": ["ROTTEN_FLESH,1,3,100", "IRON_INGOT,0,1,25"]
    },
    "skeleton_archer": {
        "displayName": "Arquero Esqueleto",
        "entityType": "SKELETON",
        "level": 6,
        "health": 24,
        "damage": 5.0,
        "defense": 1.0,
        "xpReward": 175,
        "isBoss": false,
        "spawnLocation": null,
        "drops": ["BONE,1,5,100", "ARROW,3,8,60", "STRING,0,2,30"]
    },
    "creeper_explosive": {
        "displayName": "Creeper Explosivo",
        "entityType": "CREEPER",
        "level": 4,
        "health": 20,
        "damage": 8.0,
        "defense": 0.5,
        "xpReward": 120,
        "isBoss": false,
        "spawnLocation": null,
        "drops": ["GUNPOWDER,1,4,100", "TNT,0,1,15"]
    },
    "spider_hunter": {
        "displayName": "Araña Cazadora",
        "entityType": "SPIDER",
        "level": 4,
        "health": 22,
        "damage": 3.5,
        "defense": 1.5,
        "xpReward": 110,
        "isBoss": false,
        "spawnLocation": null,
        "drops": ["STRING,2,5,100", "SPIDER_EYE,1,2,70"]
    },
    "slime_king": {
        "displayName": "Rey Slime",
        "entityType": "SLIME",
        "level": 7,
        "health": 35,
        "damage": 5.5,
        "defense": 2.5,
        "xpReward": 200,
        "isBoss": false,
        "spawnLocation": null,
        "drops": ["SLIME_BALL,3,8,100", "EMERALD,0,1,10"]
    },
    "dark_witch": {
        "displayName": "Bruja Oscura",
        "entityType": "WITCH",
        "level": 8,
        "health": 26,
        "damage": 3.5,
        "defense": 3.0,
        "xpReward": 250,
        "isBoss": false,
        "spawnLocation": null,
        "drops": ["REDSTONE,2,5,50", "GLOWSTONE_DUST,1,3,40", "POTION,1,1,25", "AMETHYST_SHARD,0,1,15"]
    },
    "phantom_night_terror": {
        "displayName": "Terror Nocturno",
        "entityType": "PHANTOM",
        "level": 8,
        "health": 28,
        "damage": 6.5,
        "defense": 1.0,
        "xpReward": 220,
        "isBoss": false,
        "spawnLocation": null,
        "drops": ["PHANTOM_MEMBRANE,1,3,100", "FEATHER,2,5,50"]
    },
    "enderman_shadow": {
        "displayName": "Enderman Sombrío",
        "entityType": "ENDERMAN",
        "level": 9,
        "health": 40,
        "damage": 7.0,
        "defense": 3.0,
        "xpReward": 300,
        "isBoss": false,
        "spawnLocation": null,
        "drops": ["ENDER_PEARL,1,2,100", "OBSIDIAN,0,1,25"]
    },
    "ghast_phantom": {
        "displayName": "Ghast Fantasma",
        "entityType": "GHAST",
        "level": 10,
        "health": 50,
        "damage": 8.5,
        "defense": 1.0,
        "xpReward": 350,
        "isBoss": false,
        "spawnLocation": null,
        "drops": ["GHAST_TEAR,1,2,100", "GUNPOWDER,2,4,70"]
    },
    "ice_golem": {
        "displayName": "Gólem de Hielo",
        "entityType": "IRON_GOLEM",
        "level": 10,
        "health": 100,
        "damage": 8.0,
        "defense": 6.0,
        "xpReward": 400,
        "isBoss": false,
        "spawnLocation": null,
        "drops": ["IRON_INGOT,3,8,100", "SNOWBALL,10,20,50", "BLUE_ICE,1,3,30"]
    },
    "blaze_inferno": {
        "displayName": "Blaze Infernal",
        "entityType": "BLAZE",
        "level": 11,
        "health": 45,
        "damage": 9.0,
        "defense": 2.5,
        "xpReward": 450,
        "isBoss": false,
        "spawnLocation": null,
        "drops": ["BLAZE_ROD,1,3,100", "FIRE_CHARGE,2,5,60"]
    },
    "piglin_brute_elite": {
        "displayName": "Piglin Bruto de Élite",
        "entityType": "PIGLIN_BRUTE",
        "level": 12,
        "health": 80,
        "damage": 10.0,
        "defense": 5.0,
        "xpReward": 500,
        "isBoss": false,
        "spawnLocation": null,
        "drops": ["GOLD_INGOT,3,7,100", "GOLDEN_AXE,0,1,30", "CRYING_OBSIDIAN,0,1,20"]
    },
    "wither_skeleton_knight": {
        "displayName": "Caballero Wither",
        "entityType": "WITHER_SKELETON",
        "level": 13,
        "health": 60,
        "damage": 11.0,
        "defense": 4.0,
        "xpReward": 550,
        "isBoss": false,
        "spawnLocation": null,
        "drops": ["BONE,2,6,100", "COAL,3,8,80", "WITHER_SKELETON_SKULL,0,1,5"]
    },
    "elite_ravager": {
        "displayName": "Asolador de Élite",
        "entityType": "RAVAGER",
        "level": 14,
        "health": 120,
        "damage": 13.0,
        "defense": 7.0,
        "xpReward": 800,
        "isBoss": false,
        "spawnLocation": null,
        "drops": ["EMERALD,2,5,100", "SADDLE,0,1,40", "DIAMOND,1,3,30"]
    },
    "elite_vindicator": {
        "displayName": "Vengador de Élite",
        "entityType": "VINDICATOR",
        "level": 13,
        "health": 90,
        "damage": 12.0,
        "defense": 5.5,
        "xpReward": 700,
        "isBoss": false,
        "spawnLocation": null,
        "drops": ["EMERALD,2,4,100", "IRON_AXE,0,1,50", "TOTEM_OF_UNDYING,0,1,5"]
    },
    "elite_evoker": {
        "displayName": "Evocador de Élite",
        "entityType": "EVOKER",
        "level": 15,
        "health": 100,
        "damage": 11.0,
        "defense": 6.0,
        "xpReward": 900,
        "isBoss": false,
        "spawnLocation": null,
        "drops": ["EMERALD,3,6,100", "TOTEM_OF_UNDYING,1,1,25", "ENCHANTED_BOOK,0,1,40"]
    },
    "elite_guardian": {
        "displayName": "Guardián Antiguo de Élite",
        "entityType": "ELDER_GUARDIAN",
        "level": 16,
        "health": 130,
        "damage": 14.0,
        "defense": 8.0,
        "xpReward": 1000,
        "isBoss": false,
        "spawnLocation": null,
        "drops": ["PRISMARINE_SHARD,5,10,100", "PRISMARINE_CRYSTALS,3,7,80", "SPONGE,1,2,60", "DIAMOND,1,3,40"]
    },
    "elite_shulker": {
        "displayName": "Shulker de Élite",
        "entityType": "SHULKER",
        "level": 17,
        "health": 110,
        "damage": 10.0,
        "defense": 9.0,
        "xpReward": 1100,
        "isBoss": false,
        "spawnLocation": null,
        "drops": ["SHULKER_SHELL,1,3,100", "ENDER_PEARL,2,4,70", "DIAMOND,1,2,35"]
    },
    "necromancer_king": {
        "displayName": "Rey Necrómante",
        "entityType": "WITHER",
        "level": 18,
        "health": 150,
        "damage": 12.0,
        "defense": 8.0,
        "xpReward": 4000,
        "isBoss": true,
        "spawnLocation": "world,0,100,0",
        "drops": ["NETHER_STAR,1,1,100", "DIAMOND,5,10,100", "GOLD_INGOT,5,15,90", "ENCHANTED_GOLDEN_APPLE,1,2,60"]
    },
    "arctic_titan": {
        "displayName": "Titán Ártico",
        "entityType": "IRON_GOLEM",
        "level": 19,
        "health": 180,
        "damage": 14.0,
        "defense": 12.0,
        "xpReward": 4500,
        "isBoss": true,
        "spawnLocation": "world,200,80,200",
        "drops": ["IRON_BLOCK,3,6,100", "DIAMOND,4,8,100", "BLUE_ICE,5,10,100", "NETHERITE_SCRAP,1,3,40"]
    },
    "corrupted_dragon": {
        "displayName": "Dragón Corrupto",
        "entityType": "ENDER_DRAGON",
        "level": 20,
        "health": 200,
        "damage": 15.0,
        "defense": 10.0,
        "xpReward": 5000,
        "isBoss": true,
        "spawnLocation": "world,0,150,0",
        "drops": ["DRAGON_BREATH,1,1,100", "DIAMOND,3,8,100", "EMERALD,2,5,80", "NETHERITE_INGOT,1,2,50", "DRAGON_EGG,0,1,10"]
    }
}
EOF
        echo "    ✅ mobs_example.json universal creado con todos los mobs de la guía"
fi

# Crear ejemplos de NPCs
if [ ! -f "$NPC_DIR/npcs_example.json" ]; then
        cat > "$NPC_DIR/npcs_example.json" << 'EOF'
[
    { "id": "merchant_1", "name": "Comerciante Hugo", "type": "MERCHANT", "location": { "world": "village", "x": -80, "y": 65, "z": 210 }, "shop": ["potion", "sword", "shield"] },
    { "id": "trainer_1", "name": "Entrenador Lara", "type": "TRAINER", "location": { "world": "castle", "x": 510, "y": 80, "z": 110 }, "skills": ["strength", "agility"] },
    { "id": "guard_1", "name": "Guardia Boris", "type": "GUARD", "location": { "world": "city", "x": 120, "y": 70, "z": -60 }, "patrol": ["gate", "market"] },
    { "id": "quest_giver_1", "name": "Maestra Quests", "type": "QUEST_GIVER", "location": { "world": "village", "x": -90, "y": 65, "z": 215 }, "quests": ["quest_1", "quest_2"] },
    { "id": "merchant_2", "name": "Comerciante Ana", "type": "MERCHANT", "location": { "world": "city", "x": 130, "y": 70, "z": -65 }, "shop": ["armor", "bow"] }
]
EOF
        echo "    ✅ npcs_example.json creado con ejemplos"
fi

# Crear ejemplos de Quests
if [ ! -f "$QUEST_DIR/quests_example.json" ]; then
        cat > "$QUEST_DIR/quests_example.json" << 'EOF'
[
    { "id": "quest_1", "name": "El Tesoro Perdido", "objectives": ["Encuentra el cofre en la mazmorra", "Derrota al Esqueleto Guerrero"], "rewards": ["100 monedas", "espada rara"], "difficulty": "MEDIUM" },
    { "id": "quest_2", "name": "Ayuda al Comerciante", "objectives": ["Recolecta 10 pociones", "Entrega a Hugo"], "rewards": ["50 monedas", "poción especial"], "difficulty": "EASY" },
    { "id": "quest_3", "name": "Defensa del Pueblo", "objectives": ["Derrota 5 bandidos", "Habla con Boris"], "rewards": ["escudo", "200 monedas"], "difficulty": "HARD" },
    { "id": "quest_4", "name": "El Rey Slime", "objectives": ["Derrota al Rey Slime", "Recoge la corona"], "rewards": ["crown", "300 monedas"], "difficulty": "BOSS" },
    { "id": "quest_5", "name": "Entrenamiento Especial", "objectives": ["Habla con Lara", "Completa el circuito de agilidad"], "rewards": ["habilidad agilidad", "50 monedas"], "difficulty": "MEDIUM" }
]
EOF
        echo "    ✅ quests_example.json creado con ejemplos"
fi

echo "\nArchivos RPG de ejemplo generados en $PLUGIN_CONFIG_DIR"
