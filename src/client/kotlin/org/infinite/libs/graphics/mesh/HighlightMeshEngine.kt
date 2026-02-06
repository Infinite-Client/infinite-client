package org.infinite.libs.graphics.mesh

import net.minecraft.world.phys.Vec3
import org.infinite.libs.graphics.Graphics3D
import org.infinite.utils.alpha
import java.util.LinkedList

/**
 * ブロックのハイライト描画のためのメッシュエンジン。
 * キューブの座標と色を受け取り、線または面として描画コマンドを生成します。
 * 今後の最適化の余地を残しています。
 */
class HighlightMeshEngine(private val graphics3D: Graphics3D) {

    enum class HighlightMode {
        Lines,
        Faces,
    }

    // ハイライトするキューブの情報を保持するデータクラス
    data class HighlightCube(val min: Vec3, val max: Vec3, val color: Int)

    private val pendingCubes = LinkedList<HighlightCube>()

    /**
     * ハイライトしたい1x1x1のキューブを追加します。
     * @param pos キューブの最小座標 (X, Y, Z)
     * @param color キューブの色 (AARRGGBB)
     */
    fun addCube(pos: Vec3, color: Int) {
        // 座標はブロックの最小点と最大点を表現
        pendingCubes.add(HighlightCube(pos, pos.add(1.0, 1.0, 1.0), color))
    }

    /**
     * 現在保留中のキューブを描画します。
     * @param mode 描画モード (線または面)
     */
    fun render(mode: HighlightMode) {
        // ここに最適化ロジック（例: 面のマージ、重なり排除）を実装する
        // 現時点では単純に各キューブを描画

        for (cube in pendingCubes) {
            when (mode) {
                HighlightMode.Lines -> {
                    graphics3D.boxOptimized(cube.min, cube.max, cube.color, 2.0f, false)
                }

                HighlightMode.Faces -> {
                    val transparentColor = cube.color.alpha(60) // 面は半透明にする

                    // 6つの面をそれぞれ描画
                    // Bottom face (y=min.y)
                    graphics3D.rectangleFill(
                        Vec3(cube.min.x, cube.min.y, cube.min.z),
                        Vec3(cube.max.x, cube.min.y, cube.min.z),
                        Vec3(cube.max.x, cube.min.y, cube.max.z),
                        Vec3(cube.min.x, cube.min.y, cube.max.z),
                        transparentColor,
                        false,
                    )
                    // Top face (y=max.y)
                    graphics3D.rectangleFill(
                        Vec3(cube.min.x, cube.max.y, cube.min.z),
                        Vec3(cube.min.x, cube.max.y, cube.max.z),
                        Vec3(cube.max.x, cube.max.y, cube.max.z),
                        Vec3(cube.max.x, cube.max.y, cube.min.z),
                        transparentColor,
                        false,
                    )
                    // North face (z=min.z)
                    graphics3D.rectangleFill(
                        Vec3(cube.min.x, cube.min.y, cube.min.z),
                        Vec3(cube.min.x, cube.max.y, cube.min.z),
                        Vec3(cube.max.x, cube.max.y, cube.min.z),
                        Vec3(cube.max.x, cube.min.y, cube.min.z),
                        transparentColor,
                        false,
                    )
                    // South face (z=max.z)
                    graphics3D.rectangleFill(
                        Vec3(cube.min.x, cube.min.y, cube.max.z),
                        Vec3(cube.max.x, cube.min.y, cube.max.z),
                        Vec3(cube.max.x, cube.max.y, cube.max.z),
                        Vec3(cube.min.x, cube.max.y, cube.max.z),
                        transparentColor,
                        false,
                    )
                    // West face (x=min.x)
                    graphics3D.rectangleFill(
                        Vec3(cube.min.x, cube.min.y, cube.min.z),
                        Vec3(cube.min.x, cube.min.y, cube.max.z),
                        Vec3(cube.min.x, cube.max.y, cube.max.z),
                        Vec3(cube.min.x, cube.max.y, cube.min.z),
                        transparentColor,
                        false,
                    )
                    // East face (x=max.x)
                    graphics3D.rectangleFill(
                        Vec3(cube.max.x, cube.min.y, cube.min.z),
                        Vec3(cube.max.x, cube.max.y, cube.min.z),
                        Vec3(cube.max.x, cube.max.y, cube.max.z),
                        Vec3(cube.max.x, cube.min.y, cube.max.z),
                        transparentColor,
                        false,
                    )
                }
            }
        }
        pendingCubes.clear() // 描画後にキューをクリア
    }
}
