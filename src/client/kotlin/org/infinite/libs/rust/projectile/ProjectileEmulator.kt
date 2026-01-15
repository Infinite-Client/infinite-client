package org.infinite.libs.rust.projectile

import org.infinite.libs.rust.LibInfiniteClient
import java.lang.foreign.*
import java.lang.invoke.MethodHandle

object ProjectileEmulator {
    private val solvePitchHandle: MethodHandle

    private val RESULT_LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_FLOAT.withName("pitch"),
        ValueLayout.JAVA_INT.withName("travel_ticks"),
        ValueLayout.JAVA_FLOAT.withName("hit_y"),
        ValueLayout.JAVA_FLOAT.withName("hit_x"),
        ValueLayout.JAVA_INT.withName("success"),
    ).withByteAlignment(4)

    init {
        LibInfiniteClient.loadNativeLibrary()
        val linker = Linker.nativeLinker()
        val lookup = SymbolLookup.loaderLookup()
        val symbol = lookup.find("rust_solve_pitch").orElseThrow()

        // Java 25 の Downcall 規約: 構造体リターンの場合
        // descriptor には「戻り値の型」を含めますが、
        // 実際の MethodHandle は、第1引数に SegmentAllocator を要求します。
        val descriptor = FunctionDescriptor.of(
            RESULT_LAYOUT,
            ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT,
            ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT,
        )

        solvePitchHandle = linker.downcallHandle(symbol, descriptor)
    }

    fun solve(p: Float, dx: Float, dy: Float, minP: Float, maxP: Float, prec: Int, steps: Int, drag: Float, grav: Float): TrajectoryData {
        Arena.ofConfined().use { arena ->
            // Java 25 規約: invoke の第1引数に arena (SegmentAllocator) を渡す
            // これにより、Rust側から返される構造体データが格納されるメモリが安全に確保されます
            val seg = solvePitchHandle.invoke(arena, p, dx, dy, minP, maxP, prec, steps, drag, grav) as MemorySegment

            return TrajectoryData(
                pitch = seg.get(ValueLayout.JAVA_FLOAT, 0),
                ticks = seg.get(ValueLayout.JAVA_INT, 4),
                success = seg.get(ValueLayout.JAVA_INT, 16) == 1,
            )
        }
    }

    data class TrajectoryData(val pitch: Float, val ticks: Int, val success: Boolean)
}
