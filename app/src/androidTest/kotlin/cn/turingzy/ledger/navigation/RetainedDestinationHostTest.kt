package cn.turingzy.ledger.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RetainedDestinationHostTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun switchingTabsRetainsCompositionAndStateWithoutLayingOutHiddenPages() {
        var selected by mutableStateOf(TopLevelDestination.LEDGER)
        val created = mutableListOf<TopLevelDestination>()
        val disposed = mutableListOf<TopLevelDestination>()
        val measurements = mutableMapOf<TopLevelDestination, Int>()
        compose.setContent {
            RetainedDestinationHost(selected, Modifier.fillMaxSize()) { destination ->
                var count by remember { mutableIntStateOf(0) }
                DisposableEffect(destination) {
                    created.add(destination)
                    onDispose { disposed.add(destination) }
                }
                Column(
                    Modifier.layout { measurable, constraints ->
                        measurements[destination] = measurements.getOrDefault(destination, 0) + 1
                        val child = measurable.measure(constraints)
                        layout(child.width, child.height) { child.placeRelative(0, 0) }
                    },
                ) {
                    Text("${destination.name}:$count")
                    Button(onClick = { count++ }) { Text("increment ${destination.name}") }
                }
            }
        }
        compose.onNodeWithText("increment LEDGER").performClick()
        compose.onNodeWithText("LEDGER:1").assertIsDisplayed()
        var ledgerMeasurements = 0
        compose.runOnIdle { ledgerMeasurements = measurements.getValue(TopLevelDestination.LEDGER) }

        listOf(TopLevelDestination.WALLET, TopLevelDestination.ANALYTICS, TopLevelDestination.SETTINGS).forEach { destination ->
            compose.runOnIdle { selected = destination }
            compose.onNodeWithText("${destination.name}:0").assertIsDisplayed()
            compose.onNodeWithText("LEDGER:1").assertIsNotDisplayed()
        }
        compose.runOnIdle {
            assertEquals(ledgerMeasurements, measurements.getValue(TopLevelDestination.LEDGER))
            selected = TopLevelDestination.LEDGER
        }
        compose.onNodeWithText("LEDGER:1").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(4, created.size)
            assertTrue(disposed.isEmpty())
        }
    }

    @Test
    fun hiddenTabStopsAtStartedAndKeepsTheSameLifecycleWhenRevisited() {
        var selected by mutableStateOf(TopLevelDestination.LEDGER)
        val owners = mutableMapOf<TopLevelDestination, LifecycleOwner>()
        compose.setContent {
            RetainedDestinationHost(selected, Modifier.fillMaxSize()) { destination ->
                val owner = LocalLifecycleOwner.current
                DisposableEffect(destination, owner) {
                    owners[destination] = owner
                    onDispose { owners.remove(destination) }
                }
                Text(destination.name)
            }
        }
        compose.runOnIdle { selected = TopLevelDestination.WALLET }
        compose.onNodeWithText("WALLET").assertIsDisplayed()
        lateinit var ledgerOwner: LifecycleOwner
        compose.runOnIdle {
            ledgerOwner = owners.getValue(TopLevelDestination.LEDGER)
            assertEquals(Lifecycle.State.STARTED, ledgerOwner.lifecycle.currentState)
            assertEquals(Lifecycle.State.RESUMED, owners.getValue(TopLevelDestination.WALLET).lifecycle.currentState)
            selected = TopLevelDestination.LEDGER
        }
        compose.onNodeWithText("LEDGER").assertIsDisplayed()
        compose.runOnIdle {
            assertTrue(ledgerOwner === owners.getValue(TopLevelDestination.LEDGER))
            assertEquals(Lifecycle.State.RESUMED, ledgerOwner.lifecycle.currentState)
            assertEquals(Lifecycle.State.STARTED, owners.getValue(TopLevelDestination.WALLET).lifecycle.currentState)
        }
    }
}
