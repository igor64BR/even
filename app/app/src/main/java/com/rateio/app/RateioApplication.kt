package com.rateio.app

import android.app.Application
import com.rateio.app.di.AppContainer

/** Owner of the [AppContainer] — one instance per process, survives Activity recreations. */
class RateioApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
