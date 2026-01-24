package org.infinite.libs.rust.projectile

import net.minecraft.world.phys.Vec3
import org.infinite.nativebind.AdvancedResultPtr
import org.infinite.nativebind.infinite_client_h
import java.lang.foreign.Arena

object ProjectileEmulator {

    // スレッドごとに計算用バッファを保持（ゼロ・アロケーション維持）
    // jextractが生成したレイアウト(AdvancedResultPtr.layout())を使用
    private val threadArena = ThreadLocal.withInitial { Arena.ofAuto() }
    private val threadBuffer = ThreadLocal.withInitial {
        AdvancedResultPtr.allocate(threadArena.get())
    }

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
        val buffer = threadBuffer.get()

        // jextractが生成した静的メソッドを直接呼び出し
        // 型安全であり、内部でMethodHandleが最適化されているため高速です
        infinite_client_h.rust_analyze_advanced(
            power,
            start.x.toFloat(), start.y.toFloat(), start.z.toFloat(),
            target.x.toFloat(), target.y.toFloat(), target.z.toFloat(),
            vel.x.toFloat(), vel.y.toFloat(), vel.z.toFloat(),
            drag, grav, targetGrav,
            prec, steps, iter,
            buffer, // MemorySegment (Address) として渡される
        )

        // 書き込まれたデータを読み取る
        // オフセット計算(0, 4, 8...)をjextract生成のゲッターに任せることで安全性を確保
        return AdvancedResult(
            AdvancedResultPtr.low_pitch(buffer),
            AdvancedResultPtr.high_pitch(buffer),
            AdvancedResultPtr.yaw(buffer),
            AdvancedResultPtr.travel_ticks(buffer),
            Vec3(
                AdvancedResultPtr.target_pos_x(buffer).toDouble(),
                AdvancedResultPtr.target_pos_y(buffer).toDouble(),
                AdvancedResultPtr.target_pos_z(buffer).toDouble(),
            ),
            AdvancedResultPtr.max_range_dist(buffer),
        )
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
