package com.pusacoffee.pusatrack.ui.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pusacoffee.pusatrack.data.local.SessionManager
import com.pusacoffee.pusatrack.data.repository.AccountRepository
import com.pusacoffee.pusatrack.data.repository.RepoResult
import com.pusacoffee.pusatrack.databinding.FragmentEditAccountBinding

class EditAccountFragment : Fragment() {

    private var _binding: FragmentEditAccountBinding? = null
    private val binding get() = _binding!!

    private var targetUserId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        targetUserId = arguments?.getInt("userId", -1) ?: -1
        val user = AccountRepository.getUserById(targetUserId)
        if (user == null) {
            findNavController().navigateUp()
            return
        }

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.etFullName.setText(user.fullName)
        binding.etUsername.setText(user.username)

        binding.etFullName.doAfterTextChanged { binding.layoutErrorBanner.isVisible = false }
        binding.etUsername.doAfterTextChanged { binding.layoutErrorBanner.isVisible = false }

        binding.btnSaveAccount.setOnClickListener {
            val fullName = binding.etFullName.text?.toString().orEmpty()
            val username = binding.etUsername.text?.toString().orEmpty()

            when (val result = AccountRepository.updateAccount(targetUserId, fullName, username)) {
                is RepoResult.Success -> {
                    val session = SessionManager(requireContext())
                    if (session.getUserId() == targetUserId) {
                        session.saveSession(result.data)
                    }
                    findNavController().navigateUp()
                }

                is RepoResult.Error -> {
                    binding.tvErrorMessage.text = result.message
                    binding.layoutErrorBanner.isVisible = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
