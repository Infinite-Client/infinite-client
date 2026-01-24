package org.infinite.libs.minecraft.aim

import org.infinite.libs.interfaces.MinecraftInterface
import org.infinite.libs.minecraft.aim.task.AimTask
import org.infinite.libs.minecraft.aim.task.config.AimPriority
import org.infinite.libs.minecraft.aim.task.config.AimProcessResult

object AimSystem : MinecraftInterface() {
    // 初期化を標準的な空のデキューに変更
    private val tasks = ArrayDeque<AimTask>()

    fun addTask(task: AimTask) {
        // 同じタスクが既に存在する場合は追加しない（二重登録防止）
        if (tasks.contains(task)) return

        when (task.priority) {
            AimPriority.Immediately -> {
                // 最優先：先頭に追加
                tasks.addFirst(task)
            }

            AimPriority.Preferentially -> {
                // 優先：最初の Normally タスクの前に挿入
                val insertionIndex = tasks.indexOfFirst { it.priority == AimPriority.Normally }
                if (insertionIndex >= 0) {
                    tasks.add(insertionIndex, task)
                } else {
                    // Normally がなければ最後に追加
                    tasks.addLast(task)
                }
            }

            AimPriority.Normally -> {
                // 通常：最後に追加
                tasks.addLast(task)
            }
        }
    }

    // --- 他のメソッドはそのまま ---
    fun taskLength(): Int = tasks.size
    fun currentTask(): AimTask? = tasks.firstOrNull()
    fun clear() = tasks.clear()
    fun remove(task: AimTask) = tasks.remove(task)

    fun process() {
        val p = player ?: return run { clear() }
        if (!p.isAlive) return run { clear() }

        val currentTask = tasks.firstOrNull() ?: return

        when (currentTask.process()) {
            AimProcessResult.Progress -> {}

            AimProcessResult.Failure -> {
                currentTask.atFailure()
                tasks.removeFirstOrNull()
            }

            AimProcessResult.Success -> {
                currentTask.atSuccess()
                tasks.removeFirstOrNull()
            }
        }
    }
}
