package org.infinite.infinite.features.local.level.highlight

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.Level
import org.infinite.nativebind.BlockHighlight
import org.infinite.utils.rendering.BlockMesh
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

object BlockHighlightRenderer {
    private val blockPositions = ConcurrentHashMap<SectionPos, MutableMap<BlockPos, Int>>()
    private val meshCache = ConcurrentHashMap<SectionPos, BlockMesh>()
    private val sectionFirstSeen = ConcurrentHashMap<SectionPos, Long>()

    private val native = BlockHighlight()
    private var handlerId = -1

    private var currentScanIndex = 0
    private var lastSettingsHash = 0
    private val colorCache = mutableMapOf<String, Int>()

    private fun getColorForBlock(id: Identifier): Int? = colorCache[id.toString()]

    fun tick(feature: BlockHighlightFeature) {
        if (handlerId == -1) {
            handlerId = native.registerToMgpu3d().toInt()
        }

        val mc = Minecraft.getInstance()
        val world = mc.level ?: return
        val player = mc.player ?: return
        val settingsHash = feature.blocksToHighlight.value.hashCode()
        if (settingsHash != lastSettingsHash) {
            clear()
            lastSettingsHash = settingsHash
            feature.blocksToHighlight.value.forEach {
                colorCache[it.blockId] = it.color
            }
        }

        val scanRadius = feature.scanRange.value
        val side = scanRadius * 2 + 1
        val center = player.chunkPosition()

        // Scan multiple chunks per tick to cover the area faster
        val chunksToScanPerTick = 16
        repeat(chunksToScanPerTick) {
            val ox = (currentScanIndex % side) - scanRadius
            val oz = (currentScanIndex / side) - scanRadius
            scanChunk(world, center.x + ox, center.z + oz)
            currentScanIndex = (currentScanIndex + 1) % (side * side)
        }

        if (mc.player!!.tickCount % 100 == 0) {
            val toRemove = blockPositions.keys.filter { abs(it.x - center.x) > scanRadius + 2 || abs(it.z - center.z) > scanRadius + 2 }
            toRemove.forEach {
                blockPositions.remove(it)
                meshCache.remove(it)
                sectionFirstSeen.remove(it)
            }
        }

        // --- 同期: Rust 側へデータを送る ---
        syncWithRust(feature)
    }

    private fun syncWithRust(feature: BlockHighlightFeature) {
        native.clear()
        native.lineWidth = feature.lineWidth.value
        native.renderStyle = feature.renderStyle.value.ordinal

        val positions = IntArray(blockPositions.values.sumOf { it.size } * 3)
        val colors = IntArray(blockPositions.values.sumOf { it.size })
        var i = 0
        var j = 0
        blockPositions.values.forEach { map ->
            map.forEach { (pos, color) ->
                positions[i++] = pos.x
                positions[i++] = pos.y
                positions[i++] = pos.z
                colors[j++] = color
            }
        }
        native.addBlocks(positions, colors)

        native.generate()
    }

    private fun scanChunk(world: Level, cx: Int, cz: Int) {
        if (!world.chunkSource.hasChunk(cx, cz)) return
        val chunk = world.getChunk(cx, cz)

        chunk.sections.forEachIndexed { index, section ->
            val sectionY = world.minSectionY + index
            val sp = SectionPos.of(cx, sectionY, cz)
            val newBlocks = mutableMapOf<BlockPos, Int>()
            val minY = sectionY shl 4
            for (y in 0..15) {
                for (z in 0..15) {
                    for (x in 0..15) {
                        val state = section.getBlockState(x, y, z)
                        if (state.isAir) continue
                        val color = getColorForBlock(BuiltInRegistries.BLOCK.getKey(state.block)) ?: continue
                        newBlocks[BlockPos((cx shl 4) + x, minY + y, (cz shl 4) + z)] = color
                    }
                }
            }

            if (blockPositions[sp] != newBlocks) {
                if (newBlocks.isEmpty()) {
                    blockPositions.remove(sp)
                    meshCache.remove(sp)
                    sectionFirstSeen.remove(sp)
                } else {
                    blockPositions[sp] = newBlocks
                    meshCache.remove(sp)
                    if (!sectionFirstSeen.containsKey(sp)) sectionFirstSeen[sp] = System.currentTimeMillis()
                }
            }
        }
    }

    fun clear() {
        blockPositions.clear()
        meshCache.clear()
        sectionFirstSeen.clear()
        currentScanIndex = 0
    }
}
