package cn.turingzy.ledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cn.turingzy.ledger.ui.LedgerRoot
import dagger.hilt.android.AndroidEntryPoint

/**
 * 账本应用主界面 Activity，负责启用边缘到边缘显示并托管 Compose 根组件 [LedgerRoot]。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /**
     * Activity 创建时回调，配置沉浸式系统边衬并设置 Compose 内容视图。
     *
     * @param savedInstanceState 之前保存的状态数据。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LedgerRoot(window)
        }
    }
}
