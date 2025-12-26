package org.infinite.libs.graphics.graphics2d

import org.infinite.libs.graphics.graphics2d.structs.RenderCommand
import java.util.LinkedList
import kotlin.math.atan2

class Graphics2DPrimitivesFill(
    private val commandQueue: LinkedList<RenderCommand>,
    private val getFillStyle: () -> Int, // Lambda to get current fillStyle from Graphics2D
) {
    private val fillStyle: Int get() = getFillStyle()

    fun fillRect(x: Float, y: Float, width: Float, height: Float) {
        commandQueue.add(RenderCommand.FillRect(x, y, width, height, fillStyle, fillStyle, fillStyle, fillStyle))
    }

    fun fillQuad(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        fillQuad(x0, y0, x1, y1, x2, y2, x3, y3, fillStyle, fillStyle, fillStyle, fillStyle)
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
        // 頂点データと色のペアをリスト化
        val vertices = mutableListOf(
            Vertex(x0, y0, col0),
            Vertex(x1, y1, col1),
            Vertex(x2, y2, col2),
            Vertex(x3, y3, col3),
        )

        // 重心を計算
        val centerX = vertices.map { it.x }.average().toFloat()
        val centerY = vertices.map { it.y }.average().toFloat()

        // 重心からの角度でソート (時計回り)
        // Math.atan2(y, x) は反時計回りなので、マイナスを付けてソート
        vertices.sortBy { atan2((it.y - centerY).toDouble(), (it.x - centerX).toDouble()) }

        commandQueue.add(
            RenderCommand.FillQuad(
                vertices[0].x, vertices[0].y,
                vertices[1].x, vertices[1].y,
                vertices[2].x, vertices[2].y,
                vertices[3].x, vertices[3].y,
                vertices[0].color, vertices[1].color, vertices[2].color, vertices[3].color,
            ),
        )
    }

    fun fillTriangle(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float) {
        fillTriangle(x0, y0, x1, y1, x2, y2, fillStyle, fillStyle, fillStyle)
    }

    fun fillTriangle(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        col0: Int,
        col1: Int,
        col2: Int,
    ) {
        // 外積 (Vector Cross Product) を利用して回転方向を判定
        // (x1-x0)*(y2-y0) - (y1-y0)*(x2-x0)
        val crossProduct = (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0)

        // crossProduct > 0 なら反時計回りなので、頂点1と2を入れ替えて時計回りにする
        if (crossProduct > 0) {
            addFillTriangle(x0, y0, x2, y2, x1, y1, col0, col2, col1)
        } else {
            addFillTriangle(x0, y0, x1, y1, x2, y2, col0, col1, col2)
        }
    }

    private fun addFillTriangle(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        c0: Int,
        c1: Int,
        c2: Int,
    ) {
        commandQueue.add(RenderCommand.FillTriangle(x0, y0, x1, y1, x2, y2, c0, c1, c2))
    }
}
