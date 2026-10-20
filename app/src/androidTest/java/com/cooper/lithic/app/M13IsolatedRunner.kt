package com.cooper.lithic.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/** Synthetic M13 integration only: never construct the normal Application or its ledger owner. */
class M13IsolatedRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application =
        super.newApplication(cl, Application::class.java.name, context)
}
