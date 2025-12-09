# Fase 5: Plan de Limpieza de Duplicados

## Archivos Actuales en `plugins/MMORPGPlugin/data/`

### Configuración (mal ubicada - debería estar en raíz o ser copiada desde config/)
- `achievements_config.json` - ❌ Debe estar en raíz de MMORPGPlugin/
- `bestiary_config.json` - ❌ Debe estar en raíz de MMORPGPlugin/
- `invasions_config.json` - ❌ Debe estar en raíz de MMORPGPlugin/
- `ranks_config.json` - ❌ Debe estar en raíz de MMORPGPlugin/

### Datos Universales (correctamente ubicado)
- `items.json` - ✅ Correcto en data/
- `mobs.json` - ✅ Correcto en data/

### Datos Locales (correctamente ubicado)
- `world/metadata.json` - ✅ Correcto en data/world/
- `world/players.json` - ✅ Correcto en data/world/
- `world/status.json` - ✅ Correcto en data/world/

## Plan de Limpieza

### Paso 1: Mover configuración mal ubicada
```bash
# Mover desde data/ a raíz de MMORPGPlugin/
mv plugins/MMORPGPlugin/data/achievements_config.json plugins/MMORPGPlugin/
mv plugins/MMORPGPlugin/data/bestiary_config.json plugins/MMORPGPlugin/
mv plugins/MMORPGPlugin/data/invasions_config.json plugins/MMORPGPlugin/
mv plugins/MMORPGPlugin/data/ranks_config.json plugins/MMORPGPlugin/
```

### Paso 2: Verificar que no hay duplicados de datos

**Ya verificado:** No hay duplicados, la estructura es correcta.

## Archivos a Mantener (después de limpieza)

```
plugins/MMORPGPlugin/
├── achievements_config.json      # De config/plugin/achievements_config.json.example
├── bestiary_config.json          # De config/plugin/bestiary_config.json.example
├── crafting_config.json          # ✅ Ya está aquí
├── dungeons_config.json          # ✅ Ya está aquí
├── enchanting_config.json        # ✅ Ya está aquí
├── enchantments_config.json      # ✅ Ya está aquí
├── events_config.json            # ✅ Ya está aquí
├── invasions_config.json         # De config/plugin/invasions_config.json.example
├── pets_config.json              # ✅ Ya está aquí
├── ranks_config.json             # De config/plugin/ranks_config.json.example
├── respawn_config.json           # ✅ Ya está aquí
├── squad_config.json             # ✅ Ya está aquí
└── data/
    ├── items.json                # De config/plugin-data/items.json.example
    ├── mobs.json                 # De config/plugin-data/mobs.json.example
    ├── npcs.json                 # De config/plugin-data/npcs.json.example
    ├── quests.json               # De config/plugin-data/quests.json.example
    ├── enchantments.json         # De config/plugin-data/enchantments.json.example
    ├── pets.json                 # De config/plugin-data/pets.json.example
    └── world/
        ├── metadata.json         # Local al mundo
        ├── players.json          # Local al mundo
        └── status.json           # Local al mundo
```

## Acción Recomendada

La estructura **ya está mayormente correcta**. Solo necesita:
1. ✅ Mover 4 archivos de config de `data/` a raíz
2. ✅ Agregar archivos faltantes (npcs.json, quests.json, enchantments.json, pets.json)
3. ✅ Usar DataInitializer para auto-crear en próximas instalaciones

## Impacto de Cambios

- **Riesgo bajo:** Solo se mueven/agregan archivos
- **Backcompat:** Scripts instalación copiaran desde config/ automáticamente
- **DataInitializer:** Creará automáticamente cualquier archivo faltante
