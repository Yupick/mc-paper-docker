#!/bin/bash
# cleanup-docker.sh
# Script parametrizable para limpiar contenedores, redes y volúmenes de un proyecto Docker Compose

set -euo pipefail

PROJECT_NAME="${1:-minecraft-paper}"   # Si no pasás argumento, usa minecraft-paper por defecto

echo "🔎 Buscando contenedores del proyecto $PROJECT_NAME..."

containers=$(docker ps -a --filter "name=${PROJECT_NAME}" --format "{{.ID}}")

if [ -n "$containers" ]; then
  echo "🛑 Deteniendo y eliminando contenedores..."
  for c in $containers; do
    echo " -> Eliminando contenedor $c"
    docker rm -f "$c" || echo "No se pudo eliminar $c"
  done
else
  echo "✅ No hay contenedores activos con nombre $PROJECT_NAME"
fi

echo "🔎 Buscando redes asociadas..."
networks=$(docker network ls --filter "name=${PROJECT_NAME}_default" --format "{{.ID}}")

if [ -n "$networks" ]; then
  echo "🗑️ Eliminando redes..."
  for n in $networks; do
    echo " -> Revisando red $n"
    # Forzar desconexión de endpoints antes de eliminar
    endpoints=$(docker network inspect "$n" --format '{{range $k,$v := .Containers}}{{$k}} {{end}}')
    if [ -n "$endpoints" ]; then
      echo " -> Desconectando endpoints: $endpoints"
      for e in $endpoints; do
        docker network disconnect -f "$n" "$e" || echo "No se pudo desconectar $e"
      done
    fi
    echo " -> Eliminando red $n"
    docker network rm "$n" || echo "No se pudo eliminar $n"
  done
else
  echo "✅ No hay redes bloqueadas con nombre ${PROJECT_NAME}_default"
fi

echo "🔎 Buscando volúmenes huérfanos..."
volumes=$(docker volume ls --filter "name=${PROJECT_NAME}" --format "{{.Name}}")

if [ -n "$volumes" ]; then
  echo "🗑️ Eliminando volúmenes..."
  for v in $volumes; do
    echo " -> Eliminando volumen $v"
    docker volume rm "$v" || echo "No se pudo eliminar $v"
  done
else
  echo "✅ No hay volúmenes huérfanos con nombre $PROJECT_NAME"
fi

echo "🎉 Limpieza completa."

