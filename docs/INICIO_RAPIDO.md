# 🚀 Inicio Rápido - Sistema Multi-Mundo v2.0

## Instalación en 3 Pasos

### 1. Instalar Sistema Completo
```bash
chmod +x create.sh
./create.sh
```

**Esto crea:**
- ✅ Estructura de directorios (worlds/, backups/, config/, web/)
- ✅ Plugins esenciales (EssentialsX, Vault, LuckPerms)
- ✅ Archivos de configuración (backup_config.json, panel_config.json)
- ✅ Imagen Docker optimizada

### 2. Configurar Credenciales del Panel
```bash
cd web
nano .env
```

**Editar:**
```env
ADMIN_USERNAME=admin
ADMIN_PASSWORD=tu_contraseña_segura
SECRET_KEY=generar_con_comando_abajo
```

**Generar SECRET_KEY:**
```bash
python3 -c "import secrets; print(secrets.token_hex(32))"
```

### 3. Iniciar Servicios
```bash
# Terminal 1: Servidor Minecraft
docker-compose up -d

# Terminal 2: Panel Web
cd web
./start-web-panel.sh
```

**Acceder:**
- 🌐 Panel Web: http://localhost:5000
- 🎮 Servidor Minecraft: localhost:25565

---

## Primeros Pasos

### 1. Login en el Panel
1. Abrir http://localhost:5000
2. Usuario: `admin` (o el que configuraste)
3. Contraseña: la que configuraste
4. **Cambiar contraseña** en primer login (obligatorio)

### 2. Verificar Sistema
```bash
scripts/run-tests.sh
```

**Esperar:** ✅ 12/12 tests passed

### 2. Migrar a Multi-Mundo (si tienes servidor existente)
```bash
chmod +x scripts/migrate-to-multiworld.sh
scripts/migrate-to-multiworld.sh
```

**Esto:**
- Crea backup de seguridad
- Mueve mundo actual a `worlds/world-default/`
- Crea symlink `worlds/active → world-default`
- Actualiza docker-compose.yml
- **100% reversible** con `./rollback-multiworld.sh`

---

## Uso Básico

### Crear Nuevo Mundo

**Desde Panel Web:**
1. Ir a **🌍 Mundos**
2. Click en **"+ Crear Mundo"**
3. Completar formulario:
   - Nombre: `Survival Extremo`
   - Slug: `survival-extremo`
   - Gamemode: `survival`
   - Dificultad: `hard`
   - PVP: ✅
4. Click en **"Crear Mundo"**

**Desde API:**
```bash
curl -X POST http://localhost:5000/api/worlds \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Survival Extremo",
    "slug": "survival-extremo",
    "gamemode": "survival",
    "difficulty": "hard",
    "pvp": true
  }'
```

### Cambiar de Mundo

**Desde Panel Web:**
1. Ir a tarjeta del mundo deseado
2. Click en **"Activar"**
3. Confirmar en modal
4. Esperar 30-60 segundos

**⚠️ IMPORTANTE:**
- El servidor se reiniciará
- Los jugadores serán desconectados
- Se creará backup automático (si está habilitado)

### Crear Backup Manual

**Desde Panel Web:**
1. Click en **"Backups"** del mundo
2. Click en **"Crear Backup"**
3. Esperar confirmación

**Nomenclatura:**
```
{slug}_manual_{timestamp}.tar.gz
Ejemplo: survival-extremo_manual_20251130_183045.tar.gz
```

### Configurar Backups Automáticos

**Desde Panel Web:**
1. Ir a **💾 Backups**
2. Card "Configuración de Backups Automáticos"
3. **Toggle Auto-Backup:** ON
4. **Retención:** 5 backups (recomendado)

**¿Cuándo se crean?**
- Al cambiar de mundo activo
- Backup del mundo anterior (antes del cambio)

### Optimizar Rendimiento del Panel

**Desde Panel Web:**
1. Ir a **⚙️ Configuración**
2. Card "Optimización de Rendimiento"
3. Configurar intervalos:
   - **Refresh:** 10 segundos (recomendado)
   - **Logs:** 15 segundos
   - **TPS:** 20 segundos
4. **Pausar cuando oculto:** ON

**Impacto:**
- Reduce solicitudes RCON hasta 78%
- Mejora rendimiento del servidor
- 0 solicitudes cuando tab oculto

---

## Comandos Útiles

### Gestión del Servidor
```bash
docker-compose up -d           # Iniciar servidor
docker-compose down            # Detener servidor
docker-compose restart         # Reiniciar servidor
docker logs -f mc-paper        # Ver logs en tiempo real
```

### Panel Web
```bash
cd web
./start-web-panel.sh           # Iniciar panel
./stop-web-panel.sh            # Detener panel
./restart-web-panel.sh         # Reiniciar panel
tail -f panel.log              # Ver logs
```

### Multi-Mundo
```bash
scripts/migrate-to-multiworld.sh     # Migrar a multi-mundo
scripts/rollback-multiworld.sh       # Revertir migración
readlink worlds/active               # Ver mundo activo
ls -la worlds/                       # Ver todos los mundos
```

### Testing
```bash
scripts/run-tests.sh                 # Suite completa (12 checks)
scripts/verify-panel.sh              # Verificar instalación
```

### Backups Manuales
```bash
./backup.sh                    # Crear backup completo
./restore-backup.sh archivo.tar.gz  # Restaurar backup

# Ver backups de un mundo
ls -lh backups/worlds/world-default/
```

---

## Estructura de Directorios

```
mc-paper/
├── create.sh              ← Instalación automática
├── uninstall.sh           ← Desinstalación completa
├── docker-compose.yml     ← Configuración Docker
├── worlds/                ← Sistema multi-mundo
│   ├── active/            ← Symlink → mundo activo
│   ├── world-default/     ← Mundo por defecto
│   └── {slug}/            ← Otros mundos
├── backups/               ← Backups del sistema
│   └── worlds/            ← Backups por mundo
│       └── {slug}/        ← Backups de cada mundo
├── config/                ← Configuración
│   ├── server.properties
│   ├── backup_config.json
│   └── panel_config.json
└── web/                   ← Panel de administración
    ├── app.py             ← Backend Flask
    ├── models/            ← Modelos (World, WorldManager)
    ├── services/          ← Servicios (BackupService)
    └── start-web-panel.sh ← Iniciar panel
```

---

## Verificaciones Rápidas

### ¿El servidor está corriendo?
```bash
docker ps | grep mc-paper
# Debe mostrar: Up X minutes
```

### ¿Qué mundo está activo?
```bash
readlink worlds/active
# Muestra: world-default (o el mundo activo)
```

### ¿El panel está corriendo?
```bash
ps aux | grep "python.*app.py"
# Debe mostrar proceso activo
```

### ¿Cuántos backups tengo?
```bash
find backups/worlds/ -name "*.tar.gz" | wc -l
# Muestra número total de backups
```

### ¿Cuánto espacio ocupan los mundos?
```bash
du -sh worlds/*/
# Muestra tamaño de cada mundo
```

---

## Solución Rápida de Problemas

### Servidor no inicia
```bash
# Ver logs
docker logs mc-paper --tail 50

# Verificar symlink
readlink worlds/active
ls -la worlds/active/world/

# Reiniciar contenedor
docker restart mc-paper
```

### Panel web no accesible
```bash
# Verificar si está corriendo
ps aux | grep "python.*app.py"

# Reiniciar panel
cd web
./stop-web-panel.sh
./start-web-panel.sh

# Ver logs
tail -f web/panel.log
```

### Cambio de mundo falla
```bash
# Verificar mundo existe
ls -la worlds/{slug}/world/

# Verificar permisos
chmod 755 worlds/{slug}
chmod 755 worlds/{slug}/world*

# Reiniciar servidor manualmente
docker restart mc-paper
```

### Backup falla
```bash
# Verificar espacio en disco
df -h

# Verificar permisos
chmod 755 backups/worlds/
chmod 755 backups/worlds/{slug}/

# Crear backup manualmente
cd worlds/{slug}
tar -czf ../../backups/worlds/{slug}/manual_$(date +%Y%m%d_%H%M%S).tar.gz world world_nether world_the_end
```

---

## Recursos y Documentación

### Documentación Completa
- **[../README.md](../README.md)** - Documentación principal
- **[GUIA_MULTIMUNDOS.md](GUIA_MULTIMUNDOS.md)** - Guía completa multi-mundo (1,000+ líneas)
- **[BACKUP_SYSTEM.md](BACKUP_SYSTEM.md)** - Sistema de backups detallado
- **[PERFORMANCE_OPTIMIZATION.md](PERFORMANCE_OPTIMIZATION.md)** - Optimización de rendimiento
- **[RESUMEN_SISTEMA_V2.md](RESUMEN_SISTEMA_V2.md)** - Resumen técnico completo

### API REST
**24 Endpoints disponibles:**
- 8 Mundos (list, create, activate, delete, duplicate, config)
- 4 Backups (list, create, restore, delete)
- 2 Backup Config (get, update)
- 2 Panel Config (get, update)
- 8 Servidor (status, logs, players, TPS, commands, chat)

**Ejemplo:**
```bash
# Listar mundos
curl http://localhost:5000/api/worlds

# Obtener configuración de panel
curl http://localhost:5000/api/panel-config
```

### Soporte
- **Issues:** GitHub Issues
- **Logs:** `docker logs mc-paper` y `web/panel.log`
- **Tests:** `./run-tests.sh` para diagnóstico completo

---

## Consejos y Mejores Prácticas

### Seguridad
1. ✅ Cambiar contraseña en primer login
2. ✅ Usar SECRET_KEY aleatorio (32+ caracteres)
3. ✅ Configurar firewall (ufw allow 25565/tcp)
4. ✅ Backups automáticos activados

### Rendimiento
1. ✅ Configurar intervalos de polling según necesidad
2. ✅ Activar "Pausar cuando oculto"
3. ✅ Retención de backups: 5-10 (no más de necesario)
4. ✅ Limpiar mundos antiguos no usados

### Backups
1. ✅ Auto-backup: ON
2. ✅ Retención: 5 backups automáticos
3. ✅ Crear backup manual antes de cambios importantes
4. ✅ Backups programados con cron (diarios/semanales)

### Mundos
1. ✅ Slugs descriptivos: `survival-2025`, `creative-builds`
2. ✅ Descripciones claras
3. ✅ Configuración independiente por tipo de mundo
4. ✅ Duplicar antes de cambios experimentales

---

## Próximos Pasos

1. **Explorar el Panel Web**
   - Dashboard con estadísticas
   - Configuración de server.properties
   - Gestión de jugadores
   - Consola interactiva

2. **Crear Mundos Personalizados**
   - Mundo creativo para builds
   - Mundo survival hard
   - Mundo de testing

3. **Configurar Backups Programados**
   ```bash
   crontab -e
   # Añadir: 0 3 * * * /ruta/backup.sh
   ```

4. **Optimizar según tu Servidor**
   - Ajustar intervalos de polling
   - Configurar retención de backups
   - Limpiar mundos no usados

5. **Explorar Documentación Avanzada**
   - GUIA_MULTIMUNDOS.md para tutoriales
   - PERFORMANCE_OPTIMIZATION.md para optimización
   - BACKUP_SYSTEM.md para backups avanzados

---

<div align="center">

**¡Sistema Multi-Mundo v2.0 Listo!** 🎮🌍✨

**¿Necesitas ayuda?** Lee [GUIA_MULTIMUNDOS.md](GUIA_MULTIMUNDOS.md)

[⬆ Volver arriba](#-inicio-rápido---sistema-multi-mundo-v20)

</div>
