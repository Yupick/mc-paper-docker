# Cambios realizados - Persistencia de Mundos

## Problema identificado
Los mundos de Minecraft NO estaban siendo persistentes porque:
- El volumen `./data:/server/data` no capturaba los mundos
- Los mundos se guardan en `/server/world`, `/server/world_nether`, `/server/world_the_end`
- Al eliminar el contenedor, los mundos se perdían

## Solución implementada
Se cambió la estructura de volúmenes para mapear TODO el directorio del servidor:

### Antes:
```yaml
volumes:
  - ./data:/server/data
  - ./plugins:/server/plugins
  - ./themes:/server/themes
  - ./resourcepacks:/server/resourcepacks
  - ./config/server.properties:/server/server.properties
```

### Ahora:
```yaml
volumes:
  - ./worlds:/server
  - ./plugins:/server/plugins
  - ./resourcepacks:/server/resourcepacks
  - ./config/server.properties:/server/server.properties
```

## Archivos modificados
1. ✅ `docker-compose.yml` - Volúmenes actualizados
2. ✅ `create.sh` - Crea carpeta `worlds` en lugar de `data` y `themes`
3. ✅ `update.sh` - Actualizado para usar `worlds`
4. ✅ `uninstall.sh` - Elimina `worlds` en lugar de `data` y `themes`
5. ✅ `setup-minecraft.md` - Documentación actualizada
6. ✅ `migrate.sh` - Script nuevo para migrar datos existentes

## Nueva estructura de carpetas
```
mc-paper/
├── worlds/              # ← TODO el servidor (mundos, logs, configs generados)
├── plugins/             # ← Plugins
├── resourcepacks/       # ← Paquetes de recursos
├── config/
│   └── server.properties # ← Configuración principal
├── create.sh
├── update.sh
├── run.sh
├── stop.sh
├── uninstall.sh
└── migrate.sh           # ← Nuevo script de migración
```

## Qué contiene ahora `./worlds/`
- `world/` - Mundo principal (Overworld)
- `world_nether/` - El Nether
- `world_the_end/` - El End
- `logs/` - Logs del servidor
- `cache/` - Caché
- `libraries/` - Librerías de Paper
- `versions/` - Versiones
- Todos los archivos generados por el servidor

## Para migrar datos existentes

**IMPORTANTE:** Primero debes poder detener el contenedor actual. Si tienes problemas de permisos:

```bash
# Opción 1: Reiniciar el sistema
sudo reboot

# Después de reiniciar, ejecuta:
cd /home/mkd/contenedores/mc-paper
./migrate.sh
```

## Para iniciar desde cero
Si no tienes datos que migrar o quieres empezar de nuevo:

```bash
cd /home/mkd/contenedores/mc-paper
sudo docker-compose down  # Detener servidor actual
sudo rm -rf data themes   # Eliminar carpetas antiguas
./create.sh               # Crear con nueva estructura
```

## Ventajas de la nueva estructura
✅ Los mundos son 100% persistentes
✅ Toda la configuración del servidor se mantiene
✅ Los logs se guardan fuera del contenedor
✅ Estructura más simple y clara
✅ No se pierde ningún dato al actualizar el servidor
