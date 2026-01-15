# 🎮 Minecraft PaperMC Server - Docker + Panel de Administración Web

[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![PaperMC](https://img.shields.io/badge/PaperMC-1.21.4--1.21.10-orange?logo=minecraft)](https://papermc.io/)
[![Flask](https://img.shields.io/badge/Flask-3.0.0-000000?logo=flask&logoColor=white)](https://flask.palletsprojects.com/)
[![Python](https://img.shields.io/badge/Python-3.8+-3776AB?logo=python&logoColor=white)](https://www.python.org/)
[![Bootstrap](https://img.shields.io/badge/Bootstrap-5.3-7952B3?logo=bootstrap&logoColor=white)](https://getbootstrap.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

> **Servidor Minecraft PaperMC completo con Docker y panel de administración web profesional**

Gestiona tu servidor Minecraft de forma moderna y eficiente con interfaz web completa, backups automáticos, monitoreo en tiempo real y más de 45 endpoints API.

---

## 📑 Tabla de Contenidos

- [Características](#-características)
- [Capturas de Pantalla](#-capturas-de-pantalla)
- [Inicio Rápido](#-inicio-rápido)
- [Instalación Detallada](#-instalación-detallada)
- [Panel de Administración Web](#-panel-de-administración-web)
- [Arquitectura](#-arquitectura)
- [API REST](#-api-rest)
- [Gestión del Servidor](#-gestión-del-servidor)
- [Seguridad](#-seguridad)
- [Solución de Problemas](#-solución-de-problemas)
- [Contribuir](#-contribuir)
- [Licencia](#-licencia)

---

## ✨ Características

### 🎯 Servidor Minecraft
- ✅ **PaperMC** 1.21.4 - 1.21.10 (configurable)
- ✅ **Docker** con persistencia de datos
- ✅ **Auto-reinicio** en caso de caída
- ✅ **Backups automáticos** programables
- ✅ **Scripts de gestión** (start, stop, restart, update)
- ✅ **Logs centralizados**
- ✅ **Configuración flexible** mediante variables de entorno

### 🌐 Panel de Administración Web
- ✅ **Dashboard moderno** con Bootstrap 5
- ✅ **100% funcionalidades** implementadas (20/20)
- ✅ **24 endpoints API** REST completos
- ✅ **Autenticación segura** con hash de contraseñas
- ✅ **Tema oscuro** profesional (#1a1d29)
- ✅ **Responsive design** mobile-friendly
- ✅ **Actualizaciones optimizadas** (configurables 1-60s)
- ✅ **Cambio obligatorio de contraseña** en primer login
- ✅ **Email de recuperación** opcional

### 📊 Monitoreo y Estadísticas
- ✅ **CPU/RAM/Jugadores** en tiempo real
- ✅ **TPS (Ticks Per Second)** con indicadores de salud
- ✅ **Gráficos históricos** (Chart.js) de 24 horas
- ✅ **Chat en vivo** visualización y envío
- ✅ **Logs del servidor** actualizados cada 10s

### 🔧 Gestión Completa
- ✅ **Configuración server.properties** (edición rápida y completa)
- ✅ **Gestión de plugins** (upload, delete, reload)
- ✅ **Gestión de jugadores** (kick, ban, gamemode)
- ✅ **Whitelist y Operadores** (OPs)
- ✅ **Sistema de backups** (crear, restaurar, descargar, auto-cleanup)
- ✅ **Consola web interactiva** con comandos rápidos
- ✅ **Editor de archivos** YAML/JSON/Properties
- ✅ **Multi-Mundos** con cambio en caliente

### 🌍 Sistema Multi-Mundo (NUEVO)
- ✅ **Crear mundos ilimitados** con configuración independiente
- ✅ **Cambio en caliente** sin detener el servidor
- ✅ **Duplicar mundos** con un click
- ✅ **Backups individuales** por mundo con retención configurable
- ✅ **Configuración por mundo** (server.properties independientes)
- ✅ **Arquitectura symlink** (worlds/active → worlds/{slug}/)
- ✅ **Metadata JSON** con información de cada mundo
- ✅ **Script de migración** desde modo single-world

### ⚡ Optimización de Rendimiento (NUEVO)
- ✅ **Polling dinámico** con intervalos configurables (1-60s)
- ✅ **Page Visibility API** pausa automática cuando tab oculto
- ✅ **Reducción de 78%** en solicitudes RCON (potencial)
- ✅ **Configuración per-endpoint** (refresh, logs, TPS)
- ✅ **Cache TTL** configurable
- ✅ **Status UI** (Active/Paused)

### 🎯 Sistema de Base de Datos SQLite (FASE 3 - COMPLETADO)
- ✅ **Migración completa de JSON a SQLite** - 30 tablas implementadas
- ✅ **Base de datos universal** (`config/data/universal.db`) - Datos compartidos
- ✅ **Base de datos local por mundo** (`worlds/{world}/data/{world}-rpg.db`)
- ✅ **DatabaseManager.java** - Gestión de BD universal
- ✅ **WorldDatabaseManager.java** - Gestión de BDs locales por mundo
- ✅ **Auto-creación de tablas** al iniciar plugin
- ✅ **17 Managers migrados a SQL** - PlayerManager, QuestManager, NPCManager, etc.
- ✅ **Panel web de visualización** - Ver ambas BDs en tiempo real
- ✅ **Índices optimizados** - Queries eficientes
- ✅ **Tests de integración** - Validación completa
- 📄 **Documentación:** Ver [docs/MIGRACION_SQLITE.md](docs/MIGRACION_SQLITE.md)

### 📁 Reorganización de Directorios (COMPLETADO)
- ✅ **Raíz limpia** - Solo 7 scripts principales + README
- ✅ **Carpeta `scripts/`** - 24 scripts de utilidades y mantenimiento
- ✅ **Carpeta `test/`** - 6 archivos de pruebas y testing
- ✅ **Carpeta `config/`** - Estructura normalizada con .example
- ✅ **Carpeta `docs/`** - Toda documentación centralizada

---

## 📸 Capturas de Pantalla

### Dashboard Principal
```
┌─────────────────────────────────────────────────────────┐
│  Panel Minecraft - Administración Completa    🌙 Logout │
├──────────┬──────────────────────────────────────────────┤
│          │  📊 Dashboard                                │
│  📊 Dash │  ┌─────────────┬─────────────┬──────────────┐│
│  ⚙️ Conf │  │ Estado: ON  │ CPU: 45%    │ RAM: 2.1GB  ││
│  👥 Jugs │  │ TPS: 20.0   │ Uptime: 5h  │ Players: 3/20││
│  💻 Cons │  └─────────────┴─────────────┴──────────────┘│
│  🔌 Plug │                                              │
│  📁 Arch │  [Gráfico de CPU/RAM/Jugadores - 24h]       │
│  🌍 Mund │                                              │
│  💾 Back │  [Logs en tiempo real]                      │
│  📋 Whit │                                              │
│  👑 OPs  │                                              │
│  📈 Stat │                                              │
└──────────┴──────────────────────────────────────────────┘
```

### Gestión de Plugins
```
┌─────────────────────────────────────────────────────────┐
│  🔌 Gestión de Plugins                                  │
├─────────────────────────────────────────────────────────┤
│  [Drag & Drop o Click para Subir .jar]                 │
├─────────────────────────────────────────────────────────┤
│  📦 EssentialsX          15.2 MB    [Delete] [Reload]   │
│  📦 WorldEdit            8.4 MB     [Delete] [Reload]   │
│  📦 Vault                2.1 MB     [Delete] [Reload]   │
└─────────────────────────────────────────────────────────┘
```

---

## 🚀 Inicio Rápido

### Prerrequisitos
- **Docker** instalado y en ejecución
- **Python 3.8+** con pip
- **4GB RAM** mínimo (recomendado 8GB)
- **10GB espacio** en disco

### Instalación en 3 Minutos (Automática)

```bash
# 1. Clonar repositorio
git clone https://github.com/Yupick/mc-paper-docker.git
cd mc-paper-docker

# 2. Ejecutar instalación automática
chmod +x create.sh
./create.sh

# 3. Configurar credenciales del panel web
cd web
nano .env  # Editar ADMIN_USERNAME y ADMIN_PASSWORD

# 4. Iniciar servicios
docker-compose up -d              # Servidor Minecraft
cd web && ./start-web-panel.sh    # Panel Web

# 5. Acceder
# Panel Web: http://localhost:5000
# Minecraft: localhost:25565
```

**El script `create.sh` automáticamente:**
- ✅ Crea estructura de directorios (worlds/, backups/, config/, web/models, web/services)
- ✅ Descarga plugins esenciales (EssentialsX, Vault, LuckPerms)
- ✅ Genera configuración inicial (server.properties, backup_config.json, panel_config.json)
- ✅ Acepta EULA de Minecraft
- ✅ Construye imagen Docker optimizada
- ✅ Muestra comandos disponibles

### Primer Login
1. Acceder a `http://localhost:5000`
2. Usuario: `admin` (o el que configuraste)
3. Contraseña: la que configuraste en `.env`
4. **Aparecerá modal de cambio de contraseña**
5. Establecer nueva contraseña segura (mínimo 8 caracteres)
6. Opcionalmente agregar email de recuperación
7. Re-login con nueva contraseña

---

## 📦 Instalación Detallada

### 1. Servidor Minecraft con Docker

#### Estructura de Directorios
```
mc-paper-docker/
├── docker-compose.yml          # Configuración Docker
├── .env                        # Variables de entorno
├── README.md                   # Documentación principal
├── create.sh                   # Script de instalación completa
├── uninstall.sh                # Script de desinstalación
├── run.sh                      # Iniciar servidor
├── stop.sh                     # Detener servidor
├── start-web-panel.sh          # Iniciar panel web
├── stop-web-panel.sh           # Detener panel web
├── restart-web-panel.sh        # Reiniciar panel web
├── status-web-panel.sh         # Estado del panel web
├── logs-web-panel.sh           # Ver logs del panel web
├── scripts/                    # Scripts auxiliares
│   ├── migrate-to-multiworld.sh    # Migrar a sistema multi-mundo
│   ├── rollback-multiworld.sh      # Revertir migración
│   ├── run-tests.sh                # Testing de integración
│   ├── verify-panel.sh             # Verificar instalación
│   ├── update.sh                   # Actualizar servidor
│   └── ...                         # Otros scripts auxiliares
├── plugins/                    # Plugins de Minecraft
├── worlds/                     # Sistema multi-mundo
│   ├── active/                 # Symlink → mundo activo
│   ├── world-default/          # Mundo por defecto
│   │   ├── metadata.json       # Metadata del mundo
│   │   ├── world/              # Overworld
│   │   ├── world_nether/       # Nether
│   │   └── world_the_end/      # End
│   └── {slug}/                 # Otros mundos (creative, survival, etc.)
├── config/                     # Archivos de configuración
│   ├── server.properties       # Configuración global
│   ├── backup_config.json      # Config de backups automáticos
│   ├── panel_config.json       # Config de rendimiento del panel
│   └── worlds.json             # Config de mundos (opcional)
├── backups/                    # Backups del servidor
│   └── worlds/                 # Backups por mundo
│       └── {slug}/             # Backups de cada mundo
│           └── backup_*.tar.gz
├── logs/                       # Logs del servidor
├── docs/                       # 📚 Documentación del servidor
│   ├── INSTALACION_RAPIDA.md
│   ├── CAMBIOS_PERSISTENCIA.md
│   └── setup-minecraft.md
├── docs/                       # 📚 Documentación completa
│   ├── BACKUP_SYSTEM.md        # Sistema de backups
│   ├── BACKUP_CONFIG.md        # Configuración de backups
│   ├── PERFORMANCE_OPTIMIZATION.md # Optimización
│   ├── GUIA_MULTIMUNDOS.md     # Guía completa multi-mundo
│   └── ...                     # Más documentación
└── web/                        # Panel de administración
    ├── app.py                  # Backend Flask (1874 líneas)
    ├── start-web-panel.sh      # Iniciar panel
    ├── stop-web-panel.sh       # Detener panel
    ├── .env                    # Configuración panel
    ├── requirements.txt        # Dependencias Python
    ├── models/                 # Modelos de datos
    │   ├── world.py            # Clase World (247 líneas)
    │   └── world_manager.py    # Clase WorldManager (404 líneas)
    ├── services/               # Servicios de negocio
    │   └── backup_service.py   # BackupService (309 líneas)
    ├── templates/              # Templates HTML
    │   ├── login.html
    │   └── dashboard_v2.html   # Dashboard completo (1196 líneas)
    ├── static/                 # CSS/JS
    │   └── dashboard.js        # Frontend logic (1816 líneas)
    └── docs/                   # 📚 Documentación del panel
        ├── GUIA_COMPLETA.md   # Guía completa de funcionalidades
        ├── VIRTUALMIN-CONFIG.md
        ├── PANEL_README.md
        └── README.md
```

#### Configuración `.env` del Servidor
```env
# Versión de Minecraft
MINECRAFT_VERSION=1.21.4

# Recursos
MEMORY=4G
CPU_COUNT=2

# Puerto del servidor
MINECRAFT_PORT=25565

# Configuración del mundo
LEVEL_NAME=world
GAMEMODE=survival
DIFFICULTY=normal
MAX_PLAYERS=20
VIEW_DISTANCE=10
ENABLE_COMMAND_BLOCK=false
PVP=true
ONLINE_MODE=true
MOTD=Servidor Minecraft con Docker y Panel Web
```

#### Iniciar Servidor
```bash
./start-server.sh
```

**El script automáticamente:**
- ✅ Verifica Docker
- ✅ Crea directorios necesarios
- ✅ Descarga PaperMC
- ✅ Inicia contenedor
- ✅ Muestra logs en tiempo real

---

### 2. Panel de Administración Web

#### Instalación de Dependencias
```bash
cd web
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

**Dependencias:**
- Flask 3.0.0
- Flask-Login
- python-dotenv
- docker (SDK de Python)
- Werkzeug

#### Configuración `.env` del Panel
```env
# Credenciales de administrador
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin123  # Será reemplazada en primer login

# Clave secreta de Flask (generar aleatoria)
SECRET_KEY=tu_clave_secreta_muy_aleatoria_y_larga

# Configuración del servidor
DOCKER_CONTAINER_NAME=mc-paper
MINECRAFT_DIR=/home/mkd/contenedores/mc-paper
```

#### Generar Secret Key Segura
```bash
python3 -c "import secrets; print(secrets.token_hex(32))"
```

#### Iniciar Panel Web
```bash
./start-web-panel.sh
```

El panel se iniciará en `http://localhost:5000`

#### Modo Daemon (Background)
```bash
# Iniciar en background
nohup python3 app.py > panel.log 2>&1 &

# Ver logs
tail -f panel.log

# Detener
./stop-web-panel.sh
```

---

## 🌐 Panel de Administración Web

### Secciones del Panel

#### 1. 📊 Dashboard
- Estado del servidor (online/offline)
- Uso de CPU y RAM
- TPS (rendimiento)
- Jugadores conectados
- Controles (start, stop, restart)
- Logs en tiempo real
- Versión y uptime

#### 2. ⚙️ Configuración
- **Edición rápida:** Formulario con 6 propiedades principales
- **Edición completa:** Editor de texto raw de server.properties
- Propiedades: dificultad, gamemode, max-players, view-distance, PVP, whitelist

#### 3. 👥 Jugadores
- Lista de jugadores online (actualización cada 10s)
- **Acciones:**
  - Kick con razón
  - Ban con razón
  - Cambiar gamemode (survival, creative, adventure, spectator)
- **Chat en vivo:**
  - Ver mensajes del servidor
  - Enviar mensajes como servidor

#### 4. 💻 Consola
- Ejecutar cualquier comando de Minecraft
- **Comandos rápidos:**
  - `list` - Jugadores conectados
  - `save-all` - Guardar mundos
  - `stop` - Detener servidor
  - `tps` - Ver rendimiento
  - `version` - Ver versión
- Output en tiempo real

#### 5. 🔌 Plugins
- **Listar plugins:** Con nombre, tamaño, versión, estado
- **Subir plugins:** Drag & drop de archivos .jar (max 50MB)
- **Eliminar plugins:** Con confirmación
- **Reload plugins:** Recargar sin reiniciar servidor

#### 6. 📁 Editor de Archivos
- Navegación por árbol de directorios
- Edición de YAML, JSON, Properties
- Syntax highlighting básico
- Backup automático antes de guardar (.backup)
- Validación de sintaxis

#### 7. 🌍 Mundos (Sistema Multi-Mundo)
- **Listar mundos:** Grid responsive con tarjetas
- **Crear mundo:** Modal con configuración (nombre, slug, descripción, gamemode, dificultad, PVP, semilla)
- **Activar mundo:** Cambio en caliente con backup automático pre-switch
- **Duplicar mundo:** Copia completa de mundo existente
- **Editar configuración:** server.properties independiente por mundo
- **Backup individual:** Crear/restaurar backups por mundo
- **Eliminar mundo:** Con confirmación de seguridad
- **Información detallada:** Tamaño, dimensiones, fecha de creación, último acceso
- **Indicador de activo:** Badge verde en mundo actualmente en uso

#### 8. 💾 Backups (Sistema Avanzado)
- **Backups por mundo:** Almacenamiento en `backups/worlds/{slug}/`
- **Crear backup:** Compresión tar.gz con metadata JSON
- **Listar backups:** Con fecha, tamaño, tipo (manual/auto), nombre
- **Restaurar backup:** Con backup de seguridad pre-restauración automático
- **Descargar backups:** Descarga directa de archivos .tar.gz
- **Eliminar backups:** Con confirmación de seguridad
- **Auto-cleanup:** Retención configurable (por defecto: 5 backups automáticos)
- **Backups automáticos:** Al cambiar de mundo (configurable)
- **Metadata tracking:** Información completa de cada backup (fecha, tamaño, mundo, tipo)

**Configuración de Backups:**
- **Toggle auto-backup:** Activar/desactivar backups automáticos al cambiar mundo
- **Retención:** Configurar cantidad de backups automáticos a mantener (1-50)
- **Panel de configuración:** Card dedicado en sección Backups

#### 9. 📋 Whitelist
- Ver jugadores en whitelist
- Editar JSON completo
- Validación de sintaxis
- Formato: `[{"uuid": "...", "name": "..."}]`

#### 10. 👑 Operadores (OPs)
- Ver lista de operadores
- Agregar nuevos operadores
- Remover operadores
- Ver nivel de permisos (1-4)

#### 11. 📈 Estadísticas
- **Gráfico combinado:** CPU, RAM, Jugadores (Chart.js)
- **Periodo:** Últimas 24 horas
- **Actualización:** Automática cada minuto
- **Almacenamiento:** JSON local

#### 12. ⚡ Rendimiento (Optimización de Polling)
- **Configuración de intervalos:** Refresh (1-60s), Logs (5-120s), TPS (5-120s)
- **Presets rápidos:** 6 opciones predefinidas por endpoint
- **Pausa automática:** Toggle para pausar cuando tab oculto (Page Visibility API)
- **Indicador de estado:** Badge "Active" (verde) / "Paused" (amarillo)
- **Reducción de RCON:** Hasta 78% menos solicitudes con configuración óptima
- **Cache configurable:** TTL de 1-30 segundos
- **Panel dedicado:** Card en sección Configuración

---

## 🏗️ Arquitectura

### Stack Tecnológico

#### Backend
- **Flask 3.0.0** - Framework web Python
- **Docker SDK** - Comunicación con contenedor Docker
- **RCON** - Comandos de Minecraft
- **Flask-Login** - Autenticación y sesiones
- **Werkzeug** - Seguridad (password hashing)

#### Frontend
- **Bootstrap 5.3** - UI Framework
- **Bootstrap Icons** - Iconografía
- **Chart.js 4.4** - Gráficos y estadísticas
- **Vanilla JavaScript ES6+** - Lógica frontend
- **Fetch API** - Comunicación con backend

#### Infraestructura
- **Docker** - Contenedorización del servidor Minecraft
- **PaperMC** - Servidor optimizado de Minecraft
- **Linux (Ubuntu/Debian)** - Sistema operativo

### Flujo de Datos

```
Usuario → Dashboard (HTML/JS)
    ↓
Fetch API (AJAX)
    ↓
Flask Backend (app.py)
    ↓
Docker SDK / RCON / File System
    ↓
Contenedor Minecraft / Archivos de Configuración
    ↓
Respuesta JSON
    ↓
Actualización UI en Tiempo Real
```

### Seguridad

#### Autenticación
- **Flask-Login** para gestión de sesiones
- **Password hashing** con pbkdf2_sha256
- **Secret key** para firmar sesiones
- **@login_required** en todas las rutas sensibles

#### Validaciones
- **Tipos de archivo:** Solo .jar para plugins
- **Tamaño máximo:** 50MB por upload
- **Path traversal:** Verificación de rutas seguras
- **Backup automático:** Antes de modificaciones críticas

#### Cambio Obligatorio de Contraseña
```python
# Al detectar contraseña sin hash:
1. Modal bloqueante al cargar dashboard
2. Formulario con validaciones:
   - Contraseña mínimo 8 caracteres
   - Confirmación de contraseña
   - Email opcional
3. Actualizar .env con hash
4. Eliminar contraseña plana
5. Logout automático
6. Re-login con nueva contraseña
```

---

## 🔌 API REST

### Documentación Completa: 24 Endpoints

**Distribución:**
- 8 Endpoints de Servidor (status, control, logs, players, TPS, command, chat)
- 8 Endpoints de Mundos (list, get, create, activate, delete, duplicate, config)
- 4 Endpoints de Backups (list, create, restore, delete)
- 2 Endpoints de Configuración de Backups (get, update)
- 2 Endpoints de Configuración del Panel (get, update)

Ver documentación detallada en: [`web/docs/GUIA_COMPLETA.md`](web/docs/GUIA_COMPLETA.md)

### Endpoints Principales

#### Servidor (8 endpoints)
```http
GET  /api/server/status       # Estado: running, cpu, memory, uptime
GET  /api/server/logs         # Últimos 50 logs
GET  /api/server/players      # Jugadores online
GET  /api/server/tps          # Ticks per second (1m, 5m, 15m)
GET  /api/server/chat         # Mensajes del chat
POST /api/server/start        # Iniciar servidor
POST /api/server/stop         # Detener servidor
POST /api/server/restart      # Reiniciar servidor
POST /api/server/command      # Ejecutar comando
```

#### Mundos (8 endpoints)
```http
GET  /api/worlds                   # Listar todos los mundos
GET  /api/worlds/<slug>            # Obtener mundo específico
POST /api/worlds                   # Crear nuevo mundo
POST /api/worlds/<slug>/activate   # Activar mundo (cambio en caliente)
DELETE /api/worlds/<slug>          # Eliminar mundo
POST /api/worlds/<slug>/duplicate  # Duplicar mundo
GET  /api/worlds/<slug>/config     # Obtener configuración (server.properties)
PUT  /api/worlds/<slug>/config     # Actualizar configuración
```

#### Backups por Mundo (4 endpoints)
```http
GET  /api/worlds/<slug>/backups         # Listar backups de un mundo
POST /api/worlds/<slug>/backup          # Crear backup de un mundo
POST /api/worlds/<slug>/restore         # Restaurar backup de un mundo
DELETE /api/backups/<filename>          # Eliminar backup específico
```

#### Configuración de Backups (2 endpoints)
```http
GET  /api/backup-config           # Obtener configuración de backups
PUT  /api/backup-config           # Actualizar configuración (auto_backup, retention)
```

#### Configuración del Panel (2 endpoints)
```http
GET  /api/panel-config            # Obtener configuración de rendimiento
PUT  /api/panel-config            # Actualizar intervalos de polling
```

### Ejemplo de Uso

#### Obtener Estado del Servidor
```bash
curl -X GET http://localhost:5000/api/server/status \
  -H "Cookie: session=tu_session_cookie"
```

**Respuesta:**
```json
{
  "running": true,
  "cpu_percent": 45.2,
  "memory_percent": 62.8,
  "memory_usage": "2.1 GB",
  "uptime": "5 hours, 23 minutes"
}
```

#### Crear Nuevo Mundo
```bash
curl -X POST http://localhost:5000/api/worlds \
  -H "Content-Type: application/json" \
  -H "Cookie: session=tu_session_cookie" \
  -d '{
    "name": "Survival Extremo",
    "slug": "survival-extremo",
    "description": "Modo supervivencia con dificultad Hard",
    "gamemode": "survival",
    "difficulty": "hard",
    "pvp": true,
    "seed": "12345"
  }'
```

**Respuesta:**
```json
{
  "success": true,
  "message": "Mundo creado exitosamente",
  "world": {
    "slug": "survival-extremo",
    "name": "Survival Extremo",
    "description": "Modo supervivencia con dificultad Hard",
    "active": false,
    "created_at": "2025-11-30T18:30:00",
    "size_mb": 0.5
  }
}
```

#### Activar Mundo (Cambio en Caliente)
```bash
curl -X POST http://localhost:5000/api/worlds/survival-extremo/activate \
  -H "Cookie: session=tu_session_cookie"
```

**Respuesta:**
```json
{
  "success": true,
  "message": "Mundo activado exitosamente",
  "backup_created": true,
  "backup_filename": "world-default_auto_20251130_183045.tar.gz"
}
```

#### Configurar Rendimiento del Panel
```bash
curl -X PUT http://localhost:5000/api/panel-config \
  -H "Content-Type: application/json" \
  -H "Cookie: session=tu_session_cookie" \
  -d '{
    "refresh_interval": 10000,
    "logs_interval": 15000,
    "tps_interval": 20000,
    "pause_when_hidden": true
  }'
```

**Respuesta:**
```json
{
  "success": true,
  "message": "Configuración actualizada",
  "config": {
    "refresh_interval": 10000,
    "logs_interval": 15000,
    "tps_interval": 20000,
    "pause_when_hidden": true,
    "enable_cache": true,
    "cache_ttl": 3000
  }
}
```

---

## 🎮 Gestión del Servidor

### Scripts Disponibles

#### Servidor Minecraft
```bash
./run.sh                       # Iniciar servidor
./stop.sh                      # Detener servidor
docker-compose restart         # Reiniciar servidor
docker logs -f mc-paper        # Ver logs en tiempo real
```

#### Panel Web
```bash
./start-web-panel.sh           # Iniciar panel web
./stop-web-panel.sh            # Detener panel web
./restart-web-panel.sh         # Reiniciar panel web
./status-web-panel.sh          # Ver estado del panel
./logs-web-panel.sh            # Ver logs del panel
```

#### Sistema Multi-Mundo
```bash
scripts/migrate-to-multiworld.sh     # Migrar de single-world a multi-world
scripts/rollback-multiworld.sh       # Revertir migración multi-world
```

**Proceso de Migración:**
1. Crea backup timestamped del mundo actual
2. Mueve mundo a `worlds/world-default/`
3. Crea symlink `worlds/active` → `worlds/world-default/`
4. Genera `metadata.json` del mundo
5. Actualiza `docker-compose.yml` con symlinks
6. 100% reversible con `rollback-multiworld.sh`

#### Verificación y Testing
```bash
scripts/verify-panel.sh        # Verificar instalación del panel
scripts/run-tests.sh           # Testing de integración completo (12 checks)
```

**verify-panel.sh - Verificaciones:**
- ✅ Estructura de directorios
- ✅ Archivos de Python
- ✅ Templates HTML
- ✅ JavaScript y assets
- ✅ Dependencias de Python
- ✅ Configuración .env
- ✅ Docker y contenedor
- ✅ Scripts de gestión

**run-tests.sh - 12 Checks de Integración:**
1. ✅ Verificar directorios (worlds/, backups/, config/, web/models, web/services)
2. ✅ Verificar archivos esenciales (app.py, models, services, templates, JS)
3. ✅ Verificar permisos de scripts
4. ✅ Verificar configuración (backup_config.json, panel_config.json)
5. ✅ Test de BackupService
6. ✅ Verificar sintaxis Python
7. ✅ Verificar docker-compose.yml
8. ✅ Verificar symlinks de mundos
9. ✅ Verificar endpoints API
10. ✅ Verificar funciones JavaScript
11. ✅ Verificar modales HTML
12. ✅ Resumen final

### Comandos Docker Útiles

```bash
# Ver logs del contenedor
docker logs -f mc-paper

# Ejecutar comando en el servidor
docker exec mc-paper rcon-cli <comando>

# Ver estadísticas de recursos
docker stats mc-paper

# Reiniciar contenedor
docker restart mc-paper

# Detener contenedor
docker stop mc-paper

# Iniciar contenedor
docker start mc-paper

# Acceder a la consola del contenedor
docker exec -it mc-paper bash
```

### Backups

#### Backup Manual
```bash
./backup.sh
```

Crea archivo: `backups/backup_YYYY-MM-DD_HH-MM-SS.tar.gz`

#### Backups por Mundo (Sistema Avanzado)

**Desde Panel Web:**
1. Ir a sección **Mundos**
2. Click en **"Backups"** del mundo deseado
3. Modal con lista de backups del mundo
4. **Crear backup manual:** Button "Crear Backup"
5. **Restaurar:** Click en "Restore" con confirmación
6. **Eliminar:** Click en "Delete" con confirmación

**Características:**
- 📦 **Almacenamiento:** `backups/worlds/{slug}/`
- 🏷️ **Nomenclatura:** `{slug}_{tipo}_{timestamp}.tar.gz`
  - Tipo: `manual` o `auto`
- 📝 **Metadata:** JSON con fecha, tamaño, mundo, tipo
- 🔄 **Auto-cleanup:** Mantiene solo N backups automáticos (configurable)
- 🛡️ **Seguridad:** Backup pre-restauración automático
- 📊 **Compresión:** tar.gz para optimizar espacio

#### Configuración de Backups Automáticos

**Desde Panel Web:**
1. Ir a sección **Backups**
2. Card "Configuración de Backups Automáticos"
3. **Toggle Auto-Backup:** Activar/desactivar backups al cambiar mundo
4. **Retención:** Slider 1-50 backups automáticos a mantener
5. Cambios se guardan automáticamente

**Archivo de configuración:** `config/backup_config.json`
```json
{
  "auto_backup_enabled": true,
  "retention_count": 5
}
```

**Funcionamiento:**
- Al activar un mundo nuevo, se crea backup automático del mundo anterior
- Solo si `auto_backup_enabled: true`
- Cleanup automático mantiene solo los últimos N backups auto
- Backups manuales nunca se eliminan automáticamente

#### Restaurar Backup
```bash
./restore-backup.sh backups/backup_2025-11-30_18-00-00.tar.gz
```

**Proceso:**
1. Crea backup de seguridad actual
2. Detiene servidor
3. Extrae backup seleccionado
4. Reinicia servidor

#### Backups Programados (Cron)
```bash
# Editar crontab
crontab -e

# Agregar backup diario a las 3 AM
0 3 * * * /home/mkd/contenedores/mc-paper/backup.sh

# Agregar backup cada 6 horas
0 */6 * * * /home/mkd/contenedores/mc-paper/backup.sh
```

#### Documentación Completa de Backups
Ver documentación detallada en:
- **[BACKUP_SYSTEM.md](BACKUP_SYSTEM.md)** - Sistema completo de backups (400+ líneas)
- **[BACKUP_CONFIG.md](BACKUP_CONFIG.md)** - Configuración de backups automáticos (200+ líneas)

---

### Optimización de Rendimiento

#### Problema: RCON Polling Excesivo

**Antes:**
- 18 solicitudes RCON por minuto (constante)
- Polling continúa cuando panel oculto/inactivo
- Intervalos fijos sin configuración

**Después:**
- 2-18 solicitudes por minuto (configurable)
- 0 solicitudes cuando tab oculto (Page Visibility API)
- **Reducción potencial: hasta 78%**

#### Configuración desde Panel Web

1. Ir a sección **Configuración**
2. Card "Optimización de Rendimiento"
3. Configurar intervalos:
   - **Refresh Interval:** 1-60 segundos (stats generales)
   - **Logs Interval:** 5-120 segundos (logs del servidor)
   - **TPS Interval:** 5-120 segundos (ticks per second)
4. **Toggle "Pausar cuando oculto":** Activa Page Visibility API
5. **Status Indicator:** Badge verde (Active) o amarillo (Paused)

**Archivo de configuración:** `config/panel_config.json`
```json
{
  "refresh_interval": 5000,
  "logs_interval": 10000,
  "tps_interval": 10000,
  "pause_when_hidden": true,
  "enable_cache": true,
  "cache_ttl": 3000
}
```

#### Comparación de Rendimiento

| Configuración | Solicitudes/min | Reducción | Uso Servidor |
|---------------|-----------------|-----------|--------------|
| Original (5s/10s/10s) | 18 | 0% | Alto |
| Moderado (10s/15s/20s) | 9 | 50% | Medio |
| Conservador (30s/60s/60s) | 4 | 78% | Bajo |
| Panel Oculto | 0 | 100% | Ninguno |

#### Documentación Completa de Optimización
Ver: **[PERFORMANCE_OPTIMIZATION.md](PERFORMANCE_OPTIMIZATION.md)** (300+ líneas)

---

## 🔒 Seguridad

### Mejores Prácticas

#### 1. Contraseñas Seguras
```bash
# Generar contraseña aleatoria
openssl rand -base64 32

# Generar secret key
python3 -c "import secrets; print(secrets.token_hex(32))"
```

#### 2. HTTPS con Let's Encrypt
```bash
# Instalar Certbot
sudo apt install certbot

# Obtener certificado
sudo certbot certonly --standalone -d minecraft.tudominio.com

# Configurar Nginx como reverse proxy
# Ver: web/docs/VIRTUALMIN-CONFIG.md
```

#### 3. Firewall (UFW)
```bash
# Permitir SSH
sudo ufw allow 22/tcp

# Permitir Minecraft
sudo ufw allow 25565/tcp

# Permitir panel web (solo desde IPs específicas)
sudo ufw allow from 192.168.1.0/24 to any port 5000

# Activar firewall
sudo ufw enable
```

#### 4. Systemd Service (Auto-inicio)
```bash
# Crear service para panel web
sudo nano /etc/systemd/system/minecraft-panel.service
```

```ini
[Unit]
Description=Minecraft Web Panel
After=network.target docker.service

[Service]
Type=simple
User=mkd
WorkingDirectory=/home/mkd/contenedores/mc-paper/web
Environment="PATH=/home/mkd/contenedores/mc-paper/web/.venv/bin"
ExecStart=/home/mkd/contenedores/mc-paper/web/.venv/bin/python app.py
Restart=always

[Install]
WantedBy=multi-user.target
```

```bash
# Activar service
sudo systemctl enable minecraft-panel
sudo systemctl start minecraft-panel
sudo systemctl status minecraft-panel
```

#### 5. Permisos de Docker
```bash
# Agregar usuario al grupo docker
sudo usermod -aG docker $USER

# Recargar grupos (o logout/login)
newgrp docker
```

#### 6. Actualizar Regularmente
```bash
# Actualizar PaperMC
./update-server.sh

# Actualizar dependencias de Python
cd web
source .venv/bin/activate
pip install --upgrade -r requirements.txt
```

---

## 🐛 Solución de Problemas

### Servidor No Inicia

#### Síntoma: Contenedor se detiene inmediatamente
```bash
# Ver logs
docker logs mc-paper

# Problemas comunes:
# - Falta aceptar EULA
# - Puerto 25565 en uso
# - Falta memoria RAM
```

**Solución:**
```bash
# Aceptar EULA
echo "eula=true" > eula.txt

# Verificar puerto
lsof -i :25565
# Si está en uso, cambiar en .env o detener proceso

# Verificar memoria
free -h
# Ajustar MEMORY en .env si es necesario
```

---

### Panel Web No Accesible

#### Síntoma: No se puede acceder a http://localhost:5000

```bash
# Verificar si el panel está corriendo
ps aux | grep "python.*app.py"

# Ver logs del panel
cd web
tail -f panel.log

# Verificar puerto
lsof -i :5000
```

**Solución:**
```bash
# Reiniciar panel
./stop-web-panel.sh
./start-web-panel.sh

# Si puerto 5000 está en uso, cambiar en start-web-panel.sh
flask run --host=0.0.0.0 --port=5001
```

---

### Error de Autenticación

#### Síntoma: "Credenciales incorrectas"

```bash
# Verificar credenciales en .env
cat web/.env | grep ADMIN

# Verificar hash de contraseña
cd web
source .venv/bin/activate
python3 generate_hash.py
```

**Solución:**
```bash
# Resetear contraseña
cd web
nano .env

# Cambiar a contraseña temporal (será hasheada en primer login)
ADMIN_PASSWORD=admin123
# Comentar ADMIN_PASSWORD_HASH
# ADMIN_PASSWORD_HASH=...

# Reiniciar panel
./restart-web-panel.sh
```

---

### Docker No Responde

#### Síntoma: Comandos docker no funcionan

```bash
# Verificar servicio Docker
systemctl status docker

# Ver logs de Docker
journalctl -u docker -n 50
```

**Solución:**
```bash
# Reiniciar Docker
sudo systemctl restart docker

# Verificar permisos
sudo usermod -aG docker $USER
newgrp docker

# Verificar instalación
docker --version
docker ps
```

---

### Backups Fallan

#### Síntoma: Error al crear/restaurar backup

```bash
# Verificar espacio en disco
df -h

# Verificar permisos
ls -la backups/

# Ver logs
docker logs mc-paper | grep -i backup
```

**Solución:**
```bash
# Crear directorio si no existe
mkdir -p backups

# Ajustar permisos
chmod 755 backups
chown $USER:$USER backups

# Limpiar backups antiguos
./cleanup-old-backups.sh  # Si existe
# O manualmente:
find backups/ -name "backup_*.tar.gz" -mtime +30 -delete
```

---

### TPS Bajo (Lag)

#### Síntoma: TPS < 18, servidor con lag

```bash
# Ver uso de recursos
docker stats mc-paper

# Ver plugins cargados
docker exec mc-paper rcon-cli plugins

# Ver entidades
docker exec mc-paper rcon-cli "forge tps"  # Paper específico
```

**Solución:**
```bash
# Aumentar memoria en .env
MEMORY=6G  # o más

# Optimizar server.properties
view-distance=8  # Reducir si es muy alto
simulation-distance=6

# Reducir entidades
docker exec mc-paper rcon-cli "minecraft:kill @e[type=!player]"

# Desactivar plugins problemáticos
# Desde panel web: Plugins → Delete/Toggle
```

---

### Pérdida de Datos

#### Síntoma: Mundos/configuración desaparecieron

```bash
# Verificar volúmenes de Docker
docker volume ls

# Verificar montajes
docker inspect mc-paper | grep -A 10 Mounts
```

**Solución:**
```bash
# Restaurar desde backup
./restore-backup.sh backups/backup_YYYY-MM-DD_HH-MM-SS.tar.gz

# Si no hay backup, verificar volúmenes huérfanos
docker volume ls -qf dangling=true

# Prevención: Backups automáticos
# Agregar a crontab
crontab -e
0 */6 * * * /home/mkd/contenedores/mc-paper/backup.sh
```

---

## 📚 Documentación Adicional

### 🚀 Inicio Rápido
- **[docs/INICIO_RAPIDO.md](docs/INICIO_RAPIDO.md)** - Guía de inicio rápido (instalación en 3 pasos) 🆕

### Documentación del Sistema Multi-Mundo
- **[docs/GUIA_MULTIMUNDOS.md](docs/GUIA_MULTIMUNDOS.md)** - Guía completa del sistema multi-mundo (1,000+ líneas) 🆕
- **[docs/RESUMEN_SISTEMA_V2.md](docs/RESUMEN_SISTEMA_V2.md)** - Resumen técnico completo v2.0 🆕
- **[docs/ROADMAP_MULTIMUNDOS.md](docs/ROADMAP_MULTIMUNDOS.md)** - Roadmap del desarrollo multi-mundo

### Documentación del Sistema de Backups
- **[docs/BACKUP_SYSTEM.md](docs/BACKUP_SYSTEM.md)** - Sistema completo de backups (400+ líneas)
- **[docs/BACKUP_CONFIG.md](docs/BACKUP_CONFIG.md)** - Configuración de backups automáticos (200+ líneas)

### Documentación de Optimización
- **[docs/PERFORMANCE_OPTIMIZATION.md](docs/PERFORMANCE_OPTIMIZATION.md)** - Optimización de rendimiento (300+ líneas)

### Documentación del Servidor
- **[docs/INSTALACION_RAPIDA.md](docs/INSTALACION_RAPIDA.md)** - Guía rápida de instalación
- **[docs/setup-minecraft.md](docs/setup-minecraft.md)** - Setup inicial del servidor
- **[docs/CAMBIOS_PERSISTENCIA.md](docs/CAMBIOS_PERSISTENCIA.md)** - Configuración de persistencia

### Documentación del Panel Web
- **[web/docs/GUIA_COMPLETA.md](web/docs/GUIA_COMPLETA.md)** - Guía completa de funcionalidades (20 características)
- **[web/docs/PANEL_README.md](web/docs/PANEL_README.md)** - README del panel web
- **[web/docs/VIRTUALMIN-CONFIG.md](web/docs/VIRTUALMIN-CONFIG.md)** - Configuración para producción

---

## 🤝 Contribuir

### Cómo Contribuir

1. **Fork** el repositorio
2. **Clonar** tu fork
```bash
git clone https://github.com/tu-usuario/mc-paper.git
```

3. **Crear rama** para tu feature
```bash
git checkout -b feature/nueva-funcionalidad
```

4. **Hacer cambios** y commit
```bash
git add .
git commit -m "feat: agregar nueva funcionalidad X"
```

5. **Push** a tu fork
```bash
git push origin feature/nueva-funcionalidad
```

6. **Crear Pull Request** en GitHub

### Estándares de Código

#### Python (Backend)
- **PEP 8** para estilo de código
- **Type hints** cuando sea posible
- **Docstrings** en funciones principales
- **Try/except** para manejo de errores

#### JavaScript (Frontend)
- **ES6+** features
- **Async/await** para operaciones asíncronas
- **Comentarios** en funciones complejas
- **Nombres descriptivos** de variables

#### Commits
Formato: `tipo: descripción`

**Tipos:**
- `feat`: Nueva funcionalidad
- `fix`: Corrección de bug
- `docs`: Cambios en documentación
- `style`: Formateo, punto y coma, etc.
- `refactor`: Refactorización de código
- `test`: Agregar tests
- `chore`: Tareas de mantenimiento

### Roadmap de Desarrollo

#### v2.0 (ACTUAL) ✅
- [x] Sistema multi-mundo completo
- [x] Backups automáticos por mundo
- [x] Configuración de backups desde UI
- [x] Optimización de polling RCON
- [x] Page Visibility API
- [x] Testing de integración

#### v2.1 (Próximo)
- [ ] Sistema de alertas (email/Discord)
- [ ] Programación de backups desde UI (cron visual)
- [ ] Exportar/importar mundos (.zip)
- [ ] API REST pública con tokens
- [ ] Roles y permisos de usuario

#### v2.2 (Futuro)
- [ ] Multi-servidor (gestionar varios servidores)
- [ ] WebSocket para actualizaciones en tiempo real
- [ ] Panel de métricas avanzado (Prometheus/Grafana)
- [ ] Integración con servicios en la nube (S3 backups)
- [ ] Mobile app (React Native)

#### v3.0 (Largo plazo)
- [ ] Kubernetes deployment
- [ ] Cluster de servidores (BungeeCord/Velocity)
- [ ] Soporte para otros tipos de servidor (Spigot, Fabric, Forge)
- [ ] Marketplace de configuraciones/modpacks
- [ ] AI para optimización automática

---

## 📄 Documentación

### Documentos Principales
- 📘 **[Migración SQLite](docs/MIGRACION_SQLITE.md)** - Guía completa de migración a bases de datos
- 📗 **[Arquitectura MMORPG](docs/ARQUITECTURA_MMORPG.md)** - Arquitectura del plugin
- 📙 **[Sistema Multimundos](docs/GUIA_MULTIMUNDOS.md)** - Gestión de múltiples mundos
- 📕 **[Sistema de Backups](docs/BACKUP_SYSTEM.md)** - Configuración de backups
- 📔 **[Sistema de Configuración](docs/CONFIG_SYSTEM.md)** - Gestión de configuraciones

### Índice Completo
Ver **[docs/INDICE_DOCUMENTACION.md](docs/INDICE_DOCUMENTACION.md)** para acceso a toda la documentación organizada por categorías.

### Documentación Histórica
Los documentos de migración SQLite están archivados en: **[docs/migracion-sqlite/](docs/migracion-sqlite/)**

---

## 📄 Licencia

MIT License

Copyright (c) 2025 MC-Paper Project

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

---

## 🌟 Créditos

### Desarrollado por
**GitHub Copilot** con contribuciones de la comunidad

### Tecnologías Utilizadas
- [PaperMC](https://papermc.io/) - Servidor Minecraft optimizado
- [Docker](https://www.docker.com/) - Contenedorización
- [Flask](https://flask.palletsprojects.com/) - Framework web Python
- [Bootstrap](https://getbootstrap.com/) - Framework CSS
- [Chart.js](https://www.chartjs.org/) - Gráficos interactivos

### Agradecimientos
- Comunidad de Minecraft
- Equipo de PaperMC
- Contribuidores de código abierto

---

## 📞 Soporte

### Reportar Bugs
[Crear Issue en GitHub](https://github.com/tu-usuario/mc-paper/issues)

### Preguntas Frecuentes
Ver: [web/docs/GUIA_COMPLETA.md](web/docs/GUIA_COMPLETA.md)

### Contacto
- **GitHub:** [@tu-usuario](https://github.com/tu-usuario)
- **Email:** soporte@tudominio.com

---

<div align="center">

**¡Gracias por usar MC-Paper!** 🎮✨

[⬆ Volver arriba](#-minecraft-papermc-server---docker--panel-de-administración-web)

</div>
