#!/bin/bash

# Script para cambiar la versión del servidor a una específica
# Uso: ./change-server-version.sh 1.20.4

if [ -z "$1" ]; then
    echo "❌ Error: Debes especificar una versión"
    echo ""
    echo "Uso: ./change-server-version.sh VERSION"
    echo ""
    echo "Ejemplos:"
    echo "  ./change-server-version.sh 1.20.4"
    echo "  ./change-server-version.sh 1.19.4"
    echo "  ./change-server-version.sh 1.18.2"
    echo ""
    echo "Versiones disponibles:"
    curl -s https://api.papermc.io/v2/projects/paper | jq -r '.versions[] | select(test("^[0-9]+\\.[0-9]+(\\.[0-9]+)?$"))' | tail -20
    exit 1
fi

TARGET_VERSION="$1"

echo "========================================="
echo "🔄 Cambiar Versión del Servidor"
echo "========================================="
echo ""
echo "Versión objetivo: $TARGET_VERSION"
echo ""

# Verificar que la versión existe
echo "🔍 Verificando que la versión existe..."
if ! curl -s "https://api.papermc.io/v2/projects/paper/versions/$TARGET_VERSION" | grep -q "builds"; then
    echo "❌ Error: La versión $TARGET_VERSION no existe en PaperMC"
    echo ""
    echo "Versiones disponibles:"
    curl -s https://api.papermc.io/v2/projects/paper | jq -r '.versions[] | select(test("^[0-9]+\\.[0-9]+(\\.[0-9]+)?$"))' | tail -20
    exit 1
fi

echo "✅ Versión encontrada"
echo ""

# Advertencia
echo "⚠️  ADVERTENCIA:"
echo "   • Esto detendrá el servidor actual"
echo "   • Se descargará PaperMC $TARGET_VERSION"
echo "   • Tus mundos y configuración se conservan"
echo "   • Los plugins pueden no ser compatibles"
echo ""
read -p "¿Continuar? (s/n): " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[SsYy]$ ]]; then
    echo "Cancelado"
    exit 0
fi

# Crear backup del Dockerfile
cp Dockerfile Dockerfile.backup

# Modificar Dockerfile para usar versión específica
cat > Dockerfile << EOF
FROM eclipse-temurin:21-jdk-jammy

# Instalar dependencias necesarias
RUN apt-get update && \\
    apt-get install -y curl jq && \\
    apt-get clean && \\
    rm -rf /var/lib/apt/lists/*

# Crear directorio de trabajo
WORKDIR /server

# Descargar versión específica de PaperMC: $TARGET_VERSION
RUN MINECRAFT_VERSION="$TARGET_VERSION" && \\
    echo "Descargando PaperMC versión \${MINECRAFT_VERSION}..." && \\
    BUILD=\$(curl -s https://api.papermc.io/v2/projects/paper/versions/\${MINECRAFT_VERSION}/builds | jq -r '.builds[-1].build') && \\
    curl -o paper.jar https://api.papermc.io/v2/projects/paper/versions/\${MINECRAFT_VERSION}/builds/\${BUILD}/downloads/paper-\${MINECRAFT_VERSION}-\${BUILD}.jar && \\
    echo "PaperMC descargado: \$(ls -lh paper.jar)"

# Aceptar EULA automáticamente
RUN echo "eula=true" > eula.txt

# Exponer puertos
EXPOSE 25565/tcp
EXPOSE 19132/udp

# Comando de inicio
CMD ["java", "-Xms1G", "-Xmx2G", "-jar", "paper.jar", "--nogui"]
EOF

echo "✅ Dockerfile modificado para versión $TARGET_VERSION"
echo ""

# Reconstruir
echo "🔨 Reconstruyendo contenedor..."
sudo docker-compose down
sudo docker-compose build --no-cache

if [ $? -ne 0 ]; then
    echo "❌ Error al construir"
    echo "Restaurando Dockerfile original..."
    mv Dockerfile.backup Dockerfile
    exit 1
fi

echo "✅ Contenedor reconstruido"
echo ""

# Iniciar
echo "🚀 Iniciando servidor..."
sudo docker-compose up -d

echo ""
echo "========================================="
echo "✅ Servidor actualizado a versión $TARGET_VERSION"
echo "========================================="
echo ""
echo "Para ver los logs: ./run.sh"
echo "Para volver a la última versión: ./update.sh"
