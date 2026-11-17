package cn.turingzy.ledger.ui.component.base

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class UiContainerBoundaryTest {
    @Test fun nativeWindowsAndSurfacesAreOwnedByBaseComponents() {
        val relative = "src/main/kotlin/cn/turingzy/ledger/ui"
        val root = listOf(File(relative), File("app/$relative")).firstOrNull { it.isDirectory }
            ?: error("UI source directory was not found")
        val nativeContainer = Regex("(?<![A-Za-z0-9_])(?:Scaffold|ModalBottomSheet|Dialog|BasicAlertDialog|AlertDialog|Popup|Surface)\\s*\\(")
        val violations = root.walkTopDown().filter { it.extension == "kt" && "/component/base/" !in it.invariantSeparatorsPath }
            .filter { nativeContainer.containsMatchIn(it.readText()) }.map { it.relativeTo(root).path }.toList()
        assertTrue("Native UI containers must use the shared base components: $violations", violations.isEmpty())
    }
}
