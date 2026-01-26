package org.infinite.libs.graphics.graphics2d

import org.infinite.libs.graphics.graphics2d.structs.RenderCommand2DProvider
import kotlin.math.atan2

class Graphics2DPrimitivesFill(
    private val provider: RenderCommand2DProvider,
    private val getFillStyle: () -> Int,
) {
    private val fillStyle: Int get() = getFillStyle()

    // --- 再利用のための一時バッファ群 (Alloc回避) ---
    private val tempVertices = ArrayList<Vertex>(16)

    data class Vertex(val x: Float, val y: Float, val color: Int)

    fun fillRect(x: Float, y: Float, width: Float, height: Float) {
        provider.getFillRect().set(x, y, width, height, fillStyle, fillStyle, fillStyle, fillStyle)
    }

    fun fillRect(x: Float, y: Float, width: Float, height: Float, color0: Int, color1: Int, color2: Int, color3: Int) {
        provider.getFillRect()
            .set(x, y, width, height, color0, color1, color2, color3)
    }

    fun fillQuad(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        provider.getFillQuad().set(x0, y0, x1, y1, x2, y2, x3, y3, fillStyle, fillStyle, fillStyle, fillStyle)
    }

    fun fillQuad(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        col0: Int,
        col1: Int,
        col2: Int,
        col3: Int,
    ) {
        tempVertices.clear()
        tempVertices.add(Vertex(x0, y0, col0))
        tempVertices.add(Vertex(x1, y1, col1))
        tempVertices.add(Vertex(x2, y2, col2))
        tempVertices.add(Vertex(x3, y3, col3))

        val cx = (x0 + x1 + x2 + x3) / 4f
        val cy = (y0 + y1 + y2 + y3) / 4f

        tempVertices.sortBy { atan2((it.y - cy).toDouble(), (it.x - cx).toDouble()) }

        provider.getFillQuad().set(
            tempVertices[0].x, tempVertices[0].y,
            tempVertices[1].x, tempVertices[1].y,
            tempVertices[2].x, tempVertices[2].y,
            tempVertices[3].x, tempVertices[3].y,
            tempVertices[0].color, tempVertices[1].color, tempVertices[2].color, tempVertices[3].color,
        )
    }

    fun fillTriangle(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, c0: Int, c1: Int, c2: Int) {
        val crossProduct = (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0)
        if (crossProduct > 0) {
            provider.getFillTriangle().set(x0, y0, x2, y2, x1, y1, c0, c2, c1)
        } else {
            provider.getFillTriangle().set(x0, y0, x1, y1, x2, y2, c0, c1, c2)
        }
    }
}
