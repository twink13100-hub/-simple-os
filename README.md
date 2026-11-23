# S1MPLE OS (Android alpha)

A fullscreen Android WebView shell with a local desktop UI and demo apps: Files, Browser, Settings, Gallery, Terminal and About.

## Build APK

The included GitHub Actions workflow builds `app/build/outputs/apk/debug/app-debug.apk` on every push to `main`/`master`, or when manually triggered from the Actions tab.

Requirements for local build: JDK 17, Android SDK platform 35, Gradle 8.9.

This is an alpha shell, not a full operating system. File access, real browser rendering inside the app, gallery picker, and deeper Android integration are not implemented yet.
