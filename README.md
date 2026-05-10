# Octopus DeFi Tracker

Android app for tracking crypto pairs, interactive price charts, multi-chain wallet balances, and transaction history. The app is built with Kotlin, Jetpack Compose, Hilt, Room, Retrofit, MPAndroidChart, Coil, and Glance widgets.

## What The App Does

Octopus is organized around three main tabs:

- `Pairs`: track USDT crypto pairs, search Binance symbols, view live prices, and open detail charts.
- `Wallet`: save wallet addresses, inspect multi-chain token balances, copy/delete wallets, and refresh balances.
- `History`: inspect wallet transactions grouped by network using CoinStats wallet transactions.

The detail screen includes candlestick charts, volume, Bollinger Bands, and StochRSI indicators.

## Current Features

- Real-time pair tracking for Binance-backed symbols.
- Search and add tracked crypto pairs.
- Pair detail screen with candlestick chart, live price updates, volume, Bollinger Bands, and StochRSI.
- Saved wallet list backed by Room.
- Multi-chain wallet balances through CoinStats.
- Transaction history through CoinStats, grouped by network.
- Home screen widget for tracked crypto pairs using Jetpack Glance.
- Compose navigation with animated transitions.
- Dark Material 3 UI tuned for a trading/portfolio app.
- Splash screen with app branding.

## Tech Stack

- Language: Kotlin
- UI: Jetpack Compose, Material 3, Compose Animation
- Navigation: Navigation Compose
- Dependency Injection: Hilt
- Persistence: Room
- Networking: Retrofit + Gson
- Async: Kotlin Coroutines and Flow
- Charts: MPAndroidChart through `AndroidView`
- Images: Coil
- Widgets: Jetpack Glance
- Minimum SDK: 26
- Target/Compile SDK: 34
- Java/Kotlin target: JVM 17

## Project Structure

```text
app/src/main/java/com/defitracker/app/
|-- core/
|   `-- Constants.kt
|-- data/
|   |-- local/
|   |   |-- AppDatabase.kt
|   |   |-- TrackedPairDao.kt
|   |   |-- TrackedPairEntity.kt
|   |   |-- WalletDao.kt
|   |   `-- WalletEntity.kt
|   |-- remote/
|   |   |-- BinanceApi.kt
|   |   |-- CoinStatsApi.kt
|   |   `-- dto/
|   `-- repository/
|       `-- CryptoRepositoryImpl.kt
|-- di/
|   `-- AppModule.kt
|-- domain/
|   |-- model/
|   `-- repository/
|-- presentation/
|   |-- crypto_detail/
|   |-- crypto_list/
|   |-- transactions/
|   `-- wallet/
|-- ui/theme/
|-- widget/
|-- DeFiTrackerApp.kt
`-- MainActivity.kt
```

## Data Sources

The app currently has clients for several APIs:

- Binance: symbol search, ticker stats, prices, and klines.
- CoinStats: wallet balances and transaction history.

### API Keys

API keys are currently stored in:

```text
app/src/main/java/com/defitracker/app/core/Constants.kt
```

Current constants include:

- `COINSTATS_API_KEY`

For production, move these keys out of source control. Recommended options:

- `local.properties` + Gradle `buildConfigField`
- encrypted remote config
- backend proxy for third-party APIs

## Setup

### Requirements

- Android Studio Iguana or newer.
- Android SDK 34 installed.
- JDK 17. Android Studio's bundled JBR works.
- Internet access for Gradle dependencies and external APIs.

### Open In Android Studio

1. Open this directory in Android Studio.
2. Let Gradle sync finish.
3. Select the `app` configuration.
4. Run on an emulator or physical device running Android 8.0+.

### Command Line Build

This repository currently contains `gradle/wrapper/gradle-wrapper.properties`, but does not include the wrapper scripts/JAR in the working tree. If wrapper files are restored, use:

```bash
./gradlew :app:assembleDebug
```

On Windows with an installed/cached Gradle distribution, build with Gradle and JDK 17:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
gradle :app:assembleDebug
```

In this workspace, the debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Main Screens

### Pairs

Files:

- `presentation/crypto_list/CryptoListScreen.kt`
- `presentation/crypto_list/CryptoListViewModel.kt`
- `presentation/crypto_list/components/CryptoPairItem.kt`

Behavior:

- Loads available USDT symbols from Binance.
- Adds/removes tracked pairs stored in Room.
- Refreshes prices periodically.
- Updates the Glance widget at a lower cadence than UI price refreshes.

### Crypto Detail

Files:

- `presentation/crypto_detail/CryptoDetailScreen.kt`
- `presentation/crypto_detail/CryptoDetailViewModel.kt`

Behavior:

- Reads `symbol` and `source` from navigation arguments.
- Loads pair details and kline data.
- Computes Bollinger Bands and StochRSI off the main thread.
- Uses MPAndroidChart in Compose through `AndroidView`.

### Wallet Explorer

Files:

- `presentation/wallet/WalletScreen.kt`
- `presentation/wallet/WalletViewModel.kt`

Behavior:

- Saves wallets in Room.
- Loads balances from CoinStats by chain.
- Uses sequential calls with small delays to avoid API rate limits.
- Shows real API/auth/rate-limit errors instead of silently treating them as empty balances.

### Transaction History

Files:

- `presentation/transactions/TransactionsScreen.kt`
- `presentation/transactions/TransactionsViewModel.kt`
- `data/remote/dto/CoinStatsTransactionDto.kt`

Behavior:

- Uses CoinStats `/wallet/transactions`.
- Queries networks sequentially to avoid rate limits.
- Filters CoinStats `Fill` records because they are synthetic balance entries rather than useful user transactions.
- Groups transactions by network in the UI.

## Local Database

Room database:

```text
data/local/AppDatabase.kt
```

Entities:

- `TrackedPairEntity`
- `WalletEntity`

DAOs:

- `TrackedPairDao`
- `WalletDao`

The database uses `fallbackToDestructiveMigration()` in `AppModule.kt`. This is convenient during early development, but production migrations should be added before real release use.

## Dependency Injection

Hilt module:

```text
app/src/main/java/com/defitracker/app/di/AppModule.kt
```

It provides:

- Retrofit API clients.
- Room database and DAOs.
- `CryptoRepository` implementation.

## Known Notes And Caveats

- Do not make aggressive parallel calls to CoinStats; it can hit rate limits quickly.
- Some third-party wallet transaction feeds include spam/fake tokens. UI should display the data clearly, but future filtering may be needed.
- `fallbackToDestructiveMigration()` can wipe local Room data after schema changes.
- API keys should not remain hard-coded for a public release.

## Troubleshooting

### Wallet balances show empty

Check:

- `COINSTATS_API_KEY` is valid.
- The address is a public wallet address supported by CoinStats.
- The API did not return 401/403/429.

The app now surfaces common CoinStats auth/rate-limit errors in UI state.

### Transaction history is empty

Check:

- The wallet is saved and selected.
- CoinStats supports the selected chain/address.
- CoinStats did not return synthetic-only records. `Fill` records are intentionally filtered.

### Build cannot find Java

Set `JAVA_HOME` to JDK 17. With Android Studio:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
```

### Build cannot find Gradle

Open in Android Studio, or restore the Gradle wrapper scripts and wrapper JAR. The project expects Gradle 8.10 based on `gradle/wrapper/gradle-wrapper.properties`.

## License

This project is for demonstration and personal development use unless a separate license is added.
