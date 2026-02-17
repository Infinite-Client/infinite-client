package org.infinite.libs.graphics.mesh

import net.minecraft.world.phys.Vec3
import org.infinite.libs.graphics.Graphics3D
import org.infinite.utils.alpha

/**
 * 汎用的な3Dメッシュ構築エンジン。
 * 内部でネイティブ実装の InfiniteMesh を使用し、高効率なレンダリングを提供します。
 */
class MeshEngine : AutoCloseable {

    enum class RenderMode {
        Lines,
        Faces,
    }

    private val mesh = InfiniteMesh()

    /**
     * 指定された位置に1x1x1のキューブを追加します。
     * @param pos 最小座標 (X, Y, Z)
     * @param color キューブの色 (ARGB)
     * @param mode 描画モード (線または面)
     */
    fun addCube(pos: Vec3, color: Int, mode: RenderMode = RenderMode.Lines) {
        addBox(pos, Vec3(1.0, 1.0, 1.0), color, mode)
    }

    /**
     * 任意のサイズのボックスを追加します。
     * @param pos 最小座標
     * @param size サイズ (幅, 高さ, 奥行き)
     * @param color 色
     * @param mode 描画モード
     */
    fun addBox(pos: Vec3, size: Vec3, color: Int, mode: RenderMode = RenderMode.Lines) {
        // 面モードの場合は透過度を調整（従来の仕様を継承）
        val finalColor = if (mode == RenderMode.Faces) color.alpha(60) else color
        mesh.addBox(pos, size, finalColor, mode == RenderMode.Lines)
    }

    /**
     * 線分を追加します。
     */
    fun addLine(start: Vec3, end: Vec3, color: Int) {
        mesh.addLine(start, end, color)
    }

    /**
     * 蓄積されたメッシュデータを Graphics3D を通じて描画し、バッファをクリアします。
     */
    fun render(graphics3D: Graphics3D) {
        graphics3D.mesh(mesh)
        mesh.clear()
    }

    /**
     * メッシュのリセット。
     */
    fun clear() {
        mesh.clear()
    }

    override fun close() {
        mesh.close()
    }
}
