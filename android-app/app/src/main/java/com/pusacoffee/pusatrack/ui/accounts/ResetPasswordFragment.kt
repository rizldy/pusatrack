package com.pusacoffee.pusatrack.ui.accounts

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.local.SessionManager
import com.pusacoffee.pusatrack.data.repository.AccountRepository
import com.pusacoffee.pusatrack.data.repository.RepoResult
import com.pusacoffee.pusatrack.databinding.FragmentResetPasswordBinding

class ResetPasswordFragment : Fragment() {

    private var _binding: FragmentResetPasswordBinding? = null
    private val binding get() = _binding!!

    private var targetUserId: Int = -1
    private var newVisible = false
    private var confirmVisible = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResetPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        targetUserId = arguments?.getInt("userId", -1) ?: -1
        val targetUser = AccountRepository.getUserById(targetUserId)
        if (targetUser == null) {
            findNavController().navigateUp()
            return
        }

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.tvTargetUser.text = "${targetUser.fullName} (@${targetUser.username})"

        binding.etNewPassword.setOnFocusChangeListener { _, hasFocus ->
            binding.containerNewPassword.isActivated = hasFocus
        }
        binding.etConfirmPassword.setOnFocusChangeListener { _, hasFocus ->
            binding.containerConfirmPassword.isActivated = hasFocus
        }

        binding.etNewPassword.doAfterTextChanged { binding.layoutErrorBanner.isVisible = false }
        binding.etConfirmPassword.doAfterTextChanged { binding.layoutErrorBanner.isVisible = false }

        binding.btnToggleNewPassword.setOnClickListener {
            newVisible = !newVisible
            applyVisibility(binding.etNewPassword, binding.btnToggleNewPassword, newVisible)
        }
        binding.btnToggleConfirmPassword.setOnClickListener {
            confirmVisible = !confirmVisible
            applyVisibility(binding.etConfirmPassword, binding.btnToggleConfirmPassword, confirmVisible)
        }

        binding.btnProceedVerify.setOnClickListener {
            val newPass = binding.etNewPassword.text?.toString().orEmpty()
            val confirmPass = binding.etConfirmPassword.text?.toString().orEmpty()
            val validationError = AccountRepository.validatePasswordPair(newPass, confirmPass)
            if (validationError != null) {
                binding.tvErrorMessage.text = validationError
                binding.layoutErrorBanner.isVisible = true
                return@setOnClickListener
            }

            VerifyPasswordDialogFragment()
                .show(childFragmentManager, VerifyPasswordDialogFragment.TAG)
        }

        childFragmentManager.setFragmentResultListener(
            VerifyPasswordDialogFragment.RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val verifiedPassword = bundle.getString(VerifyPasswordDialogFragment.KEY_PASSWORD).orEmpty()
            if (verifiedPassword.isNotEmpty()) {
                val actingUserId = SessionManager(requireContext()).getUserId()
                val newPass = binding.etNewPassword.text?.toString().orEmpty()
                val confirmPass = binding.etConfirmPassword.text?.toString().orEmpty()

                when (
                    val result = AccountRepository.resetPassword(
                        targetUserId = targetUserId,
                        newPassword = newPass,
                        confirmPassword = confirmPass,
                        actingUserId = actingUserId,
                        actingUserPassword = verifiedPassword
                    )
                ) {
                    is RepoResult.Success -> {
                        Toast.makeText(
                            requireContext(),
                            "Password ${targetUser.fullName} berhasil diperbarui.",
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().navigateUp()
                    }

                    is RepoResult.Error -> {
                        binding.tvErrorMessage.text = result.message
                        binding.layoutErrorBanner.isVisible = true
                    }
                }
            }
        }
    }

    private fun applyVisibility(field: EditText, toggle: ImageButton, visible: Boolean) {
        val sel = field.selectionEnd
        field.transformationMethod = if (visible) {
            HideReturnsTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }
        toggle.setImageResource(if (visible) R.drawable.ic_visibility else R.drawable.ic_visibility_off)
        if (sel >= 0) field.setSelection(sel)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
