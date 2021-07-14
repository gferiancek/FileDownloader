package com.example.filedownloader.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.filedownloader.R
import com.example.filedownloader.databinding.FragmentDownloadBinding
import com.example.filedownloader.viewmodel.DownloadViewModel
import com.example.filedownloader.views.LoadingButton

class DownloadFragment : Fragment() {

    private lateinit var binding: FragmentDownloadBinding
    private val viewModel by activityViewModels<DownloadViewModel>()

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
            val buttonState = binding.button.getState()
            if (checkedId != -1 &&
                buttonState != LoadingButton.ButtonState.Inactive &&
                buttonState != LoadingButton.ButtonState.Downloading
            ) {
                binding.button.animateToNewState(LoadingButton.ButtonState.Inactive)
            }
        }

        viewModel.eventStartDownload.observe(viewLifecycleOwner) { isDownloading ->
            if (isDownloading) {
                val url = when (binding.radioGroup.checkedRadioButtonId) {
                    binding.mb100.id -> binding.mb100.tag.toString()
                    else -> binding.gb1.tag.toString()
                }
                binding.button.updateButtonState(LoadingButton.ButtonState.Downloading)
                viewModel.downloadData(url)
                viewModel.onStartDownloadCompleted()
            }
        }
        viewModel.progress.observe(viewLifecycleOwner) { downloadStatus ->
            binding.button.animateProgress(downloadStatus)
        }

        return binding.root
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
}