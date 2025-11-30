# Objetivo
Quiero que generes los siguientes archivos para un servidor Minecraft Paper dentro de Docker, con soporte para usuarios Java y Bedrock (usando GeyserMC y Floodgate), y que además acepte clientes de múltiples versiones de Minecraft Java (usando ViaVersion, ViaBackwards y ViaRewind). La persistencia de datos debe estar fuera del contenedor, incluyendo carpetas para addons, texturas, configuraciones y mundos.

# Archivos requeridos

## 1. Dockerfile
- Imagen base: openjdk:21-jdk-slim
- Descargar automáticamente la última versión de PaperMC.
- **No descargar plugins dentro del contenedor.**
- Exponer puertos: 25565/tcp (Java) y 19132/udp (Bedrock).
- Comando de inicio: `java -Xms1G -Xmx2G -jar paper.jar --nogui`.

## 2. docker-compose.yml
- Servicio `minecraft`.
- Montar volúmenes externos:
  - `./worlds:/server` (mundos y todos los datos del servidor).
  - `./plugins:/server/plugins` (addons/plugins).
  - `./resourcepacks:/server/resourcepacks` (texturas).
  - `./config/server.properties:/server/server.properties` (archivo de configuración).
- Puertos mapeados: 25565 y 19132/udp.
- Reinicio automático: `unless-stopped`.

## 3. create.sh
Script en Bash que:
- Cree las carpetas externas: `worlds/`, `plugins/`, `resourcepacks/`, `config/`.
- Descargue **siempre las últimas versiones disponibles** de los siguientes plugins en la carpeta `plugins/` **antes de levantar el contenedor**:
  - GeyserMC
  - Floodgate
  - ViaVersion
  - ViaBackwards
  - ViaRewind
- Construya la imagen con `docker-compose build`.
- Cree y levante el contenedor con `docker-compose up -d`.

## 4. update.sh
Script en Bash que:
- Detenga el contenedor.
- Elimine la imagen actual.
- Vuelva a construir la imagen (descargando la última versión de PaperMC).
- Reinicie el contenedor.
- **No descarga plugins, ya que estos se gestionan externamente.**

## 5. run.sh
Script en Bash que:
- Ejecute el contenedor si está detenido.
- Muestre logs en tiempo real con `docker-compose logs -f`.

## 6. stop.sh
Script en Bash que:
- Detenga el contenedor con `docker-compose down`.
- Muestre mensaje: "Servidor detenido correctamente".

## 7. uninstall.sh
Script en Bash que:
- Pregunte confirmación: "¿Seguro que deseas desinstalar el servidor Minecraft (s/n)?"
- Si la respuesta es "s":
  - Detenga y elimine el contenedor.
  - Pregunte confirmación: "¿Deseas eliminar también los datos del mundo y configuraciones (s/n)?"
  - Si la respuesta es "s":
    - Elimine las carpetas externas: `worlds/`, `plugins/`, `resourcepacks/`, `config/server.properties`.
- Muestre mensajes claros en cada paso.

# Requisitos adicionales
- Los scripts deben tener `#!/bin/bash` al inicio.
- Deben incluir mensajes de log claros (ej: "Construyendo imagen...", "Servidor actualizado", "Servidor detenido").
- Los volúmenes externos deben permitir actualizar addons, texturas, configuraciones y mundos sin reconstruir el contenedor.
- **Los plugins se descargan en el script `create.sh` antes de levantar el servidor por primera vez.**
- **Siempre se debe descargar la última versión disponible de cada plugin (GeyserMC, Floodgate, ViaVersion, ViaBackwards, ViaRewind).**
Aquí tienes los archivos solicitados para configurar un servidor Minecraft Paper con soporte para Java y Bedrock, utilizando Docker y los plugins mencionados.