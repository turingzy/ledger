package cn.turingzy.ledger

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import cn.turingzy.ledger.data.debug.DevelopmentDataInitializer
import javax.inject.Inject

@HiltAndroidApp
class LedgerApplication : Application() {
    @Inject lateinit var developmentDataInitializer: DevelopmentDataInitializer

    override fun onCreate() {
        super.onCreate()
        developmentDataInitializer.initialize()
    }
}

