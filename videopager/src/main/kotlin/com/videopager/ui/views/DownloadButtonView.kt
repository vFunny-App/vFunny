package com.videopager.ui.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.animation.AccelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import com.videopager.R

class DownloadButtonView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private var isExpanded = false
    private val itemList: MutableList<Item> = mutableListOf()
    private val contentView: LinearLayout
    private val expandButton: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.expandable_button_view, this, true)
        orientation = VERTICAL

        contentView = findViewById(R.id.content_view)
        expandButton = findViewById(R.id.expand_button)

        expandButton.setOnClickListener {
            toggleExpansion()
        }
    }

    private fun toggleExpansion() {
        isExpanded = !isExpanded

        val startHeight = if (isExpanded) 0 else contentView.measuredHeight
        val endHeight = if (isExpanded) contentView.measuredHeight else 0

        val animator = ValueAnimator.ofInt(startHeight, endHeight)
        animator.interpolator = AccelerateInterpolator()
        animator.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int
            val layoutParams = contentView.layoutParams
            layoutParams.height = value
            contentView.layoutParams = layoutParams
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (isExpanded) {
                    displayItems()
                } else {
                    contentView.removeAllViews()
                }
            }
        })
        animator.duration = 200
        animator.start()
    }

    private fun displayItems() {
        contentView.removeAllViews()

        Log.e("TAG", "displayItems: SHOWING TOTAL : $itemList ", )

        for (item in itemList) {
            val itemView = LayoutInflater.from(context).inflate(R.layout.item_view, contentView, false)
            val textView1 = itemView.findViewById<TextView>(R.id.text_view_1)
            val textView2 = itemView.findViewById<TextView>(R.id.text_view_2)
            textView1.text = item.text1
            textView2.text = item.text2
            contentView.addView(itemView)
        }
    }

    fun setItems(items: List<Item>) {
        itemList.clear()
        itemList.addAll(items)
    }

    data class Item(val text1: String, val text2: String)
}
