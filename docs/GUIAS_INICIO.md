# 🎮 Sistema MMORPG - Guía Rápida

## ⚡ Instalación Rápida (3 pasos)

### **Paso 1: Compilar el Plugin**
```bash
bash quick-install.sh
```

### **Paso 2: Iniciar el Servidor**
```bash
docker-compose up -d
# O si hay problemas con permisos:
sudo systemctl restart docker
docker-compose up -d
```

### **Paso 3: Iniciar el Panel Web**
```bash
bash start-web-panel.sh
```

**¡Listo!** Abre http://localhost:5000

---

## 📝 Comandos Útiles

### **Ver logs del servidor**
```bash
docker logs -f minecraft-paper | grep MMORPG
```

### **Reiniciar servidor (después de cambios al plugin)**
```bash
docker-compose restart
```

### **Verificar que el plugin cargó**
```bash
docker logs minecraft-paper 2>&1 | grep "MMORPGPlugin habilitado"
```

---

## 🔧 Desarrollo

### **Modificaste código Java?**
```bash
# 1. Compilar
bash quick-install.sh

# 2. Copiar al servidor (si está corriendo)
docker cp mmorpg-plugin/target/mmorpg-plugin-1.0.0.jar \
  minecraft-paper:/server/plugins/mmorpg-plugin-1.0.0.jar

# 3. Reiniciar
docker-compose restart
```

### **Solo trabajas en el panel web?**
```bash
bash start-web-panel.sh
# No necesitas recompilar el plugin
```

---

## ✅ El Plugin es Independiente

El plugin **CREA SUS PROPIOS ARCHIVOS** al iniciar:

- ✅ `pets_config.json` con 10 mascotas y 5 monturas por defecto
- ✅ `crafting_config.json` si no existe
- ✅ `enchantments_config.json` si no existe
- ✅ Carpeta `data/` para guardar información de jugadores

**NO necesitas copiar archivos manualmente.**

---

## 🐛 Problemas Conocidos

### **Docker no reinicia**
```bash
# Solo en local, no en producción
sudo systemctl restart docker
```

### **Panel web no inicia**
```bash
# Verificar que el puerto 5000 esté libre
lsof -i :5000
# Si está ocupado, matar el proceso
kill -9 $(lsof -ti:5000)
```

---

## 📚 Documentación Completa

- `ARQUITECTURA_MMORPG.md` - Diseño del sistema
- `GUIA_TESTING_PRODUCCION.md` - Tests completos
- `INSTALACION_PLUGIN_MMORPG.md` - Instalación detallada

---

**Contacto:** GitHub @Yupick  
**Proyecto:** mc-paper-docker  
**Branch:** mc-paper-mmorpg
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
# ✅ Configuración del Plugin MMORPG - Resumen

## 🔧 Cambios Realizados

### 1. **Script de Instalación Automática**
**Archivo:** `scripts/install-mmorpg-plugin.sh`

- ✅ Detecta si el plugin MMORPG está instalado
- ✅ Verifica archivos de configuración necesarios
- ✅ Copia archivos faltantes automáticamente
- ✅ Sincroniza archivos con el contenedor Docker si está corriendo
- ✅ Proporciona feedback visual con códigos de color

**Uso:**
```bash
bash scripts/install-mmorpg-plugin.sh
```

---

### 2. **Mejoras en start-web-panel.sh**
**Archivo:** `start-web-panel.sh`

**Cambios:**
- ✅ Agregadas funciones de utilidad (show_status, show_warning, etc.)
- ✅ Verificación automática del plugin MMORPG al iniciar
- ✅ Si falta algún archivo, ejecuta automáticamente el instalador
- ✅ Muestra estado de instalación del plugin

**Flujo:**
1. Inicia el script con `./start-web-panel.sh`
2. Verifica si los archivos de configuración del plugin existen
3. Si faltan, ejecuta automáticamente `install-mmorpg-plugin.sh`
4. Una vez completada la instalación, continúa con el panel web

---

### 3. **Correcciones en Archivos JSON**

#### enchantments_config.json
- ✅ Corregidos formatos de decimales (0.70 → 0.7, 0.85 → 0.85, etc.)
- ✅ Eliminados problemas de parsing de valores numéricos

**Linea 297:**
```json
"base_success_rate": 0.7,    // Antes: 0.70
"EPIC": 0.7,                 // Antes: 0.70
"LEGENDARY": 0.5             // Antes: 0.50
```

---

## 📋 Archivos Involucrados

| Archivo | Estado | Descripción |
|---------|--------|-------------|
| `scripts/install-mmorpg-plugin.sh` | ✅ Creado | Script de instalación con verificación automática |
| `start-web-panel.sh` | ✅ Actualizado | Agregada verificación y ejecución de instalador |
| `config/crafting_config.json` | ✅ Existente | 412 líneas, 15 recetas, listo |
| `config/enchantments_config.json` | ✅ Corregido | 308 líneas, 12 encantamientos, parsing mejorado |
| `config/pets_config.json` | ✅ Existente | 692 líneas, 10 mascotas, 5 monturas, listo |
| `config/rpg_world_layout.json` | ✅ Existente | 220 líneas, coordenadas del mundo, listo |
| `docker-compose.yml` | ✅ Actualizado | Agregado volumen para sincronización |

---

## 🚀 Cómo Usar

### Opción 1: Instalación Manual
```bash
bash scripts/install-mmorpg-plugin.sh
```

**Resultado esperado:**
```
✅ Directorio del plugin existe
✅ Copiado: crafting_config.json
✅ Copiado: enchantments_config.json
✅ Copiado: pets_config.json
✅ Copiado: rpg_world_layout.json
✅ 4 archivos sincronizados al contenedor
✅ Instalación completada exitosamente
```

### Opción 2: Instalación Automática (Recomendada)
```bash
./start-web-panel.sh
```

**Resultado esperado:**
```
📦 Verificando instalación del plugin MMORPG...
✅ Plugin MMORPG completamente instalado
✅ Entorno virtual activado
🌐 Iniciando servidor web...
✅ Panel web iniciado exitosamente
```

---

## 📊 Permisos Corregidos

Se corrigieron los permisos del directorio `/plugins/MMORPGPlugin/`:

```bash
sudo chown -R mkd:mkd /home/mkd/contenedores/mc-paper/plugins/MMORPGPlugin/
```

Ahora el usuario puede escribir archivos sin sudo.

---

## ✅ Verificación

### Verificar instalación manual
```bash
ls -lh plugins/MMORPGPlugin/*.json
```

**Debe mostrar:**
```
-rw-r--r--  crafting_config.json
-rw-r--r--  enchantments_config.json
-rw-r--r--  pets_config.json
-rw-r--r--  rpg_world_layout.json
```

### Verificar en contenedor Docker
```bash
docker exec minecraft-paper ls -la /server/plugins/MMORPGPlugin/*.json
```

---

## 🎯 Próximos Pasos

1. **Ejecutar instalación:**
   ```bash
   ./start-web-panel.sh
   ```

2. **Reiniciar el servidor para cargar configuraciones:**
   ```bash
   docker-compose restart
   ```

3. **Verificar logs del plugin:**
   ```bash
   docker logs minecraft-paper | grep MMORPGPlugin
   ```

4. **Acceder al panel web:**
   ```
   http://localhost:5000
   ```

---

## 🔍 Solución de Problemas

### Problema: "Permisos denegados" al copiar archivos
**Solución:**
```bash
sudo chown -R mkd:mkd plugins/MMORPGPlugin/
```

### Problema: Archivos no se sincronizan al contenedor
**Verificación:**
1. ¿Está el contenedor corriendo? `docker ps`
2. ¿Están los archivos en el host? `ls -l plugins/MMORPGPlugin/`
3. Ejecutar sincronización manual: `bash scripts/install-mmorpg-plugin.sh`

### Problema: Plugin no carga configuraciones
**Solución:**
1. Verificar archivos en contenedor: `docker exec minecraft-paper ls -lh /server/plugins/MMORPGPlugin/`
2. Reiniciar contenedor: `docker-compose restart`
3. Revisar logs: `docker logs minecraft-paper | grep -i error`

---

## 📝 Notas Técnicas

- Los archivos se montan en `/server/plugins/MMORPGPlugin/` dentro del contenedor
- El script de instalación detecta automáticamente si Docker está corriendo y sincroniza
- Los permisos se heredan del usuario propietario del directorio
- El script es idempotente (ejecutarlo múltiples veces es seguro)

---

**Última actualización:** 5 de diciembre de 2025
**Estado:** ✅ Implementación completada
