# Infinite Addon Development Guide (English)

This is the official development guide for extending the `Infinite` MOD. `Infinite` features a modular architecture comprising **Features**, **Categories**, and **Themes**, allowing developers to seamlessly inject custom logic and visuals.

## 1. Setup (build.gradle)

To use the `Infinite` API in your project, add the following dependency. (Replace the repository URL and version with the latest official releases.)

```gradle
repositories {
    maven { url 'https://jitpack.io' } // Example
}

dependencies {
    modImplementation "org.infinite:infinite-client:VERSION"
}

```

## 2. Implementing the Entrypoint

Create a class that implements the `InfiniteAddon` interface.

```kotlin
package com.example.addon

import org.infinite.libs.addon.InfiniteAddon
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.categories.MovementCategory

class MyAwesomeAddon : InfiniteAddon {
    override fun onInitializeAddon(client: InfiniteClient) {
        // Implementation logic goes here
    }
}

```

Register your entrypoint in `fabric.mod.json`:

```json
{
  "entrypoints": {
    "infinite_addon": [
      "com.example.addon.MyAwesomeAddon"
    ]
  }
}

```

## 3. Core Extension Methods

### A. Adding a New Feature to an Existing Category

You can inject custom features into existing categories (e.g., Movement). Features registered this way are **automatically displayed in the GUI and saved to the Config**.

```kotlin
class MyFlightFeature : LocalFeature() {
    // Tick logic
    override fun onTick() {
        if (isEnabled) {
            // Flight logic here
        }
    }
}

// Registration
client.localFeatures.getCategory(MovementCategory::class)?.register(MyFlightFeature())

```

### B. Adding a New Category

Use this if you want to add an entirely new tab/category to the menu.

```kotlin
class MyUtilityCategory : LocalCategory() {
    val autoTool by feature(AutoToolFeature())
}

// Registration
client.localFeatures.register(MyUtilityCategory())

```

### C. Adding a New Theme

You can customize the UI appearance by adding a new theme.

```kotlin
class NeonTheme : Theme {
    override val id: String = "NeonTheme"
    // ... Define colors and styles
}

// Registration
client.themeManager.register(NeonTheme())

```

---

## 4. Design Rules & Lifecycle

`InfiniteClient` initializes in the following order:

1. **Addon Loading**: `onInitializeAddon` is called. Complete all `register` calls here.
2. **Config Loading**: Configuration values for all registered features (including addons) are loaded from files.
3. **Feature Initialization**: `onInitialized()` is called to enable and set up the features.

---

## 5. API Reference (Core Classes)

| Class Name | Description |
| --- | --- |
| `InfiniteClient` | The core of the MOD. Provides access to all managers. |
| `LocalFeature` | Base class for features with per-server/world settings. |
| `GlobalFeature` | Base class for features with global client settings. |
| `ThemeManager` | Manages UI themes. Use `register()` to add new themes. |

---

**Tips for Addon Developers:**

* By defining a `Property` (e.g., `BooleanProperty`) within a `Feature` class, a corresponding setting item will be **automatically generated in the GUI**.
* Don't forget to add translation keys to your `en_us.json` using the format: `infinite.features.<scope>.<category>.<feature>`.
