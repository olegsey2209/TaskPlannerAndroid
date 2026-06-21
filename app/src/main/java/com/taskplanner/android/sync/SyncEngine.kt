package com.taskplanner.android.sync

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.taskplanner.android.core.model.SyncStatus
import com.taskplanner.android.core.model.GoalStatus
import com.taskplanner.android.core.model.TaskOriginType
import com.taskplanner.android.core.model.TaskStatus
import com.taskplanner.android.data.local.AppDatabase
import com.taskplanner.android.sync.mappers.CategorySyncMapper
import com.taskplanner.android.sync.mappers.GoalStepSyncMapper
import com.taskplanner.android.sync.mappers.GoalSyncMapper
import com.taskplanner.android.sync.mappers.RecurrenceRuleSyncMapper
import com.taskplanner.android.sync.mappers.ScheduleTemplateItemSyncMapper
import com.taskplanner.android.sync.mappers.ScheduleTemplateSyncMapper
import com.taskplanner.android.sync.mappers.SyncMapperHelpers
import com.taskplanner.android.sync.mappers.TaskSyncMapper
import com.taskplanner.android.sync.mappers.TemplateApplicationSyncMapper
import com.taskplanner.android.sync.mappers.UserProfileSyncMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicBoolean

private val Context.syncDataStore by preferencesDataStore(name = "taskplanner_sync_prefs")

class SyncEngine(
    private val context: Context,
    private val db: AppDatabase,
    private val firestore: FirestoreService = FirestoreService()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncing = AtomicBoolean(false)
    private val pendingResumeUserIds = java.util.concurrent.CopyOnWriteArraySet<String>()
    private val noopTrigger = SyncTrigger {}
    private val recurrenceRepo by lazy {
        com.taskplanner.android.data.repository.RecurrenceRepository(
            db.recurrenceRuleDao(), db.taskDao(), noopTrigger
        )
    }
    @Volatile private var pendingSync: String? = null

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val keyLastSync = longPreferencesKey("lastSyncDate")
    private fun keyInitialDone(uid: String) = booleanPreferencesKey("initialSyncCompleted_$uid")
    private fun keyLegacyCategoryRepairDone(uid: String) =
        booleanPreferencesKey("legacyCategoryRepairV1_$uid")

    fun observeLastSyncDate(): Flow<Long?> =
        context.syncDataStore.data.map { prefs: Preferences -> prefs[keyLastSync] }

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    fun forceSync(userId: String) {
        _lastError.value = null
        _isOnline.value = true
        triggerSync(userId)
    }

    fun triggerSync(userId: String) {
        if (userId.isBlank()) return
        if (!syncing.compareAndSet(false, true)) {
            
            pendingSync = userId
            return
        }

        _isSyncing.value = true
        scope.launch {
            try {
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                var attempts = 0
                while (auth.currentUser == null && attempts < 20) {
                    kotlinx.coroutines.delay(100)
                    attempts++
                }
                val firebaseUser = auth.currentUser
                if (firebaseUser == null) {
                    android.util.Log.w("TaskPlanner.Sync", "No firebase user, skip sync")
                    _isOnline.value = false
                    _lastError.value = "Не авторизован"
                    return@launch
                }
                if (firebaseUser.uid != userId) {
                    android.util.Log.w("TaskPlanner.Sync", "uid mismatch: firebase=${firebaseUser.uid} request=$userId")
                    _lastError.value = "Неверный пользователь"
                    return@launch
                }
                runCatching { firebaseUser.getIdToken(false).await() }
                    .onFailure { android.util.Log.w("TaskPlanner.Sync", "getIdToken не удалось", it) }

                android.util.Log.d("TaskPlanner.Sync", "Запуск sync for $userId")
                performSync(userId)
                context.syncDataStore.edit { prefs -> prefs[keyLastSync] = System.currentTimeMillis() }
                _isOnline.value = true
                _lastError.value = null
                android.util.Log.d("TaskPlanner.Sync", "Sync completed for $userId")
            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                android.util.Log.e("TaskPlanner.Sync", "Firestore ошибка: ${e.code} ${e.message}", e)
                _lastError.value = when (e.code) {
                    com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        "Нет доступа к облаку. Проверьте правила Firestore."
                    com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                        "Не авторизован в облаке."
                    com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE ->
                        "Нет связи с облаком."
                    else -> "Ошибка синхронизации: ${e.code}"
                }
                _isOnline.value = e.code != com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED
            } catch (e: Exception) {
                android.util.Log.e("TaskPlanner.Sync", "Sync не удалось", e)
                _lastError.value = e.localizedMessage ?: e.toString()
                _isOnline.value = false
            } finally {
                syncing.set(false)
                _isSyncing.value = false
                
                val pending = pendingSync
                if (pending != null) {
                    pendingSync = null
                    triggerSync(pending)
                }
            }
        }
    }

    private suspend fun performSync(userId: String) {
        if (performInitialSyncIfNeeded(userId)) return

        
        uploadLocalChanges(userId)
        
        downloadRemoteChanges(userId)
        if (repairLegacyDeletedCategoriesIfNeeded(userId)) {
            uploadLocalChanges(userId)
        }
        if (repairDeletedRecurrenceBoundaryTasks(userId)) {
            uploadLocalChanges(userId)
        }
        if (reconcileGoalStepsFromLinkedTasks(userId)) {
            uploadLocalChanges(userId)
        }
        if (normalizeCategoryIcons(userId)) {
            uploadLocalChanges(userId)
        }
        if (deduplicateActiveCategories(userId)) {
            uploadLocalChanges(userId)
        }
        if (deduplicateGeneratedRecurrenceTasks(userId)) {
            uploadLocalChanges(userId)
        }


        if (pendingResumeUserIds.remove(userId)) {
            recurrenceRepo.generateUpcomingTasksIfNeeded(userId)
            if (deduplicateGeneratedRecurrenceTasks(userId)) {
                uploadLocalChanges(userId)
            }
        }
    }

    private suspend fun performInitialSyncIfNeeded(userId: String): Boolean {
        val key = keyInitialDone(userId)
        val done = context.syncDataStore.data.map { it[key] ?: false }.first()
        if (done) return false

        val empty = isCloudEmpty(userId)
        if (empty) {
            uploadAllLocalForInitialSync(userId)
            context.syncDataStore.edit { prefs -> prefs[key] = true }
            return true
        }

        downloadRemoteChanges(userId)
        if (repairLegacyDeletedCategoriesIfNeeded(userId)) {
            uploadLocalChanges(userId)
        }
        if (repairDeletedRecurrenceBoundaryTasks(userId)) {
            uploadLocalChanges(userId)
        }
        if (reconcileGoalStepsFromLinkedTasks(userId)) {
            uploadLocalChanges(userId)
        }
        if (normalizeCategoryIcons(userId)) {
            uploadLocalChanges(userId)
        }
        if (deduplicateActiveCategories(userId)) {
            uploadLocalChanges(userId)
        }
        if (deduplicateGeneratedRecurrenceTasks(userId)) {
            uploadLocalChanges(userId)
        }
        context.syncDataStore.edit { prefs -> prefs[key] = true }
        return true
    }

    private suspend fun isCloudEmpty(userId: String): Boolean {
        return firestore.isCollectionEmpty(userId, SyncEntityType.TASK) &&
            firestore.isCollectionEmpty(userId, SyncEntityType.CATEGORY) &&
            firestore.isCollectionEmpty(userId, SyncEntityType.GOAL) &&
            firestore.isCollectionEmpty(userId, SyncEntityType.TEMPLATE) &&
            firestore.isCollectionEmpty(userId, SyncEntityType.USER_PROFILE)
    }

    private suspend fun reconcileGoalStepsFromLinkedTasks(userId: String): Boolean {
        val activeTasksByStep = db.taskDao()
            .getAllForUser(userId)
            .asSequence()
            .filter { it.deletedAt == null && !it.goalStepId.isNullOrBlank() }
            .groupBy { it.goalStepId!! }

        val changedGoalIds = mutableSetOf<String>()
        var changed = false

        db.goalStepDao().getAllForUser(userId)
            .filter { it.deletedAt == null }
            .forEach { step ->
                val newestTask = activeTasksByStep[step.id]?.maxByOrNull { it.updatedAt } ?: return@forEach
                val taskIsCompleted = newestTask.status == TaskStatus.COMPLETED.raw

                if (newestTask.updatedAt >= step.updatedAt && step.isCompleted != taskIsCompleted) {
                    val now = System.currentTimeMillis()
                    db.goalStepDao().update(
                        step.copy(
                            isCompleted = taskIsCompleted,
                            completedAt = if (taskIsCompleted) newestTask.completedAt ?: now else null,
                            updatedAt = now,
                            syncStatus = SyncStatus.UPDATED_LOCAL.raw
                        )
                    )
                    changedGoalIds += step.goalId
                    changed = true
                }
            }

        changedGoalIds.forEach { goalId ->
            val goal = db.goalDao().getById(userId, goalId) ?: return@forEach
            val steps = db.goalStepDao().getForGoal(userId, goalId).filter { it.deletedAt == null }
            val progress = if (steps.isEmpty()) 0.0 else steps.count { it.isCompleted }.toDouble() / steps.size.toDouble()
            val now = System.currentTimeMillis()
            val isCompleted = progress >= 1.0
            db.goalDao().update(
                goal.copy(
                    progressCached = progress,
                    status = if (isCompleted) GoalStatus.COMPLETED.raw else GoalStatus.ACTIVE.raw,
                    completedAt = if (isCompleted) goal.completedAt ?: now else null,
                    updatedAt = now,
                    syncStatus = SyncStatus.UPDATED_LOCAL.raw
                )
            )
        }

        return changed
    }

    private suspend fun repairDeletedRecurrenceBoundaryTasks(userId: String): Boolean {
        val tasks = db.taskDao().getAllForUser(userId)
        var changed = false

        db.recurrenceRuleDao().getAllForUser(userId)
            .filter { it.deletedAt != null }
            .forEach { rule ->
                val deletedAt = rule.deletedAt ?: return@forEach
                val deletionDay = com.taskplanner.android.core.util.TimeUtils.startOfDayMillis(
                    com.taskplanner.android.core.util.TimeUtils.localDateFromMillis(deletedAt)
                )
                val nextDay = com.taskplanner.android.core.util.TimeUtils.startOfDayMillis(
                    com.taskplanner.android.core.util.TimeUtils.localDateFromMillis(deletedAt).plusDays(1)
                )

                tasks.asSequence()
                    .filter {
                        it.recurrenceRuleId == rule.id &&
                            it.originType == TaskOriginType.RECURRENCE.raw &&
                            it.deletedAt != null &&
                            it.date >= deletionDay &&
                            it.date < nextDay &&
                            kotlin.math.abs((it.deletedAt ?: 0L) - deletedAt) <= 60_000L
                    }
                    .forEach { task ->
                        val now = System.currentTimeMillis()
                        db.taskDao().upsert(
                            task.copy(
                                recurrenceRuleId = null,
                                instanceDate = null,
                                originType = TaskOriginType.MANUAL.raw,
                                deletedAt = null,
                                updatedAt = now,
                                syncStatus = SyncStatus.UPDATED_LOCAL.raw
                            )
                        )
                        changed = true
                    }
            }

        return changed
    }

    
    
    

    private suspend fun downloadRemoteChanges(userId: String) {
        
        
        
        val order = listOf(
            SyncEntityType.USER_PROFILE,
            SyncEntityType.CATEGORY,
            SyncEntityType.GOAL,
            SyncEntityType.GOAL_STEP,
            SyncEntityType.TEMPLATE,
            SyncEntityType.TEMPLATE_ITEM,
            SyncEntityType.TEMPLATE_APPLICATION,
            SyncEntityType.RECURRENCE_RULE,
            SyncEntityType.TASK
        )
        for (type in order) {
            val remote = firestore.loadDocuments(userId, type)
            applyRemoteObjects(remote, type, userId)
        }
    }

    private suspend fun applyRemoteObjects(
        remoteObjects: List<Map<String, Any?>>,
        type: SyncEntityType,
        userId: String
    ) {
        val syncedRaw = SyncStatus.SYNCED.raw

        for (data in remoteObjects) {
            val remoteId = SyncMapperHelpers.stringFromAny(data["id"]) ?: continue
            if (remoteId.isEmpty()) continue
            val remoteUpdated = SyncMapperHelpers.epochMillisFromAny(data["updatedAt"]) ?: Long.MIN_VALUE

            when (type) {
                SyncEntityType.TASK -> {
                    val existing = db.taskDao().getByIdAny(userId, remoteId)
                    val localUpdated = existing?.updatedAt ?: Long.MIN_VALUE

                    if (existing != null && existing.syncStatus != syncedRaw && localUpdated >= remoteUpdated) continue

                    if (existing?.deletedAt != null && existing.syncStatus != syncedRaw) continue
                    if (existing == null || remoteUpdated > localUpdated) {
                        TaskSyncMapper.fromFirestore(data, existing)?.let { task ->
                            db.taskDao().upsert(task)
                            
                            com.taskplanner.android.notifications.NotificationHelper.cancelReminder(context, task.id)
                            if (task.hasReminder &&
                                task.deletedAt == null &&
                                task.status == TaskStatus.PLANNED.raw
                            ) {
                                com.taskplanner.android.notifications.NotificationHelper.scheduleReminder(context, task)
                            }
                        }
                    }
                }
                SyncEntityType.CATEGORY -> {
                    val existing = db.categoryDao().getByIdAny(userId, remoteId)
                    val remoteDeletedAt = SyncMapperHelpers.epochMillisFromAny(data["deletedAt"])
                    val remoteCreatedAt = SyncMapperHelpers.epochMillisFromAny(data["createdAt"])
                    val localUpdated = existing?.updatedAt ?: Long.MIN_VALUE

                    if (existing == null && remoteDeletedAt == null) {
                        val remoteName = SyncMapperHelpers.stringFromAny(data["name"])?.trim().orEmpty()
                        if (remoteName.isNotEmpty()) {
                            val deletedCategory = db.categoryDao().getDeletedByName(userId, remoteName)
                            val deletedAt = deletedCategory?.deletedAt
                            if (deletedAt != null && (remoteCreatedAt == null || remoteCreatedAt <= deletedAt)) continue
                        }
                    }

                    if (existing?.deletedAt != null && remoteDeletedAt == null) continue
                    if (existing != null && existing.syncStatus != syncedRaw && localUpdated >= remoteUpdated) continue

                    if (existing == null || remoteUpdated > localUpdated || (remoteDeletedAt != null && existing.deletedAt == null)) {
                        CategorySyncMapper.fromFirestore(data, existing)?.let { db.categoryDao().upsert(it) }
                    }
                }
                SyncEntityType.GOAL -> {
                    val existing = db.goalDao().getByIdAny(userId, remoteId)
                    val localUpd_goal = existing?.updatedAt ?: Long.MIN_VALUE
                    if (existing != null && existing.syncStatus != syncedRaw && localUpd_goal >= remoteUpdated) continue
                    val localUpdated = existing?.updatedAt ?: Long.MIN_VALUE
                    if (existing == null || remoteUpdated > localUpdated) {
                        GoalSyncMapper.fromFirestore(data, existing)?.let { db.goalDao().upsert(it) }
                    }
                }
                SyncEntityType.GOAL_STEP -> {
                    val existing = db.goalStepDao().getByIdAny(userId, remoteId)
                    val localUpd_goal_step = existing?.updatedAt ?: Long.MIN_VALUE
                    if (existing != null && existing.syncStatus != syncedRaw && localUpd_goal_step >= remoteUpdated) continue
                    val localUpdated = existing?.updatedAt ?: Long.MIN_VALUE
                    if (existing == null || remoteUpdated > localUpdated) {
                        GoalStepSyncMapper.fromFirestore(data, existing)?.let { db.goalStepDao().upsert(it) }
                    }
                }
                SyncEntityType.RECURRENCE_RULE -> {
                    val existing = db.recurrenceRuleDao().getByIdAny(userId, remoteId)
                    val localUpdated = existing?.updatedAt ?: Long.MIN_VALUE


                    if (existing != null && existing.syncStatus != syncedRaw && localUpdated > remoteUpdated) continue


                    val mapped = RecurrenceRuleSyncMapper.fromFirestore(data, existing)
                    if (mapped != null && (
                        existing == null ||
                        remoteUpdated >= localUpdated ||
                        mapped.endDate != existing.endDate ||
                        mapped.deletedAt != existing.deletedAt
                    )) {
                        val now = System.currentTimeMillis()
                        val today = com.taskplanner.android.core.util.TimeUtils.startOfDayMillis(java.time.LocalDate.now())
                        val tomorrow = com.taskplanner.android.core.util.TimeUtils.startOfDayMillis(java.time.LocalDate.now().plusDays(1))
                        val sourceTask = db.taskDao().getByIdAny(userId, mapped.sourceTaskId)
                        val sourceHasGeneratedOccurrence = sourceTask?.let { source ->
                            val sourceDate = com.taskplanner.android.core.util.TimeUtils.localDateFromMillis(source.date)
                            val sourceDay = com.taskplanner.android.core.util.TimeUtils.startOfDayMillis(sourceDate)
                            val sourceDayEnd = com.taskplanner.android.core.util.TimeUtils.startOfDayMillis(sourceDate.plusDays(1))
                            db.taskDao().getForRange(userId, sourceDay, sourceDayEnd).any {
                                it.recurrenceRuleId == remoteId &&
                                    it.originType == TaskOriginType.RECURRENCE.raw &&
                                    it.deletedAt == null
                            }
                        } ?: false

                        when {
                            mapped.deletedAt != null && (existing == null || existing.deletedAt == null) -> {
                                db.recurrenceRuleDao().upsert(mapped)
                                db.taskDao().restoreGeneratedTaskForDay(
                                    userId = userId,
                                    ruleId = remoteId,
                                    recurrenceOriginType = TaskOriginType.RECURRENCE.raw,
                                    manualOriginType = TaskOriginType.MANUAL.raw,
                                    startInclusive = today,
                                    endExclusive = tomorrow,
                                    updatedAt = now,
                                    syncStatus = SyncStatus.UPDATED_LOCAL.raw
                                )
                                db.taskDao().softDeleteGeneratedForRuleFromDate(userId, remoteId, TaskOriginType.RECURRENCE.raw, tomorrow, now)
                                db.taskDao().detachFromRuleBefore(
                                    userId = userId,
                                    ruleId = remoteId,
                                    before = tomorrow,
                                    manualOriginType = TaskOriginType.MANUAL.raw,
                                    updatedAt = now,
                                    syncStatus = SyncStatus.UPDATED_LOCAL.raw
                                )
                                if (sourceTask != null && sourceTask.deletedAt == null) {
                                    if (sourceHasGeneratedOccurrence) {
                                        db.taskDao().softDelete(
                                            userId = userId,
                                            id = sourceTask.id,
                                            deletedAt = now,
                                            updatedAt = now,
                                            syncStatus = SyncStatus.DELETED_LOCAL.raw
                                        )
                                    } else {
                                        db.taskDao().upsert(
                                            sourceTask.copy(
                                                recurrenceRuleId = null,
                                                instanceDate = null,
                                                originType = TaskOriginType.MANUAL.raw,
                                                updatedAt = now,
                                                syncStatus = SyncStatus.UPDATED_LOCAL.raw
                                            )
                                        )
                                    }
                                }
                            }
                            mapped.deletedAt == null && mapped.endDate != null && mapped.endDate != existing?.endDate -> {

                                val cutoff = mapped.endDate + com.taskplanner.android.data.repository.RecurrenceRepository.DAY_MILLIS
                                db.taskDao().softDeleteGeneratedForRuleFromDate(userId, remoteId, com.taskplanner.android.core.model.TaskOriginType.RECURRENCE.raw, cutoff, now)
                                db.recurrenceRuleDao().upsert(mapped.copy(lastGeneratedAt = mapped.endDate))
                            }
                            mapped.deletedAt == null && mapped.endDate == null && existing?.endDate != null -> {

                                val generationStart = if (mapped.startDate > today) mapped.startDate else today
                                val lastGen = generationStart - com.taskplanner.android.data.repository.RecurrenceRepository.DAY_MILLIS
                                db.recurrenceRuleDao().upsert(mapped.copy(lastGeneratedAt = lastGen))
                                pendingResumeUserIds.add(userId)
                            }
                            else -> {
                                db.recurrenceRuleDao().upsert(mapped)
                            }
                        }
                    }
                }
                SyncEntityType.TEMPLATE -> {
                    val existing = db.scheduleTemplateDao().getByIdAny(userId, remoteId)
                    val localUpd_template = existing?.updatedAt ?: Long.MIN_VALUE
                    if (existing != null && existing.syncStatus != syncedRaw && localUpd_template >= remoteUpdated) continue
                    val localUpdated = existing?.updatedAt ?: Long.MIN_VALUE
                    if (existing == null || remoteUpdated > localUpdated) {
                        ScheduleTemplateSyncMapper.fromFirestore(data, existing)?.let { db.scheduleTemplateDao().upsert(it) }
                    }
                }
                SyncEntityType.TEMPLATE_ITEM -> {
                    val existing = db.scheduleTemplateItemDao().getByIdAny(userId, remoteId)
                    val localUpd_template_item = existing?.updatedAt ?: Long.MIN_VALUE
                    if (existing != null && existing.syncStatus != syncedRaw && localUpd_template_item >= remoteUpdated) continue
                    val localUpdated = existing?.updatedAt ?: Long.MIN_VALUE
                    if (existing == null || remoteUpdated > localUpdated) {
                        ScheduleTemplateItemSyncMapper.fromFirestore(data, existing)?.let { db.scheduleTemplateItemDao().upsert(it) }
                    }
                }
                SyncEntityType.TEMPLATE_APPLICATION -> {
                    val existing = db.templateApplicationDao().getByIdAny(userId, remoteId)
                    val localDeletedAt = existing?.deletedAt
                    val localUpdated = existing?.updatedAt ?: Long.MIN_VALUE


                    if (existing != null && existing.syncStatus != syncedRaw && localUpdated >= remoteUpdated) continue

                    if (localDeletedAt != null) continue

                    if (existing == null || remoteUpdated > localUpdated ||
                        (existing.syncStatus == syncedRaw && remoteUpdated >= localUpdated)) {
                        val mapped = TemplateApplicationSyncMapper.fromFirestore(data, existing)
                        if (mapped != null) {
                            db.templateApplicationDao().upsert(mapped)
                            if (mapped.deletedAt != null && (existing == null || existing.deletedAt == null)) {
                                val now = System.currentTimeMillis()
                                db.taskDao().softDeleteForTemplateApplication(userId, remoteId, now)
                            }
                        }
                    }
                }
                SyncEntityType.USER_PROFILE -> {
                    val existing = db.userProfileDao().getById(remoteId)
                    val localUpdated = existing?.updatedAt ?: Long.MIN_VALUE
                    val shouldFillMissingName =
                        existing != null &&
                            !UserProfileSyncMapper.hasUsableName(existing) &&
                            UserProfileSyncMapper.hasUsableName(data, existing.email)

                    if (existing != null && existing.syncStatus != syncedRaw && !shouldFillMissingName) continue
                    if (existing == null || remoteUpdated > localUpdated || shouldFillMissingName) {
                        UserProfileSyncMapper.fromFirestore(data, existing)?.let { db.userProfileDao().upsert(it) }
                    }
                }
            }
        }
    }

    private suspend fun deduplicateActiveCategories(userId: String): Boolean {
        val categories = db.categoryDao().getAll(userId)
        val duplicateGroups = categories
            .groupBy { it.name.trim().lowercase() }
            .values
            .filter { it.size > 1 }

        if (duplicateGroups.isEmpty()) return false

        val now = System.currentTimeMillis()
        var didChange = false

        duplicateGroups.forEach { duplicates ->
            val ordered = duplicates.sortedWith(
                compareBy<com.taskplanner.android.data.local.entities.CategoryEntity> { it.sortOrder }
                    .thenBy { it.createdAt }
                    .thenBy { it.id }
            )

            var keeper = ordered.firstOrNull() ?: return@forEach
            ordered.drop(1).forEach { duplicate ->
                val recoveredIcon = keeper.iconName.isBlank() && duplicate.iconName.isNotBlank()
                val recoveredColor = keeper.colorHex.isBlank() && duplicate.colorHex.isNotBlank()
                if (recoveredIcon || recoveredColor) {
                    keeper = keeper.copy(
                        iconName = if (recoveredIcon) duplicate.iconName else keeper.iconName,
                        colorHex = if (recoveredColor) duplicate.colorHex else keeper.colorHex,
                        updatedAt = now,
                        syncStatus = SyncStatus.UPDATED_LOCAL.raw
                    )
                    db.categoryDao().upsert(keeper)
                    didChange = true
                }

                db.taskDao().replaceCategoryId(
                    userId = userId,
                    oldCategoryId = duplicate.id,
                    newCategoryId = keeper.id,
                    updatedAt = now,
                    syncStatus = SyncStatus.UPDATED_LOCAL.raw
                )
                db.scheduleTemplateItemDao().replaceCategoryId(
                    userId = userId,
                    oldCategoryId = duplicate.id,
                    newCategoryId = keeper.id,
                    updatedAt = now,
                    syncStatus = SyncStatus.UPDATED_LOCAL.raw
                )
                db.categoryDao().upsert(
                    duplicate.copy(
                        deletedAt = now,
                        updatedAt = now,
                        syncStatus = SyncStatus.DELETED_LOCAL.raw
                    )
                )
                didChange = true
            }
        }

        return didChange
    }

    private suspend fun repairLegacyDeletedCategoriesIfNeeded(userId: String): Boolean {
        val key = keyLegacyCategoryRepairDone(userId)
        val alreadyRepaired = context.syncDataStore.data.map { it[key] ?: false }.first()
        if (alreadyRepaired) return false

        val categories = db.categoryDao().getAllForUser(userId)
        val groups = categories.groupBy { it.name.trim().lowercase() }
        val now = System.currentTimeMillis()
        var didRepair = false

        groups.values.forEach { group ->
            val active = group.filter { it.deletedAt == null }
            val deleted = group.filter { it.deletedAt != null }

            // Старый дедупликатор на двух устройствах мог выбрать разные ID,
            // из-за чего в облаке удалялись обе копии одной категории.
            if (active.isNotEmpty() || deleted.size < 2) return@forEach

            val keeper = deleted.sortedWith(
                compareBy<com.taskplanner.android.data.local.entities.CategoryEntity> { it.sortOrder }
                    .thenBy { it.createdAt }
                    .thenBy { it.id }
            ).first()

            db.categoryDao().upsert(
                keeper.copy(
                    deletedAt = null,
                    updatedAt = now,
                    syncStatus = SyncStatus.UPDATED_LOCAL.raw
                )
            )

            deleted.filter { it.id != keeper.id }.forEach { duplicate ->
                db.taskDao().replaceCategoryId(
                    userId = userId,
                    oldCategoryId = duplicate.id,
                    newCategoryId = keeper.id,
                    updatedAt = now,
                    syncStatus = SyncStatus.UPDATED_LOCAL.raw
                )
                db.scheduleTemplateItemDao().replaceCategoryId(
                    userId = userId,
                    oldCategoryId = duplicate.id,
                    newCategoryId = keeper.id,
                    updatedAt = now,
                    syncStatus = SyncStatus.UPDATED_LOCAL.raw
                )
            }
            didRepair = true
        }

        context.syncDataStore.edit { preferences ->
            preferences[key] = true
        }
        return didRepair
    }

    private suspend fun normalizeCategoryIcons(userId: String): Boolean {
        val categories = db.categoryDao().getAllForUser(userId)
        val now = System.currentTimeMillis()
        var didChange = false

        categories.forEach { category ->
            val normalized = normalizedCategoryIconName(category.iconName)
            if (category.iconName != normalized) {
                db.categoryDao().upsert(
                    category.copy(
                        iconName = normalized,
                        updatedAt = now,
                        syncStatus = if (category.deletedAt == null) SyncStatus.UPDATED_LOCAL.raw else category.syncStatus
                    )
                )
                didChange = true
            }
        }

        return didChange
    }

    private fun normalizedCategoryIconName(iconName: String): String {
        return when (iconName) {
            "tag" -> "tag.fill"
            "person" -> "person.fill"
            "work" -> "briefcase.fill"
            "home" -> "house.fill"
            "school" -> "book.fill"
            "favorite" -> "heart.fill"
            "fitness_center" -> "figure.run"
            "palette" -> "paintpalette.fill"
            "shopping_cart" -> "cart.fill"
            "directions_car" -> "car.fill"
            "flight" -> "airplane"
            "sports_esports" -> "gamecontroller.fill"
            "" -> "tag.fill"
            else -> iconName
        }
    }

    private suspend fun deduplicateGeneratedRecurrenceTasks(userId: String): Boolean {
        val tasks = db.taskDao().getActiveGeneratedRecurrenceTasks(userId, TaskOriginType.RECURRENCE.raw)
        val duplicateGroups = tasks.groupBy { task ->
            val instanceDay = task.instanceDate ?: task.date
            "${task.recurrenceRuleId.orEmpty()}|$instanceDay"
        }.values.filter { it.size > 1 }

        if (duplicateGroups.isEmpty()) return false

        val now = System.currentTimeMillis()
        var didDeleteDuplicates = false
        duplicateGroups.forEach { duplicates ->
            val ordered = duplicates.sortedWith(
                compareByDescending<com.taskplanner.android.data.local.entities.TaskEntity> {
                    it.status == TaskStatus.COMPLETED.raw
                }
                    .thenByDescending { it.updatedAt }
                    .thenBy { it.createdAt }
            )

            ordered.drop(1).forEach { task ->
                db.taskDao().softDelete(
                    userId = userId,
                    id = task.id,
                    deletedAt = now,
                    updatedAt = now,
                    syncStatus = SyncStatus.DELETED_LOCAL.raw
                )
                didDeleteDuplicates = true
            }
        }

        return didDeleteDuplicates
    }

    
    
    

    private suspend fun uploadLocalChanges(userId: String) {
        val ops = mutableListOf<WriteOperation>()
        val markers = mutableListOf<() -> Unit>()
        val syncedRaw = SyncStatus.SYNCED.raw

        db.userProfileDao().getUnsynced(userId, syncedRaw).forEach { p ->
            val withDeletion = ensureDeletionTimestamps(p)
            ops += WriteOperation(SyncEntityType.USER_PROFILE, p.id, UserProfileSyncMapper.toFirestore(withDeletion))
        }
        db.categoryDao().getUnsynced(userId, syncedRaw).forEach { c ->
            val withDeletion = ensureDeletionTimestampsC(c)
            ops += WriteOperation(SyncEntityType.CATEGORY, c.id, CategorySyncMapper.toFirestore(withDeletion))
        }
        db.goalDao().getUnsynced(userId, syncedRaw).forEach { g ->
            val withDeletion = ensureDeletionTimestampsG(g)
            ops += WriteOperation(SyncEntityType.GOAL, g.id, GoalSyncMapper.toFirestore(withDeletion))
        }
        db.goalStepDao().getUnsynced(userId, syncedRaw).forEach { s ->
            val withDeletion = ensureDeletionTimestampsGS(s)
            ops += WriteOperation(SyncEntityType.GOAL_STEP, s.id, GoalStepSyncMapper.toFirestore(withDeletion))
        }
        db.scheduleTemplateDao().getUnsynced(userId, syncedRaw).forEach { t ->
            val withDeletion = ensureDeletionTimestampsT(t)
            ops += WriteOperation(SyncEntityType.TEMPLATE, t.id, ScheduleTemplateSyncMapper.toFirestore(withDeletion))
        }
        db.scheduleTemplateItemDao().getUnsynced(userId, syncedRaw).forEach { i ->
            val withDeletion = ensureDeletionTimestampsTI(i)
            ops += WriteOperation(SyncEntityType.TEMPLATE_ITEM, i.id, ScheduleTemplateItemSyncMapper.toFirestore(withDeletion))
        }
        db.templateApplicationDao().getUnsynced(userId, syncedRaw).forEach { a ->
            val withDeletion = ensureDeletionTimestampsTA(a)
            ops += WriteOperation(SyncEntityType.TEMPLATE_APPLICATION, a.id, TemplateApplicationSyncMapper.toFirestore(withDeletion))
        }
        db.recurrenceRuleDao().getUnsynced(userId, syncedRaw).forEach { r ->
            val withDeletion = ensureDeletionTimestampsR(r)
            ops += WriteOperation(SyncEntityType.RECURRENCE_RULE, r.id, RecurrenceRuleSyncMapper.toFirestore(withDeletion))
        }
        db.taskDao().getUnsynced(userId, syncedRaw).forEach { t ->
            val withDeletion = ensureDeletionTimestampsTk(t)
            ops += WriteOperation(SyncEntityType.TASK, t.id, TaskSyncMapper.toFirestore(withDeletion))
        }

        if (ops.isEmpty()) return
        firestore.uploadBatch(userId, ops)

        for (op in ops) {
            when (op.type) {
                SyncEntityType.TASK -> db.taskDao().markSynced(userId, op.documentId, syncedRaw)
                SyncEntityType.CATEGORY -> db.categoryDao().markSynced(userId, op.documentId, syncedRaw)
                SyncEntityType.GOAL -> db.goalDao().markSynced(userId, op.documentId, syncedRaw)
                SyncEntityType.GOAL_STEP -> db.goalStepDao().markSynced(userId, op.documentId, syncedRaw)
                SyncEntityType.RECURRENCE_RULE -> db.recurrenceRuleDao().markSynced(userId, op.documentId, syncedRaw)
                SyncEntityType.TEMPLATE -> db.scheduleTemplateDao().markSynced(userId, op.documentId, syncedRaw)
                SyncEntityType.TEMPLATE_ITEM -> db.scheduleTemplateItemDao().markSynced(userId, op.documentId, syncedRaw)
                SyncEntityType.TEMPLATE_APPLICATION -> db.templateApplicationDao().markSynced(userId, op.documentId, syncedRaw)
                SyncEntityType.USER_PROFILE -> db.userProfileDao().markSynced(op.documentId, syncedRaw)
            }
        }
    }

    private suspend fun uploadAllLocalForInitialSync(userId: String) {
        val ops = mutableListOf<WriteOperation>()
        val syncedRaw = SyncStatus.SYNCED.raw

        db.userProfileDao().getAllForUser(userId).forEach { p ->
            ops += WriteOperation(SyncEntityType.USER_PROFILE, p.id, UserProfileSyncMapper.toFirestore(p))
        }
        db.categoryDao().getAllForUser(userId).forEach { c ->
            ops += WriteOperation(SyncEntityType.CATEGORY, c.id, CategorySyncMapper.toFirestore(c))
        }
        db.goalDao().getAllForUser(userId).forEach { g ->
            ops += WriteOperation(SyncEntityType.GOAL, g.id, GoalSyncMapper.toFirestore(g))
        }
        db.goalStepDao().getAllForUser(userId).forEach { s ->
            ops += WriteOperation(SyncEntityType.GOAL_STEP, s.id, GoalStepSyncMapper.toFirestore(s))
        }
        db.scheduleTemplateDao().getAllForUser(userId).forEach { t ->
            ops += WriteOperation(SyncEntityType.TEMPLATE, t.id, ScheduleTemplateSyncMapper.toFirestore(t))
        }
        db.scheduleTemplateItemDao().getAllForUser(userId).forEach { i ->
            ops += WriteOperation(SyncEntityType.TEMPLATE_ITEM, i.id, ScheduleTemplateItemSyncMapper.toFirestore(i))
        }
        db.templateApplicationDao().getAllForUser(userId).forEach { a ->
            ops += WriteOperation(SyncEntityType.TEMPLATE_APPLICATION, a.id, TemplateApplicationSyncMapper.toFirestore(a))
        }
        db.recurrenceRuleDao().getAllForUser(userId).forEach { r ->
            ops += WriteOperation(SyncEntityType.RECURRENCE_RULE, r.id, RecurrenceRuleSyncMapper.toFirestore(r))
        }
        db.taskDao().getAllForUser(userId).forEach { t ->
            ops += WriteOperation(SyncEntityType.TASK, t.id, TaskSyncMapper.toFirestore(t))
        }

        if (ops.isEmpty()) return
        firestore.uploadBatch(userId, ops)

        for (op in ops) {
            when (op.type) {
                SyncEntityType.TASK -> db.taskDao().markSynced(userId, op.documentId, syncedRaw)
                SyncEntityType.CATEGORY -> db.categoryDao().markSynced(userId, op.documentId, syncedRaw)
                SyncEntityType.GOAL -> db.goalDao().markSynced(userId, op.documentId, syncedRaw)
                SyncEntityType.GOAL_STEP -> db.goalStepDao().markSynced(userId, op.documentId, syncedRaw)
                SyncEntityType.RECURRENCE_RULE -> db.recurrenceRuleDao().markSynced(userId, op.documentId, syncedRaw)
                SyncEntityType.TEMPLATE -> db.scheduleTemplateDao().markSynced(userId, op.documentId, syncedRaw)
                SyncEntityType.TEMPLATE_ITEM -> db.scheduleTemplateItemDao().markSynced(userId, op.documentId, syncedRaw)
                SyncEntityType.TEMPLATE_APPLICATION -> db.templateApplicationDao().markSynced(userId, op.documentId, syncedRaw)
                SyncEntityType.USER_PROFILE -> db.userProfileDao().markSynced(op.documentId, syncedRaw)
            }
        }
    }

    
    
    
    

    private fun ensureDeletionTimestamps(p: com.taskplanner.android.data.local.entities.UserProfileEntity) =
        if (p.syncStatus == SyncStatus.DELETED_LOCAL.raw && p.deletedAt == null) {
            val now = System.currentTimeMillis()
            p.copy(deletedAt = now, updatedAt = now)
        } else p

    private fun ensureDeletionTimestampsC(c: com.taskplanner.android.data.local.entities.CategoryEntity) =
        if (c.syncStatus == SyncStatus.DELETED_LOCAL.raw && c.deletedAt == null) {
            val now = System.currentTimeMillis()
            c.copy(deletedAt = now, updatedAt = now)
        } else c

    private fun ensureDeletionTimestampsG(g: com.taskplanner.android.data.local.entities.GoalEntity) =
        if (g.syncStatus == SyncStatus.DELETED_LOCAL.raw && g.deletedAt == null) {
            val now = System.currentTimeMillis()
            g.copy(deletedAt = now, updatedAt = now)
        } else g

    private fun ensureDeletionTimestampsGS(s: com.taskplanner.android.data.local.entities.GoalStepEntity) =
        if (s.syncStatus == SyncStatus.DELETED_LOCAL.raw && s.deletedAt == null) {
            val now = System.currentTimeMillis()
            s.copy(deletedAt = now, updatedAt = now)
        } else s

    private fun ensureDeletionTimestampsT(t: com.taskplanner.android.data.local.entities.ScheduleTemplateEntity) =
        if (t.syncStatus == SyncStatus.DELETED_LOCAL.raw && t.deletedAt == null) {
            val now = System.currentTimeMillis()
            t.copy(deletedAt = now, updatedAt = now)
        } else t

    private fun ensureDeletionTimestampsTI(i: com.taskplanner.android.data.local.entities.ScheduleTemplateItemEntity) =
        if (i.syncStatus == SyncStatus.DELETED_LOCAL.raw && i.deletedAt == null) {
            val now = System.currentTimeMillis()
            i.copy(deletedAt = now, updatedAt = now)
        } else i

    private fun ensureDeletionTimestampsTA(a: com.taskplanner.android.data.local.entities.TemplateApplicationEntity) =
        if (a.syncStatus == SyncStatus.DELETED_LOCAL.raw && a.deletedAt == null) {
            val now = System.currentTimeMillis()
            a.copy(deletedAt = now, updatedAt = now)
        } else a

    private fun ensureDeletionTimestampsR(r: com.taskplanner.android.data.local.entities.RecurrenceRuleEntity) =
        if (r.syncStatus == SyncStatus.DELETED_LOCAL.raw && r.deletedAt == null) {
            val now = System.currentTimeMillis()
            r.copy(deletedAt = now, updatedAt = now)
        } else r

    private fun ensureDeletionTimestampsTk(t: com.taskplanner.android.data.local.entities.TaskEntity) =
        if (t.syncStatus == SyncStatus.DELETED_LOCAL.raw && t.deletedAt == null) {
            val now = System.currentTimeMillis()
            t.copy(deletedAt = now, updatedAt = now)
        } else t
}
