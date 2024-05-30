package com.example.pokinfo.adapter.decoration

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class VerticalSpaceItemDecoration(context: Context, dp: Int) : RecyclerView.ItemDecoration() {
    private val verticalSpaceHeight = context.dpToPx(dp)
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.bottom = verticalSpaceHeight
    }
}

fun Context.dpToPx(dp: Int): Int {
    return (dp * resources.displayMetrics.density).toInt()
}

class StartEndDecoration(context: Context, private val marginStartEnd: Int) : RecyclerView.ItemDecoration() {
    private val horizontalSpaceWidth = context.dpToPx(marginStartEnd)
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        val totalCount = parent.adapter?.itemCount ?: 0

        // Setze den linken Rand für das erste Element
        if (position == 0) {
            outRect.left = horizontalSpaceWidth
        }

        // Setze den rechten Rand für das letzte Element
        if (position == totalCount - 1) {
            outRect.right = horizontalSpaceWidth
        }
    }
}
