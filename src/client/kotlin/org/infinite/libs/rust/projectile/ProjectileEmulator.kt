package org.infinite.libs.rust.projectile

import net.minecraft.world.phys.Vec3
import org.infinite.nativebind.AdvancedResult as NativeAdvancedResult

object ProjectileEmulator {

    fun analyzeAdvanced(
        power: Float,
        start: Vec3,
        target: Vec3,
        vel: Vec3,
        drag: Float,
        grav: Float,
        targetGrav: Float,
        prec: Int,
        steps: Int,
        iter: Int,
    ): AdvancedResult {
        // Rust の #[xross_new] pub fn new(...) に対応するコンストラクタ
        val res = NativeAdvancedResult(
            power,
            start.x.toFloat(), start.y.toFloat(), start.z.toFloat(),
            target.x.toFloat(), target.y.toFloat(), target.z.toFloat(),
            vel.x.toFloat(), vel.y.toFloat(), vel.z.toFloat(),
            drag, grav, targetGrav,
            prec, steps, iter
        )

        res.use { nativeRes ->
            return AdvancedResult(
                nativeRes.lowPitch,
                nativeRes.highPitch,
                nativeRes.yaw,
                nativeRes.travelTicks,
                Vec3(
                    nativeRes.targetPosX.toDouble(),
                    nativeRes.targetPosY.toDouble(),
                    nativeRes.targetPosZ.toDouble(),
                ),
                nativeRes.maxRangeDist,
            )
        }
    }

    data class AdvancedResult(
        val lowP: Float,
        val highP: Float,
        val yaw: Float,
        val ticks: Int,
        val predictedPos: Vec3,
        val maxDist: Float,
    )
}
