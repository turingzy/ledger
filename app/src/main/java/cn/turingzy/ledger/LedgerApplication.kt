package cn.turingzy.ledger

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LedgerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}

