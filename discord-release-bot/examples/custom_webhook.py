#!/usr/bin/env python3
"""
Пример кастомного webhook для отправки уведомлений о релизах
"""

import requests
import json
from datetime import datetime

class CustomDiscordWebhook:
    def __init__(self, webhook_url):
        self.webhook_url = webhook_url
    
    def send_release_notification(self, release_data):
        """Отправка кастомного уведомления о релизе"""
        
        # Создание кастомного embed
        embed = {
            "title": f"🎉 Новый релиз: {release_data.get('name', 'Unnamed')}",
            "description": self._format_description(release_data),
            "color": self._get_color_by_type(release_data),
            "url": release_data.get('html_url', ''),
            "timestamp": datetime.now().isoformat(),
            "footer": {
                "text": f"Репозиторий: {release_data.get('repository', {}).get('full_name', 'Unknown')}",
                "icon_url": "https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png"
            },
            "author": {
                "name": release_data.get('author', {}).get('login', 'Unknown'),
                "url": release_data.get('author', {}).get('html_url', ''),
                "icon_url": release_data.get('author', {}).get('avatar_url', '')
            },
            "fields": self._create_fields(release_data),
            "thumbnail": {
                "url": release_data.get('author', {}).get('avatar_url', '')
            }
        }
        
        # Отправка сообщения
        payload = {
            "embeds": [embed],
            "username": "Custom Release Bot",
            "avatar_url": "https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png"
        }
        
        try:
            response = requests.post(self.webhook_url, json=payload)
            response.raise_for_status()
            print("✅ Кастомное уведомление отправлено успешно")
            return True
        except requests.exceptions.RequestException as e:
            print(f"❌ Ошибка при отправке: {e}")
            return False
    
    def _format_description(self, release_data):
        """Форматирование описания релиза"""
        body = release_data.get('body', '')
        if not body:
            return "Описание отсутствует"
        
        # Ограничение длины описания
        if len(body) > 2000:
            body = body[:1997] + "..."
        
        return body
    
    def _get_color_by_type(self, release_data):
        """Получение цвета в зависимости от типа релиза"""
        if release_data.get('prerelease', False):
            return 0xff8800  # Оранжевый для pre-release
        elif release_data.get('draft', False):
            return 0x808080  # Серый для draft
        else:
            return 0x00ff00  # Зеленый для обычного релиза
    
    def _create_fields(self, release_data):
        """Создание полей для embed"""
        fields = []
        
        # Основная информация
        if release_data.get('tag_name'):
            fields.append({
                "name": "🏷️ Тег",
                "value": release_data['tag_name'],
                "inline": True
            })
        
        if release_data.get('author', {}).get('login'):
            fields.append({
                "name": "👤 Автор",
                "value": release_data['author']['login'],
                "inline": True
            })
        
        if release_data.get('created_at'):
            created_date = release_data['created_at'][:10]
            fields.append({
                "name": "📅 Дата",
                "value": created_date,
                "inline": True
            })
        
        # Статус релиза
        status_parts = []
        if release_data.get('prerelease', False):
            status_parts.append("🔶 Pre-release")
        if release_data.get('draft', False):
            status_parts.append("📝 Draft")
        
        if status_parts:
            fields.append({
                "name": "⚠️ Статус",
                "value": " | ".join(status_parts),
                "inline": True
            })
        
        # Статистика релиза
        stats = []
        if release_data.get('assets'):
            stats.append(f"📁 {len(release_data['assets'])} файлов")
        
        if stats:
            fields.append({
                "name": "📊 Статистика",
                "value": " | ".join(stats),
                "inline": True
            })
        
        # Ссылки
        links = []
        if release_data.get('html_url'):
            links.append(f"[📦 Релиз]({release_data['html_url']})")
        
        repository = release_data.get('repository', {})
        if repository.get('html_url'):
            links.append(f"[🏠 Репозиторий]({repository['html_url']})")
        
        if links:
            fields.append({
                "name": "🔗 Ссылки",
                "value": " | ".join(links),
                "inline": False
            })
        
        return fields

# Пример использования
if __name__ == "__main__":
    # Пример данных релиза
    sample_release = {
        "name": "v2.1.0",
        "tag_name": "v2.1.0",
        "body": "## Что нового\n- Добавлена новая функция\n- Исправлены баги\n- Улучшена производительность",
        "author": {
            "login": "developer",
            "html_url": "https://github.com/developer",
            "avatar_url": "https://github.com/developer.png"
        },
        "created_at": "2024-01-15T10:30:00Z",
        "html_url": "https://github.com/user/repo/releases/tag/v2.1.0",
        "prerelease": False,
        "draft": False,
        "assets": [
            {
                "name": "app-v2.1.0.zip",
                "size": 1024000,
                "browser_download_url": "https://github.com/user/repo/releases/download/v2.1.0/app-v2.1.0.zip"
            }
        ],
        "repository": {
            "full_name": "user/repo",
            "html_url": "https://github.com/user/repo"
        }
    }
    
    # Инициализация webhook
    webhook = CustomDiscordWebhook("YOUR_WEBHOOK_URL_HERE")
    
    # Отправка уведомления
    webhook.send_release_notification(sample_release)
