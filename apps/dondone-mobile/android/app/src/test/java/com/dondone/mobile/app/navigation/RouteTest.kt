package com.dondone.mobile.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteTest {
    @Test
    fun `root routes use root tab navigation`() {
        val rootCalls = mutableListOf<String>()
        val directCalls = mutableListOf<String>()

        navigateWithinApp(
            route = Route.WORKPROOF,
            navigateToRootTab = rootCalls::add,
            navigateDirect = directCalls::add
        )

        assertEquals(listOf(Route.WORKPROOF), rootCalls)
        assertTrue(directCalls.isEmpty())
    }

    @Test
    fun `child routes use direct navigation`() {
        val rootCalls = mutableListOf<String>()
        val directCalls = mutableListOf<String>()

        navigateWithinApp(
            route = Route.WAGE,
            navigateToRootTab = rootCalls::add,
            navigateDirect = directCalls::add
        )

        assertTrue(rootCalls.isEmpty())
        assertEquals(listOf(Route.WAGE), directCalls)
    }

    @Test
    fun `leaving workproof root route requests transient state reset`() {
        assertTrue(
            shouldResetWorkproofUiState(
                previousRoute = Route.WORKPROOF,
                nextRoute = Route.HOME
            )
        )
        assertTrue(
            shouldResetWorkproofUiState(
                previousRoute = Route.WORKPROOF,
                nextRoute = Route.WAGE
            )
        )
    }

    @Test
    fun `staying on workproof or entering it does not reset transient state`() {
        assertFalse(
            shouldResetWorkproofUiState(
                previousRoute = Route.WORKPROOF,
                nextRoute = Route.WORKPROOF
            )
        )
        assertFalse(
            shouldResetWorkproofUiState(
                previousRoute = Route.HOME,
                nextRoute = Route.WORKPROOF
            )
        )
    }
}
