# GAC GS8 Руководство — Android

Автономное Android-приложение для электронного руководства GAC GS8.

## Содержимое

- `app/` — нативная Android-оболочка WebView;
- материалы руководства копируются в `app/src/main/assets/site` перед сборкой;
- поиск, навигация и изображения работают без подключения к интернету.

## Сборка

Требуются JDK 17, Android SDK 35 и Gradle 8.7.

```bash
rm -rf app/src/main/assets/site
mkdir -p app/src/main/assets/site
unzip ../gac-gs8-manual-site.zip -d app/src/main/assets/site
gradle assembleDebug
```

Готовый тестовый APK появится в `app/build/outputs/apk/debug/`.
