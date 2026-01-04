"""
Sprint 4: Routes Initialization
Registra todos los blueprints del API
"""
from .config_routes import config_bp
from .events_routes import events_bp
from .dungeons_routes import dungeons_bp
from .invasions_routes import invasions_bp
from .classes_routes import classes_bp
from .enchantments_routes import enchantments_bp
from .crafting_routes import crafting_bp
from .respawn_routes import respawn_bp

def init_routes(app):
    """Registra todos los blueprints en la aplicación Flask"""
    app.register_blueprint(config_bp)
    app.register_blueprint(events_bp)
    app.register_blueprint(dungeons_bp)
    app.register_blueprint(invasions_bp)
    app.register_blueprint(classes_bp)
    app.register_blueprint(enchantments_bp)
    app.register_blueprint(crafting_bp)
    app.register_blueprint(respawn_bp)
