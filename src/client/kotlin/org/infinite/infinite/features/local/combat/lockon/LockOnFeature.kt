package org.infinite.infinite.features.local.combat.lockon

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.core.features.property.number.DoubleProperty
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.minecraft.aim.AimSystem
import org.infinite.libs.minecraft.aim.task.AimTask
import org.infinite.libs.minecraft.aim.task.condition.AimTaskConditionInterface
import org.infinite.libs.minecraft.aim.task.condition.AimTaskConditionReturn
import org.infinite.libs.minecraft.aim.task.config.AimCalculateMethod
import org.infinite.libs.minecraft.aim.task.config.AimPriority
import org.infinite.libs.minecraft.aim.task.config.AimTarget
import org.infinite.utils.alpha
import org.lwjgl.glfw.GLFW
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2

class LockOnFeature : LocalFeature() {
    private val targetList = mutableListOf<LivingEntity>()
    private var currentIndex = -1
    var isPaused = false
        private set
    val selectedEntity: LivingEntity?
        get() = if (currentIndex in targetList.indices) targetList[currentIndex] else null

    /**
     * 現在エイムシステムが追従しているターゲット。
     * 一時停止中 (isPaused == true) は null を返します。
     */
    val lockedEntity: LivingEntity?
        get() = if (!isPaused) selectedEntity else null
    private var currentTask: AimTask? = null
        set(value) {
            field?.let { AimSystem.remove(it) }
            value?.let { AimSystem.addTask(it) }
            field = value
        }

    // --- プロパティ ---
    private val range by property(DoubleProperty(32.0, 3.0, 256.0))
    private val players by property(BooleanProperty(true))
    private val mobs by property(BooleanProperty(true))
    private val aimSpeed by property(DoubleProperty(1.0, 0.5, 10.0))
    private val checkLineOfSight by property(BooleanProperty(true))
    private val pauseOnKill by property(BooleanProperty(false)) // 倒した時に一時停止するか
    private val autoNextTarget by property(BooleanProperty(true)) // 次の標的を自動で探すか

    init {
        // L: 新しいターゲットを追加
        defineAction("add_target", GLFW.GLFW_KEY_K) {
            addNewTarget()
            isPaused = false
        }

        // I: ロックオンの一時停止/再開
        defineAction("toggle_pause", GLFW.GLFW_KEY_I) {
            isPaused = !isPaused
            if (isPaused) currentTask = null else updateAimTask()
        }

        // O: 現在のターゲットを削除
        defineAction("remove_target", GLFW.GLFW_KEY_O) {
            if (currentIndex in targetList.indices) {
                targetList.removeAt(currentIndex)
                if (targetList.isEmpty()) {
                    currentIndex = -1
                    currentTask = null
                } else {
                    currentIndex %= targetList.size
                    updateAimTask()
                }
            }
        }

        defineAction("next_target_spatial", GLFW.GLFW_KEY_RIGHT) {
            switchTargetSpatial(true) // 右側のターゲットへ
        }
        defineAction("prev_target_spatial", GLFW.GLFW_KEY_LEFT) {
            switchTargetSpatial(false) // 左側のターゲットへ
        }

        defineAction("focus_best", GLFW.GLFW_KEY_SEMICOLON) {
            reorderTargetsByPriority()
        }

        // 追加機能案: 全解除 (Pキー)
        defineAction("clear_all", GLFW.GLFW_KEY_J) {
            targetList.clear()
            currentIndex = -1
            currentTask = null
        }
    }

    override fun onEnabled() {
        // 有効化した時点では何もしない（ターゲットリストを空で開始）
        targetList.clear()
        currentIndex = -1
        isPaused = false
    }

    override fun onDisabled() {
        currentTask = null
        targetList.clear()
    }

    override fun onStartTick() {
        if (!isEnabled()) return
        val player = this@LockOnFeature.player ?: return

        val iterator = targetList.iterator()
        var targetWasRemoved = false
        val currentFocus = selectedEntity

        while (iterator.hasNext()) {
            val t = iterator.next()
            if (!t.isAlive || t.isRemoved || player.distanceTo(t) > range.value) {
                if (t == currentFocus) targetWasRemoved = true
                iterator.remove()
            }
        }

        if (targetWasRemoved) {
            // 1. 倒した瞬間のエイム挙動を決定
            if (pauseOnKill.value) {
                isPaused = true
                currentTask = null
            }

            // 2. 次のターゲットへの移行
            if (targetList.isNotEmpty()) {
                currentIndex = currentIndex.coerceIn(0, targetList.size - 1)
                // 継続モードなら即座にタスクを更新
                if (!isPaused) updateAimTask()
            } else if (autoNextTarget.value) {
                // リストが空なら周囲を索敵
                val found = autoSearchNextTarget()
                // 索敵に成功しても、一時停止設定ならタスクは作らない
                if (found && isPaused) currentTask = null
            } else {
                currentIndex = -1
                currentTask = null
            }
        }
    }

    /**
     * リストが空になった際、周囲の有効なターゲットを1体自動で取得してロックする
     */
    private fun autoSearchNextTarget(): Boolean {
        val player = player ?: return false
        val level = level ?: return false

        val nextBest = level.getEntities(player, player.boundingBox.inflate(range.value))
            .asSequence()
            .filterIsInstance<LivingEntity>()
            .filter { it != player && it.isAlive }
            .filter { (players.value && it is Player) || (mobs.value && it !is Player) }
            .filter { !checkLineOfSight.value || player.hasLineOfSight(it) }
            // 視線の中心に近い敵を優先
            .minByOrNull { calculateCombinedScore(player, it) }

        return if (nextBest != null) {
            targetList.add(nextBest)
            currentIndex = 0
            updateAimTask()
            true
        } else {
            false
        }
    }

    private fun addNewTarget() {
        val player = player ?: return
        val level = level ?: return

        // 視界に入っている最も「良い」ターゲットを探す（未登録のものに限定）
        val newTarget = level.getEntities(player, player.boundingBox.inflate(range.value))
            .asSequence()
            .filterIsInstance<LivingEntity>()
            .filter { it != player && it.isAlive && it !in targetList }
            .filter { (players.value && it is Player) || (mobs.value && it !is Player) }
            .filter { !checkLineOfSight.value || player.hasLineOfSight(it) }
            .minByOrNull { calculateCombinedScore(player, it) }

        if (newTarget != null) {
            targetList.add(newTarget)
            currentIndex = targetList.size - 1
            updateAimTask()
        }
    }

    private fun updateAimTask() {
        val target = lockedEntity ?: run {
            currentTask = null
            return
        }

        currentTask = AimTask(
            priority = AimPriority.Normally,
            target = AimTarget.EntityTarget(target, AimTarget.EntityTarget.EntityAnchor.Center),
            condition = object : AimTaskConditionInterface {
                override fun check(): AimTaskConditionReturn {
                    return if (isEnabled() && !isPaused && targetList.contains(target) && target.isAlive) {
                        AimTaskConditionReturn.Exec
                    } else {
                        AimTaskConditionReturn.Failure
                    }
                }
            },
            calcMethod = AimCalculateMethod.Linear,
            multiply = aimSpeed.value,
        )
    }

    private fun reorderTargetsByPriority() {
        val player = player ?: return
        if (targetList.isEmpty()) return

        // 現在のリストをスコア順に並び替え、最初のものを選択
        targetList.sortBy { calculateCombinedScore(player, it) }
        currentIndex = 0
        updateAimTask()
    }

    private fun calculateCombinedScore(p: Player, target: LivingEntity): Double {
        val playerLookVec = p.lookAngle.normalize()
        val targetVec = target.boundingBox.center.subtract(p.eyePosition).normalize()
        val angle = Math.toDegrees(acos(playerLookVec.dot(targetVec).coerceIn(-1.0, 1.0)))

        val distNormalized = (p.distanceTo(target) / range.value).coerceIn(0.0, 1.0)
        val angleNormalized = (angle / 90.0).coerceIn(0.0, 1.0)
        return (angleNormalized * 0.7) + (distNormalized * 0.3)
    }

    override fun onEndUiRendering(graphics2D: Graphics2D) {
        if (targetList.isEmpty()) return
        val isMultiple = targetList.size > 1
        targetList.forEachIndexed { index, entity ->
            val isCurrent = index == currentIndex
            renderTargetMark(graphics2D, entity, isCurrent, isPaused)
            // ロックオン情報のテキスト表示 (アクティブなターゲットのみ)
            if (isCurrent && isMultiple) {
                renderTargetInfo(graphics2D, entity, index + 1, targetList.size)
            }
        }
    }

    private fun renderTargetInfo(graphics2D: Graphics2D, target: LivingEntity, current: Int, total: Int) {
        val pos = target.getPosition(graphics2D.gameDelta).add(0.0, target.eyeHeight.toDouble(), 0.0)
        val screenPos = graphics2D.projectWorldToScreen(pos) ?: return

        val x = screenPos.first.toFloat()
        val y = screenPos.second.toFloat() + 20f // マークの下に表示

        val text = "$current / $total"
        graphics2D.textStyle.size = 10f
        graphics2D.textStyle.font = "infinite_regular"
        graphics2D.fillStyle = InfiniteClient.theme.colorScheme.accentColor
        graphics2D.textCentered(text, x, y)
    }

    private fun renderTargetMark(graphics2D: Graphics2D, target: LivingEntity, active: Boolean, paused: Boolean) {
        // ターゲットの描画位置（目の高さ）
        val targetPos = target.getPosition(graphics2D.gameDelta).add(0.0, target.eyeHeight.toDouble(), 0.0)
        val screenPos = graphics2D.projectWorldToScreen(targetPos) ?: return

        val x = screenPos.first.toFloat()
        val y = screenPos.second.toFloat()

        // 状態に応じたサイズと色の決定
        val baseSize = 12f
        val currentSize = if (active) baseSize else baseSize * 0.5f

        val colorScheme = InfiniteClient.theme.colorScheme
        val color = when {
            paused -> colorScheme.secondaryColor // ポーズ中
            active -> colorScheme.accentColor // アクティブ
            else -> colorScheme.accentColor.alpha(120) // 非アクティブ（少し透過）
        }

        // --- 描画処理 ---
        graphics2D.beginPath()
        graphics2D.strokeStyle.color = color
        graphics2D.strokeStyle.width = if (active) 2f else 1f

        // 円の描画 (アクティブ時のみ 0.75倍)
        graphics2D.arc(x, y, currentSize * 0.75f, 0f, (PI * 2).toFloat())
        graphics2D.strokePath()

        // X印の描画
        graphics2D.beginPath()
        graphics2D.moveTo(x - currentSize, y - currentSize)
        graphics2D.lineTo(x + currentSize, y + currentSize)
        graphics2D.strokePath()

        graphics2D.beginPath()
        graphics2D.moveTo(x + currentSize, y - currentSize)
        graphics2D.lineTo(x - currentSize, y + currentSize)
        graphics2D.strokePath()
    }

    /**
     * 現在のターゲットから見て、画面上の右または左にいる最も近いターゲットに切り替える
     */
    private fun switchTargetSpatial(right: Boolean) {
        val player = player ?: return
        val current = lockedEntity ?: return
        if (targetList.size <= 1) return

        val currentYaw = getRelativeYaw(player, current)

        // 現在のターゲットより右（または左）にあるものを抽出
        val candidates = targetList.filter { it != current }.map {
            it to getRelativeYaw(player, it)
        }.filter { (_, yaw) ->
            if (right) yaw > currentYaw else yaw < currentYaw
        }

        val nextSelected = if (right) {
            // 右側の中で最も現在に近いもの。なければ一番左端を選択（ループ）
            candidates.minByOrNull { it.second }?.first ?: targetList.minBy { getRelativeYaw(player, it) }
        } else {
            // 左側の中で最も現在に近いもの。なければ一番右端を選択（ループ）
            candidates.maxByOrNull { it.second }?.first ?: targetList.maxBy { getRelativeYaw(player, it) }
        }

        currentIndex = targetList.indexOf(nextSelected)
        updateAimTask()
    }

    private fun getRelativeYaw(p: Player, target: LivingEntity): Double {
        // 1. ターゲットへのベクトルを計算
        val vec = target.boundingBox.center.subtract(p.eyePosition)

        // 2. ターゲットへの絶対的な Yaw を計算 (Minecraftの座標系に合わせる)
        // atan2(-x, z) は Minecraft のエンティティが向く方向の計算式
        val targetYaw = Math.toDegrees(atan2(-vec.x, vec.z))

        // 3. プレイヤーの現在の Yaw との差を取る
        var relativeYaw = targetYaw - p.yRot.toDouble()

        // 4. 角度を -180° ～ 180° の範囲に正規化
        // これにより、背後にいる敵でも「右から回ったほうが近いか左か」が正しく判別できる
        while (relativeYaw <= -180.0) relativeYaw += 360.0
        while (relativeYaw > 180.0) relativeYaw -= 360.0

        return relativeYaw
    }
}
