package com.example.filedownloader.views

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.withStyledAttributes
import com.example.filedownloader.R
import com.google.android.material.color.MaterialColors
import kotlin.properties.Delegates

class LoadingButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : View(context, attrs, defStyle) {

    private var radius = 0.0f
    private var downloadProgress = 0f
    private var buttonImage = ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_download_24, null)

    private var buttonColor = 0
    private var completedColor = 0
    private var failedColor = 0
    private var progressTrackColor = 0
    private var progressBarColor = 0
    private var iconColor = 0


    init {
        isEnabled = false

        context.withStyledAttributes(attrs, R.styleable.LoadingButton) {
            buttonColor = getColor(R.styleable.LoadingButton_buttonColor, MaterialColors.getColor(context, R.attr.colorPrimary, 0))
            completedColor = getColor(R.styleable.LoadingButton_completedColor, Color.GREEN)
            failedColor = getColor(R.styleable.LoadingButton_failedColor, Color.RED)
            progressTrackColor = getColor(R.styleable.LoadingButton_progressTrackColor, MaterialColors.getColor(context, R.attr.colorPrimaryVariant, 0))
            progressBarColor = getColor(R.styleable.LoadingButton_progressBarColor, MaterialColors.getColor(context, R.attr.colorPrimaryDark, 0))
            iconColor = getColor(R.styleable.LoadingButton_iconColor, MaterialColors.getColor(context, R.attr.colorOnPrimary, 0))
        }
    }

    private val mainCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.GRAY
    }
    private val progressBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = progressBarColor
    }
    private val progressTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = progressTrackColor
    }


    private val rectF = RectF()

    private var buttonState by Delegates.observable<ButtonState>(ButtonState.Disabled) {_, _, new ->
        when (new) {
            is ButtonState.Disabled -> {
                isEnabled = false
                mainCirclePaint.color = Color.GRAY
            }
            is ButtonState.Inactive -> {
                isEnabled = true
                mainCirclePaint.color = buttonColor
                buttonImage = ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_download_24, null)
            }
            is ButtonState.Downloading -> {
                isEnabled = false
                mainCirclePaint.color = buttonColor
                buttonImage = ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_download_24, null)
            }
            is ButtonState.Completed -> {
                isEnabled = true
                mainCirclePaint.color = completedColor
                buttonImage = ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_check_24, null)
            }
            is ButtonState.Failed -> {
                isEnabled = true
                mainCirclePaint.color = failedColor
                buttonImage = ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_close_24, null)
            }
        }
    }


    override fun onDraw(canvas: Canvas) {
        val ch = height / 2.0f
        val cw = width / 2.0f

        buttonImage?.setBounds((cw - radius).toInt(), (ch - radius).toInt(), (cw + radius).toInt(), (ch + radius).toInt())

        when (buttonState) {
            is ButtonState.Disabled, is ButtonState.Inactive -> {
                canvas.drawCircle(cw, ch, radius, mainCirclePaint)
                buttonImage?.draw(canvas)
            }
            is ButtonState.Downloading -> {
                canvas.drawCircle(cw, ch, radius, mainCirclePaint)
                rectF.set(cw - radius * 0.98f, ch - radius, cw + radius * 0.98f, ch + radius)
                canvas.drawArc(rectF, 0f, 360f, false, progressTrackPaint)
                canvas.drawArc(rectF, -90f, -downloadProgress, false, progressBarPaint)
                buttonImage?.draw(canvas)
            }
            is ButtonState.Completed, is ButtonState.Failed -> {
                canvas.drawCircle(cw, ch, radius, mainCirclePaint)
                buttonImage?.draw(canvas)
            }
        }
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
        progressBarPaint.strokeWidth = radius / 25
        progressTrackPaint.strokeWidth = radius / 25
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

    fun getButtonColor() : Int {
        return mainCirclePaint.color
    }

    fun setButtonColor(color: Int) {
        mainCirclePaint.color = color
        invalidate()
    }

    fun getButtonAlpha() : Int? {
        return buttonImage?.alpha
    }

    fun setButtonAlpha(alpha: Int) {
        buttonImage?.alpha = alpha
        invalidate()
    }

    fun getState() : ButtonState {
        return buttonState
    }
}

sealed class ButtonState {
    object Disabled : ButtonState()
    object Inactive : ButtonState()
    object Downloading : ButtonState()
    object Completed : ButtonState()
    object Failed : ButtonState()
}

