package org.infinite.infinite.ui.widget

import org.infinite.libs.core.features.Feature
import org.infinite.libs.ui.widgets.ToggleButton

class FeatureToggleButton(x: Int, y: Int, width: Int, height: Int, private val feature: Feature) : ToggleButton(x, y, width, height) {
    override var value: Boolean
        get() = feature.isEnabled()
        set(value) {
            if (value) {
                feature.enable()
            } else {
                feature.disable()
            }
        }
}
