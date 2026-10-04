package com.example.familysafety

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AuthDataTest {
    private val email = "alex@example.test"
    @Before fun reset() { MockMemberDataSource.resetForTests(); MockAuthDataSource.resetForTests() }

    @Test fun correctCredentialsUseExistingMember() {
        MockMemberDataSource.getMembers().forEach { member ->
            assertEquals(member, MockAuthDataSource.login(member.account, MockAuthDataSource.INITIAL_PASSWORD))
        }
    }
    @Test fun wrongPasswordFails() { assertNull(MockAuthDataSource.login("alex", "Wrong123")) }
    @Test fun unknownAccountFails() { assertNull(MockAuthDataSource.login("unknown", MockAuthDataSource.INITIAL_PASSWORD)) }
    @Test fun blankCredentialsFail() {
        assertNull(MockAuthDataSource.login(" ", MockAuthDataSource.INITIAL_PASSWORD))
        assertNull(MockAuthDataSource.login("alex", " "))
    }
    @Test fun existingEmailIsNormalized() { assertTrue(MockAuthDataSource.emailExists(" ALEX@example.test ")) }
    @Test fun unknownAndBlankEmailsFail() {
        assertFalse(MockAuthDataSource.emailExists("unknown@example.test"))
        assertFalse(MockAuthDataSource.emailExists(""))
    }
    @Test fun correctCodeAuthorizesOnlyMatchingEmail() {
        assertTrue(MockAuthDataSource.verifyResetCode(email, "123456"))
        assertTrue(MockAuthDataSource.isResetVerified(email))
        assertFalse(MockAuthDataSource.isResetVerified("jamie@example.test"))
        assertFalse(MockAuthDataSource.resetPassword("jamie@example.test", "New12345"))
    }
    @Test fun wrongOrMalformedCodeDoesNotAuthorize() {
        listOf("", "12345", "1234567", "abcdef", "000000").forEach {
            assertFalse(MockAuthDataSource.verifyResetCode(email, it))
            assertFalse(MockAuthDataSource.isResetVerified(email))
        }
        assertFalse(MockAuthDataSource.verifyResetCode("unknown@example.test", "123456"))
    }
    @Test fun resetRequiresVerification() {
        assertFalse(MockAuthDataSource.resetPassword(email, "New12345"))
        assertNotNull(MockAuthDataSource.login("alex", MockAuthDataSource.INITIAL_PASSWORD))
    }
    @Test fun resetUpdatesCredentialRejectsOldAcceptsNewAndConsumesGrant() {
        assertTrue(MockAuthDataSource.verifyResetCode(email, "123456"))
        assertTrue(MockAuthDataSource.resetPassword(email, "New12345"))
        assertNull(MockAuthDataSource.login("alex", MockAuthDataSource.INITIAL_PASSWORD))
        assertNotNull(MockAuthDataSource.login("alex", "New12345"))
        assertFalse(MockAuthDataSource.isResetVerified(email))
        assertFalse(MockAuthDataSource.resetPassword(email, "Next1234"))
        assertNotNull(MockAuthDataSource.login("jamie", MockAuthDataSource.INITIAL_PASSWORD))
    }
    @Test fun passwordRuleRejectsInvalidValuesAndAcceptsBoundaries() {
        listOf("abc12", "abc1234567890", "abcdef", "123456").forEach {
            assertEquals(PasswordValidation.RULE, validatePassword(it, it))
        }
        assertNull(validatePassword("abc123", "abc123"))
        assertNull(validatePassword("abc123456789", "abc123456789"))
        assertNull(validatePassword("abc!12", "abc!12"))
    }
    @Test fun blankAndMismatchValidation() {
        assertEquals(PasswordValidation.REQUIRED, validatePassword("", ""))
        assertEquals(PasswordValidation.REQUIRED, validatePassword("abc123", " "))
        assertEquals(PasswordValidation.MISMATCH, validatePassword("abc123", "abc456"))
    }
    @Test fun invalidPasswordPreservesCredentialAndGrant() {
        MockAuthDataSource.verifyResetCode(email, "123456")
        assertFalse(MockAuthDataSource.resetPassword(email, "short"))
        assertTrue(MockAuthDataSource.isResetVerified(email))
        assertNotNull(MockAuthDataSource.login("alex", MockAuthDataSource.INITIAL_PASSWORD))
    }
    @Test fun samePasswordResetSucceedsAndConsumesGrant() {
        assertTrue(MockAuthDataSource.verifyResetCode(email, "123456"))
        assertTrue(MockAuthDataSource.resetPassword(email, MockAuthDataSource.INITIAL_PASSWORD))
        assertNotNull(MockAuthDataSource.login("alex", MockAuthDataSource.INITIAL_PASSWORD))
        assertFalse(MockAuthDataSource.isResetVerified(email))
    }
    @Test fun loginSetsSessionAndLogoutClearsItWithoutChangingPassword() {
        assertNull(MockAuthDataSource.getCurrentMember())
        assertNull(MockAuthDataSource.login("jamie", "Wrong123"))
        assertNull(MockAuthDataSource.getCurrentMember())
        val member = MockAuthDataSource.login("jamie", MockAuthDataSource.INITIAL_PASSWORD)!!
        assertEquals(member, MockAuthDataSource.getCurrentMember())
        MockAuthDataSource.verifyResetCode(member.email, "123456")
        MockAuthDataSource.logout()
        assertNull(MockAuthDataSource.getCurrentMember())
        assertFalse(MockAuthDataSource.isResetVerified(member.email))
        assertEquals(member, MockAuthDataSource.login("jamie", MockAuthDataSource.INITIAL_PASSWORD))
    }
    @Test fun restartResetFlowOrWrongCodeRevokesPreviousGrant() {
        MockAuthDataSource.verifyResetCode(email, "123456")
        assertTrue(MockAuthDataSource.beginPasswordReset(email))
        assertFalse(MockAuthDataSource.isResetVerified(email))
        MockAuthDataSource.verifyResetCode(email, "123456")
        MockAuthDataSource.verifyResetCode(email, "000000")
        assertFalse(MockAuthDataSource.isResetVerified(email))
    }
    @Test fun newMemberUsesSharedIdentityWithoutAutomaticallyAssignedPassword() {
        assertNotNull(MockAuthDataSource.login("alex", MockAuthDataSource.INITIAL_PASSWORD))
        val member = MockMemberDataSource.addMember("Morgan", "morgan", "morgan@example.test", MemberRole.MEMBER)!!
        assertTrue(MockAuthDataSource.emailExists(member.email))
        assertNull(MockAuthDataSource.login(member.account, MockAuthDataSource.INITIAL_PASSWORD))
        MockAuthDataSource.verifyResetCode(member.email, "123456")
        assertTrue(MockAuthDataSource.resetPassword(member.email, "New12345"))
        assertEquals(member, MockAuthDataSource.login(member.account, "New12345"))
    }
    @Test fun processResetRestoresInitialMockCredentials() {
        MockAuthDataSource.verifyResetCode(email, "123456")
        MockAuthDataSource.resetPassword(email, "New12345")
        MockAuthDataSource.resetForTests()
        assertNotNull(MockAuthDataSource.login("alex", MockAuthDataSource.INITIAL_PASSWORD))
        assertNull(MockAuthDataSource.login("alex", "New12345"))
    }
    @Test fun adminSessionCanManage() {
        MockAuthDataSource.login("alex", MockAuthDataSource.INITIAL_PASSWORD)
        assertEquals(MemberRole.ADMIN, MockAuthDataSource.getCurrentMember()!!.role)
        assertTrue(MockAuthDataSource.canManage())
    }
    @Test fun memberSessionsCannotManage() {
        listOf("jamie", "taylor").forEach {
            MockAuthDataSource.login(it, MockAuthDataSource.INITIAL_PASSWORD)
            assertEquals(MemberRole.MEMBER, MockAuthDataSource.getCurrentMember()!!.role)
            assertFalse(MockAuthDataSource.canManage())
        }
    }
    @Test fun permissionsFollowSessionAcrossLogoutAndLogin() {
        assertFalse(MockAuthDataSource.canManage())
        listOf("alex" to true, "jamie" to false, "alex" to true).forEach { (account, allowed) ->
            MockAuthDataSource.login(account, MockAuthDataSource.INITIAL_PASSWORD)
            assertEquals(allowed, MockAuthDataSource.canManage())
            MockAuthDataSource.logout()
            assertNull(MockAuthDataSource.getCurrentMember())
            assertFalse(MockAuthDataSource.canManage())
        }
    }
}
