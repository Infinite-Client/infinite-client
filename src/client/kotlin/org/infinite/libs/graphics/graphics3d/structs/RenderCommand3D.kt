package org.infinite.libs.graphics.graphics3d.structs

import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f

/**
 * 3D空間での描画命令をカプセル化するデータ構造
 */
sealed interface RenderCommand3D {
    data class Line(
        val from: Vec3,
        val to: Vec3,
        val color: Int,
        val size: Float,
        val depthTest: Boolean,
    ) : RenderCommand3D

    data class Triangle(
        val a: Vec3,
        val b: Vec3,
        val c: Vec3,
        val color: Int,
        val depthTest: Boolean,
    ) : RenderCommand3D

    data class TriangleFill(
        val a: Vec3,
        val b: Vec3,
        val c: Vec3,
        val color: Int,
        val depthTest: Boolean,
    ) : RenderCommand3D

    data class Quad(
        val a: Vec3,
        val b: Vec3,
        val c: Vec3,
        val d: Vec3,
        val color: Int,
        val depthTest: Boolean,
    ) : RenderCommand3D

    data class QuadFill(
        val a: Vec3,
        val b: Vec3,
        val c: Vec3,
        val d: Vec3,
        val color: Int,
        val depthTest: Boolean,
    ) : RenderCommand3D

    data class SetMatrix(val matrix: Matrix4f) : RenderCommand3D

    object PushMatrix : RenderCommand3D
    object PopMatrix : RenderCommand3D
}
