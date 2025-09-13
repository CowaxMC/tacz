"""
Конфигурационный файл для Discord бота релизов
"""

import os
from dotenv import load_dotenv

# Загрузка переменных окружения
load_dotenv()

class Config:
    """Класс конфигурации"""
    
    # Discord настройки
    DISCORD_BOT_TOKEN = os.getenv('DISCORD_BOT_TOKEN')
    DISCORD_CHANNEL_ID = int(os.getenv('DISCORD_CHANNEL_ID', 0))
    DISCORD_WEBHOOK_URL = os.getenv('DISCORD_WEBHOOK_URL')
    
    # GitHub настройки
    GITHUB_WEBHOOK_SECRET = os.getenv('GITHUB_WEBHOOK_SECRET')
    
    # Flask настройки
    FLASK_HOST = os.getenv('FLASK_HOST', '0.0.0.0')
    FLASK_PORT = int(os.getenv('FLASK_PORT', 5000))
    FLASK_DEBUG = os.getenv('FLASK_DEBUG', 'False').lower() == 'true'
    
    # Настройки бота
    BOT_PREFIX = os.getenv('BOT_PREFIX', '!')
    
    @classmethod
    def validate(cls):
        """Проверка обязательных настроек"""
        required_settings = [
            ('DISCORD_BOT_TOKEN', cls.DISCORD_BOT_TOKEN),
            ('DISCORD_WEBHOOK_URL', cls.DISCORD_WEBHOOK_URL),
        ]
        
        missing = []
        for name, value in required_settings:
            if not value:
                missing.append(name)
        
        if missing:
            raise ValueError(f"Отсутствуют обязательные настройки: {', '.join(missing)}")
        
        return True
