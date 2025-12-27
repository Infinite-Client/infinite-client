package org.infinite.infinite.features.rendering

import org.infinite.infinite.features.Feature
import org.infinite.infinite.features.FeatureCategory
import org.infinite.infinite.features.rendering.camera.CameraConfig
import org.infinite.infinite.features.rendering.camera.FreeCamera
import org.infinite.infinite.features.rendering.detailinfo.DetailInfo
import org.infinite.infinite.features.rendering.overlay.AntiOverlay
import org.infinite.infinite.features.rendering.portalgui.PortalGui
import org.infinite.infinite.features.rendering.search.BlockSearch
import org.infinite.infinite.features.rendering.sensory.ExtraSensory
import org.infinite.infinite.features.rendering.shader.SimpleShader
import org.infinite.infinite.features.rendering.sight.SuperSight
import org.infinite.infinite.features.rendering.tag.HyperTag
import org.infinite.infinite.features.rendering.ui.HyperUi
import org.infinite.infinite.features.rendering.xray.XRay

class RenderingFeatureCategory :
    FeatureCategory(
        "Rendering",
        mutableListOf(
            Feature(HyperUi()),
            Feature(AntiOverlay()),
            Feature(SuperSight()),
            Feature(XRay()),
            Feature(CameraConfig()),
            Feature(FreeCamera()), // 追加),
            Feature(SimpleShader()),
            Feature(ExtraSensory()),
            Feature(DetailInfo()),
            Feature(HyperTag()),
            Feature(PortalGui()),
            Feature(BlockSearch()),
        ),
    )
