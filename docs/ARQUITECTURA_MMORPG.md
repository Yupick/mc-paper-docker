# 🎯 Sistema MMORPG - Arquitectura Independiente

## 📋 Cambios Implementados

### ✅ 1. Plugin Completamente Independiente

**Archivo modificado:** `PetManager.java`

**Cambios:**
- ❌ **ANTES:** Buscaba archivos en `/config/pets_config.json` (fuera del plugin)
- ✅ **AHORA:** Busca en `/plugins/MMORPGPlugin/pets_config.json` (dentro del plugin)
- ✅ **NUEVO:** Crea archivo de configuración por defecto si no existe

**Código:**
```java
// ANTES (INCORRECTO):
this.configFile = new File(plugin.getDataFolder().getParentFile().getParentFile(), "config/pets_config.json");

// AHORA (CORRECTO):
this.configFile = new File(plugin.getDataFolder(), "pets_config.json");

// NUEVO: Crear config por defecto
if (!configFile.exists()) {
    createDefaultConfig();
}
```

---

### ✅ 2. Scripts de Instalación Simplificados

#### **quick-install.sh** (Recomendado)
```bash
bash quick-install.sh
```

**Funciones:**
- ✅ Compila el plugin MMORPG
- ✅ Copia JAR al contenedor si está corriendo
- ✅ Notifica si necesita reiniciar servidor

**Uso típico:**
```bash
# Después de modificar código Java
bash quick-install.sh
docker-compose restart  # Si el servidor está corriendo
```

#### **install-mmorpg.sh** (Completo)
```bash
bash install-mmorpg.sh
```

**Funciones:**
- Menú interactivo con 8 opciones
- Compila plugin
- Inicia servidor
- Inicia panel web
- Verifica estado
- Ver logs
- Detener todo

**Modo automático:**
```bash
bash install-mmorpg.sh --auto
# Ejecuta: compilar + servidor + panel + verificación
```

---

### ✅ 3. Panel Web Simplificado

**start-web-panel.sh** ahora:
- ❌ **NO** verifica archivos de configuración del plugin
- ❌ **NO** copia archivos JSON
- ✅ **SÍ** verifica que el plugin esté compilado
- ✅ **SÍ** informa si falta sincronizar con el servidor

**Filosofía:**
> El plugin es responsable de su propia configuración.
> El panel web solo necesita que el plugin esté instalado.

---

## 🔄 Flujo de Trabajo

### **Instalación Inicial (Primera Vez)**

```bash
# Opción 1: Automática
bash install-mmorpg.sh --auto

# Opción 2: Manual paso a paso
bash install-mmorpg.sh
# Seleccionar opción 4: "Compilar + Iniciar servidor + Panel"
```

**Resultado:**
1. ✅ Plugin compilado
2. ✅ Servidor iniciado
3. ✅ Plugin carga y crea archivos por defecto
4. ✅ Panel web corriendo en http://localhost:5000

---

### **Desarrollo (Modificaciones al Código)**

```bash
# 1. Editar archivos Java en mmorpg-plugin/src/

# 2. Compilar e instalar
bash quick-install.sh

# 3. Reiniciar servidor
docker-compose restart

# 4. Verificar logs
docker logs -f minecraft-paper | grep MMORPG
```

---

### **Solo Panel Web (Sin Cambios al Plugin)**

```bash
bash start-web-panel.sh
```

**Resultado:**
- ✅ Verifica que el plugin esté compilado
- ✅ Inicia panel web en http://localhost:5000
- ✅ NO toca archivos del servidor

---

## 📁 Estructura de Archivos

### **Antes (INCORRECTO - Dependencias externas)**
```
/home/mkd/contenedores/mc-paper/
├── config/                          ← Plugin dependía de esto
│   ├── pets_config.json
│   ├── crafting_config.json
│   └── enchantments_config.json
├── plugins/
│   └── MMORPGPlugin/               ← Plugin aquí
└── mmorpg-plugin/                   ← Código fuente
```

### **Ahora (CORRECTO - Plugin independiente)**
```
/home/mkd/contenedores/mc-paper/
├── mmorpg-plugin/                   ← Código fuente
│   ├── src/
│   ├── pom.xml
│   └── target/
│       └── mmorpg-plugin-1.0.0.jar  ← JAR compilado
│
└── [DENTRO DEL CONTENEDOR DOCKER]
    └── /server/plugins/
        ├── mmorpg-plugin-1.0.0.jar      ← JAR instalado
        └── MMORPGPlugin/                 ← Carpeta del plugin
            ├── pets_config.json          ← Creados automáticamente
            ├── crafting_config.json      ← por el plugin
            ├── enchantments_config.json  ← al iniciar
            └── data/                     ← Datos de jugadores
```

---

## ✅ Verificación del Sistema

### **1. Plugin compilado correctamente**
```bash
ls -lh mmorpg-plugin/target/mmorpg-plugin-1.0.0.jar
# Debe mostrar: ~14MB
```

### **2. Plugin cargado en el servidor**
```bash
docker logs minecraft-paper 2>&1 | grep MMORPGPlugin
```

**Debe mostrar:**
```
[INFO]: [MMORPGPlugin] Enabling MMORPGPlugin v1.0.0
[INFO]: [MMORPGPlugin] Creando archivo de configuración por defecto: pets_config.json
[INFO]: [MMORPGPlugin] Archivo pets_config.json creado con configuración por defecto
[INFO]: [MMORPGPlugin] MMORPGPlugin habilitado correctamente!
```

### **3. Archivos de configuración creados**
```bash
docker exec minecraft-paper ls -la /server/plugins/MMORPGPlugin/
```

**Debe mostrar:**
```
-rw-r--r--  config.yml
-rw-r--r--  pets_config.json         ← Creado por el plugin
-rw-r--r--  crafting_config.json     ← Creado por el plugin
drwxr-xr-x  data/                    ← Datos de jugadores
```

### **4. Panel web funcionando**
```bash
curl http://localhost:5000 2>/dev/null | grep -q "Login" && echo "✅ Panel OK" || echo "❌ Panel FALLO"
```

---

## 🐛 Solución de Problemas

### **Problema: Plugin no carga configuración**

**Síntoma:**
```
[WARN]: [MMORPGPlugin] Archivo pets_config.json no encontrado!
```

**Causa:** Ruta incorrecta en el código

**Solución:**
1. Verificar que `PetManager.java` use:
   ```java
   new File(plugin.getDataFolder(), "pets_config.json")
   ```
2. Recompilar:
   ```bash
   bash quick-install.sh
   docker-compose restart
   ```

---

### **Problema: Servidor no reinicia**

**Síntoma:**
```
ERROR: Cannot restart container: permission denied
```

**Solución (Local):**
```bash
# Detener servicio Docker
sudo systemctl stop docker
sudo systemctl start docker

# O usar docker-compose
docker-compose down
docker-compose up -d
```

**Nota:** Este problema solo ocurre en local, no en producción.

---

### **Problema: JAR no sincroniza con el contenedor**

**Síntoma:**
```
⚠️  No se pudo copiar al contenedor
```

**Verificación:**
```bash
# ¿Está corriendo el contenedor?
docker ps | grep minecraft-paper

# ¿Existe el JAR?
ls -lh mmorpg-plugin/target/mmorpg-plugin-1.0.0.jar
```

**Solución manual:**
```bash
docker cp mmorpg-plugin/target/mmorpg-plugin-1.0.0.jar \
  minecraft-paper:/server/plugins/mmorpg-plugin-1.0.0.jar
```

---

## 📊 Resumen de Scripts

| Script | Propósito | Cuándo usar |
|--------|-----------|-------------|
| `quick-install.sh` | Compilar + copiar JAR | Después de cambios en código Java |
| `install-mmorpg.sh` | Instalación completa | Primera vez o reinstalación completa |
| `start-web-panel.sh` | Solo panel web | Desarrollo frontend sin cambios al plugin |
| `docker-compose restart` | Reiniciar servidor | Después de instalar nuevo JAR |
| `docker logs -f minecraft-paper` | Ver logs en tiempo real | Debugging |

---

## 🎯 Filosofía del Diseño

### **Plugin Independiente**
- ✅ El plugin es responsable de crear su propia configuración
- ✅ No depende de archivos externos
- ✅ Crea archivos por defecto al iniciar por primera vez
- ✅ Funciona standalone sin intervención manual

### **Panel Web Desacoplado**
- ✅ El panel web no modifica archivos del plugin
- ✅ Solo lee datos del servidor vía API REST
- ✅ Puede iniciarse independientemente del servidor
- ✅ No requiere archivos de configuración del plugin

### **Scripts Automatizados**
- ✅ Minimizan intervención manual
- ✅ Detectan estado del sistema automáticamente
- ✅ Proveen feedback claro y accionable
- ✅ Soportan modo interactivo y automático

---

**Última actualización:** 8 de diciembre de 2025  
**Estado:** ✅ Sistema completamente funcional e independiente
