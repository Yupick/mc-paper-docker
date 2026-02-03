# Panel Web de Administración - Servidor Minecraft

Panel web para administrar el servidor Minecraft Paper con soporte para Java y Bedrock.

## Características

- ✅ Control del servidor (iniciar, detener, reiniciar)
- ✅ Monitoreo en tiempo real (CPU, memoria, estado)
- ✅ Gestión de plugins (activar/desactivar, subir, eliminar)
- ✅ Edición de whitelist y blacklist
- ✅ Edición de server.properties
- ✅ Visualización de logs en tiempo real
- ✅ Autenticación con usuario y contraseña

## Instalación

1. Instalar dependencias:
```bash
cd /home/mkd/contenedores/mc-paper/web
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

2. Configurar credenciales en `.env`:
```bash
SECRET_KEY=tu-clave-secreta-aqui
ADMIN_USERNAME=tu-usuario
ADMIN_PASSWORD=tu-contraseña
```

3. Iniciar el panel:
```bash
./start-panel.sh
```

O manualmente:
```bash
source venv/bin/activate
python app.py
```

## Acceso

- URL: http://localhost:5000
- Usuario por defecto: admin
- Contraseña por defecto: minecraft123

## Configuración para producción

Para usar en producción con Nginx/Apache:

1. Usar Gunicorn:
```bash
pip install gunicorn
gunicorn -w 4 -b 0.0.0.0:5000 app:app
```

2. Configurar proxy reverso en Nginx para panel.mc.nightslayer.com.ar

## Seguridad

- ⚠️ Cambia las credenciales por defecto
- ⚠️ Usa HTTPS en producción
- ⚠️ Configura firewall para puerto 5000
