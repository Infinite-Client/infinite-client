package org.infinite.infinite.features.local.level.highlight

import net.minecraft.client.multiplayer.ClientLevel
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.core.features.property.list.BlockAndColorListProperty
import org.infinite.libs.core.features.property.list.BlockAndColorListProperty.Companion.asNative
import org.infinite.libs.core.features.property.list.serializer.BlockAndColor
import org.infinite.libs.core.features.property.number.FloatProperty
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.libs.core.features.property.selection.EnumSelectionProperty
import java.lang.foreign.Arena
import java.lang.foreign.ValueLayout
import org.infinite.nativebind.features.local.level.highlight.BlockHighlightFeature as Native

class BlockHighlightFeature : LocalFeature() {
    override val featureType = FeatureLevel.Utils

    enum class RenderStyle { Lines, Faces, Both }
    enum class ViewFocus { None, Balanced, Strict }
    enum class Animation { None, Pulse, FadeIn }

    val blocksToHighlight by property(
        BlockAndColorListProperty(
            listOf(
                // --- 超重要・貴重ブロック ---
                BlockAndColor("minecraft:ancient_debris", 0x80583431.toInt()),
                BlockAndColor("minecraft:diamond_ore", 0x8000FFFF.toInt()),
                BlockAndColor("minecraft:deepslate_diamond_ore", 0x8000FFFF.toInt()),
                BlockAndColor("minecraft:diamond_block", 0x8000FFFF.toInt()),
                BlockAndColor("minecraft:emerald_ore", 0x8000FF00.toInt()),
                BlockAndColor("minecraft:deepslate_emerald_ore", 0x8000FF00.toInt()),
                BlockAndColor("minecraft:emerald_block", 0x8000FF00.toInt()),

                // --- 一般鉱石 ---
                BlockAndColor("minecraft:gold_ore", 0x80FFD700.toInt()),
                BlockAndColor("minecraft:deepslate_gold_ore", 0x80FFD700.toInt()),
                BlockAndColor("minecraft:nether_gold_ore", 0x80FFD700.toInt()),
                BlockAndColor("minecraft:gold_block", 0x80FFD700.toInt()),
                BlockAndColor("minecraft:iron_ore", 0x80D8AF93.toInt()),
                BlockAndColor("minecraft:deepslate_iron_ore", 0x80D8AF93.toInt()),
                BlockAndColor("minecraft:iron_block", 0x80C0C0C0.toInt()),
                BlockAndColor("minecraft:copper_ore", 0x80E77C5E.toInt()),
                BlockAndColor("minecraft:deepslate_copper_ore", 0x80E77C5E.toInt()),
                BlockAndColor("minecraft:coal_ore", 0x80333333.toInt()),
                BlockAndColor("minecraft:deepslate_coal_ore", 0x80333333.toInt()),
                BlockAndColor("minecraft:lapis_ore", 0x800000FF.toInt()),
                BlockAndColor("minecraft:deepslate_lapis_ore", 0x800000FF.toInt()),
                BlockAndColor("minecraft:redstone_ore", 0x80FF0000.toInt()),
                BlockAndColor("minecraft:deepslate_redstone_ore", 0x80FF0000.toInt()),
                BlockAndColor("minecraft:nether_quartz_ore", 0x80FFFFFF.toInt()),

                // --- ストレージ・ユーティリティ ---
                BlockAndColor("minecraft:chest", 0x80FFA500.toInt()),
                BlockAndColor("minecraft:trapped_chest", 0x80FF4500.toInt()),
                BlockAndColor("minecraft:ender_chest", 0x80800080.toInt()),
                BlockAndColor("minecraft:barrel", 0x808B4513.toInt()),
                BlockAndColor("minecraft:shulker_box", 0x80FF00FF.toInt()),
                BlockAndColor("minecraft:white_shulker_box", 0x80FFFFFF.toInt()),
                BlockAndColor("minecraft:orange_shulker_box", 0x80FFA500.toInt()),
                BlockAndColor("minecraft:magenta_shulker_box", 0x80FF00FF.toInt()),
                BlockAndColor("minecraft:light_blue_shulker_box", 0x80ADD8E6.toInt()),
                BlockAndColor("minecraft:yellow_shulker_box", 0x80FFFF00.toInt()),
                BlockAndColor("minecraft:lime_shulker_box", 0x8000FF00.toInt()),
                BlockAndColor("minecraft:pink_shulker_box", 0x80FFC0CB.toInt()),
                BlockAndColor("minecraft:gray_shulker_box", 0x80808080.toInt()),
                BlockAndColor("minecraft:light_gray_shulker_box", 0x80D3D3D3.toInt()),
                BlockAndColor("minecraft:cyan_shulker_box", 0x8000FFFF.toInt()),
                BlockAndColor("minecraft:purple_shulker_box", 0x80800080.toInt()),
                BlockAndColor("minecraft:blue_shulker_box", 0x800000FF.toInt()),
                BlockAndColor("minecraft:brown_shulker_box", 0x808B4513.toInt()),
                BlockAndColor("minecraft:green_shulker_box", 0x80008000.toInt()),
                BlockAndColor("minecraft:red_shulker_box", 0x80FF0000.toInt()),
                BlockAndColor("minecraft:black_shulker_box", 0x80000000.toInt()),

                // --- 構造物・重要地点 ---
                BlockAndColor("minecraft:spawner", 0x801E90FF.toInt()),
                BlockAndColor("minecraft:beacon", 0x8000FFFF.toInt()),
                BlockAndColor("minecraft:enchanting_table", 0x80FF00FF.toInt()),
                BlockAndColor("minecraft:tnt", 0x80FF0000.toInt()),
                BlockAndColor("minecraft:respawn_anchor", 0x80FFFF00.toInt()),
                BlockAndColor("minecraft:lodestone", 0x80C0C0C0.toInt()),

                // --- ポータル・レアブロック ---
                BlockAndColor("minecraft:nether_portal", 0x809932CC.toInt()),
                BlockAndColor("minecraft:end_portal_frame", 0x80006400.toInt()),
                BlockAndColor("minecraft:end_portal", 0x80000000.toInt()),
                BlockAndColor("minecraft:suspicious_sand", 0x80EEDC82.toInt()),
                BlockAndColor("minecraft:suspicious_gravel", 0x80808080.toInt()),
                BlockAndColor("minecraft:budding_amethyst", 0x80A45AEE.toInt()),
                BlockAndColor("minecraft:trial_spawner", 0x80FF8C00.toInt()),
                BlockAndColor("minecraft:vault", 0x80B8860B.toInt()),
            ),
        ),
    )

    val scanRange by property(IntProperty(12, 1, 32, " chunks"))
    val renderRange by property(IntProperty(128, 8, 512, " blocks"))
    val renderStyle by property(EnumSelectionProperty(RenderStyle.Lines))
    val maxDrawCount by property(IntProperty(20000, 1000, 100000, " elements"))
    val lineWidth by property(FloatProperty(1.5f, 0.1f, 5.0f, " px"))
    val viewFocus by property(EnumSelectionProperty(ViewFocus.Balanced))
    val animation by property(EnumSelectionProperty(Animation.Pulse))
    val maxY by property(IntProperty(64, -64, 320, " y"))
    val checkSurroundings by property(BooleanProperty(true))
    val skyLightThreshold by property(IntProperty(10, 0, 15, " level"))
    val playerExclusionRadius by property(IntProperty(10, 0, 64, " blocks"))
    private fun refreshNative() {
        Native.updateHighlightList(blocksToHighlight.value.asNative())
        Native.setScanRange(scanRange.value)
        Native.setRenderRange(renderRange.value)
        Native.setMaxDrawCount(maxDrawCount.value)
        Native.setLineWidthBits(lineWidth.value.toRawBits().toUInt())
        Native.setRenderStyle(renderStyle.value.ordinal.toUInt())
        Native.setViewFocus(viewFocus.value.ordinal.toUInt())
        Native.setAnimation(animation.value.ordinal.toUInt())
        Native.setMaxY(maxY.value)
        Native.setCheckSurroundings(checkSurroundings.value)
        Native.setSkyLightThreshold(skyLightThreshold.value)
        Native.setPlayerExclusionRadius(playerExclusionRadius.value)
    }

    init {
        // リスト更新
        blocksToHighlight.addListener { _, newValue ->
            Native.updateHighlightList(newValue.asNative())
        }

        // 数値・基本設定
        scanRange.addListener { _, newValue ->
            Native.setScanRange(newValue)
        }

        renderRange.addListener { _, newValue ->
            Native.setRenderRange(newValue)
        }

        maxDrawCount.addListener { _, newValue ->
            Native.setMaxDrawCount(newValue)
        }

        // Float (bitsとして送信)
        lineWidth.addListener { _, newValue ->
            Native.setLineWidthBits(newValue.toRawBits().toUInt())
        }

        // Enum (ordinalをUIntとして送信)
        renderStyle.addListener { _, newValue ->
            Native.setRenderStyle(newValue.ordinal.toUInt())
        }

        viewFocus.addListener { _, newValue ->
            Native.setViewFocus(newValue.ordinal.toUInt())
        }

        animation.addListener { _, newValue ->
            Native.setAnimation(newValue.ordinal.toUInt())
        }

        // 追加項目 (max_y, check_surroundings 等)
        maxY.addListener { _, newValue ->
            Native.setMaxY(newValue)
        }

        checkSurroundings.addListener { _, newValue ->
            Native.setCheckSurroundings(newValue)
        }

        skyLightThreshold.addListener { _, newValue ->
            Native.setSkyLightThreshold(newValue)
        }

        playerExclusionRadius.addListener { _, newValue ->
            Native.setPlayerExclusionRadius(newValue)
        }
    }

    override fun onConnected() {
        refreshNative()
    }

    private var currentScanIndex = 0
    override fun onEndTick() {
        val player = player ?: return
        val level = level ?: return

        // 1. 基本情報を通知 (クリーンアップおよびプレイヤー座標の同期)
        Native.onTick(player.x, player.y, player.z, level.minY, level.maxY)

        // 2. スキャン範囲の計算
        val scanRadius = scanRange.value
        val side = scanRadius * 2 + 1
        val center = player.chunkPosition()

        // 3. 1ティックに指定されたチャンク数分スキャンを実行
        // currentScanIndex は Feature 内で保持されている前提
        repeat(16) {
            val ox = (currentScanIndex % side) - scanRadius
            val oz = (currentScanIndex / side) - scanRadius

            scanChunk(level, center.x + ox, center.z + oz)

            currentScanIndex = (currentScanIndex + 1) % (side * side)
        }
        BlockHighlightRenderer.tick(this)
    }

    // Featureのクラス内で、オフヒープバッファを保持しておく
    private val arena = Arena.ofAuto()
    private val nativeScanBuffer = arena.allocate(ValueLayout.JAVA_INT, 4096)

    private fun scanChunk(level: ClientLevel, targetX: Int, targetZ: Int) {
        val chunk = level.chunkSource.getChunkNow(targetX, targetZ) ?: return

        chunk.sections.forEachIndexed { yOffset, section ->
            if (!section.hasOnlyAir()) {
                val container = section.states
                // 1. 安全に 4096 個の BlockState インデックスを取得
                // PalettedContainer.getAll(consumer) または手動展開を使用
                for (i in 0 until 4096) {
                    // (i & 15, (i shr 8) & 15, (i shr 4) & 15) は一般的な YZX 配列順
                    val state = container.get(i and 15, (i shr 8) and 15, (i shr 4) and 15)
                    val blockId = net.minecraft.world.level.block.Block.getId(state)

                    // オフヒープバッファに直接書き込む
                    nativeScanBuffer.setAtIndex(ValueLayout.JAVA_INT, i.toLong(), blockId)
                }
                // 2. Rust 側へ送信 (MemorySegment をそのまま渡す)
                Native.pushSectionData(
                    targetX,
                    yOffset + (level.minY shr 4),
                    targetZ,
                    nativeScanBuffer.toArray(ValueLayout.JAVA_INT),
                )
            }
        }
    }
//    override fun onLevelRendering(graphics3D: Graphics3D) {
//        BlockHighlightRenderer.render(graphics3D, this)
//    }

    override fun onEnabled() {
        Native.onEnabled()
    }

    override fun onDisabled() {
        Native.onDisabled()
        BlockHighlightRenderer.clear()
    }
}
