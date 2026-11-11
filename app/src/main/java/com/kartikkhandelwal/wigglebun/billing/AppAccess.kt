package com.kartikkhandelwal.wigglebun.billing

import com.kartikkhandelwal.wigglebun.BuildConfig
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Whether the app's core loop is available (CLAUDE.md §5.5: hard paywall with a free trial). */
enum class AppAccess {
    /** No answer yet: nothing cached on this phone and RevenueCat hasn't replied. */
    Checking,

    /** Trial, subscription or lifetime is active, or this build doesn't lock. */
    Open,

    /** No entitlement: Home shows the napping pet and reminders and the widget rest. */
    Locked,
}

/**
 * The rule, in one place:
 * - a build that doesn't lock ([locks] false: the closed test, whose test subscriptions lapse
 *   within hours) is always open;
 * - an active entitlement opens the app, none locks it;
 * - billing not set up ([PremiumState.Unavailable], a build without a RevenueCat key) is open in
 *   a debug build, so the app can be developed without keys, and locked anywhere else: a release
 *   that can't sell must not give the app away.
 */
fun appAccess(premium: PremiumState, locks: Boolean, debug: Boolean): AppAccess = when {
    !locks -> AppAccess.Open
    premium is PremiumState.Entitled -> AppAccess.Open
    premium is PremiumState.NotEntitled -> AppAccess.Locked
    premium is PremiumState.Unavailable -> if (debug) AppAccess.Open else AppAccess.Locked
    else -> AppAccess.Checking
}

/** What the rest of the app asks instead of reading [PremiumRepository] itself. */
interface AccessGate {
    val access: Flow<AppAccess>

    /** For background work (a due reminder, a widget redraw): open right now? [AppAccess.Checking] is not. */
    suspend fun isOpen(): Boolean = access.first() == AppAccess.Open

    /** For tests and previews. */
    object AlwaysOpen : AccessGate {
        override val access: Flow<AppAccess> = flowOf(AppAccess.Open)
    }
}

@Singleton
class PremiumAccessGate @Inject constructor(premium: PremiumRepository) : AccessGate {
    override val access: Flow<AppAccess> =
        premium.premium
            .map { appAccess(it, locks = BuildConfig.PAYWALL_LOCKS, debug = BuildConfig.DEBUG) }
            .distinctUntilChanged()
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AccessModule {
    @Binds
    abstract fun bindAccessGate(impl: PremiumAccessGate): AccessGate
}
