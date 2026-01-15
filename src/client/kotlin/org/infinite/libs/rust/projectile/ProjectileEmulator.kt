package org.infinite.libs.rust.projectile

import net.minecraft.world.phys.Vec3
import java.lang.foreign.*
import java.lang.invoke.MethodHandle

object ProjectileEmulator {
    private val advancedHandle: MethodHandle

    private val ADVANCED_LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_FLOAT.withName("low_pitch"),
        ValueLayout.JAVA_FLOAT.withName("high_pitch"),
        ValueLayout.JAVA_FLOAT.withName("yaw"),
        ValueLayout.JAVA_INT.withName("travel_ticks"),
        ValueLayout.JAVA_FLOAT.withName("target_pos_x"),
        ValueLayout.JAVA_FLOAT.withName("target_pos_y"),
        ValueLayout.JAVA_FLOAT.withName("target_pos_z"),
        ValueLayout.JAVA_FLOAT.withName("max_range_dist"),
    ).withByteAlignment(4)

    init {
        val linker = Linker.nativeLinker()
        val symbol = SymbolLookup.loaderLookup().find("rust_analyze_advanced").orElseThrow()

        val desc = FunctionDescriptor.of(
            ADVANCED_LAYOUT,
            ValueLayout.JAVA_FLOAT, // power
            ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, // start xyz
            ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, // target xyz
            ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, // vel xyz
            ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, // drag, grav, target_grav
            ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, // prec, steps, iter
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
        Arena.ofConfined().use { arena ->
            val seg = advancedHandle.invoke(
                arena, power,
                start.x.toFloat(), start.y.toFloat(), start.z.toFloat(),
                target.x.toFloat(), target.y.toFloat(), target.z.toFloat(),
                vel.x.toFloat(), vel.y.toFloat(), vel.z.toFloat(),
                drag, grav, targetGrav, prec, steps, iter,
            ) as MemorySegment

            return AdvancedResult(
                seg.get(ValueLayout.JAVA_FLOAT, 0),
                seg.get(ValueLayout.JAVA_FLOAT, 4),
                seg.get(ValueLayout.JAVA_FLOAT, 8),
                seg.get(ValueLayout.JAVA_INT, 12),
                Vec3(seg.get(ValueLayout.JAVA_FLOAT, 16).toDouble(), seg.get(ValueLayout.JAVA_FLOAT, 20).toDouble(), seg.get(ValueLayout.JAVA_FLOAT, 24).toDouble()),
                seg.get(ValueLayout.JAVA_FLOAT, 28),
            )
        }
    }

    data class AdvancedResult(val lowP: Float, val highP: Float, val yaw: Float, val ticks: Int, val predictedPos: Vec3, val maxDist: Float)
}
