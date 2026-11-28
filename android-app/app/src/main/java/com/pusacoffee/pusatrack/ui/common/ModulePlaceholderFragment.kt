package com.pusacoffee.pusatrack.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pusacoffee.pusatrack.databinding.FragmentPlaceholderBinding

open class ModulePlaceholderFragment : Fragment() {

    private var _binding: FragmentPlaceholderBinding? = null
    protected val binding get() = _binding!!

    protected open val defaultTitle: String = "Modul"
    protected open val defaultModuleCode: String = "PUSATRACK"
    protected open val defaultShowBackButton: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaceholderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString("title") ?: defaultTitle
        val moduleCode = arguments?.getString("moduleCode") ?: defaultModuleCode
        val showBack = arguments?.getBoolean("showBackButton", defaultShowBackButton)
            ?: defaultShowBackButton

        binding.tvTitle.text = title
        binding.tvModuleCode.text = moduleCode
        binding.btnBack.isVisible = showBack
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
