package org.infinite.libs.rust

import org.infinite.libs.log.LogSystem
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.*
import kotlin.io.path.exists

object LibInfiniteClient {
    private var hasLoaded: Boolean = false

    init {
        try {
            loadNativeLibrary()
        } catch (e: Exception) {
            LogSystem.error("Failed to load native library: ${e.message}")
        }
    }

    private fun getPlatformIdentifier(): String {
        val os = System.getProperty("os.name").lowercase(Locale.ENGLISH)
        val arch = System.getProperty("os.arch").lowercase(Locale.ENGLISH)
        val isArm64 = arch.contains("aarch64") || arch.contains("arm64") || arch.contains("armv8")

        return when {
            os.contains("win") -> if (isArm64) "windows-arm64" else "windows-x64"
            os.contains("mac") -> if (isArm64) "macos-arm64" else "macos-x64"
            os.contains("nix") || os.contains("nux") -> if (isArm64) "linux-arm64" else "linux-x64"
            else -> throw RuntimeException("Unsupported OS: $os")
        }
    }

    fun loadNativeLibrary() {
        if (hasLoaded) return

        val platformDir = getPlatformIdentifier()
        val libName = System.mapLibraryName("infinite_client")
        val resourcePath = "/natives/$platformDir/$libName"
        val destDir = Path.of("infinite", "lib")
        val destPath = destDir.resolve("${platformDir}_$libName")

        val resourceUrl = LibInfiniteClient::class.java.getResource(resourcePath)
            ?: throw RuntimeException("Native library not found in JAR: $resourcePath")

        resourceUrl.openStream().use { input ->
            if (shouldUpdate(input, destPath)) {
                Files.createDirectories(destDir)
                resourceUrl.openStream().use { freshInput ->
                    Files.copy(freshInput, destPath, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }

        val absolutePath = destPath.toAbsolutePath()
        // --- 修正の要：SymbolLookupの作成 ---
        // Java 25 FFM APIを使用して直接パスからライブラリをロードし、Lookupを作成する
        System.load(absolutePath.toString())
        hasLoaded = true
        LogSystem.info("Successfully loaded native library via FFM: $absolutePath")
    }

    private fun shouldUpdate(resourceStream: InputStream, destPath: Path): Boolean {
        if (!destPath.exists()) return true

        return try {
            val resHash = calculateHash(resourceStream)
            val fileHash = Files.newInputStream(destPath).use { calculateHash(it) }
            !resHash.contentEquals(fileHash)
        } catch (_: Exception) {
            true
        }
    }

    private fun calculateHash(input: InputStream): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            digest.update(buffer, 0, read)
        }
        return digest.digest()
    }
}
