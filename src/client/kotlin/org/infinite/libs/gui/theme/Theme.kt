package org.infinite.libs.gui.theme

open class Theme(
    val name: String,
    val colors: ThemeColors,
    val icon: ThemeIcon?,
) {
    companion object {
        fun default(): Theme = Theme("default", ThemeColors(), null)
    }
}
