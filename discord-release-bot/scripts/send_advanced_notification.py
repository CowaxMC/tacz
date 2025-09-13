#!/usr/bin/env python3
"""
Продвинутый скрипт для отправки уведомлений о релизах в Discord
"""

import os
import json
import requests
from datetime import datetime

def load_release_data():
    """Загрузка данных о релизе"""
    try:
        with open('release_data.json', 'r', encoding='utf-8') as f:
            return json.load(f)
    except FileNotFoundError:
        print("Файл release_data.json не найден")
        return {}

def get_github_event_data():
    """Получение данных из GitHub события"""
    github_event_path = os.getenv('GITHUB_EVENT_PATH')
    
    if not github_event_path:
        return {}
    
    try:
        with open(github_event_path, 'r', encoding='utf-8') as f:
            return json.load(f)
    except Exception as e:
        print(f"Ошибка при чтении GitHub события: {e}")
        return {}

def create_advanced_embed(event_data, release_data):
    """Создание продвинутого Discord embed"""
    release = event_data.get('release', {})
    repository = event_data.get('repository', {})
    action = event_data.get('action', 'published')
    
    # Определение цвета и эмодзи
    color_map = {
        'published': 0x00ff00,
        'edited': 0xffff00,
        'prereleased': 0xff8800,
        'released': 0x00ff00,
    }
    color = color_map.get(action, 0x0099ff)
    
    emoji_map = {
        'published': '🚀',
        'edited': '✏️',
        'prereleased': '🔶',
        'released': '🎉',
    }
    emoji = emoji_map.get(action, '📦')
    
    # Создание основного embed
    embed = {
        "title": f"{emoji} {action.title()} Release: {release.get('name', 'Unnamed Release')}",
        "description": release.get('body', 'Описание отсутствует'),
        "color": color,
        "url": release.get('html_url', ''),
        "timestamp": release.get('published_at', release.get('created_at', datetime.now().isoformat())),
        "footer": {
            "text": f"Repository: {repository.get('full_name', 'Unknown')}",
            "icon_url": "https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png"
        },
        "author": {
            "name": release.get('author', {}).get('login', 'Unknown'),
            "url": release.get('author', {}).get('html_url', ''),
            "icon_url": release.get('author', {}).get('avatar_url', '')
        },
        "fields": []
    }
    
    # Добавление полей
    fields = []
    
    # Основная информация
    if release.get('tag_name'):
        fields.append({
            "name": "🏷️ Tag",
            "value": release['tag_name'],
            "inline": True
        })
    
    if release.get('author', {}).get('login'):
        fields.append({
            "name": "👤 Author",
            "value": release['author']['login'],
            "inline": True
        })
    
    if release.get('created_at'):
        created_date = release['created_at'][:10]
        fields.append({
            "name": "📅 Created",
            "value": created_date,
            "inline": True
        })
    
    # Статус релиза
    status_parts = []
    if release.get('prerelease', False):
        status_parts.append("🔶 Pre-release")
    if release.get('draft', False):
        status_parts.append("📝 Draft")
    
    if status_parts:
        fields.append({
            "name": "⚠️ Status",
            "value": " | ".join(status_parts),
            "inline": True
        })
    
    # Статистика коммитов
    commit_stats = release_data.get('commit_stats', {})
    if commit_stats.get('total_commits'):
        fields.append({
            "name": "📊 Commits",
            "value": str(commit_stats['total_commits']),
            "inline": True
        })
    
    # Информация о репозитории
    repo_info = release_data.get('repository_info', {})
    if repo_info.get('stars'):
        fields.append({
            "name": "⭐ Stars",
            "value": str(repo_info['stars']),
            "inline": True
        })
    
    if repo_info.get('language'):
        fields.append({
            "name": "💻 Language",
            "value": repo_info['language'],
            "inline": True
        })
    
    # Информация об ассетах
    assets_info = release_data.get('assets_info', {})
    if assets_info.get('total_assets'):
        asset_text = f"{assets_info['total_assets']} files"
        if assets_info.get('total_size_mb'):
            asset_text += f" ({assets_info['total_size_mb']} MB)"
        if assets_info.get('total_downloads'):
            asset_text += f" - {assets_info['total_downloads']} downloads"
        
        fields.append({
            "name": "📁 Assets",
            "value": asset_text,
            "inline": True
        })
    
    # Ссылки
    links = []
    if release.get('html_url'):
        links.append(f"[📦 Release]({release['html_url']})")
    
    if repository.get('html_url'):
        links.append(f"[🏠 Repository]({repository['html_url']})")
    
    if links:
        fields.append({
            "name": "🔗 Links",
            "value": " | ".join(links),
            "inline": False
        })
    
    # Кастомное сообщение
    custom_message = os.getenv('CUSTOM_MESSAGE', '')
    if custom_message:
        fields.append({
            "name": "💬 Custom Message",
            "value": custom_message,
            "inline": False
        })
    
    embed["fields"] = fields
    
    return embed

def send_discord_notification(embed):
    """Отправка уведомления в Discord"""
    webhook_url = os.getenv('DISCORD_WEBHOOK_URL')
    
    if not webhook_url:
        print("DISCORD_WEBHOOK_URL не установлен")
        return False
    
    payload = {
        "embeds": [embed],
        "username": "Advanced GitHub Release Bot",
        "avatar_url": "https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png"
    }
    
    try:
        response = requests.post(webhook_url, json=payload)
        response.raise_for_status()
        print("✅ Продвинутое уведомление успешно отправлено в Discord")
        return True
    except requests.exceptions.RequestException as e:
        print(f"❌ Ошибка при отправке уведомления: {e}")
        return False

def main():
    """Основная функция"""
    print("Отправка продвинутого уведомления о релизе в Discord...")
    
    # Загрузка данных
    event_data = get_github_event_data()
    release_data = load_release_data()
    
    if not event_data:
        print("Не удалось получить данные о событии GitHub")
        return
    
    # Создание embed
    embed = create_advanced_embed(event_data, release_data)
    
    # Отправка уведомления
    success = send_discord_notification(embed)
    
    if success:
        print("✅ Продвинутое уведомление отправлено успешно")
    else:
        print("❌ Ошибка при отправке продвинутого уведомления")
        exit(1)

if __name__ == "__main__":
    main()
