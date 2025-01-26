package de.jepfa.yapm.ui

import android.view.animation.Animation
import android.view.animation.Transformation
import com.google.android.material.progressindicator.CircularProgressIndicator

class ProgressCircleAnimation(
    private val progressCircle: CircularProgressIndicator,
    private val from: Float,
    private val to: Float
) :
    Animation() {
    override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
        super.applyTransformation(interpolatedTime, t)
        val value = from + (to - from) * interpolatedTime
        progressCircle.progress = value.toInt()
    }

}