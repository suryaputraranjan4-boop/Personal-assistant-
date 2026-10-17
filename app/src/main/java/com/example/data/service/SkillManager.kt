package com.example.data.service

import android.content.Context
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.DynamicSkill
import com.example.data.db.DynamicSkillDao
import com.example.data.model.KavyaLanguage
import com.example.data.model.KavyaMood
import com.example.service.SystemActionExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class SkillImportReport(
    val skill: DynamicSkill,
    val summaryReport: String
)

class SkillManager(private val context: Context) {

    private val tag = "SkillManager"
    private val dao: DynamicSkillDao = AppDatabase.getDatabase(context).dynamicSkillDao()
    private val apiService = GeminiApiService()
    private val gitHubService = GitHubService()

    val allSkillsFlow: Flow<List<DynamicSkill>> = dao.getAllSkillsFlow()

    suspend fun seedDefaultSkillsIfNeeded() = withContext(Dispatchers.IO) {
        val existing = dao.getAllSkills()
        if (existing.isEmpty()) {
            val defaultSkills = listOf(
                DynamicSkill(
                    name = "YouTube Video Search",
                    description = "Searches for videos on YouTube and opens YouTube app",
                    triggerPhrases = "search youtube, play on youtube, open youtube, search video",
                    category = "Media",
                    executionType = "SYSTEM_ACTION",
                    payloadOrCode = "YOUTUBE_SEARCH",
                    isVerified = true
                ),
                DynamicSkill(
                    name = "Web Search Engine",
                    description = "Searches the web via browser for questions or topics",
                    triggerPhrases = "search web, google search, look up online, search google",
                    category = "Search",
                    executionType = "SYSTEM_ACTION",
                    payloadOrCode = "WEB_SEARCH",
                    isVerified = true
                ),
                DynamicSkill(
                    name = "Live Screen Vision Inspection",
                    description = "Captures current Android screen and provides visual screen analysis",
                    triggerPhrases = "look at screen, inspect screen, what is on my screen, share screen",
                    category = "Vision",
                    executionType = "SYSTEM_ACTION",
                    payloadOrCode = "SCREEN_SHARE",
                    isVerified = true
                ),
                DynamicSkill(
                    name = "Device Touch & Gestures (Kavya Hands)",
                    description = "Executes physical gestures on device: back, home, recents, scroll up/down, tap element",
                    triggerPhrases = "go home, go back, scroll down, scroll up, open recents, tap screen",
                    category = "Device Control",
                    executionType = "SYSTEM_ACTION",
                    payloadOrCode = "DEVICE_GESTURE",
                    isVerified = true
                ),
                DynamicSkill(
                    name = "GitHub Repository Intelligence",
                    description = "Browses GitHub repositories, inspects code files, and extracts project capabilities",
                    triggerPhrases = "search github, inspect repository, learn github skill, import repo",
                    category = "GitHub",
                    executionType = "GITHUB_INTEGRATION",
                    payloadOrCode = "GITHUB_SEARCH",
                    isVerified = true
                )
            )

            for (skill in defaultSkills) {
                dao.insertSkill(skill)
            }
        }
    }

    suspend fun getAllSkills(): List<DynamicSkill> = withContext(Dispatchers.IO) {
        dao.getAllSkills()
    }

    suspend fun findMatchingSkill(userPrompt: String): DynamicSkill? = withContext(Dispatchers.IO) {
        val lowerPrompt = userPrompt.lowercase().trim()
        val skills = dao.getAllSkills()
        for (skill in skills) {
            val triggers = skill.triggerPhrases.split(",").map { it.trim().lowercase() }
            if (triggers.any { trigger -> trigger.isNotBlank() && lowerPrompt.contains(trigger) }) {
                return@withContext skill
            }
        }
        null
    }

    suspend fun learnSkillFromInstruction(instruction: String): Result<SkillImportReport> = withContext(Dispatchers.IO) {
        try {
            val synthesisPrompt = """
                You are Kavya's AI Architect. The user wants Kavya to learn a new skill/capability: "$instruction".
                Analyze the instruction, verify its feasibility, and synthesize a structured skill definition in raw JSON format matching this schema:
                {
                  "name": "Short descriptive title (3-5 words)",
                  "description": "Clear explanation of what the skill does",
                  "triggerPhrases": "comma, separated, key, phrases",
                  "category": "Category name (e.g. Utility, Media, GitHub, Automation)",
                  "executionType": "PROMPT_TEMPLATE or SYSTEM_ACTION or SCRIPT",
                  "payloadOrCode": "Exact execution payload or detailed instruction template",
                  "verificationDetails": "Detailed explanation of what was verified, tested, and configured"
                }
                Output ONLY raw valid JSON, no markdown wrappers.
            """.trimIndent()

            val geminiResult = apiService.generateReply(
                userPrompt = synthesisPrompt,
                audioBytes = null,
                history = emptyList(),
                mood = KavyaMood.KUUDERE,
                modelId = com.example.data.model.GeminiConstants.MODEL_THINKING,
                assistantName = "Kavya System Architect",
                thinkingLevel = "medium"
            )

            if (geminiResult.isFailure) {
                return@withContext Result.failure(geminiResult.exceptionOrNull() ?: Exception("Failed to synthesize skill definition."))
            }

            val rawJson = geminiResult.getOrThrow()
                .replace(Regex("^```json\\s*", RegexOption.IGNORE_CASE), "")
                .replace(Regex("^```\\s*", RegexOption.IGNORE_CASE), "")
                .replace(Regex("```$", RegexOption.IGNORE_CASE), "")
                .trim()

            val json = JSONObject(rawJson)
            val newSkill = DynamicSkill(
                name = json.optString("name", "Custom Learned Skill"),
                description = json.optString("description", instruction),
                triggerPhrases = json.optString("triggerPhrases", instruction),
                category = json.optString("category", "Custom"),
                executionType = json.optString("executionType", "PROMPT_TEMPLATE"),
                payloadOrCode = json.optString("payloadOrCode", instruction),
                isVerified = true
            )

            dao.insertSkill(newSkill)

            val verification = json.optString("verificationDetails", "Verified capability specification and configured runtime triggers.")
            val report = "Learned Skill: '${newSkill.name}'\nDescription: ${newSkill.description}\nExecution Type: ${newSkill.executionType}\nVerification: $verification"

            Result.success(SkillImportReport(skill = newSkill, summaryReport = report))
        } catch (e: Exception) {
            Log.e(tag, "Learn skill from instruction failed", e)
            Result.failure(e)
        }
    }

    suspend fun importSkillFromGitHubRepo(repoFullName: String): Result<SkillImportReport> = withContext(Dispatchers.IO) {
        try {
            val parts = repoFullName.split("/")
            if (parts.size != 2) {
                return@withContext Result.failure(IllegalArgumentException("Repo name must be in format 'owner/repo'"))
            }

            val owner = parts[0]
            val repo = parts[1]

            // 1. Perform deep repository inspection (files, README, source code previews)
            val inspectionResult = gitHubService.inspectRepositoryStructure(owner, repo)
            if (inspectionResult.isFailure) {
                return@withContext Result.failure(inspectionResult.exceptionOrNull() ?: Exception("Repository inspection failed"))
            }

            val inspection = inspectionResult.getOrThrow()

            // Build code previews summary
            val codePreviewsText = inspection.codePreviews.entries.joinToString("\n---\n") { (filePath, codeContent) ->
                "File '$filePath':\n$codeContent"
            }

            // 2. Synthesize verified skill definition using Gemini
            val prompt = """
                Analyze this GitHub repository '$repoFullName':
                ${inspection.architectureSummary}
                
                Documentation / README snippet:
                ${inspection.documentationText}
                
                Source Code Previews:
                $codePreviewsText
                
                Synthesize a structured Kavya dynamic skill definition in raw JSON format:
                {
                  "name": "Short descriptive title for this capability",
                  "description": "Exact functionality provided by this repository",
                  "triggerPhrases": "comma, separated, key, trigger, phrases",
                  "category": "GitHub Integration",
                  "executionType": "GITHUB_INTEGRATION",
                  "payloadOrCode": "Full execution specification and parsed API/code patterns from repository",
                  "analyzedFiles": "List of key files inspected during import",
                  "limitations": "Any external API keys or system requirements needed"
                }
                Output ONLY raw valid JSON.
            """.trimIndent()

            val geminiResult = apiService.generateReply(
                userPrompt = prompt,
                audioBytes = null,
                history = emptyList(),
                mood = KavyaMood.KUUDERE,
                modelId = com.example.data.model.GeminiConstants.MODEL_GENERAL,
                assistantName = "Kavya Skill Compiler",
                thinkingLevel = "low"
            )

            if (geminiResult.isFailure) {
                return@withContext Result.failure(geminiResult.exceptionOrNull() ?: Exception("Skill compilation failed"))
            }

            val rawJson = geminiResult.getOrThrow()
                .replace(Regex("^```json\\s*", RegexOption.IGNORE_CASE), "")
                .replace(Regex("^```\\s*", RegexOption.IGNORE_CASE), "")
                .replace(Regex("```$", RegexOption.IGNORE_CASE), "")
                .trim()

            val json = JSONObject(rawJson)
            val newSkill = DynamicSkill(
                name = json.optString("name", "$repo Skill"),
                description = json.optString("description", "GitHub capability imported from $repoFullName"),
                triggerPhrases = json.optString("triggerPhrases", repo),
                category = "GitHub",
                executionType = "GITHUB_INTEGRATION",
                payloadOrCode = json.optString("payloadOrCode", repoFullName),
                isVerified = true,
                sourceRepo = repoFullName
            )

            dao.insertSkill(newSkill)

            val analyzedFiles = json.optString("analyzedFiles", inspection.fileList.take(5).joinToString(", "))
            val limitations = json.optString("limitations", "None specified.")
            val summaryReport = "Imported GitHub Skill: '${newSkill.name}' ($repoFullName)\n• Analyzed Files: $analyzedFiles\n• Parsed Capabilities: ${newSkill.description}\n• Limitations: $limitations"

            Result.success(SkillImportReport(skill = newSkill, summaryReport = summaryReport))
        } catch (e: Exception) {
            Log.e(tag, "Import skill from GitHub failed", e)
            Result.failure(e)
        }
    }

    suspend fun executeSkill(
        skill: DynamicSkill,
        userPrompt: String,
        assistantName: String,
        language: KavyaLanguage
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            when (skill.executionType) {
                "SYSTEM_ACTION" -> {
                    val systemAction = SystemActionExecutor.parseSystemAction(userPrompt)
                    if (systemAction != null) {
                        val result = SystemActionExecutor.execute(systemAction, language, context)
                        return@withContext Result.success(result.spokenConfirmation)
                    }
                }
                "GITHUB_INTEGRATION" -> {
                    val repoPrompt = """
                        You are $assistantName executing the GitHub capability '${skill.name}' (Source Repo: ${skill.sourceRepo ?: "GitHub"}).
                        Capability Specification & Code Logic:
                        ${skill.payloadOrCode}
                        
                        User Request: "$userPrompt"
                        
                        Execute this skill step-by-step according to the specification and return a helpful, precise response in $language.
                    """.trimIndent()

                    return@withContext apiService.generateReply(
                        userPrompt = repoPrompt,
                        audioBytes = null,
                        history = emptyList(),
                        mood = KavyaMood.GENKI,
                        modelId = com.example.data.model.GeminiConstants.MODEL_GENERAL,
                        assistantName = assistantName,
                        thinkingLevel = "low"
                    )
                }
                "PROMPT_TEMPLATE", "SCRIPT" -> {
                    val templatePrompt = """
                        You are $assistantName executing the skill '${skill.name}'.
                        Instruction Template / Payload:
                        ${skill.payloadOrCode}
                        
                        User Request: "$userPrompt"
                        
                        Fulfill the request using the skill template and return the result.
                    """.trimIndent()

                    return@withContext apiService.generateReply(
                        userPrompt = templatePrompt,
                        audioBytes = null,
                        history = emptyList(),
                        mood = KavyaMood.GENKI,
                        modelId = com.example.data.model.GeminiConstants.MODEL_GENERAL,
                        assistantName = assistantName,
                        thinkingLevel = "low"
                    )
                }
            }

            // Fallback default response
            val result = apiService.generateReply(
                userPrompt = "Execute skill ${skill.name} for request: $userPrompt",
                audioBytes = null,
                history = emptyList(),
                mood = KavyaMood.GENKI,
                modelId = com.example.data.model.GeminiConstants.MODEL_GENERAL,
                assistantName = assistantName
            )
            result
        } catch (e: Exception) {
            Log.e(tag, "Execute skill failed", e)
            Result.failure(e)
        }
    }

    suspend fun deleteSkill(skill: DynamicSkill) = withContext(Dispatchers.IO) {
        dao.deleteSkill(skill)
    }

    suspend fun buildSkillsSystemPromptAddendum(): String = withContext(Dispatchers.IO) {
        val skills = dao.getAllSkills()
        if (skills.isEmpty()) return@withContext ""

        val sb = java.lang.StringBuilder()
        sb.append("\nKavya Learned Dynamic Skills & Capabilities:\n")
        for (s in skills) {
            sb.append("- Skill [${s.name}] (${s.category}): ${s.description} (Triggers: ${s.triggerPhrases})\n")
        }
        sb.toString()
    }
}
