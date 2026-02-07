# ChatV2 - Профессиональное локальное чат-приложение

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-red.svg)](https://maven.apache.org/)
[![Netty](https://img.shields.io/badge/Netty-4.1.109-blue.svg)](https://netty.io/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/Tests-344%20PASSING-brightgreen.svg)](#тестирование)

Профессиональное локальное чат-приложение с клиент-серверной архитектурой, построенное на Java 21 с современными возможностями, включая виртуальные потоки, шифрование AES-256 и графический интерфейс JavaFX.

## 📋 Содержание

 - [Что нового](#что-нового)
 - [Особенности](#особенности)
 - [Архитектура](#архитектура)
 - [Технологический стек](#технологический-стек)
 - [Требования](#требования)
 - [Быстрый старт](#быстрый-старт)
 - [Конфигурация](#конфигурация)
 - [Логирование](#логирование)
 - [Структура проекта](#структура-проекта)
 - [Архитектура протокола](#архитектура-протокола)
 - [Шифрование](#шифрование)
 - [Разработка](#разработка)
 - [Тестирование](#тестирование)
 - [Безопасность](#безопасность)
 - [Устранение неполадок](#устранение-неполадок)
 - [Дорожная карта](#дорожная-карта)

## 🆕 Что нового

### Последние изменения (Февраль 2026)

#### 🐛 Исправления
- **Исправление проблемы с открытием Logs в серверном GUI** (2026-02-07)
  - Проблема: Представление Logs не загружалось при открытии
  - Реализована асинхронная инициализация Log4j2 appender с retry механизмом
  - Добавлен fallback на чтение логов из файла при неудаче инициализации
  - Исправлена фильтрация логов по уровню (numeric comparison)
  - Удалены ANSI коды из UI для чистого отображения
  - Все 9 новых тестов прошли успешно
  - Общее количество тестов: 135/135 PASS
  - Подробности см. в [chat-server/CHANGELOG.md](chat-server/CHANGELOG.md)

- **Исправление ошибок GUI**
  - Исправлены проблемы с загрузкой FXML-файлов
  - Исправлены ошибки загрузки иконок в интерфейсе
  - Стабилизирована работа JavaFX-компонентов

- **Исправление ошибки H2 базы данных**
  - Нормализованы пути к файлам базы данных
  - Исправлена проблема с путями при запуске на Windows
  - Добавлена корректная поддержка `AUTO_SERVER=TRUE` для H2

#### ✨ Новые функции
- **Реализация бинарного протокола**
  - Полная реализация 28-байтного заголовка `PacketHeader`
  - Поддержка бинарного формата пакетов: `[Header: 28 байт][Payload: N байт][Checksum: 4 байта]`
  - CRC32 контрольная сумма для целостности данных

- **Интеграция BinaryMessageCodec**
  - Кодек `BinaryMessageCodec` интегрирован в серверную часть
  - Кодек `BinaryMessageCodec` интегрирован в клиентскую часть
  - Полная поддержка шифрования и сжатия в бинарном формате

- **Модульная система шифрования через SPI**
  - Реализован интерфейс `EncryptionPlugin` для расширяемой криптографии
  - AES-256-GCM плагин для симметричного шифрования сообщений
  - RSA-4096 плагин для обмена ключами
  - Автоматическая загрузка всех плагинов через ServiceLoader при старте сервера
  - Поддержка добавления собственных плагинов шифрования

#### 📝 Улучшения
- **Разделение логов по модулям**
  - Обновлена конфигурация `logback.xml`
  - Логи разделены по модулям: сервер, клиент, шифрование
  - Цветной вывод в консоль и ротация лог-файлов

- **Упрощение запуска**
  - Создан `build_and_run.bat` для полной сборки и запуска
  - Создан `run_server.bat` для запуска сервера с UTF-8 кодировкой
  - Все bat-скрипты поддерживают корректное отображение кириллицы

- **Исправления серверного GUI**
  - **Исправлена загрузка представлений Chats и Logs**
    - Проблема: Представления Chats и Logs не загружались при нажатии соответствующих кнопок
    - Причина: Отсутствовал импорт `javafx.scene.layout.Region` в FXML файлах
    - Решение: Добавлен импорт Region в `ChatManagementView.fxml` и `LogViewerView.fxml`
  
  - **Исправлено отображение графика в темной теме**
    - Проблема: Подписи осей, деления и легенда графика не были видны на темном фоне
    - Решение: Добавлены CSS стили для:
      - Осей графика (tick-label-fill, axis-label)
      - Сетки графика (horizontal-grid-lines, vertical-grid-lines)
      - Легенды (chart-legend, chart-legend-item)
      - Цветов линий (#4ec9b0 для Messages, #0078d7 для Connections)
      - Фона графика (chart-plot-background)
  
  - **Оптимизировано ограничение точек графика**
    - Статус: Защита уже была реализована, но улучшена
    - Изменение: Вынесено ограничение точек в константу `MAX_CHART_POINTS = 20`
    - Эффект: График обновляется каждые 5 секунд, но отображает только последние 20 точек (100 секунд истории)

---

## ✨ Особенности

- **Современный Java 21**: Использование виртуальных потоков для массового параллелизма
- **Защищённое общение**: AES-256-GCM шифрование с обменом ключами RSA-4096
- **Автообнаружение**: Автоматическое обнаружение серверов через UDP broadcast
- **Богатый GUI**: Интерфейс на JavaFX для клиента и админ-панели сервера
- **Плагинная архитектура**: Расширяемая система шифрования через SPI
- **Полнофункциональность**: Профили пользователей, аватары, личные/групповые чаты, история сообщений
- **Встроенная БД**: База данных H2 для персистентности данных
- **Реальное время**: Мгновенные сообщения с индикаторами набора текста и уведомлениями о прочтении

## 🏗 Архитектура

```
┌──────────────────┐         UDP/TCP         ┌──────────────────┐
│   ChatClient     │ ◄──── Discovery ────── │   ChatServer     │
│  (JavaFX GUI)    │ ◄──── Encrypted ────── │  (Netty Server)  │
└──────────────────┘         AES-256          └──────────────────┘
                                                   │
                                            ┌──────┴──────┐
                                            │   H2 DB     │
                                            └─────────────┘
```

## 🛠 Технологический стек

| Технология | Версия | Назначение |
|------------|---------|------------|
| Java | 21 (LTS) | Основной язык с виртуальными потоками |
| Netty | 4.1.109.Final | Асинхронный сетевой фреймворк |
 | JavaFX | 21.0.1 | Современный GUI для десктопа |
 | H2 Database | 2.2.224 | Встроенная SQL база данных |
  | Bouncy Castle | 1.77 | Криптографические примитивы |
  | JUnit | 5.10.2 | Модульное и интеграционное тестирование |
  | Logback | 1.4.14 | Фреймворк логирования с модульной конфигурацией |
  | Maven | 3.9.6 | Сборка и управление зависимостями |
  | Jackson | 2.16.1 | Сериализация JSON |
  | Encryption API | 1.0.0 | Интерфейс плагинов шифрования (SPI) |
  | AES Plugin | 1.0.0 | Реализация AES-256-GCM через SPI |
  | RSA Plugin | 1.0.0 | Реализация RSA-4096 через SPI |

## 📦 Требования

### Обязательные

- **Java 21** или выше ([Скачать](https://www.oracle.com/java/technologies/downloads/))
- **Maven 3.9** или выше ([Скачать](https://maven.apache.org/download.cgi))

### Опциональные

- **JavaFX SDK** (обычно включён в JDK 21)
- **IDE** (IntelliJ IDEA, Eclipse или NetBeans)

### Плагины шифрования

Плагины шифрования подключаются как Maven зависимости в модули сервера и клиента:

```xml
<!-- API плагинов шифрования -->
<dependency>
    <groupId>com.chatv2</groupId>
    <artifactId>chat-encryption-api</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Плагин AES-256-GCM -->
<dependency>
    <groupId>com.chatv2</groupId>
    <artifactId>chat-encryption-aes</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Плагин RSA-4096 -->
<dependency>
    <groupId>com.chatv2</groupId>
    <artifactId>chat-encryption-rsa</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 🚀 Быстрый старт

### Установка и запуск через BAT-скрипты (Windows)

Самый простой способ запуска на Windows с использованием готовых скриптов:

#### Полная сборка и запуск

```batch
# Выполнить полную сборку проекта и запустить сервер
build_and_run.bat
```

Этот скрипт:
1. Переключает кодировку консоли на UTF-8 для корректного отображения кириллицы
2. Выполняет `mvn clean package -DskipTests` для сборки всех модулей
3. Автоматически находит JAR-файл и запускает сервер

#### Запуск сервера

```batch
# Запуск сервера (предварительно должен быть собран проект)
run_server.bat
```

Этот скрипт:
1. Устанавливает кодировку UTF-8 (`chcp 65001`)
2. Запускает сервер из JAR-файла `chat-apps/chat-server-launcher/target/chat-server-launcher-1.0.0-jar-with-dependencies.jar`
3. Передаёт все аргументы командной строки приложению

#### Запуск клиента

Для запуска клиента используется аналогичный подход:

```batch
# После сборки проекта, запустите клиент
java -jar chat-apps/chat-client-launcher/target/chat-client-launcher-1.0.0-jar-with-dependencies.jar
```

Или создайте файл `run_client.bat`:
```batch
@echo off
chcp 65001 >nul
echo Starting ChatV2 Client...
java -jar chat-apps/chat-client-launcher/target/chat-client-launcher-1.0.0-jar-with-dependencies.jar %*
pause
```

### Сборка проекта (Maven)

```bash
# Клонирование репозитория
git clone <repository-url>
cd Chatv2

# Сборка всех модулей
mvn clean install

# Пропустить тесты
mvn clean install -DskipTests

# Сборка конкретного модуля
mvn clean install -pl chat-server

# Создание исполняемых JAR-файлов
mvn clean package
```

### Запуск сервера

```bash
# Переход в директорию launcher'а
cd chat-apps/chat-server-launcher

# Запуск сервера
java -jar target/chat-server-launcher-1.0.0-jar-with-dependencies.jar

# Или с помощью Maven
mvn exec:java -Dexec.mainClass="com.chatv2.launcher.server.ServerLauncher"
```

**Сервер выполнит следующие действия:**
- Запустится на порту `8080` (настраивается)
- Будет слушать UDP broadcast на порту `9999`
- Инициализирует встроенную БД H2 в `data/chat.db`
- Запустит админ-панель (JavaFX GUI)
- Автоматически загрузит все плагины шифрования через SPI:
  - AES-256-GCM плагин (активный по умолчанию)
  - RSA-4096 плагин (для обмена ключами)

### Запуск клиента

```bash
# Переход в директорию launcher'а
cd chat-apps/chat-client-launcher

# Запуск клиента
java -jar target/chat-client-launcher-1.0.0-jar-with-dependencies.jar

# Или с помощью Maven
mvn exec:java -Dexec.mainClass="com.chatv2.launcher.client.ClientLauncher"
```

**Клиент выполнит следующие действия:**
- Покажет экран выбора сервера (автообнаруженные серверы)
- Запросит вход или регистрацию
- Отобразит основной интерфейс чата

## ⚙️ Конфигурация

### Конфигурация сервера

Создайте/отредактируйте `config/server-config.yaml`:

```yaml
server:
  host: "0.0.0.0"
  port: 8080
  name: "ChatV2 Server"

udp:
  enabled: true
  multicast_address: "239.255.255.250"
  port: 9999
  broadcast_interval_seconds: 5

database:
  path: "data/chat.db"
  connection_pool_size: 10

encryption:
  required: true
  default_plugin: "AES-256"
  rsa_key_size: 4096

session:
  token_expiration_seconds: 3600
  refresh_token_expiration_days: 7

 logging:
   level: "INFO"
   file: "logs/server.log"
 ```

### Логирование

Проект использует **Logback** для логирования с модульной конфигурацией в файле `chat-common/src/main/resources/logback.xml`.

#### Особенности конфигурации

- **Цветной вывод в консоль:** Разные цвета для уровней логирования
- **Разделение логов по модулям:** Отдельные файлы для сервера, клиента и шифрования
- **Ротация логов:** По размеру (10MB) и времени (ежедневно)
- **Сжатие:** Архивирование старых логов в GZIP
- **UTF-8 кодировка:** Полная поддержка кириллицы в логах

#### Лог-файлы

| Файл | Назначение | Фильтр пакетов |
|------|------------|----------------|
| `logs/application.log` | Общий лог всех приложений | Все пакеты |
| `logs/server.log` | Лог сервера | `com.chatv2.server.*`, `com.chatv2.launcher.server.*` |
| `logs/client.log` | Лог клиента | `com.chatv2.client.*`, `com.chatv2.launcher.client.*` |
| `logs/encryption.log` | Лог модулей шифрования | `com.chatv2.encryption.*` |

#### Уровни логирования

| Пакет | Уровень | Описание |
|-------|---------|----------|
| `com.chatv2.server` | DEBUG | Подробные логи серверной части |
| `com.chatv2.launcher.server` | DEBUG | Лог запуска сервера |
| `com.chatv2.client` | DEBUG | Подробные логи клиентской части |
| `com.chatv2.launcher.client` | DEBUG | Лог запуска клиента |
| `com.chatv2.encryption` | INFO | Лог шифрования (без лишних деталей) |
| `com.chatv2.common` | INFO | Общие компоненты |
| `root` | INFO | Корневой уровень |

#### Структура конфигурации logback.xml

```xml
<configuration scan="true" scanPeriod="30 seconds">
    <!-- Консольный аппендер с цветным выводом -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <pattern>%d{HH:mm:ss.SSS} [%thread] %highlight(%-5level) %cyan(%logger{36}) - %msg%n</pattern>
        <charset>UTF-8</charset>
    </appender>

    <!-- Файловые аппендеры с ротацией и фильтрацией по модулям -->
    <appender name="FILE_SERVER" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/server.log</file>
        <filter class="ch.qos.logback.core.filter.EvaluatorFilter">
            <expression>logger.startsWith("com.chatv2.server") || logger.startsWith("com.chatv2.launcher.server")</expression>
        </filter>
    </appender>
    <!-- ... аналогично для FILE_CLIENT и FILE_ENCRYPTION ... -->

    <!-- Настройка уровней по пакетам -->
    <logger name="com.chatv2.server" level="DEBUG">
        <appender-ref ref="FILE_SERVER"/>
    </logger>
</configuration>
```

#### Просмотр логов

```bash
# Просмотр логов сервера в реальном времени
tail -f logs/server.log

# Просмотр всех логов
cat logs/application.log

# Поиск ошибок в логах
grep ERROR logs/*.log
```

 ### Конфигурация клиента

Создайте/отредактируйте `config/client-config.yaml`:

```yaml
client:
  name: "ChatV2 Client"
  version: "1.0.0"

discovery:
  enabled: true
  multicast_address: "239.255.255.250"
  port: 9999
  timeout_seconds: 30

connection:
  reconnect_attempts: 5
  reconnect_delay_seconds: 5
  heartbeat_interval_seconds: 30

encryption:
  enabled: true

ui:
  theme: "dark"
  language: "ru"
  avatar_size: 64
```

## 📂 Структура проекта

```
 chatv2/
 ├── pom.xml                                    # Корневой Maven POM
 ├── README.md                                  # Этот файл
 ├── TECHNICAL_SPEC.md                          # Техническое задание
 ├── PROTOCOL_SPEC.md                           # Спецификация протокола
 ├── DEVELOPMENT_PLAN.md                        # План развития
 ├── build_and_run.bat                          # Скрипт полной сборки и запуска (Windows)
 ├── run_server.bat                             # Скрипт запуска сервера (Windows)
 ├── build_and_test.bat                         # Скрипт сборки и тестирования
 │
├── chat-common/                               # Общий модуль
│   ├── pom.xml
│   └── src/main/java/com/chatv2/common/
│       ├── model/                             # Модели данных
│       ├── protocol/                          # Бинарный протокол
│       ├── crypto/                            # Криптографические утилиты
│       └── exception/                         # Кастомные исключения
│
├── chat-server/                               # Серверный модуль
│   ├── pom.xml
│   └── src/main/java/com/chatv2/server/
│       ├── core/                              # Ядро сервера
│       ├── manager/                           # Бизнес-логика
│       ├── handler/                           # Netty обработчики
│       ├── storage/                           # Доступ к БД
│       └── gui/                               # Админ-интерфейс
│
├── chat-client/                               # Клиентский модуль
│   ├── pom.xml
│   └── src/main/java/com/chatv2/client/
│       ├── core/                              # Ядро клиента
│       ├── discovery/                         # UDP обнаружение
│       ├── gui/                               # JavaFX UI
│       └── network/                           # Сетевой клиент
│
 ├── chat-encryption-plugins/                   # Плагины шифрования (SPI)
 │   ├── chat-encryption-api/                   # API и интерфейс EncryptionPlugin
 │   ├── chat-encryption-aes/                   # Реализация AES-256-GCM
 │   └── chat-encryption-rsa/                   # Реализация RSA-4096
│
└── chat-apps/                                 # Лаунчеры приложений
    ├── chat-server-launcher/
    └── chat-client-launcher/
```

### Описание модулей

| Модуль | Описание | Статус |
|--------|----------|--------|
| chat-common | Общие модели, протокол, утилиты | ✅ Готово |
| chat-encryption-api | Интерфейс плагинов шифрования (SPI) | ✅ Готово |
| chat-encryption-aes | Реализация AES-256-GCM через SPI | ✅ Готово |
| chat-encryption-rsa | Реализация RSA-4096 через SPI | ✅ Готово |
| chat-server | Серверная логика и GUI | ✅ Готово |
| chat-client | Клиентская логика и GUI | ⚠️ В разработке |
| chat-server-launcher | Запуск сервера | ✅ Готово |
| chat-client-launcher | Запуск клиента | ✅ Готово |

## 🔐 Архитектура протокола

### Общая информация

Приложение использует бинарный протокол поверх TCP со следующими характеристиками:

- **Формат пакета:** `[Header: 28 байт][Payload: N байт][Checksum: 4 байта]`
- **Общий размер:** 32 + N байт (где N — длина полезной нагрузки)
- **Полезная нагрузка:** JSON-кодированные данные (опционально шифрованные)
- **Контрольная сумма:** CRC32 для заголовка и полезной нагрузки
- **Шифрование:** AES-256-GCM для всех аутентифицированных сообщений
- **Обмен ключами:** RSA-4096 для первоначального рукопожатия
- **Обнаружение:** UDP broadcast на 239.255.255.250:9999

### Структура пакета (Packet Format)

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          СТРУКТУРА БИНАРНОГО ПАКЕТА                              │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Смещение     Размер (байт)   Поле                  Описание                     │
│  ───────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  0x00          4               MAGIC_NUMBER           Идентификатор протокола     │
│                                                         "CHAT" (0x43484154)      │
│  0x04          2               MESSAGE_TYPE           Тип сообщения (enum)       │
│  0x06          1               VERSION                Версия протокола (0x01)    │
│  0x07          1               FLAGS                  Флаги (битовая маска)      │
│  0x08          8               MESSAGE_ID             UUID сообщения (8 байт)     │
│  0x10          4               PAYLOAD_LENGTH         Длина полезной нагрузки    │
│  0x14          8               TIMESTAMP              Unix epoch (ms)             │
│  ─────────────────────────────────────────────────────────────────────────────── │
│  0x1C          28              HEADER                 Итого: 28 байт             │
│  0x1C+N        N               PAYLOAD                Полезная нагрузка (JSON)    │
│  0x1C+N+4      4               CHECKSUM               CRC32 (Header + Payload)    │
│  ─────────────────────────────────────────────────────────────────────────────── │
│  ИТОГО         32 + N                                  БАЙТ                       │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

### Поля заголовка (PacketHeader)

| Поле | Размер | Описание | Константы |
|------|--------|----------|-----------|
| `MAGIC` | 4 байта | Magic number "CHAT" | `0x43484154` |
| `MESSAGE_TYPE` | 2 байта | Тип сообщения (uint16) | enum `ProtocolMessageType` |
| `VERSION` | 1 байт | Версия протокола | `0x01` |
| `FLAGS` | 1 байт | Флаги (битовая маска) | см. ниже |
| `MESSAGE_ID` | 8 байт | UUID сообщения (compact) | первые 8 байт UUID |
| `PAYLOAD_LENGTH` | 4 байта | Длина полезной нагрузки | uint32 |
| `TIMESTAMP` | 8 байт | Время создания (ms) | Unix epoch |

### Флаги (Flags)

| Бит | Маска | Название | Описание |
|-----|-------|----------|----------|
| 7 | `0x80` | `ENCRYPTED` | Полезная нагрузка зашифрована AES-256-GCM |
| 6 | `0x40` | `COMPRESSED` | Полезная нагрузка сжата (GZIP) |
| 5 | `0x20` | `URGENT` | Срочное сообщение (высокий приоритет) |
| 4 | `0x10` | `ACK_REQUIRED` | Требуется подтверждение получения |
| 3 | `0x08` | `REPLY` | Это ответ на предыдущее сообщение |

### CRC32 Контрольная сумма

- **Расположение:** Последние 4 байта пакета
- **Покрывает:** Заголовок (28 байт) + Полезная нагрузка (N байт)
- **Алгоритм:** Стандартный CRC32 (Java `java.util.zip.CRC32`)
- **Назначение:** Проверка целостности данных при передаче

### Поддержка шифрования

Протокол поддерживает шифрование на нескольких уровнях:

1. **Поля в заголовке:**
   - Флаг `ENCRYPTED` (0x80) указывает, что полезная нагрузка зашифрована
   - Типы сообщений аутентификации и обмена ключами не шифруются

2. **Процесс шифрования:**
   - **Обмен ключами:** RSA-4096 с OAEP padding для первоначального рукопожатия
   - **Сессионное шифрование:** AES-256-GCM для всех сообщений после аутентификации
   - **IV:** 128-битный случайный вектор инициализации для каждого сообщения
   - **Tag:** 128-битный тег аутентификации GCM

### Реализация в коде

Основные классы реализации:

- **`PacketHeader`** - 28-байтный заголовок пакета (`chat-common/src/main/java/com/chatv2/common/protocol/PacketHeader.java`)
  - Методы `read()` и `write()` для работы с ByteBuffer
  - Валидация полей через метод `isValid()`
  - Компактное хранение UUID (8 байт)

- **`BinaryMessageCodec`** - Netty кодек для кодирования/декодирования (`chat-common/src/main/java/com/chatv2/common/protocol/BinaryMessageCodec.java`)
  - Кодирование: `ChatMessage` → `ByteBuf`
  - Декодирование: `ByteBuf` → `ChatMessage`
  - Автоматический расчёт и проверка CRC32
  - Поддержка шифрованных полезных нагрузок

### Пример использования

```java
// Создание заголовка
PacketHeader header = PacketHeader.create(
    ProtocolMessageType.MESSAGE_SEND_REQ.getCode(),
    PacketHeader.FLAG_ENCRYPTED,
    messageId,
    payloadBytes.length
);

// Кодирование сообщения
BinaryMessageCodec codec = new BinaryMessageCodec();
ChannelHandlerContext ctx = ...;
ChatMessage message = new ChatMessage(...);
List<Object> out = new ArrayList<>();
codec.encode(ctx, message, out);
ByteBuf packet = (ByteBuf) out.get(0);
```

 **Подробная информация:** Смотрите полную спецификацию в файле [PROTOCOL_SPEC.md](PROTOCOL_SPEC.md)

## 🔒 Шифрование

### Конфигурация по умолчанию

- **Алгоритм:** AES-256-GCM
- **Размер ключа:** 256 бит
- **Размер IV:** 128 бит (случайный для каждого сообщения)
- **Размер Tag:** 128 бит (аутентификация)
- **Обмен ключами:** RSA-4096 с OAEP padding

### Плагины шифрования

ChatV2 использует модульную архитектуру плагинов шифрования, которая позволяет легко расширять систему новыми алгоритмами шифрования.

#### Поддерживаемые плагины

| Плагин | Алгоритм | Назначение | Статус |
|--------|----------|------------|--------|
| **AES-256-GCM** | AES-256-GCM | Симметричное шифрование сообщений | ✅ Активен по умолчанию |
| **RSA-4096** | RSA/ECB/OAEP | Обмен ключами, цифровая подпись | ✅ Поддерживается |

#### Система плагинов (SPI)

Архитектура плагинов основана на Java SPI (Service Provider Interface):

```
┌─────────────────────────────────────────────────────────────┐
│                  EncryptionPluginManager                     │
│                   (Управление плагинами)                     │
└──────────────┬──────────────────────────────────────────────┘
               │
               ├── ServiceLoader.load(EncryptionPlugin.class)
               │
               ▼
        ┌──────────────┐
        │  SPI (JDK)   │  Автоматическое обнаружение
        └──────┬───────┘
               │
       ┌───────┴────────┐
       │                │
       ▼                ▼
┌──────────────┐  ┌──────────────┐
│ AES Plugin   │  │ RSA Plugin   │
│ AES-256-GCM  │  │ RSA-4096     │
└──────────────┘  └──────────────┘
```

**Как работает SPI:**

1. **Интерфейс плагина:** `EncryptionPlugin` (в `chat-encryption-api`)
   - Определяет контракт для всех плагинов шифрования
   - Методы: `encrypt()`, `decrypt()`, `generateKey()`, `getName()`

2. **Регистрация плагина:** Файл `META-INF/services/com.chatv2.encryption.api.EncryptionPlugin`
   - Содержит полное имя класса реализации
   - Пример для AES: `com.chatv2.encryption.aes.AesEncryptionPlugin`

3. **Автозагрузка:** `EncryptionPluginManager` использует `ServiceLoader`
   - Автоматически загружает все плагины при создании
   - Логирует: `Loaded encryption plugin: {name} v{version}`

4. **Выбор активного плагина:** Первый загруженный плагин становится активным
   - Можно изменить через `setActivePlugin(name)`
   - По умолчанию активен AES-256-GCM

#### Автоматическая загрузка плагинов

Оба плагина шифрования загружаются автоматически при старте сервера:

```java
// В ServerInitializer.java
public EncryptionPluginManager getEncryptionPluginManager() {
    return encryptionPluginManager;  // Автоматически инициализирован
}

// В логах при старте сервера:
// Loaded encryption plugin: AES-256-GCM v1.0.0
// Loaded encryption plugin: RSA-4096 v1.0.0
// Active encryption plugin set to: AES-256-GCM
```

#### Добавление нового плагина шифрования

Чтобы добавить новый плагин шифрования:

1. **Создайте модуль плагина:**

```xml
<!-- pom.xml нового плагина -->
<project>
    <parent>
        <groupId>com.chatv2</groupId>
        <artifactId>chat-encryption-plugins</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>chat-encryption-custom</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.chatv2</groupId>
            <artifactId>chat-encryption-api</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>
</project>
```

2. **Реализуйте интерфейс EncryptionPlugin:**

```java
package com.chatv2.encryption.custom;

import com.chatv2.encryption.api.EncryptionPlugin;
import com.chatv2.encryption.api.EncryptionAlgorithm;
import com.chatv2.common.crypto.EncryptionResult;
import java.security.Key;
import java.util.concurrent.CompletableFuture;

public class CustomEncryptionPlugin implements EncryptionPlugin {

    @Override
    public String getName() {
        return "CUSTOM-ALGORITHM";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public EncryptionAlgorithm getAlgorithm() {
        return new EncryptionAlgorithm("CUSTOM-ALGORITHM", "CustomTransformation");
    }

    @Override
    public CompletableFuture<EncryptionResult> encrypt(byte[] plaintext, Key key) {
        // Реализация шифрования
        return CompletableFuture.completedFuture(/* ... */);
    }

    @Override
    public CompletableFuture<byte[]> decrypt(byte[] ciphertext, byte[] iv, byte[] tag, Key key) {
        // Реализация дешифрования
        return CompletableFuture.completedFuture(/* ... */);
    }

    @Override
    public CompletableFuture<Key> generateKey() {
        // Генерация ключа
        return CompletableFuture.completedFuture(/* ... */);
    }

    @Override
    public boolean isKeyValid(Key key) {
        // Валидация ключа
        return key != null && key.getAlgorithm().equals("CUSTOM");
    }
}
```

3. **Зарегистрируйте плагин через SPI:**

Создайте файл `src/main/resources/META-INF/services/com.chatv2.encryption.api.EncryptionPlugin`:

```
com.chatv2.encryption.custom.CustomEncryptionPlugin
```

4. **Добавьте зависимость в pom.xml сервера/клиента:**

```xml
<dependency>
    <groupId>com.chatv2</groupId>
    <artifactId>chat-encryption-custom</artifactId>
    <version>1.0.0</version>
</dependency>
```

5. **Пересоберите проект:**

```bash
mvn clean install
```

Плагин будет автоматически загружен при следующем запуске сервера.

### Процесс обмена ключами

1. Клиент подключается к серверу
2. Сервер отправляет RSA публичный ключ (4096 бит)
3. Клиент генерирует AES-256 сессионный ключ
4. Клиент шифрует сессионный ключ RSA публичным ключом
5. Сервер расшифровывает сессионный ключ RSA приватным ключом
6. Все последующие сообщения шифруются AES-256

## 💻 Разработка

### Настройка IDE

#### IntelliJ IDEA

1. Откройте проект как Maven-проект
2. Убедитесь, что выбран JDK 21
3. Включите обработку аннотаций при необходимости
4. Запуск тестов: ПКМ на проект -> Run Tests

#### Eclipse

1. Импортируйте как Maven-проект
2. Настройте JRE Java 21
3. Обновите Maven-проект
4. Запуск как JUnit тест

### Запуск тестов

```bash
# Запуск всех тестов
mvn test

# Запуск тестов конкретного модуля
mvn test -pl chat-server

# Запуск конкретного тестового класса
mvn test -Dtest=UserManagerTest

# Генерация отчёта о покрытии
mvn test jacoco:report
```

### Качество кода

```bash
# Проверка стиля кода
mvn checkstyle:check

# Поиск багов
mvn spotbugs:check

# Анализ PMD
mvn pmd:check

# Полная проверка качества
mvn verify
```

## 🧪 Тестирование

### Статус тестов

| Модуль | Тесты | Статус |
|--------|-------|--------|
| chat-common | 202 | ✅ PASS |
| chat-encryption-api | 14 | ✅ PASS |
| chat-encryption-aes | 17 | ✅ PASS |
| chat-encryption-rsa | 16 | ✅ PASS |
| chat-server | 95 | ✅ PASS |
| chat-client | - | ⚠️ Нужны исправления |
| **ИТОГО** | **344** | **✅ PASS** |

 ### Недавние исправления (Февраль 2026)

#### GUI и FXML
- ✅ Исправлены вызовы getter-методов в ServerLauncher.java
- ✅ Добавлен отсутствующий импорт javafx.application.Application
- ✅ Исправлено использование устаревшего API в ClientLauncher.java
- ✅ Исправлены ошибки загрузки FXML-файлов
- ✅ Исправлены ошибки загрузки иконок в интерфейсе

#### Тестирование
- ✅ Исправлен дублирующийся код в ChatManagerTest.java
- ✅ Исправлены сбои тестов RSA шифрования (16/16 PASS)
- ✅ Исправлены все сбои тестов серверного модуля (95/95 PASS)
- ✅ Добавлен импорт CountDownLatch в базу тестов JavaFX
- ✅ Исправлены модификаторы доступа setUp/tearDown в GUI тестах

#### Бинарный протокол
- ✅ Реализован 28-байтный заголовок PacketHeader
- ✅ Интегрирован BinaryMessageCodec в сервер и клиент
- ✅ Добавлена поддержка CRC32 контрольной суммы
- ✅ Добавлена поддержка флагов шифрования и сжатия

#### База данных
- ✅ Исправлена ошибка H2 базы данных (пути к БД)
- ✅ Нормализованы пути для AUTO_SERVER=TRUE
- ✅ Исправлены проблемы с путями на Windows

#### Инфраструктура
- ✅ Создан build_and_run.bat для полной сборки и запуска
- ✅ Создан run_server.bat для запуска сервера с UTF-8
- ✅ Обновлена конфигурация logback.xml с разделением по модулям
- ✅ Все bat-скрипты поддерживают UTF-8 кодировку

## 🔐 Безопасность

### Лучшие практики безопасности

1. **Пароли**: Хранятся как хеши Argon2id с уникальной солью для каждого пользователя
2. **Токены сессии**: JWT с 256-битным секретом, срок действия 1 час
3. **Шифрование**: AES-256-GCM с аутентифицированным шифрованием
4. **Управление ключами**: RSA ключи хранятся в Java KeyStore
5. **Валидация ввода**: Все пользовательские данные очищаются и проверяются
6. **Ограничение скорости**: Защита от брутфорса при аутентификации
7. **Модульная криптография**: Расширяемая архитектура через SPI плагины шифрования

### Архитектура шифрования

Система шифрования ChatV2 построена на модульной архитектуре с поддержкой плагинов:

```
┌─────────────────────────────────────────────────────────────┐
│                    Безопасность сообщений                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────┐      RSA-4096       ┌─────────────────┐ │
│  │   Обмен ключами │ ◄──────────────────► │  Сервер         │ │
│  │   (клиент)      │   OAEP Padding      │  (Key Store)    │ │
│  └────────┬────────┘                     └─────────────────┘ │
│           │                                                    │
│           │ AES-256 сессионный ключ                            │
│           ▼                                                    │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │         AES-256-GCM (активный плагин)                    │ │
│  │  • IV 128 бит (уникальный для каждого сообщения)         │ │
│  │  • Tag 128 бит (аутентификация)                          │ │
│  │  • Защита от replay-атак                                │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Слои защиты:**

1. **Транспортный уровень:** Защита через TLS (опционально)
2. **Протокол шифрования:** AES-256-GCM для всех сообщений после аутентификации
3. **Обмен ключами:** RSA-4096 с OAEP padding для безопасной передачи сессионных ключей
4. **Аутентификация:** JWT токены с коротким сроком действия
5. **Интеграция с плагинами:** SPI позволяет легко добавлять новые алгоритмы шифрования

### Аудиты безопасности

- Предотвращение SQL-инъекций
- Защита от XSS (если добавлены веб-функции)
- Защита от CSRF
- Смягчение timing attacks
- Безопасная генерация случайных чисел

## 🎯 Производительность

### Бенчмарки

| Метрика | Цель | Текущее |
|---------|------|---------|
| Параллельные соединения | 10,000+ | TBD |
| Пропускная способность | 100,000 msg/s | TBD |
| Память на соединение | < 10KB | TBD |
| Время запуска (сервер) | < 5 сек | TBD |
| Задержка (средняя) | < 50ms | TBD |

### Оптимизация

- Виртуальные потоки для массового параллелизма
- Пул соединений для базы данных
- Асинхронный I/O с Netty
- Сжатие сообщений для больших полезных нагрузок
- Эффективный бинарный протокол

## 🔧 Устранение неполадок

### Сервер не запускается

- Проверьте, доступен ли порт 8080
- Убедитесь, что установлен Java 21: `java -version`
- Проверьте настройки фаервола для UDP/TCP портов
- Проверьте логи в `logs/server.log`

### Клиент не находит сервер

- Убедитесь, что UDP broadcast включён в конфигурации сервера
- Проверьте, разрешает ли фаервол UDP на порту 9999
- Убедитесь, что клиент и сервер в одной сети
- Попробуйте ввести адрес сервера вручную

### Проблемы с соединением

- Убедитесь, что настройки шифрования совпадают
- Проверьте сетевое подключение
- Проверьте логи сервера на наличие ошибок
- Убедитесь, что сервер не достиг лимита подключений

## 🗺 Дорожная карта

- [x] Основная архитектура
- [x] Бинарный протокол
- [x] Плагины шифрования
- [x] Серверный модуль
- [x] Тесты сервера (95/95)
- [x] Тесты шифрования (48/48)
- [x] Тесты общего модуля (202/202)
- [⚠️] Клиентский модуль (GUI/сеть)
- [ ] Полная реализация GUI
- [ ] Передача файлов
- [ ] Голосовые сообщения
- [ ] Видеозвонки
- [ ] Мобильный клиент (Android/iOS)
- [ ] Веб-клиент (WebSocket)
- [ ] Мультиязычность
- [ ] Маркетплейс плагинов

## 📚 Документация

- [Техническое задание](TECHNICAL_SPEC.md) - Полное техническое задание
- [Спецификация протокола](PROTOCOL_SPEC.md) - Детали бинарного протокола
- [План разработки](DEVELOPMENT_PLAN.md) - План поэтапной реализации

## 🤝 Участие в разработке

Вклады приветствуются! Пожалуйста, следуйте этим рекомендациям:

1. Форкните репозиторий
2. Создайте ветку для функции (`git checkout -b feature/amazing-feature`)
3. Зафиксируйте изменения (`git commit -m 'Add amazing feature'`)
4. Отправьте в ветку (`git push origin feature/amazing-feature`)
5. Откройте Pull Request

### Стиль кода

- Следуйте соглашениям Java Code Conventions
- Используйте 4-пробельный отступ
- Добавляйте JavaDoc для всех публичных API
- Пишите unit тесты для новых функций
- Убедитесь, что все тесты проходят перед PR

## 📄 Лицензия

Этот проект лицензирован под MIT License - см. файл [LICENSE](LICENSE) для деталей.

## 🙏 Благодарности

- Команде Netty за отличный сетевой фреймворк
- Сообществу JavaFX за современный toolkit
- Bouncy Castle за библиотеку криптографии
- Всем участникам этого проекта

## 📞 Контакт

- Руководитель проекта: [Ваше Имя]
- Email: your.email@example.com
- Issues: [GitHub Issues](https://github.com/yourusername/chatv2/issues)

---

**Версия:** 1.0.0
**Последнее обновление:** 7 Февраля 2026
**Статус:** В разработке

---

*ChatV2 - Безопасное общение, современная архитектура, открытый код* 💬🔐
