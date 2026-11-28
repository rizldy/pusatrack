package com.pusacoffee.pusatrack

import com.pusacoffee.pusatrack.data.repository.AccountRepository
import com.pusacoffee.pusatrack.data.repository.RepoResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AccountManagementTest {

    @Before
    fun setUp() {
        AccountRepository.resetForTesting()
    }

    @Test
    fun createAccount_succeedsCanAuthenticateImmediatelyAndRejectsDuplicateUsername() {
        val createResult = AccountRepository.createAccount(
            fullName = "Alim Anugrah",
            username = "alim_btp",
            password = "secret123",
            confirmPassword = "secret123"
        )
        assertTrue(createResult is RepoResult.Success)
        val created = (createResult as RepoResult.Success).data
        assertEquals("Alim Anugrah", created.fullName)
        assertEquals("alim_btp", created.username)
        assertTrue(created.isActive)

        // Immediately usable for login
        val authUser = AccountRepository.authenticate("alim_btp", "secret123")
        assertNotNull(authUser)
        assertEquals(created.userId, authUser!!.userId)

        // Duplicate username (case-insensitive) rejected
        val dupResult = AccountRepository.createAccount(
            fullName = "Duplikat Natashya",
            username = "Natashya",
            password = "password1",
            confirmPassword = "password1"
        )
        assertTrue(dupResult is RepoResult.Error)
    }

    @Test
    fun editAccount_updatesFullNameAndUsername() {
        val updateResult = AccountRepository.updateAccount(
            userId = 2,
            fullName = "Afdal Rizaldy BTP",
            username = "afdal_btp"
        )
        assertTrue(updateResult is RepoResult.Success)
        val updated = (updateResult as RepoResult.Success).data
        assertEquals("Afdal Rizaldy BTP", updated.fullName)
        assertEquals("afdal_btp", updated.username)

        // Can authenticate with updated username
        assertNotNull(AccountRepository.authenticate("afdal_btp", "pusa1234"))
        assertNull(AccountRepository.authenticate("afdal", "pusa1234"))
    }

    @Test
    fun resetPassword_requiresValidActingPasswordAndReplacesOldPassword() {
        // Fails if acting user password is wrong
        val wrongVerify = AccountRepository.resetPassword(
            targetUserId = 2,
            newPassword = "newpass99",
            confirmPassword = "newpass99",
            actingUserId = 1,
            actingUserPassword = "wrongpassword"
        )
        assertTrue(wrongVerify is RepoResult.Error)

        // Succeeds when acting user password is valid
        val okVerify = AccountRepository.resetPassword(
            targetUserId = 2,
            newPassword = "newpass99",
            confirmPassword = "newpass99",
            actingUserId = 1,
            actingUserPassword = "pusa1234"
        )
        assertTrue(okVerify is RepoResult.Success)

        // New password accepted, old password rejected
        assertNotNull(AccountRepository.authenticate("afdal", "newpass99"))
        assertNull(AccountRepository.authenticate("afdal", "pusa1234"))
    }

    @Test
    fun deactivateAndReactivate_blocksSelfDeactivationBlocksLoginWhenInactiveAndRestoresLogin() {
        // Self-deactivation rejected
        val selfDeactivate = AccountRepository.deactivateAccount(
            targetUserId = 1,
            actingUserId = 1,
            actingUserPassword = "pusa1234"
        )
        assertTrue(selfDeactivate is RepoResult.Error)

        // Deactivate another user (afdal, id=2)
        val deactivateOther = AccountRepository.deactivateAccount(
            targetUserId = 2,
            actingUserId = 1,
            actingUserPassword = "pusa1234"
        )
        assertTrue(deactivateOther is RepoResult.Success)
        assertFalse((deactivateOther as RepoResult.Success).data.isActive)

        // Deactivated user rejected at login
        assertNull(AccountRepository.authenticate("afdal", "pusa1234"))

        // Reactivate user -> can login again
        val reactivate = AccountRepository.reactivateAccount(2)
        assertTrue(reactivate is RepoResult.Success)
        assertTrue((reactivate as RepoResult.Success).data.isActive)
        assertNotNull(AccountRepository.authenticate("afdal", "pusa1234"))
    }
}
