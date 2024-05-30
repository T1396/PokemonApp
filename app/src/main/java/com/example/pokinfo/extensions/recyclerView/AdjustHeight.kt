package com.example.pokinfo.extensions.recyclerView

import androidx.recyclerview.widget.RecyclerView
import com.example.pokinfo.extensions.dpToPx

fun RecyclerView.adjustHeight(maxItems: Int) {
    this.post {
        val totalItemCount = this.adapter?.itemCount ?: 0
        if (totalItemCount == 0) {
            this.layoutParams = this.layoutParams.apply {
                height = 0
            }
            return@post
        }
        val oneItemHeight = (this.getChildAt(0)?.measuredHeight) ?: 0
        val totalHeight = (oneItemHeight * totalItemCount.coerceAtMost(maxItems)) +
                (6 * dpToPx(this.context, totalItemCount.coerceAtMost(maxItems)))

        this.layoutParams = this.layoutParams.apply {
            height = totalHeight
        }
    }
}

