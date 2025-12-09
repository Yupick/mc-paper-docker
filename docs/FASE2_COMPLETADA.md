# MMORPG Plugin - Fase 2 Completada ✅

## 📋 Sistemas Implementados

### 🛡️ Sistema de Clases

**Clases Disponibles:**
- **Guerrero (⚔)**: Maestro del combate cuerpo a cuerpo
  - Vida: 120 | Maná: 100 | Defensa: 10
  - Habilidades: Carga Brutal, Escudo Defensivo, Furia Berserker

- **Mago (✦)**: Manipulador de energía arcana
  - Vida: 80 | Maná: 200 | Defensa: 8
  - Habilidades: Bola de Fuego, Teletransporte, Lluvia de Meteoros

- **Arquero (➶)**: Experto en ataques a distancia
  - Vida: 90 | Maná: 120 | Defensa: 12
  - Habilidades: Disparo Múltiple, Trampa Explosiva, Lluvia de Flechas

**Características:**
- Sistema de niveles y experiencia
- Estadísticas escalables por nivel (10% por nivel)
- Regeneración automática de maná (5% por segundo)
- Habilidades con cooldown y costo de maná
- Persistencia de datos de jugadores

**Comandos:**
```
/class list              - Ver todas las clases disponibles
/class choose <clase>    - Elegir tu clase (guerrero/mago/arquero)
/class info [clase]      - Ver información de una clase
/class skills            - Ver tus habilidades disponibles
/class use <habilidad>   - Usar una habilidad específica
```

### 👥 Sistema de NPCs

**Tipos de NPCs:**
- **Dador de Misiones (§e)**: Ofrece quests y recompensas
- **Comerciante (§a)**: Compra y vende objetos
- **Entrenador (§6)**: Enseña habilidades y mejoras
- **Guardia (§c)**: Protege áreas y ayuda en combate
- **Aldeano (§7)**: NPC genérico con diálogos

**Características:**
- NPCs personalizados con nombres y tipos
- Sistema de diálogos con múltiples opciones
- Spawn/despawn automático
- Interacción mediante click derecho
- Invulnerables y con IA desactivada
- Asociación con quests

**Archivos Generados:**
- `plugins/MMORPGPlugin/npcs/npcs.json` - Configuración de NPCs

### 📜 Sistema de Quests

**Tipos de Objetivos:**
- **KILL**: Eliminar enemigos
- **COLLECT**: Recolectar items
- **TALK**: Hablar con NPCs
- **REACH**: Llegar a ubicaciones
- **USE**: Usar items
- **DELIVER**: Entregar items a NPCs

**Dificultades:**
- Fácil (§a) - Multiplicador 1.0x
- Normal (§e) - Multiplicador 1.5x
- Difícil (§6) - Multiplicador 2.0x
- Épica (§5) - Multiplicador 3.0x
- Legendaria (§c) - Multiplicador 5.0x

**Tipos de Recompensas:**
- Experiencia
- Dinero (preparado para economía)
- Items
- Puntos de habilidad de clase

**Características:**
- Quests repetibles con cooldown
- Múltiples objetivos por quest
- Tracking de progreso en tiempo real
- Notificaciones de completado
- Persistencia de progreso

**Comandos:**
```
/quest list              - Ver quests disponibles
/quest active            - Ver tus quests activas
/quest completed         - Ver quests completadas
/quest accept <id>       - Aceptar una quest
/quest progress [id]     - Ver progreso de quests
/quest complete <id>     - Reclamar recompensas
/quest info <id>         - Ver información de una quest
```

**Quests Por Defecto:**
1. **welcome_quest** - Bienvenido al Mundo RPG (Fácil, Nivel 1)
2. **hunt_zombies** - Cazador de No-Muertos (Normal, Nivel 3, Repetible)
3. **gather_resources** - Recolector Experto (Fácil, Nivel 2)
4. **dragon_slayer** - Asesino de Dragones (Épica, Nivel 15)

### 📊 Integración con Panel Web

**Archivos JSON Exportados:**
- `status.json` - Estado general del mundo RPG
- `players.json` - Jugadores online con sus datos
- `classes.json` - Información de todas las clases
- `quests.json` - Todas las quests disponibles
- `npcs.json` - NPCs spawneados y configuración

**Datos Sincronizados:**
- Jugadores online y sus estadísticas
- Clases activas en el mundo
- Quests activas y completadas
- NPCs spawneados
- Actualización automática cada 30 segundos

## 🏗️ Estructura de Archivos

```
mmorpg-plugin/
├── src/main/java/com/nightslayer/mmorpg/
│   ├── MMORPGPlugin.java          # Plugin principal con integración
│   ├── DataManager.java           # Exportación de datos al panel web
│   ├── WorldRPGManager.java       # Gestión de mundos RPG
│   ├── RPGCommand.java            # Comando /rpg
│   ├── WorldMetadata.java         # Metadata de mundos
│   │
│   ├── classes/
│   │   ├── ClassType.java         # Enum de clases (Guerrero, Mago, Arquero)
│   │   ├── ClassStats.java        # Estadísticas de clases
│   │   ├── ClassAbility.java      # Habilidades de clases
│   │   ├── PlayerClass.java       # Clase y progresión del jugador
│   │   └── ClassManager.java      # Gestor del sistema de clases
│   │
│   ├── npcs/
│   │   ├── NPCType.java           # Tipos de NPCs
│   │   ├── NPCDialogue.java       # Sistema de diálogos
│   │   ├── CustomNPC.java         # NPC personalizado
│   │   └── NPCManager.java        # Gestor de NPCs
│   │
│   ├── quests/
│   │   ├── QuestObjectiveType.java    # Tipos de objetivos
│   │   ├── QuestObjective.java        # Objetivo de quest
│   │   ├── QuestReward.java           # Recompensa de quest
│   │   ├── Quest.java                 # Quest completa
│   │   ├── PlayerQuestProgress.java   # Progreso del jugador
│   │   └── QuestManager.java          # Gestor de quests
│   │
│   └── commands/
│       ├── ClassCommand.java      # Comando /class
│       └── QuestCommand.java      # Comando /quest
│
└── src/main/resources/
    ├── plugin.yml                 # Configuración del plugin
    └── config.yml                 # Configuración RPG
```

## 🎮 Uso en el Juego

### Para Jugadores

1. **Elegir una Clase:**
   ```
   /class list           # Ver clases disponibles
   /class choose mago    # Elegir clase de Mago
   /class info           # Ver tu información
   ```

2. **Usar Habilidades:**
   ```
   /class skills                  # Ver habilidades disponibles
   /class use mage_fireball       # Lanzar bola de fuego
   ```

3. **Completar Quests:**
   ```
   /quest list                    # Ver quests disponibles
   /quest accept welcome_quest    # Aceptar quest
   /quest progress                # Ver progreso
   /quest complete welcome_quest  # Reclamar recompensas
   ```

4. **Interactuar con NPCs:**
   - Click derecho en un NPC para hablar
   - Seguir los diálogos y opciones
   - Aceptar quests de NPCs

### Para Administradores

**Instalación:**
```bash
# Compilar plugin
./scripts/build-mmorpg-plugin.sh

# Reiniciar servidor para aplicar cambios
docker-compose restart minecraft
```

**Configuración:**
- Editar `plugins/MMORPGPlugin/config.yml` para configurar features RPG
- Los datos se guardan automáticamente en `plugins/MMORPGPlugin/`
- Las clases de jugadores se guardan en `plugins/MMORPGPlugin/classes/`
- Las quests se guardan en `plugins/MMORPGPlugin/quests/`

## 📈 Mejoras Futuras (Fase 3+)

- ⚔️ Sistema de combate avanzado con combos
- 💰 Sistema de economía completo
- 🏪 Tiendas de NPCs funcionales
- 🎒 Sistema de inventario RPG
- 🏰 Mazmorras y raids
- 🎁 Loot tables personalizadas
- 📊 Leaderboards y rankings
- 🎨 Interfaz gráfica (GUI) para quests y clases

## ✅ Testing

**Comandos para probar:**
```bash
# En el servidor
/class list
/class choose guerrero
/class skills
/quest list
/quest accept welcome_quest
/rpg status
```

**Panel Web:**
- Acceder a la pestaña "RPG" en el dashboard
- Ver estadísticas en tiempo real
- Monitorear jugadores, clases y quests activas

## 🐛 Troubleshooting

**El plugin no carga:**
- Verificar que el servidor use Paper 1.21.1
- Comprobar logs en `logs/latest.log`

**Los comandos no funcionan:**
- Verificar permisos en `plugin.yml`
- Reiniciar el servidor después de cambios

**Panel web no muestra datos:**
- Verificar que existen archivos JSON en `plugins/MMORPGPlugin/data/`
- Reiniciar panel web: `./restart-web-panel.sh`
- Comprobar paths en `web/models/rpg_manager.py`

---

**Desarrollado por:** NightSlayer Team  
**Versión:** 1.0.0 (Fase 2)  
**Fecha:** Diciembre 2025
