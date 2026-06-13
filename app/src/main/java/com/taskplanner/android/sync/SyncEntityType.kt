package com.taskplanner.android.sync

enum class SyncEntityType(val collection: String) {
    TASK("tasks"),
    CATEGORY("categories"),
    GOAL("goals"),
    GOAL_STEP("goalSteps"),
    RECURRENCE_RULE("recurrenceRules"),
    TEMPLATE("templates"),
    TEMPLATE_ITEM("templateItems"),
    TEMPLATE_APPLICATION("templateApplications"),
    USER_PROFILE("userProfile")
}
