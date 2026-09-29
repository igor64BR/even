package com.tally.app

import android.app.Application
import com.tally.app.di.AppContainer

/** Owner of the [AppContainer] — one instance per process, survives Activity recreations. */
class TallyApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
