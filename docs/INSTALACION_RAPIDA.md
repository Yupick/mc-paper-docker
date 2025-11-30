# 🚀 Instalación Rápida en Servidor Nuevo

Si estás configurando el servidor por primera vez y no tienes Python/Docker instalado, sigue estos pasos:

## 📦 Paso 1: Instalar todas las dependencias

Ejecuta el script de instalación automática:

```bash
./install-dependencies.sh
```

Este script instalará:
- ✅ Python3 y pip
- ✅ Docker y Docker Compose
- ✅ Entorno virtual de Python
- ✅ Todas las dependencias necesarias (Flask, Docker SDK, etc.)

⚠️ **IMPORTANTE:** Si Docker se instaló por primera vez, **cierra sesión y vuelve a entrar** para aplicar los permisos.

## 🎮 Paso 2: Crear el servidor de Minecraft

```bash
./create.sh
```

Esto descargará PaperMC, plugins (GeyserMC, Floodgate, ViaVersion) y creará el contenedor Docker.

## 🌐 Paso 3: Iniciar el panel web

```bash
./start-web-panel.sh
```

El script:
- ✅ Detecta y activa automáticamente el entorno virtual
- ✅ Crea el archivo `.env` si no existe
- ✅ Instala dependencias faltantes
- ✅ Inicia el servidor web en el puerto 5000

Accede al panel en:
- Local: http://localhost:5000
- Remoto: http://TU_IP_SERVIDOR:5000

**Credenciales por defecto:**
- Usuario: `admin`
- Contraseña: `minecraft123`

⚠️ **Cambia la contraseña después del primer inicio:**
```bash
cd web
python3 generate_hash.py tu_nueva_contraseña
# Copia el hash generado al archivo .env
```

## 🔧 Configuración del Firewall (si es necesario)

Si no puedes acceder al panel desde fuera del servidor:

```bash
sudo ufw allow 5000/tcp    # Panel web
sudo ufw allow 25565/tcp   # Minecraft Java
sudo ufw allow 19132/udp   # Minecraft Bedrock
```

## 📝 Comandos Útiles

```bash
./run.sh          # Iniciar servidor Minecraft
./stop.sh         # Detener servidor
./update.sh       # Actualizar PaperMC
./update.sh --plugins  # Actualizar solo plugins
```

## 🆘 Solución de Problemas

### Error: "pip3: command not found"
**Solución:** Ejecuta `./install-dependencies.sh` primero

### Error: "ModuleNotFoundError: No module named 'flask'"
**Solución:** El entorno virtual no está activado. Ejecuta:
```bash
source .venv/bin/activate
cd web
python3 app.py
```

O simplemente usa:
```bash
./start-web-panel.sh
```

### Error: "Cannot connect to Docker daemon"
**Solución:** 
1. Verifica que Docker esté corriendo: `sudo systemctl status docker`
2. Si acabas de instalar Docker, cierra sesión y vuelve a entrar
3. Verifica que tu usuario esté en el grupo docker: `groups`

### El panel web no es accesible desde fuera
**Solución:**
```bash
# Verifica que el puerto esté abierto
sudo ufw status

# Abre el puerto si está cerrado
sudo ufw allow 5000/tcp
```

## 📂 Estructura de Archivos

```
mc-paper/
├── .venv/                    # Entorno virtual de Python (creado automáticamente)
├── web/                      # Panel web
│   ├── app.py               # Aplicación Flask
│   ├── .env                 # Configuración (creado automáticamente)
│   └── templates/           # Plantillas HTML
├── worlds/                   # Mundos de Minecraft
├── plugins/                  # Plugins instalados
├── backups/                  # Backups de mundos
├── install-dependencies.sh   # Instalar todo lo necesario
├── start-web-panel.sh       # Iniciar panel web
├── create.sh                # Crear servidor
└── run.sh                   # Ejecutar servidor
```

## ✨ ¡Listo!

Tu servidor está configurado y listo para usar con rutas relativas que funcionan en cualquier ubicación.
