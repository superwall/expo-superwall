package com.superwall.sdk.models.assignment

import com.superwall.sdk.assertTrue
import org.junit.Test

class AssignmentTest {
    @Test
    fun `make sure equality works`() {
        val firstAssignment = Assignment("123", "456")
        val secondAssignment = Assignment("123", "456")
        assertTrue(firstAssignment == secondAssignment)
    }

    @Test
    fun `make sure inequality works`() {
        val firstAssignment = Assignment("123", "456")
        val secondAssignment = Assignment("123", "789")
        assertTrue(firstAssignment != secondAssignment)
    }
}
