//  File.kt
//
//
//  Created by Yusuf Tör on 29/09/2022.
//

import com.superwall.sdk.identity.IdentityLogic
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityLogicTests {
    @Test
    fun test_shouldGetAssignments_hasAccount_accountExistedPreStaticConfig() {
        val outcome =
            IdentityLogic.shouldGetAssignments(
                isLoggedIn = true,
                neverCalledStaticConfig = true,
                isFirstAppOpen = true,
            )
        assertTrue(outcome)
    }

    @Test
    fun test_shouldGetAssignments_isAnonymous_firstAppOpen_accountExistedPreStaticConfig() {
        val outcome =
            IdentityLogic.shouldGetAssignments(
                isLoggedIn = false,
                neverCalledStaticConfig = true,
                isFirstAppOpen = true,
            )
        assertFalse(outcome)
    }

    @Test
    fun test_shouldGetAssignments_hasAccount_noAccountPreStaticConfig() {
        val outcome =
            IdentityLogic.shouldGetAssignments(
                isLoggedIn = true,
                neverCalledStaticConfig = false,
                isFirstAppOpen = true,
            )
        assertFalse(outcome)
    }

    @Test
    fun test_shouldGetAssignments_isAnonymous_isFirstAppOpen() {
        val outcome =
            IdentityLogic.shouldGetAssignments(
                isLoggedIn = false,
                neverCalledStaticConfig = false,
                isFirstAppOpen = true,
            )
        assertFalse(outcome)
    }

    @Test
    fun test_shouldGetAssignments_isAnonymous_isNotFirstAppOpen_accountExistedPreStaticConfig() {
        val outcome =
            IdentityLogic.shouldGetAssignments(
                isLoggedIn = false,
                neverCalledStaticConfig = true,
                isFirstAppOpen = false,
            )
        assertTrue(outcome)
    }

    @Test
    fun test_shouldGetAssignments_isAnonymous_isNotFirstAppOpen_noAccountPreStaticConfig() {
        val outcome =
            IdentityLogic.shouldGetAssignments(
                isLoggedIn = false,
                neverCalledStaticConfig = false,
                isFirstAppOpen = false,
            )
        assertFalse(outcome)
    }
}
