package org.infinite.gui.theme

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

object CustomThemeLoader {
    private val logger = LoggerFactory.getLogger("InfiniteClient-ThemeLoader")
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class ThemeConfig(
        val name: String,
        @SerialName("backgroundColor") val backgroundColor: String? = null,
        @SerialName("foregroundColor") val foregroundColor: String? = null,
        @SerialName("primaryColor") val primaryColor: String? = null,
        @SerialName("secondaryColor") val secondaryColor: String? = null,
        val icon: String? = null,
    )

    fun load(directory: File): List<Theme> {
        if (!directory.exists() || !directory.isDirectory) return emptyList()
        val themes = mutableListOf<Theme>()
        directory
            .walk()
            .maxDepth(1)
            .filter { it.isFile && it.extension.equals("json", ignoreCase = true) }
            .forEach { file ->
                decodeTheme(file.name, file.readText())?.let { themes.add(it) }
            }
        return themes
    }

    fun loadBundled(): List<Theme> {
        val container = FabricLoader.getInstance().getModContainer("infinite")
        val resourcePath = container.flatMap { it.findPath("themes") }
        if (resourcePath.isEmpty) return emptyList()

        val themes = mutableListOf<Theme>()
        runCatching {
            Files.walk(resourcePath.get()).use { stream ->
                stream
                    .filter { Files.isRegularFile(it) && it.toString().endsWith(".json") }
                    .forEach { path ->
                        val name = path.fileName.toString()
                        val text = Files.readString(path)
                        decodeTheme(name, text)?.let { themes.add(it) }
                    }
            }
        }.onFailure { ex ->
            logger.warn("Failed to load bundled themes: ${ex.message}")
        }
        return themes
    }

    private fun parseColor(raw: String): Int {
        val cleaned = raw.trim().removePrefix("#")
        val value =
            when (cleaned.length) {
                6 -> cleaned.toLong(16) or 0xFF000000
                8 -> cleaned.toLong(16)
                else -> throw IllegalArgumentException("Color must be 6 or 8 hex digits: $raw")
        }
        return value.toInt()
    }

    private fun decodeTheme(
        sourceName: String,
        text: String,
    ): Theme? =
        runCatching {
            val cfg = json.decodeFromString<ThemeConfig>(text)
            val colors =
                object : ThemeColors() {
                    override val backgroundColor = cfg.backgroundColor?.let { parseColor(it) } ?: super.backgroundColor
                    override val foregroundColor = cfg.foregroundColor?.let { parseColor(it) } ?: super.foregroundColor
                    override val primaryColor = cfg.primaryColor?.let { parseColor(it) } ?: super.primaryColor
                    override val secondaryColor = cfg.secondaryColor?.let { parseColor(it) } ?: super.secondaryColor
                }
            val icon =
                cfg.icon?.let {
                    runCatching { ThemeIcon(Identifier.of(it)) }.getOrNull()
                }
            Theme(cfg.name, colors, icon)
        }.onFailure { ex ->
            logger.warn("Failed to load theme from $sourceName: ${ex.message}")
        }.getOrNull()
}
