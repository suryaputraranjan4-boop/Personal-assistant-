package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DynamicSkillDao {

    @Query("SELECT * FROM dynamic_skills ORDER BY createdAt DESC")
    fun getAllSkillsFlow(): Flow<List<DynamicSkill>>

    @Query("SELECT * FROM dynamic_skills ORDER BY createdAt DESC")
    suspend fun getAllSkills(): List<DynamicSkill>

    @Query("SELECT * FROM dynamic_skills WHERE id = :id LIMIT 1")
    suspend fun getSkillById(id: String): DynamicSkill?

    @Query("SELECT * FROM dynamic_skills WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR triggerPhrases LIKE '%' || :query || '%'")
    suspend fun searchSkills(query: String): List<DynamicSkill>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkill(skill: DynamicSkill)

    @Update
    suspend fun updateSkill(skill: DynamicSkill)

    @Delete
    suspend fun deleteSkill(skill: DynamicSkill)

    @Query("DELETE FROM dynamic_skills")
    suspend fun clearAllSkills()
}
