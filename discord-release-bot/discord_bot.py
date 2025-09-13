import discord
from discord.ext import commands
import json
import os
from flask import Flask, request, jsonify
import threading
import asyncio

# Настройки бота
BOT_TOKEN = os.getenv('DISCORD_BOT_TOKEN')
WEBHOOK_URL = os.getenv('DISCORD_WEBHOOK_URL')
CHANNEL_ID = int(os.getenv('DISCORD_CHANNEL_ID', 0))

# Создание бота
intents = discord.Intents.default()
intents.message_content = True
bot = commands.Bot(command_prefix='!', intents=intents)

# Flask приложение для получения webhook от GitHub
app = Flask(__name__)

class ReleaseNotifier:
    def __init__(self, bot):
        self.bot = bot
        self.channel = None
    
    async def setup_channel(self):
        """Настройка канала для отправки уведомлений"""
        if CHANNEL_ID:
            self.channel = self.bot.get_channel(CHANNEL_ID)
            if not self.channel:
                print(f"Канал с ID {CHANNEL_ID} не найден!")
        else:
            print("CHANNEL_ID не установлен!")
    
    async def send_release_notification(self, release_data):
        """Отправка уведомления о релизе в Discord"""
        if not self.channel:
            await self.setup_channel()
        
        if not self.channel:
            print("Не удалось найти канал для отправки уведомлений")
            return
        
        # Создание embed для релиза
        embed = discord.Embed(
            title=f"🚀 Новый релиз: {release_data['name']}",
            description=release_data.get('body', 'Описание отсутствует'),
            color=0x00ff00,
            url=release_data.get('html_url', '')
        )
        
        # Добавление полей
        embed.add_field(
            name="📦 Версия", 
            value=release_data.get('tag_name', 'Не указана'), 
            inline=True
        )
        embed.add_field(
            name="👤 Автор", 
            value=release_data.get('author', {}).get('login', 'Неизвестно'), 
            inline=True
        )
        embed.add_field(
            name="📅 Дата создания", 
            value=release_data.get('created_at', 'Не указана')[:10], 
            inline=True
        )
        
        # Добавление информации о пре-релизе
        if release_data.get('prerelease', False):
            embed.add_field(
                name="⚠️ Статус", 
                value="Pre-release", 
                inline=True
            )
        
        # Добавление информации о драфте
        if release_data.get('draft', False):
            embed.add_field(
                name="📝 Статус", 
                value="Draft", 
                inline=True
            )
        
        # Добавление ссылок на ассеты
        assets = release_data.get('assets', [])
        if assets:
            asset_links = []
            for asset in assets[:5]:  # Ограничиваем до 5 ассетов
                asset_links.append(f"[{asset['name']}]({asset['browser_download_url']})")
            
            if asset_links:
                embed.add_field(
                    name="📁 Загрузки", 
                    value="\n".join(asset_links), 
                    inline=False
                )
        
        # Добавление footer
        embed.set_footer(text=f"Репозиторий: {release_data.get('repository', {}).get('full_name', 'Неизвестно')}")
        
        # Добавление thumbnail (аватар автора)
        if release_data.get('author', {}).get('avatar_url'):
            embed.set_thumbnail(url=release_data['author']['avatar_url'])
        
        try:
            await self.channel.send(embed=embed)
            print(f"Уведомление о релизе {release_data['name']} отправлено успешно")
        except Exception as e:
            print(f"Ошибка при отправке уведомления: {e}")

# Создание экземпляра уведомлятеля
release_notifier = ReleaseNotifier(bot)

@bot.event
async def on_ready():
    """Событие готовности бота"""
    print(f'{bot.user} подключен к Discord!')
    await release_notifier.setup_channel()

@bot.command(name='ping')
async def ping(ctx):
    """Команда для проверки работы бота"""
    await ctx.send('Pong! Бот работает!')

@bot.command(name='setup')
async def setup_channel(ctx):
    """Команда для настройки канала уведомлений"""
    global CHANNEL_ID
    CHANNEL_ID = ctx.channel.id
    release_notifier.channel = ctx.channel
    await ctx.send(f'Канал {ctx.channel.name} настроен для получения уведомлений о релизах!')

@app.route('/webhook', methods=['POST'])
def github_webhook():
    """Webhook endpoint для получения уведомлений от GitHub"""
    try:
        # Получение данных от GitHub
        payload = request.get_json()
        
        # Проверка типа события
        event_type = request.headers.get('X-GitHub-Event')
        
        if event_type == 'release':
            # Обработка релиза
            release_data = payload.get('release', {})
            
            # Добавление информации о репозитории
            release_data['repository'] = payload.get('repository', {})
            
            # Отправка уведомления в Discord
            asyncio.create_task(release_notifier.send_release_notification(release_data))
            
            return jsonify({'status': 'success', 'message': 'Release notification sent'})
        
        elif event_type == 'push':
            # Обработка push (для тегов)
            ref = payload.get('ref', '')
            if ref.startswith('refs/tags/'):
                tag_name = ref.replace('refs/tags/', '')
                
                # Создание данных релиза из push события
                release_data = {
                    'name': f"Release {tag_name}",
                    'tag_name': tag_name,
                    'body': f"Автоматический релиз для тега {tag_name}",
                    'author': payload.get('pusher', {}),
                    'created_at': payload.get('head_commit', {}).get('timestamp', ''),
                    'html_url': f"{payload.get('repository', {}).get('html_url', '')}/releases/tag/{tag_name}",
                    'prerelease': False,
                    'draft': False,
                    'repository': payload.get('repository', {})
                }
                
                asyncio.create_task(release_notifier.send_release_notification(release_data))
                
                return jsonify({'status': 'success', 'message': 'Tag release notification sent'})
        
        return jsonify({'status': 'ignored', 'message': f'Event type {event_type} not handled'})
    
    except Exception as e:
        print(f"Ошибка в webhook: {e}")
        return jsonify({'status': 'error', 'message': str(e)}), 500

@app.route('/health', methods=['GET'])
def health_check():
    """Проверка здоровья сервиса"""
    return jsonify({'status': 'healthy', 'bot_ready': bot.is_ready()})

def run_flask():
    """Запуск Flask приложения"""
    app.run(host='0.0.0.0', port=5000, debug=False)

def run_bot():
    """Запуск Discord бота"""
    if not BOT_TOKEN:
        print("DISCORD_BOT_TOKEN не установлен!")
        return
    
    bot.run(BOT_TOKEN)

if __name__ == '__main__':
    # Запуск Flask в отдельном потоке
    flask_thread = threading.Thread(target=run_flask)
    flask_thread.daemon = True
    flask_thread.start()
    
    # Запуск Discord бота
    run_bot()
