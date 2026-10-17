package com.example.data.service

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GitHubRepo(
    val id: Long,
    val name: String,
    val fullName: String,
    val description: String,
    val htmlUrl: String,
    val starsCount: Int,
    val language: String,
    val ownerLogin: String,
    val defaultBranch: String
)

data class GitHubContentItem(
    val name: String,
    val path: String,
    val type: String, // "file" or "dir"
    val downloadUrl: String?,
    val size: Long
)

data class GitHubRepoInspection(
    val repoFullName: String,
    val fileList: List<String>,
    val documentationText: String,
    val codePreviews: Map<String, String>,
    val architectureSummary: String
)

class GitHubService {

    private val tag = "GitHubService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun searchRepositories(query: String): Result<List<GitHubRepo>> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://api.github.com/search/repositories?q=$encodedQuery&sort=stars&order=desc&per_page=10"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Kavya-AI-Assistant-Android")
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("GitHub API ${response.code}: $responseBody"))
            }

            val json = JSONObject(responseBody)
            val items = json.optJSONArray("items") ?: JSONArray()
            val repos = mutableListOf<GitHubRepo>()

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val owner = item.optJSONObject("owner")
                repos.add(
                    GitHubRepo(
                        id = item.optLong("id"),
                        name = item.optString("name"),
                        fullName = item.optString("full_name"),
                        description = item.optString("description", "No description provided."),
                        htmlUrl = item.optString("html_url"),
                        starsCount = item.optInt("stargazers_count", 0),
                        language = item.optString("language", "Unknown"),
                        ownerLogin = owner?.optString("login") ?: "unknown",
                        defaultBranch = item.optString("default_branch", "main")
                    )
                )
            }

            Result.success(repos)
        } catch (e: Exception) {
            Log.e(tag, "GitHub repository search failed", e)
            Result.failure(e)
        }
    }

    suspend fun getRepoContents(owner: String, repo: String, path: String = ""): Result<List<GitHubContentItem>> = withContext(Dispatchers.IO) {
        try {
            val cleanPath = if (path.startsWith("/")) path.drop(1) else path
            val url = "https://api.github.com/repos/$owner/$repo/contents/$cleanPath"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Kavya-AI-Assistant-Android")
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("GitHub API ${response.code}: $responseBody"))
            }

            val list = mutableListOf<GitHubContentItem>()
            if (responseBody.trim().startsWith("[")) {
                val jsonArray = JSONArray(responseBody)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        GitHubContentItem(
                            name = obj.optString("name"),
                            path = obj.optString("path"),
                            type = obj.optString("type"),
                            downloadUrl = obj.optString("download_url", null),
                            size = obj.optLong("size", 0)
                        )
                    )
                }
            } else if (responseBody.trim().startsWith("{")) {
                val obj = JSONObject(responseBody)
                list.add(
                    GitHubContentItem(
                        name = obj.optString("name"),
                        path = obj.optString("path"),
                        type = obj.optString("type"),
                        downloadUrl = obj.optString("download_url", null),
                        size = obj.optLong("size", 0)
                    )
                )
            }

            Result.success(list)
        } catch (e: Exception) {
            Log.e(tag, "Get repo contents failed", e)
            Result.failure(e)
        }
    }

    suspend fun fetchRawFileContent(owner: String, repo: String, filePath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanPath = if (filePath.startsWith("/")) filePath.drop(1) else filePath
            val url = "https://api.github.com/repos/$owner/$repo/contents/$cleanPath"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Kavya-AI-Assistant-Android")
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("GitHub API ${response.code}: $responseBody"))
            }

            val json = JSONObject(responseBody)
            val encoding = json.optString("encoding")
            val content = json.optString("content")

            if (encoding == "base64" && content.isNotBlank()) {
                val cleanBase64 = content.replace("\n", "").replace("\r", "")
                val decoded = String(Base64.decode(cleanBase64, Base64.DEFAULT))
                return@withContext Result.success(decoded)
            }

            Result.success(content)
        } catch (e: Exception) {
            Log.e(tag, "Fetch raw file content failed", e)
            Result.failure(e)
        }
    }

    suspend fun inspectRepositoryStructure(owner: String, repo: String): Result<GitHubRepoInspection> = withContext(Dispatchers.IO) {
        try {
            val rootContentsResult = getRepoContents(owner, repo, "")
            val rootItems = rootContentsResult.getOrDefault(emptyList())
            val filePaths = rootItems.map { it.path }

            // Fetch documentation (README.md or similar)
            val readmeItem = rootItems.firstOrNull { it.name.equals("README.md", ignoreCase = true) || it.name.contains("readme", ignoreCase = true) }
            val docText = if (readmeItem != null) {
                fetchRawFileContent(owner, repo, readmeItem.path).getOrDefault("No documentation content found.")
            } else "No README file found in root directory."

            // Fetch key source code / config files
            val codePreviews = mutableMapOf<String, String>()
            val codeFilesToFetch = rootItems.filter { item ->
                item.type == "file" && (
                    item.name.endsWith(".kt", true) ||
                    item.name.endsWith(".java", true) ||
                    item.name.endsWith(".json", true) ||
                    item.name.endsWith(".gradle.kts", true) ||
                    item.name.endsWith(".py", true) ||
                    item.name.endsWith(".js", true) ||
                    item.name.endsWith(".ts", true) ||
                    item.name.endsWith(".md", true)
                )
            }.take(5)

            for (codeFile in codeFilesToFetch) {
                val contentResult = fetchRawFileContent(owner, repo, codeFile.path)
                if (contentResult.isSuccess) {
                    val codeText = contentResult.getOrThrow()
                    codePreviews[codeFile.path] = codeText.take(1500)
                }
            }

            val summary = "Repository '$owner/$repo' contains ${rootItems.size} root items. File structure: ${filePaths.joinToString(", ")}."
            Result.success(
                GitHubRepoInspection(
                    repoFullName = "$owner/$repo",
                    fileList = filePaths,
                    documentationText = docText.take(3000),
                    codePreviews = codePreviews,
                    architectureSummary = summary
                )
            )
        } catch (e: Exception) {
            Log.e(tag, "Inspect repository structure failed", e)
            Result.failure(e)
        }
    }
}
