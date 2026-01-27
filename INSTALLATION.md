# Infinite Client - macOS Installation Guide

This guide covers installing **Infinite Client** (a Fabric mod for Minecraft 1.21.11) on macOS, including setting up JDK 25 and configuring the Minecraft launcher.

---

## Prerequisites

- **macOS** (Intel or Apple Silicon)
- **Minecraft Java Edition** with the official Minecraft Launcher
- The **Infinite Client .jar mod file** (e.g., `infinite-client-2.1.0+1.21.11.jar`)

---

## Step 1: Install JDK 25

Infinite Client requires **Java 25** due to its use of modern Java features like the Foreign Function & Memory API.

### Option A: Download Directly from Oracle/OpenJDK

1. Visit the [OpenJDK Early Access Downloads](https://jdk.java.net/25/)
2. Download the macOS version:
   - **Apple Silicon (M1/M2/M3)**: `macOS/AArch64`
   - **Intel Mac**: `macOS/x64`
3. Extract the downloaded `.tar.gz`:
   ```bash
   cd ~/Downloads
   tar -xzf openjdk-25_macos-*.tar.gz
   ```
4. Move to the Java directory:
   ```bash
   sudo mv jdk-25.jdk /Library/Java/JavaVirtualMachines/
   ```

### Option B: Install via Homebrew (Recommended)

```bash
# Install Homebrew if not already installed
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install OpenJDK 25 (Early Access)
brew install --cask openjdk@25
```

After installation via Homebrew, create a symlink:
```bash
sudo ln -sfn /opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-25.jdk
```

### Verify Installation

```bash
/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home/bin/java --version
```

You should see output like:
```
openjdk 25-ea ...
```

---

## Step 2: Install Fabric Loader

1. Download the **Fabric Installer** from [fabricmc.net/use/installer](https://fabricmc.net/use/installer/)
2. Run the installer:
   - Double-click `fabric-installer-x.x.x.jar`
   - Select **Client**
   - Choose Minecraft version: **1.21.11**
   - Click **Install**
3. Close the installer when complete

---

## Step 3: Install the Infinite Client Mod

1. Open Finder and press `Cmd + Shift + G`
2. Navigate to `~/Library/Application Support/minecraft`
3. Create a `mods` folder if it doesn't exist
4. Copy the **Infinite Client .jar file** into the `mods` folder:
   ```
   ~/Library/Application Support/minecraft/mods/infinite-client-2.1.0+1.21.11.jar
   ```

### Required Dependencies

Infinite Client requires **Fabric API** and **Fabric Language Kotlin**:

1. Download [Fabric API](https://modrinth.com/mod/fabric-api) (version 0.139.5+1.21.11 or compatible)
2. Download [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin) (version 1.13.7+kotlin.2.2.21 or compatible)
3. Place both `.jar` files in the `mods` folder

---

## Step 4: Configure Minecraft Launcher to Use JDK 25

### Official Minecraft Launcher

1. Open the **Minecraft Launcher**
2. Click on **Installations** tab
3. Find the **fabric-loader-1.21.11** installation and click the **⋯** (three dots) → **Edit**
4. Click **More Options** to expand advanced settings
5. In the **Java Executable** field, enter the path to JDK 25:
   ```
   /Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home/bin/java
   ```
   
   > **Note**: If you installed via Homebrew, use:
   > ```
   > /Library/Java/JavaVirtualMachines/openjdk-25.jdk/Contents/Home/bin/java
   > ```

6. In the **JVM Arguments** field, add these flags (required for native access):
   ```
   -Xmx4G --enable-native-access=ALL-UNNAMED -Dforeign.restricted=permit
   ```
   
   > Adjust `-Xmx4G` based on your available RAM (4GB minimum recommended)

7. Click **Save**

---

## Step 5: Launch Minecraft

1. In the Minecraft Launcher, select the **fabric-loader-1.21.11** profile
2. Click **Play**
3. The game should launch with Infinite Client loaded

---

## Troubleshooting

### "Wrong Java Version" Error
- Ensure you've set the correct Java Executable path in the launcher
- Verify JDK 25 is installed: run `/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home/bin/java --version`

### "Access Denied" or Native Library Errors
- Make sure `--enable-native-access=ALL-UNNAMED` is in your JVM arguments
- Add `-Dforeign.restricted=permit` if not already present

### Mod Not Loading
- Ensure all dependencies (Fabric API, Fabric Language Kotlin) are in the `mods` folder
- Check the Minecraft version matches (1.21.11)
- Check Fabric Loader version is 0.18.2 or later

### Finding Your Java Path

To list all installed Java versions:
```bash
/usr/libexec/java_home -V
```

To get the path to a specific version:
```bash
/usr/libexec/java_home -v 25
```

---

## File Locations Reference

| Item | Location |
|------|----------|
| Minecraft Folder | `~/Library/Application Support/minecraft` |
| Mods Folder | `~/Library/Application Support/minecraft/mods` |
| JDK 25 (Manual Install) | `/Library/Java/JavaVirtualMachines/jdk-25.jdk` |
| JDK 25 (Homebrew) | `/Library/Java/JavaVirtualMachines/openjdk-25.jdk` |

---

## Additional Notes

- **Memory**: Allocate at least 4GB RAM (`-Xmx4G`) for optimal performance
- **Apple Silicon**: The mod includes native ARM64 libraries for best performance on M1/M2/M3 Macs
- **Updates**: When updating the mod, simply replace the old `.jar` file with the new one in the `mods` folder
