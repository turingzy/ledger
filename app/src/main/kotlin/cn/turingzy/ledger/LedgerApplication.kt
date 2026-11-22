package cn.turingzy.ledger

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import cn.turingzy.ledger.data.debug.DevelopmentDataInitializer
import javax.inject.Inject

/**
 * 账本应用程序入口，配置 Hilt 依赖注入框架并初始化开发调试数据。
 */
@HiltAndroidApp
class LedgerApplication : Application() {
    /**
     * 开发环境数据初始化器，在调试构建中负责预置初始数据。
     */
    @Inject lateinit var developmentDataInitializer: DevelopmentDataInitializer

    /**
     * 应用程序创建时回调，执行基础组件及开发数据的初始化。
     */
    override fun onCreate() {
        super.onCreate()
        developmentDataInitializer.initialize()
    }
}

