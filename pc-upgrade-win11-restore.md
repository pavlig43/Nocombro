# Задание для Codex: восстановление софта после Windows 11

## Цель

Вернуть рабочую среду для Android Studio, Gradle и KMP-проекта на уже установленной Windows 11. Видеокарту не трогать.

## Текущее железо

- CPU: AMD Ryzen 5 2600
- Плата: Gigabyte B450M S2H
- BIOS: F50
- RAM: 32 ГБ DDR4-2666, 2 слота заняты
- Системный диск: SATA SSD 480 ГБ
- Второй диск: HDD 1 ТБ

## Выбранное железо

CPU: AMD Ryzen 7 5700X OEM.

Кулер: DeepCool AK400 или DeepCool AG400.

SSD: M.2 NVMe 1 ТБ.

RAM не менять: проверка сборки показала, что 32 ГБ хватает.

## Что сделать до замены CPU

1. Обновить BIOS на Gigabyte B450M S2H с F50 до свежей версии с сайта Gigabyte.
2. После обновления BIOS проверить, что ПК грузится со старым Ryzen 5 2600.
3. В BIOS включить XMP для RAM, если профиль есть.
4. После этого ставить Ryzen 7 5700X.

## Что поставить сразу

### Драйверы

- AMD Chipset Driver
- NVIDIA Driver / NVIDIA App

### База и связь

- Yandex Browser
- 7-Zip
- Notepad++
- Windows Terminal
- Telegram
- Codex

### Разработка

- Git
- GitHub CLI
- JetBrains Toolbox
- DB Browser for SQLite
- DBeaver

JDK отдельно сначала не ставить. Android Studio поставит свой JBR. Если проекту понадобится отдельный JDK, поставить позже.

### Медиа и загрузки

- SumatraPDF
- VLC
- OBS Studio
- AutoHotkey
- qBittorrent

### VPN

- AdGuardVPN

## Что восстановить из бэкапа

Бэкап лежит на рабочем столе:

```text
C:\Users\user\Desktop\Nocombro-Win11-Restore
```

Вернуть:

- `.ssh` в `C:\Users\user\.ssh`
- `.gitconfig` в `C:\Users\user\.gitconfig`
- `Nocombro` в `C:\Users\user\AppData\Roaming\Nocombro`
- `JetBrains-Roaming` в `C:\Users\user\AppData\Roaming\JetBrains`
- `YandexBrowser-User Data` в `C:\Users\user\AppData\Local\Yandex\YandexBrowser\User Data`
- `DBeaverData` в `C:\Users\user\AppData\Roaming\DBeaverData`
- `obs-studio` в `C:\Users\user\AppData\Roaming\obs-studio`
- `Adguard Software Limited` в `C:\Users\user\AppData\Roaming\Adguard Software Limited`
- `AdGuard-Local` в `C:\Users\user\AppData\Local\AdGuard`
- `Adguard_Software_Limited-Local` в `C:\Users\user\AppData\Local\Adguard_Software_Limited`
- `Codex-Global` в `C:\Users\user\.codex`: глобальный `AGENTS.md` и skill `writing-simple`

`JetBrains-Roaming` нужен для настроек IDE: темы, горячие клавиши, плагины и прочий конфиг.

Настройки Telegram и qBittorrent в старой системе не найдены. После установки войти и настроить их заново.

В `Nocombro` должны быть важные файлы:

- `mobile-sync-secrets.properties` - секреты для сборки Android APK и sync телефона
- `ydb-sa-key.json` - ключ YDB для desktop, CLI и пересборки Android-секретов
- `s3.properties` - доступ к S3 для файлов
- `nocombro.db` - локальная база

## Проверка после установки

1. Проверить, что Git видит ключи SSH.
2. Открыть Nocombro в Android Studio.
3. Дать Android Studio скачать SDK и индексы.
4. Настроить Android CLI и Android Studio semantic tools по инструкции:

```text
C:\Users\user\Desktop\Nocombro-Win11-Restore\Docs\ANDROID_STUDIO_CLI_SETUP.md
```

5. Проверить:

```powershell
android studio check
```

6. Запустить замер сборки:

```powershell
.\gradlew :app:desktopApp:compileKotlinDesktop --rerun-tasks --no-daemon
```

7. Проверить, что локальные данные Nocombro на месте.
8. Сравнить время сборки со старым ПК.

Старый замер на Ryzen 5 2600, 32 ГБ DDR4-2666 и SATA SSD:

- команда: `.\gradlew :app:desktopApp:compileKotlinDesktop --rerun-tasks --no-daemon`
- результат: `BUILD SUCCESSFUL in 2m 48s`
- задач: `169 actionable tasks: 169 executed`
- CPU: пики до 90%
- диск: пики до 99% active time
- RAM: максимум около 15 ГБ занято, около 17 ГБ свободно
