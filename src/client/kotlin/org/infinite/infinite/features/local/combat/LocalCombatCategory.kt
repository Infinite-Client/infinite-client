package org.infinite.infinite.features.local.combat

import org.infinite.infinite.features.local.combat.archery.ArcheryFeature
import org.infinite.infinite.features.local.combat.attack.CriticalFeature
import org.infinite.infinite.features.local.combat.counter.CounterFeature
import org.infinite.infinite.features.local.combat.instantuse.InstantUseFeature
import org.infinite.infinite.features.local.combat.lockon.LockOnFeature
import org.infinite.infinite.features.local.combat.mace.MaceBoostFeature
import org.infinite.infinite.features.local.combat.quickshot.QuickShotFeature
import org.infinite.infinite.features.local.combat.swapshot.SwapShotFeature
import org.infinite.infinite.features.local.combat.throwable.ThrowableFeature
import org.infinite.libs.core.features.categories.category.LocalCategory

@Suppress("Unused")
class LocalCombatCategory : LocalCategory() {
    val archeryFeature by feature(ArcheryFeature())
    val criticalFeature by feature(CriticalFeature())
    val counterFeature by feature(CounterFeature())
    val lockOnFeature by feature(LockOnFeature())
    val maceBoostFeature by feature(MaceBoostFeature())
    val throwableFeature by feature(ThrowableFeature())
    val quickShotFeature by feature(QuickShotFeature())
    val swapShotFeature by feature(SwapShotFeature())
    val instantUseFeature by feature(InstantUseFeature())
}
