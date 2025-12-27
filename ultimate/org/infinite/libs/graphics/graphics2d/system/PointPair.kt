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
            val v1x = if (len1 < 0.0001f) 0f else d1x / len1
            val v1y = if (len1 < 0.0001f) 0f else d1y / len1

            val d2x = nextX - currX
            val d2y = nextY - currY
            val len2 = sqrt(d2x * d2x + d2y * d2y)
            val v2x = if (len2 < 0.0001f) 0f else d2x / len2
            val v2y = if (len2 < 0.0001f) 0f else d2y / len2

            // 2. 各辺の法線ベクトル（画面座標系では右手系なので、90度右回転）
            // 法線 = (-y, x) で時計回りに90度回転
            val n1x = -v1y

            val n2x = -v2y

            // 3. Miterベクトル = 2つの法線の和
            val miterX = n1x + n2x
            val miterY = v1x + v2x
            val mLenSq = miterX * miterX + miterY * miterY

            // 4. miterベクトルをどれだけ伸ばすか計算
            // miter と n1 の内積 = |miter| * |n1| * cos(θ/2)
            // halfWidth / cos(θ/2) が必要な長さ
            if (mLenSq < 0.0001f) {
                // 180度のコーナー（折り返し）の場合
                return PointPair(
                    ix = currX + n1x * halfWidth,
                    iy = currY + v1x * halfWidth,
                    ox = currX - n1x * halfWidth,
                    oy = currY - v1x * halfWidth,
                )
            }

            val mLen = sqrt(mLenSq)
            val dot = miterX * n1x + miterY * v1x // 正しい内積
            val scale = halfWidth / (dot / mLen)

            val finalMiterX = (miterX / mLen) * scale
            val finalMiterY = (miterY / mLen) * scale

            return PointPair(
                ix = currX - finalMiterX,
                iy = currY - finalMiterY,
                ox = currX + finalMiterX,
                oy = currY + finalMiterY,
            )
        }
    }
}
