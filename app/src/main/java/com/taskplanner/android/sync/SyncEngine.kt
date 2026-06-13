package com.taskplanner.android.sync

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.taskplanner.android.core.model.SyncStatus
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
    @Volatile private var pendingSync: String? = null

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val keyLastSync = longPreferencesKey("lastSyncDate")
    private fun keyInitialDone(uid: String) = booleanPreferencesKey("initialSyncCompleted_$uid")

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
                    
                    if (existing != null && existing.syncStatus != syncedRaw) continue
                    val localUpdated = existing?.updatedAt ?: Long.MIN_VALUE
                    if (existing == null || remoteUpdated > localUpdated) {
                        TaskSyncMapper.fromFirestore(data, existing)?.let { task ->
                            db.taskDao().upsert(task)
                            
                            com.taskplanner.android.notifications.NotificationHelper.cancelReminder(context, task.id)
                            if (task.hasReminder && task.deletedAt == null) {
                                com.taskplanner.android.notifications.NotificationHelper.scheduleReminder(context, task)
                            }
                        }
                    }
                }
                SyncEntityType.CATEGORY -> {
                    val existing = db.categoryDao().getByIdAny(userId, remoteId)
                        ?: SyncMapperHelpers.stringFromAny(data["name"])
                            ?.let { db.categoryDao().getByName(userId, it) }
                    if (existing != null && existing.syncStatus != syncedRaw) continue
                    val localUpdated = existing?.updatedAt ?: Long.MIN_VALUE
                    if (existing == null || remoteUpdated > localUpdated) {
                        CategorySyncMapper.fromFirestore(data, existing)?.let { db.categoryDao().upsert(it) }
                    }
                }
                SyncEntityType.GOAL -> {
                    val existing = db.goalDao().getByIdAny(userId, remoteId)
                    if (existing != null && existing.syncStatus != syncedRaw) continue
                    val localUpdated = existing?.updatedAt ?: Long.MIN_VALUE
                    if (existing == null || remoteUpdated > localUpdated) {
                        GoalSyncMapper.fromFirestore(data, existing)?.let { db.goalDao().upsert(it) }
                    }
                }
                SyncEntityType.GOAL_STEP -> {
                    val existing = db.goalStepDao().getByIdAny(userId, remoteId)
                    if (existing != null && existing.syncStatus != syncedRaw) continue
                    val localUpdated = existing?.updatedAt ?: Long.MIN_VALUE
                    if (existing == null || remoteUpdated > localUpdated) {
                        GoalStepSyncMapper.fromFirestore(data, existing)?.let { db.goalStepDao().upsert(it) }
                    }
                }
                SyncEntityType.RECURRENCE_RULE -> {
                    val existing = db.recurrenceRuleDao().getByIdAny(userId, remoteId)
                    if (existing != null && existing.syncStatus != syncedRaw) continue
                    val localUpdated = existing?.updatedAt ?: Long.MIN_VALUE
                    if (existing == null || remoteUpdated > localUpdated) {
                        RecurrenceRuleSyncMapper.fromFirestore(data, existing)?.let { db.recurrenceRuleDao().upsert(it) }
                    }
                }
                SyncEntityType.TEMPLATE -> {
                    val existing = db.scheduleTemplateDao().getByIdAny(userId, remoteId)
                    if (existing != null && existing.syncStatus != syncedRaw) continue
                    val localUpdated = existing?.updatedAt ?: Long.MIN_VALUE
                    if (existing == null || remoteUpdated > localUpdated) {
                        ScheduleTemplateSyncMapper.fromFirestore(data, existing)?.let { db.scheduleTemplateDao().upsert(it) }
                    }
                }
                SyncEntityType.TEMPLATE_ITEM -> {
                    val existing = db.scheduleTemplateItemDao().getByIdAny(userId, remoteId)
                    if (existing != null && existing.syncStatus != syncedRaw) continue
                    val localUpdated = existing?.updatedAt ?: Long.MIN_VALUE
                    if (existing == null || remoteUpdated > localUpdated) {
                        ScheduleTemplateItemSyncMapper.fromFirestore(data, existing)?.let { db.scheduleTemplateItemDao().upsert(it) }
                    }
                }
                SyncEntityType.TEMPLATE_APPLICATION -> {
                    val existing = db.templateApplicationDao().getByIdAny(userId, remoteId)
                    if (existing != null && existing.syncStatus != syncedRaw) continue
                    val localUpdated = existing?.updatedAt ?: Long.MIN_VALUE
                    if (existing == null || remoteUpdated > localUpdated) {
                        TemplateApplicationSyncMapper.fromFirestore(data, existing)?.let { db.templateApplicationDao().upsert(it) }
                    }
                }
                SyncEntityType.USER_PROFILE -> {
                    val existing = db.userProfileDao().getById(remoteId)
                    if (existing != null && existing.syncStatus != syncedRaw) continue
                    val localUpdated = existing?.updatedAt ?: Long.MIN_VALUE
                    if (existing == null || remoteUpdated > localUpdated) {
                        UserProfileSyncMapper.fromFirestore(data, existing)?.let { db.userProfileDao().upsert(it) }
                    }
                }
            }
        }
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
