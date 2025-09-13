#!/usr/bin/env python3
"""
Простой тест webhook'а Discord
"""

import requests
import json

def test_discord_webhook():
    """Тест отправки сообщения в Discord через webhook"""
    
    webhook_url = "https://discord.com/api/webhooks/1416361961263857774/8_jlWLGQtAE5Fei0zhqxnEt5CCk97rFQDnAs7XTktJiR3CNDLQkOpKUas8--5jz2GEHb"
    
    # Простое сообщение
    payload = {
        "content": "🧪 Тест webhook'а! Если вы видите это сообщение, то webhook работает!"
    }
    
    try:
        response = requests.post(webhook_url, json=payload)
        response.raise_for_status()
        print("✅ Тест webhook'а успешен! Сообщение отправлено в Discord")
        return True
    except requests.exceptions.RequestException as e:
        print(f"❌ Ошибка при тестировании webhook'а: {e}")
        return False

if __name__ == "__main__":
    print("Тестирование Discord webhook'а...")
    test_discord_webhook()
