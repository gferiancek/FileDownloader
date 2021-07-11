package com.example.filedownloader.views

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.filedownloader.R
import kotlin.properties.Delegates

class LoadingButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : View(context, attrs, defStyle) {

    private var radius = 0.0f
    private var downloadProgress = 0f

    private val mainCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.GRAY
    }
    private val progressCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        //setShadowLayer(40.0f, 0.0f, 2.0f, Color.BLACK)
    }
    private var buttonImage = ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_download_24, null)
    private val rectF = RectF()

    private var buttonState by Delegates.observable<ButtonState>(ButtonState.Disabled) {_, _, new ->
        when (new) {
            is ButtonState.Disabled -> {
                isEnabled = false
                mainCirclePaint.color = Color.GRAY
            }
            is ButtonState.Inactive -> {
                isEnabled = true
                mainCirclePaint.color = ResourcesCompat.getColor(resources, R.color.design_default_color_primary, null)
                buttonImage?.setTint(ResourcesCompat.getColor(resources, R.color.design_default_color_on_primary, null))
            }
            is ButtonState.Downloading -> {}
            is ButtonState.Completed -> {}
            is ButtonState.Failed -> {}
        }
        invalidate()
    }

    init {
        isEnabled = false
        buttonImage?.setTint(Color.LTGRAY)
    }
    override fun onDraw(canvas: Canvas) {
        val ch = height / 2.0f
        val cw = width / 2.0f
        when (buttonState) {
            is ButtonState.Disabled, is ButtonState.Inactive -> {
                canvas.drawCircle(cw, ch, radius, mainCirclePaint)
                buttonImage?.setBounds((cw - radius).toInt(), (ch - radius).toInt(), (cw + radius).toInt(), (ch + radius).toInt())
                buttonImage?.draw(canvas)
            }
            is ButtonState.Downloading -> {}
            is ButtonState.Completed -> {}
            is ButtonState.Failed -> {}
        }
        // making rectF slightly smaller than radius so that the arc is inset inside of the button.
        //rectF.set(cw - radius * 0.98f, ch - radius, cw + radius * 0.98f, ch + radius)
        //canvas.drawArc(rectF, -90f, -downloadProgress, false, progressCirclePaint)


    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // minw is set to 2x the default button size, since this view is used as a main focus of the ui
        val minw = 484
        val w: Int = resolveSizeAndState(minw, widthMeasureSpec, 1)
        val h: Int = resolveSizeAndState(
            MeasureSpec.getSize(w),
            heightMeasureSpec,
            0
        )

        radius = (w / 2.0 * 0.95).toFloat()
        // setting stroke width and shadows based on the radius so that they scale with the size of the circle.
        // radius / 25 makes them 4% the size of the radius, which seems like a good ratio.
        progressCirclePaint.strokeWidth = radius / 25
        mainCirclePaint.setShadowLayer(radius / 25, 0f, 0f, Color.BLACK)
        setMeasuredDimension(w, h)
    }

    fun setProgress(progress: Float) {
        downloadProgress = progress
        invalidate()
    }

    fun getProgress() : Float {
        return downloadProgress
    }

    fun updateButtonState(newState: ButtonState) {
        buttonState = newState
    }
}

sealed class ButtonState {
    object Disabled : ButtonState()
    object Inactive : ButtonState()
    object Downloading : ButtonState()
    object Completed : ButtonState()
    object Failed : ButtonState()
}

