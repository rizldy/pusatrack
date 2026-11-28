package com.pusacoffee.pusatrack.ui.login

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()
    private var isPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (viewModel.isAlreadyLoggedIn()) {
            navigateToHome()
            return
        }

        setupInputs()
        setupActions()
        observeUiState()
    }

    override fun onResume() {
        super.onResume()
        applyStatusBarStyle(isDarkBackground = true)
    }

    override fun onStop() {
        super.onStop()
        applyStatusBarStyle(isDarkBackground = false)
    }

    private fun applyStatusBarStyle(isDarkBackground: Boolean) {
        val window = activity?.window ?: return
        val bgColor = if (isDarkBackground) {
            ContextCompat.getColor(requireContext(), R.color.primary_forest)
        } else {
            ContextCompat.getColor(requireContext(), R.color.bg_white)
        }
        @Suppress("DEPRECATION")
        window.statusBarColor = bgColor
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars =
            !isDarkBackground
    }

    private fun setupInputs() {
        binding.etUsername.setOnFocusChangeListener { _, hasFocus ->
            binding.containerUsername.isActivated = hasFocus
            val tint = if (hasFocus) R.color.primary_forest else R.color.text_muted
            binding.ivUsernameIcon.setColorFilter(ContextCompat.getColor(requireContext(), tint))
        }

        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            binding.containerPassword.isActivated = hasFocus
            val tint = if (hasFocus) R.color.primary_forest else R.color.text_muted
            binding.ivPasswordIcon.setColorFilter(ContextCompat.getColor(requireContext(), tint))
        }

        binding.etUsername.doAfterTextChanged { viewModel.clearError() }
        binding.etPassword.doAfterTextChanged { viewModel.clearError() }

        binding.btnTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }

        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin()
                true
            } else {
                false
            }
        }
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        val selection = binding.etPassword.selectionEnd
        binding.etPassword.transformationMethod = if (isPasswordVisible) {
            HideReturnsTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }
        binding.btnTogglePassword.setImageResource(
            if (isPasswordVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
        )
        if (selection >= 0) {
            binding.etPassword.setSelection(selection)
        }
    }

    private fun setupActions() {
        binding.btnLogin.setOnClickListener {
            attemptLogin()
        }
    }

    private fun attemptLogin() {
        val username = binding.etUsername.text?.toString().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        viewModel.login(username, password)
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is LoginUiState.Idle -> {
                            setLoading(false)
                            binding.layoutErrorBanner.isVisible = false
                        }

                        is LoginUiState.Loading -> {
                            setLoading(true)
                            binding.layoutErrorBanner.isVisible = false
                        }

                        is LoginUiState.Error -> {
                            setLoading(false)
                            binding.tvErrorMessage.text = state.message
                            binding.layoutErrorBanner.isVisible = true
                        }

                        is LoginUiState.Success -> {
                            setLoading(false)
                            navigateToHome()
                        }
                    }
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnLogin.isEnabled = !isLoading
        binding.etUsername.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.btnTogglePassword.isEnabled = !isLoading
        binding.progressLogin.isVisible = isLoading
        binding.btnLogin.setText(
            if (isLoading) R.string.login_button_loading else R.string.login_button
        )
    }

    private fun navigateToHome() {
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.loginFragment, true)
            .build()
        findNavController().navigate(R.id.homeFragment, null, navOptions)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
