package com.rateio.app

import android.app.Application
import com.rateio.app.di.AppContainer

/** Dono do [AppContainer] — uma instância por processo, sobrevive a recriações de Activity. */
class RateioApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
