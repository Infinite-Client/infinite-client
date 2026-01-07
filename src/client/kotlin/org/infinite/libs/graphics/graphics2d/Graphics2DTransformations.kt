package org.infinite.libs.graphics.graphics2d

import org.infinite.libs.graphics.graphics2d.structs.RenderCommand2D
import org.joml.Matrix3x2f
import org.joml.Vector3f
import java.util.LinkedList

class Graphics2DTransformations(
    private val commandQueue: LinkedList<RenderCommand2D>,
) {
    fun transform(x: Float, y: Float, z: Float) {
        commandQueue.add(RenderCommand2D.Transform(Vector3f(x, y, z)))
    }

    fun translate(x: Float, y: Float) {
        commandQueue.add(RenderCommand2D.Translate(x, y))
    }

    fun rotate(angle: Float) {
        commandQueue.add(RenderCommand2D.Rotate(angle))
    }

    fun scale(x: Float, y: Float) {
        commandQueue.add(RenderCommand2D.Scale(x, y))
    }

    fun setTransform(m00: Float, m10: Float, m01: Float, m11: Float, m02: Float, m12: Float) {
        commandQueue.add(RenderCommand2D.SetTransform(Matrix3x2f(m00, m10, m01, m11, m02, m12)))
    }

    fun resetTransform() {
        commandQueue.add(RenderCommand2D.ResetTransform)
    }

    fun push() {
        commandQueue.add(RenderCommand2D.PushTransform)
    }

    fun pop() {
        commandQueue.add(RenderCommand2D.PopTransform)
    }
}
