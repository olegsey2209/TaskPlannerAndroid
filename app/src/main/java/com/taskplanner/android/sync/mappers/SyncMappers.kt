package com.taskplanner.android.sync.mappers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

import com.taskplanner.android.core.model.SyncStatus
import com.taskplanner.android.data.local.entities.CategoryEntity
import com.taskplanner.android.data.local.entities.GoalEntity
import com.taskplanner.android.data.local.entities.GoalStepEntity
import com.taskplanner.android.data.local.entities.RecurrenceRuleEntity
import com.taskplanner.android.data.local.entities.ScheduleTemplateEntity
import com.taskplanner.android.data.local.entities.ScheduleTemplateItemEntity
import com.taskplanner.android.data.local.entities.TaskEntity
import com.taskplanner.android.data.local.entities.TemplateApplicationEntity
import com.taskplanner.android.data.local.entities.UserProfileEntity

object TaskSyncMapper {
    fun toFirestore(t: TaskEntity): Map<String, Any?> {
        return mapOf(
            "id" to t.id,
            "userId" to t.userId,
            "title" to t.title,
            "taskDescription" to t.description,
            "notes" to t.notes,
            "priority" to t.priority,
            "status" to t.status,
            "hasReminder" to t.hasReminder,
            "reminderOffsetMinutes" to t.reminderOffsetMinutes,
            "originType" to t.originType,
            "position" to t.position,
            "syncStatus" to t.syncStatus,
            "date" to SyncMapperHelpers.timestamp(t.date),
            "startTime" to SyncMapperHelpers.timestamp(t.startTime),
            "instanceDate" to SyncMapperHelpers.timestamp(t.instanceDate),
            "completedAt" to SyncMapperHelpers.timestamp(t.completedAt),
            "createdAt" to SyncMapperHelpers.timestamp(t.createdAt),
            "updatedAt" to SyncMapperHelpers.timestamp(t.updatedAt),
            "deletedAt" to SyncMapperHelpers.timestamp(t.deletedAt),
            "categoryId" to t.categoryId,
            "goalStepId" to t.goalStepId,
            "parentTaskId" to t.parentTaskId,
            "recurrenceRuleId" to t.recurrenceRuleId,
            "templateItemId" to t.templateItemId,
            "templateApplicationId" to t.templateApplicationId,
            "imageBase64" to compressImageToBase64(t.imageData)
        )
    }

    
    fun compressImageToBase64(raw: ByteArray?): String? {
        raw ?: return null
        val bmp = BitmapFactory.decodeByteArray(raw, 0, raw.size) ?: return null
        val maxSide = 800
        val scale = minOf(maxSide.toFloat() / bmp.width, maxSide.toFloat() / bmp.height, 1f)
        val w = (bmp.width * scale).toInt().coerceAtLeast(1)
        val h = (bmp.height * scale).toInt().coerceAtLeast(1)
        val resized = if (scale < 1f) Bitmap.createScaledBitmap(bmp, w, h, true) else bmp

        val maxBytes = 700_000
        var quality = 80
        while (quality >= 10) {
            val out = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val bytes = out.toByteArray()
            if (bytes.size <= maxBytes) {
                return Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
            quality -= 10
        }
        
        val out = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 10, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    fun fromFirestore(data: Map<String, Any?>, existing: TaskEntity?): TaskEntity? {
        val id = SyncMapperHelpers.stringFromAny(data["id"]) ?: return null
        val userId = SyncMapperHelpers.stringFromAny(data["userId"]) ?: existing?.userId ?: return null
        val title = SyncMapperHelpers.stringFromAny(data["title"]) ?: existing?.title ?: ""
        val description = SyncMapperHelpers.stringFromAny(data["taskDescription"])
        val notes = SyncMapperHelpers.stringFromAny(data["notes"])
        val priority = SyncMapperHelpers.intFromAny(data["priority"]) ?: existing?.priority ?: 1
        val status = SyncMapperHelpers.intFromAny(data["status"]) ?: existing?.status ?: 0
        val hasReminder = SyncMapperHelpers.boolFromAny(data["hasReminder"]) ?: existing?.hasReminder ?: false
        val reminderOffsetMinutes = SyncMapperHelpers.intFromAny(data["reminderOffsetMinutes"]) ?: existing?.reminderOffsetMinutes ?: 15
        val originType = SyncMapperHelpers.intFromAny(data["originType"]) ?: existing?.originType ?: 0
        val position = SyncMapperHelpers.intFromAny(data["position"]) ?: existing?.position ?: 0
        val date = SyncMapperHelpers.epochMillisFromAny(data["date"]) ?: existing?.date ?: return null
        val startTime = SyncMapperHelpers.epochMillisFromAny(data["startTime"])
        val instanceDate = SyncMapperHelpers.epochMillisFromAny(data["instanceDate"])
        val completedAt = SyncMapperHelpers.epochMillisFromAny(data["completedAt"])
        val createdAt = SyncMapperHelpers.epochMillisFromAny(data["createdAt"]) ?: existing?.createdAt ?: System.currentTimeMillis()
        val updatedAt = SyncMapperHelpers.epochMillisFromAny(data["updatedAt"]) ?: existing?.updatedAt ?: System.currentTimeMillis()
        val deletedAt = SyncMapperHelpers.epochMillisFromAny(data["deletedAt"])
        val categoryId = SyncMapperHelpers.stringFromAny(data["categoryId"])
        val goalStepId = SyncMapperHelpers.stringFromAny(data["goalStepId"])
        val parentTaskId = SyncMapperHelpers.stringFromAny(data["parentTaskId"])
        val recurrenceRuleId = SyncMapperHelpers.stringFromAny(data["recurrenceRuleId"])
        val templateItemId = SyncMapperHelpers.stringFromAny(data["templateItemId"])
        val templateApplicationId = SyncMapperHelpers.stringFromAny(data["templateApplicationId"])

        val searchText = (title + " " + (description ?: "")).lowercase()

        return TaskEntity(
            id = id,
            userId = userId,
            title = title,
            description = description,
            notes = notes,
            imageData = run {
                val b64 = SyncMapperHelpers.stringFromAny(data["imageBase64"])
                when {
                    b64 != null && b64.isNotEmpty() -> {
                        try { Base64.decode(b64, Base64.NO_WRAP) } catch (e: Exception) { existing?.imageData }
                    }
                    data.containsKey("imageBase64") && b64 == null -> null  
                    else -> existing?.imageData  
                }
            },
            imageBase64 = SyncMapperHelpers.stringFromAny(data["imageBase64"]),
            date = date,
            startTime = startTime,
            priority = priority,
            status = status,
            completedAt = completedAt,
            hasReminder = hasReminder,
            reminderOffsetMinutes = reminderOffsetMinutes,
            categoryId = categoryId,
            parentTaskId = parentTaskId,
            goalStepId = goalStepId,
            originType = originType,
            instanceDate = instanceDate,
            recurrenceRuleId = recurrenceRuleId,
            templateItemId = templateItemId,
            templateApplicationId = templateApplicationId,
            position = position,
            searchText = searchText,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            syncStatus = SyncStatus.SYNCED.raw
        )
    }
}

object CategorySyncMapper {
    fun toFirestore(c: CategoryEntity): Map<String, Any?> {
        return mapOf(
            "id" to c.id,
            "userId" to c.userId,
            "name" to c.name,
            "iconName" to c.iconName,
            "colorHex" to c.colorHex,
            "sortOrder" to c.sortOrder,
            "isArchived" to c.isArchived,
            "createdAt" to SyncMapperHelpers.timestamp(c.createdAt),
            "updatedAt" to SyncMapperHelpers.timestamp(c.updatedAt),
            "deletedAt" to SyncMapperHelpers.timestamp(c.deletedAt),
            "syncStatus" to c.syncStatus
        )
    }

    fun fromFirestore(data: Map<String, Any?>, existing: CategoryEntity?): CategoryEntity? {
        val id = SyncMapperHelpers.stringFromAny(data["id"]) ?: return null
        val userId = SyncMapperHelpers.stringFromAny(data["userId"]) ?: existing?.userId ?: return null
        return CategoryEntity(
            id = id,
            userId = userId,
            name = SyncMapperHelpers.stringFromAny(data["name"]) ?: existing?.name ?: "",
            iconName = SyncMapperHelpers.stringFromAny(data["iconName"])?.takeIf { it.isNotBlank() } ?: existing?.iconName ?: "tag.fill",
            colorHex = SyncMapperHelpers.stringFromAny(data["colorHex"]) ?: existing?.colorHex ?: "#007AFF",
            sortOrder = SyncMapperHelpers.intFromAny(data["sortOrder"]) ?: existing?.sortOrder ?: 0,
            isArchived = SyncMapperHelpers.boolFromAny(data["isArchived"]) ?: existing?.isArchived ?: false,
            createdAt = SyncMapperHelpers.epochMillisFromAny(data["createdAt"]) ?: existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = SyncMapperHelpers.epochMillisFromAny(data["updatedAt"]) ?: existing?.updatedAt ?: System.currentTimeMillis(),
            deletedAt = SyncMapperHelpers.epochMillisFromAny(data["deletedAt"]),
            syncStatus = SyncStatus.SYNCED.raw
        )
    }
}

object GoalSyncMapper {
    fun toFirestore(g: GoalEntity): Map<String, Any?> {
        return mapOf(
            "id" to g.id,
            "userId" to g.userId,
            "title" to g.title,
            "goalDescription" to g.description,
            "status" to g.status,
            "progressCached" to g.progressCached,
            "completedAt" to SyncMapperHelpers.timestamp(g.completedAt),
            "createdAt" to SyncMapperHelpers.timestamp(g.createdAt),
            "updatedAt" to SyncMapperHelpers.timestamp(g.updatedAt),
            "deletedAt" to SyncMapperHelpers.timestamp(g.deletedAt),
            "syncStatus" to g.syncStatus
        )
    }

    fun fromFirestore(data: Map<String, Any?>, existing: GoalEntity?): GoalEntity? {
        val id = SyncMapperHelpers.stringFromAny(data["id"]) ?: return null
        val userId = SyncMapperHelpers.stringFromAny(data["userId"]) ?: existing?.userId ?: return null
        return GoalEntity(
            id = id,
            userId = userId,
            title = SyncMapperHelpers.stringFromAny(data["title"]) ?: existing?.title ?: "",
            description = SyncMapperHelpers.stringFromAny(data["goalDescription"]),
            status = SyncMapperHelpers.intFromAny(data["status"]) ?: existing?.status ?: 0,
            progressCached = SyncMapperHelpers.doubleFromAny(data["progressCached"]) ?: existing?.progressCached ?: 0.0,
            completedAt = SyncMapperHelpers.epochMillisFromAny(data["completedAt"]),
            createdAt = SyncMapperHelpers.epochMillisFromAny(data["createdAt"]) ?: existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = SyncMapperHelpers.epochMillisFromAny(data["updatedAt"]) ?: existing?.updatedAt ?: System.currentTimeMillis(),
            deletedAt = SyncMapperHelpers.epochMillisFromAny(data["deletedAt"]),
            syncStatus = SyncStatus.SYNCED.raw
        )
    }
}

object GoalStepSyncMapper {
    fun toFirestore(s: GoalStepEntity): Map<String, Any?> {
        return mapOf(
            "id" to s.id,
            "userId" to s.userId,
            "goalId" to s.goalId,
            "title" to s.title,
            "stepDescription" to s.description,
            "orderIndex" to s.orderIndex,
            "isCompleted" to s.isCompleted,
            "plannedDate" to SyncMapperHelpers.timestamp(s.plannedDate),
            "completedAt" to SyncMapperHelpers.timestamp(s.completedAt),
            "createdAt" to SyncMapperHelpers.timestamp(s.createdAt),
            "updatedAt" to SyncMapperHelpers.timestamp(s.updatedAt),
            "deletedAt" to SyncMapperHelpers.timestamp(s.deletedAt),
            "syncStatus" to s.syncStatus
        )
    }

    fun fromFirestore(data: Map<String, Any?>, existing: GoalStepEntity?): GoalStepEntity? {
        val id = SyncMapperHelpers.stringFromAny(data["id"]) ?: return null
        val userId = SyncMapperHelpers.stringFromAny(data["userId"]) ?: existing?.userId ?: return null
        val goalId = SyncMapperHelpers.stringFromAny(data["goalId"]) ?: existing?.goalId ?: return null
        return GoalStepEntity(
            id = id,
            userId = userId,
            goalId = goalId,
            title = SyncMapperHelpers.stringFromAny(data["title"]) ?: existing?.title ?: "",
            description = SyncMapperHelpers.stringFromAny(data["stepDescription"]),
            orderIndex = SyncMapperHelpers.intFromAny(data["orderIndex"]) ?: existing?.orderIndex ?: 0,
            isCompleted = SyncMapperHelpers.boolFromAny(data["isCompleted"]) ?: existing?.isCompleted ?: false,
            plannedDate = SyncMapperHelpers.epochMillisFromAny(data["plannedDate"]),
            completedAt = SyncMapperHelpers.epochMillisFromAny(data["completedAt"]),
            createdAt = SyncMapperHelpers.epochMillisFromAny(data["createdAt"]) ?: existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = SyncMapperHelpers.epochMillisFromAny(data["updatedAt"]) ?: existing?.updatedAt ?: System.currentTimeMillis(),
            deletedAt = SyncMapperHelpers.epochMillisFromAny(data["deletedAt"]),
            syncStatus = SyncStatus.SYNCED.raw
        )
    }
}

object RecurrenceRuleSyncMapper {
    fun toFirestore(r: RecurrenceRuleEntity): Map<String, Any?> {
        return mapOf(
            "id" to r.id,
            "userId" to r.userId,
            "sourceTaskId" to r.sourceTaskId,
            "frequency" to r.frequency,
            "intervalValue" to r.intervalValue,
            "startDate" to SyncMapperHelpers.timestamp(r.startDate),
            "endDate" to SyncMapperHelpers.timestamp(r.endDate),
            "weekdaysMask" to r.weekdaysMask,
            "dayOfMonth" to r.dayOfMonth,
            "monthOfYear" to r.monthOfYear,
            "lastGeneratedAt" to SyncMapperHelpers.timestamp(r.lastGeneratedAt),
            "createdAt" to SyncMapperHelpers.timestamp(r.createdAt),
            "updatedAt" to SyncMapperHelpers.timestamp(r.updatedAt),
            "deletedAt" to SyncMapperHelpers.timestamp(r.deletedAt),
            "syncStatus" to r.syncStatus
        )
    }

    fun fromFirestore(data: Map<String, Any?>, existing: RecurrenceRuleEntity?): RecurrenceRuleEntity? {
        val id = SyncMapperHelpers.stringFromAny(data["id"]) ?: return null
        val userId = SyncMapperHelpers.stringFromAny(data["userId"]) ?: existing?.userId ?: return null
        val sourceTaskId = SyncMapperHelpers.stringFromAny(data["sourceTaskId"]) ?: existing?.sourceTaskId ?: return null
        val startDate = SyncMapperHelpers.epochMillisFromAny(data["startDate"]) ?: existing?.startDate ?: return null
        return RecurrenceRuleEntity(
            id = id,
            userId = userId,
            frequency = SyncMapperHelpers.intFromAny(data["frequency"]) ?: existing?.frequency ?: 1,
            intervalValue = SyncMapperHelpers.intFromAny(data["intervalValue"]) ?: existing?.intervalValue ?: 1,
            weekdaysMask = SyncMapperHelpers.intFromAny(data["weekdaysMask"]) ?: existing?.weekdaysMask ?: 0,
            dayOfMonth = SyncMapperHelpers.intFromAny(data["dayOfMonth"]) ?: existing?.dayOfMonth ?: 0,
            monthOfYear = SyncMapperHelpers.intFromAny(data["monthOfYear"]) ?: existing?.monthOfYear ?: 0,
            sourceTaskId = sourceTaskId,
            startDate = startDate,
            endDate = SyncMapperHelpers.epochMillisFromAny(data["endDate"]),
            lastGeneratedAt = SyncMapperHelpers.epochMillisFromAny(data["lastGeneratedAt"]),
            createdAt = SyncMapperHelpers.epochMillisFromAny(data["createdAt"]) ?: existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = SyncMapperHelpers.epochMillisFromAny(data["updatedAt"]) ?: existing?.updatedAt ?: System.currentTimeMillis(),
            deletedAt = SyncMapperHelpers.epochMillisFromAny(data["deletedAt"]),
            syncStatus = SyncStatus.SYNCED.raw
        )
    }
}

object ScheduleTemplateSyncMapper {
    fun toFirestore(t: ScheduleTemplateEntity): Map<String, Any?> {
        return mapOf(
            "id" to t.id,
            "userId" to t.userId,
            "title" to t.title,
            "templateDescription" to t.description,
            "isArchived" to t.isArchived,
            "createdAt" to SyncMapperHelpers.timestamp(t.createdAt),
            "updatedAt" to SyncMapperHelpers.timestamp(t.updatedAt),
            "deletedAt" to SyncMapperHelpers.timestamp(t.deletedAt),
            "syncStatus" to t.syncStatus
        )
    }

    fun fromFirestore(data: Map<String, Any?>, existing: ScheduleTemplateEntity?): ScheduleTemplateEntity? {
        val id = SyncMapperHelpers.stringFromAny(data["id"]) ?: return null
        val userId = SyncMapperHelpers.stringFromAny(data["userId"]) ?: existing?.userId ?: return null
        return ScheduleTemplateEntity(
            id = id,
            userId = userId,
            title = SyncMapperHelpers.stringFromAny(data["title"]) ?: existing?.title ?: "",
            description = SyncMapperHelpers.stringFromAny(data["templateDescription"]),
            isArchived = SyncMapperHelpers.boolFromAny(data["isArchived"]) ?: existing?.isArchived ?: false,
            createdAt = SyncMapperHelpers.epochMillisFromAny(data["createdAt"]) ?: existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = SyncMapperHelpers.epochMillisFromAny(data["updatedAt"]) ?: existing?.updatedAt ?: System.currentTimeMillis(),
            deletedAt = SyncMapperHelpers.epochMillisFromAny(data["deletedAt"]),
            syncStatus = SyncStatus.SYNCED.raw
        )
    }
}

object ScheduleTemplateItemSyncMapper {
    fun toFirestore(i: ScheduleTemplateItemEntity): Map<String, Any?> {
        return mapOf(
            "id" to i.id,
            "userId" to i.userId,
            "templateId" to i.templateId,
            "title" to i.title,
            "itemDescription" to i.description,
            "weekday" to i.weekday,
            "position" to i.position,
            "priority" to i.priority,
            "hasReminder" to i.hasReminder,
            "reminderOffsetMinutes" to i.reminderOffsetMinutes,
            "categoryId" to i.categoryId,
            "startTime" to SyncMapperHelpers.timestamp(i.startTime),
            "createdAt" to SyncMapperHelpers.timestamp(i.createdAt),
            "updatedAt" to SyncMapperHelpers.timestamp(i.updatedAt),
            "deletedAt" to SyncMapperHelpers.timestamp(i.deletedAt),
            "syncStatus" to i.syncStatus
        )
    }

    fun fromFirestore(data: Map<String, Any?>, existing: ScheduleTemplateItemEntity?): ScheduleTemplateItemEntity? {
        val id = SyncMapperHelpers.stringFromAny(data["id"]) ?: return null
        val userId = SyncMapperHelpers.stringFromAny(data["userId"]) ?: existing?.userId ?: return null
        val templateId = SyncMapperHelpers.stringFromAny(data["templateId"]) ?: existing?.templateId ?: return null
        return ScheduleTemplateItemEntity(
            id = id,
            userId = userId,
            templateId = templateId,
            weekday = SyncMapperHelpers.intFromAny(data["weekday"]) ?: existing?.weekday ?: 1,
            title = SyncMapperHelpers.stringFromAny(data["title"]) ?: existing?.title ?: "",
            description = SyncMapperHelpers.stringFromAny(data["itemDescription"]),
            startTime = SyncMapperHelpers.epochMillisFromAny(data["startTime"]),
            priority = SyncMapperHelpers.intFromAny(data["priority"]) ?: existing?.priority ?: 1,
            hasReminder = SyncMapperHelpers.boolFromAny(data["hasReminder"]) ?: existing?.hasReminder ?: false,
            reminderOffsetMinutes = SyncMapperHelpers.intFromAny(data["reminderOffsetMinutes"]) ?: existing?.reminderOffsetMinutes ?: 15,
            categoryId = SyncMapperHelpers.stringFromAny(data["categoryId"]),
            position = SyncMapperHelpers.intFromAny(data["position"]) ?: existing?.position ?: 0,
            createdAt = SyncMapperHelpers.epochMillisFromAny(data["createdAt"]) ?: existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = SyncMapperHelpers.epochMillisFromAny(data["updatedAt"]) ?: existing?.updatedAt ?: System.currentTimeMillis(),
            deletedAt = SyncMapperHelpers.epochMillisFromAny(data["deletedAt"]),
            syncStatus = SyncStatus.SYNCED.raw
        )
    }
}

object TemplateApplicationSyncMapper {
    fun toFirestore(a: TemplateApplicationEntity): Map<String, Any?> {
        return mapOf(
            "id" to a.id,
            "userId" to a.userId,
            "templateId" to a.templateId,
            "startDate" to SyncMapperHelpers.timestamp(a.startDate),
            "endDate" to SyncMapperHelpers.timestamp(a.endDate),
            "isActive" to a.isActive,
            "lastGeneratedAt" to SyncMapperHelpers.timestamp(a.lastGeneratedAt),
            "createdAt" to SyncMapperHelpers.timestamp(a.createdAt),
            "updatedAt" to SyncMapperHelpers.timestamp(a.updatedAt),
            "deletedAt" to SyncMapperHelpers.timestamp(a.deletedAt),
            "syncStatus" to a.syncStatus
        )
    }

    fun fromFirestore(data: Map<String, Any?>, existing: TemplateApplicationEntity?): TemplateApplicationEntity? {
        val id = SyncMapperHelpers.stringFromAny(data["id"]) ?: return null
        val userId = SyncMapperHelpers.stringFromAny(data["userId"]) ?: existing?.userId ?: return null
        val templateId = SyncMapperHelpers.stringFromAny(data["templateId"]) ?: existing?.templateId ?: return null
        val startDate = SyncMapperHelpers.epochMillisFromAny(data["startDate"]) ?: existing?.startDate ?: return null
        val endDate = SyncMapperHelpers.epochMillisFromAny(data["endDate"]) ?: existing?.endDate ?: return null
        return TemplateApplicationEntity(
            id = id,
            userId = userId,
            templateId = templateId,
            startDate = startDate,
            endDate = endDate,
            isActive = SyncMapperHelpers.boolFromAny(data["isActive"]) ?: existing?.isActive ?: true,
            lastGeneratedAt = SyncMapperHelpers.epochMillisFromAny(data["lastGeneratedAt"]),
            createdAt = SyncMapperHelpers.epochMillisFromAny(data["createdAt"]) ?: existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = SyncMapperHelpers.epochMillisFromAny(data["updatedAt"]) ?: existing?.updatedAt ?: System.currentTimeMillis(),
            deletedAt = SyncMapperHelpers.epochMillisFromAny(data["deletedAt"]),
            syncStatus = SyncStatus.SYNCED.raw
        )
    }
}

object UserProfileSyncMapper {
    fun toFirestore(p: UserProfileEntity): Map<String, Any?> {
        return mapOf(
            "id" to p.id,
            "email" to p.email,
            "username" to p.username,
            "displayName" to p.username,
            "lastLoginAt" to SyncMapperHelpers.timestamp(p.lastLoginAt),
            "createdAt" to SyncMapperHelpers.timestamp(p.createdAt),
            "updatedAt" to SyncMapperHelpers.timestamp(p.updatedAt),
            "deletedAt" to SyncMapperHelpers.timestamp(p.deletedAt),
            "syncStatus" to p.syncStatus
        )
    }

    fun fromFirestore(data: Map<String, Any?>, existing: UserProfileEntity?): UserProfileEntity? {
        val id = SyncMapperHelpers.stringFromAny(data["id"]) ?: return null
        return UserProfileEntity(
            id = id,
            email = SyncMapperHelpers.stringFromAny(data["email"]) ?: existing?.email,
            username = SyncMapperHelpers.stringFromAny(data["username"])
                ?: SyncMapperHelpers.stringFromAny(data["displayName"])
                ?: existing?.username,
            createdAt = SyncMapperHelpers.epochMillisFromAny(data["createdAt"]) ?: existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = SyncMapperHelpers.epochMillisFromAny(data["updatedAt"]) ?: existing?.updatedAt ?: System.currentTimeMillis(),
            lastLoginAt = SyncMapperHelpers.epochMillisFromAny(data["lastLoginAt"]) ?: existing?.lastLoginAt ?: System.currentTimeMillis(),
            deletedAt = SyncMapperHelpers.epochMillisFromAny(data["deletedAt"]),
            syncStatus = SyncStatus.SYNCED.raw
        )
    }
}
