package org.infinite.infinite.features.global.rendering.uistyle

import org.infinite.infinite.ui.UiStyleRegistry
import org.infinite.libs.core.features.feature.GlobalFeature
import org.infinite.libs.core.features.property.SelectionProperty

class UIStyleFeature : GlobalFeature() {
    init {
        enabled.value = true
    }

    class UiStyleSelectionProperty :
        SelectionProperty<String>(
            UiStyleRegistry.DEFAULT_STYLE,
            emptyList(),
        ) {
        override val options: List<String>
            get() = UiStyleRegistry.availableStyles()
    }

    val style by property(UiStyleSelectionProperty())
}
