package com.example.filedownloader.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

        viewModel.eventReceived.observe(viewLifecycleOwner) { string ->
            if (string.isNotBlank()) {
                Toast.makeText(requireContext(), string, Toast.LENGTH_SHORT).show()
                viewModel.onReceivedCompleted()
            }
        }

        // Inflate the layout for this fragment
        return binding.root
    }
}