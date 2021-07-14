package com.example.filedownloader.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.filedownloader.R
import com.example.filedownloader.databinding.FragmentDownloadBinding
import com.example.filedownloader.viewmodel.DownloadViewModel
import com.example.filedownloader.views.ButtonState

class DownloadFragment : Fragment() {

    private lateinit var binding: FragmentDownloadBinding
    private val viewModel by activityViewModels<DownloadViewModel>()
    private var cachedAnimator = ObjectAnimator()
    private var shouldAnimate = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_download,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        createChannel(
            getString(R.string.download_complete_channel_id),
            getString(R.string.download_complete_channel_name)
        )

        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId != -1 && binding.button.getState() != ButtonState.Inactive) {
                animateState(ButtonState.Inactive)
            }
        }

        viewModel.eventStartDownload.observe(viewLifecycleOwner) { isDownloading ->
            if (isDownloading) {
                val url = when (binding.radioGroup.checkedRadioButtonId) {
                    binding.mb100.id -> binding.mb100.tag.toString()
                    binding.gb1.id -> binding.gb1.tag.toString()
                    else -> ""
                }
                if (url.isNotBlank()) {
                    binding.button.updateButtonState(ButtonState.Downloading)
                    viewModel.downloadData(url)
                }
                viewModel.onStartDownloadCompleted()
            }
        }
        viewModel.progress.observe(viewLifecycleOwner) { downloadStatus ->
            val currentProgress = calculateProgressFloat(downloadStatus)

            if (shouldAnimate ||
                    downloadStatus == 100) {
                val progressAnimator = ObjectAnimator.ofFloat(
                    binding.button,
                    "progress",
                    binding.button.getProgress(),
                    currentProgress
                ).apply {
                    duration = 3000
                    interpolator = DecelerateInterpolator()
                    addListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator?) {
                            shouldAnimate = false
                        }

                        override fun onAnimationEnd(animation: Animator?) {
                            shouldAnimate = true
                            if (downloadStatus == 100) {
                                animateState(ButtonState.Completed)
                            }
                        }

                        override fun onAnimationCancel(animation: Animator?) {
                        }

                        override fun onAnimationRepeat(animation: Animator?) {
                        }
                    })
                }
                animateProgress(downloadStatus, progressAnimator)
            } else if (downloadStatus == -1) {
                animateState(ButtonState.Failed)
            }
        }
        return binding.root
    }

    private fun animateState(newState: ButtonState) {
        val oldColor = binding.button.getButtonColor()
        val imageFadeOut = ObjectAnimator.ofInt(binding.button, "buttonAlpha", 255, 0).apply {
            duration = 500
        }
        val colorFadeOut =
            ObjectAnimator.ofArgb(binding.button, "buttonColor", oldColor, Color.GRAY).apply {
                duration = 500
            }
        AnimatorSet().apply {
            playTogether(imageFadeOut, colorFadeOut)
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                }

                override fun onAnimationEnd(animation: Animator?) {
                    binding.button.updateButtonState(newState)
                    val newColor = binding.button.getButtonColor()
                    val imageFadeIn =
                        ObjectAnimator.ofInt(binding.button, "buttonAlpha", 0, 255).apply {
                            duration = 500
                        }
                    val colorFadeIn =
                        ObjectAnimator.ofArgb(binding.button, "buttonColor", Color.GRAY, newColor)
                            .apply {
                                duration = 500
                                disableViewDuringRotation(binding.button)
                            }
                    AnimatorSet().apply {
                        playTogether(colorFadeIn, imageFadeIn)
                    }.start()
                }

                override fun onAnimationCancel(animation: Animator?) {
                }

                override fun onAnimationRepeat(animation: Animator?) {
                }

            })
        }.start()
    }

    /**
     * Function that takes the downloadStatus and the progressAnimator to create and start the animation
     * based on the download status.  Originally, a global progressAnimator was used, but that caused the animation
     * to delay significantly (only showed ~10% done when the download was near 90% finished in the notification).
     * Creating a new animator every pass solved that issue, but also required that we cache the previous animation.
     *
     * progressAnimator has a duration of 3000ms to give it more of a constant animation, instead of being stop/start,
     * but because of that, it can sometimes lag behind and show 100% completed late.  To fix that, we cancel the
     * cachedAnimator, change the duration on progressAnimator to 500ms, and quickly animate from the last state
     * to 100%.
     */
    private fun animateProgress(
        downloadStatus: Int,
        progressAnimator: ObjectAnimator
    ) {
        when (downloadStatus) {
            100 -> {
                cachedAnimator.cancel()
                progressAnimator.duration = 500
                progressAnimator.start()
            }
            else -> {
                progressAnimator.start()
                cachedAnimator = progressAnimator
            }
        }
    }

    /**
     * Since the LoadingButton uses an Arc for the progress indicator, we convert the progress Int
     * (which ranges from -1 - 100) to the equivalent range of 0 - 360 for an arc.  Simple to do by multiplying
     * the progress int by 3.6f. (100 : 360 is a 1 : 3.6 ratio)
     */
    private fun calculateProgressFloat(progress: Int): Float {
        return when (progress) {
            -1, 0 -> {
                // if -1 or 0, the download has failed or hasn't started.  Initilize the button to 0 progress
                // TODO Implement button state and update button progress to 0 when switched to loading state
                binding.button.setProgress(0f)
                0f
            }
            else -> progress * 3.6f
        }
    }

    private fun createChannel(id: String, name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                id,
                name,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                lightColor = Color.MAGENTA
                enableVibration(true)
                description = getString(R.string.download_complete_channel_description)
            }
            val notificationManager =
                requireActivity().getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun ObjectAnimator.disableViewDuringRotation(view: View) {
        addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                view.isEnabled = false
            }

            override fun onAnimationEnd(animation: Animator?) {
                view.isEnabled = true
            }
        })
    }
}