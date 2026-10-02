package com.even.app

import android.app.Application
import com.even.app.di.AppContainer

/** Owner of the [AppContainer] — one instance per process, survives Activity recreations. */
class EvenApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
