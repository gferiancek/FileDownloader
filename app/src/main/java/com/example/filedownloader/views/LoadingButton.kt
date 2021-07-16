package com.example.filedownloader.views

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.withStyledAttributes
import com.example.filedownloader.R
import com.google.android.material.color.MaterialColors
import kotlin.properties.Delegates

class LoadingButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    sealed class ButtonState {
        object Disabled : ButtonState()
        object Inactive : ButtonState()
        object Downloading : ButtonState()
        object Completed : ButtonState()
        object Failed : ButtonState()
    }

    private var radius = 0.0f
    private val progressRectF = RectF()
    private var buttonIcon =
        ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_download_24, null)
    private var notAnimatingProgress = true

    private var sweepAngle = 0f
        set(sweepAngle) {
            field = sweepAngle
            invalidate()
        }
    private var downloadProgress = 0f
        set(downloadProgress) {
            field = downloadProgress
            invalidate()
        }
    private var iconAlpha: Int = 255
        set(iconAlpha) {
            field = iconAlpha
            invalidate()
        }
    private var paintColor: Int = Color.GRAY
        set(paintColor) {
            field = paintColor
            invalidate()
        }

    private var baseColor = 0
    private var completedColor = 0
    private var failedColor = 0
    private var progressTrackColor = 0
    private var progressBarColor = 0
    private var iconColor = 0

    init {
        context.withStyledAttributes(attrs, R.styleable.LoadingButton) {
            baseColor = getColor(
                R.styleable.LoadingButton_baseColor,
                MaterialColors.getColor(context, R.attr.colorPrimary, 0)
            )
            completedColor = getColor(R.styleable.LoadingButton_completedColor, Color.GREEN)
            failedColor = getColor(R.styleable.LoadingButton_failedColor, Color.RED)
            progressTrackColor = getColor(
                R.styleable.LoadingButton_progressTrackColor,
                MaterialColors.getColor(context, R.attr.colorPrimaryVariant, 0)
            )
            progressBarColor = getColor(
                R.styleable.LoadingButton_progressBarColor,
                MaterialColors.getColor(context, R.attr.colorPrimaryDark, 0)
            )
            iconColor = getColor(
                R.styleable.LoadingButton_iconColor,
                MaterialColors.getColor(context, R.attr.colorOnPrimary, 0)
            )
        }
    }

    private val mainCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = paintColor
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

    // Used to track the buttonState and update key values whenever the state is changed.
    var buttonState by Delegates.observable<ButtonState>(ButtonState.Disabled) { _, _, new ->
        when (new) {
            is ButtonState.Disabled -> {
                paintColor = Color.GRAY
            }
            is ButtonState.Inactive -> {
                isEnabled = true
                paintColor = baseColor
                buttonIcon =
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_download_24, null)
            }
            is ButtonState.Downloading -> {
                isEnabled = false
                paintColor = baseColor
                buttonIcon =
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_download_24, null)
                downloadProgress = 0f
                sweepAngle = 0f
                animateProgressTrack()
            }
            is ButtonState.Completed -> {
                isEnabled = true
                paintColor = completedColor
                buttonIcon =
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_check_24, null)
                downloadProgress = 0f
                sweepAngle = 0f
            }
            is ButtonState.Failed -> {
                isEnabled = true
                paintColor = failedColor
                buttonIcon =
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_close_24, null)
                downloadProgress = 0f
                sweepAngle = 0f
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // desiredWidth is set to the default button size
        val desiredWidth = 242
        val resolvedWidth: Int = resolveSizeAndState(desiredWidth, widthMeasureSpec, 1)
        val resolvedHeight: Int = resolveSizeAndState(
            MeasureSpec.getSize(resolvedWidth),
            heightMeasureSpec,
            0
        )
        radius = (resolvedWidth / 2.0 * 0.95).toFloat()
        // setting stroke width and shadows based on the radius so that they scale with the size of the button.
        progressBarPaint.strokeWidth = radius / 25
        progressTrackPaint.strokeWidth = radius / 25
        mainCirclePaint.setShadowLayer(radius / 25, 0f, 0f, Color.GRAY)
        setMeasuredDimension(resolvedWidth, resolvedHeight)
    }

    override fun onDraw(canvas: Canvas) {
        val centerHeight = height / 2.0f
        val centerWidth = width / 2.0f

        buttonIcon?.setBounds(
            (centerWidth - radius).toInt(),
            (centerHeight - radius).toInt(),
            (centerWidth + radius).toInt(),
            (centerHeight + radius).toInt()
        )
        buttonIcon?.alpha = iconAlpha
        mainCirclePaint.color = paintColor

        when (buttonState) {
            is ButtonState.Downloading -> {
                canvas.drawCircle(centerWidth, centerHeight, radius, mainCirclePaint)
                // * 0.98f insets the progressArcs slightly so that they are drawn inside the button
                // and don't draw over the shadows.
                progressRectF.set(
                    centerWidth - radius * 0.98f,
                    centerHeight - radius,
                    centerWidth + radius * 0.98f,
                    centerHeight + radius
                )
                canvas.drawArc(progressRectF, -90f, sweepAngle, false, progressTrackPaint)
                canvas.drawArc(progressRectF, -90f, -downloadProgress, false, progressBarPaint)
                buttonIcon?.draw(canvas)
            }
            else -> {
                canvas.drawCircle(centerWidth, centerHeight, radius, mainCirclePaint)
                buttonIcon?.draw(canvas)
            }
        }
    }

    /**
     * Function that animates the progress bar present in the ButtonState.Downloading state and advances
     * the button to the next state based on downloadProgress. (-1 = failed, 100 = completed)  I think
     * this code will technically draw the new animation over the existing one once downloadIsFinished becomes
     * true, but it shouldn't cause any performance issues. This code is also much cleaner than the alternative.
     * (Alternate solution involved saving the progressAnimator in a global cachedAnimator variable and then
     * cancelling it downloadIsFinished was true.)
     */
    fun animateProgress(currentProgress: Int) {
        when (currentProgress) {
            -1 -> animateToNewState(ButtonState.Failed)
            else -> {
                val downloadIsFinished = currentProgress == 100
                if (notAnimatingProgress ||
                    downloadIsFinished
                ) {
                    val progressAnimator = ObjectAnimator.ofFloat(
                        this,
                        "downloadProgress",
                        downloadProgress,
                        // currentProgress ranges from 0 - 100 so to convert that to a circle,
                        // we multiply by 3.6f to get a range of 0f  - 360f.
                        currentProgress * 3.6f
                    ).apply {
                        duration = 1000
                        interpolator = AccelerateDecelerateInterpolator()
                        doOnStart { notAnimatingProgress = false }
                        doOnEnd { notAnimatingProgress = true }
                    }
                    if (downloadIsFinished) {
                        progressAnimator.apply {
                            doOnEnd {
                                notAnimatingProgress = true
                                animateToNewState(ButtonState.Completed)
                            }
                        }
                    }
                    progressAnimator.start()
                }
            }
        }
    }

    /**
     * Function that compares the oldState to the passed in newState.  Fades out to a neutral state
     * (Gray background and 0 alpha button icon) before updating to the newState and then fading in to the
     * new values that are set in the buttonState by Delegates.Observable declaration near the top of the class.
     */
    fun animateToNewState(newState: ButtonState) {
        val oldState = buttonState
        if (oldState != newState) {
            createFadeOutAnimation(oldState).apply {
                duration = 250
                doOnEnd {
                    buttonState = newState
                    createFadeInAnimation().start()
                }
            }.start()
        }
    }

    /**
     * Helper function to create a FadeOutAnimation.  Depending on the oldState, we may not need to fade
     * out the icon. This creates and sets up the AnimatorSet with the ObjectAnimators needed based on the
     * oldState.
     */
    private fun createFadeOutAnimation(oldState: ButtonState): AnimatorSet {
        val fadeOutAnimatorSet = AnimatorSet()
        val colorFadeOut = ObjectAnimator.ofArgb(
            this,
            "paintColor",
            paintColor,
            Color.GRAY
        )
        when (oldState) {
            is ButtonState.Downloading, is ButtonState.Failed, is ButtonState.Completed -> {
                val iconFadeOut = ObjectAnimator.ofInt(
                    this,
                    "iconAlpha",
                    iconAlpha,
                    0
                )
                fadeOutAnimatorSet.playTogether(colorFadeOut, iconFadeOut)
            }
            else -> {
                fadeOutAnimatorSet.play(colorFadeOut)
            }
        }
        return fadeOutAnimatorSet
    }

    /**
     * Helper function to create our FadeInAnimation.  Since we are fading in to the default values
     * (255 alpha and paintColor) we don't need to check buttonState.  If we need to animate, then we run it
     * and if we don't need to running it simply won't animate anything.
     */
    private fun createFadeInAnimation(): AnimatorSet {
        val fadeInAnimatorSet = AnimatorSet()
        val colorFadeIn = ObjectAnimator.ofArgb(
            this,
            "paintColor",
            Color.GRAY,
            paintColor
        )
        val iconFadeIn = ObjectAnimator.ofInt(
            this,
            "iconAlpha",
            iconAlpha,
            255
        )
        return fadeInAnimatorSet.apply {
            playTogether(colorFadeIn, iconFadeIn)
            duration = 250
        }
    }

    /**
     * Helper function to animate the drawing on the progressTrack when starting a new download.
     */
    private fun animateProgressTrack() {
        ObjectAnimator.ofFloat(
            this,
            "sweepAngle",
            sweepAngle,
            360f
        ).apply {
            duration = 2000
            interpolator = BounceInterpolator()
        }.start()
    }
}