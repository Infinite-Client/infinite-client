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

    // Java 25のSymbolLookupなどで必要になる可能性があるため、ロード済みパスを保持
    var loadedLibraryPath: String? = null
        private set

    init {
        try {
            loadNativeLibrary()
        } catch (e: Exception) {
            LogSystem.error("Failed to load native library: ${e.message}")
            // 致命的なエラーとして報告するか、Modの機能を制限するフラグを立てる
        }
    }

    private fun getPlatformIdentifier(): String {
        val os = System.getProperty("os.name").lowercase(Locale.ENGLISH)
        val arch = System.getProperty("os.arch").lowercase(Locale.ENGLISH)

        // アーキテクチャの正規化
        val isArm64 = arch.contains("aarch64") || arch.contains("arm64") || arch.contains("armv8")
        val isX64 = arch.contains("amd64") || arch.contains("x86_64")

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

        // 保存先ディレクトリ: .minecraft/infinite/lib/
        val destDir = Path.of("infinite", "lib")
        val destPath = destDir.resolve("${platformDir}_$libName") // 同一フォルダでOS混在しても大丈夫なように名前を工夫

        val resourceUrl = LibInfiniteClient::class.java.getResource(resourcePath)
            ?: throw RuntimeException("Native library not found in JAR: $resourcePath")

        // コープが必要かチェック
        resourceUrl.openStream().use { input ->
            if (shouldUpdate(input, destPath)) {
                Files.createDirectories(destDir)
                resourceUrl.openStream().use { freshInput ->
                    Files.copy(freshInput, destPath, StandardCopyOption.REPLACE_EXISTING)
                }
                LogSystem.info("Native library updated: $destPath")
            }
        }

        val absolutePath = destPath.toAbsolutePath().toString()
        System.load(absolutePath)
        loadedLibraryPath = absolutePath
        hasLoaded = true
        LogSystem.info("Successfully loaded native library: $platformDir")
    }

    private fun shouldUpdate(resourceStream: InputStream, destPath: Path): Boolean {
        if (!destPath.exists()) return true

        return try {
            val resHash = calculateHash(resourceStream)
            val fileHash = Files.newInputStream(destPath).use { calculateHash(it) }
            !resHash.contentEquals(fileHash)
        } catch (e: Exception) {
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
