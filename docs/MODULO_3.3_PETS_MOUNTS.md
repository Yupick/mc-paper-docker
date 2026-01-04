# Módulo 3.3: Sistema de Mascotas y Monturas

## 📋 Índice

1. [Resumen del Sistema](#resumen-del-sistema)
2. [Arquitectura](#arquitectura)
3. [Configuración](#configuración)
4. [Comandos del Juego](#comandos-del-juego)
5. [API Web](#api-web)
6. [Estructuras de Datos](#estructuras-de-datos)
7. [Integración](#integración)
8. [Pruebas](#pruebas)
9. [Troubleshooting](#troubleshooting)

---

## 📝 Resumen del Sistema

El sistema de Mascotas y Monturas permite a los jugadores:

- **Adoptar mascotas** con diferentes rarezas (COMMON, UNCOMMON, RARE, LEGENDARY)
- **Alimentar** mascotas para restaurar salud y hambre
- **Evolucionar** mascotas ganando experiencia
- **Equipar** mascotas activas que los acompañan
- **Desbloquear monturas** para viajar más rápido
- **Usar habilidades** especiales de mascotas
- **Gestionar** todo desde el panel web

### Características Principales

- ✅ 10 tipos de mascotas con sistema de evolución (3 niveles cada una)
- ✅ 5 monturas con velocidades progresivas (1.2x a 2.8x)
- ✅ 21 habilidades únicas con cooldowns
- ✅ Sistema de hambre y salud para mascotas
- ✅ Integración completa con economía (RPG Coins)
- ✅ Panel web para gestión visual
- ✅ Persistencia en JSON compatible Java/Python

---

## 🏗️ Arquitectura

### Diagrama de Componentes

```
┌─────────────────────────────────────────────────────────┐
│                    JUGADOR                              │
└──────────────┬───────────────────┬──────────────────────┘
               │                   │
       Comandos /pet          Panel Web
               │                   │
       ┌───────▼──────┐    ┌──────▼──────────┐
       │  PetCommand  │    │  Flask API      │
       │  (Java)      │    │  (Python)       │
       └───────┬──────┘    └──────┬──────────┘
               │                   │
               └────────┬──────────┘
                        │
               ┌────────▼─────────┐
               │   PetManager     │
               │   (Java Core)    │
               └────────┬─────────┘
                        │
        ┌───────────────┴───────────────┐
        │                               │
   ┌────▼─────────┐           ┌────────▼────────┐
   │pets_config.  │           │{uuid}.json      │
   │json          │           │(Datos jugador)  │
   │(Configuración│           └─────────────────┘
   │ del juego)   │
   └──────────────┘
```

### Flujo de Datos

1. **Adopción de Mascota (In-Game)**:
   - Jugador: `/pet adopt wolf_pup`
   - PetCommand valida comando
   - EconomyManager verifica/deduce monedas
   - PetManager crea entrada en `{uuid}.json`
   - Panel web lee cambios automáticamente

2. **Adopción de Mascota (Web)**:
   - Jugador: Click en "Adoptar" en panel
   - API POST `/api/rpg/pets/adopt`
   - Python escribe en `{uuid}.json`
   - Próximo ingreso al servidor: Java lee cambios

---

## ⚙️ Configuración

### Archivo: `plugins/MMORPGPlugin/pets_config.json`

Estructura principal:

```json
{
  "pet_settings": {
    "max_pets_per_player": 10,
    "hunger_decay_rate": 0.5,
    "xp_per_mob_kill": 10
  },
  "pets": [
    {
      "id": "wolf_pup",
      "display_name": "Cachorro de Lobo",
      "rarity": "COMMON",
      "adoption_cost": 50,
      "base_stats": {
        "health": 100.0,
        "attack_damage": 5.0
      },
      "evolution_levels": [
        {
          "level": 1,
          "xp_required": 0,
          "stats_multiplier": 1.0,
          "abilities": ["bite"]
        },
        {
          "level": 2,
          "xp_required": 100,
          "stats_multiplier": 1.5,
          "abilities": ["bite", "howl"]
        },
        {
          "level": 3,
          "xp_required": 250,
          "stats_multiplier": 2.0,
          "abilities": ["bite", "howl", "pack_leader"]
        }
      ]
    }
  ],
  "mounts": [
    {
      "id": "horse_brown",
      "display_name": "Caballo Marrón",
      "unlock_cost": 100,
      "unlock_level": 5,
      "speed_multiplier": 1.2
    }
  ],
  "abilities": [
    {
      "id": "bite",
      "display_name": "Mordisco",
      "cooldown_seconds": 5,
      "effect": "Ataque básico de mascota"
    }
  ]
}
```

### Agregar Nueva Mascota

1. Añadir entrada en el array `pets`:

```json
{
  "id": "dragon_baby",
  "display_name": "Dragón Bebé",
  "rarity": "LEGENDARY",
  "adoption_cost": 1000,
  "base_stats": {
    "health": 200.0,
    "attack_damage": 15.0
  },
  "evolution_levels": [
    {
      "level": 1,
      "xp_required": 0,
      "stats_multiplier": 1.0,
      "abilities": ["fireball"]
    },
    {
      "level": 2,
      "xp_required": 500,
      "stats_multiplier": 1.8,
      "abilities": ["fireball", "dragon_breath"]
    },
    {
      "level": 3,
      "xp_required": 1000,
      "stats_multiplier": 3.0,
      "abilities": ["fireball", "dragon_breath", "inferno"]
    }
  ]
}
```

2. Reiniciar servidor o usar `/pet reload` (si implementado)

### Agregar Nueva Montura

```json
{
  "id": "unicorn_rainbow",
  "display_name": "Unicornio Arcoíris",
  "unlock_cost": 5000,
  "unlock_level": 50,
  "speed_multiplier": 2.5
}
```

---

## 🎮 Comandos del Juego

Comando base: `/pet`

### Lista de Subcomandos

| Comando | Descripción | Ejemplo |
|---------|-------------|---------|
| `list` | Ver mascotas disponibles | `/pet list` |
| `adopt <id>` | Adoptar mascota | `/pet adopt wolf_pup` |
| `info [id]` | Ver info de mascota | `/pet info` o `/pet info wolf_pup` |
| `feed <id>` | Alimentar mascota | `/pet feed wolf_pup` |
| `evolve <id>` | Evolucionar mascota | `/pet evolve wolf_pup` |
| `release <id>` | Liberar mascota | `/pet release wolf_pup` |
| `equip <id>` | Equipar mascota | `/pet equip wolf_pup` |
| `mounts` | Ver monturas | `/pet mounts` |
| `unlock-mount <id>` | Desbloquear montura | `/pet unlock-mount horse_brown` |
| `equip-mount <id>` | Equipar montura | `/pet equip-mount horse_brown` |

### Permisos

Permiso principal: `mmorpg.pet` (definido en `plugin.yml`)

### Ejemplos de Uso

```bash
# Ver mascotas disponibles
/pet list

# Adoptar un cachorro de lobo (cuesta 50 coins)
/pet adopt wolf_pup

# Ver información de todas mis mascotas
/pet info

# Alimentar a la mascota equipada
/pet feed wolf_pup

# Evolucionar cuando tenga suficiente XP
/pet evolve wolf_pup

# Equipar la mascota para que me acompañe
/pet equip wolf_pup

# Ver monturas disponibles
/pet mounts

# Desbloquear caballo (nivel 5, 100 coins)
/pet unlock-mount horse_brown

# Montar el caballo
/pet equip-mount horse_brown
```

---

## 🌐 API Web

Base URL: `/api/rpg/pets`

### Endpoints

#### 1. Listar Mascotas Disponibles

```http
GET /api/rpg/pets/list
```

**Respuesta:**
```json
{
  "pets": [
    {
      "id": "wolf_pup",
      "display_name": "Cachorro de Lobo",
      "rarity": "COMMON",
      "adoption_cost": 50,
      "base_stats": {...}
    }
  ]
}
```

#### 2. Obtener Mis Mascotas

```http
GET /api/rpg/pets/my-pets
```

**Respuesta:**
```json
{
  "pets": [
    {
      "petId": "wolf_pup",
      "customName": "Lobo",
      "level": 2,
      "experience": 150,
      "currentHealth": 150.0,
      "hungerLevel": 80.0,
      "lastFedTimestamp": 1234567890000,
      "max_health": 150.0,
      "next_evolution_xp": 250
    }
  ]
}
```

#### 3. Adoptar Mascota

```http
POST /api/rpg/pets/adopt
Content-Type: application/json

{
  "pet_id": "wolf_pup",
  "custom_name": "Mi Lobo"
}
```

**Respuesta:**
```json
{
  "success": true,
  "message": "Mascota adoptada correctamente"
}
```

#### 4. Alimentar Mascota

```http
POST /api/rpg/pets/feed
Content-Type: application/json

{
  "pet_id": "wolf_pup"
}
```

**Respuesta:**
```json
{
  "success": true,
  "message": "Mascota alimentada"
}
```

#### 5. Evolucionar Mascota

```http
POST /api/rpg/pets/evolve
Content-Type: application/json

{
  "pet_id": "wolf_pup"
}
```

**Respuesta:**
```json
{
  "success": true,
  "message": "¡Mascota evolucionada al nivel 2!"
}
```

#### 6. Equipar Mascota

```http
POST /api/rpg/pets/equip
Content-Type: application/json

{
  "pet_id": "wolf_pup"
}
```

**Respuesta:**
```json
{
  "success": true,
  "message": "Mascota equipada correctamente"
}
```

#### 7. Liberar Mascota

```http
POST /api/rpg/pets/release
Content-Type: application/json

{
  "pet_id": "wolf_pup"
}
```

**Respuesta:**
```json
{
  "success": true,
  "message": "Has liberado a Mi Lobo"
}
```

#### 8. Ver Monturas

```http
GET /api/rpg/pets/mounts
```

**Respuesta:**
```json
{
  "mounts": [
    {
      "id": "horse_brown",
      "display_name": "Caballo Marrón",
      "unlock_cost": 100,
      "unlock_level": 5,
      "speed_multiplier": 1.2,
      "unlocked": true
    }
  ]
}
```

#### 9. Desbloquear Montura

```http
POST /api/rpg/pets/unlock-mount
Content-Type: application/json

{
  "mount_id": "horse_brown"
}
```

**Respuesta:**
```json
{
  "success": true,
  "message": "Montura desbloqueada"
}
```

#### 10. Obtener Estadísticas

```http
GET /api/rpg/pets/stats
```

**Respuesta:**
```json
{
  "total_pets": 3,
  "total_mounts": 2,
  "total_evolutions": 5,
  "coins_spent": 450,
  "activity_history": []
}
```

---

## 📊 Estructuras de Datos

### PlayerPetData (JSON per jugador)

**Ubicación:** `plugins/MMORPGPlugin/pets/{uuid}.json`

```json
{
  "playerUuid": "12345678-1234-1234-1234-123456789012",
  "ownedPets": [
    {
      "petId": "wolf_pup",
      "customName": "Mi Lobo",
      "level": 2,
      "experience": 150,
      "currentHealth": 150.0,
      "hungerLevel": 80.0,
      "lastFedTimestamp": 1234567890000,
      "abilityCooldowns": {
        "bite": 0,
        "howl": 0
      }
    }
  ],
  "unlockedMounts": [
    "horse_brown",
    "pegasus_sky"
  ],
  "activePetId": "wolf_pup",
  "activeMountId": "horse_brown"
}
```

### Clases Java

#### PetManager.java

Métodos principales:

```java
public boolean adoptPet(Player player, String petId, String customName)
public boolean feedPet(Player player, String petId)
public boolean evolvePet(Player player, String petId)
public boolean releasePet(Player player, String petId)
public boolean setActivePet(Player player, String petId)
public boolean unlockMount(Player player, String mountId)
public boolean setActiveMount(Player player, String mountId)
public Map<String, Object> getEvolutionProgress(Player player, String petId)
```

---

## 🔗 Integración

### Java ↔ Python

**Ambos sistemas comparten los mismos archivos JSON:**

1. **Java escribe** cuando jugador usa comandos in-game
2. **Python lee** esos archivos para mostrar en panel web
3. **Python escribe** cuando jugador usa panel web
4. **Java lee** al siguiente login o reload

### Sincronización

- ✅ **Sin conflictos**: Archivos por UUID evitan colisiones
- ✅ **Encoding UTF-8**: Soporte para caracteres especiales
- ✅ **Timestamps Java**: Milisegundos desde epoch
- ✅ **Estructura idéntica**: Validada en ambos lados

### Integración con Economía

```java
// En PetCommand.java (comando adopt)
int playerCoins = economyManager.getRPGCoins(player);
if (playerCoins < adoptionCost) {
    player.sendMessage("§cNo tienes suficientes monedas");
    return true;
}

if (economyManager.withdrawRPGCoins(player, adoptionCost)) {
    petManager.adoptPet(player, petId, customName);
}
```

```python
# En app.py - TODO: Implementar verificación
# Actualmente solo escribe JSON, no verifica monedas
# Futuro: Integrar con EconomyManager o leer economy.json
```

---

## 🧪 Pruebas

### Checklist de Testing

#### Pruebas In-Game

- [ ] `/pet list` muestra 10 mascotas
- [ ] `/pet adopt wolf_pup` deduce 50 coins
- [ ] `/pet info` muestra mascota adoptada
- [ ] `/pet feed wolf_pup` restaura hambre/salud
- [ ] `/pet evolve wolf_pup` funciona con XP suficiente
- [ ] `/pet equip wolf_pup` marca mascota activa
- [ ] `/pet mounts` muestra 5 monturas
- [ ] `/pet unlock-mount horse_brown` deduce 100 coins
- [ ] `/pet equip-mount horse_brown` aumenta velocidad
- [ ] `/pet release wolf_pup` elimina mascota

#### Pruebas Panel Web

- [ ] **Tab "Tienda"**: Muestra 10 mascotas con precios
- [ ] **Botón "Adoptar"**: Crea mascota en JSON
- [ ] **Tab "Mis Mascotas"**: Lista mascotas adoptadas
- [ ] **Botón "Alimentar"**: Actualiza hambre/salud
- [ ] **Botón "Evolucionar"**: Sube nivel con XP suficiente
- [ ] **Botón "Equipar"**: Marca como activa
- [ ] **Tab "Monturas"**: Muestra 5 monturas
- [ ] **Botón "Desbloquear"**: Añade a unlockedMounts
- [ ] **Tab "Estadísticas"**: Muestra totales correctos

#### Pruebas de Integración

- [ ] Adoptar in-game → Panel web refleja cambio
- [ ] Adoptar en web → `/pet info` muestra mascota
- [ ] Alimentar in-game → Panel muestra stats actualizados
- [ ] Evolucionar en web → Nivel in-game aumenta
- [ ] Desbloquear montura en web → `/pet mounts` la lista
- [ ] Sincronización bidireccional funciona

#### Pruebas de Persistencia

- [ ] Desconectar → Reconectar: Mascotas persisten
- [ ] Reiniciar servidor: JSON se carga correctamente
- [ ] Múltiples jugadores: Archivos UUID separados
- [ ] UTF-8 en nombres custom: Se guarda correctamente

### Comandos de Testing

```bash
# Verificar archivos creados
ls -lh plugins/MMORPGPlugin/pets/

# Ver contenido de datos de jugador
cat plugins/MMORPGPlugin/pets/{uuid}.json | jq

# Ver logs del plugin
tail -f logs/latest.log | grep MMORPGPlugin

# Verificar economía del jugador
/money {player}

# Dar XP a mascota para testing (si implementado)
/pet addxp wolf_pup 100
```

---

## 🐛 Troubleshooting

### Problemas Comunes

#### 1. Comando `/pet` no funciona

**Causa**: Plugin no cargado o error en `plugin.yml`

**Solución**:
```bash
# Ver logs
grep "MMORPGPlugin" logs/latest.log

# Verificar plugins activos
/plugins

# Recargar plugin
/reload confirm
```

#### 2. No se crean archivos JSON

**Causa**: Permisos de directorio

**Solución**:
```bash
chmod -R 755 plugins/MMORPGPlugin/
mkdir -p plugins/MMORPGPlugin/pets
```

#### 3. Panel web no muestra mascotas

**Causa**: Ruta incorrecta en `app.py`

**Verificar**:
```python
# En app.py, verificar:
PLUGINS_DIR = os.path.join(MINECRAFT_DIR, 'plugins')
pets_config_path = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'pets_config.json')
```

#### 4. Error "No tienes suficientes monedas"

**Causa**: EconomyManager no cargado o sin coins

**Solución**:
```bash
# Dar coins al jugador
/money give {player} 1000

# Verificar economía
/money {player}
```

#### 5. Mascota no evoluciona

**Causa**: XP insuficiente

**Verificar**:
```bash
# Ver progreso de evolución
/pet info wolf_pup

# Debería mostrar: XP: 150/250
# Si XP < requerido, dar más XP (matar mobs o comando)
```

#### 6. Error al deserializar JSON

**Causa**: Formato incorrecto en archivo JSON

**Solución**:
```bash
# Validar JSON
python3 -m json.tool plugins/MMORPGPlugin/pets_config.json

# Si hay errores, corregir o regenerar
cp config/pets_config.json plugins/MMORPGPlugin/pets_config.json
```

#### 7. Panel web muestra "Configuración no encontrada"

**Causa**: `pets_config.json` no existe en ubicación correcta

**Solución**:
```bash
# Copiar configuración
cp config/pets_config.json plugins/MMORPGPlugin/pets_config.json

# Verificar permisos
chmod 644 plugins/MMORPGPlugin/pets_config.json
```

#### 8. Mascota activa no aparece en juego

**Causa**: Lógica de spawn no implementada (parte visual)

**Estado**: Actualmente el sistema solo gestiona datos. La representación visual (spawn de entidad) requiere implementación adicional en PetManager.

### Logs Útiles

```bash
# Ver todos los logs del plugin
grep "MMORPGPlugin" logs/latest.log

# Ver errores
grep "ERROR" logs/latest.log | grep -i pet

# Ver operaciones de mascotas
grep "Pet" logs/latest.log

# Monitorear en tiempo real
tail -f logs/latest.log | grep -i pet
```

### Validación de Datos

```python
# Script Python para validar estructura JSON
import json
import os

def validate_player_data(uuid):
    path = f'plugins/MMORPGPlugin/pets/{uuid}.json'
    
    if not os.path.exists(path):
        print(f"❌ Archivo no existe: {path}")
        return False
    
    with open(path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    required_keys = ['playerUuid', 'ownedPets', 'unlockedMounts', 'activePetId', 'activeMountId']
    
    for key in required_keys:
        if key not in data:
            print(f"❌ Falta clave: {key}")
            return False
    
    print(f"✅ Estructura válida para {uuid}")
    return True

# Ejecutar
validate_player_data('tu-uuid-aqui')
```

---

## 📚 Referencias

- **Código Java**: `mmorpg-plugin/src/main/java/com/nightslayer/mmorpg/`
  - `PetManager.java` - Lógica central
  - `commands/PetCommand.java` - Comandos
  - `models/PlayerPetData.java` - Modelo de datos

- **Código Python**: `web/app.py` (líneas ~5755-6310)
  - Endpoints de API REST
  - Integración con panel web

- **Configuración**: `config/pets_config.json`
  - 10 mascotas, 5 monturas, 21 habilidades

- **UI Web**: `web/templates/pets_panel.html` + `web/static/pets.js`

---

## 🚀 Próximos Pasos

### Funcionalidades Futuras

1. **Sistema Visual**: Spawn de entidades de mascotas que sigan al jugador
2. **Habilidades Activas**: Implementar cooldowns y efectos de habilidades
3. **Combate de Mascotas**: Mascotas atacan mobs automáticamente
4. **PvP con Mascotas**: Batallas entre mascotas de jugadores
5. **Breeding**: Cruzar mascotas para obtener nuevas
6. **Inventario de Mascotas**: Items especiales para mascotas
7. **Misiones de Mascotas**: Quests específicas con mascotas
8. **Leaderboard**: Ranking de mascotas más poderosas

### Optimizaciones

- [ ] Cache de archivos JSON para reducir I/O
- [ ] Validación de esquema JSON con JSON Schema
- [ ] Rate limiting en endpoints de API
- [ ] Compresión de datos históricos
- [ ] Backup automático de datos de mascotas

---

## 📄 Licencia

Este módulo es parte del proyecto MMORPG Plugin para Minecraft.

---

**Última actualización**: 2024
**Versión**: 1.0.0
**Estado**: ✅ Completado - Integración Java/Python funcional
