package org.infinite.libs.minecraft.aim.task.condition

class ImmediatelyAimTaskCondition : AimTaskConditionInterface {
    override fun check(): AimTaskConditionReturn = AimTaskConditionReturn.Force
}
