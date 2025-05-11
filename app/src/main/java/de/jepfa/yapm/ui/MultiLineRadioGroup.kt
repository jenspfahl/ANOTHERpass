package de.jepfa.yapm.ui

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.widget.RadioGroup
import kotlin.math.max
import kotlin.math.min


// Taken from https://github.com/jevonbeck/AbstractMachine/blob/jevon_dev/app/src/main/java/org/ricts/abstractmachine/ui/utils/MultiLineRadioGroup.java
class MultiLineRadioGroup @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null
) :
    RadioGroup(context, attrs) {
    private val viewRectMap: MutableMap<View, Rect> =
        HashMap()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasurement = MeasureSpec.getSize(widthMeasureSpec)
        var heightMeasurement = MeasureSpec.getSize(heightMeasureSpec)
        when (orientation) {
            HORIZONTAL -> heightMeasurement =
                findHorizontalHeight(widthMeasureSpec, heightMeasureSpec)

            VERTICAL -> widthMeasurement = findVerticalWidth(widthMeasureSpec, heightMeasureSpec)
        }
        setMeasuredDimension(widthMeasurement, heightMeasurement)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val count = childCount
        for (x in 0 until count) {
            val button = getChildAt(x)
            val dims = viewRectMap[button]
            button.layout(dims!!.left, dims.top, dims.right, dims.bottom)
        }
    }

    private fun findHorizontalHeight(widthMeasureSpec: Int, heightMeasureSpec: Int): Int {
        val parentHeight = MeasureSpec.getSize(heightMeasureSpec)
        val maxRight = MeasureSpec.getSize(widthMeasureSpec) - paddingRight

        // create MeasureSpecs to accommodate max space that RadioButtons can occupy
        val newWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
            maxRight - paddingLeft,
            MeasureSpec.getMode(widthMeasureSpec)
        )
        val newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
            parentHeight - (paddingTop + paddingBottom),
            MeasureSpec.getMode(heightMeasureSpec)
        )

        var nextLeft = paddingLeft
        var nextTop = paddingTop
        var maxRowHeight = 0
        viewRectMap.clear()
        // measure and find placement for each RadioButton (results to be used in onLayout() stage)
        val count = childCount
        for (x in 0 until count) {
            val button = getChildAt(x)
            measureChild(button, newWidthMeasureSpec, newHeightMeasureSpec)

            maxRowHeight =
                max(maxRowHeight.toDouble(), button.measuredHeight.toDouble()).toInt()

            // determine RadioButton placement
            var nextRight = nextLeft + button.measuredWidth
            if (nextRight > maxRight) { // if current button will exceed border on this row ...
                // ... move to next row
                nextLeft = paddingLeft
                nextTop += maxRowHeight

                // adjust for next row values
                nextRight = nextLeft + button.measuredWidth
                maxRowHeight = button.measuredHeight
            }

            val nextBottom = nextTop + button.measuredHeight
            viewRectMap[button] = Rect(nextLeft, nextTop, nextRight, nextBottom)

            // update nextLeft
            nextLeft = nextRight
        }

        // height of RadioGroup is a natural by-product of placing all the children
        val idealHeight = nextTop + maxRowHeight + paddingBottom
        return when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.UNSPECIFIED -> idealHeight
            MeasureSpec.AT_MOST -> min(idealHeight.toDouble(), parentHeight.toDouble())
                .toInt()

            MeasureSpec.EXACTLY -> parentHeight
            else -> parentHeight
        }
    }

    private fun findVerticalWidth(widthMeasureSpec: Int, heightMeasureSpec: Int): Int {
        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val maxBottom = MeasureSpec.getSize(heightMeasureSpec) - paddingBottom

        // create MeasureSpecs to accommodate max space that RadioButtons can occupy
        val newWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
            parentWidth - (paddingLeft + paddingRight),
            MeasureSpec.getMode(widthMeasureSpec)
        )
        val newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
            maxBottom - paddingTop,
            MeasureSpec.getMode(heightMeasureSpec)
        )

        var nextTop = paddingTop
        var nextLeft = paddingLeft
        var maxColWidth = 0
        viewRectMap.clear()
        // measure and find placement for each RadioButton (results to be used in onLayout() stage)
        val count = childCount
        for (x in 0 until count) {
            val button = getChildAt(x)
            measureChild(button, newWidthMeasureSpec, newHeightMeasureSpec)

            maxColWidth =
                max(maxColWidth.toDouble(), button.measuredWidth.toDouble()).toInt()

            // determine RadioButton placement
            var nextBottom = nextTop + button.measuredHeight
            if (nextBottom > maxBottom) { // if current button will exceed border for this column ...
                // ... move to next column
                nextTop = paddingTop
                nextLeft += maxColWidth

                // adjust for next row values
                nextBottom = nextTop + button.measuredHeight
                maxColWidth = button.measuredWidth
            }

            val nextRight = nextLeft + button.measuredWidth
            viewRectMap[button] = Rect(nextLeft, nextTop, nextRight, nextBottom)

            // update nextTop
            nextTop = nextBottom
        }

        // width of RadioGroup is a natural by-product of placing all the children
        val idealWidth = nextLeft + maxColWidth + paddingRight
        return when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.UNSPECIFIED -> idealWidth
            MeasureSpec.AT_MOST -> min(idealWidth.toDouble(), parentWidth.toDouble())
                .toInt()

            MeasureSpec.EXACTLY -> parentWidth
            else -> parentWidth
        }
    }
}