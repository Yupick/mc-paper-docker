# 🎮 Panel de Administración Web - Minecraft Server

Panel web moderno y completo para administrar tu servidor de Minecraft PaperMC con soporte para Java y Bedrock.

## ✨ Características

### 🎯 Funcionalidades Principales
- ✅ **Inicio/Parada/Reinicio** del servidor con un clic
- 📊 **Monitoreo en tiempo real** (CPU, RAM, estado)
- 👥 **Jugadores online** - Lista actualizada cada 10 segundos
- 🔌 **Gestión de plugins** - Subir, activar, desactivar, eliminar
- 📝 **Logs en tiempo real** - Ver lo que sucede en el servidor
- ⚙️ **Edición de server.properties** desde el navegador
- 📋 **Whitelist y Blacklist** - Gestión de jugadores permitidos/baneados
- 💾 **Sistema de backups** - Crear, descargar y restaurar backups
- 🔄 **Actualización automática** - Actualizar servidor y plugins con un botón
- 🌓 **Tema claro/oscuro** - Interfaz moderna y responsive

### 🔐 Seguridad
- Autenticación con usuario y contraseña
- Soporte para hash de contraseñas (bcrypt/pbkdf2)
- Secret key para sesiones seguras
- Control de acceso por roles

## 📦 Instalación Rápida

### 1. Requisitos Previos
```bash
# Instalar dependencias de Python
pip3 install flask flask-login python-dotenv docker --break-system-packages
```

### 2. Configurar Credenciales

Edita el archivo `.env` en `/home/mkd/contenedores/mc-paper/web/.env`:

```bash
# Credenciales de administrador
ADMIN_USERNAME=admin
ADMIN_PASSWORD=tu_contraseña_aqui

# O usa un hash de contraseña (más seguro)
ADMIN_PASSWORD_HASH=pbkdf2:sha256:600000$...

# Clave secreta para sesiones (genera una aleatoria)
SECRET_KEY=tu_clave_secreta_aqui
```

### 3. Generar Hash de Contraseña (Recomendado)

```bash
cd /home/mkd/contenedores/mc-paper/web
python3 generate_hash.py tu_contraseña_segura
```

Copia el hash generado y agrégalo al archivo `.env`:
```
ADMIN_PASSWORD_HASH=pbkdf2:sha256:600000$abc123...
```

### 4. Generar Secret Key

```bash
python3 -c "import secrets; print(secrets.token_hex(32))"
```

Copia el resultado al `.env`:
```
SECRET_KEY=resultado_aqui
```

## 🚀 Inicio del Panel Web

### Opción 1: Script Automático (Recomendado)
```bash
cd /home/mkd/contenedores/mc-paper
./start-web-panel.sh
```

### Opción 2: Manual
```bash
cd /home/mkd/contenedores/mc-paper/web
python3 app.py
```

El panel estará disponible en:
- **Local:** http://localhost:5000
- **Red local:** http://tu_ip:5000
- **Dominio:** http://mc.nightslayer.com.ar (si configuraste nginx/apache)

## 🌐 Configuración para Acceso Público

### Con Nginx (Recomendado)

1. Instalar Nginx:
```bash
sudo apt install nginx
```

2. Crear configuración:
```bash
sudo nano /etc/nginx/sites-available/minecraft-panel
```

Contenido:
```nginx
server {
    listen 80;
    server_name mc.nightslayer.com.ar;

    location / {
        proxy_pass http://127.0.0.1:5000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

3. Activar configuración:
```bash
sudo ln -s /etc/nginx/sites-available/minecraft-panel /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

4. Obtener SSL con Let's Encrypt:
```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d mc.nightslayer.com.ar
```

## 🛠️ Configuración Avanzada

### Permisos Sudoers (Necesario para funciones avanzadas)

Para permitir que el panel ejecute comandos sin pedir contraseña:

```bash
sudo visudo
```

Agrega al final:
```
mkd ALL=(ALL) NOPASSWD: /usr/bin/docker-compose
mkd ALL=(ALL) NOPASSWD: /home/mkd/contenedores/mc-paper/update.sh
mkd ALL=(ALL) NOPASSWD: /home/mkd/contenedores/mc-paper/create.sh
mkd ALL=(ALL) NOPASSWD: /home/mkd/contenedores/mc-paper/run.sh
mkd ALL=(ALL) NOPASSWD: /home/mkd/contenedores/mc-paper/stop.sh
```

### Ejecutar como Servicio Systemd

Crea `/etc/systemd/system/minecraft-panel.service`:

```ini
[Unit]
Description=Minecraft Server Web Panel
After=network.target docker.service

[Service]
Type=simple
User=mkd
WorkingDirectory=/home/mkd/contenedores/mc-paper/web
ExecStart=/usr/bin/python3 /home/mkd/contenedores/mc-paper/web/app.py
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Activar:
```bash
sudo systemctl daemon-reload
sudo systemctl enable minecraft-panel
sudo systemctl start minecraft-panel
sudo systemctl status minecraft-panel
```

## 📚 Uso del Panel

### Dashboard Principal
- **Estado del servidor:** Verde (ONLINE) / Rojo (OFFLINE)
- **Versión de PaperMC:** Se muestra debajo del estado
- **Uptime:** Tiempo que lleva corriendo el servidor
- **CPU y RAM:** Monitoreo en tiempo real
- **Controles:** Botones para iniciar, detener y reiniciar

### Jugadores Online
- Lista actualizada cada 10 segundos
- Muestra cantidad de jugadores conectados
- Nombre de cada jugador conectado

### Gestión de Plugins
- **Subir plugins:** Arrastra archivos .jar o haz clic para seleccionar
- **Activar/Desactivar:** Toggle para habilitar/deshabilitar plugins
- **Eliminar:** Botón para eliminar plugins permanentemente
- **Actualizar todos:** Actualiza GeyserMC, Floodgate, ViaVersion, etc.

### Actualizar Servidor
- **Actualizar Servidor:** Descarga la última versión de PaperMC
- **Actualizar Plugins:** Descarga las últimas versiones de todos los plugins

### Sistema de Backups
- **Crear backup:** Comprime todos los mundos en un archivo .tar.gz
- **Descargar backup:** Descarga el archivo comprimido
- **Eliminar backup:** Borra backups antiguos para liberar espacio

### Whitelist y Blacklist
- **Whitelist:** Lista de jugadores permitidos (formato JSON)
- **Blacklist:** Lista de jugadores baneados (formato JSON)
- Edita directamente en el navegador y guarda

### Logs en Tiempo Real
- Ver logs del servidor actualizados cada 10 segundos
- Botón para actualizar manualmente
- Botón para limpiar logs de la vista

### Configuración
- Editar `server.properties` directamente
- Cambios aplicados requieren reinicio del servidor

## 🔧 API Endpoints

El panel expone los siguientes endpoints:

### Estado del Servidor
- `GET /api/server/status` - Estado, CPU, RAM
- `GET /api/server/players` - Jugadores online
- `GET /api/server/version` - Versión de PaperMC
- `GET /api/server/uptime` - Tiempo de actividad
- `GET /api/server/logs` - Logs del servidor

### Controles del Servidor
- `POST /api/server/start` - Iniciar servidor
- `POST /api/server/stop` - Detener servidor
- `POST /api/server/restart` - Reiniciar servidor
- `POST /api/server/update` - Actualizar PaperMC

### Plugins
- `GET /api/plugins` - Lista de plugins
- `POST /api/plugins/upload` - Subir plugin
- `POST /api/plugins/toggle` - Activar/desactivar plugin
- `POST /api/plugins/delete` - Eliminar plugin
- `POST /api/plugins/update-all` - Actualizar todos los plugins

### Backups
- `POST /api/backup/create` - Crear backup
- `GET /api/backup/list` - Lista de backups
- `GET /api/backup/download/<filename>` - Descargar backup
- `POST /api/backup/delete` - Eliminar backup

### Whitelist/Blacklist
- `GET /api/whitelist` - Obtener whitelist
- `POST /api/whitelist` - Guardar whitelist
- `GET /api/blacklist` - Obtener blacklist
- `POST /api/blacklist` - Guardar blacklist

### Configuración
- `GET /api/config/server-properties` - Obtener server.properties
- `POST /api/config/server-properties` - Guardar server.properties

## 🐛 Solución de Problemas

### El panel no se inicia
```bash
# Verificar dependencias
pip3 list | grep -E 'flask|docker'

# Reinstalar si es necesario
pip3 install flask flask-login python-dotenv docker --break-system-packages --force-reinstall
```

### Error al conectar con Docker
```bash
# Verificar que Docker esté corriendo
sudo systemctl status docker

# Agregar usuario al grupo docker
sudo usermod -aG docker $USER
newgrp docker
```

### Error de permisos
```bash
# Verificar permisos de los archivos
ls -la /home/mkd/contenedores/mc-paper/web/

# Corregir permisos si es necesario
chmod +x /home/mkd/contenedores/mc-paper/*.sh
chown -R $USER:$USER /home/mkd/contenedores/mc-paper/
```

### El servidor no inicia/detiene desde el panel
```bash
# Configurar sudoers (ver sección "Configuración Avanzada")
sudo visudo
```

## 📊 Estructura de Archivos

```
/home/mkd/contenedores/mc-paper/
├── Dockerfile                  # Imagen Docker del servidor
├── docker-compose.yml          # Configuración de contenedor
├── create.sh                   # Script de creación inicial
├── update.sh                   # Script de actualización
├── run.sh                      # Script para iniciar
├── stop.sh                     # Script para detener
├── start-web-panel.sh          # Iniciar panel web
├── web/
│   ├── app.py                  # Aplicación Flask principal
│   ├── .env                    # Configuración y credenciales
│   ├── generate_hash.py        # Generar hash de contraseña
│   ├── MEJORAS_IMPLEMENTADAS.md # Documentación de mejoras
│   ├── templates/
│   │   ├── login.html          # Página de login
│   │   ├── dashboard.html      # Panel principal
│   │   └── dashboard.html.backup # Backup del dashboard original
│   └── static/                 # (opcional) Archivos estáticos
├── worlds/                     # Mundos del servidor
├── plugins/                    # Plugins instalados
├── logs/                       # Logs del servidor
├── backups/                    # Backups de mundos
└── config/
    └── server.properties       # Configuración del servidor
```

## 🔄 Actualización del Panel

```bash
cd /home/mkd/contenedores/mc-paper
git pull  # Si usas Git
# O reemplaza los archivos manualmente

# Reinicia el panel
sudo systemctl restart minecraft-panel
# O si lo ejecutas manualmente, detén con Ctrl+C y vuelve a iniciar
```

## 📝 Notas Importantes

1. **Backups automáticos:** Considera crear un cron job para backups periódicos
2. **Firewall:** Abre el puerto 5000 si quieres acceso remoto
3. **Seguridad:** SIEMPRE usa contraseñas seguras y hash
4. **Recursos:** El panel consume mínimos recursos (~50MB RAM)
5. **Compatibilidad:** Funciona con cualquier versión de PaperMC

## 🤝 Soporte

Si encuentras problemas:
1. Verifica los logs del panel web
2. Verifica los logs de Docker: `docker logs mc-paper`
3. Revisa la configuración en `.env`
4. Asegúrate de tener permisos de sudo configurados

## 📜 Licencia

Este panel es de código abierto y de uso libre.

---

**¡Disfruta administrando tu servidor de Minecraft!** 🎮🚀
