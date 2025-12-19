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
