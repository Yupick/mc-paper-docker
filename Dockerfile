FROM eclipse-temurin:21-jdk-jammy

# Instalar dependencias necesarias
RUN apt-get update && \
    apt-get install -y curl jq wget && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Instalar mcrcon para ejecutar comandos remotos RCON
RUN cd /tmp && \
    wget https://github.com/Tiiffi/mcrcon/releases/download/v0.7.2/mcrcon-0.7.2-linux-x86-64.tar.gz && \
    tar -xzf mcrcon-0.7.2-linux-x86-64.tar.gz && \
    mv mcrcon /usr/local/bin/ && \
    chmod +x /usr/local/bin/mcrcon && \
    rm -f mcrcon-0.7.2-linux-x86-64.tar.gz && \
    ln -s /usr/local/bin/mcrcon /usr/local/bin/rcon-cli

# Crear directorio de trabajo
WORKDIR /server

# Descargar la última versión ESTABLE de PaperMC (sin pre-releases)
RUN MINECRAFT_VERSION=$(curl -s https://api.papermc.io/v2/projects/paper | jq -r '.versions[] | select(test("^[0-9]+\\.[0-9]+(\\.[0-9]+)?$"))' | tail -1) && \
    echo "Descargando PaperMC versión ESTABLE ${MINECRAFT_VERSION}..." && \
    BUILD=$(curl -s https://api.papermc.io/v2/projects/paper/versions/${MINECRAFT_VERSION}/builds | jq -r '.builds[-1].build') && \
    curl -o paper.jar https://api.papermc.io/v2/projects/paper/versions/${MINECRAFT_VERSION}/builds/${BUILD}/downloads/paper-${MINECRAFT_VERSION}-${BUILD}.jar && \
    echo "PaperMC descargado: $(ls -lh paper.jar)"

# Aceptar EULA automáticamente
RUN echo "eula=true" > eula.txt

# Exponer puertos
EXPOSE 25565/tcp
EXPOSE 19132/udp
EXPOSE 25575/tcp

# Comando de inicio
CMD ["java", "-Xms1G", "-Xmx2G", "-jar", "paper.jar", "--nogui"]
