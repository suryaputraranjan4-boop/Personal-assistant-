package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "dynamic_skills")
data class DynamicSkill(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val triggerPhrases: String, // Comma separated phrases, e.g. "play music, start music"
    val category: String = "General",
    val executionType: String = "SYSTEM_ACTION", // SYSTEM_ACTION, PROMPT_TEMPLATE, GITHUB_INTEGRATION, SCRIPT
    val payloadOrCode: String, // Command payload, GitHub repo ref, or instruction template
    val isVerified: Boolean = true,
    val sourceRepo: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
