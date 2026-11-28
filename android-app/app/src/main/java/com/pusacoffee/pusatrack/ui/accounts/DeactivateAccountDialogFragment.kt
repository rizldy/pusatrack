package com.pusacoffee.pusatrack.ui.accounts

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.pusacoffee.pusatrack.databinding.DialogDeactivateAccountBinding

class DeactivateAccountDialogFragment : DialogFragment() {

    private var _binding: DialogDeactivateAccountBinding? = null
    private val binding get() = _binding!!

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
        _binding = DialogDeactivateAccountBinding.inflate(inflater, container, false)
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

        val fullName = arguments?.getString(ARG_FULL_NAME).orEmpty()
        val username = arguments?.getString(ARG_USERNAME).orEmpty()
        binding.tvTargetUser.text = "$fullName (@$username)"

        binding.btnCancelDeactivate.setOnClickListener { dismiss() }
        binding.btnProceedDeactivate.setOnClickListener {
            val bundle = Bundle().apply { putBoolean(KEY_PROCEED, true) }
            setFragmentResult(RESULT_KEY, bundle)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "DeactivateAccountDialogFragment"
        const val RESULT_KEY = "deactivate_account_result"
        const val KEY_PROCEED = "proceed"

        private const val ARG_FULL_NAME = "arg_full_name"
        private const val ARG_USERNAME = "arg_username"

        fun newInstance(fullName: String, username: String): DeactivateAccountDialogFragment {
            return DeactivateAccountDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FULL_NAME, fullName)
                    putString(ARG_USERNAME, username)
                }
            }
        }
    }
}
