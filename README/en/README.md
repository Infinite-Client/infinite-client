# Infinite Client

Next-generation utility mod for Minecraft Anarchy Servers.

**Infinite Client** is a high-performance Minecraft client mod built on the **Fabric Loader (1.21.11)**. It leverages the cutting-edge **Project Panama (Foreign Function & Memory API)** in Java 25 to bridge with **Rust** (via [Project Xross](./project-xross/)), delivering extreme performance and memory safety.

## 🚀 Key Features

- **⚡️ Native Power**: Core rendering and logic are implemented in Rust, maximizing throughput and minimizing overhead.
- **🛡️ Modern Tech Stack**: Utilizes Java 25 EA, Kotlin, and Rust for a robust and future-proof development environment.
- **🎨 NativeMeshEngine**: High-speed, native-side mesh generation for advanced visuals like ESP and world overlays.
- **🏗️ Project Xross Integration**: Seamless, thread-safe communication between JVM (Kotlin/Java) and Native (Rust).
- **🔧 Designed for Anarchy**: Built specifically for the unique challenges and requirements of Minecraft anarchy gameplay.

## 🛠️ Requirements

- **Minecraft**: 1.21.11
- **Fabric Loader**: 0.18.2 or later
- **Java**: **JDK 25** (Required for Project Panama / FFM API)
- **Dependencies**:
  - Fabric API
  - Fabric Language Kotlin

## 📦 Installation

For detailed instructions on setting up JDK 25 and configuring your launcher (especially for macOS users), please refer to the [INSTALLATION.md](./INSTALLATION.md).

### Quick Start
1. Install **JDK 25** (Early Access).
2. Download and install **Fabric Loader 1.21.11**.
3. Place `infinite-client.jar`, `fabric-api.jar`, and `fabric-language-kotlin.jar` in your `mods` folder.
4. Add the following JVM arguments to your launcher profile:
   ```bash
   --enable-native-access=ALL-UNNAMED -Dforeign.restricted=permit
   ```

## 🏗️ Architecture: Project Xross

The core functionality of Infinite Client is powered by **Project Xross**, a framework designed to eliminate the boundary between Rust and JVM. It replaces traditional JNI with the high-performance FFM API.

For more information, see the [Project Xross README](./project-xross/README.md).

## 📜 License

This project is licensed under the **MIT License**.
