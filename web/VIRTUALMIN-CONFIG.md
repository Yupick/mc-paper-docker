# Configuración de Virtualmin para Panel Minecraft

## Opción 1: Subdominio (Recomendado) - panel.mc.nightslayer.com.ar

### Paso 1: Crear el Subdominio en Virtualmin

1. **Accede a Virtualmin**
   - Inicia sesión en Virtualmin (https://tu-servidor:10000)

2. **Selecciona el Virtual Server**
   - En el menú izquierdo, selecciona el dominio `nightslayer.com.ar`

3. **Crear Sub-Server**
   - Ve a: `Create Virtual Server` → `Sub-Server`
   - O: `Edit Virtual Server` → `Create Sub-Server`
   
   Configuración:
   ```
   Domain name: panel.mc.nightslayer.com.ar
   Description: Panel de Administración Minecraft
   Administration username: (usar el mismo del dominio principal)
   Directory for virtual server files: /home/nightslayer/domains/panel.mc.nightslayer.com.ar
   ```

4. **Habilitar solo lo necesario**
   - ✅ Setup DNS zone
   - ✅ Setup website
   - ✅ Setup SSL certificate
   - ❌ Deshabilitar email, bases de datos (no son necesarias)

### Paso 2: Configurar el Reverse Proxy

1. **Editar la configuración de Nginx/Apache**
   
   En Virtualmin:
   - Ve a: `Services` → `Configure Website`
   - O: `Server Configuration` → `Edit Directives`

2. **Para Nginx** (Recomendado):

   ```nginx
   server {
       listen 80;
       listen [::]:80;
       server_name panel.mc.nightslayer.com.ar;
       
       # Redirigir a HTTPS
       return 301 https://$server_name$request_uri;
   }

   server {
       listen 443 ssl http2;
       listen [::]:443 ssl http2;
       server_name panel.mc.nightslayer.com.ar;
       
       # Certificados SSL (Let's Encrypt)
       ssl_certificate /home/nightslayer/ssl.combined;
       ssl_certificate_key /home/nightslayer/ssl.key;
       
       # Configuración SSL
       ssl_protocols TLSv1.2 TLSv1.3;
       ssl_ciphers HIGH:!aNULL:!MD5;
       ssl_prefer_server_ciphers on;
       
       # Logs
       access_log /var/log/virtualmin/panel.mc.nightslayer.com.ar_access_log;
       error_log /var/log/virtualmin/panel.mc.nightslayer.com.ar_error_log;
       
       # Proxy al panel Flask
       location / {
           proxy_pass http://127.0.0.1:5000;
           proxy_http_version 1.1;
           
           # Headers necesarios
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
           proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
           proxy_set_header X-Forwarded-Proto $scheme;
           
           # WebSocket support (para futuras mejoras)
           proxy_set_header Upgrade $http_upgrade;
           proxy_set_header Connection "upgrade";
           
           # Timeouts
           proxy_connect_timeout 60s;
           proxy_send_timeout 60s;
           proxy_read_timeout 60s;
       }
       
       # Archivos estáticos (si los usas en el futuro)
       location /static {
           alias /home/mkd/contenedores/mc-paper/web/static;
           expires 30d;
           add_header Cache-Control "public, immutable";
       }
   }
   ```

3. **Para Apache**:

   ```apache
   <VirtualHost *:80>
       ServerName panel.mc.nightslayer.com.ar
       ServerAlias www.panel.mc.nightslayer.com.ar
       
       # Redirigir a HTTPS
       RewriteEngine On
       RewriteCond %{HTTPS} off
       RewriteRule ^(.*)$ https://%{HTTP_HOST}$1 [R=301,L]
   </VirtualHost>

   <VirtualHost *:443>
       ServerName panel.mc.nightslayer.com.ar
       ServerAlias www.panel.mc.nightslayer.com.ar
       
       # SSL
       SSLEngine on
       SSLCertificateFile /home/nightslayer/ssl.cert
       SSLCertificateKeyFile /home/nightslayer/ssl.key
       SSLCertificateChainFile /home/nightslayer/ssl.ca
       
       # Logs
       CustomLog /var/log/virtualmin/panel.mc.nightslayer.com.ar_access_log combined
       ErrorLog /var/log/virtualmin/panel.mc.nightslayer.com.ar_error_log
       
       # Proxy
       ProxyPreserveHost On
       ProxyPass / http://127.0.0.1:5000/
       ProxyPassReverse / http://127.0.0.1:5000/
       
       # Headers
       RequestHeader set X-Forwarded-Proto "https"
       RequestHeader set X-Forwarded-Port "443"
   </VirtualHost>
   ```

### Paso 3: Configurar SSL con Let's Encrypt

1. **En Virtualmin**:
   - Ve a: `Server Configuration` → `SSL Certificate`
   - Click en: `Let's Encrypt` tab
   - Selecciona: `Request Certificate`
   - Espera la confirmación

2. **O desde terminal**:
   ```bash
   sudo virtualmin generate-cert --domain panel.mc.nightslayer.com.ar --letsencrypt
   ```

### Paso 4: Configurar el Panel como Servicio Systemd

1. **Crear el archivo de servicio**:
   ```bash
   sudo nano /etc/systemd/system/minecraft-panel.service
   ```

2. **Contenido**:
   ```ini
   [Unit]
   Description=Panel Web Minecraft Server
   After=network.target

   [Service]
   Type=simple
   User=mkd
   Group=mkd
   WorkingDirectory=/home/mkd/contenedores/mc-paper/web
   Environment="PATH=/home/mkd/contenedores/mc-paper/web/venv/bin"
   ExecStart=/home/mkd/contenedores/mc-paper/web/venv/bin/gunicorn -w 4 -b 127.0.0.1:5000 app:app
   Restart=always
   RestartSec=10

   [Install]
   WantedBy=multi-user.target
   ```

3. **Instalar Gunicorn**:
   ```bash
   cd /home/mkd/contenedores/mc-paper/web
   source venv/bin/activate
   pip install gunicorn
   ```

4. **Habilitar y arrancar**:
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable minecraft-panel
   sudo systemctl start minecraft-panel
   sudo systemctl status minecraft-panel
   ```

### Paso 5: Configurar Firewall

1. **Si usas UFW**:
   ```bash
   # No necesitas abrir 5000 porque el proxy se encarga
   sudo ufw status
   ```

2. **Verificar que 80 y 443 estén abiertos**:
   ```bash
   sudo ufw allow 80/tcp
   sudo ufw allow 443/tcp
   ```

### Paso 6: Verificar

1. **Test del servicio**:
   ```bash
   curl http://localhost:5000
   ```

2. **Test del proxy**:
   ```bash
   curl http://panel.mc.nightslayer.com.ar
   ```

3. **Acceder desde navegador**:
   ```
   https://panel.mc.nightslayer.com.ar
   ```

---

## Opción 2: Subdirectorio - mc.nightslayer.com.ar/panel

### Configuración para Nginx

```nginx
location /panel {
    rewrite ^/panel(/.*)$ $1 break;
    proxy_pass http://127.0.0.1:5000;
    proxy_http_version 1.1;
    
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Script-Name /panel;
}
```

### Modificar Flask para subdirectorio

En `app.py`, agregar:
```python
from werkzeug.middleware.proxy_fix import ProxyFix

app.wsgi_app = ProxyFix(app.wsgi_app, x_proto=1, x_host=1)
app.config['APPLICATION_ROOT'] = '/panel'
```

---

## Permisos necesarios

### Usuario mkd necesita ejecutar docker con sudo

Agregar a `/etc/sudoers.d/minecraft-panel`:
```bash
sudo visudo -f /etc/sudoers.d/minecraft-panel
```

Contenido:
```
mkd ALL=(ALL) NOPASSWD: /usr/bin/docker-compose
mkd ALL=(ALL) NOPASSWD: /usr/bin/docker
```

### O agregar usuario al grupo docker:
```bash
sudo usermod -aG docker mkd
# Reiniciar sesión después
```

---

## Troubleshooting

### Ver logs del panel:
```bash
sudo journalctl -u minecraft-panel -f
```

### Ver logs de Nginx:
```bash
sudo tail -f /var/log/virtualmin/panel.mc.nightslayer.com.ar_error_log
```

### Reiniciar servicios:
```bash
sudo systemctl restart minecraft-panel
sudo systemctl restart nginx
# o
sudo systemctl restart httpd  # si usas Apache
```

### Verificar que el panel está corriendo:
```bash
sudo systemctl status minecraft-panel
sudo netstat -tlnp | grep 5000
```

### Test de conectividad:
```bash
curl -I http://localhost:5000
curl -I https://panel.mc.nightslayer.com.ar
```

---

## Seguridad adicional

### 1. Limitar acceso por IP (opcional)

En Nginx:
```nginx
location / {
    # Solo permitir estas IPs
    allow 192.168.1.0/24;  # Red local
    allow TU_IP_PUBLICA;    # Tu IP
    deny all;
    
    proxy_pass http://127.0.0.1:5000;
    # ... resto de configuración
}
```

### 2. Autenticación básica adicional

```bash
sudo apt install apache2-utils
sudo htpasswd -c /etc/nginx/.htpasswd admin
```

En Nginx:
```nginx
location / {
    auth_basic "Panel Minecraft";
    auth_basic_user_file /etc/nginx/.htpasswd;
    
    proxy_pass http://127.0.0.1:5000;
    # ... resto de configuración
}
```

### 3. Rate Limiting

En Nginx:
```nginx
limit_req_zone $binary_remote_addr zone=panel:10m rate=10r/s;

location / {
    limit_req zone=panel burst=20 nodelay;
    
    proxy_pass http://127.0.0.1:5000;
    # ... resto de configuración
}
```

---

## Resumen de pasos rápidos

```bash
# 1. Crear servicio systemd
sudo nano /etc/systemd/system/minecraft-panel.service

# 2. Instalar gunicorn
cd /home/mkd/contenedores/mc-paper/web
source venv/bin/activate
pip install gunicorn

# 3. Iniciar servicio
sudo systemctl daemon-reload
sudo systemctl enable minecraft-panel
sudo systemctl start minecraft-panel

# 4. Crear subdominio en Virtualmin (GUI)
# 5. Configurar proxy en Virtualmin (GUI o editar manualmente)
# 6. Solicitar certificado SSL en Virtualmin (GUI)

# 7. Verificar
curl http://localhost:5000
curl https://panel.mc.nightslayer.com.ar
```

¡Listo! El panel debería estar accesible en `https://panel.mc.nightslayer.com.ar`
