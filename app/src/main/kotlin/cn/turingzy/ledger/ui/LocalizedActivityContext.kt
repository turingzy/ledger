package cn.turingzy.ledger.ui

import android.content.Context
import android.content.res.Configuration
import android.view.ContextThemeWrapper

internal fun localizedActivityContext(context: Context, configuration: Configuration): ContextThemeWrapper =
    // Hilt and navigation must still be able to unwrap the original Activity.
    ContextThemeWrapper(context, context.theme).apply { applyOverrideConfiguration(configuration) }
