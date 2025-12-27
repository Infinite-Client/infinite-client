package org.infinite.infinite.features.automatic

import org.infinite.infinite.features.Feature
import org.infinite.infinite.features.FeatureCategory
import org.infinite.infinite.features.automatic.branch.BranchMiner
import org.infinite.infinite.features.automatic.pilot.AutoPilot
import org.infinite.infinite.features.automatic.tunnel.ShieldMachine
import org.infinite.infinite.features.automatic.wood.WoodMiner

class AutomaticFeatureCategory :
    FeatureCategory(
        "Automatic",
        mutableListOf(
            Feature(AutoPilot()),
            Feature(WoodMiner()),
            Feature(ShieldMachine()),
            Feature(BranchMiner()),
        ),
    )
