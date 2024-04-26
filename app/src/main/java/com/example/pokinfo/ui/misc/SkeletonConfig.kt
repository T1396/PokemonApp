package com.example.pokinfo.ui.misc

import android.graphics.Color
import com.faltenreich.skeletonlayout.SkeletonConfig
import com.faltenreich.skeletonlayout.mask.SkeletonShimmerDirection

object SkeletonConf {
    val darkMode: SkeletonConfig = SkeletonConfig(
        maskCornerRadius = 60f,
        maskColor = Color.argb(255, 38, 38, 38),
        showShimmer = true,
        shimmerColor = Color.argb(255, 50, 50, 50),
        shimmerDurationInMillis = 750,
        shimmerDirection = SkeletonShimmerDirection.LEFT_TO_RIGHT,
        shimmerAngle = 10
    )
    val whiteMode: SkeletonConfig = SkeletonConfig(
        maskCornerRadius = 60f,
        maskColor = Color.argb(255, 255, 255, 255), // Heller Hintergrund
        showShimmer = true,
        shimmerColor = Color.argb(255, 220, 220, 220), // Hellerer Schimmer
        shimmerDurationInMillis = 750,
        shimmerDirection = SkeletonShimmerDirection.LEFT_TO_RIGHT,
        shimmerAngle = 10
    )

}