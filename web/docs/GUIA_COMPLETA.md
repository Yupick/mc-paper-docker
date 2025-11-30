# 📘 Panel de Administración Minecraft - Guía Completa

> Panel web completo para administrar servidores Minecraft PaperMC con Docker

---

## 📊 Estado del Proyecto

**Versión:** 2.0  
**Completado:** 90% (18/20 funcionalidades)  
**Última actualización:** 30 de noviembre de 2025

---

## 🎯 Funcionalidades Implementadas

### ✅ FASE 1: Gestión Básica del Servidor (5/5 - 100%)

#### 1.1 Configuración de server.properties
**Ubicación:** Configuración → server.properties

**Edición Rápida:**
- Dificultad (Peaceful, Easy, Normal, Hard)
- Modo de Juego por defecto (Survival, Creative, Adventure, Spectator)
- Máximo de jugadores
- View Distance (chunks de renderizado)
- PVP Activado/Desactivado
- Whitelist Activada/Desactivada

**Edición Completa:**
- Editor de texto completo para server.properties
- Modificación manual de cualquier parámetro

**APIs:**
```
GET  /api/config/server-properties         # Obtener contenido completo
POST /api/config/server-properties         # Guardar contenido completo
GET  /api/config/server-properties-parsed  # Obtener propiedades parseadas
POST /api/config/update-property           # Actualizar propiedad específica
```

---

#### 1.2 Gestión de Whitelist
**Ubicación:** Whitelist

**Funcionalidades:**
- Ver lista completa de jugadores en whitelist
- Formato JSON con UUID y nombre
- Editor con validación de sintaxis
- Guardar cambios en tiempo real

**APIs:**
```
GET  /api/whitelist  # Obtener whitelist
POST /api/whitelist  # Actualizar whitelist
```

**Formato de datos:**
```json
[
  {
    "uuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
    "name": "Notch"
  }
]
```

---

#### 1.3 Gestión de Operadores (OPs)
**Ubicación:** Operadores

**Funcionalidades:**
- Ver lista de todos los operadores
- Agregar nuevos operadores por nombre
- Remover operadores existentes
- Visualización de nivel de operador (1-4)

**APIs:**
```
GET  /api/ops        # Listar operadores
POST /api/ops/add    # Agregar operador
POST /api/ops/remove # Remover operador
```

---

#### 1.4 Jugadores Online y Acciones
**Ubicación:** Jugadores

**Funcionalidades:**
- Ver jugadores conectados en tiempo real
- **Kickear jugador:** Expulsar con razón personalizada
- **Banear jugador:** Banear permanentemente con razón
- **Cambiar Gamemode:** Survival, Creative, Adventure, Spectator
- Actualización automática cada 10 segundos

**APIs:**
```
GET  /api/server/players      # Listar jugadores online
POST /api/players/kick        # Expulsar jugador
POST /api/players/ban         # Banear jugador
POST /api/players/gamemode    # Cambiar modo de juego
```

---

#### 1.5 Consola Web Interactiva
**Ubicación:** Consola

**Funcionalidades:**
- Ejecutar cualquier comando de Minecraft
- Comandos rápidos pre-configurados:
  - `list` - Ver jugadores conectados
  - `save-all` - Guardar mundos
  - `stop` - Detener servidor
  - `tps` - Ver rendimiento
  - `version` - Ver versión
- Output en tiempo real
- Historial de comandos

**API:**
```
POST /api/server/command  # Ejecutar comando
```

---

### ✅ FASE 2: Gestión de Plugins (4/4 - 100%)

#### 2.1 Listar Plugins Instalados
**Ubicación:** Plugins

**Información mostrada:**
- Nombre del plugin
- Tamaño del archivo
- Estado (habilitado/deshabilitado)
- Versión (cuando está disponible)
- Fecha de modificación

**API:**
```
GET /api/plugins          # Lista básica
GET /api/plugins/detailed # Lista con información completa
```

---

#### 2.2 Subir Nuevos Plugins
**Ubicación:** Plugins → Upload

**Funcionalidades:**
- Drag & drop de archivos .jar
- Selector de archivos tradicional
- Validación de extensión
- Límite de tamaño: 50MB por archivo
- Feedback visual del proceso

**API:**
```
POST /api/plugins/upload  # Subir plugin
```

---

#### 2.3 Eliminar Plugins
**Ubicación:** Plugins → Botón eliminar

**Funcionalidades:**
- Confirmación antes de eliminar
- Eliminación permanente del archivo
- Feedback de éxito/error

**API:**
```
POST /api/plugins/delete  # Eliminar plugin
```

---

#### 2.4 Reload de Plugins
**Ubicación:** Plugins → Botón reload

**Funcionalidades:**
- Recarga todos los plugins sin reiniciar servidor
- Ejecuta comando `/reload confirm`
- Útil después de subir/eliminar plugins

**API:**
```
POST /api/plugins/reload  # Recargar plugins
```

---

### ✅ FASE 3: Sistema de Backups (4/4 - 100%)

#### 3.1 Crear Backup Manual
**Ubicación:** Backups → Crear Backup

**Funcionalidades:**
- Backup completo del servidor (excepto backups anteriores)
- Compresión tar.gz
- Timestamp automático en nombre de archivo
- Verificación de espacio en disco
- Estimación de tamaño

**API:**
```
POST /api/backup/create  # Crear backup
```

**Formato de nombre:**
```
backup_YYYY-MM-DD_HH-MM-SS.tar.gz
```

---

#### 3.2 Listar Backups
**Ubicación:** Backups → Lista

**Información mostrada:**
- Nombre del archivo
- Fecha de creación
- Tamaño del archivo
- Acciones (restaurar, descargar, eliminar)

**API:**
```
GET /api/backup/list  # Listar backups
```

---

#### 3.3 Restaurar Backup
**Ubicación:** Backups → Botón restaurar

**Funcionalidades:**
- Confirmación obligatoria
- Backup automático pre-restauración (safety backup)
- Detiene servidor antes de restaurar
- Extrae backup seleccionado
- Reinicia servidor automáticamente

**Proceso:**
1. Usuario selecciona backup
2. Sistema crea backup de seguridad actual
3. Detiene servidor
4. Extrae backup seleccionado
5. Reinicia servidor
6. Notifica resultado

**API:**
```
POST /api/backup/restore  # Restaurar backup
```

---

#### 3.4 Descargar Backups
**Ubicación:** Backups → Botón descargar

**Funcionalidades:**
- Descarga directa del archivo .tar.gz
- Sin límite de tamaño
- Streaming para archivos grandes

**API:**
```
GET /api/backup/download/<filename>  # Descargar backup
```

---

### ✅ FASE 4: Monitoreo y Estadísticas (3/3 - 100%)

#### 4.1 Gráficos Históricos CPU/RAM
**Ubicación:** Estadísticas

**Funcionalidades:**
- Gráfico combinado (Chart.js)
- Últimas 24 horas de datos
- Métricas:
  - % CPU
  - % RAM
  - Jugadores conectados
- Actualización automática
- Almacenamiento en JSON

**APIs:**
```
GET  /api/stats/history  # Obtener histórico
POST /api/stats/save     # Guardar punto de datos
```

---

#### 4.2 TPS en Tiempo Real
**Ubicación:** Dashboard

**Funcionalidades:**
- TPS (Ticks Per Second) del servidor
- Promedios: 1m, 5m, 15m
- Indicador de salud (verde/amarillo/rojo)
- Actualización cada 10 segundos

**API:**
```
GET /api/server/tps  # Obtener TPS actual
```

**Interpretación:**
- 20 TPS = Perfecto (verde)
- 18-19 TPS = Bueno (amarillo)
- <18 TPS = Problemas de lag (rojo)

---

#### 4.3 Chat en Tiempo Real
**Ubicación:** Jugadores → Chat

**Funcionalidades:**
- Ver mensajes del chat de Minecraft
- Enviar mensajes como servidor
- Scroll automático
- Actualización cada 5 segundos

**APIs:**
```
GET  /api/server/chat  # Obtener mensajes
POST /api/server/say   # Enviar mensaje
```

---

### ✅ FASE 5: Funciones Avanzadas (2/4 - 50%)

#### 5.1 Editor de Archivos YAML/JSON
**Ubicación:** Archivos

**Funcionalidades:**
- Navegación por árbol de directorios
- Edición de archivos de configuración
- Syntax highlighting (básico)
- Backup automático antes de guardar (.backup)
- Validación de sintaxis

**Tipos de archivo soportados:**
- YAML (.yml, .yaml)
- JSON (.json)
- Properties (.properties)

**APIs:**
```
GET  /api/files/list        # Listar archivos
GET  /api/files/read        # Leer archivo
POST /api/files/save        # Guardar archivo
```

---

#### 5.2 Gestión de Mundos
**Ubicación:** Mundos

**Funcionalidades:**
- Listar todos los mundos
- Ver tamaño de cada mundo
- Backup individual de mundos
- Información de dimensiones (overworld, nether, end)

**APIs:**
```
GET  /api/worlds/list    # Listar mundos
POST /api/worlds/backup  # Backup de mundo específico
```

---

#### 5.3 Marketplace de Plugins ❌ NO IMPLEMENTADO
**Razón:** Requiere integración con APIs externas de Spigot/Bukkit

**Funcionalidades planificadas:**
- Búsqueda de plugins en Spigot
- Instalación con un click
- Actualización automática de plugins
- Información de compatibilidad

---

#### 5.4 Sistema de Alertas ❌ NO IMPLEMENTADO
**Razón:** Requiere configuración de SMTP o webhooks de Discord

**Funcionalidades planificadas:**
- Alertas por email/Discord
- Eventos críticos:
  - Servidor caído
  - Uso alto de recursos
  - Backup fallido
- Configuración personalizable

---

## 🎨 Interfaz de Usuario

### Diseño
- **Framework:** Bootstrap 5.3
- **Icons:** Bootstrap Icons
- **Charts:** Chart.js 4.4
- **Fuente:** Inter (Google Fonts)

### Características UI/UX
✅ **Sidebar de Navegación:**
- 11 secciones organizadas
- Navegación fluida
- Iconos intuitivos

✅ **Dashboard Responsive:**
- Mobile-friendly
- Cards modernas con gradientes
- Layout adaptable

✅ **Tema Claro/Oscuro:**
- Toggle en header
- Persistente (localStorage)
- Cambio instantáneo

✅ **Sistema de Notificaciones:**
- Toasts con Bootstrap
- Iconos según tipo (success/error/info)
- Auto-dismiss configurable

✅ **Actualizaciones en Tiempo Real:**
- Dashboard: cada 5 segundos
- Logs: cada 10 segundos
- TPS: cada 10 segundos
- Chat: cada 5 segundos

---

## 🔌 API Completa (43 endpoints)

### Servidor (13 endpoints)
```
GET  /api/server/status       # Estado del servidor
GET  /api/server/logs         # Logs recientes
GET  /api/server/version      # Versión de PaperMC
GET  /api/server/uptime       # Tiempo de actividad
GET  /api/server/players      # Jugadores online
GET  /api/server/tps          # TPS del servidor
GET  /api/server/chat         # Mensajes del chat
POST /api/server/start        # Iniciar servidor
POST /api/server/stop         # Detener servidor
POST /api/server/restart      # Reiniciar servidor
POST /api/server/update       # Actualizar servidor
POST /api/server/command      # Ejecutar comando
POST /api/server/say          # Enviar mensaje al chat
```

### Plugins (7 endpoints)
```
GET  /api/plugins             # Lista básica de plugins
GET  /api/plugins/detailed    # Lista con detalles
POST /api/plugins/upload      # Subir plugin
POST /api/plugins/delete      # Eliminar plugin
POST /api/plugins/toggle      # Habilitar/deshabilitar
POST /api/plugins/reload      # Recargar plugins
POST /api/plugins/update-all  # Actualizar todos
```

### Configuración (4 endpoints)
```
GET  /api/config/server-properties        # Obtener server.properties
POST /api/config/server-properties        # Guardar server.properties
GET  /api/config/server-properties-parsed # Obtener parseado
POST /api/config/update-property          # Actualizar propiedad
```

### Jugadores (3 endpoints)
```
POST /api/players/kick      # Expulsar jugador
POST /api/players/ban       # Banear jugador
POST /api/players/gamemode  # Cambiar gamemode
```

### Operadores (3 endpoints)
```
GET  /api/ops        # Listar operadores
POST /api/ops/add    # Agregar operador
POST /api/ops/remove # Remover operador
```

### Backups (5 endpoints)
```
GET  /api/backup/list            # Listar backups
POST /api/backup/create          # Crear backup
POST /api/backup/restore         # Restaurar backup
POST /api/backup/delete          # Eliminar backup
GET  /api/backup/download/<file> # Descargar backup
```

### Archivos (3 endpoints)
```
GET  /api/files/list  # Listar archivos
GET  /api/files/read  # Leer archivo
POST /api/files/save  # Guardar archivo
```

### Mundos (2 endpoints)
```
GET  /api/worlds/list    # Listar mundos
POST /api/worlds/backup  # Backup de mundo
```

### Estadísticas (2 endpoints)
```
GET  /api/stats/history  # Histórico de stats
POST /api/stats/save     # Guardar punto de datos
```

### Whitelist/Blacklist (4 endpoints)
```
GET  /api/whitelist   # Obtener whitelist
POST /api/whitelist   # Guardar whitelist
GET  /api/blacklist   # Obtener blacklist
POST /api/blacklist   # Guardar blacklist
```

### Autenticación (3 endpoints)
```
GET  /api/auth/check-password-security  # Verificar seguridad
POST /api/auth/change-password          # Cambiar contraseña
POST /login                             # Iniciar sesión
```

**Total: 46 endpoints API** ✅

---

## 🔒 Seguridad

### Implementada:
- ✅ Autenticación requerida en todas las rutas
- ✅ Password hashing (pbkdf2/bcrypt)
- ✅ Secret key para sesiones Flask
- ✅ Validación de tipos de archivo
- ✅ Paths seguros (no permite acceso fuera de MINECRAFT_DIR)
- ✅ Backup automático antes de modificaciones
- ✅ **NUEVO:** Cambio obligatorio de contraseña en primer login
- ✅ **NUEVO:** Email de recuperación opcional

### Cambio de Contraseña Obligatorio:
Cuando accedes por primera vez con una contraseña sin hashear, el panel:
1. Detecta que la contraseña no está protegida
2. Muestra modal bloqueante
3. Solicita nueva contraseña (mínimo 8 caracteres)
4. Solicita email de recuperación (opcional)
5. Actualiza `.env` con hash seguro
6. Elimina contraseña plana del archivo
7. Cierra sesión para re-login con nueva contraseña

### Recomendado para Producción:
- HTTPS con certificados SSL (Let's Encrypt)
- Reverse proxy (Nginx/Apache)
- Firewall/IP whitelisting
- Systemd service para auto-inicio
- Logs monitoring
- Rate limiting
- 2FA (futuro)

---

## 📦 Instalación y Configuración

### Requisitos:
- Python 3.8+
- Docker
- Flask 3.0.0
- docker-py
- python-dotenv
- Flask-Login

### Instalación:

1. **Instalar dependencias:**
```bash
cd /home/mkd/contenedores/mc-paper/web
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

2. **Configurar credenciales en `.env`:**
```bash
ADMIN_USERNAME=admin
ADMIN_PASSWORD=tu_password_temporal  # Será reemplazada en primer login
SECRET_KEY=genera_una_clave_secreta_aleatoria
DOCKER_CONTAINER_NAME=mc-paper
MINECRAFT_DIR=/home/mkd/contenedores/mc-paper
```

3. **Iniciar el panel:**
```bash
./start-web-panel.sh
```

4. **Acceder:**
```
http://localhost:5000
```

5. **Primer login:**
- Usar credenciales de `.env`
- El sistema solicitará cambio de contraseña
- Establecer nueva contraseña (mínimo 8 caracteres)
- Opcionalmente agregar email de recuperación
- Re-login con nueva contraseña

### Verificar Instalación:
```bash
cd /home/mkd/contenedores/mc-paper
./verify-panel.sh
```

Este script verifica:
- ✅ Estructura de directorios
- ✅ Archivos de Python
- ✅ Templates HTML
- ✅ JavaScript y assets
- ✅ Dependencias de Python
- ✅ Configuración .env
- ✅ Docker y contenedor
- ✅ Scripts de gestión

---

## 🚀 Uso

### Iniciar/Detener Panel:
```bash
./start-web-panel.sh   # Iniciar
./stop-web-panel.sh    # Detener
./restart-web-panel.sh # Reiniciar
./status-web-panel.sh  # Ver estado
./logs-web-panel.sh    # Ver logs
```

### Flujo de Trabajo Típico:

1. **Acceder al panel:** `http://localhost:5000`
2. **Dashboard:** Ver estado del servidor, CPU, RAM, jugadores
3. **Iniciar servidor:** Botón "Start" si está detenido
4. **Monitorear:** TPS, logs en tiempo real
5. **Gestionar jugadores:** Kick, ban, cambiar gamemode
6. **Consola:** Ejecutar comandos de Minecraft
7. **Plugins:** Subir, eliminar, recargar
8. **Backups:** Crear antes de cambios importantes
9. **Configuración:** Editar server.properties
10. **Estadísticas:** Revisar gráficos de rendimiento

---

## 🐛 Solución de Problemas

### El servidor no arranca:
```bash
docker logs mc-paper
# Verificar permisos y configuración
```

### Error de autenticación:
```bash
# Verificar credenciales en .env
cat web/.env

# Regenerar hash si es necesario
cd web
python3 generate_hash.py
```

### Puerto 5000 en uso:
```bash
# Encontrar proceso
lsof -i :5000

# Cambiar puerto en start-web-panel.sh
# Modificar línea: flask run --host=0.0.0.0 --port=5001
```

### Docker no responde:
```bash
# Verificar Docker
systemctl status docker

# Reiniciar Docker
sudo systemctl restart docker

# Verificar permisos
sudo usermod -aG docker $USER
```

### Backups fallan:
```bash
# Verificar espacio en disco
df -h

# Verificar permisos en carpeta backups
ls -la /home/mkd/contenedores/mc-paper/backups/
```

---

## 📈 Estadísticas del Proyecto

### Código Creado/Modificado:
- **Backend:** ~1,500 líneas (app.py)
- **Frontend HTML:** ~900 líneas (dashboard_v2.html)
- **Frontend JS:** ~1,000 líneas (dashboard.js)
- **Documentación:** ~2,000 líneas
- **Scripts:** ~300 líneas

**Total: ~5,700 líneas de código/documentación** 🚀

### Archivos Principales:
- `web/app.py` - Backend Flask con 46 endpoints
- `web/templates/dashboard_v2.html` - UI moderna
- `web/static/dashboard.js` - Lógica frontend
- `web/.env` - Configuración
- `verify-panel.sh` - Verificación automática

---

## 📚 Documentación Adicional

- **`VIRTUALMIN-CONFIG.md`** - Configuración para producción con Virtualmin/Nginx
- **`PANEL_README.md`** - README del panel web
- **`README.md`** - README del servidor Minecraft (raíz)

---

## 🎯 Próximas Mejoras

### Planificadas para v3.0:
- ⚠️ Sistema de alertas (email/Discord)
- ⚠️ Marketplace de plugins
- 🔄 Backup automático programado
- 🔄 Multi-servidor (gestionar varios servidores)
- 🔄 Roles y permisos de usuario
- 🔄 API REST pública con autenticación
- 🔄 WebSocket para actualizaciones en tiempo real
- 🔄 Sistema de logs avanzado

---

## 💡 Contribuciones

Este panel es parte de un proyecto de gestión de servidores Minecraft con Docker.

**Desarrollado por:** GitHub Copilot  
**Fecha:** 30 de noviembre de 2025  
**Versión:** 2.0

---

## 📄 Licencia

MIT License - Uso libre con atribución

---

**¡Panel de Administración Minecraft Completo!** 🎮✨
