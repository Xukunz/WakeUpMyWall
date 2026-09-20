package com.xukunz.wakeupmywall.ui.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.xukunz.wakeupmywall.core.theme.WakeUpMyWallTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AgentSectionTest {

    @Test
    fun `pairing needs six digits and hands the code to the caller`() = runComposeUiTest {
        var pairedWith: String? = null
        setContent {
            WakeUpMyWallTheme {
                AgentSection(
                    host = "192.168.1.10",
                    port = "9876",
                    ui = AgentPairingUi(
                        paired = false,
                        isPairing = false,
                        note = null,
                        onPair = { pairedWith = it },
                        onUnpair = {},
                    ),
                )
            }
        }

        onNodeWithTag("device:pairing-state", useUnmergedTree = true).assertTextEquals("Not paired yet")
        onNodeWithTag("device:pair").assertIsNotEnabled()

        onNodeWithTag("device:pairing-code").performTextInput("123456")

        onNodeWithTag("device:pair").assertIsEnabled().performClick()
        assertEquals("123456", pairedWith)
    }

    @Test
    fun `a paired device offers unpair and shows where the token lives`() = runComposeUiTest {
        var unpaired = false
        setContent {
            WakeUpMyWallTheme {
                AgentSection(
                    host = "192.168.1.10",
                    port = "9876",
                    ui = AgentPairingUi(
                        paired = true,
                        isPairing = false,
                        note = null,
                        onPair = {},
                        onUnpair = { unpaired = true },
                    ),
                )
            }
        }

        onNodeWithTag("device:pairing-state", useUnmergedTree = true)
            .assertTextEquals("Paired · token stored in Keystore")

        onNodeWithTag("device:unpair").performClick()
        assertEquals(true, unpaired)
    }

    @Test
    fun `a pairing failure is shown verbatim`() = runComposeUiTest {
        setContent {
            WakeUpMyWallTheme {
                AgentSection(
                    host = "192.168.1.10",
                    port = "9876",
                    ui = AgentPairingUi(
                        paired = false,
                        isPairing = false,
                        note = "invalid or expired pairing code",
                        onPair = {},
                        onUnpair = {},
                    ),
                )
            }
        }

        onNodeWithTag("device:pairing-note", useUnmergedTree = true)
            .assertTextEquals("invalid or expired pairing code")
    }
}
