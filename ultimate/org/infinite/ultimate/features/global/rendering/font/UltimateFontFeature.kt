package org.infinite.ultimate.features.global.rendering.font

import org.infinite.libs.core.features.feature.GlobalFeature
import org.infinite.libs.core.features.property.StringProperty

class UltimateFontFeature : GlobalFeature() {
    val regularFont by property(StringProperty("infinite_regular"))
    val boldFont by property(StringProperty("infinite_bold"))
    val italicFont by property(StringProperty("infinite_italic"))
    val boldItalicFont by property(StringProperty("infinite_bolditalic"))
}
