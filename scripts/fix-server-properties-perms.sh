#!/usr/bin/env bash
set -euo pipefail

# Arregla permisos de server.properties dentro de un contenedor Paper
# Uso: ./scripts/fix-server-properties-perms.sh [nombre_contenedor]
# Por defecto el contenedor es "minecraft-paper"

CONTAINER_NAME="${1:-minecraft-paper}"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
info(){ echo -e "${BLUE}[INFO]${NC} $*"; }
ok(){ echo -e "${GREEN}[OK]${NC}  $*"; }
warn(){ echo -e "${YELLOW}[WARN]${NC} $*"; }
err(){ echo -e "${RED}[ERR]${NC}  $*"; }

need_cmd(){ command -v "$1" >/dev/null 2>&1 || { err "Falta comando: $1"; exit 1; }; }

main(){
  need_cmd docker

  info "Verificando contenedor: $CONTAINER_NAME"
  if ! docker ps -a --format '{{.Names}}' | grep -Fxq "$CONTAINER_NAME"; then
    err "No existe el contenedor '$CONTAINER_NAME'"
    echo "Contenedores disponibles:"; docker ps -a --format '  - {{.Names}} ({{.Status}})'
    exit 1
  fi

  # Detectar ruta de server.properties dentro del contenedor
  info "Buscando server.properties dentro del contenedor..."
  SERVER_FILE=$(docker exec "$CONTAINER_NAME" sh -lc '
    for p in \
      server.properties \
      /data/server.properties \
      /server/server.properties \
      /minecraft/server.properties \
      /papermc/server.properties; do
      [ -f "$p" ] && { echo "$p"; exit 0; }
    done
    # Búsqueda acotada como fallback
    find / -maxdepth 3 -type f -name server.properties 2>/dev/null | head -n1 || true
  ')

  if [ -z "${SERVER_FILE}" ]; then
    warn "No se encontró server.properties dentro del contenedor. Intentando resolver vía mounts..."
    MOUNT_LINE=$(docker inspect "$CONTAINER_NAME" --format '{{range .Mounts}}{{if and (eq .Destination "/server/server.properties") (eq .Type "bind")}}{{.Source}} {{.Destination}} {{.RW}}{{end}}{{end}}')
    if [ -n "$MOUNT_LINE" ]; then
      HOST_FILE=$(echo "$MOUNT_LINE" | awk '{print $1}')
      SERVER_FILE="/server/server.properties"
      SERVER_DIR="/server"
      RW_STATE=$(echo "$MOUNT_LINE" | awk '{print $3}')
      info "Archivo enlazado: $HOST_FILE -> $SERVER_FILE (RW=$RW_STATE)"
      if [ ! -e "$HOST_FILE" ]; then
        warn "El archivo no existe en host, lo crearé."
        sudo mkdir -p "$(dirname "$HOST_FILE")"
        sudo touch "$HOST_FILE"
      fi
    else
      err "No se pudo resolver la ruta de server.properties. Revisa docker-compose.yml."
      exit 1
    fi
  else
    ok "Encontrado: $SERVER_FILE"
    SERVER_DIR=$(dirname "$SERVER_FILE")
  fi

  # UID/GID del proceso dentro del contenedor
  CONTAINER_UID=$(docker exec "$CONTAINER_NAME" sh -lc 'id -u' || echo 1000)
  CONTAINER_GID=$(docker exec "$CONTAINER_NAME" sh -lc 'id -g' || echo 1000)
  info "UID:GID del contenedor = ${CONTAINER_UID}:${CONTAINER_GID}"

  # Mapear a ruta host mediante mounts (solo si no se resolvió directamente)
  if [ -z "${HOST_FILE:-}" ]; then
    info "Resolviendo ruta en host mediante mounts..."
    # Formato: "SRC DEST"
    mapfile -t MOUNTS < <(docker inspect "$CONTAINER_NAME" --format '{{range .Mounts}}{{println .Source " " .Destination}}{{end}}')

    HOST_DIR=""
    for line in "${MOUNTS[@]}"; do
      SRC=$(echo "$line" | awk '{print $1}')
      DEST=$(echo "$line" | awk '{print $2}')
      # Normalizar por si hay trailing slashes
      case "$SERVER_DIR" in
        "$DEST"|"$DEST"/*)
          REL="${SERVER_DIR#$DEST}"
          REL="${REL#/}"
          HOST_DIR="$SRC/${REL}"
          break
        ;;
      esac
    done

    if [ -z "$HOST_DIR" ]; then
      err "No se pudo inferir ruta de host; revisa los mounts con: docker inspect $CONTAINER_NAME"
      exit 1
    fi

    HOST_FILE="$HOST_DIR/server.properties"
  fi

  info "Ruta en host estimada: $HOST_FILE"
  if [ ! -e "$HOST_FILE" ]; then
    warn "El archivo no existe en host como $HOST_FILE"
    warn "Continuaré corrigiendo permisos del directorio: $(dirname "$HOST_FILE")"
  fi

  # Requiere privilegios para chown/chmod del host
  if [ $EUID -ne 0 ]; then
    warn "Se requieren privilegios para corregir permisos. Intentando con sudo..."
  fi

  TARGET_DIR=$(dirname "$HOST_FILE")

  # Quitar inmutable si existiera
  if command -v chattr >/dev/null 2>&1 && [ -e "$HOST_FILE" ]; then
    sudo chattr -i "$HOST_FILE" 2>/dev/null || true
  fi

  info "Aplicando permisos en host..."
  sudo mkdir -p "$TARGET_DIR"
  sudo chown -R "${CONTAINER_UID}:${CONTAINER_GID}" "$TARGET_DIR"
  sudo chmod -R u+rwX,g+rwX "$TARGET_DIR"
  if [ -e "$HOST_FILE" ]; then
    sudo chmod 664 "$HOST_FILE"
  fi
  ok "Permisos aplicados en $(dirname "$HOST_FILE")"

  # Verificaciones adicionales: rootfs y mounts ro
  RO_ROOTFS=$(docker inspect "$CONTAINER_NAME" --format '{{.HostConfig.ReadonlyRootfs}}' || echo false)
  if [ "$RO_ROOTFS" = "true" ]; then
    warn "El contenedor se ejecuta con ReadOnlyRootfs=true. Debes deshabilitarlo en docker-compose."
  fi

  # Revisar si el mount está RW
  if [ -z "${RW_STATE:-}" ]; then
    RW_STATE=$(docker inspect "$CONTAINER_NAME" --format '{{range .Mounts}}{{if eq .Destination '"$SERVER_DIR"'}}{{.RW}}{{end}}{{end}}')
  fi
  if [ "$RW_STATE" = "false" ]; then
    warn "El volumen que contiene $SERVER_DIR (o server.properties) está montado en modo sólo lectura (RW=false)."
    warn "Cámbialo a RW en docker-compose o elimina ':ro' del bind mount."
  fi

  ok "Listo. Reinicia el contenedor y verifica que el error haya desaparecido."
  echo
  echo "Comandos sugeridos:"
  echo "  docker restart $CONTAINER_NAME"
}

main "$@"
