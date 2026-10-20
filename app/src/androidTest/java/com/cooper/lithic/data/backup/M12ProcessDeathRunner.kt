package com.cooper.lithic.data.backup

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/** Explicit T04 entry point: do not construct LithicApplication or its default ledger owner. */
class M12ProcessDeathRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application =
        super.newApplication(cl, Application::class.java.name, context)
}
