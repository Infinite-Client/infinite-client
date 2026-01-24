package org.infinite.infinite.features.local.rendering.ui

import org.infinite.libs.graphics.Graphics2D

interface IUiRenderer {
    fun render(graphics2D: Graphics2D): Unit
}
