package com.mapgie.dash.tagalarm

import com.mapgie.dash.alarm.AlarmScheduler
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.conflictingTagAlarms
import com.mapgie.dash.data.repository.ReminderRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one place a tag-alarm is armed or turned off, whether from a tap on its
 * NFC tag (MainActivity), Set for next / Turn off in the memo sheet, a swipe
 * on the list, or Stop for today on the ring screen. Every path goes through
 * the same record update and the same alarm re-sync, so they cannot drift.
 *
 * Deliberately knows nothing about chores or tasks; the tag-alarm feature
 * touches them only through the "is this tag id taken" check the sheets make.
 */
@Singleton
class TagAlarmService @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
) {
    /** The alarm as armed, plus the other tag-alarms already set for the same morning. */
    data class Armed(val alarm: ReminderDto, val conflicts: List<ReminderDto>)

    /** The unarchived tag-alarm a tag with [tagId] arms, or null when the tag is not a tag-alarm's. */
    suspend fun findByTagId(tagId: String): ReminderDto? = reminderRepository.findTagAlarmByTagId(tagId)

    /**
     * Arms [id]'s next morning (a no-op when a ring is already ahead) and reports
     * which other tag-alarms are set for that same morning, so the caller can ask
     * whether to turn them off. The tapped alarm is armed before the question is
     * asked: a dialog nobody answers must never cost tomorrow's alarm.
     */
    suspend fun arm(id: String): Armed? {
        val armed = reminderRepository.armTagAlarm(id) ?: return null
        alarmScheduler.syncReminder(armed)
        val conflicts = armed.conflictingTagAlarms(reminderRepository.loadReminders())
        return Armed(armed, conflicts)
    }

    /** Turn off / Stop for today: dormant, every pending ring of its morning cancelled. */
    suspend fun disarm(id: String): ReminderDto? {
        val dormant = reminderRepository.disarmTagAlarm(id) ?: return null
        alarmScheduler.syncReminder(dormant)
        return dormant
    }

    suspend fun disarmAll(ids: List<String>) {
        ids.forEach { disarm(it) }
    }
}
