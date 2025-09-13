#!/usr/bin/env python3
"""
Скрипт для отправки уведомлений о релизах в Discord через GitHub Actions
"""

import os
import json
import requests
from datetime import datetime

def get_release_data():
    """Получение данных о релизе из переменных окружения GitHub Actions"""
    # Данные из контекста GitHub Actions
    github_event_path = os.getenv('GITHUB_EVENT_PATH')
    
    if not github_event_path:
        print("GITHUB_EVENT_PATH не найден")
        return None
    
    try:
        with open(github_event_path, 'r', encoding='utf-8') as f:
            event_data = json.load(f)
        
        # Получение данных релиза
        release = event_data.get('release', {})
        repository = event_data.get('repository', {})
        
        return {
            'release': release,
            'repository': repository,
            'action': event_data.get('action', 'published')
        }
    except Exception as e:
        print(f"Ошибка при чтении данных события: {e}")
        return None

def create_discord_embed(release_data):
    """Создание Discord embed для релиза"""
    release = release_data['release']
    repository = release_data['repository']
    action = release_data['action']
    
    # Определение цвета в зависимости от действия
    color_map = {
        'published': 0x00ff00,  # Зеленый
        'edited': 0xffff00,     # Желтый
        'prereleased': 0xff8800, # Оранжевый
        'released': 0x00ff00,   # Зеленый
    }
    color = color_map.get(action, 0x0099ff)  # Синий по умолчанию
    
    # Определение эмодзи в зависимости от действия
    emoji_map = {
        'published': '🚀',
        'edited': '✏️',
        'prereleased': '🔶',
        'released': '🎉',
    }
    emoji = emoji_map.get(action, '📦')
    
    # Создание embed
    embed = {
        "title": f"{emoji} {action.title()} Release: {release.get('name', 'Unnamed Release')}",
        "description": release.get('body', 'Описание отсутствует'),
        "color": color,
        "url": release.get('html_url', ''),
        "timestamp": release.get('published_at', release.get('created_at', datetime.now().isoformat())),
        "footer": {
            "text": f"Repository: {repository.get('full_name', 'Unknown')}"
        },
        "author": {
            "name": release.get('author', {}).get('login', 'Unknown'),
            "url": release.get('author', {}).get('html_url', ''),
            "icon_url": release.get('author', {}).get('avatar_url', '')
        },
        "fields": []
    }
    
    # Добавление полей
    if release.get('tag_name'):
        embed["fields"].append({
            "name": "📦 Tag",
            "value": release['tag_name'],
            "inline": True
        })
    
    if release.get('author', {}).get('login'):
        embed["fields"].append({
            "name": "👤 Author",
            "value": release['author']['login'],
            "inline": True
        })
    
    if release.get('created_at'):
        created_date = release['created_at'][:10]
        embed["fields"].append({
            "name": "📅 Created",
            "value": created_date,
            "inline": True
        })
    
    # Статус релиза
    status_fields = []
    if release.get('prerelease', False):
        status_fields.append("Pre-release")
    if release.get('draft', False):
        status_fields.append("Draft")
    
    if status_fields:
        embed["fields"].append({
            "name": "⚠️ Status",
            "value": " | ".join(status_fields),
            "inline": True
        })
    
    # Добавление информации о ассетах
    assets = release.get('assets', [])
    if assets:
        asset_info = []
        for asset in assets[:5]:  # Ограничиваем до 5 ассетов
            size = asset.get('size', 0)
            size_mb = size / (1024 * 1024)
            asset_info.append(f"• [{asset['name']}]({asset['browser_download_url']}) ({size_mb:.1f} MB)")
        
        embed["fields"].append({
            "name": "📁 Downloads",
            "value": "\n".join(asset_info),
            "inline": False
        })
    
    return embed

def send_discord_notification(embed):
    """Отправка уведомления в Discord"""
    webhook_url = os.getenv('DISCORD_WEBHOOK_URL')
    
    if not webhook_url:
        print("DISCORD_WEBHOOK_URL не установлен")
        return False
    
    payload = {
        "embeds": [embed],
        "username": "GitHub Release Bot",
        "avatar_url": "https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png"
    }
    
    try:
        response = requests.post(webhook_url, json=payload)
        response.raise_for_status()
        print("Уведомление успешно отправлено в Discord")
        return True
    except requests.exceptions.RequestException as e:
        print(f"Ошибка при отправке уведомления: {e}")
        return False

def main():
    """Основная функция"""
    print("Отправка уведомления о релизе в Discord...")
    
    # Получение данных о релизе
    release_data = get_release_data()
    if not release_data:
        print("Не удалось получить данные о релизе")
        return
    
    # Создание Discord embed
    embed = create_discord_embed(release_data)
    
    # Отправка уведомления
    success = send_discord_notification(embed)
    
    if success:
        print("✅ Уведомление отправлено успешно")
    else:
        print("❌ Ошибка при отправке уведомления")
        exit(1)

if __name__ == "__main__":
    main()
