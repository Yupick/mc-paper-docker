#!/bin/bash

# Verificar si se pasó el flag --plugins
if [ "$1" == "--plugins" ]; then
    echo "========================================="
    echo "Actualizando solo plugins"
    echo "========================================="
    
    echo ""
    echo "[1/3] Deteniendo contenedor..."
    sudo docker-compose stop
    
    echo ""
    echo "[2/3] Descargando últimas versiones de plugins..."
    echo "Actualizando: GeyserMC, Floodgate, ViaVersion, ViaBackwards, ViaRewind"
    
    # Backup de plugins actuales
    mkdir -p plugins_backup
    cp plugins/*.jar plugins_backup/ 2>/dev/null
    echo "  ℹ️  Backup de plugins guardado en plugins_backup/"
    
    # Eliminar plugins antiguos
    rm -f plugins/Geyser-Spigot.jar plugins/floodgate-spigot.jar plugins/ViaVersion.jar plugins/ViaBackwards.jar plugins/ViaRewind.jar
    
    # Descargar GeyserMC
    echo "  → Descargando GeyserMC..."
    curl -L -o plugins/Geyser-Spigot.jar "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot" 2>/dev/null
    if [ -f plugins/Geyser-Spigot.jar ]; then
        GEYSER_SIZE=$(ls -lh plugins/Geyser-Spigot.jar 2>/dev/null | awk '{print $5}')
        if [ "$GEYSER_SIZE" != "31" ]; then
            echo "    ✅ GeyserMC actualizado ($GEYSER_SIZE)"
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
            echo "    ✅ Floodgate actualizado ($FLOODGATE_SIZE)"
        else
            echo "    ❌ Error descargando Floodgate (archivo corrupto)"
            rm -f plugins/floodgate-spigot.jar
        fi
    fi
    
    # Descargar ViaVersion
    echo "  → Descargando ViaVersion..."
    VIAVERSION_URL=$(curl -s https://api.github.com/repos/ViaVersion/ViaVersion/releases/latest | grep "browser_download_url.*ViaVersion-.*\.jar" | cut -d '"' -f 4)
    curl -L -o plugins/ViaVersion.jar "$VIAVERSION_URL" 2>/dev/null
    echo "    ✅ ViaVersion actualizado ($(ls -lh plugins/ViaVersion.jar 2>/dev/null | awk '{print $5}'))"
    
    # Descargar ViaBackwards
    echo "  → Descargando ViaBackwards..."
    VIABACKWARDS_URL=$(curl -s https://api.github.com/repos/ViaVersion/ViaBackwards/releases/latest | grep "browser_download_url.*ViaBackwards-.*\.jar" | cut -d '"' -f 4)
    curl -L -o plugins/ViaBackwards.jar "$VIABACKWARDS_URL" 2>/dev/null
    echo "    ✅ ViaBackwards actualizado ($(ls -lh plugins/ViaBackwards.jar 2>/dev/null | awk '{print $5}'))"
    
    # Descargar ViaRewind
    echo "  → Descargando ViaRewind..."
    VIAREWIND_URL=$(curl -s https://api.github.com/repos/ViaVersion/ViaRewind/releases/latest | grep "browser_download_url.*ViaRewind-.*\.jar" | cut -d '"' -f 4)
    curl -L -o plugins/ViaRewind.jar "$VIAREWIND_URL" 2>/dev/null
    echo "    ✅ ViaRewind actualizado ($(ls -lh plugins/ViaRewind.jar 2>/dev/null | awk '{print $5}'))"
    
    echo ""
    echo "[3/3] Reiniciando contenedor..."
    sudo docker-compose start
    
    echo ""
    echo "========================================="
    echo "✅ Plugins actualizados exitosamente"
    echo "========================================="
    echo ""
    echo "Para ver los logs: ./run.sh"
    
else
    echo "========================================="
    echo "Actualizando servidor Minecraft Paper"
    echo "========================================="
    
    echo ""
    echo "[1/5] Deteniendo contenedor..."
    sudo docker-compose down
    
    if [ $? -ne 0 ]; then
        echo "⚠️  Advertencia: No se pudo detener el contenedor (puede que no esté corriendo)"
    fi
    
    echo ""
    echo "[2/5] Eliminando imagen actual..."
    sudo docker rmi mc-paper_minecraft 2>/dev/null || echo "⚠️  Imagen no encontrada, continuando..."
    
    echo ""
    echo "[3/5] Construyendo nueva imagen (descargando última versión de PaperMC)..."
    sudo docker-compose build --no-cache
    
    if [ $? -ne 0 ]; then
        echo "❌ Error al construir la nueva imagen"
        exit 1
    fi
    
    echo ""
    echo "[4/5] Asegurando directorios necesarios..."
    mkdir -p worlds plugins resourcepacks config logs
    
    echo ""
    echo "[5/5] Reiniciando contenedor..."
    sudo docker-compose up -d
    
    if [ $? -ne 0 ]; then
        echo "❌ Error al reiniciar el contenedor"
        exit 1
    fi
    
    echo ""
    echo "========================================="
    echo "✅ Servidor actualizado exitosamente"
    echo "========================================="
    echo ""
    echo "El servidor ahora está corriendo con la última versión de PaperMC"
    echo ""
    echo "Nota: Los plugins NO se actualizaron automáticamente."
    echo "Para actualizar solo los plugins, ejecuta: ./update.sh --plugins"
    echo ""
    echo "Para ver los logs: ./run.sh"
fi
