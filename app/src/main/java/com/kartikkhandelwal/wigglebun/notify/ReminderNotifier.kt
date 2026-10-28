package com.kartikkhandelwal.wigglebun.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kartikkhandelwal.wigglebun.MainActivity
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.ui.labelRes
import com.kartikkhandelwal.wigglebun.ui.pet.displayName
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/** Builds and posts the "Bun Break" reminder (CLAUDE.md §9). */
@Singleton
class ReminderNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val manager: NotificationManagerCompat,
) {

    /** Creates the channel; safe to call on every app start. */
    fun createChannel() {
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.channel_break_reminders_name))
                .setDescription(context.getString(R.string.channel_break_reminders_description))
                .build(),
        )
    }

    /**
     * Whether a notification would actually be shown. On Android 13+ this needs the
     * POST_NOTIFICATIONS runtime permission, which we only ask for just-in-time (M9);
     * until then reminders silently no-op and the in-app timer keeps working (§10).
     */
    fun canPost(): Boolean {
        if (!manager.areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** Returns true if the reminder was posted. */
    @SuppressLint("MissingPermission") // checked in canPost()
    fun showReminder(pet: ActivePet, suggested: BreakType, snoozesLeft: Int): Boolean {
        if (!canPost()) return false
        val name = pet.displayName(context)
        val texts = context.resources.getStringArray(R.array.reminder_texts)
        val text = texts[Random.nextInt(texts.size)].format(name)
        val suggestion = context.getString(
            R.string.reminder_suggestion,
            context.getString(suggested.labelRes),
            suggested.durationMinutes,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(text)
            .setSubText(suggestion)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .addAction(0, context.getString(R.string.action_done), BreakActionReceiver.intent(context, BreakActionReceiver.ACTION_DONE, suggested))
        if (snoozesLeft > 0) {
            builder.addAction(0, context.getString(R.string.action_snooze), BreakActionReceiver.intent(context, BreakActionReceiver.ACTION_SNOOZE, suggested))
        }
        builder.addAction(0, context.getString(R.string.action_skip), BreakActionReceiver.intent(context, BreakActionReceiver.ACTION_SKIP, suggested))

        return try {
            manager.notify(NOTIFICATION_ID, builder.build())
            true
        } catch (_: SecurityException) {
            false // permission revoked between the check and the post
        }
    }

    fun cancel() = manager.cancel(NOTIFICATION_ID)

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val CHANNEL_ID = "break_reminders"
        const val NOTIFICATION_ID = 1
    }
}
