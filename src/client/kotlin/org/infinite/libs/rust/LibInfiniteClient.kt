package org.infinite.libs.rust

import java.lang.foreign.*
import java.lang.invoke.MethodHandle
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

object LibInfiniteClient {
    private val calculateDistanceHandle: MethodHandle

    init {
        loadNativeLibrary()

        // 2. Project Panama で関数をルックアップ
        val linker = Linker.nativeLinker()
        // System.loadでロードしたライブラリは loaderLookup で検索可能
        val lookup = SymbolLookup.loaderLookup()

        val symbol = lookup.find("calculate_distance")
            .orElseThrow { RuntimeException("Failed to find symbol: calculate_distance") }

        val descriptor = FunctionDescriptor.of(
            ValueLayout.JAVA_FLOAT, // 戻り値
            ValueLayout.JAVA_FLOAT, // 引数 x
            ValueLayout.JAVA_FLOAT, // 引数 y
            ValueLayout.JAVA_FLOAT, // 引数 z
        )

        calculateDistanceHandle = linker.downcallHandle(symbol, descriptor)
    }
    private var hasLoaded: Boolean = false
    fun loadNativeLibrary() {
        if (hasLoaded) return
        hasLoaded = true
        val os = System.getProperty("os.name").lowercase(Locale.ENGLISH)
        val arch = System.getProperty("os.arch").lowercase(Locale.ENGLISH)

        // build.gradle.kts の into("natives/$folder") と一致させる
        val platformDir = when {
            os.contains("win") -> "windows-x64"
            os.contains("nix") || os.contains("nux") -> "linux-x64"
            os.contains("mac") -> {
                if (arch.contains("aarch64") || arch.contains("arm")) {
                    "macos-arm64"
                } else {
                    "macos-x64"
                }
            }
            else -> throw RuntimeException("Unsupported OS: $os")
        }

        // Cargo.toml の [lib] name = "infinite_client" に基づく
        val libName = System.mapLibraryName("infinite_client")
        val resourcePath = "/natives/$platformDir/$libName"

        val resourceStream = LibInfiniteClient::class.java.getResourceAsStream(resourcePath)
            ?: throw RuntimeException("Native library not found at $resourcePath")

        // 一時ファイルの作成 (Windows用に拡張子を適切に設定)
        val suffix = when {
            os.contains("win") -> ".dll"
            os.contains("mac") -> ".dylib"
            else -> ".so"
        }

        val tempFile = Files.createTempFile("infinite_client_", suffix)
        tempFile.toFile().deleteOnExit()

        resourceStream.use { input ->
            Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING)
        }

        System.load(tempFile.toAbsolutePath().toString())
    }

    fun calculateDistance(x: Float, y: Float, z: Float): Float {
        return try {
            calculateDistanceHandle.invokeExact(x, y, z) as Float
        } catch (e: Throwable) {
            throw RuntimeException("Error calling native function calculate_distance", e)
        }
    }
}
