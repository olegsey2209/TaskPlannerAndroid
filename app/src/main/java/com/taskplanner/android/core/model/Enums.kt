package com.taskplanner.android.core.model

enum class SyncStatus(val raw: Int) {
    SYNCED(0),
    CREATED_LOCAL(1),
    UPDATED_LOCAL(2),
    DELETED_LOCAL(3),
    CONFLICT(4)
}

enum class TaskPriority(val raw: Int) {
    LOW(0),
    MEDIUM(1),
    HIGH(2)
}

enum class TaskStatus(val raw: Int) {
    PLANNED(0),
    COMPLETED(1),
    SKIPPED(2),
    CANCELLED(3)
}

enum class TaskOriginType(val raw: Int) {
    MANUAL(0),
    GOAL_STEP(1),
    RECURRENCE(2),
    TEMPLATE(3)
}

enum class GoalStatus(val raw: Int) {
    ACTIVE(0),
    COMPLETED(1),
    ARCHIVED(2)
}

enum class RecurrenceFrequency(val raw: Int) {
    DAILY(0),
    WEEKLY(1),
    MONTHLY(2),
    YEARLY(3)
}

