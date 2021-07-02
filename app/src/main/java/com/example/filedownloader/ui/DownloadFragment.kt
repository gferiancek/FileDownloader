package com.example.filedownloader.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.example.filedownloader.R
import com.example.filedownloader.databinding.FragmentDownloadBinding
import com.example.filedownloader.viewmodel.DownloadViewModel

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

        viewModel.eventStartDownload.observe(viewLifecycleOwner) { isDownloading ->
            if (isDownloading) {
                val url = when (binding.radioGroup.checkedRadioButtonId) {
                    binding.mb100.id -> binding.mb100.tag.toString()
                    binding.gb1.id -> binding.gb1.tag.toString()
                    else -> {
                        Toast.makeText(requireContext(), "Please select a file to download", Toast.LENGTH_SHORT).show()
                        // return "" as the url since no download is selected
                        ""
                    }
                }
                if (url.isNotBlank()) {
                    viewModel.downloadData(url)
                }
                viewModel.onStartDownloadCompleted()
            }
        }

        // Inflate the layout for this fragment
        return binding.root
    }

    private fun createChannel(id: String, name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                id,
                name,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.apply {
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