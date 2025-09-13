# Discord Release Bot

Discord бот для автоматической отправки уведомлений о релизах GitHub в Discord канал через GitHub Actions.

## 🚀 Возможности

- **Автоматические уведомления о релизах** - получайте красивые embed-сообщения о новых релизах
- **Уведомления о тегах** - отслеживание создания новых тегов
- **GitHub Actions интеграция** - полная автоматизация через GitHub Actions
- **Webhook поддержка** - получение уведомлений напрямую от GitHub
- **Настраиваемые embed** - красивые сообщения с полной информацией о релизе
- **Docker поддержка** - легкое развертывание в контейнере

## 📋 Требования

- Python 3.9+
- Discord Bot Token
- Discord Webhook URL (для GitHub Actions)
- GitHub репозиторий с настроенными Actions

## 🛠️ Установка

### 1. Клонирование репозитория

```bash
git clone <your-repo-url>
cd discord-release-bot
```

### 2. Установка зависимостей

```bash
pip install -r requirements.txt
```

### 3. Настройка переменных окружения

Скопируйте `env.example` в `.env` и заполните необходимые значения:

```bash
cp env.example .env
```

Отредактируйте `.env` файл:

```env
# Discord Bot Configuration
DISCORD_BOT_TOKEN=your_discord_bot_token_here
DISCORD_CHANNEL_ID=your_channel_id_here
DISCORD_WEBHOOK_URL=your_webhook_url_here

# GitHub Configuration (опционально)
GITHUB_WEBHOOK_SECRET=your_webhook_secret_here
```

## 🤖 Настройка Discord бота

### 1. Создание Discord приложения

1. Перейдите на [Discord Developer Portal](https://discord.com/developers/applications)
2. Создайте новое приложение
3. В разделе "Bot" создайте бота и скопируйте токен
4. Включите необходимые разрешения (Send Messages, Embed Links, etc.)

### 2. Создание Webhook

1. В настройках канала Discord выберите "Integrations" → "Webhooks"
2. Создайте новый webhook и скопируйте URL

### 3. Получение ID канала

1. Включите режим разработчика в Discord
2. Правый клик на канал → "Copy ID"

## ⚙️ Настройка GitHub Actions

### 1. Добавление секретов в репозиторий

В настройках вашего GitHub репозитория добавьте следующие секреты:

- `DISCORD_WEBHOOK_URL` - URL webhook'а Discord
- `GITHUB_TOKEN` - токен GitHub (автоматически предоставляется)

### 2. Настройка workflow

Workflow уже настроен в `.github/workflows/discord-release-notification.yml` и будет автоматически:

- Отправлять уведомления при создании релизов
- Отправлять уведомления при создании тегов
- Обрабатывать различные типы релизов (pre-release, draft, etc.)

## 🚀 Запуск

### Локальный запуск

```bash
python discord_bot.py
```

### Docker запуск

```bash
# Сборка образа
docker build -t discord-release-bot .

# Запуск контейнера
docker run -d \
  --name discord-release-bot \
  --env-file .env \
  -p 5000:5000 \
  discord-release-bot
```

### Docker Compose

```bash
docker-compose up -d
```

## 📝 Использование

### Команды бота

- `!ping` - проверка работы бота
- `!setup` - настройка текущего канала для уведомлений

### Webhook endpoints

- `POST /webhook` - получение уведомлений от GitHub
- `GET /health` - проверка состояния сервиса

## 🔧 Конфигурация

### Настройка через переменные окружения

| Переменная | Описание | Обязательная |
|------------|----------|--------------|
| `DISCORD_BOT_TOKEN` | Токен Discord бота | Да |
| `DISCORD_WEBHOOK_URL` | URL webhook'а Discord | Да |
| `DISCORD_CHANNEL_ID` | ID канала Discord | Нет |
| `GITHUB_WEBHOOK_SECRET` | Секрет для проверки webhook'ов | Нет |
| `FLASK_HOST` | Хост Flask сервера | Нет (по умолчанию: 0.0.0.0) |
| `FLASK_PORT` | Порт Flask сервера | Нет (по умолчанию: 5000) |

### Настройка GitHub Actions

Workflow поддерживает следующие события:

- `release` - создание, редактирование, публикация релизов
- `push` с тегами - создание новых тегов

## 📊 Примеры уведомлений

### Уведомление о релизе

```
🚀 Published Release: v1.2.0
Описание релиза...

📦 Tag: v1.2.0
👤 Author: username
📅 Created: 2024-01-15
📁 Downloads:
• app-v1.2.0.zip (2.5 MB)
• app-v1.2.0.tar.gz (1.8 MB)
```

### Уведомление о теге

```
🏷️ New Tag: v1.2.0-beta
Описание коммита...

🏷️ Tag Name: v1.2.0-beta
👤 Created by: username
📅 Created: 2024-01-15
🔗 Links: View Tag | View Commit
```

## 🐛 Устранение неполадок

### Бот не отвечает

1. Проверьте правильность токена бота
2. Убедитесь, что бот добавлен на сервер
3. Проверьте права бота в канале

### Webhook не работает

1. Проверьте правильность URL webhook'а
2. Убедитесь, что webhook активен
3. Проверьте логи на наличие ошибок

### GitHub Actions не срабатывает

1. Проверьте настройки секретов в репозитории
2. Убедитесь, что workflow файл находится в правильной папке
3. Проверьте права доступа к репозиторию

## 📄 Лицензия

MIT License

## 🤝 Вклад в проект

1. Форкните репозиторий
2. Создайте ветку для новой функции
3. Внесите изменения
4. Создайте Pull Request

## 📞 Поддержка

Если у вас возникли вопросы или проблемы, создайте Issue в репозитории.
