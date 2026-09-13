# Стенд Android ↔ ESP32

2026-09-13. Первый этап — Android; iOS пользователь разрешил пока заменить заглушкой/отложить. iOS-приложение и KMP-модуль ещё не созданы. Идентификатор проекта по прямому указанию пользователя: **vea.raceremote** (applicationId, namespace, Kotlin packages и тесты).

Подтверждены паяльник, мультиметр и 3D-принтер. Подключены:

- Samsung SM-G973F, Android 12, ADB `RF8M811X8FK`.
- ESP32, COM4, CP210x 10C4:EA60; esptool определил ESP32-D0WDQ6 rev 1.0, 4 MB flash, кварц 40 MHz, MAC c8:f0:9e:a6:6d:a0. Название WROOM-32U и micro-USB — со слов пользователя; модель несущей платы ещё не осмотрена. Пользователь подтвердил: внешняя антенна не подключена.

Перед дальнейшими радиотестами подключить антенну 2,4 ГГц, 50 Ом, U.FL / I-PEX MHF I. По [Espressif, datasheet v2.7, раздел 9.2](https://www.espressif.com/sites/default/files/documentation/esp32-wroom-32d_esp32-wroom-32u_datasheet_en.pdf) рекомендуемое усиление не выше 2,33 dBi; практически подбираем компактную антенну около 2 dBi. MHF4 — другой разъём. Отсутствие антенны является вероятной причиной нестабильности, но окончательная проверка причины — повторить тест после установки.

## Запуск

Прошивка находится в соседнем checkout `C:\Users\Evgeny\orca\RC_CAR_ESP32`, проверенный коммит `940afab`. PlatformIO 6.1.14, espressif32 6.9.0, Arduino ESP32 2.0.17, WebSockets 2.6.1. Сборка `pio run -e esp32-bench`; прошивка `pio run -e esp32-bench -t upload --upload-port COM4`. Закрыть serial monitor перед прошивкой. После запуска serial 115200 печатает READY, имя Wi-Fi и `outputs=disabled`.

До первой прошивки сохранены все 4 MB исходной flash в локальный игнорируемый `RC_CAR_ESP32/artifacts/original-COM4-20260913.bin`. SHA256: `1BDC0AC66B8BF886EAF6503C22232DDF5A0A36311B89590103C4BC407F17D95A`. Это локальная резервная копия, не файл репозитория. Не публиковать: образ может содержать данные старой прошивки.

Android, PowerShell из корня RaceRemote, JDK 17:

```powershell
$env:ANDROID_HOME = 'C:/Users/Evgeny/AppData/Local/Android/Sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:connectedDebugAndroidTest --console=plain
& "$env:ANDROID_HOME/platform-tools/adb.exe" -s RF8M811X8FK install -r app/build/outputs/apk/debug/app-debug.apk
& "$env:ANDROID_HOME/platform-tools/adb.exe" -s RF8M811X8FK shell am start -n vea.raceremote/.app.MainActivity
```

В настройках телефона выбрать Wi-Fi `RaceRemote-a06da6`, пароль `RaceRemote-01`. Samsung спрашивает о сети без интернета — в испытании выбрано «Только в этот раз». В приложении адрес `192.168.4.1`, затем «Подключить». Газ и руль доступны после ACK, «СТОП» сбрасывает сессию. Для диагностики доступен HTTP `http://192.168.4.1/status`, лог Android с тегом `RaceRemoteControl` и serial контроллера.

Новая debug-сборка установлена на Samsung; прежняя тестовая установка удалена после смены идентификатора. Проверка пакета на телефоне прошла. [Результаты проверки связи и открытые проблемы](control-v1.md#проверка-и-ограничения) отделены от успешной сборки: пока это стенд команд, не ездящая машинка.

Следующий шаг: устранить тайм-ауты радиоканала, проверить газ/руль/отпускание/фон/Wi-Fi-loss. Затем осмотреть рулевой привод и проводку K989, зафиксировать схему стенда привода и подключить нагрузку. Одновременно остаются подбор камеры под 30/60 FPS, питание 2S и компоновка повторяемого целевого шасси.
