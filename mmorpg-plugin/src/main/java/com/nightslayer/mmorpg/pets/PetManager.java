package com.nightslayer.mmorpg.pets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nightslayer.mmorpg.MMORPGPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class PetManager {
    private final MMORPGPlugin plugin;
    private final Gson gson;
    private final File configFile;
    private final File dataFolder;

    private Map<String, Pet> pets;
    private Map<String, Mount> mounts;
    private Map<String, PetAbility> abilities;
    private Map<String, PlayerPetData> playerData;
    private Map<String, Object> settings;

    public PetManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        // Usar directorio del plugin, no subir niveles
        this.configFile = new File(plugin.getDataFolder(), "pets_config.json");
        this.dataFolder = new File(plugin.getDataFolder(), "pets");

        this.pets = new HashMap<>();
        this.mounts = new HashMap<>();
        this.abilities = new HashMap<>();
        this.playerData = new HashMap<>();
        this.settings = new HashMap<>();

        // Crear directorios si no existen
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // Crear archivo de configuración por defecto si no existe
        if (!configFile.exists()) {
            plugin.getLogger().info("Creando archivo de configuración por defecto: pets_config.json");
            createDefaultConfig();
        }

        loadConfig();
    }

    private void createDefaultConfig() {
        try {
            JsonObject root = new JsonObject();
            
            // Crear estructura de mascotas por defecto
            JsonArray petsArray = new JsonArray();
            String[] petNames = {"Perro Común", "Gato Místico", "Lobo Plateado", "Fénix de Fuego", "Dragón Legendario",
                                "Unicornio Celestial", "Grifo Guardián", "Kitsune Oculta", "Titán Antiguo", "Ser Divino"};
            String[] petIds = {"pet_dog_common", "pet_cat_mystic", "pet_wolf_silver", "pet_phoenix_fire", "pet_dragon_legendary",
                              "pet_unicorn_celestial", "pet_griffin_guardian", "pet_kitsune_hidden", "pet_titan_ancient", "pet_being_divine"};
            
            for (int i = 0; i < petNames.length; i++) {
                JsonObject pet = new JsonObject();
                pet.addProperty("id", petIds[i]);
                pet.addProperty("name", petNames[i]);
                pet.addProperty("rarity", i < 2 ? "COMMON" : i < 5 ? "UNCOMMON" : i < 8 ? "RARE" : "LEGENDARY");
                pet.addProperty("base_health", 50 + (i * 15));
                pet.addProperty("base_damage", 5 + (i * 2));
                pet.addProperty("base_speed", 0.5 + (i * 0.1));
                petsArray.add(pet);
            }
            root.add("pets", petsArray);
            
            // Crear estructura de monturas por defecto
            JsonArray mountsArray = new JsonArray();
            String[] mountNames = {"Caballo Común", "Caballo Rápido", "Caballo Legendario", "Wyvern Voladora", "Dragón Volador"};
            String[] mountIds = {"mount_horse_common", "mount_horse_fast", "mount_horse_legendary", "mount_wyvern_flying", "mount_dragon_flying"};
            float[] mountSpeeds = {1.0f, 1.5f, 2.0f, 2.5f, 3.0f};
            
            for (int i = 0; i < mountNames.length; i++) {
                JsonObject mount = new JsonObject();
                mount.addProperty("id", mountIds[i]);
                mount.addProperty("name", mountNames[i]);
                mount.addProperty("speed_multiplier", mountSpeeds[i]);
                mount.addProperty("required_level", i * 10);
                mountsArray.add(mount);
            }
            root.add("mounts", mountsArray);
            
            // Escribir archivo
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(root, writer);
            }
            plugin.getLogger().info("Archivo pets_config.json creado con configuración por defecto");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al crear archivo por defecto", e);
        }
    }

    private void loadConfig() {
        if (!configFile.exists()) {
            plugin.getLogger().warning("Archivo pets_config.json no encontrado!");
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);

            if (root == null) {
                plugin.getLogger().warning("pets_config.json vacío o inválido, creando configuración por defecto");
                createDefaultConfig();
                return;
            }

            // Cargar mascotas
            if (root.has("pets")) {
                JsonArray petsArray = root.getAsJsonArray("pets");
                for (JsonElement element : petsArray) {
                    Pet pet = parsePet(element.getAsJsonObject());
                    if (pet != null) {
                        pets.put(pet.getId(), pet);
                    }
                }
            }

            // Cargar monturas
            if (root.has("mounts")) {
                JsonArray mountsArray = root.getAsJsonArray("mounts");
                for (JsonElement element : mountsArray) {
                    Mount mount = parseMount(element.getAsJsonObject());
                    if (mount != null) {
                        mounts.put(mount.getId(), mount);
                    }
                }
            }

            // Cargar habilidades
            if (root.has("abilities")) {
                JsonArray abilitiesArray = root.getAsJsonArray("abilities");
                for (JsonElement element : abilitiesArray) {
                    PetAbility ability = parseAbility(element.getAsJsonObject());
                    if (ability != null) {
                        abilities.put(ability.getId(), ability);
                    }
                }
            }

            // Cargar configuraciones
            if (root.has("pet_settings")) {
                JsonObject settingsObj = root.getAsJsonObject("pet_settings");
                for (Map.Entry<String, JsonElement> entry : settingsObj.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        if (entry.getValue().getAsJsonPrimitive().isNumber()) {
                            settings.put(entry.getKey(), entry.getValue().getAsNumber());
                        } else if (entry.getValue().getAsJsonPrimitive().isBoolean()) {
                            settings.put(entry.getKey(), entry.getValue().getAsBoolean());
                        } else {
                            settings.put(entry.getKey(), entry.getValue().getAsString());
                        }
                    }
                }
            }

            plugin.getLogger().info("Cargadas " + pets.size() + " mascotas, " + mounts.size() + " monturas, " + abilities.size() + " habilidades");

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al cargar pets_config.json", e);
        }
    }

    private Pet parsePet(JsonObject obj) {
        // Validar campos requeridos
        if (obj.get("id") == null || obj.get("name") == null || obj.get("type") == null) {
            plugin.getLogger().warning("Pet con campos faltantes (id, name o type), saltando...");
            return null;
        }
        
        String id = obj.get("id").getAsString();
        String name = obj.get("name").getAsString();
        PetType type;
        try {
            type = PetType.valueOf(obj.get("type").getAsString());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Tipo de pet inválido para " + id + ": " + obj.get("type").getAsString());
            return null;
        }
        String rarity = obj.get("rarity") != null ? obj.get("rarity").getAsString() : "COMMON";
        String description = obj.get("description") != null ? obj.get("description").getAsString() : "";

        // Parse base stats
        Map<String, Double> baseStats = new HashMap<>();
        if (obj.has("base_stats") && obj.get("base_stats").isJsonObject()) {
            JsonObject statsObj = obj.getAsJsonObject("base_stats");
            for (Map.Entry<String, JsonElement> entry : statsObj.entrySet()) {
                baseStats.put(entry.getKey(), entry.getValue().getAsDouble());
            }
        }

        // Parse evolution levels
        List<Pet.EvolutionLevel> evolutionLevels = new ArrayList<>();
        if (obj.has("evolution_levels") && obj.get("evolution_levels").isJsonArray()) {
            JsonArray evolutionArray = obj.getAsJsonArray("evolution_levels");
            for (JsonElement element : evolutionArray) {
                JsonObject evolObj = element.getAsJsonObject();
                if (evolObj.get("level") == null || evolObj.get("name") == null) continue;
                
                int level = evolObj.get("level").getAsInt();
                String levelName = evolObj.get("name").getAsString();
                int requiredXp = evolObj.has("required_xp") ? evolObj.get("required_xp").getAsInt() : 100;
                double statsMultiplier = evolObj.has("stats_multiplier") ? evolObj.get("stats_multiplier").getAsDouble() : 1.0;
                
                List<String> abilityIds = new ArrayList<>();
                if (evolObj.has("abilities") && evolObj.get("abilities").isJsonArray()) {
                    JsonArray abilitiesArray = evolObj.getAsJsonArray("abilities");
                    for (JsonElement abilityElement : abilitiesArray) {
                        abilityIds.add(abilityElement.getAsString());
                    }
                }

                evolutionLevels.add(new Pet.EvolutionLevel(level, levelName, requiredXp, statsMultiplier, abilityIds));
            }
        }

        // Parse food preferences
        List<String> foodPreferences = new ArrayList<>();
        if (obj.has("food_preferences") && obj.get("food_preferences").isJsonArray()) {
            JsonArray foodArray = obj.getAsJsonArray("food_preferences");
            for (JsonElement element : foodArray) {
                foodPreferences.add(element.getAsString());
            }
        }

        int adoptionCost = obj.has("adoption_cost") ? obj.get("adoption_cost").getAsInt() : 100;
        double feedRestoreHealth = obj.has("feed_restore_health") ? obj.get("feed_restore_health").getAsDouble() : 5.0;

        return new Pet(id, name, type, rarity, description, baseStats, evolutionLevels, foodPreferences, adoptionCost, feedRestoreHealth);
    }

    private Mount parseMount(JsonObject obj) {
        if (obj.get("id") == null || obj.get("name") == null) {
            plugin.getLogger().warning("Mount con campos faltantes (id o name), saltando...");
            return null;
        }
        
        String id = obj.get("id").getAsString();
        String name = obj.get("name").getAsString();
        String rarity = obj.has("rarity") ? obj.get("rarity").getAsString() : "COMMON";
        String description = obj.has("description") ? obj.get("description").getAsString() : "";
        double speed = obj.has("speed") ? obj.get("speed").getAsDouble() : 1.0;
        double jumpStrength = obj.has("jump_strength") ? obj.get("jump_strength").getAsDouble() : 1.0;
        double health = obj.has("health") ? obj.get("health").getAsDouble() : 20.0;
        String specialAbility = obj.has("special_ability") && !obj.get("special_ability").isJsonNull() 
                ? obj.get("special_ability").getAsString() : null;
        int unlockCost = obj.has("unlock_cost") ? obj.get("unlock_cost").getAsInt() : 500;
        int unlockLevel = obj.has("unlock_level") ? obj.get("unlock_level").getAsInt() : 1;

        return new Mount(id, name, rarity, description, speed, jumpStrength, health, specialAbility, unlockCost, unlockLevel);
    }

    private PetAbility parseAbility(JsonObject obj) {
        if (obj.get("id") == null || obj.get("name") == null) {
            plugin.getLogger().warning("PetAbility con campos faltantes (id o name), saltando...");
            return null;
        }
        String id = obj.get("id").getAsString();
        String name = obj.get("name").getAsString();
        String description = obj.get("description").getAsString();
        int cooldown = obj.has("cooldown") ? obj.get("cooldown").getAsInt() : 0;
        boolean passive = obj.has("passive") && obj.get("passive").getAsBoolean();

        Map<String, Object> properties = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            if (key.equals("id") || key.equals("name") || key.equals("description") || 
                key.equals("cooldown") || key.equals("passive")) {
                continue;
            }

            JsonElement value = entry.getValue();
            if (value.isJsonPrimitive()) {
                if (value.getAsJsonPrimitive().isNumber()) {
                    properties.put(key, value.getAsNumber().doubleValue());
                } else if (value.getAsJsonPrimitive().isBoolean()) {
                    properties.put(key, value.getAsBoolean());
                } else {
                    properties.put(key, value.getAsString());
                }
            }
        }

        return new PetAbility(id, name, description, cooldown, passive, properties);
    }

    public void loadPlayerData(String playerUuid) {
        File playerFile = new File(dataFolder, playerUuid + ".json");
        if (!playerFile.exists()) {
            playerData.put(playerUuid, new PlayerPetData(playerUuid));
            return;
        }

        try (FileReader reader = new FileReader(playerFile)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            PlayerPetData data = new PlayerPetData(playerUuid);

            // Load owned pets
            if (root.has("owned_pets")) {
                JsonArray petsArray = root.getAsJsonArray("owned_pets");
                for (JsonElement element : petsArray) {
                    JsonObject petObj = element.getAsJsonObject();
                    String petId = petObj.get("pet_id").getAsString();
                    PlayerPetData.OwnedPet ownedPet = new PlayerPetData.OwnedPet(petId);

                    if (petObj.has("custom_name") && !petObj.get("custom_name").isJsonNull()) {
                        ownedPet.setCustomName(petObj.get("custom_name").getAsString());
                    }
                    ownedPet.setLevel(petObj.get("level").getAsInt());
                    ownedPet.setExperience(petObj.get("experience").getAsInt());
                    ownedPet.setCurrentHealth(petObj.get("current_health").getAsDouble());
                    ownedPet.setHungerLevel(petObj.get("hunger_level").getAsDouble());
                    ownedPet.setLastFedTimestamp(petObj.get("last_fed_timestamp").getAsLong());

                    data.getOwnedPets().add(ownedPet);
                }
            }

            // Load unlocked mounts
            if (root.has("unlocked_mounts")) {
                JsonArray mountsArray = root.getAsJsonArray("unlocked_mounts");
                for (JsonElement element : mountsArray) {
                    data.getUnlockedMounts().add(element.getAsString());
                }
            }

            if (root.has("active_pet_id") && !root.get("active_pet_id").isJsonNull()) {
                data.setActivePetId(root.get("active_pet_id").getAsString());
            }

            if (root.has("active_mount_id") && !root.get("active_mount_id").isJsonNull()) {
                data.setActiveMountId(root.get("active_mount_id").getAsString());
            }

            playerData.put(playerUuid, data);

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al cargar datos de mascotas del jugador " + playerUuid, e);
            playerData.put(playerUuid, new PlayerPetData(playerUuid));
        }
    }

    public void savePlayerData(String playerUuid) {
        PlayerPetData data = playerData.get(playerUuid);
        if (data == null) return;

        File playerFile = new File(dataFolder, playerUuid + ".json");

        try (FileWriter writer = new FileWriter(playerFile)) {
            JsonObject root = new JsonObject();

            // Save owned pets
            JsonArray petsArray = new JsonArray();
            for (PlayerPetData.OwnedPet ownedPet : data.getOwnedPets()) {
                JsonObject petObj = new JsonObject();
                petObj.addProperty("pet_id", ownedPet.getPetId());
                if (ownedPet.getCustomName() != null) {
                    petObj.addProperty("custom_name", ownedPet.getCustomName());
                }
                petObj.addProperty("level", ownedPet.getLevel());
                petObj.addProperty("experience", ownedPet.getExperience());
                petObj.addProperty("current_health", ownedPet.getCurrentHealth());
                petObj.addProperty("hunger_level", ownedPet.getHungerLevel());
                petObj.addProperty("last_fed_timestamp", ownedPet.getLastFedTimestamp());
                petsArray.add(petObj);
            }
            root.add("owned_pets", petsArray);

            // Save unlocked mounts
            JsonArray mountsArray = new JsonArray();
            for (String mountId : data.getUnlockedMounts()) {
                mountsArray.add(mountId);
            }
            root.add("unlocked_mounts", mountsArray);

            if (data.getActivePetId() != null) {
                root.addProperty("active_pet_id", data.getActivePetId());
            }

            if (data.getActiveMountId() != null) {
                root.addProperty("active_mount_id", data.getActiveMountId());
            }

            gson.toJson(root, writer);

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al guardar datos de mascotas del jugador " + playerUuid, e);
        }
    }

    public void saveAllPlayerData() {
        for (String playerUuid : playerData.keySet()) {
            savePlayerData(playerUuid);
        }
    }

    public void shutdown() {
        saveAllPlayerData();
        playerData.clear();
        pets.clear();
        mounts.clear();
        abilities.clear();
    }

    // Getters
    public Pet getPet(String id) {
        return pets.get(id);
    }

    public Mount getMount(String id) {
        return mounts.get(id);
    }

    public PetAbility getAbility(String id) {
        return abilities.get(id);
    }

    public PlayerPetData getPlayerData(String playerUuid) {
        if (!playerData.containsKey(playerUuid)) {
            loadPlayerData(playerUuid);
        }
        return playerData.get(playerUuid);
    }

    public Collection<Pet> getAllPets() {
        return pets.values();
    }

    public Collection<Mount> getAllMounts() {
        return mounts.values();
    }

    public Collection<PetAbility> getAllAbilities() {
        return abilities.values();
    }

    public Object getSetting(String key) {
        return settings.get(key);
    }

    public int getMaxPetsPerPlayer() {
        Object value = settings.get("max_pets_per_player");
        return value instanceof Number ? ((Number) value).intValue() : 10;
    }

    public int getMaxActivePets() {
        Object value = settings.get("max_active_pets");
        return value instanceof Number ? ((Number) value).intValue() : 1;
    }
}
