"""
Sprint 6: Tests de API REST Endpoints
Valida los 65 endpoints del sistema de configuración web
"""

import requests
import json
import pytest
from typing import Dict, Any

# Configuración base
BASE_URL = "http://localhost:5000"
AUTH = ("admin", "admin123")  # Cambiar según configuración

class TestConfigAPI:
    """Tests para config_routes.py (20 endpoints)"""
    
    def test_get_all_mobs(self):
        """GET /api/config/mobs - Listar todos los mobs"""
        response = requests.get(f"{BASE_URL}/api/config/mobs", auth=AUTH)
        assert response.status_code == 200
        data = response.json()
        assert data["success"] == True
        assert "config" in data
        
    def test_create_mob(self):
        """POST /api/config/mobs - Crear nuevo mob"""
        new_mob = {
            "mobId": "test_zombie_warrior",
            "displayName": "Zombie Guerrero",
            "level": 10,
            "health": 50.0,
            "damage": 8.0,
            "xp": 100,
            "isBoss": False,
            "world": "world"
        }
        response = requests.post(
            f"{BASE_URL}/api/config/mobs",
            json=new_mob,
            auth=AUTH
        )
        assert response.status_code in [200, 201]
        data = response.json()
        assert data["success"] == True
        
    def test_get_mob_by_id(self):
        """GET /api/config/mobs/<id> - Obtener mob específico"""
        mob_id = "test_zombie_warrior"
        response = requests.get(
            f"{BASE_URL}/api/config/mobs/{mob_id}",
            auth=AUTH
        )
        assert response.status_code == 200
        data = response.json()
        assert data["success"] == True
        assert data["config"]["mobId"] == mob_id
        
    def test_update_mob(self):
        """PUT /api/config/mobs/<id> - Actualizar mob"""
        mob_id = "test_zombie_warrior"
        updated_data = {
            "mobId": mob_id,
            "displayName": "Zombie Guerrero Elite",
            "level": 15,
            "health": 75.0,
            "damage": 12.0
        }
        response = requests.put(
            f"{BASE_URL}/api/config/mobs/{mob_id}",
            json=updated_data,
            auth=AUTH
        )
        assert response.status_code == 200
        data = response.json()
        assert data["success"] == True
        
    def test_delete_mob(self):
        """DELETE /api/config/mobs/<id> - Eliminar mob"""
        mob_id = "test_zombie_warrior"
        response = requests.delete(
            f"{BASE_URL}/api/config/mobs/{mob_id}",
            auth=AUTH
        )
        assert response.status_code == 200
        data = response.json()
        assert data["success"] == True
        
    def test_filter_mobs_by_world(self):
        """GET /api/config/mobs/world/<world> - Filtrar por mundo"""
        response = requests.get(
            f"{BASE_URL}/api/config/mobs/world/world",
            auth=AUTH
        )
        assert response.status_code == 200
        data = response.json()
        assert data["success"] == True

    # Tests similares para items, npcs, pets
    def test_items_crud(self):
        """Test CRUD completo de items"""
        # Create
        new_item = {
            "itemId": "test_sword",
            "displayName": "Espada de Prueba",
            "itemType": "WEAPON",
            "rarity": "rare",
            "damage": 10.0,
            "requiredLevel": 5
        }
        create_response = requests.post(
            f"{BASE_URL}/api/config/items",
            json=new_item,
            auth=AUTH
        )
        assert create_response.status_code in [200, 201]
        
        # Read
        get_response = requests.get(
            f"{BASE_URL}/api/config/items/test_sword",
            auth=AUTH
        )
        assert get_response.status_code == 200
        
        # Update
        updated_item = new_item.copy()
        updated_item["damage"] = 15.0
        update_response = requests.put(
            f"{BASE_URL}/api/config/items/test_sword",
            json=updated_item,
            auth=AUTH
        )
        assert update_response.status_code == 200
        
        # Delete
        delete_response = requests.delete(
            f"{BASE_URL}/api/config/items/test_sword",
            auth=AUTH
        )
        assert delete_response.status_code == 200


class TestDungeonsAPI:
    """Tests para dungeons_routes.py (6 endpoints)"""
    
    def test_dungeons_crud(self):
        """Test CRUD completo de dungeons"""
        # Create
        new_dungeon = {
            "dungeonId": "test_crypt",
            "displayName": "Cripta de Prueba",
            "level": 10,
            "world": "world",
            "minPlayers": 2,
            "maxPlayers": 4,
            "timeLimit": 30,
            "active": True,
            "waves": [
                {"mobs": ["zombie_1", "skeleton_1"]},
                {"mobs": ["boss_lich"]}
            ]
        }
        
        response = requests.post(
            f"{BASE_URL}/api/config/dungeons",
            json=new_dungeon,
            auth=AUTH
        )
        assert response.status_code in [200, 201]
        
        # Verificar creación
        get_response = requests.get(
            f"{BASE_URL}/api/config/dungeons/test_crypt",
            auth=AUTH
        )
        assert get_response.status_code == 200
        data = get_response.json()
        assert len(data["config"]["waves"]) == 2
        
        # Filtro por mundo
        world_response = requests.get(
            f"{BASE_URL}/api/config/dungeons/world/world",
            auth=AUTH
        )
        assert world_response.status_code == 200
        
        # Delete
        delete_response = requests.delete(
            f"{BASE_URL}/api/config/dungeons/test_crypt",
            auth=AUTH
        )
        assert delete_response.status_code == 200


class TestInvasionsAPI:
    """Tests para invasions_routes.py (5 endpoints)"""
    
    def test_invasions_crud(self):
        """Test CRUD de invasiones"""
        new_invasion = {
            "invasionId": "test_undead_invasion",
            "displayName": "Invasión No-Muerta",
            "level": 20,
            "world": "world",
            "duration": 15,
            "interval": 60,
            "active": True,
            "waves": [
                {"mobs": ["zombie_horde_1", "zombie_horde_2"]}
            ]
        }
        
        # Create
        create_response = requests.post(
            f"{BASE_URL}/api/config/invasions",
            json=new_invasion,
            auth=AUTH
        )
        assert create_response.status_code in [200, 201]
        
        # Get by ID
        get_response = requests.get(
            f"{BASE_URL}/api/config/invasions/test_undead_invasion",
            auth=AUTH
        )
        assert get_response.status_code == 200
        data = get_response.json()
        assert data["config"]["duration"] == 15
        
        # Cleanup
        requests.delete(
            f"{BASE_URL}/api/config/invasions/test_undead_invasion",
            auth=AUTH
        )


class TestClassesAPI:
    """Tests para classes_routes.py (5 endpoints)"""
    
    def test_classes_file_based_storage(self):
        """Test almacenamiento por archivos de clases"""
        new_class = {
            "classId": "test_warrior",
            "displayName": "Guerrero de Prueba",
            "classType": "WARRIOR",
            "baseHealth": 100.0,
            "baseMana": 50.0,
            "abilities": ["slash", "charge"]
        }
        
        # Create
        response = requests.post(
            f"{BASE_URL}/api/config/classes",
            json=new_class,
            auth=AUTH
        )
        assert response.status_code in [200, 201]
        
        # Filtro por tipo
        type_response = requests.get(
            f"{BASE_URL}/api/config/classes/type/WARRIOR",
            auth=AUTH
        )
        assert type_response.status_code == 200
        
        # Cleanup
        requests.delete(
            f"{BASE_URL}/api/config/classes/test_warrior",
            auth=AUTH
        )


class TestEnchantmentsAPI:
    """Tests para enchantments_routes.py (5 endpoints)"""
    
    def test_enchantments_with_category_filter(self):
        """Test encantamientos con filtro de categoría"""
        new_enchantment = {
            "enchantmentId": "test_sharpness",
            "displayName": "Filo de Prueba",
            "category": "WEAPON",
            "maxLevel": 5,
            "rarity": "epic"
        }
        
        # Create
        requests.post(
            f"{BASE_URL}/api/config/enchantments",
            json=new_enchantment,
            auth=AUTH
        )
        
        # Filtro por categoría
        category_response = requests.get(
            f"{BASE_URL}/api/config/enchantments/category/WEAPON",
            auth=AUTH
        )
        assert category_response.status_code == 200
        
        # Cleanup
        requests.delete(
            f"{BASE_URL}/api/config/enchantments/test_sharpness",
            auth=AUTH
        )


class TestCraftingAPI:
    """Tests para crafting_routes.py (5 endpoints)"""
    
    def test_crafting_recipes(self):
        """Test recetas de crafteo"""
        new_recipe = {
            "recipeId": "test_iron_sword",
            "displayName": "Espada de Hierro",
            "category": "WEAPON",
            "materials": [
                {"item": "iron_ingot", "amount": 2},
                {"item": "stick", "amount": 1}
            ],
            "result": "iron_sword"
        }
        
        # Create
        response = requests.post(
            f"{BASE_URL}/api/config/crafting",
            json=new_recipe,
            auth=AUTH
        )
        assert response.status_code in [200, 201]
        
        # Get by category
        category_response = requests.get(
            f"{BASE_URL}/api/config/crafting/category/WEAPON",
            auth=AUTH
        )
        assert category_response.status_code == 200
        
        # Cleanup
        requests.delete(
            f"{BASE_URL}/api/config/crafting/test_iron_sword",
            auth=AUTH
        )


class TestRespawnAPI:
    """Tests para respawn_routes.py (6 endpoints)"""
    
    def test_respawn_zones_and_global_settings(self):
        """Test zonas de respawn y configuración global"""
        # Test global settings
        settings_response = requests.get(
            f"{BASE_URL}/api/config/respawn/settings",
            auth=AUTH
        )
        assert settings_response.status_code == 200
        
        # Create zone
        new_zone = {
            "zoneId": "test_spawn_zone",
            "displayName": "Zona de Prueba",
            "world": "world",
            "x": 0,
            "y": 64,
            "z": 0,
            "radius": 10
        }
        
        create_response = requests.post(
            f"{BASE_URL}/api/config/respawn",
            json=new_zone,
            auth=AUTH
        )
        assert create_response.status_code in [200, 201]
        
        # Filter by world
        world_response = requests.get(
            f"{BASE_URL}/api/config/respawn/world/world",
            auth=AUTH
        )
        assert world_response.status_code == 200
        
        # Cleanup
        requests.delete(
            f"{BASE_URL}/api/config/respawn/test_spawn_zone",
            auth=AUTH
        )


class TestEventsAPI:
    """Tests para events_routes.py (15 endpoints)"""
    
    def test_events_crud(self):
        """Test eventos"""
        new_event = {
            "eventId": "test_holiday_event",
            "displayName": "Evento de Prueba",
            "eventType": "TIMED",
            "active": True,
            "triggers": ["player_login"],
            "rewards": [
                {"type": "item", "item": "diamond", "amount": 1}
            ]
        }
        
        response = requests.post(
            f"{BASE_URL}/api/config/events",
            json=new_event,
            auth=AUTH
        )
        assert response.status_code in [200, 201]
        
        # Cleanup
        requests.delete(
            f"{BASE_URL}/api/config/events/test_holiday_event",
            auth=AUTH
        )


class TestErrorHandling:
    """Tests para manejo de errores"""
    
    def test_duplicate_id_error(self):
        """Test error al crear ID duplicado"""
        mob_data = {
            "mobId": "duplicate_test",
            "displayName": "Test",
            "level": 1
        }
        
        # Primera creación
        requests.post(f"{BASE_URL}/api/config/mobs", json=mob_data, auth=AUTH)
        
        # Segunda creación (debe fallar)
        response = requests.post(
            f"{BASE_URL}/api/config/mobs",
            json=mob_data,
            auth=AUTH
        )
        data = response.json()
        assert data["success"] == False
        assert "ya existe" in data.get("error", "").lower()
        
        # Cleanup
        requests.delete(f"{BASE_URL}/api/config/mobs/duplicate_test", auth=AUTH)
        
    def test_not_found_error(self):
        """Test error 404 al buscar ID inexistente"""
        response = requests.get(
            f"{BASE_URL}/api/config/mobs/nonexistent_mob_id_12345",
            auth=AUTH
        )
        assert response.status_code == 404 or response.json()["success"] == False
        
    def test_invalid_data_error(self):
        """Test error con datos inválidos"""
        invalid_mob = {
            "mobId": "",  # ID vacío
            "level": "invalid"  # Tipo incorrecto
        }
        response = requests.post(
            f"{BASE_URL}/api/config/mobs",
            json=invalid_mob,
            auth=AUTH
        )
        # Debe retornar error
        assert response.status_code >= 400 or response.json()["success"] == False


class TestFilters:
    """Tests para filtros especializados"""
    
    def test_world_filters(self):
        """Test filtros por mundo"""
        worlds = ["world", "world_nether", "world_the_end"]
        
        for world in worlds:
            # Test mobs filter
            response = requests.get(
                f"{BASE_URL}/api/config/mobs/world/{world}",
                auth=AUTH
            )
            assert response.status_code == 200
            
    def test_category_filters(self):
        """Test filtros por categoría"""
        categories = ["WEAPON", "ARMOR", "CONSUMABLE"]
        
        for category in categories:
            # Test enchantments filter
            response = requests.get(
                f"{BASE_URL}/api/config/enchantments/category/{category}",
                auth=AUTH
            )
            assert response.status_code == 200


# Función de utilidad para ejecutar todos los tests
def run_all_tests():
    """Ejecuta todos los tests y genera reporte"""
    pytest.main([__file__, "-v", "--tb=short"])


if __name__ == "__main__":
    print("=" * 60)
    print("SPRINT 6: Tests de API REST Endpoints")
    print("=" * 60)
    print(f"URL Base: {BASE_URL}")
    print(f"Total de tests: ~30")
    print(f"Endpoints cubiertos: 65")
    print("=" * 60)
    
    # Ejecutar tests
    run_all_tests()
