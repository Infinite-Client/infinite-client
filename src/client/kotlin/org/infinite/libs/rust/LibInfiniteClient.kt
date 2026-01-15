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
    init {
        loadNativeLibrary()
    }

    private var hasLoaded: Boolean = false

    fun loadNativeLibrary() {
        if (hasLoaded) return

        val os = System.getProperty("os.name").lowercase(Locale.ENGLISH)
        val arch = System.getProperty("os.arch").lowercase(Locale.ENGLISH)

        val platformDir = when {
            os.contains("win") -> "windows-x64"
            os.contains("nix") || os.contains("nux") -> "linux-x64"
            os.contains("mac") -> if (arch.contains("aarch64") || arch.contains("arm")) "macos-arm64" else "macos-x64"
            else -> throw RuntimeException("Unsupported OS: $os")
        }

        val libName = System.mapLibraryName("infinite_client")
        val resourcePath = "/natives/$platformDir/$libName"

        // 保存先ディレクトリ: .minecraft/infinite/lib/
        val destDir = Path.of("infinite", "lib")
        val destPath = destDir.resolve(libName)

        val resourceStream = LibInfiniteClient::class.java.getResourceAsStream(resourcePath)
            ?: throw RuntimeException("Native library not found in JAR: $resourcePath")

        // コピーが必要かチェック
        if (shouldUpdate(resourceStream, destPath)) {
            Files.createDirectories(destDir)
            // resourceStreamは一回読み込むと消費されるため、再度取得
            LibInfiniteClient::class.java.getResourceAsStream(resourcePath)!!.use { input ->
                Files.copy(input, destPath, StandardCopyOption.REPLACE_EXISTING)
            }
        }

        System.load(destPath.toAbsolutePath().toString())
        hasLoaded = true
    }

    /**
     * ファイルが存在しない、またはハッシュが一致しない場合にtrueを返す
     */
    private fun shouldUpdate(resourceStream: InputStream, destPath: Path): Boolean {
        if (!destPath.exists()) return true

        return try {
            val resHash = calculateHash(resourceStream)
            val fileHash = Files.newInputStream(destPath).use { calculateHash(it) }
            !resHash.contentEquals(fileHash)
        } catch (e: Exception) {
            LogSystem.error("Error while loading library: $e")
            true // エラー時は安全のために再コピー
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
