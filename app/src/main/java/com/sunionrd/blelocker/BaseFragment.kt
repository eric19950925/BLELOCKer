package com.sunionrd.blelocker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BaseFragment: Fragment() {
    protected abstract fun getLayoutRes(): Int?

    protected abstract fun getLayoutBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding?
    private var _binding:ViewBinding?= null
    private val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = getLayoutBinding(inflater,container)
        return binding?.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onViewHasCreated()
//        (requireActivity() as AppCompatActivity).apply {
//            // Redirect system "Back" press to our dispatcher
//            onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedDispatcher)
//        }
//        Log.d("TAG","onViewCreated")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    abstract fun onViewHasCreated()
    abstract fun onBackPressed()
    fun reStartActivity() = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
        val intent = requireActivity().intent
        intent.addFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_NO_ANIMATION
        )
        requireActivity().overridePendingTransition(0, 0)
        requireActivity().finish()

        requireActivity().overridePendingTransition(0, 0)
        startActivity(intent)
    }
//    private val backPressedDispatcher = object : OnBackPressedCallback(true) {
//        override fun handleOnBackPressed() {
//            // Redirect to our own function
//            this@BaseFragment.onBackPressed()
//        }
//    }
}