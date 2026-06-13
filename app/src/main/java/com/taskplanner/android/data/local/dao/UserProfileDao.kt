package com.taskplanner.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.taskplanner.android.data.local.entities.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE id = :userId AND syncStatus != :syncedRaw")
    suspend fun getUnsynced(userId: String, syncedRaw: Int): List<UserProfileEntity>

    @Query("SELECT * FROM user_profiles WHERE id = :userId")
    suspend fun getAllForUser(userId: String): List<UserProfileEntity>

    @Query("UPDATE user_profiles SET syncStatus = :syncedRaw WHERE id = :id")
    suspend fun markSynced(id: String, syncedRaw: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)
}
