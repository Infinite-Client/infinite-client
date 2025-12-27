package org.infinite.libs.global.server

import org.infinite.libs.global.GlobalFeature
import org.infinite.libs.global.GlobalFeatureCategory
import org.infinite.libs.global.server.protocol.ProtocolSpoofingSetting

class GlobalServerFeatureCategory :
    GlobalFeatureCategory(
        "Server",
        mutableListOf(
            GlobalFeature(ProtocolSpoofingSetting()), // Add ProtocolSpoofingSetting here
        ),
    )
