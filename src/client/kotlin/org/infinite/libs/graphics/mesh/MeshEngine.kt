package org.infinite.libs.graphics.mesh

import net.minecraft.world.phys.Vec3
import org.infinite.libs.graphics.Graphics3D

/**
 * 汎用的な3Dメッシュ構築エンジン（一時的な空実装）。
 */
class MeshEngine : AutoCloseable {

    enum class RenderMode {
        Lines,
        Faces,
    }

    private val mesh = InfiniteMesh()

    fun addCube(pos: Vec3, color: Int, mode: RenderMode = RenderMode.Lines) {}

    fun addBox(pos: Vec3, size: Vec3, color: Int, mode: RenderMode = RenderMode.Lines) {}

    fun addLine(start: Vec3, end: Vec3, color: Int) {}

    fun render(graphics3D: Graphics3D) {
        graphics3D.mesh(mesh)
        mesh.clear()
    }

    fun clear() {
        mesh.clear()
    }

    override fun close() {
        mesh.close()
    }
}
