package org.infinite.infinite.features.server

import org.infinite.infinite.features.Feature
import org.infinite.infinite.features.FeatureCategory
import org.infinite.infinite.features.server.anti.AntiCheat
import org.infinite.infinite.features.server.connection.AutoConnect
import org.infinite.infinite.features.server.connection.AutoLeave
import org.infinite.infinite.features.server.detect.DetectServer
import org.infinite.infinite.features.server.info.ServerInfo

class ServerFeatureCategory :
    FeatureCategory(
        "Server",
        mutableListOf(
            Feature(ServerInfo()),
            Feature(AutoConnect()),
            Feature(DetectServer()),
            Feature(AutoLeave()),
            Feature(AntiCheat()),
        ),
    )
