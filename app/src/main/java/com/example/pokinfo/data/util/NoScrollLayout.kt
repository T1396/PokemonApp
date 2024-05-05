package com.example.pokinfo.data.util

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager


class NoScrollLayoutManager : LinearLayoutManager {

    constructor(context: Context) : super(context)

    constructor(context: Context, orientation: Int, reverseLayout: Boolean) : super(context, orientation, reverseLayout)

    // Hinzufügen des Konstruktors, der von der XML-Inflation benötigt wird
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

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

