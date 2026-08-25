package com.poi.core.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.poi.core.model.AttendanceStatus
import com.poi.core.model.Event
import java.util.concurrent.TimeUnit
import org.json.JSONObject

class EventReminderScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun sync(
        events: List<Event>,
        attendance: Map<String, AttendanceStatus>,
        enabled: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        if (!enabled) {
            storedRecords().forEach(::cancel)
            preferences.edit().clear().apply()
            return
        }

        val wanted = events.asSequence()
            .filter { event ->
                attendance[event.id] in setOf(AttendanceStatus.INTERESTED, AttendanceStatus.GOING) &&
                    event.startsAtMillis > nowMillis
            }
            .associate { event ->
                val planned = event.startsAtMillis - TimeUnit.HOURS.toMillis(1)
                event.id to ReminderRecord(
                    eventId = event.id,
                    eventTitle = event.title,
                    triggerAtMillis = planned.coerceAtLeast(nowMillis + 5_000L),
                )
            }

        storedRecords().filter { it.eventId !in wanted }.forEach { record ->
            cancel(record)
            preferences.edit().remove(record.key()).apply()
        }
        wanted.values.forEach { record ->
            val stored = readRecord(record.eventId)
            if (stored != record) {
                schedule(record)
                preferences.edit().putString(record.key(), record.toJson().toString()).apply()
            }
        }
    }

    internal fun schedule(record: ReminderRecord) {
        val alarmManager = appContext.getSystemService(AlarmManager::class.java)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            record.triggerAtMillis,
            pendingIntent(record),
        )
    }

    private fun cancel(record: ReminderRecord) {
        appContext.getSystemService(AlarmManager::class.java).cancel(pendingIntent(record))
    }

    private fun pendingIntent(record: ReminderRecord): PendingIntent = PendingIntent.getBroadcast(
        appContext,
        record.eventId.hashCode(),
        Intent(appContext, EventReminderReceiver::class.java).apply {
            action = ACTION_REMIND_EVENT
            putExtra(EXTRA_EVENT_ID, record.eventId)
            putExtra(EXTRA_EVENT_TITLE, record.eventTitle)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    internal fun storedRecords(): List<ReminderRecord> = preferences.all.keys.mapNotNull(::readRecord)

    private fun readRecord(eventIdOrKey: String): ReminderRecord? {
        val key = if (eventIdOrKey.startsWith(KEY_PREFIX)) eventIdOrKey else KEY_PREFIX + eventIdOrKey
        val encoded = preferences.getString(key, null) ?: return null
        return runCatching {
            val json = JSONObject(encoded)
            ReminderRecord(
                eventId = json.getString("eventId"),
                eventTitle = json.getString("eventTitle"),
                triggerAtMillis = json.getLong("triggerAtMillis"),
            )
        }.getOrNull()
    }

    internal fun forget(eventId: String) {
        preferences.edit().remove(KEY_PREFIX + eventId).apply()
    }

    companion object {
        internal const val ACTION_REMIND_EVENT = "com.poi.action.REMIND_EVENT"
        internal const val EXTRA_EVENT_ID = "event_id"
        internal const val EXTRA_EVENT_TITLE = "event_title"
        internal const val CHANNEL_ID = "event_reminders"
        private const val PREFERENCES_NAME = "poi_event_reminders"
        private const val KEY_PREFIX = "reminder."
    }
}

internal data class ReminderRecord(
    val eventId: String,
    val eventTitle: String,
    val triggerAtMillis: Long,
) {
    fun key() = "reminder.$eventId"

    fun toJson() = JSONObject().apply {
        put("eventId", eventId)
        put("eventTitle", eventTitle)
        put("triggerAtMillis", triggerAtMillis)
    }
}

class EventReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != EventReminderScheduler.ACTION_REMIND_EVENT) return
        val eventId = intent.getStringExtra(EventReminderScheduler.EXTRA_EVENT_ID) ?: return
        val eventTitle = intent.getStringExtra(EventReminderScheduler.EXTRA_EVENT_TITLE) ?: "Your event"
        createNotificationChannel(context)

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(
                context,
                eventId.hashCode(),
                it.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val notification = NotificationCompat.Builder(context, EventReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Starting in about an hour")
            .setContentText(eventTitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$eventTitle starts in about an hour."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (permitted) {
            NotificationManagerCompat.from(context).notify(eventId.hashCode(), notification)
        }
        EventReminderScheduler(context).forget(eventId)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            EventReminderScheduler.CHANNEL_ID,
            "Event reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Reminders for events you marked interested or going"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val scheduler = EventReminderScheduler(context)
        val now = System.currentTimeMillis()
        scheduler.storedRecords()
            .filter { it.triggerAtMillis > now }
            .forEach(scheduler::schedule)
    }
}
