package com.pusacoffee.pusatrack.data.repository

import com.pusacoffee.pusatrack.data.model.User
import com.pusacoffee.pusatrack.utils.DateFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for employee accounts & credentials (M1 Login & M12 Account Management L33-L39).
 */
object AccountRepository {

    private data class StoredAccount(
        val user: User,
        val password: String
    )

    private val USERNAME_REGEX = Regex("^[a-z0-9_]+$")
    const val MIN_PASSWORD_LENGTH = 6

    private val accountsStore = mutableListOf<StoredAccount>()
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private var nextUserId = 5

    init {
        seedInitialAccounts()
    }

    fun getAllUsers(): List<User> = _users.value

    fun getUserById(userId: Int): User? = _users.value.find { it.userId == userId }

    fun authenticate(username: String, password: String): User? {
        val cleanUsername = username.trim()
        val matched = accountsStore.find {
            it.user.username.equals(cleanUsername, ignoreCase = true)
        } ?: return null

        if (!matched.user.isActive || matched.password != password) {
            return null
        }
        return matched.user
    }

    fun verifyCurrentUserPassword(currentUserId: Int, password: String): Boolean {
        if (password.isEmpty()) return false
        val matched = accountsStore.find { it.user.userId == currentUserId } ?: return false
        return matched.user.isActive && matched.password == password
    }

    fun createAccount(
        fullName: String,
        username: String,
        password: String,
        confirmPassword: String
    ): RepoResult<User> {
        val cleanName = fullName.trim()
        val cleanUsername = username.trim()

        if (cleanName.isEmpty()) {
            return RepoResult.Error("Nama lengkap wajib diisi.")
        }
        val usernameError = validateUsername(cleanUsername, excludeUserId = null)
        if (usernameError != null) return RepoResult.Error(usernameError)

        val passwordError = validatePasswordPair(password, confirmPassword)
        if (passwordError != null) return RepoResult.Error(passwordError)

        val newUser = User(
            userId = nextUserId++,
            username = cleanUsername,
            fullName = cleanName,
            isActive = true,
            createdAtLabel = DateFormatter.formatDate()
        )
        accountsStore.add(StoredAccount(newUser, password))
        publishUsers()
        return RepoResult.Success(newUser)
    }

    fun updateAccount(
        userId: Int,
        fullName: String,
        username: String
    ): RepoResult<User> {
        val index = accountsStore.indexOfFirst { it.user.userId == userId }
        if (index < 0) return RepoResult.Error("Akun tidak ditemukan.")

        val cleanName = fullName.trim()
        val cleanUsername = username.trim()

        if (cleanName.isEmpty()) {
            return RepoResult.Error("Nama lengkap wajib diisi.")
        }
        val usernameError = validateUsername(cleanUsername, excludeUserId = userId)
        if (usernameError != null) return RepoResult.Error(usernameError)

        val existing = accountsStore[index]
        val updatedUser = existing.user.copy(
            fullName = cleanName,
            username = cleanUsername
        )
        accountsStore[index] = existing.copy(user = updatedUser)
        publishUsers()
        return RepoResult.Success(updatedUser)
    }

    fun resetPassword(
        targetUserId: Int,
        newPassword: String,
        confirmPassword: String,
        actingUserId: Int,
        actingUserPassword: String
    ): RepoResult<User> {
        val index = accountsStore.indexOfFirst { it.user.userId == targetUserId }
        if (index < 0) return RepoResult.Error("Akun tidak ditemukan.")

        val passwordError = validatePasswordPair(newPassword, confirmPassword)
        if (passwordError != null) return RepoResult.Error(passwordError)

        if (!verifyCurrentUserPassword(actingUserId, actingUserPassword)) {
            return RepoResult.Error("Password verifikasi akun aktif tidak sesuai.")
        }

        val existing = accountsStore[index]
        accountsStore[index] = existing.copy(password = newPassword)
        publishUsers()
        return RepoResult.Success(existing.user)
    }

    fun deactivateAccount(
        targetUserId: Int,
        actingUserId: Int,
        actingUserPassword: String
    ): RepoResult<User> {
        if (targetUserId == actingUserId) {
            return RepoResult.Error("Tidak bisa menonaktifkan akun yang sedang dipakai.")
        }

        val index = accountsStore.indexOfFirst { it.user.userId == targetUserId }
        if (index < 0) return RepoResult.Error("Akun tidak ditemukan.")

        if (!verifyCurrentUserPassword(actingUserId, actingUserPassword)) {
            return RepoResult.Error("Password verifikasi akun aktif tidak sesuai.")
        }

        val existing = accountsStore[index]
        val updated = existing.user.copy(isActive = false)
        accountsStore[index] = existing.copy(user = updated)
        publishUsers()
        return RepoResult.Success(updated)
    }

    fun reactivateAccount(targetUserId: Int): RepoResult<User> {
        val index = accountsStore.indexOfFirst { it.user.userId == targetUserId }
        if (index < 0) return RepoResult.Error("Akun tidak ditemukan.")

        val existing = accountsStore[index]
        val updated = existing.user.copy(isActive = true)
        accountsStore[index] = existing.copy(user = updated)
        publishUsers()
        return RepoResult.Success(updated)
    }

    fun validateUsername(username: String, excludeUserId: Int?): String? {
        if (username.isEmpty()) {
            return "Username wajib diisi."
        }
        if (username.contains(" ")) {
            return "Username tidak boleh mengandung spasi."
        }
        if (!USERNAME_REGEX.matches(username)) {
            return "Username hanya boleh huruf kecil, angka, atau garis bawah (_)."
        }
        val duplicate = accountsStore.any {
            it.user.userId != excludeUserId &&
                it.user.username.equals(username, ignoreCase = true)
        }
        if (duplicate) {
            return "Username \"$username\" sudah digunakan oleh karyawan lain."
        }
        return null
    }

    fun validatePasswordPair(password: String, confirmPassword: String): String? {
        if (password.length < MIN_PASSWORD_LENGTH) {
            return "Password minimal $MIN_PASSWORD_LENGTH karakter."
        }
        if (password != confirmPassword) {
            return "Konfirmasi password tidak cocok."
        }
        return null
    }

    fun resetForTesting() {
        seedInitialAccounts()
    }

    private fun seedInitialAccounts() {
        accountsStore.clear()
        accountsStore.addAll(
            listOf(
                StoredAccount(
                    user = User(1, "natashya", "Natashya Rosario", isActive = true, createdAtLabel = "1 Nov 2026"),
                    password = DEFAULT_SEED_PASSWORD
                ),
                StoredAccount(
                    user = User(2, "afdal", "Afdal Rizaldy", isActive = true, createdAtLabel = "1 Nov 2026"),
                    password = DEFAULT_SEED_PASSWORD
                ),
                StoredAccount(
                    user = User(3, "raidah", "Raidah Syahirah", isActive = true, createdAtLabel = "1 Nov 2026"),
                    password = DEFAULT_SEED_PASSWORD
                ),
                StoredAccount(
                    user = User(4, "rizky", "Rizky Pratama", isActive = false, createdAtLabel = "1 Nov 2026"),
                    password = DEFAULT_SEED_PASSWORD
                )
            )
        )
        nextUserId = 5
        publishUsers()
    }

    private fun publishUsers() {
        _users.value = accountsStore.map { it.user }
    }

    private const val DEFAULT_SEED_PASSWORD = "pusa1234"
}
