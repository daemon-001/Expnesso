package com.daemon.expnesso.utils

import java.util.Locale

object FormatUtils {
    fun formatAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f", amount)
        } else {
            String.format(Locale.US, "%.2f", amount)
        }
    }
}
