package org.infinite.infinite.features.local.level.highlight

import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.list.BlockAndColorListProperty
import org.infinite.libs.core.features.property.list.serializer.BlockAndColor
import org.infinite.libs.core.features.property.number.FloatProperty
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.libs.core.features.property.selection.EnumSelectionProperty
import org.infinite.libs.graphics.Graphics3D

class CaveHighlightFeature : LocalFeature() {
    override val featureType = FeatureLevel.Utils

    enum class RenderStyle { Lines, Faces, Both }
    enum class ViewFocus { None, Balanced, Strict }
    enum class Animation { None, Pulse, FadeIn }

    val blocksToHighlight by property(
        BlockAndColorListProperty(
            listOf(
                BlockAndColor("minecraft:lava", 0x20FF4500),
                BlockAndColor("minecraft:water", 0x200045FF),
                BlockAndColor("minecraft:air", 0x20FFFFFF), // Cave air/void
            ),
        ),
    )

    val scanRange by property(IntProperty(12, 1, 32, " chunks"))
    val renderRange by property(IntProperty(128, 8, 512, " blocks"))
    val renderStyle by property(EnumSelectionProperty(RenderStyle.Faces))
    val maxDrawCount by property(IntProperty(20000, 1000, 100000, " elements"))

    val lineWidth by property(FloatProperty(1.5f, 0.1f, 5.0f, " px"))
    val viewFocus by property(EnumSelectionProperty(ViewFocus.Balanced))
    val animation by property(EnumSelectionProperty(Animation.Pulse))

    // Cave specific settings
    val maxY by property(IntProperty(64, -64, 320, " Y"))
    val checkSurroundings by property(org.infinite.libs.core.features.property.BooleanProperty(true))
    val skyLightThreshold by property(IntProperty(0, 0, 15, " level"))
    val playerExclusionRadius by property(IntProperty(5, 0, 20, " blocks"))

    override fun onEndTick() {
        CaveHighlightRenderer.tick(this)
    }

    override fun onLevelRendering(graphics3D: Graphics3D) {
        CaveHighlightRenderer.render(graphics3D, this)
    }

    override fun onDisabled() {
        CaveHighlightRenderer.clear()
    }
}
