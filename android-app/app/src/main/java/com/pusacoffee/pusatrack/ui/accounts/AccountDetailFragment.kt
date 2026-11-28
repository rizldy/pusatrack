package com.pusacoffee.pusatrack.ui.accounts

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.local.SessionManager
import com.pusacoffee.pusatrack.data.model.User
import com.pusacoffee.pusatrack.data.repository.AccountRepository
import com.pusacoffee.pusatrack.data.repository.RepoResult
import com.pusacoffee.pusatrack.databinding.FragmentAccountDetailBinding
import kotlinx.coroutines.launch

class AccountDetailFragment : Fragment() {

    private var _binding: FragmentAccountDetailBinding? = null
    private val binding get() = _binding!!

    private var targetUserId: Int = -1
    private var currentUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        targetUserId = arguments?.getInt("userId", -1) ?: -1
        val actingUserId = SessionManager(requireContext()).getUserId()

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnEditAccount.setOnClickListener {
            val args = Bundle().apply { putInt("userId", targetUserId) }
            findNavController().navigate(R.id.editAccountFragment, args)
        }

        binding.btnResetPassword.setOnClickListener {
            val args = Bundle().apply { putInt("userId", targetUserId) }
            findNavController().navigate(R.id.resetPasswordFragment, args)
        }

        binding.btnToggleActive.setOnClickListener {
            val user = currentUser ?: return@setOnClickListener
            if (user.userId == actingUserId) return@setOnClickListener

            if (user.isActive) {
                DeactivateAccountDialogFragment.newInstance(user.fullName, user.username)
                    .show(childFragmentManager, DeactivateAccountDialogFragment.TAG)
            } else {
                when (val result = AccountRepository.reactivateAccount(user.userId)) {
                    is RepoResult.Success -> Toast.makeText(
                        requireContext(),
                        "Akun ${user.fullName} diaktifkan kembali.",
                        Toast.LENGTH_SHORT
                    ).show()

                    is RepoResult.Error -> Toast.makeText(
                        requireContext(),
                        result.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // L39 -> L38 flow
        childFragmentManager.setFragmentResultListener(
            DeactivateAccountDialogFragment.RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            if (bundle.getBoolean(DeactivateAccountDialogFragment.KEY_PROCEED, false)) {
                VerifyPasswordDialogFragment()
                    .show(childFragmentManager, VerifyPasswordDialogFragment.TAG)
            }
        }

        // L38 verified -> execute deactivation
        childFragmentManager.setFragmentResultListener(
            VerifyPasswordDialogFragment.RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val verifiedPassword = bundle.getString(VerifyPasswordDialogFragment.KEY_PASSWORD).orEmpty()
            if (verifiedPassword.isNotEmpty()) {
                when (
                    val result = AccountRepository.deactivateAccount(
                        targetUserId = targetUserId,
                        actingUserId = actingUserId,
                        actingUserPassword = verifiedPassword
                    )
                ) {
                    is RepoResult.Success -> Toast.makeText(
                        requireContext(),
                        "Akun ${result.data.fullName} telah dinonaktifkan.",
                        Toast.LENGTH_SHORT
                    ).show()

                    is RepoResult.Error -> Toast.makeText(
                        requireContext(),
                        result.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                AccountRepository.users.collect { list ->
                    val user = list.find { it.userId == targetUserId }
                    if (user == null) {
                        findNavController().navigateUp()
                        return@collect
                    }
                    currentUser = user
                    bindUser(user, actingUserId)
                }
            }
        }
    }

    private fun bindUser(user: User, actingUserId: Int) {
        val ctx = requireContext()
        val isSelf = user.userId == actingUserId

        val words = user.fullName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        binding.tvInitials.text = when {
            words.size >= 2 -> "${words[0][0]}${words[1][0]}".uppercase()
            words.size == 1 -> words[0].take(2).uppercase()
            else -> "KR"
        }

        binding.tvFullName.text = user.fullName
        binding.tvUsername.text = "@${user.username}"
        binding.tvYouBadge.isVisible = isSelf
        binding.tvInfoCreatedAt.text = user.createdAtLabel

        if (user.isActive) {
            binding.tvStatusBadge.text = "Aktif"
            binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_safe)
            binding.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.primary_forest))
            binding.tvInfoStatus.text = "Aktif · Bisa login"
        } else {
            binding.tvStatusBadge.text = "Nonaktif"
            binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_out)
            binding.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.tertiary_text))
            binding.tvInfoStatus.text = "Nonaktif · Diblokir login"
        }

        if (isSelf) {
            binding.btnToggleActive.text = "Nonaktifkan Akun"
            binding.btnToggleActive.isEnabled = false
            binding.btnToggleActive.alpha = 0.45f
            binding.btnToggleActive.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(ctx, R.color.tertiary_berry)
            )
            binding.tvSelfProtectNote.isVisible = true
        } else if (user.isActive) {
            binding.btnToggleActive.text = "Nonaktifkan Akun"
            binding.btnToggleActive.isEnabled = true
            binding.btnToggleActive.alpha = 1.0f
            binding.btnToggleActive.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(ctx, R.color.tertiary_berry)
            )
            binding.tvSelfProtectNote.isVisible = false
        } else {
            binding.btnToggleActive.text = "Aktifkan Kembali"
            binding.btnToggleActive.isEnabled = true
            binding.btnToggleActive.alpha = 1.0f
            binding.btnToggleActive.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(ctx, R.color.primary_forest)
            )
            binding.tvSelfProtectNote.isVisible = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
