package com.example.filedownloader.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.filedownloader.R
import com.example.filedownloader.databinding.FragmentDetailBinding
import com.example.filedownloader.model.DownloadedFile

class DetailFragment : Fragment() {

    private lateinit var binding: FragmentDetailBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_detail,
            container,
            false
        )
        // Inflate the layout for this fragment
        val downloadedFile = requireArguments().getParcelable<DownloadedFile>("downloadedFile")
        binding.downloadedFile = downloadedFile
        binding.motionLayout.addTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {}
            override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {}

            /**
             * We draw a "fake" DownloadFragment layout and use MotionLayout to transition to it. Once
             * that transition is finished, we can navigate to the DownloadFragment with no animations
             * and there is no visible proof that we switched fragments. Gives the illusion of a
             * "Swipe to animate to new fragment" behaviour.  (If you have any feedback on how to really do this,
             * then I would love to hear about it!)
             */
            override fun onTransitionCompleted(p0: MotionLayout?, p1: Int) {
                if (p1 == R.id.close) findNavController().navigate(DetailFragmentDirections.actionDetailFragmentToDownloadFragment())
            }

            override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {}
        })

        return binding.root
    }
}