package de.jepfa.yapm.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.ExpandableListView

/*
Taken from https://stackoverflow.com/questions/37605545/android-nestedscrollview-which-contains-expandablelistview-doesnt-scroll-when/37605908
 */
class NonScrollExpandableListView : ExpandableListView {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightMeasureSpec = MeasureSpec.makeMeasureSpec(
            Int.MAX_VALUE shr 2, MeasureSpec.AT_MOST
        )
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val params = layoutParams
        params.height = measuredHeight
    }
}