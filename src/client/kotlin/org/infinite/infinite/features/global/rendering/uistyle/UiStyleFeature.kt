package org.infinite.infinite.features.global.rendering.uistyle

import org.infinite.libs.core.features.feature.GlobalFeature
import org.infinite.libs.core.features.property.selection.EnumSelectionProperty
import org.infinite.libs.ui.style.UiStyle

class UiStyleFeature : GlobalFeature() {
    val style by property(EnumSelectionProperty(UiStyle.Simple))
}
