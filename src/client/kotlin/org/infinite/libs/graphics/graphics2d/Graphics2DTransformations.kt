package org.infinite.libs.graphics.graphics2d

import org.joml.Matrix3x2f
import java.util.Stack

class Graphics2DTransformations(
    private val transformMatrix: Matrix3x2f,
    private val transformStack: Stack<Matrix3x2f>,
) {
    /**
     * 現在の変換行列に変換を適用します。
     */
    fun transform(m00: Float, m10: Float, m01: Float, m11: Float, m02: Float, m12: Float) {
        transformMatrix.mul(Matrix3x2f(m00, m10, m01, m11, m02, m12))
    }

    /**
     * 現在の変換行列に移動変換を適用します。
     */
    fun translate(x: Float, y: Float) {
        transformMatrix.translate(x, y)
    }

    /**
     * 現在の変換状態をスタックに保存します。
     */
    fun save() {
        transformStack.push(Matrix3x2f(transformMatrix)) // 現在の行列のコピーをプッシュ
    }

    /**
     * スタックから変換状態を復元します。
     * スタックが空の場合は何もしません。
     */
    fun restore() {
        if (transformStack.isNotEmpty()) {
            transformMatrix.set(transformStack.pop()) // スタックからポップして復元
        }
    }
}
