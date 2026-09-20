package com.xukunz.wakeupmywall.core.agent

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AgentTokenStoreTest {

    @Test
    fun `tokens are stored per device`() = runTest {
        val store = InMemoryAgentTokenStore()

        store.write("desktop-alpha", "token-a")
        store.write("study", "token-b")

        assertEquals("token-a", store.read("desktop-alpha"))
        assertEquals("token-b", store.read("study"))
        assertNull(store.read("never-paired"))
    }

    @Test
    fun `clearing one device leaves the others alone`() = runTest {
        val store = InMemoryAgentTokenStore(mapOf("desktop-alpha" to "token-a", "study" to "token-b"))

        store.clear("desktop-alpha")

        assertNull(store.read("desktop-alpha"))
        assertEquals("token-b", store.read("study"))
    }

    @Test
    fun `writing again replaces the previous token`() = runTest {
        val store = InMemoryAgentTokenStore(mapOf("desktop-alpha" to "old"))

        store.write("desktop-alpha", "new")

        assertEquals("new", store.read("desktop-alpha"))
    }
}
