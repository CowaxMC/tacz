#!/usr/bin/env python3
"""
Скрипт для получения дополнительных данных о релизе
"""

import os
import json
import requests
from datetime import datetime

def get_commit_stats():
    """Получение статистики коммитов"""
    github_token = os.getenv('GITHUB_TOKEN')
    repository = os.getenv('GITHUB_REPOSITORY')
    
    if not github_token or not repository:
        return {}
    
    headers = {
        'Authorization': f'token {github_token}',
        'Accept': 'application/vnd.github.v3+json'
    }
    
    try:
        # Получение статистики коммитов за последний релиз
        commits_url = f"https://api.github.com/repos/{repository}/commits"
        response = requests.get(commits_url, headers=headers, params={'per_page': 100})
        
        if response.status_code == 200:
            commits = response.json()
            return {
                'total_commits': len(commits),
                'authors': list(set(commit['commit']['author']['name'] for commit in commits[:10])),
                'last_commit_date': commits[0]['commit']['author']['date'] if commits else None
            }
    except Exception as e:
        print(f"Ошибка при получении статистики коммитов: {e}")
    
    return {}

def get_release_assets_info():
    """Получение информации об ассетах релиза"""
    github_event_path = os.getenv('GITHUB_EVENT_PATH')
    
    if not github_event_path:
        return {}
    
    try:
        with open(github_event_path, 'r', encoding='utf-8') as f:
            event_data = json.load(f)
        
        release = event_data.get('release', {})
        assets = release.get('assets', [])
        
        total_size = sum(asset.get('size', 0) for asset in assets)
        download_count = sum(asset.get('download_count', 0) for asset in assets)
        
        return {
            'total_assets': len(assets),
            'total_size_mb': round(total_size / (1024 * 1024), 2),
            'total_downloads': download_count,
            'assets': [
                {
                    'name': asset['name'],
                    'size_mb': round(asset.get('size', 0) / (1024 * 1024), 2),
                    'downloads': asset.get('download_count', 0)
                }
                for asset in assets
            ]
        }
    except Exception as e:
        print(f"Ошибка при получении информации об ассетах: {e}")
    
    return {}

def get_repository_info():
    """Получение информации о репозитории"""
    github_token = os.getenv('GITHUB_TOKEN')
    repository = os.getenv('GITHUB_REPOSITORY')
    
    if not github_token or not repository:
        return {}
    
    headers = {
        'Authorization': f'token {github_token}',
        'Accept': 'application/vnd.github.v3+json'
    }
    
    try:
        repo_url = f"https://api.github.com/repos/{repository}"
        response = requests.get(repo_url, headers=headers)
        
        if response.status_code == 200:
            repo_data = response.json()
            return {
                'stars': repo_data.get('stargazers_count', 0),
                'forks': repo_data.get('forks_count', 0),
                'watchers': repo_data.get('watchers_count', 0),
                'language': repo_data.get('language', 'Unknown'),
                'description': repo_data.get('description', ''),
                'topics': repo_data.get('topics', [])
            }
    except Exception as e:
        print(f"Ошибка при получении информации о репозитории: {e}")
    
    return {}

def main():
    """Основная функция"""
    print("Получение дополнительных данных о релизе...")
    
    # Сбор всех данных
    data = {
        'commit_stats': get_commit_stats(),
        'assets_info': get_release_assets_info(),
        'repository_info': get_repository_info(),
        'timestamp': datetime.now().isoformat()
    }
    
    # Сохранение данных в файл для использования в других шагах
    with open('release_data.json', 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    
    print("✅ Данные о релизе сохранены в release_data.json")
    
    # Вывод в GitHub Actions output
    print(f"::set-output name=commit_count::{data['commit_stats'].get('total_commits', 0)}")
    print(f"::set-output name=total_assets::{data['assets_info'].get('total_assets', 0)}")
    print(f"::set-output name=repository_stars::{data['repository_info'].get('stars', 0)}")

if __name__ == "__main__":
    main()
