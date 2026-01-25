# Infinite Addon Development Guide

`Infinite` MODを拡張するための公式開発ガイドです。このMODは、独自の **Feature（機能）**、**Category（カテゴリ）**、**Theme（テーマ）** システムを備えており、アドオンからこれらを自由に追加・変更できます。

## 1. 準備 (build.gradle)

あなたのMODで `Infinite` のAPIを使用するために、依存関係を追加します（リポジトリのURLは公開環境に合わせて差し替えてください）。

```gradle
repositories {
    maven { url 'https://jitpack.io' } // 例
}

dependencies {
    modImplementation "org.infinite:infinite-client:VERSION"
}

```

## 2. エントリポイントの実装

`InfiniteAddon` インターフェースを実装するクラスを作成します。

```kotlin
package com.example.addon

import org.infinite.libs.addon.InfiniteAddon
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.categories.MovementCategory

class MyAwesomeAddon : InfiniteAddon {
    override fun onInitializeAddon(client: InfiniteClient) {
        // ここに拡張処理を記述します
    }
}

```

`fabric.mod.json` にエントリポイントを登録します。

```json
{
  "entrypoints": {
    "infinite_addon": [
      "com.example.addon.MyAwesomeAddon"
    ]
  }
}

```

## 3. 主要な拡張方法

### A. 新しい機能 (Feature) を既存カテゴリに追加する

既存のカテゴリ（例：移動系）に独自の機能を追加できます。追加された機能は、自動的に **GUIに表示され、Configに保存** されます。

```kotlin
class MyFlightFeature : LocalFeature() {
    // 毎Tickの処理
    override fun onTick() {
        if (isEnabled) {
            // 飛行ロジック
        }
    }
}

// 登録方法
client.localFeatures.getCategory(MovementCategory::class)?.register(MyFlightFeature())

```

### B. 新しいカテゴリ (Category) を追加する

全く新しいタブをメニューに追加したい場合に使用します。

```kotlin
class MyUtilityCategory : LocalCategory() {
    val autoTool by feature(AutoToolFeature())
}

// 登録方法
client.localFeatures.register(MyUtilityCategory())

```

### C. 新しいテーマ (Theme) を追加する

UIの見た目をカスタマイズするテーマを追加できます。

```kotlin
class NeonTheme : Theme {
    override val id: String = "NeonTheme"
    // ... 色やスタイルの定義
}

// 登録方法
client.themeManager.register(NeonTheme())

```

---

## 4. 設計のルールとライフサイクル

InfiniteClientは以下の順序で初期化されます：

1. **Addon Loading**: `onInitializeAddon` が呼ばれます。ここで `register` を完了させてください。
2. **Config Loading**: アドオンが登録した Feature の設定値がファイルから読み込まれます。
3. **Feature Initialization**: `onInitialized()` が呼ばれ、機能が有効化されます。

---

## 5. APIリファレンス (主要クラス)

| クラス名 | 説明 |
| --- | --- |
| `InfiniteClient` | MODの心臓部。各マネージャーへのアクセスを提供します。 |
| `LocalFeature` | サーバー/ワールドごとに設定を保持する機能の基底クラス。 |
| `GlobalFeature` | クライアント全体で共通の設定を保持する機能の基底クラス。 |
| `ThemeManager` | UIのテーマを管理。`register()` で新テーマを追加可能。 |

---

**アドオン開発者へのヒント:**

* `Feature` クラス内で `Property`（BooleanPropertyなど）を定義すると、自動的にGUIに設定項目が表示されます。
* 翻訳ファイル（`en_us.json`など）に `infinite.features.<scope>.<category>.<feature>` の形式でキーを追加することを忘れないでください。

