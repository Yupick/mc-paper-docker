# 🔧 Configuración RCON - Corrección de Consola Web

## 🚨 Problema Detectado

La consola web mostraba error "undefined" al ejecutar comandos porque **RCON no estaba habilitado** en el servidor.

## ✅ Cambios Aplicados

### 1. **server.properties**
```properties
# Antes:
enable-rcon=false
rcon.password=

# Después:
enable-rcon=true
rcon.password=minecraft123
```

### 2. **docker-compose.yml**
```yaml
# Puerto RCON agregado:
ports:
  - "25565:25565/tcp"  # Puerto Java
  - "19132:19132/udp"  # Puerto Bedrock
  - "25575:25575/tcp"  # Puerto RCON (NUEVO)

# Variable de entorno agregada:
environment:
  - EULA=TRUE
  - RCON_PASSWORD=minecraft123  # NUEVO
```

### 3. **dashboard.js**
- ✅ Mejorado manejo de errores en `executeCommand()`
- ✅ Agregado timeout de 1 segundo para `checkPasswordSecurity()`
- ✅ Agregado manejo de errores de red

### 4. **app.py (Backend)**
- ✅ Mejorado decodificación de output de RCON
- ✅ Mensajes de error más descriptivos
- ✅ Manejo de casos donde output es `None`

## 🔄 Cómo Aplicar los Cambios

### Opción 1: Reiniciar Contenedor (Rápido)
```bash
cd /home/mkd/contenedores/mc-paper
docker-compose restart
```

### Opción 2: Recrear Contenedor (Recomendado)
```bash
cd /home/mkd/contenedores/mc-paper
docker-compose down
docker-compose up -d
```

### Opción 3: Usar Script de Reinicio
```bash
./restart-server.sh
```

## ✅ Verificar que Funciona

### 1. Verificar que RCON está activo
```bash
docker logs mc-paper | grep -i rcon
```

Deberías ver algo como:
```
[Server thread/INFO]: RCON running on 0.0.0.0:25575
```

### 2. Probar RCON manualmente
```bash
docker exec mc-paper rcon-cli list
```

Debería mostrar la lista de jugadores conectados.

### 3. Probar desde el panel web
1. Acceder a `http://localhost:5000`
2. Ir a la sección **Consola**
3. Ejecutar comando: `list`
4. Debería mostrar los jugadores conectados (o "There are 0 players online" si no hay nadie)

## 🔒 Seguridad

### Cambiar Contraseña de RCON

**Para mayor seguridad, cambia la contraseña de RCON:**

1. Editar `config/server.properties`:
```properties
rcon.password=TU_PASSWORD_SEGURA_AQUI
```

2. Editar `docker-compose.yml`:
```yaml
environment:
  - RCON_PASSWORD=TU_PASSWORD_SEGURA_AQUI
```

3. Reiniciar servidor:
```bash
docker-compose restart
```

### Generar Contraseña Segura
```bash
openssl rand -base64 32
```

## 🐛 Solución de Problemas

### Error: "RCON no disponible"
```bash
# Verificar que el puerto está expuesto
docker port mc-paper

# Debería mostrar:
# 25575/tcp -> 0.0.0.0:25575
```

### Error: "Connection refused"
```bash
# Verificar que RCON está habilitado en server.properties
docker exec mc-paper cat /server/server.properties | grep rcon

# Debería mostrar:
# enable-rcon=true
# rcon.password=minecraft123
# rcon.port=25575
```

### Consola sigue sin funcionar
```bash
# Verificar logs del contenedor
docker logs mc-paper --tail 50

# Reiniciar contenedor
docker-compose restart

# Verificar que rcon-cli está instalado
docker exec mc-paper which rcon-cli
```

## 📋 Checklist de Verificación

- [ ] `enable-rcon=true` en server.properties
- [ ] `rcon.password` configurado en server.properties
- [ ] Puerto 25575 expuesto en docker-compose.yml
- [ ] Variable `RCON_PASSWORD` en docker-compose.yml
- [ ] Contenedor reiniciado después de cambios
- [ ] Panel web reiniciado: `./restart-web-panel.sh`
- [ ] Consola web funciona correctamente

## 🎯 Resultado Esperado

Después de aplicar estos cambios:

1. ✅ La consola web mostrará output correcto de comandos
2. ✅ Comandos como `list`, `tps`, `version` funcionarán
3. ✅ Podrás ejecutar cualquier comando de Minecraft desde el panel
4. ✅ El modal de cambio de contraseña aparecerá correctamente (después de 1 segundo)

---

**Fecha de aplicación:** 30 de noviembre de 2025  
**Versión del panel:** 2.0
