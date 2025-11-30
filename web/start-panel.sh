#!/bin/bash

echo "========================================="
echo "Iniciando Panel Web de Administración"
echo "========================================="

cd /home/mkd/contenedores/mc-paper/web

# Verificar si existe el entorno virtual
if [ ! -d "venv" ]; then
    echo "Creando entorno virtual..."
    python3 -m venv venv
fi

# Activar entorno virtual
source venv/bin/activate

# Instalar dependencias
echo "Instalando dependencias..."
pip install -r requirements.txt

echo ""
echo "========================================="
echo "Panel web iniciado en http://localhost:5000"
echo "========================================="
echo ""
echo "Credenciales por defecto:"
echo "  Usuario: admin"
echo "  Contraseña: minecraft123"
echo ""
echo "IMPORTANTE: Cambia las credenciales en el archivo .env"
echo ""

# Iniciar aplicación
python app.py
