package org.infinite.libs.rust.projectile

import net.minecraft.world.phys.Vec3
import java.lang.foreign.*
import java.lang.invoke.MethodHandle

object ProjectileEmulator {
    private val advancedHandle: MethodHandle

    // 構造体のレイアウト定義 (RustのAdvancedResultPtrと完全一致させる)
    private val LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_FLOAT.withName("low_pitch"),
        ValueLayout.JAVA_FLOAT.withName("high_pitch"),
        ValueLayout.JAVA_FLOAT.withName("yaw"),
        ValueLayout.JAVA_INT.withName("travel_ticks"),
        ValueLayout.JAVA_FLOAT.withName("target_pos_x"),
        ValueLayout.JAVA_FLOAT.withName("target_pos_y"),
        ValueLayout.JAVA_FLOAT.withName("target_pos_z"),
        ValueLayout.JAVA_FLOAT.withName("max_range_dist"),
    )

    // スレッドごとに計算用バッファを保持（アロケーションをゼロに）
    private val threadArena = ThreadLocal.withInitial { Arena.ofAuto() }
    private val threadBuffer = ThreadLocal.withInitial {
        threadArena.get().allocate(LAYOUT)
    }

    init {
        val linker = Linker.nativeLinker()
        val symbol = SymbolLookup.loaderLookup().find("rust_analyze_advanced").orElseThrow()

        val desc = FunctionDescriptor.ofVoid(
            ValueLayout.JAVA_FLOAT, // power
            ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, // start
            ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, // target
            ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, // vel
            ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, // drag, grav, target_grav
            ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, // prec, steps, iter
            ValueLayout.ADDRESS, // out_ptr (結果書き込み用のアドレス)
        )
        advancedHandle = linker.downcallHandle(symbol, desc)
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

        // Rustを呼び出し（bufferのアドレスを直接渡す）
        advancedHandle.invoke(
            power,
            start.x.toFloat(), start.y.toFloat(), start.z.toFloat(),
            target.x.toFloat(), target.y.toFloat(), target.z.toFloat(),
            vel.x.toFloat(), vel.y.toFloat(), vel.z.toFloat(),
            drag, grav, targetGrav,
            prec, steps, iter,
            buffer,
        )

        // 書き込まれたデータを読み取ってデータクラスに変換
        // (ここでのコピーをさらに削るなら、AdvancedResult自体を排除して直接bufferから値を参照する)
        return AdvancedResult(
            buffer.get(ValueLayout.JAVA_FLOAT, 0),
            buffer.get(ValueLayout.JAVA_FLOAT, 4),
            buffer.get(ValueLayout.JAVA_FLOAT, 8),
            buffer.get(ValueLayout.JAVA_INT, 12),
            Vec3(
                buffer.get(ValueLayout.JAVA_FLOAT, 16).toDouble(),
                buffer.get(ValueLayout.JAVA_FLOAT, 20).toDouble(),
                buffer.get(ValueLayout.JAVA_FLOAT, 24).toDouble(),
            ),
            buffer.get(ValueLayout.JAVA_FLOAT, 28),
        )
    }

    data class AdvancedResult(val lowP: Float, val highP: Float, val yaw: Float, val ticks: Int, val predictedPos: Vec3, val maxDist: Float)
}
