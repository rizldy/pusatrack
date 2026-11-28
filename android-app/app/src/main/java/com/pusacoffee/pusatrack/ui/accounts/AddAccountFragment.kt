package com.pusacoffee.pusatrack.ui.accounts

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.repository.AccountRepository
import com.pusacoffee.pusatrack.data.repository.RepoResult
import com.pusacoffee.pusatrack.databinding.FragmentAddAccountBinding

class AddAccountFragment : Fragment() {

    private var _binding: FragmentAddAccountBinding? = null
    private val binding get() = _binding!!

    private var passwordVisible = false
    private var confirmVisible = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            binding.containerPassword.isActivated = hasFocus
        }
        binding.etConfirmPassword.setOnFocusChangeListener { _, hasFocus ->
            binding.containerConfirmPassword.isActivated = hasFocus
        }

        val hideError = { binding.layoutErrorBanner.isVisible = false }
        binding.etFullName.doAfterTextChanged { hideError() }
        binding.etUsername.doAfterTextChanged { hideError() }
        binding.etPassword.doAfterTextChanged { hideError() }
        binding.etConfirmPassword.doAfterTextChanged { hideError() }

        binding.btnTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            applyVisibility(binding.etPassword, binding.btnTogglePassword, passwordVisible)
        }
        binding.btnToggleConfirmPassword.setOnClickListener {
            confirmVisible = !confirmVisible
            applyVisibility(binding.etConfirmPassword, binding.btnToggleConfirmPassword, confirmVisible)
        }

        binding.btnSaveAccount.setOnClickListener {
            val fullName = binding.etFullName.text?.toString().orEmpty()
            val username = binding.etUsername.text?.toString().orEmpty()
            val password = binding.etPassword.text?.toString().orEmpty()
            val confirm = binding.etConfirmPassword.text?.toString().orEmpty()

            when (val result = AccountRepository.createAccount(fullName, username, password, confirm)) {
                is RepoResult.Success -> findNavController().navigateUp()
                is RepoResult.Error -> {
                    binding.tvErrorMessage.text = result.message
                    binding.layoutErrorBanner.isVisible = true
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
