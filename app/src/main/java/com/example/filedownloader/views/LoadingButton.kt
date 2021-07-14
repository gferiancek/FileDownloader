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

    // 360f = 100% downloaded.  By initializing to this value, it adds a nice animation on the progress
    // bar when starting a download.  (Animates from 360f to 0f and is just a little visual flair while the download starts.)
    private var downloadProgress = 360f
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
            mainCirclePaint.color = paintColor
            invalidate()
        }

    private var baseColor = 0
    private var completedColor = 0
    private var failedColor = 0
    private var progressTrackColor = 0
    private var progressBarColor = 0
    private var iconColor = 0

    init {
        isEnabled = false

        context.withStyledAttributes(attrs, R.styleable.LoadingButton) {
            baseColor = getColor(
                R.styleable.LoadingButton_buttonColor,
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

    // Used to track the buttonState and update key values whenever the state is changed.  Only the required
    // values are changed (e.g. going from Inactive -> Downloading will use the same paintColor and buttonIcon, so
    // there is no need to update them and you can only get to the Downloading state from the Inactive State.)
    private var buttonState by Delegates.observable<ButtonState>(ButtonState.Disabled) { _, _, new ->
        when (new) {
            is ButtonState.Disabled -> {
                isEnabled = false
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
            }
            is ButtonState.Completed -> {
                isEnabled = true
                paintColor = completedColor
                buttonIcon =
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_check_24, null)
                // reinitializing to 100% for animation on next download attempt.
                downloadProgress = 360f
            }
            is ButtonState.Failed -> {
                isEnabled = true
                paintColor = failedColor
                buttonIcon =
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_close_24, null)
                // reinitializing to 100% for animation on next download attempt.
                downloadProgress = 360f
            }
        }
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
                progressRectF.set(
                    centerWidth - radius * 0.98f,
                    centerHeight - radius,
                    centerWidth + radius * 0.98f,
                    centerHeight + radius
                )
                canvas.drawArc(progressRectF, 0f, 360f, false, progressTrackPaint)
                canvas.drawArc(progressRectF, -90f, -downloadProgress, false, progressBarPaint)
                buttonIcon?.draw(canvas)
            }
            else -> {
                canvas.drawCircle(centerWidth, centerHeight, radius, mainCirclePaint)
                buttonIcon?.draw(canvas)
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
        mainCirclePaint.setShadowLayer(radius / 25, 0f, 0f, Color.GRAY)
        setMeasuredDimension(w, h)
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
            val fadeOutAnimatorSet = AnimatorSet()
            val colorFadeOut =
                ObjectAnimator.ofArgb(
                    this,
                    "paintColor",
                    paintColor,
                    Color.GRAY
                )

            when (oldState) {
                is ButtonState.Downloading, is ButtonState.Failed, is ButtonState.Completed -> {
                    // While in these three states any state change will result in a new icon being displayed
                    // So we add a fade out animation for the button icon.
                    val iconFadeOut = ObjectAnimator.ofInt(
                        this,
                        "iconAlpha",
                        iconAlpha,
                        0
                    )
                    fadeOutAnimatorSet.playTogether(colorFadeOut, iconFadeOut)
                }
                else -> {
                    // Icon doesn't change in this scenario so we do not play the iconFadeOut animation
                    fadeOutAnimatorSet.play(colorFadeOut)
                }
            }
            fadeOutAnimatorSet.apply {
                duration = 500
                // We create the fadeIn animations inside of the fadeOutAnimatorSet's doOnEnd since we rely
                // on updating the buttonState to give us access to the new values to fade into and only want
                // to update the buttonState once the fadeOutAnimatorSet has completed.
                doOnEnd {
                    buttonState = newState
                    val fadeInAnimatorSet = AnimatorSet()
                    val colorFadeIn = ObjectAnimator.ofArgb(
                        this@LoadingButton,
                        "paintColor",
                        Color.GRAY,
                        paintColor
                    )
                    // If we played the iconFadeOut animation, this will animate from 0 to 255 and if we didn't, we're simply
                    // "animating" from 255 to 255 so there is no need to check the ButtonState before playing this animation.
                    val iconFadeIn =
                        ObjectAnimator.ofInt(
                            this@LoadingButton,
                            "iconAlpha",
                            iconAlpha,
                            255
                        )
                    fadeInAnimatorSet.apply {
                        playTogether(colorFadeIn, iconFadeIn)
                        duration = 500
                    }.start()
                }
            }.start()
        }
    }

    fun updateButtonState(newState: ButtonState) {
        buttonState = newState
    }

    fun getState(): ButtonState {
        return buttonState
    }
}