package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class KavyaAccessibilityService : AccessibilityService() {

    private val tag = "KavyaHandsService"
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private var _instance: KavyaAccessibilityService? = null
        val instance: KavyaAccessibilityService? get() = _instance

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val _lastActionStatus = MutableStateFlow("Harness Standby")
        val lastActionStatus: StateFlow<String> = _lastActionStatus.asStateFlow()

        private val _lastTappedCoordinates = MutableStateFlow<Pair<Float, Float>?>(null)
        val lastTappedCoordinates: StateFlow<Pair<Float, Float>?> = _lastTappedCoordinates.asStateFlow()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _instance = this
        _isServiceActive.value = true
        _lastActionStatus.value = "Kavya Hands Connected & Ready ✨"
        Log.i(tag, "Kavya Hands Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Passive listener for system events
    }

    override fun onInterrupt() {
        _lastActionStatus.value = "Harness Interrupted"
    }

    override fun onDestroy() {
        super.onDestroy()
        if (_instance == this) {
            _instance = null
            _isServiceActive.value = false
        }
    }

    private fun getScreenMetrics(): DisplayMetrics {
        return resources.displayMetrics
    }

    /**
     * Performs a realistic, human-like touch at exact screen coordinates.
     * Uses a micro-path (1px) with 60ms duration so Android accessibility gesture engine
     * correctly interprets it as a valid tap stroke and generates ACTION_DOWN + ACTION_UP.
     */
    fun performRealisticTouch(x: Float, y: Float, onComplete: (Boolean) -> Unit = {}) {
        val metrics = getScreenMetrics()
        val safeX = x.coerceIn(10f, (metrics.widthPixels - 10).toFloat())
        val safeY = y.coerceIn(10f, (metrics.heightPixels - 10).toFloat())

        val path = Path().apply {
            moveTo(safeX, safeY)
            lineTo(safeX, safeY + 1f)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 60)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        _lastTappedCoordinates.value = Pair(safeX, safeY)
        _lastActionStatus.value = "Tapping at (${safeX.toInt()}, ${safeY.toInt()})"
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                _lastActionStatus.value = "Touch completed ✨"
                onComplete(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                _lastActionStatus.value = "Touch cancelled"
                onComplete(false)
            }
        }, null)
    }

    /**
     * Performs a realistic double tap gesture at exact coordinates using chained stroke.
     */
    fun performRealisticDoubleTap(x: Float, y: Float, onComplete: (Boolean) -> Unit = {}) {
        val metrics = getScreenMetrics()
        val safeX = x.coerceIn(10f, (metrics.widthPixels - 10).toFloat())
        val safeY = y.coerceIn(10f, (metrics.heightPixels - 10).toFloat())

        val path1 = Path().apply {
            moveTo(safeX, safeY)
            lineTo(safeX, safeY + 1f)
        }
        val path2 = Path().apply {
            moveTo(safeX, safeY)
            lineTo(safeX, safeY + 1f)
        }
        val stroke1 = GestureDescription.StrokeDescription(path1, 0, 50, true)
        val stroke2 = stroke1.continueStroke(path2, 120, 50, false)
        val gesture = GestureDescription.Builder().addStroke(stroke1).addStroke(stroke2).build()

        _lastTappedCoordinates.value = Pair(safeX, safeY)
        _lastActionStatus.value = "Double tapping at (${safeX.toInt()}, ${safeY.toInt()})"
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                _lastActionStatus.value = "Double tap completed ✨"
                onComplete(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                _lastActionStatus.value = "Double tap cancelled"
                onComplete(false)
            }
        }, null)
    }

    /**
     * Performs a realistic long press at exact coordinates (750ms hold).
     */
    fun performLongPress(x: Float, y: Float, onComplete: (Boolean) -> Unit = {}) {
        val metrics = getScreenMetrics()
        val safeX = x.coerceIn(10f, (metrics.widthPixels - 10).toFloat())
        val safeY = y.coerceIn(10f, (metrics.heightPixels - 10).toFloat())

        val path = Path().apply {
            moveTo(safeX, safeY)
            lineTo(safeX, safeY + 1f)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 750)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        _lastTappedCoordinates.value = Pair(safeX, safeY)
        _lastActionStatus.value = "Long pressing at (${safeX.toInt()}, ${safeY.toInt()})"
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                _lastActionStatus.value = "Long press completed ✨"
                onComplete(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                _lastActionStatus.value = "Long press cancelled"
                onComplete(false)
            }
        }, null)
    }

    /**
     * Performs a smooth, realistic scroll down gesture.
     */
    fun performScrollDown(onComplete: (Boolean) -> Unit = {}) {
        val metrics = getScreenMetrics()
        val centerX = metrics.widthPixels / 2f
        val startY = metrics.heightPixels * 0.72f
        val endY = metrics.heightPixels * 0.26f

        val path = Path().apply {
            moveTo(centerX, startY)
            lineTo(centerX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 320)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        _lastActionStatus.value = "Scrolling down 📜"
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                _lastActionStatus.value = "Scroll down completed ✨"
                onComplete(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                onComplete(false)
            }
        }, null)
    }

    /**
     * Performs a smooth, realistic scroll up gesture.
     */
    fun performScrollUp(onComplete: (Boolean) -> Unit = {}) {
        val metrics = getScreenMetrics()
        val centerX = metrics.widthPixels / 2f
        val startY = metrics.heightPixels * 0.28f
        val endY = metrics.heightPixels * 0.74f

        val path = Path().apply {
            moveTo(centerX, startY)
            lineTo(centerX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 320)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        _lastActionStatus.value = "Scrolling up 📜"
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                _lastActionStatus.value = "Scroll up completed ✨"
                onComplete(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                onComplete(false)
            }
        }, null)
    }

    /**
     * Performs a compound "long press plus scroll up" (drag & scroll) gesture.
     * Holds for 650ms to activate lift/drag mode, then drags upward smoothly over 500ms.
     */
    fun performLongPressAndScrollUp(
        startX: Float = getScreenMetrics().widthPixels / 2f,
        startY: Float = getScreenMetrics().heightPixels * 0.68f,
        distance: Float = getScreenMetrics().heightPixels * 0.40f,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(startX, startY - distance)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 1150)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        _lastTappedCoordinates.value = Pair(startX, startY)
        _lastActionStatus.value = "Long-press & dragging up 👆"
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                _lastActionStatus.value = "Long-press drag completed ✨"
                onComplete(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                onComplete(false)
            }
        }, null)
    }

    /**
     * Precise targeting: Finds an element by text or content description and touches ONLY it.
     * Uses both direct AccessibilityNode click action AND coordinate touch injection.
     * STRICT Safety Guard: If no valid matching target is found, it REFUSES to touch
     * anywhere else to prevent accidental touches on unintended controls.
     */
    fun clickTargetPrecisely(targetQuery: String, onComplete: (Boolean) -> Unit = {}): Boolean {
        val root = rootInActiveWindow
        if (root == null) {
            _lastActionStatus.value = "Active screen window not accessible"
            onComplete(false)
            return false
        }
        val matchedNodes = mutableListOf<AccessibilityNodeInfo>()

        fun searchRecursive(node: AccessibilityNodeInfo?) {
            if (node == null) return
            val text = node.text?.toString() ?: ""
            val desc = node.contentDescription?.toString() ?: ""
            val viewId = node.viewIdResourceName ?: ""
            if (text.contains(targetQuery, ignoreCase = true) ||
                desc.contains(targetQuery, ignoreCase = true) ||
                viewId.contains(targetQuery, ignoreCase = true)
            ) {
                matchedNodes.add(node)
            }
            for (i in 0 until node.childCount) {
                searchRecursive(node.getChild(i))
            }
        }

        searchRecursive(root)

        // Find best match: prefer clickable node or first matched
        val target = matchedNodes.firstOrNull { it.isClickable } ?: matchedNodes.firstOrNull()
        if (target != null) {
            // Strategy 1: Direct Accessibility Click action on target or its clickable parent
            var directClicked = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (!directClicked) {
                var parent = target.parent
                while (parent != null && !directClicked) {
                    directClicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    parent = parent.parent
                }
            }

            // Strategy 2: Gesture touch on calculated node center
            val rect = Rect()
            target.getBoundsInScreen(rect)
            val metrics = getScreenMetrics()
            if (rect.width() > 0 && rect.height() > 0 &&
                rect.centerX() in 0..metrics.widthPixels &&
                rect.centerY() in 0..metrics.heightPixels
            ) {
                val cx = rect.exactCenterX()
                val cy = rect.exactCenterY()
                _lastActionStatus.value = "Found '$targetQuery' at (${cx.toInt()}, ${cy.toInt()})! Touching..."
                performRealisticTouch(cx, cy, onComplete)
                return true
            }

            if (directClicked) {
                _lastActionStatus.value = "Target '$targetQuery' clicked directly ✨"
                onComplete(true)
                return true
            }
        }

        // Safety Guard: Refuse to touch randomly if target not safely verified!
        _lastActionStatus.value = "Safety Guard: '$targetQuery' was not found on active screen. Touch cancelled."
        onComplete(false)
        return false
    }

    /**
     * Screen Sharing / Screen Reader Vision Harness:
     * Extracts all on-screen content and interactive elements so Gemini understands current screen.
     */
    fun readScreenHierarchy(): String {
        val root = rootInActiveWindow ?: return "Screen is currently unavailable or blank."
        val builder = StringBuilder("Active Screen Context:\n")
        val pkg = root.packageName?.toString() ?: "System / Launcher"
        builder.append("Foreground App: $pkg\n")
        builder.append("Visible On-Screen Elements:\n")

        var count = 0
        fun extractNodes(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || depth > 8 || count > 45) return
            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            val className = node.className?.toString()?.substringAfterLast('.') ?: ""

            if (!text.isNullOrBlank() || !desc.isNullOrBlank() || node.isClickable) {
                val rect = Rect()
                node.getBoundsInScreen(rect)
                val label = text ?: desc ?: "Interactive Control"
                val clickableTag = if (node.isClickable) "[Button]" else "[Text]"
                builder.append("- $clickableTag $className: \"$label\" (${rect.left},${rect.top})\n")
                count++
            }

            for (i in 0 until node.childCount) {
                extractNodes(node.getChild(i), depth + 1)
            }
        }

        extractNodes(root, 0)
        return builder.toString()
    }

    fun triggerGlobalHome(): Boolean {
        _lastActionStatus.value = "Navigating Home 🏠"
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun triggerGlobalBack(): Boolean {
        _lastActionStatus.value = "Navigating Back ⬅️"
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun triggerGlobalRecents(): Boolean {
        _lastActionStatus.value = "Opening Recent Apps 📱"
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    fun triggerGlobalNotifications(): Boolean {
        _lastActionStatus.value = "Opening Notifications 🔔"
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    fun triggerGlobalQuickSettings(): Boolean {
        _lastActionStatus.value = "Opening Quick Settings ⚙️"
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    fun triggerGlobalScreenshot(): Boolean {
        _lastActionStatus.value = "Capturing Screenshot 📸"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
            false
        }
    }

    fun triggerGlobalLockScreen(): Boolean {
        _lastActionStatus.value = "Locking Screen 🔒"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            false
        }
    }
}
