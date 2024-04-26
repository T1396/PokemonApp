package com.example.pokinfo.data.util

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager


class NoScrollLayoutManager(context: Context) : LinearLayoutManager(context) {
    private var isVerticalScrollEnabled = false
    private var isHorizontalScrollEnabled = false

    fun setVerticalScrollEnabled(flag: Boolean) {
        isVerticalScrollEnabled = flag
    }

    fun setHorizontalScrollEnabled(flag: Boolean) {
        isHorizontalScrollEnabled = flag
    }

    override fun canScrollVertically(): Boolean {
        return isVerticalScrollEnabled && super.canScrollVertically()
    }

    override fun canScrollHorizontally(): Boolean {
        return isHorizontalScrollEnabled && super.canScrollHorizontally()
    }
}
