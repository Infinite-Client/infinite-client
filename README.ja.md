# Infinite Client (日本語)

Minecraft Anarchy サーバー向けの次世代ユーティリティ mod。

**Infinite Client** は、**Fabric Loader (1.21.11)** 上で動作する高性能な Minecraft クライアント mod です。Java 25 で標準化される **Project Panama (Foreign Function & Memory API)** を活用し、[Project Xross](./project-xross/README.ja.md) を介して **Rust** と連携することで、圧倒的なパフォーマンスとメモリ安全性を実現しています。

## 🚀 主な特徴

- **⚡️ ネイティブパワー**: レンダリングやロジックの核となる部分を Rust で実装し、スループットを最大化。
- **🛡️ モダンな技術スタック**: Java 25 EA、Kotlin、Rust を採用。
- **🎨 NativeMeshEngine**: ESP やワールドオーバーレイなどの高度な描画を高速に行うための、ネイティブ側メッシュエンジン。
- **🏗️ Project Xross 統合**: JVM (Kotlin/Java) と Native (Rust) 間のシームレスでスレッドセーフな通信。
- **🔧 Anarchy 特化**: Anarchy サーバーでのプレイにおける独自の課題や要求に合わせて設計。

## 🛠️ 必要条件

- **Minecraft**: 1.21.11
- **Fabric Loader**: 0.18.2 以上
- **Java**: **JDK 25** (Project Panama / FFM API のために必須)
- **依存関係**:
  - Fabric API
  - Fabric Language Kotlin

## 📦 インストール方法

JDK 25 のセットアップやランチャーの設定（特に macOS ユーザー向け）に関する詳細な手順は、[INSTALLATION.md](./INSTALLATION.md) を参照してください。

### クイックスタート
1. **JDK 25** (Early Access) をインストール。
2. **Fabric Loader 1.21.11** をインストール。
3. `infinite-client.jar`, `fabric-api.jar`, `fabric-language-kotlin.jar` を `mods` フォルダに配置。
4. ランチャーの起動引数に以下を追加：
   ```bash
   --enable-native-access=ALL-UNNAMED -Dforeign.restricted=permit
   ```

## 🏗️ アーキテクチャ: Project Xross

Infinite Client のコア機能は、Rust と JVM の境界をなくすために設計されたフレームワーク **Project Xross** によって支えられています。

詳細は [Project Xross README](./project-xross/README.ja.md) をご覧ください。

## 📜 ライセンス

このプロジェクトは **MIT ライセンス** の下で公開されています。
