package com.example.service

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import com.example.data.model.KavyaLanguage
import java.net.URLEncoder

sealed class SystemAction {
    object ScrollDown : SystemAction()
    object ScrollUp : SystemAction()
    object LongPress : SystemAction()
    object LongPressAndScrollUp : SystemAction()
    data class ClickTarget(val targetName: String) : SystemAction()
    object DoubleTap : SystemAction()
    object Home : SystemAction()
    object Back : SystemAction()
    object Recents : SystemAction()
    object Notifications : SystemAction()
    object QuickSettings : SystemAction()
    object Screenshot : SystemAction()
    object LockScreen : SystemAction()
    object ReadScreen : SystemAction()
    data class YouTubeSearch(val query: String) : SystemAction()
    object OpenYouTube : SystemAction()
    data class OpenApp(val appName: String) : SystemAction()
    data class WebSearch(val query: String) : SystemAction()
}

data class SystemActionResult(
    val executed: Boolean,
    val spokenConfirmation: String,
    val screenContentSummary: String? = null
)

object SystemActionExecutor {

    /**
     * Determines if the user's prompt or Gemini's response contains a device action / hands command.
     * Supports both natural multilingual prompts (English, Hindi, Hinglish) and [ACTION: ...] tags.
     */
    fun parseSystemAction(prompt: String): SystemAction? {
        val lower = prompt.lowercase().trim()

        // 1. Check for explicit [ACTION: ...] tags from Gemini reasoning
        val ytTagMatch = Regex("\\[action:\\s*youtube_search:\\s*([^\\]]+)\\]", RegexOption.IGNORE_CASE).find(prompt)
        if (ytTagMatch != null) {
            val q = ytTagMatch.groupValues[1].trim()
            if (q.isNotBlank()) return SystemAction.YouTubeSearch(q)
        }

        val openAppTagMatch = Regex("\\[action:\\s*open_app:\\s*([^\\]]+)\\]", RegexOption.IGNORE_CASE).find(prompt)
        if (openAppTagMatch != null) {
            val app = openAppTagMatch.groupValues[1].trim()
            if (app.isNotBlank()) {
                if (app.contains("youtube", ignoreCase = true)) return SystemAction.OpenYouTube
                return SystemAction.OpenApp(app)
            }
        }

        val webTagMatch = Regex("\\[action:\\s*web_search:\\s*([^\\]]+)\\]", RegexOption.IGNORE_CASE).find(prompt)
        if (webTagMatch != null) {
            val q = webTagMatch.groupValues[1].trim()
            if (q.isNotBlank()) return SystemAction.WebSearch(q)
        }

        val tapTagMatch = Regex("\\[action:\\s*tap:\\s*([^\\]]+)\\]", RegexOption.IGNORE_CASE).find(prompt)
        if (tapTagMatch != null) {
            val target = tapTagMatch.groupValues[1].trim()
            if (target.isNotBlank()) return SystemAction.ClickTarget(target)
        }

        when {
            lower.contains("[action: open_youtube]") -> return SystemAction.OpenYouTube
            lower.contains("[action: long_press_and_scroll_up]") -> return SystemAction.LongPressAndScrollUp
            lower.contains("[action: long_press]") -> return SystemAction.LongPress
            lower.contains("[action: scroll_down]") -> return SystemAction.ScrollDown
            lower.contains("[action: scroll_up]") -> return SystemAction.ScrollUp
            lower.contains("[action: double_tap]") -> return SystemAction.DoubleTap
            lower.contains("[action: home]") -> return SystemAction.Home
            lower.contains("[action: back]") -> return SystemAction.Back
            lower.contains("[action: recents]") -> return SystemAction.Recents
            lower.contains("[action: notifications]") -> return SystemAction.Notifications
            lower.contains("[action: quick_settings]") -> return SystemAction.QuickSettings
            lower.contains("[action: screenshot]") -> return SystemAction.Screenshot
            lower.contains("[action: lock_screen]") -> return SystemAction.LockScreen
            lower.contains("[action: screen_share]") -> return SystemAction.ReadScreen
        }

        // 2. Comprehensive natural language understanding (English, Hindi, Hinglish)

        // YouTube Search: Comprehensive patterns for English, Hindi, Hinglish
        val ytSearchPatterns = listOf(
            // "open youtube and search for X" / "go to youtube and search X" / "launch youtube and search X"
            Regex("(?:please\\s+)?(?:open|launch|go\\s+to)?\\s*youtube\\s+(?:and\\s+)?(?:search|find|look\\s+for|play)\\s+(?:for\\s+)?(.+)", RegexOption.IGNORE_CASE),
            // "search youtube for X" / "search on youtube for X" / "search in youtube for X"
            Regex("(?:please\\s+)?search\\s+(?:on\\s+|in\\s+)?youtube\\s+(?:for\\s+)?(.+)", RegexOption.IGNORE_CASE),
            // "search for X on youtube" / "search X on youtube"
            Regex("(?:please\\s+)?search\\s+(?:for\\s+)?(.+?)\\s+(?:on|in|using)\\s+youtube", RegexOption.IGNORE_CASE),
            // "find X on youtube" / "look for X on youtube"
            Regex("(?:please\\s+)?(?:find|look\\s+for)\\s+(.+?)\\s+(?:on|in)\\s+youtube", RegexOption.IGNORE_CASE),
            // "play X on youtube"
            Regex("(?:please\\s+)?play\\s+(.+?)\\s+(?:on|in)\\s+youtube", RegexOption.IGNORE_CASE),
            // Hindi/Hinglish compound: "youtube kholo aur X search karo/chalao"
            Regex("youtube\\s+(?:kholo|open\\s+karo|open)\\s+aur\\s+(.+?)\\s+(?:search\\s+karo|chalao|play\\s+karo|lagao|dikhao)", RegexOption.IGNORE_CASE),
            Regex("youtube\\s+(?:kholo|open\\s+karo|open)\\s+aur\\s+(?:search\\s+karo|chalao)\\s+(.+)", RegexOption.IGNORE_CASE),
            // Hindi/Hinglish: "youtube par / pe / mein X search karo / chalao / khojo / dekho / lagao"
            Regex("youtube\\s+(?:par|pe|mein|me)\\s+(.+?)\\s+(?:search\\s+karo|chalao|lagao|dikhao|play\\s+karo|dekho|khojo|dhundo)", RegexOption.IGNORE_CASE),
            Regex("youtube\\s+(?:par|pe|mein|me)\\s+(?:search\\s+karo|chalao|khojo)\\s+(.+)", RegexOption.IGNORE_CASE),
            // "youtube search for X" / "youtube search X"
            Regex("youtube\\s+search\\s+(?:for\\s+)?(.+)", RegexOption.IGNORE_CASE)
        )
        for (pattern in ytSearchPatterns) {
            val match = pattern.find(lower)
            if (match != null) {
                val query = match.groupValues[1].trim().trim('"', '\'', '.', ',', '!', '?')
                if (query.isNotBlank() && query != "something" && query != "anything") {
                    return SystemAction.YouTubeSearch(query)
                } else if (query == "something" || query == "anything") {
                    return SystemAction.YouTubeSearch("trending")
                }
            }
        }

        // Just Open YouTube: "open youtube", "youtube kholo", "launch youtube"
        if (lower == "open youtube" || lower == "youtube kholo" || lower == "launch youtube" ||
            lower == "youtube" || lower.startsWith("open youtube ") || lower.contains("youtube open karo") ||
            lower.contains("kholo youtube") || (lower.contains("youtube") && lower.contains("open"))
        ) {
            return SystemAction.OpenYouTube
        }

        // Common Apps: Open Camera, Open Settings, Open Chrome, Open Gallery, Open Calculator
        val openAppPatterns = listOf(
            Regex("(?:please\\s+)?open\\s+([a-z0-9\\s]+)", RegexOption.IGNORE_CASE),
            Regex("([a-z0-9\\s]+)\\s+(?:kholo|open\\s+karo)", RegexOption.IGNORE_CASE)
        )
        for (pattern in openAppPatterns) {
            val match = pattern.find(lower)
            if (match != null) {
                val app = match.groupValues[1].trim()
                if (app in listOf("settings", "camera", "chrome", "gallery", "calculator", "maps", "clock", "contacts", "files")) {
                    return SystemAction.OpenApp(app)
                }
            }
        }

        // Compound gesture: Long press + scroll up / drag up (specifically requested)
        if ((lower.contains("long press") || lower.contains("daba ke") || lower.contains("hold")) &&
            (lower.contains("scroll up") || lower.contains("drag up") || lower.contains("upar") || lower.contains("plus scroll"))
        ) {
            return SystemAction.LongPressAndScrollUp
        }

        // Double Tap
        if (lower.contains("double tap") || lower.contains("do baar tap") || lower.contains("double click")) {
            return SystemAction.DoubleTap
        }

        // Long press alone
        if (lower.contains("long press") || lower.contains("daba ke rakho") || lower.contains("hold touch") || lower.contains("press and hold")) {
            return SystemAction.LongPress
        }

        // Scroll down
        if (lower.contains("scroll down") || lower.contains("niche scroll") || lower.contains("swipe down") ||
            lower.contains("scroll niche") || (lower.contains("scroll") && lower.contains("niche")) ||
            lower.contains("thoda niche jao") || lower == "scroll" || (lower.startsWith("scroll ") && !lower.contains("up"))
        ) {
            return SystemAction.ScrollDown
        }

        // Scroll up
        if (lower.contains("scroll up") || lower.contains("upar scroll") || lower.contains("swipe up") ||
            lower.contains("scroll upar") || (lower.contains("scroll") && lower.contains("upar")) ||
            lower.contains("thoda upar jao")
        ) {
            return SystemAction.ScrollUp
        }

        // Screenshot
        if (lower.contains("screenshot") || lower.contains("screen capture") || lower.contains("screen shot")) {
            return SystemAction.Screenshot
        }

        // Quick Settings
        if (lower.contains("quick settings") || lower.contains("quick setting")) {
            return SystemAction.QuickSettings
        }

        // Lock screen
        if (lower.contains("lock screen") || lower.contains("phone lock karo") || lower.contains("screen lock")) {
            return SystemAction.LockScreen
        }

        // Home
        if (lower.contains("go home") || lower == "home" || lower.contains("home screen") || lower.contains("ghar jao")) {
            return SystemAction.Home
        }

        // Back
        if (lower.contains("go back") || lower == "back" || lower.contains("piche jao") || lower.contains("wapas jao")) {
            return SystemAction.Back
        }

        // Recents / Multitasking
        if (lower.contains("recent apps") || lower.contains("recents") || lower.contains("open recents") || lower.contains("recent app")) {
            return SystemAction.Recents
        }

        // Notifications
        if (lower.contains("notification") || lower.contains("pull down notification") || lower.contains("notifications dikhao")) {
            return SystemAction.Notifications
        }

        // Screen sharing / reading: "Screen share", "Share screen", "Screen scan", "Screen par kya hai"
        if (lower.contains("screen share") || lower.contains("share screen") || lower.contains("read screen") ||
            lower.contains("what is on screen") || lower.contains("screen par kya hai") || lower.contains("screen scan") ||
            lower.contains("screen dikhao") || lower == "screen"
        ) {
            return SystemAction.ReadScreen
        }

        // Click / Tap on specific target
        if (lower.startsWith("tap on ") || lower.startsWith("click on ") || lower.startsWith("touch on ") ||
            lower.startsWith("tap ") || lower.startsWith("click ") || lower.startsWith("touch ")
        ) {
            val target = lower.removePrefix("tap on ")
                .removePrefix("click on ")
                .removePrefix("touch on ")
                .removePrefix("tap ")
                .removePrefix("click ")
                .removePrefix("touch ")
                .trim()
            if (target.isNotBlank() && !target.contains("screen")) return SystemAction.ClickTarget(target)
        }

        // Hindi / Hinglish target: "[target] par tap karo"
        if (lower.contains("par tap karo") || lower.contains("par click karo") || lower.contains("ko touch karo")) {
            val target = lower.substringBefore("par tap")
                .substringBefore("par click")
                .substringBefore("ko touch")
                .trim()
            if (target.isNotBlank()) return SystemAction.ClickTarget(target)
        }

        return null
    }

    /**
     * Executes the system action via Android Intents or KavyaAccessibilityService.
     */
    fun execute(action: SystemAction, language: KavyaLanguage, context: Context): SystemActionResult {
        // Direct Intent Actions (YouTube, App Launches) work even if Accessibility Service is not yet turned on!
        when (action) {
            is SystemAction.YouTubeSearch -> {
                val query = action.query.trim().trim('"', '\'')
                val encoded = URLEncoder.encode(query, "UTF-8")
                val webUri = Uri.parse("https://www.youtube.com/results?search_query=$encoded")

                // Primary: Direct YouTube app deep link (fastest & most reliable on Android)
                val ytPackageIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    setPackage("com.google.android.youtube")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                // Fallback 1: YouTube Search action
                val searchIntent = Intent(Intent.ACTION_SEARCH).apply {
                    setPackage("com.google.android.youtube")
                    putExtra("query", query)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                // Fallback 2: Universal Web View (opens in YouTube or browser)
                val genericIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

                val executed = try {
                    context.startActivity(ytPackageIntent)
                    true
                } catch (e1: Exception) {
                    try {
                        context.startActivity(searchIntent)
                        true
                    } catch (e2: Exception) {
                        try {
                            context.startActivity(genericIntent)
                            true
                        } catch (e3: Exception) {
                            false
                        }
                    }
                }

                val spoken = when (language) {
                    KavyaLanguage.HINDI -> "यूट्यूब खोलकर '${query}' सर्च कर दिया है! 🎬🌸"
                    KavyaLanguage.HINGLISH -> "Haanji! YouTube open karke '${query}' search kar diya hai~ Dekhiye! ✨"
                    else -> "Opening YouTube and searching for '${query}' right now! 🎬✨"
                }
                return SystemActionResult(executed = executed, spokenConfirmation = spoken)
            }

            is SystemAction.OpenYouTube -> {
                val launchIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                val intent = launchIntent ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                val executed = try {
                    context.startActivity(intent)
                    true
                } catch (e: Exception) {
                    false
                }

                val spoken = when (language) {
                    KavyaLanguage.HINDI -> "यूट्यूब खोल दिया है! 🌸"
                    KavyaLanguage.HINGLISH -> "YouTube open kar diya hai! ✨"
                    else -> "Opening YouTube for you now! 🎬"
                }
                return SystemActionResult(executed = executed, spokenConfirmation = spoken)
            }

            is SystemAction.OpenApp -> {
                val app = action.appName.lowercase().trim()
                val targetIntent = when (app) {
                    "settings" -> Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    "camera" -> Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    "chrome" -> {
                        context.packageManager.getLaunchIntentForPackage("com.android.chrome")
                            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    }
                    else -> {
                        // Look for matching installed package
                        val pm = context.packageManager
                        val pkg = pm.getInstalledApplications(0).firstOrNull {
                            it.packageName.contains(app, ignoreCase = true)
                        }?.packageName
                        if (pkg != null) pm.getLaunchIntentForPackage(pkg)?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                        else null
                    }
                }

                val executed = if (targetIntent != null) {
                    try {
                        context.startActivity(targetIntent)
                        true
                    } catch (e: Exception) {
                        false
                    }
                } else false

                val spoken = if (executed) {
                    when (language) {
                        KavyaLanguage.HINDI -> "${action.appName} खोल दिया है! 🌸"
                        KavyaLanguage.HINGLISH -> "${action.appName} open kar diya hai! ✨"
                        else -> "Opening ${action.appName} for you! ✨"
                    }
                } else {
                    when (language) {
                        KavyaLanguage.HINDI -> "मुझे ${action.appName} नहीं मिला। 🌸"
                        KavyaLanguage.HINGLISH -> "${action.appName} app nahi mila. 🌸"
                        else -> "I couldn't find ${action.appName} on this device."
                    }
                }
                return SystemActionResult(executed = executed, spokenConfirmation = spoken)
            }

            is SystemAction.WebSearch -> {
                val query = action.query.trim()
                val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra(SearchManager.QUERY, query)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val executed = try {
                    context.startActivity(intent)
                    true
                } catch (e: Exception) {
                    try {
                        context.startActivity(fallbackIntent)
                        true
                    } catch (e2: Exception) {
                        false
                    }
                }
                val spoken = when (language) {
                    KavyaLanguage.HINDI -> "गूगल पर '${query}' सर्च कर रही हूँ! 🌸"
                    KavyaLanguage.HINGLISH -> "Google par '${query}' search kar diya hai~ 🔍"
                    else -> "Searching the web for '${query}' right now! 🔍"
                }
                return SystemActionResult(executed = executed, spokenConfirmation = spoken)
            }

            else -> {
                // Accessibility Service Gestures
            }
        }

        // Accessibility Service Check
        val service = KavyaAccessibilityService.instance
        if (service == null) {
            val guidance = when (language) {
                KavyaLanguage.HINDI -> "मुझे सिस्टम और टच कंट्रोल करने के लिए 'Kavya Hands' एक्सेसिबिलिटी परमिशन की ज़रूरत है! सेटिंग्स में 'Kavya AI' चालू करें। 🌸"
                KavyaLanguage.HINGLISH -> "Kavya Hands abhi off hai! Realistic touches ke liye settings mein jakar 'Kavya AI' accessibility allow karein~ ✨"
                else -> "Kavya Hands is not enabled yet! Please turn on the Kavya Accessibility Service in Android Settings to grant gesture control~ ✨"
            }
            return SystemActionResult(executed = false, spokenConfirmation = guidance)
        }

        return when (action) {
            is SystemAction.ScrollDown -> {
                service.performScrollDown()
                val spoken = when (language) {
                    KavyaLanguage.HINDI -> "ज़रूर! मैं नीचे स्क्रॉल कर रही हूँ~ 🌸"
                    KavyaLanguage.HINGLISH -> "Haanji! Niche scroll kar diya~ Dekhiye! ✨"
                    else -> "Hai hai! Scrolling down for you smoothly~ 📜"
                }
                SystemActionResult(executed = true, spokenConfirmation = spoken)
            }
            is SystemAction.ScrollUp -> {
                service.performScrollUp()
                val spoken = when (language) {
                    KavyaLanguage.HINDI -> "ऊपर स्क्रॉल कर दिया है! 🌸"
                    KavyaLanguage.HINGLISH -> "Upar scroll kar diya hai! ✨"
                    else -> "Scrolling up for you right now~ 📜"
                }
                SystemActionResult(executed = true, spokenConfirmation = spoken)
            }
            is SystemAction.LongPress -> {
                val metrics = service.resources.displayMetrics
                service.performLongPress(metrics.widthPixels / 2f, metrics.heightPixels / 2f)
                val spoken = when (language) {
                    KavyaLanguage.HINDI -> "लॉन्ग प्रेस कर दिया है! 🌸"
                    KavyaLanguage.HINGLISH -> "Screen par realistic long press kar diya hai! 👆"
                    else -> "Executing a realistic long press gesture right now~ 👆"
                }
                SystemActionResult(executed = true, spokenConfirmation = spoken)
            }
            is SystemAction.LongPressAndScrollUp -> {
                service.performLongPressAndScrollUp()
                val spoken = when (language) {
                    KavyaLanguage.HINDI -> "लॉन्ग प्रेस करके ऊपर ड्रैग कर दिया है! 🌸"
                    KavyaLanguage.HINGLISH -> "Long press karke smoothly upar scroll/drag kar diya~ ✨"
                    else -> "Holding down and dragging smoothly upward for you! 👆"
                }
                SystemActionResult(executed = true, spokenConfirmation = spoken)
            }
            is SystemAction.ClickTarget -> {
                val success = service.clickTargetPrecisely(action.targetName)
                val spoken = if (success) {
                    when (language) {
                        KavyaLanguage.HINDI -> "'${action.targetName}' पर सटीकता से टच कर दिया है! 🌸"
                        KavyaLanguage.HINGLISH -> "Found '${action.targetName}'! Bilkul exact jagah par touch kar diya~ ✨"
                        else -> "Found '${action.targetName}' and tapped it precisely without touching anywhere else! ✨"
                    }
                } else {
                    when (language) {
                        KavyaLanguage.HINDI -> "मुझे स्क्रीन पर '${action.targetName}' नहीं मिला। 🌸"
                        KavyaLanguage.HINGLISH -> "Screen par '${action.targetName}' nahi dikh raha hai. 🌸"
                        else -> "I couldn't safely find '${action.targetName}' on the active screen to avoid touching wrong areas."
                    }
                }
                SystemActionResult(executed = success, spokenConfirmation = spoken)
            }
            is SystemAction.Home -> {
                service.triggerGlobalHome()
                val spoken = when (language) {
                    KavyaLanguage.HINDI -> "होम स्क्रीन पर जा रही हूँ~ 🏠"
                    KavyaLanguage.HINGLISH -> "Chaliye home screen par chalte hain! 🏠"
                    else -> "Taking you to the Home screen now~ 🏠"
                }
                SystemActionResult(executed = true, spokenConfirmation = spoken)
            }
            is SystemAction.Back -> {
                service.triggerGlobalBack()
                val spoken = when (language) {
                    KavyaLanguage.HINDI -> "पीछे जा रही हूँ~ ⬅️"
                    KavyaLanguage.HINGLISH -> "Back ja rahi hoon~ ⬅️"
                    else -> "Navigating back for you~ ⬅️"
                }
                SystemActionResult(executed = true, spokenConfirmation = spoken)
            }
            is SystemAction.Recents -> {
                service.triggerGlobalRecents()
                val spoken = when (language) {
                    KavyaLanguage.HINDI -> "हालिया ऐप्स खोल रही हूँ~ 📱"
                    KavyaLanguage.HINGLISH -> "Recent apps open kar diye hain~ 📱"
                    else -> "Opening recent apps for you~ 📱"
                }
                SystemActionResult(executed = true, spokenConfirmation = spoken)
            }
            is SystemAction.Notifications -> {
                service.triggerGlobalNotifications()
                val spoken = when (language) {
                    KavyaLanguage.HINDI -> "सूचनाएं खोल रही हूँ~ 🔔"
                    KavyaLanguage.HINGLISH -> "Notification panel pull kar diya hai~ 🔔"
                    else -> "Opening notification shade for you~ 🔔"
                }
                SystemActionResult(executed = true, spokenConfirmation = spoken)
            }
            is SystemAction.DoubleTap -> {
                val metrics = service.resources.displayMetrics
                service.performRealisticDoubleTap(metrics.widthPixels / 2f, metrics.heightPixels / 2f)
                val spoken = when (language) {
                    KavyaLanguage.HINDI -> "डबल टैप कर दिया है! ✨"
                    KavyaLanguage.HINGLISH -> "Screen par realistic double-tap kar diya hai~ ✨"
                    else -> "Executed a realistic double-tap gesture for you! ✨"
                }
                SystemActionResult(executed = true, spokenConfirmation = spoken)
            }
            is SystemAction.QuickSettings -> {
                val success = service.triggerGlobalQuickSettings()
                val spoken = when (language) {
                    KavyaLanguage.HINDI -> "त्वरित सेटिंग्स खोल रही हूँ~ ⚙️"
                    KavyaLanguage.HINGLISH -> "Quick Settings open kar diye hain~ ⚙️"
                    else -> "Opening Quick Settings panel for you~ ⚙️"
                }
                SystemActionResult(executed = success, spokenConfirmation = spoken)
            }
            is SystemAction.Screenshot -> {
                val success = service.triggerGlobalScreenshot()
                val spoken = when (language) {
                    KavyaLanguage.HINDI -> "स्क्रीनशॉट ले लिया है! 📸"
                    KavyaLanguage.HINGLISH -> "Screenshot capture kar liya hai! 📸"
                    else -> "Captured a screenshot of your screen! 📸"
                }
                SystemActionResult(executed = success, spokenConfirmation = spoken)
            }
            is SystemAction.LockScreen -> {
                val success = service.triggerGlobalLockScreen()
                val spoken = when (language) {
                    KavyaLanguage.HINDI -> "स्क्रीन लॉक कर दी है! 🔒"
                    KavyaLanguage.HINGLISH -> "Screen lock kar di hai~ 🔒"
                    else -> "Screen locked safely for you~ 🔒"
                }
                SystemActionResult(executed = success, spokenConfirmation = spoken)
            }
            is SystemAction.ReadScreen -> {
                val screenSummary = service.readScreenHierarchy()
                val spoken = when (language) {
                    KavyaLanguage.HINDI -> "मैंने आपकी स्क्रीन स्कैन कर ली है! यहाँ सक्रिय चीजें हैं। 🔍"
                    KavyaLanguage.HINGLISH -> "Screen scan kar li hai! Main dekh sakti hoon kya chal raha hai~ 🔍"
                    else -> "Screen shared and scanned! I can see all active elements on your screen right now~ 🔍"
                }
                SystemActionResult(executed = true, spokenConfirmation = spoken, screenContentSummary = screenSummary)
            }
            else -> {
                SystemActionResult(executed = false, spokenConfirmation = "Action not supported.")
            }
        }
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
