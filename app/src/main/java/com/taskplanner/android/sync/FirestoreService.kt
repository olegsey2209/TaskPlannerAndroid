package com.taskplanner.android.sync

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class WriteOperation(
    val type: SyncEntityType,
    val documentId: String,
    val data: Map<String, Any?>
)

class FirestoreService {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private fun collection(userId: String, type: SyncEntityType) =
        db.collection("users").document(userId).collection(type.collection)

    suspend fun isCollectionEmpty(userId: String, type: SyncEntityType): Boolean {
        val snapshot = collection(userId, type).limit(1).get().await()
        return snapshot.isEmpty
    }

    suspend fun loadDocuments(userId: String, type: SyncEntityType): List<Map<String, Any?>> {
        val snapshot = collection(userId, type).get().await()
        return snapshot.documents.map { doc ->
            val data = doc.data?.toMutableMap() ?: mutableMapOf()
            if (data["id"] == null) {
                data["id"] = doc.id
            }
            data
        }
    }

    
    suspend fun uploadBatch(userId: String, operations: List<WriteOperation>) {
        if (operations.isEmpty()) return
        val chunkSize = 450
        var index = 0
        while (index < operations.size) {
            val end = minOf(index + chunkSize, operations.size)
            val slice = operations.subList(index, end)
            val batch = db.batch()
            for (op in slice) {
                val ref = collection(userId, op.type).document(op.documentId)
                
                batch.set(ref, op.data, com.google.firebase.firestore.SetOptions.merge())
            }
            batch.commit().await()
            index = end
        }
    }
}
