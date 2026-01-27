# Banyg Libraries Reference

## Overview

This document lists all libraries used in the Banyg Finance OS project, organized by category.

---

## Core Libraries

| Library | Version | Module(s) | Purpose |
|---------|---------|-----------|---------|
| **Kotlin** | 1.9.21 | All | Programming language |
| **Android Gradle Plugin** | 8.13.2 | All | Build system |
| **KSP** | 1.9.21-1.0.15 | All | Kotlin Symbol Processing |
| **Core KTX** | 1.12.0 | All | Kotlin extensions for Android |
| **Desugaring** | 2.0.4 | All | Java 8 API support on API 24+ |

---

## UI Layer (Jetpack Compose)

| Library | Version | Module(s) | Purpose |
|---------|---------|-----------|---------|
| **Compose BOM** | 2025.01.00 | All UI | Compose version management |
| **Material 3** | (via BOM) | All UI | Material Design components |
| **Material Icons Extended** | (via BOM) | All UI | Extended icon set |
| **Activity Compose** | 1.8.1 | app | Compose activity support |
| **Lifecycle Runtime Compose** | 2.6.2 | All UI | Lifecycle-aware Compose |
| **ViewModel Compose** | 2.6.2 | All UI | ViewModel integration |

---

## Architecture & Dependency Injection

| Library | Version | Module(s) | Purpose |
|---------|---------|-----------|---------|
| **Hilt** | 2.48.1 | All | Dependency injection |
| **Hilt Core** | 2.48.1 | core:domain | Pure Kotlin DI |
| **Hilt Navigation Compose** | 1.1.0 | Feature modules | Hilt + Navigation |
| **Navigation Compose** | 2.7.6 | Feature modules | Navigation component |

---

## Data Layer

| Library | Version | Module(s) | Purpose |
|---------|---------|-----------|---------|
| **Room** | 2.6.1 | core:data | SQLite ORM/database |
| **DataStore Preferences** | 1.0.0 | core:data | Key-value storage |
| **Kotlinx Coroutines** | 1.7.3 | All | Async programming |

---

## Newly Added Libraries (2025-01-28)

### CSV Import Support
| Library | Version | Module(s) | Purpose |
|---------|---------|-----------|---------|
| **Apache Commons CSV** | 1.10.0 | core:data | CSV parsing for bank imports |

**Usage Example:**
```kotlin
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord

// Parse CSV file
val parser = CSVParser.parse(file, Charset.defaultCharset(), CSVFormat.DEFAULT)
for (record in parser) {
    val date = record.get("Date")
    val amount = record.get("Amount")
    val description = record.get("Description")
    // Process transaction...
}
```

### Date/Time Handling
| Library | Version | Module(s) | Purpose |
|---------|---------|-----------|---------|
| **Kotlinx Datetime** | 0.5.0 | All | Multiplatform date/time |

**Usage Example:**
```kotlin
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Current timestamp
val now = Clock.System.now()

// Convert to local date
val localDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date

// Parse from string
val date = LocalDate.parse("2025-01-28")
```

### Animations
| Library | Version | Module(s) | Purpose |
|---------|---------|-----------|---------|
| **Lottie Compose** | 6.3.0 | core:ui | After Effects animations |
| **Accompanist SwipeRefresh** | 0.34.0 | core:ui | Pull-to-refresh |

**Usage Example - Lottie:**
```kotlin
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState

@Composable
fun LoadingAnimation() {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.loading)
    )
    val progress by animateLottieCompositionAsState(composition)
    
    LottieAnimation(
        composition = composition,
        progress = { progress }
    )
}
```

**Usage Example - SwipeRefresh:**
```kotlin
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun InboxScreen(viewModel: InboxViewModel) {
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = { viewModel.refresh() }
    ) {
        LazyColumn {
            // Transaction list
        }
    }
}
```

### Security
| Library | Version | Module(s) | Purpose |
|---------|---------|-----------|---------|
| **Security Crypto** | 1.1.0-alpha06 | core:data | Encrypted SharedPreferences |

**Usage Example:**
```kotlin
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// Create master key
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

// Create encrypted preferences
val sharedPreferences = EncryptedSharedPreferences.create(
    context,
    "secret_shared_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

// Store sensitive data securely
sharedPreferences.edit()
    .putString("api_key", "secret_value")
    .apply()
```

---

## Testing Libraries

| Library | Version | Module(s) | Purpose |
|---------|---------|-----------|---------|
| **JUnit 4** | 4.13.2 | All | Unit testing framework |
| **MockK** | 1.13.8 | All | Kotlin mocking |
| **Truth** | 1.2.0 | All | Assertion library |
| **Turbine** | 1.0.0 | All | Flow testing |
| **Coroutines Test** | 1.7.3 | All | Coroutine testing utilities |
| **AndroidX Test** | 1.1.5 | All | Android testing |
| **Espresso** | 3.5.1 | All | UI testing |
| **Hilt Android Testing** | 2.48.1 | All | Hilt testing |

---

## Migration Guide

### From Java 8 Time to Kotlinx Datetime

**Before (java.time):**
```kotlin
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

val now = Instant.now()
val localDate = LocalDate.now()
```

**After (kotlinx.datetime):**
```kotlin
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

val now = Clock.System.now()
val localDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
```

### Why Kotlinx Datetime?
- ✅ Multiplatform ready (future iOS/Web support)
- ✅ Null-safe by design
- ✅ More Kotlin-idiomatic API
- ✅ Smaller footprint than ThreeTenABP
- ✅ Official JetBrains support

---

## Future Library Additions

Consider adding these libraries as features expand:

### Charts & Visualization (Post-MVP)
- **Vico** (2.0.0) - Modern Compose charts
- **MPAndroidChart** (3.1.0) - Alternative chart library

### Biometric Security
- **Biometric** (1.1.0) - Fingerprint/Face ID

### Background Work
- **WorkManager** (2.9.0) - Recurring tasks, exports

### Image Loading (Future)
- **Coil** (2.5.0) - Receipt images, avatars

### Testing Enhancements
- **Paparazzi** (1.3.1) - Screenshot testing
- **Robolectric** (4.11.1) - JVM Android tests

---

## Version Catalog Location

All library versions are centralized in:
```
gradle/libs.versions.toml
```

To update a library version, modify the `[versions]` section and all modules will use the new version automatically.
