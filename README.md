# DeFi Tracker - Android App

A premium, high-performance DeFi portfolio tracker for Android, built with Jetpack Compose. This app allows users to monitor crypto pairs, manage their multi-chain wallets, and view transaction history with a sleek, modern UI inspired by top-tier trading platforms.

## ✨ Features

- **Real-time Pairs Tracking**: View live prices and 24h changes for crypto pairs (e.g., BTC/USDT, ETH/USDT).
- **Advanced Charts**: Interactive candlestick charts with Bollinger Bands, StochRSI, and Volume indicators.
- **Multi-Chain Wallet**: Support for Ethereum, BNB Smart Chain (BSC), and Base. View fragmented balances in a unified, professional card-based layout.
- **Transaction History**: Track your activity across multiple networks with detailed transaction logs.
- **Modern UI/UX**: Built with Jetpack Compose, featuring dark mode, smooth animations, and a professional aesthetic.
- **App Widget**: Monitor your favorite crypto pairs directly from your home screen.
- **Splash Screen**: Branded entry experience using the modern Android SplashScreen API.

## 🛠 Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Dagger Hilt
- **Asynchronous Programming**: Kotlin Coroutines & Flow
- **Networking**: Retrofit & Gson
- **Database**: Room Persistence Library
- **Charts**: MPAndroidChart
- **Image Loading**: Coil
- **Widget**: Jetpack Glance
- **Design**: Vanilla CSS-like styling in Compose for a premium look.

## 🚀 Getting Started

### Prerequisites

- Android Studio Iguana or newer.
- Android SDK 26 (Android 8.0) or higher.

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/JoseVargasS/DeFi-tracker-Android-Apps.git
   ```
2. Open the project in Android Studio.
3. Sync Project with Gradle Files.
4. Run the `app` module on an emulator or physical device.

## 📦 Project Structure

```text
app/src/main/java/com/defitracker/app/
├── data/           # Repositories, APIs, and Local DB (Room)
├── di/             # Hilt Modules
├── domain/         # Models and Use Cases
├── presentation/   # UI Components (Compose Screens & ViewModels)
│   ├── crypto_list/
│   ├── crypto_detail/
│   ├── wallet/
│   └── transactions/
├── ui/             # Theme and Design System
└── widget/         # Home Screen Widget Implementation
```

## 📄 License

This project is for demonstration purposes. All rights reserved.
