# Universal Mod Loader — UML Core v0.1

Первый этап: Android-приложение для импорта и проверки JSON-манифеста.

## Что работает в этой версии
- Kotlin + Android Gradle Plugin.
- Выбор JSON-файла через системный picker.
- Проверка обязательных полей, `id`, `runtime` и целевой игры.
- Встроенный тестовый манифест.

## Ограничения
- Код модов **не загружается и не исполняется**.
- Внедрения в другие приложения нет.
- Совместимость с Unity, Unreal, Godot и конкретными играми не заявляется.
- `runtime: managed` пока только метка в манифесте.

## Сборка
Нужны JDK 17, Android SDK Platform 35 и установленный Gradle.

```bash
gradle --version
gradle assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

На телефоне нужен Android IDE с поддержкой Gradle/JDK 17 либо доверенная удалённая CI-сборка. Один Termux сам по себе не означает, что Android SDK уже настроен.
