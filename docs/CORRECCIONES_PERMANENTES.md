# ✅ Correcciones Permanentes Aplicadas

## 📋 Resumen

Todas las correcciones necesarias para que RCON y el panel web funcionen correctamente desde una instalación nueva.

---

## 🔧 Archivos Corregidos

### 1. **Dockerfile** ✅

**Cambios aplicados:**
- ✅ Agregado `wget` a dependencias
- ✅ **rcon-cli instalado** automáticamente en la imagen
- ✅ **Puerto 25575 expuesto** para RCON

```dockerfile
# Instalar dependencias necesarias
RUN apt-get update && \
    apt-get install -y curl jq wget && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Instalar rcon-cli para ejecutar comandos remotos
RUN wget -O /usr/local/bin/rcon-cli https://github.com/itzg/rcon-cli/releases/download/1.6.0/rcon-cli_1.6.0_linux_amd64 && \
    chmod +x /usr/local/bin/rcon-cli

# Exponer puertos
EXPOSE 25565/tcp  # Java Edition
EXPOSE 19132/udp  # Bedrock Edition
EXPOSE 25575/tcp  # RCON (NUEVO)
```

---

### 2. **docker-compose.yml** ✅

**Cambios aplicados:**
- ✅ **Puerto 25575 mapeado** (RCON)
- ✅ **Variable RCON_PASSWORD** configurada

```yaml
ports:
  - "25565:25565/tcp"  # Puerto Java
  - "19132:19132/udp"  # Puerto Bedrock
  - "25575:25575/tcp"  # Puerto RCON (NUEVO)

environment:
  - EULA=TRUE
  - RCON_PASSWORD=minecraft123  # NUEVO
```

---

### 3. **create.sh** ✅

**Cambios aplicados:**
- ✅ `enable-rcon=true` en server.properties generado
- ✅ `rcon.password=minecraft123` configurado
- ✅ Mensaje informativo sobre RCON en output

```properties
# En server.properties generado por create.sh:
enable-rcon=true
rcon.password=minecraft123
rcon.port=25575
```

**Mensaje de salida actualizado:**
```
Puertos:
  - Java Edition: 25565
  - Bedrock Edition: 19132
  - RCON (Panel Web): 25575

RCON:
  - Puerto: 25575
  - Contraseña: minecraft123
  - Estado: Habilitado
```

---

### 4. **config/server.properties** ✅

**Estado actual:**
- ✅ `enable-rcon=true`
- ✅ `rcon.password=minecraft123`
- ✅ `rcon.port=25575`

---

### 5. **web/.env** ✅

**Cambios aplicados:**
- ✅ Nombre del contenedor corregido

```env
DOCKER_CONTAINER_NAME=minecraft-paper
```

---

### 6. **web/templates/dashboard_v2.html** ✅

**Estado:**
- ✅ Modal de cambio de contraseña implementado
- ✅ Cache buster en dashboard.js: `?v=2.0.1`

---

### 7. **web/static/dashboard.js** ✅

**Cambios aplicados:**
- ✅ Función `checkPasswordSecurity()` implementada
- ✅ Función `submitPasswordChange()` implementada
- ✅ `setTimeout(() => checkPasswordSecurity(), 1000)` en DOMContentLoaded
- ✅ Manejo de errores mejorado en `executeCommand()`

---

## 🎯 Resultado Final

### ✅ En Instalación Nueva (usando create.sh)

Cuando ejecutes `./create.sh`:
1. ✅ Dockerfile construye imagen con rcon-cli incluido
2. ✅ server.properties se genera con RCON habilitado
3. ✅ docker-compose expone puerto 25575
4. ✅ Panel web está configurado con el nombre correcto del contenedor
5. ✅ Consola web funcionará inmediatamente

### ✅ En Instalación Existente

Para aplicar correcciones a una instalación existente:

**Opción 1: Reconstruir (permanente)**
```bash
cd /home/mkd/contenedores/mc-paper
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

**Opción 2: Rápido (temporal)**
```bash
# Solo si no quieres reconstruir
docker exec -u root minecraft-paper bash -c 'apt-get update && apt-get install -y wget && wget -O /usr/local/bin/rcon-cli https://github.com/itzg/rcon-cli/releases/download/1.6.0/rcon-cli_1.6.0_linux_amd64 && chmod +x /usr/local/bin/rcon-cli'
```

---

## 🧪 Verificación

### Verificar que RCON funciona:
```bash
# Verificar que rcon-cli está instalado
docker exec minecraft-paper which rcon-cli

# Probar comando
docker exec minecraft-paper rcon-cli list

# Verificar puerto expuesto
docker port minecraft-paper 25575
```

### Verificar Panel Web:
```bash
# Reiniciar panel
cd web
./restart-web-panel.sh

# Acceder
# http://localhost:5000
```

---

## 📦 Archivos de Scripts de Ayuda

### Scripts Creados:

1. **`aplicar-correcciones.sh`**
   - Reconstruye imagen Docker
   - Reinicia servicios
   - Verifica RCON

2. **`correccion-rapida.sh`**
   - Solo reinicia panel web
   - Muestra comando para instalar rcon-cli

---

## 🔒 Seguridad

### Cambiar Contraseña de RCON

**Para producción, cambia la contraseña por defecto:**

1. Editar `config/server.properties`:
```properties
rcon.password=TU_PASSWORD_SEGURA
```

2. Editar `docker-compose.yml`:
```yaml
environment:
  - RCON_PASSWORD=TU_PASSWORD_SEGURA
```

3. Editar `create.sh` (para instalaciones futuras):
```properties
rcon.password=TU_PASSWORD_SEGURA
```

4. Reiniciar:
```bash
docker-compose restart
```

### Generar Contraseña Segura:
```bash
openssl rand -base64 32
```

---

## ✅ Checklist de Correcciones Permanentes

- [x] Dockerfile instala rcon-cli automáticamente
- [x] Dockerfile expone puerto 25575
- [x] docker-compose.yml mapea puerto 25575
- [x] docker-compose.yml tiene variable RCON_PASSWORD
- [x] create.sh genera server.properties con RCON habilitado
- [x] create.sh configura rcon.password
- [x] web/.env tiene nombre correcto del contenedor
- [x] web/templates/dashboard_v2.html tiene modal de cambio de contraseña
- [x] web/static/dashboard.js implementa checkPasswordSecurity()
- [x] config/server.properties actual tiene RCON habilitado

---

**Fecha de correcciones:** 30 de noviembre de 2025  
**Versión del panel:** 2.0.1  
**Estado:** ✅ TODAS LAS CORRECCIONES PERMANENTES APLICADAS

---

## 🚀 Próxima Instalación Nueva

La próxima vez que ejecutes:
```bash
./create.sh
```

Todo funcionará automáticamente:
- ✅ RCON habilitado desde el inicio
- ✅ rcon-cli instalado en el contenedor
- ✅ Panel web con consola funcional
- ✅ Modal de cambio de contraseña implementado

**¡No necesitarás hacer correcciones manuales!** 🎉
