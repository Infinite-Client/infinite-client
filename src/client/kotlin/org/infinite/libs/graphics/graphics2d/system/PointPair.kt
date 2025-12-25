package org.infinite.libs.graphics.graphics2d.system

import kotlin.math.sqrt

data class PointPair(val ix: Float, val iy: Float, val ox: Float, val oy: Float) {
    companion object {
        fun calculateForMiter(
            currX: Float,
            currY: Float,
            prevX: Float,
            prevY: Float,
            nextX: Float,
            nextY: Float,
            halfWidth: Float,
        ): PointPair {
            // 1. 前後の辺の方向ベクトルを正規化
            val d1x = currX - prevX
            val d1y = currY - prevY
            val len1 = sqrt(d1x * d1x + d1y * d1y)
            val v1x = d1x / len1
            val v1y = d1y / len1

            val d2x = nextX - currX
            val d2y = nextY - currY
            val len2 = sqrt(d2x * d2x + d2y * d2y)
            val v2x = d2x / len2
            val v2y = d2y / len2

            // 2. 各辺の法線ベクトル (左手系/右回りを想定)
            // 法線 = (-y, x)
            val n1x = -v1y
            val n2x = -v2y

            // 3. 角の二等分線ベクトル (Miter Vector) を計算
            val miterX = n1x + n2x
            val miterY = v1x + v2x
            val mLenSq = miterX * miterX + miterY * miterY

            // 4. 厚みの補正係数を計算
            // miter . n1 (内積) = cos(θ) を利用して、1/cos(θ) 倍に伸ばす
            val dot = miterX * n1x + miterY * v1x
            val scale = if (mLenSq < 0.0001f) 1f else (halfWidth / (dot / sqrt(mLenSq)))

            val finalMiterX = miterX / sqrt(mLenSq) * scale
            val finalMiterY = miterY / sqrt(mLenSq) * scale

            return PointPair(
                ix = currX - finalMiterX,
                iy = currY - finalMiterY,
                ox = currX + finalMiterX,
                oy = currY + finalMiterY,
            )
        }
    }
}
