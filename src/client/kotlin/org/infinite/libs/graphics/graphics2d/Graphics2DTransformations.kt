package org.infinite.libs.graphics.graphics2d

import org.infinite.libs.graphics.graphics2d.structs.RenderCommand2D
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand2DProvider

class Graphics2DTransformations(
    private val provider: RenderCommand2DProvider,
) {
    fun transform(x: Float, y: Float, z: Float) {
        // Vector3f を保持する Transform コマンドの set を呼び出し
        provider.getTransform().set(x, y, z)
    }

    fun translate(x: Float, y: Float) {
        provider.getTranslate().set(x, y)
    }

    fun rotate(angle: Float) {
        provider.getRotate().set(angle)
    }

    fun scale(x: Float, y: Float) {
        provider.getScale().set(x, y)
    }

    fun setTransform(m00: Float, m10: Float, m01: Float, m11: Float, m02: Float, m12: Float) {
        provider.getSetTransform().set(m00, m10, m01, m11, m02, m12)
    }

    fun resetTransform() {
        provider.addStatic(RenderCommand2D.ResetTransform)
    }

    fun push() {
        provider.addStatic(RenderCommand2D.PushTransform)
    }

    fun pop() {
        provider.addStatic(RenderCommand2D.PopTransform)
    }
}
