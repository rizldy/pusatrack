package com.pusacoffee.pusatrack.ui.accounts

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.local.SessionManager
import com.pusacoffee.pusatrack.data.repository.AccountRepository
import com.pusacoffee.pusatrack.databinding.DialogVerifyPasswordBinding

class VerifyPasswordDialogFragment : DialogFragment() {

    private var _binding: DialogVerifyPasswordBinding? = null
    private val binding get() = _binding!!

    private var passwordVisible = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogVerifyPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (resources.displayMetrics.widthPixels * 0.92).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())
        val currentUserId = session.getUserId()
        val fullName = session.getFullName().orEmpty().ifBlank { "Karyawan" }

        binding.tvVerifySubtitle.text =
            "Masukkan password akunmu ($fullName) untuk konfirmasi keamanan."

        binding.etCurrentPassword.setOnFocusChangeListener { _, hasFocus ->
            binding.containerPassword.isActivated = hasFocus
        }
        binding.etCurrentPassword.doAfterTextChanged {
            binding.tvVerifyError.isVisible = false
        }

        binding.btnTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            val sel = binding.etCurrentPassword.selectionEnd
            binding.etCurrentPassword.transformationMethod = if (passwordVisible) {
                HideReturnsTransformationMethod.getInstance()
            } else {
                PasswordTransformationMethod.getInstance()
            }
            binding.btnTogglePassword.setImageResource(
                if (passwordVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
            )
            if (sel >= 0) binding.etCurrentPassword.setSelection(sel)
        }

        binding.btnCancelVerify.setOnClickListener { dismiss() }

        binding.btnConfirmVerify.setOnClickListener {
            val entered = binding.etCurrentPassword.text?.toString().orEmpty()
            if (entered.isEmpty()) {
                binding.tvVerifyError.text = "Masukkan password akun aktif terlebih dahulu."
                binding.tvVerifyError.isVisible = true
                return@setOnClickListener
            }

            if (!AccountRepository.verifyCurrentUserPassword(currentUserId, entered)) {
                binding.tvVerifyError.text = "Password tidak sesuai. Periksa kembali dan coba lagi."
                binding.tvVerifyError.isVisible = true
                return@setOnClickListener
            }

            val result = Bundle().apply { putString(KEY_PASSWORD, entered) }
            setFragmentResult(RESULT_KEY, result)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "VerifyPasswordDialogFragment"
        const val RESULT_KEY = "verify_password_result"
        const val KEY_PASSWORD = "verified_password"
    }
}
