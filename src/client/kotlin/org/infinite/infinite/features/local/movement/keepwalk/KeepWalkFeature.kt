package org.infinite.infinite.features.local.movement.keepwalk

import net.minecraft.world.phys.Vec3
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.graphics.Graphics3D
import org.infinite.libs.minecraft.input.InputSystem
import org.lwjgl.glfw.GLFW
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class KeepWalkFeature : LocalFeature() {
    override val defaultToggleKey: Int = GLFW.GLFW_KEY_C

    // 現在の自動歩行ベクトル
    private var moveX = 0
        set(value) {
            field = value.coerceIn(-1, 1)
        }

    // -1: 左, 1: 右
    private var moveZ = 0 // -1: 後, 1: 前
        set(value) {
            field = value.coerceIn(-1, 1)
        }

    // 前のティックの物理的なキー状態（長押し判定防止用）
    private var lastW = false
    private var lastS = false
    private var lastA = false
    private var lastD = false

    override fun onEnabled() {
        // 有効化した瞬間はベクトルをリセットして待機
        moveX = 0
        moveZ = 0
        resetLastKeys()
    }

    override fun onStartTick() {
        if (minecraft.screen != null) return

        // 1. 物理的なキー入力の状態を取得
        val isW = options.keyUp.isDown
        val isS = options.keyDown.isDown
        val isA = options.keyLeft.isDown
        val isD = options.keyRight.isDown

        // 2. 新しく押された瞬間（エッジ検出）でベクトルを更新
        if (isW && !lastW) moveZ++
        if (isS && !lastS) moveZ--
        if (isA && !lastA) moveX++
        if (isD && !lastD) moveX--

        // 物理状態を保存
        lastW = isW
        lastS = isS
        lastA = isA
        lastD = isD

        if (!isEnabled()) return

        // 3. 毎ティック、現在のベクトルを InputSystem に反映させる
        applyMovementToInputSystem()
    }

    private fun applyMovementToInputSystem() {
        // --- 前後方向 (Z) ---
        when {
            moveZ > 0 -> {
                InputSystem.press(options.keyUp)
            }

            moveZ < 0 -> {
                InputSystem.press(options.keyDown)
            }

            else -> {
            }
        }

        // --- 左右方向 (X) ---
        when {
            moveX > 0 -> {
                InputSystem.press(options.keyLeft)
            }

            moveX < 0 -> {
                InputSystem.press(options.keyRight)
            }

            else -> {
            }
        }
    }

    override fun onDisabled() {
        moveX = 0
        moveZ = 0
        // InputSystemはcondition()がfalseを返すことで自動的にReleaseされる
    }

    private fun resetLastKeys() {
        lastW = options.keyUp.isDown
        lastS = options.keyDown.isDown
        lastA = options.keyLeft.isDown
        lastD = options.keyRight.isDown
    }

    override fun onLevelRendering(graphics3D: Graphics3D) {
        if (!isEnabled() || (moveX == 0 && moveZ == 0)) return

        val p = player ?: return
        // プレイヤーの足元（少し上）の位置を取得
        val pos = p.getPosition(graphics3D.realDelta)

        // 移動方向の角度を計算 (ラジアン)
        // moveX, moveZ は -1..1 なので atan2 で角度が出る
        // Minecraftの座標系に合わせて調整（Zが前後、Xが左右）
        val angle = atan2(moveZ.toDouble(), moveX.toDouble()) - PI / 2.0
        val yaw = Math.toRadians((p.yRot).toDouble()) // プレイヤーの向き
        val totalAngle = angle + yaw

        // アニメーション用の時間（0.0 ~ 1.0 のループ）
        val time = (System.currentTimeMillis() % 1000) / 1000.0

        drawAnimatedArrow(graphics3D, pos, totalAngle, time)
    }

    private fun drawAnimatedArrow(g: Graphics3D, basePos: Vec3, angle: Double, time: Double) {
        val color = InfiniteClient.theme.colorScheme.accentColor

        // 回転行列の計算を簡易的に行うための関数
        fun rotate(vec: Vec3): Vec3 {
            val cos = cos(angle)
            val sin = sin(angle)
            return Vec3(
                vec.x * cos - vec.z * sin,
                vec.y,
                vec.x * sin + vec.z * cos,
            )
        }

        // 矢印の形状定義（中心を0,0とした相対座標）
        // アニメーションとして、矢印が少しずつ前方に移動して消えるエフェクト
        val offset = time * 0.5

        // 矢印の頂点 (三角形)
        val v1 = rotate(Vec3(0.0, 0.0, 0.3 + offset)).add(basePos)
        val v2 = rotate(Vec3(-0.2, 0.0, 0.0 + offset)).add(basePos)
        val v3 = rotate(Vec3(0.2, 0.0, 0.0 + offset)).add(basePos)

        // 矢印の描画（グラデーションで先端を明るく、後ろを透明に近づけると恰好良い）
        val alpha = ((1.0 - time) * 150).toInt() shl 24
        val fadeColor = (color shl 8 ushr 8) or alpha

        g.triangleFill(v1, v2, v3, fadeColor, depthTest = false)

        // 軸の部分 (四角形)
        val b1 = rotate(Vec3(-0.1, 0.0, 0.0 + offset)).add(basePos)
        val b2 = rotate(Vec3(0.1, 0.0, 0.0 + offset)).add(basePos)
        val b3 = rotate(Vec3(0.1, 0.0, -0.3 + offset)).add(basePos)
        val b4 = rotate(Vec3(-0.1, 0.0, -0.3 + offset)).add(basePos)

        g.rectangleFill(b1, b2, b3, b4, fadeColor, depthTest = false)
    }
}
