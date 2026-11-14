package cn.turingzy.ledger.data.transfer

import cn.turingzy.ledger.data.repository.AppUpdateRepository
import org.junit.Assert.*
import org.junit.Test

class AppUpdateVersionTest {
    @Test fun comparesNumericComponentsAndIgnoresTrailingZeroes() {
        assertTrue(AppUpdateRepository.isNewer("v1.10", "1.9.9"))
        assertFalse(AppUpdateRepository.isNewer("v1.0.0", "1.0"))
        assertFalse(AppUpdateRepository.isNewer("1.0", "1.1"))
        assertTrue(AppUpdateRepository.isNewer("2.0", "1.99"))
    }
}
