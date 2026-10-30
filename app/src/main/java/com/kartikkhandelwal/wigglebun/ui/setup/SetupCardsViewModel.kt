package com.kartikkhandelwal.wigglebun.ui.setup

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartikkhandelwal.wigglebun.activity.MovementPermissions
import com.kartikkhandelwal.wigglebun.data.prefs.DeviceLocal
import com.kartikkhandelwal.wigglebun.data.prefs.MovementStore
import com.kartikkhandelwal.wigglebun.data.repo.SitRepository
import com.kartikkhandelwal.wigglebun.notify.ReminderNotifier
import com.kartikkhandelwal.wigglebun.widget.WigglebunWidgetReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Which cards we've asked about or been told "Not now". Device-local: permissions don't come
 * along to a new phone, so neither should the memory of having asked.
 */
@Singleton
class SetupStore @Inject constructor(@DeviceLocal private val dataStore: DataStore<Preferences>) {
    private val prefs = dataStore.data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    val asked: Flow<Set<SetupCard>> = prefs.map { it.cards(Asked) }.distinctUntilChanged()
    val dismissed: Flow<Set<SetupCard>> = prefs.map { it.cards(Dismissed) }.distinctUntilChanged()

    suspend fun markAsked(card: SetupCard) = add(Asked, card)

    suspend fun dismiss(card: SetupCard) = add(Dismissed, card)

    private suspend fun add(key: Preferences.Key<Set<String>>, card: SetupCard) {
        dataStore.edit { it[key] = it[key].orEmpty() + card.name }
    }

    private fun Preferences.cards(key: Preferences.Key<Set<String>>): Set<SetupCard> =
        this[key].orEmpty().mapNotNull { name -> SetupCard.entries.firstOrNull { it.name == name } }.toSet()

    private companion object {
        val Asked = stringSetPreferencesKey("setup_asked")
        val Dismissed = stringSetPreferencesKey("setup_dismissed")
    }
}

/** Reads the phone's current answers. Open so tests can stand in for them. */
@Singleton
open class SetupStatusReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notifier: ReminderNotifier,
    private val movementPermissions: MovementPermissions,
) {
    open fun read(shouldShowRationale: (String) -> Boolean): SystemSetup {
        val widgets = AppWidgetManager.getInstance(context)
        val notificationPermissionMissing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        return SystemSetup(
            notificationsOn = notifier.canPost(),
            notificationsNeedPermission = notificationPermissionMissing,
            notificationsRationale = notificationPermissionMissing && shouldShowRationale(Manifest.permission.POST_NOTIFICATIONS),
            movementGranted = movementPermissions.granted(),
            movementRationale = movementPermissions.needsRuntimePermission && shouldShowRationale(Manifest.permission.ACTIVITY_RECOGNITION),
            widgetAdded = widgets.getAppWidgetIds(ComponentName(context, WigglebunWidgetReceiver::class.java)).isNotEmpty(),
            widgetPinSupported = widgets.isRequestPinAppWidgetSupported,
        )
    }
}

@HiltViewModel
class SetupCardsViewModel @Inject constructor(
    private val reader: SetupStatusReader,
    private val store: SetupStore,
    movementStore: MovementStore,
    private val sitRepository: SitRepository,
) : ViewModel() {

    private val system = MutableStateFlow<SystemSetup?>(null)

    /** Null until the first [refresh]. */
    val state: StateFlow<SetupCardsUi?> = combine(
        system.filterNotNull(),
        store.asked,
        store.dismissed,
        movementStore.info.map { it.enabled }.distinctUntilChanged(),
    ) { system, asked, dismissed, movementEnabled ->
        setupCardsUi(system, asked, dismissed, movementEnabled)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** On every resume: the user may have just changed something in the phone's settings. */
    fun refresh(shouldShowRationale: (String) -> Boolean) {
        system.value = reader.read(shouldShowRationale)
    }

    /** Recorded before the system dialog opens, so the answer can't be missed. */
    fun onAsking(card: SetupCard) {
        viewModelScope.launch { store.markAsked(card) }
    }

    /** Registers movement tracking and the step listener, or posts a reminder that was already due. */
    fun onGranted() {
        viewModelScope.launch { sitRepository.checkReminder() }
    }

    fun enableMovement() {
        viewModelScope.launch { sitRepository.setMovementEnabled(true) }
    }

    fun notNow(card: SetupCard) {
        viewModelScope.launch { store.dismiss(card) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
