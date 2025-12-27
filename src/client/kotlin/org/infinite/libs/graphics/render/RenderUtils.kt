package org.infinite.libs.graphics.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.util.ARGB
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.infinite.utils.rendering.Line
import org.infinite.utils.rendering.Quad
import org.joml.Vector3f

object RenderUtils {
    data class ColorBox(
        val color: Int,
        val box: AABB,
    )

    fun renderSolidBox(
        matrix: PoseStack,
        box: AABB,
        color: Int,
        buffer: VertexConsumer,
    ) {
        val entry = matrix.last()
        val x1 = box.minX.toFloat()
        val y1 = box.minY.toFloat()
        val z1 = box.minZ.toFloat()
        val x2 = box.maxX.toFloat()
        val y2 = box.maxY.toFloat()
        val z2 = box.maxZ.toFloat()

        // Y- face (Bottom) - 法線: (0, -1, 0)
        buffer.quad(entry, 0f, -1f, 0f, color, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2)

        // Y+ face (Top) - 法線: (0, 1, 0)
        buffer.quad(entry, 0f, 1f, 0f, color, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1)

        // Z- face (North) - 法線: (0, 0, -1)
        buffer.quad(entry, 0f, 0f, -1f, color, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1)

        // X+ face (East) - 法線: (1, 0, 0)
        buffer.quad(entry, 1f, 0f, 0f, color, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2)

        // Z+ face (South) - 法線: (0, 0, 1)
        buffer.quad(entry, 0f, 0f, 1f, color, x1, y1, z2, x1, y2, z2, x2, y2, z2, x2, y1, z2)
        // X- face (West) - 法線: (-1, 0, 0)
        buffer.quad(entry, -1f, 0f, 0f, color, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1)
    }

// --------------------------------------------------------------------------------------------------

    // ヘルパー関数 (RenderUtilsオブジェクト内に定義すると便利)
    fun VertexConsumer.quad(
        entry: PoseStack.Pose,
        nx: Float,
        ny: Float,
        nz: Float, // 法線
        color: Int,
        // 4つの頂点の座標 (x, y, z)
        x1: Float,
        y1: Float,
        z1: Float,
        x2: Float,
        y2: Float,
        z2: Float,
        x3: Float,
        y3: Float,
        z3: Float,
        x4: Float,
        y4: Float,
        z4: Float,
    ) {
        // 頂点情報
        // 三角形 1 (V1, V2, V3)
        addVertex(entry, x1, y1, z1).setColor(color).setNormal(entry, nx, ny, nz)
        addVertex(entry, x2, y2, z2).setColor(color).setNormal(entry, nx, ny, nz)
        addVertex(entry, x3, y3, z3).setColor(color).setNormal(entry, nx, ny, nz)
        // 三角形 2 (V3, V4, V1)
        addVertex(entry, x3, y3, z3).setColor(color).setNormal(entry, nx, ny, nz)
        addVertex(entry, x4, y4, z4).setColor(color).setNormal(entry, nx, ny, nz)
        addVertex(entry, x1, y1, z1).setColor(color).setNormal(entry, nx, ny, nz)
    }

    /**
     * 単一のBoxのアウトラインをVertexConsumerに書き込みます。
     * この関数はバッファをフラッシュしません。
     */
    fun renderLinedBox(
        matrix: PoseStack,
        box: AABB,
        color: Int,
        buffer: VertexConsumer,
    ) {
        val entry: PoseStack.Pose = matrix.last()
        val x1 = box.minX.toFloat()
        val y1 = box.minY.toFloat()
        val z1 = box.minZ.toFloat()
        val x2 = box.maxX.toFloat()
        val y2 = box.maxY.toFloat()
        val z2 = box.maxZ.toFloat()
        buffer.addVertex(entry, x1, y1, z1).setColor(color).setNormal(entry, 1f, 0f, 0f) //  を追加
        buffer.addVertex(entry, x2, y1, z1).setColor(color).setNormal(entry, 1f, 0f, 0f)
        buffer.addVertex(entry, x1, y1, z1).setColor(color).setNormal(entry, 0f, 0f, 1f)
        buffer.addVertex(entry, x1, y1, z2).setColor(color).setNormal(entry, 0f, 0f, 1f)
        buffer.addVertex(entry, x2, y1, z1).setColor(color).setNormal(entry, 0f, 0f, 1f)
        buffer.addVertex(entry, x2, y1, z2).setColor(color).setNormal(entry, 0f, 0f, 1f)
        buffer.addVertex(entry, x1, y1, z2).setColor(color).setNormal(entry, 1f, 0f, 0f)
        buffer.addVertex(entry, x2, y1, z2).setColor(color).setNormal(entry, 1f, 0f, 0f)

        // top lines
        buffer.addVertex(entry, x1, y2, z1).setColor(color).setNormal(entry, 1f, 0f, 0f)
        buffer.addVertex(entry, x2, y2, z1).setColor(color).setNormal(entry, 1f, 0f, 0f)
        buffer.addVertex(entry, x1, y2, z1).setColor(color).setNormal(entry, 0f, 0f, 1f)
        buffer.addVertex(entry, x1, y2, z2).setColor(color).setNormal(entry, 0f, 0f, 1f)
        buffer.addVertex(entry, x2, y2, z1).setColor(color).setNormal(entry, 0f, 0f, 1f)
        buffer.addVertex(entry, x2, y2, z2).setColor(color).setNormal(entry, 0f, 0f, 1f)
        buffer.addVertex(entry, x1, y2, z2).setColor(color).setNormal(entry, 1f, 0f, 0f)
        buffer.addVertex(entry, x2, y2, z2).setColor(color).setNormal(entry, 1f, 0f, 0f)

        // side lines
        buffer.addVertex(entry, x1, y1, z1).setColor(color).setNormal(entry, 0f, 1f, 0f)
        buffer.addVertex(entry, x1, y2, z1).setColor(color).setNormal(entry, 0f, 1f, 0f)
        buffer.addVertex(entry, x2, y1, z1).setColor(color).setNormal(entry, 0f, 1f, 0f)
        buffer.addVertex(entry, x2, y2, z1).setColor(color).setNormal(entry, 0f, 1f, 0f)
        buffer.addVertex(entry, x1, y1, z2).setColor(color).setNormal(entry, 0f, 1f, 0f)
        buffer.addVertex(entry, x1, y2, z2).setColor(color).setNormal(entry, 0f, 1f, 0f)
        buffer.addVertex(entry, x2, y1, z2).setColor(color).setNormal(entry, 0f, 1f, 0f)
        buffer.addVertex(entry, x2, y2, z2).setColor(color).setNormal(entry, 0f, 1f, 0f)
    }

    fun renderSolidColorBoxes(
        matrix: PoseStack,
        boxes: List<ColorBox>,
        buffer: VertexConsumer,
    ) {
        val camPos = cameraPos().reverse()
        boxes.forEach {
            renderSolidBox(matrix, it.box.move(camPos), it.color, buffer)
        }
    }

    fun renderSolidQuads(
        matrix: PoseStack,
        quads: List<Quad>,
        buffer: VertexConsumer,
    ) {
        val camPos = cameraPos().reverse()
        val entry = matrix.last()
        quads.forEach { quad ->
            // カメラ位置オフセットを適用
            val v1 = quad.vertex1.add(camPos)
            val v2 = quad.vertex2.add(camPos)
            val v3 = quad.vertex3.add(camPos)
            val v4 = quad.vertex4.add(camPos)

            buffer.quad(
                entry,
                quad.normal.x,
                quad.normal.y,
                quad.normal.z,
                quad.color,
                v1.x.toFloat(),
                v1.y.toFloat(),
                v1.z.toFloat(),
                v2.x.toFloat(),
                v2.y.toFloat(),
                v2.z.toFloat(),
                v3.x.toFloat(),
                v3.y.toFloat(),
                v3.z.toFloat(),
                v4.x.toFloat(),
                v4.y.toFloat(),
                v4.z.toFloat(),
            )
        }
    }

    fun renderLinedLines(
        matrix: PoseStack,
        lines: List<Line>,
        buffer: VertexConsumer,
    ) {
        val camPos = cameraPos().reverse()
        val entry = matrix.last()
        lines.forEach { line ->
            val s = line.start.add(camPos)
            val e = line.end.add(camPos)

            val start3f = Vector3f(s.x.toFloat(), s.y.toFloat(), s.z.toFloat())
            val end3f = Vector3f(e.x.toFloat(), e.y.toFloat(), e.z.toFloat())
            val normal = Vector3f(end3f).sub(start3f).normalize()

            buffer.addVertex(entry, start3f).setColor(line.color).setNormal(entry, normal.x, normal.y, normal.z)
            buffer.addVertex(entry, end3f).setColor(line.color).setNormal(entry, normal.x, normal.y, normal.z)
        }
    }

    /**
     * 複数の LinedColorBox のアウトラインを描画します。バッファのフラッシュはしません。
     *
     * @param buffer 描画先の VertexConsumer (Graphics3Dから取得する)
     */
    fun renderLinedColorBoxes(
        matrix: PoseStack,
        boxes: List<ColorBox>,
        buffer: VertexConsumer, // Graphics3Dから渡される
    ) {
        val camPos = cameraPos().reverse()
        boxes.forEach { renderLinedBox(matrix, it.box.move(camPos), it.color, buffer) }
    }

    // 距離によるグラデーション色の計算 (変更なし)
    private const val MAX_COLOR_DISTANCE = 64.0

    fun distColor(distance: Double): Int {
        val clampDist = Mth.clamp(distance, 0.0, MAX_COLOR_DISTANCE)
        val f = (clampDist / MAX_COLOR_DISTANCE).toFloat()

        val startColor =
            org.infinite.InfiniteClient
                .theme()
                .colors.errorColor // Red for close
        val endColor =
            org.infinite.InfiniteClient
                .theme()
                .colors.greenAccentColor // Green for far

        val r =
            Mth.lerp(
                f,
                ARGB.red(startColor).toFloat() / 255f,
                ARGB.red(endColor).toFloat() / 255f,
            )
        val g =
            Mth.lerp(
                f,
                ARGB.green(startColor).toFloat() / 255f,
                ARGB.green(endColor).toFloat() / 255f,
            )
        val b =
            Mth.lerp(
                f,
                ARGB.blue(startColor).toFloat() / 255f,
                ARGB.blue(endColor).toFloat() / 255f,
            )

        return (
            0xFF000000.toInt() or
                ((r * 255).toInt() shl 16) or
                ((g * 255).toInt() shl 8) or
                (b * 255).toInt()
            )
    }

    /**
     * 2点間に直線を描画する (ワールド座標基準)。バッファのフラッシュはしません。
     */
    fun renderLine(
        matrix: PoseStack,
        start: Vec3,
        end: Vec3,
        color: Int,
        buffer: VertexConsumer,
    ) {
        val camPos = cameraPos().reverse()
        val entry: PoseStack.Pose = matrix.last()
        val s = start.add(camPos)
        val e = end.add(camPos)

        // 始点と終点の座標
        val start3f = Vector3f(s.x.toFloat(), s.y.toFloat(), s.z.toFloat())
        val end3f = Vector3f(e.x.toFloat(), e.y.toFloat(), e.z.toFloat())
        // 🚨 修正: 正しい法線ベクトルを計算
        // 法線は線分の方向
        val normal = Vector3f(end3f).sub(start3f).normalize()
        // 頂点情報と法線の書き込み
        buffer.addVertex(entry, start3f).setColor(color).setNormal(entry, normal.x, normal.y, normal.z)
        buffer.addVertex(entry, end3f).setColor(color).setNormal(entry, normal.x, normal.y, normal.z)
    }

    fun cameraPos(): Vec3 = Minecraft.getInstance().blockEntityRenderDispatcher?.cameraPos ?: Vec3.ZERO
}
