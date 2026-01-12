package org.infinite.libs.minecraft.aim

import org.infinite.libs.interfaces.MinecraftInterface
import org.infinite.libs.minecraft.aim.task.AimTask
import org.infinite.libs.minecraft.aim.task.config.AimPriority
import org.infinite.libs.minecraft.aim.task.config.AimProcessResult

object AimSystem : MinecraftInterface() {
    private var tasks: ArrayDeque<AimTask> = ArrayDeque(listOf())

    fun addTask(task: AimTask) {
        when (task.priority) {
            AimPriority.Normally -> {
                tasks.addLast(task)
            }

            // 一番後ろに追加 (通常のタスク)
            AimPriority.Immediately -> {
                tasks.addFirst(task)
            }

            // 一番前に追加 (即時実行タスク)
            AimPriority.Preferentially -> {
                val insertionIndex = tasks.indexOfFirst { it.priority == AimPriority.Normally }
                if (insertionIndex != -1) {
                    tasks.add(insertionIndex, task)
                } else {
                    tasks.addLast(task)
                }
            }
        }
    }

    fun taskLength(): Int = tasks.size

    fun currentTask(): AimTask? = tasks.firstOrNull()
    fun clear() {
        tasks.clear()
    }
    fun remove(task: AimTask) {
        tasks.remove(task)
    }
    fun process() {
        // 1. プレイヤーが存在しない、または死亡している場合は処理を中断し、キューをクリアする
        val player = player ?: run {
            clear()
            return
        }
        if (!player.isAlive) {
            clear()
            return
        }
        val currentTask = currentTask() ?: return
        when (currentTask.process()) {
            AimProcessResult.Progress -> {}

            AimProcessResult.Failure -> {
                currentTask.atFailure()
                tasks.removeFirst()
            }

            AimProcessResult.Success -> {
                currentTask.atSuccess()
                tasks.removeFirst()
            }
        }
    }
}
