package org.infinite.docs

import io.github.classgraph.ClassGraph
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*
class Minecraft(projectDir: Path) {

    @Serializable
    data class MinecraftMetadata(
        val className: String,
        val superClassName: String?,
        val interfaces: List<String>,
        val isInterface: Boolean,
        val fields: List<FieldMetadata>,
        val methods: List<MethodMetadata>,
    )

    @Serializable
    data class FieldMetadata(
        val name: String,
        val typeDescriptor: String,
        val isStatic: Boolean,
    )

    @Serializable
    data class MethodMetadata(
        val name: String,
        val descriptor: String, // Panamaの引数型・戻り値型の特定に必須
        val isStatic: Boolean,
    )

    private val outputDir: Path = projectDir.resolve("build/mappings/net/minecraft")

    // Rust側で扱いやすいよう、整形されたJSONを出力する設定
    private val jsonConfig = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    @OptIn(ExperimentalPathApi::class)
    fun generate() {
        if (outputDir.exists()) outputDir.deleteRecursively()
        outputDir.createDirectories()

        println("🚀 Minecraftクラスをスキャン中...")

        ClassGraph()
            .enableAllInfo()
            .acceptPackages("net.minecraft")
            .scan().use { scanResult ->
                scanResult.allClasses.forEach { classInfo ->
                    try {
                        val metadata = MinecraftMetadata(
                            className = classInfo.name,
                            superClassName = classInfo.superclass?.name, // 直接文字列で取得
                            interfaces = classInfo.interfaces.map { it.name },
                            isInterface = classInfo.isInterface,
                            fields = classInfo.fieldInfo.map { f ->
                                FieldMetadata(f.name, f.typeDescriptorStr ?: "Ljava/lang/Object;", f.isStatic)
                            },
                            methods = classInfo.methodInfo.map { m ->
                                // Panamaで必要なのは内部形式の Descriptor (例: (Lnet/minecraft/core/BlockPos;)I )
                                MethodMetadata(
                                    name = m.name,
                                    descriptor = m.typeDescriptorStr,
                                    isStatic = m.isStatic,
                                )
                            },
                        )
                        saveJson(metadata)
                    } catch (e: Exception) {
                        // 特定のクラスで失敗しても全体を止めない
                        System.err.println("⚠️ クラス ${classInfo.name} の解析に失敗しました: ${e.message}")
                    }
                }
            }
        println("✅ JSONメタデータの生成が完了しました: $outputDir")
    }

    private fun saveJson(metadata: MinecraftMetadata) {
        val relativePath = metadata.className.replace(".", "/") + ".json"
        val targetFile = outputDir.resolve(relativePath)
        targetFile.parent.createDirectories()

        val jsonString = jsonConfig.encodeToString(metadata)
        targetFile.writeText(jsonString, StandardCharsets.UTF_8)
    }
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val rootPath = args[0]
            val minecraft = Minecraft(Paths.get(rootPath))
            minecraft.generate()
        }
    }
}
