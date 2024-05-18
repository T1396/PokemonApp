package com.example.pokinfo.ui.Extensions.animations

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import androidx.fragment.app.Fragment

fun Fragment.showOrHideChipGroupAnimated(scrollView: HorizontalScrollView, shouldExpand: Boolean) {
    if (shouldExpand) {
        scrollView.visibility = View.VISIBLE
        val targetHeight = ViewGroup.LayoutParams.WRAP_CONTENT
        val startHeight = 0
        val animation = ValueAnimator.ofInt(startHeight, targetHeight).apply {
            addUpdateListener { valueAnimator ->
                val value = valueAnimator.animatedValue as Int
                val layoutParams = scrollView.layoutParams
                layoutParams.height = value
                scrollView.layoutParams = layoutParams
            }
            duration = 200
        }
        animation.start()
    } else {
        val fadeOut = ObjectAnimator.ofFloat(scrollView, "alpha", 1f, 0f).apply {
            duration = 100
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    scrollView.visibility = View.GONE
                    scrollView.alpha = 1f
                }
            })
        }
        scrollView.layoutParams.height = 0
        scrollView.layoutParams = scrollView.layoutParams
        fadeOut.start()
    }
}