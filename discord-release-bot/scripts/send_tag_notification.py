#!/usr/bin/env python3
"""
Скрипт для отправки уведомлений о тегах в Discord через GitHub Actions
"""

import os
import json
import requests
from datetime import datetime

def get_tag_data():
    """Получение данных о теге из переменных окружения GitHub Actions"""
    # Получение данных из переменных окружения
    ref = os.getenv('GITHUB_REF', '')
    repository = os.getenv('GITHUB_REPOSITORY', '')
    actor = os.getenv('GITHUB_ACTOR', 'Unknown')
    
    if not ref.startswith('refs/tags/'):
        print("Это не тег")
        return None
    
    tag_name = ref.replace('refs/tags/', '')
    
    # Получение дополнительной информации о теге через GitHub API
    github_token = os.getenv('GITHUB_TOKEN')
    if github_token:
        try:
            headers = {
                'Authorization': f'token {github_token}',
                'Accept': 'application/vnd.github.v3+json'
            }
            
            # Получение информации о теге
            tag_url = f"https://api.github.com/repos/{repository}/git/refs/tags/{tag_name}"
            response = requests.get(tag_url, headers=headers)
            
            if response.status_code == 200:
                tag_info = response.json()
                # Получение информации о коммите
                commit_sha = tag_info['object']['sha']
                commit_url = f"https://api.github.com/repos/{repository}/git/commits/{commit_sha}"
                commit_response = requests.get(commit_url, headers=headers)
                
                if commit_response.status_code == 200:
                    commit_info = commit_response.json()
                    return {
                        'tag_name': tag_name,
                        'repository': repository,
                        'actor': actor,
                        'commit_message': commit_info.get('message', ''),
                        'commit_author': commit_info.get('author', {}).get('name', actor),
                        'commit_date': commit_info.get('author', {}).get('date', datetime.now().isoformat()),
                        'commit_url': f"https://github.com/{repository}/commit/{commit_sha}",
                        'tag_url': f"https://github.com/{repository}/releases/tag/{tag_name}"
                    }
        except Exception as e:
            print(f"Ошибка при получении информации о теге: {e}")
    
    # Fallback данные
    return {
        'tag_name': tag_name,
        'repository': repository,
        'actor': actor,
        'commit_message': f"Tag {tag_name} created",
        'commit_author': actor,
        'commit_date': datetime.now().isoformat(),
        'commit_url': f"https://github.com/{repository}/releases/tag/{tag_name}",
        'tag_url': f"https://github.com/{repository}/releases/tag/{tag_name}"
    }

def create_discord_embed(tag_data):
    """Создание Discord embed для тега"""
    embed = {
        "title": f"🏷️ New Tag: {tag_data['tag_name']}",
        "description": tag_data['commit_message'],
        "color": 0x0099ff,  # Синий цвет
        "url": tag_data['tag_url'],
        "timestamp": tag_data['commit_date'],
        "footer": {
            "text": f"Repository: {tag_data['repository']}"
        },
        "author": {
            "name": tag_data['commit_author'],
            "url": f"https://github.com/{tag_data['actor']}",
            "icon_url": f"https://github.com/{tag_data['actor']}.png"
        },
        "fields": [
            {
                "name": "🏷️ Tag Name",
                "value": tag_data['tag_name'],
                "inline": True
            },
            {
                "name": "👤 Created by",
                "value": tag_data['actor'],
                "inline": True
            },
            {
                "name": "📅 Created",
                "value": tag_data['commit_date'][:10],
                "inline": True
            },
            {
                "name": "🔗 Links",
                "value": f"[View Tag]({tag_data['tag_url']}) | [View Commit]({tag_data['commit_url']})",
                "inline": False
            }
        ]
    }
    
    return embed

def send_discord_notification(embed):
    """Отправка уведомления в Discord"""
    webhook_url = os.getenv('DISCORD_WEBHOOK_URL')
    
    if not webhook_url:
        print("DISCORD_WEBHOOK_URL не установлен")
        return False
    
    payload = {
        "embeds": [embed],
        "username": "GitHub Tag Bot",
        "avatar_url": "https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png"
    }
    
    try:
        response = requests.post(webhook_url, json=payload)
        response.raise_for_status()
        print("Уведомление о теге успешно отправлено в Discord")
        return True
    except requests.exceptions.RequestException as e:
        print(f"Ошибка при отправке уведомления: {e}")
        return False

def main():
    """Основная функция"""
    print("Отправка уведомления о теге в Discord...")
    
    # Получение данных о теге
    tag_data = get_tag_data()
    if not tag_data:
        print("Не удалось получить данные о теге")
        return
    
    # Создание Discord embed
    embed = create_discord_embed(tag_data)
    
    # Отправка уведомления
    success = send_discord_notification(embed)
    
    if success:
        print("✅ Уведомление о теге отправлено успешно")
    else:
        print("❌ Ошибка при отправке уведомления о теге")
        exit(1)

if __name__ == "__main__":
    main()
