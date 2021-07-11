package com.example.filedownloader.ui

import android.animation.Animator
import android.animation.ObjectAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.RadioGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
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

        binding.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            if (group.checkedRadioButtonId != -1) {
                binding.button.updateButtonState(ButtonState.Inactive)
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
                    viewModel.downloadData(url)
                }
                viewModel.onStartDownloadCompleted()
            }
        }
        viewModel.progress.observe(viewLifecycleOwner) { downloadStatus ->
            val currentProgress = calculateProgressFloat(downloadStatus)
            // Only animates the progress if:
            // A) Previous animation has finished, as noted by shouldAnimate
            // B) downloadStatus == 100, indicating the download has finished
            // C) downloadStatus == -1, indicating the download has failed.
            if (shouldAnimate ||
                downloadStatus == 100 ||
                downloadStatus == -1)
                {
                val progressAnimator = createObjectAnimator(binding.button.getProgress(), currentProgress)
                animateProgress(downloadStatus, progressAnimator)
            }
        }
        return binding.root
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
     * Helper function to create a new ObjectAnimator with the desired attributes/listeners.
     */
    private fun createObjectAnimator(
        cachedProgress: Float,
        currentProgress: Float
    ): ObjectAnimator {
        return ObjectAnimator.ofFloat(
            binding.button,
            "progress",
            cachedProgress,
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
                }

                override fun onAnimationCancel(animation: Animator?) {
                }

                override fun onAnimationRepeat(animation: Animator?) {
                }
            })
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
            val notificationManager = requireActivity().getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}