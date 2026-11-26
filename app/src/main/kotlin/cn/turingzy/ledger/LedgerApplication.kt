package cn.turingzy.ledger

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 账本应用程序入口，为应用级组件启用 Hilt 依赖注入。
 */
@HiltAndroidApp
class LedgerApplication : Application()

