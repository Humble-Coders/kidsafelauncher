package com.humblecoders.kidsafelauncher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.util.Log
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

class AppBlockAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val recentlyCheckedPackages = mutableSetOf<String>()
    
    // Track apps that are in grace period - maps package name to when grace period started
    private val appsInGracePeriod = mutableMapOf<String, Long>()
    
    // Grace period duration
    private val gracePeriodMs = 2000L // 2 seconds for overlays/app locks to appear
    
    // Additional check delay after first check (safety net)
    private val secondCheckDelayMs = 500L // Check again 500ms later as backup
    
    // Periodic check interval - to catch apps opened from recents/notifications
    private val periodicCheckIntervalMs = 1000L // Check every 1 second for better detection
    
    // Track last checked foreground app to avoid redundant checks
    private var lastCheckedForegroundPackage: String? = null
    
    // Periodic check runnable
    private var isPeriodicCheckRunning = false
    private val periodicCheckRunnable = object : Runnable {
        override fun run() {
            if (getKidMode(this@AppBlockAccessibilityService)) {
                checkForegroundAppPeriodically()
                handler.postDelayed(this, periodicCheckIntervalMs)
            } else {
                isPeriodicCheckRunning = false
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Only act if kid mode is active
        if (!getKidMode(this)) {
            return
        }
        
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString()
                Log.d("AccessibilityService", "Window state changed: $packageName")
                
                if (packageName != null && packageName != this.packageName) {
                    handlePackageWindowChange(packageName)
                }
            }
            
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Also check on content changes (for apps restored from recents)
                val packageName = event.packageName?.toString()
                if (packageName != null && packageName != this.packageName) {
                    Log.d("AccessibilityService", "Window content changed: $packageName")
                    // Quick check without grace period for content changes
                    checkAppImmediately(packageName)
                }
            }
            
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                // When notifications change, also check foreground app
                // (apps might be launched from notification)
                handler.postDelayed({
                    val currentForeground = getCurrentForegroundPackageFromUsageStats()
                    if (currentForeground != null && currentForeground != this.packageName) {
                        if (!isPackageAllowed(currentForeground) && !appsInGracePeriod.containsKey(currentForeground)) {
                            Log.d("AccessibilityService", "Notification state changed, checking foreground app: $currentForeground")
                            checkAppImmediately(currentForeground)
                        }
                    }
                }, 300L) // Small delay to let app fully launch
            }
        }
    }
    
    /**
     * Check app immediately without grace period (for apps restored from recents)
     */
    private fun checkAppImmediately(packageName: String) {
        // Settings app - always block immediately
        if (isSettingsApp(packageName)) {
            Log.w("AccessibilityService", "Settings app detected - blocking immediately: $packageName")
            appsInGracePeriod.remove(packageName)
            returnToLauncher()
            return
        }
        
        if (isPackageAllowed(packageName)) {
            return
        }
        
        // If not in grace period, check immediately
        if (!appsInGracePeriod.containsKey(packageName)) {
            val currentForeground = getCurrentForegroundPackageFromUsageStats()
            if (currentForeground == packageName && !isPackageAllowed(packageName)) {
                Log.w("AccessibilityService", "Blocking app opened from recents/notifications: $packageName")
                returnToLauncher()
            }
        }
    }
    
    private fun handlePackageWindowChange(packageName: String) {
        // Ignore our own launcher
        if (packageName == this.packageName) {
            appsInGracePeriod.remove(packageName)
            return
        }
        
        // Ensure periodic checking is running
        startPeriodicCheck()
        
        // Check if package is allowed
        if (isPackageAllowed(packageName)) {
            Log.d("AccessibilityService", "Allowed app detected: $packageName")
            appsInGracePeriod.remove(packageName)
            return
        }
        
        // Settings app - block immediately without grace period
        if (isSettingsApp(packageName)) {
            Log.w("AccessibilityService", "Settings app detected - blocking immediately: $packageName")
            appsInGracePeriod.remove(packageName) // Remove from grace period if it was there
            returnToLauncher()
            // Also check again immediately as backup
            handler.postDelayed({
                val currentForeground = getCurrentForegroundPackageFromUsageStats()
                if (currentForeground == packageName || isSettingsApp(currentForeground ?: "")) {
                    Log.w("AccessibilityService", "Settings still open, blocking again")
                    returnToLauncher()
                }
            }, 100L)
            return
        }
        
        // Skip duplicate checks (rapid-fire events)
        if (recentlyCheckedPackages.contains(packageName)) {
            return
        }
        
        recentlyCheckedPackages.add(packageName)
        handler.postDelayed({
            recentlyCheckedPackages.remove(packageName)
        }, 300L)
        
        // If app is not in grace period yet, start grace period
        if (!appsInGracePeriod.containsKey(packageName)) {
            val now = System.currentTimeMillis()
            appsInGracePeriod[packageName] = now
            Log.d("AccessibilityService", "Starting grace period (${gracePeriodMs}ms) for: $packageName")
            
            // After grace period ends, check if app should be blocked
            handler.postDelayed({
                checkAndBlockAfterGracePeriod(packageName)
            }, gracePeriodMs)
        }
    }
    
    private fun isSettingsApp(packageName: String): Boolean {
        val settingsPackages = listOf(
            "com.android.settings",
            "com.samsung.android.settings",
            "com.miui.securitycenter",
            "com.oneplus.settings",
            "com.huawei.systemmanager",
            "com.coloros.settings",
            "com.oppo.settings",
            "com.vivo.settings",
            "com.realme.settings",
            "com.xiaomi.settings"
        )
        return settingsPackages.any { packageName.startsWith(it, ignoreCase = true) } ||
               packageName.contains("settings", ignoreCase = true)
    }
    
    /**
     * Checks once after grace period if the app should be blocked.
     * Does a second check as a safety net.
     */
    private fun checkAndBlockAfterGracePeriod(packageName: String) {
        // Remove from grace period tracking
        appsInGracePeriod.remove(packageName)
        
        // Check if it's still the foreground app
        val currentForegroundPackage = getCurrentForegroundPackageFromUsageStats()
        
        // If it's no longer foreground, don't block
        if (currentForegroundPackage != packageName) {
            Log.d("AccessibilityService", "Package $packageName no longer foreground (current: $currentForegroundPackage), skipping block")
            return
        }
        
        // Re-check if it's allowed (maybe it was whitelisted during grace period)
        if (isPackageAllowed(packageName)) {
            Log.d("AccessibilityService", "Package $packageName is now allowed, skipping block")
            return
        }
        
        // Still not allowed and still foreground - BLOCK IT
        Log.w("AccessibilityService", "BLOCKING app: $packageName (not whitelisted after grace period)")
        returnToLauncher()
        
        // Do a second check after a short delay as a safety net (in case the first block didn't work)
        handler.postDelayed({
            val stillForeground = getCurrentForegroundPackageFromUsageStats()
            if (stillForeground == packageName && !isPackageAllowed(packageName)) {
                Log.w("AccessibilityService", "App $packageName still open, blocking again (second check)")
                returnToLauncher()
            }
        }, secondCheckDelayMs)
    }
    
    /**
     * Gets the current foreground app using UsageStatsManager.
     */
    private fun getCurrentForegroundPackageFromUsageStats(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                val time = System.currentTimeMillis()
                val usageEvents = usageStatsManager?.queryEvents(time - 2000, time)
                val event = UsageEvents.Event()

                var lastApp: String? = null
                var lastEventTime = 0L

                while (usageEvents?.hasNextEvent() == true) {
                    usageEvents.getNextEvent(event)
                    if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                        if (event.timeStamp > lastEventTime) {
                            lastApp = event.packageName
                            lastEventTime = event.timeStamp
                        }
                    }
                }

                return lastApp
            } catch (e: Exception) {
                Log.e("AccessibilityService", "Error getting foreground app from UsageStats", e)
            }
        }
        return null
    }

    private fun isPackageAllowed(packageName: String): Boolean {
        // BLOCK Settings app in kid mode - no exceptions
        val settingsPackages = listOf(
            "com.android.settings",
            "com.samsung.android.settings",
            "com.miui.securitycenter",
            "com.oneplus.settings",
            "com.huawei.systemmanager",
            "com.coloros.settings",
            "com.oppo.settings",
            "com.vivo.settings",
            "com.realme.settings",
            "com.xiaomi.settings"
        )
        if (settingsPackages.any { packageName.startsWith(it, ignoreCase = true) } ||
            packageName.contains("settings", ignoreCase = true)) {
            return false
        }
        
        // Allow system UI and launcher
        if (packageName == "com.android.systemui" || 
            packageName.contains("launcher", ignoreCase = true) ||
            packageName == this.packageName) {
            return true
        }
        
        // Allow system packages (but Settings is already blocked above)
        if (packageName.startsWith("com.android.") || 
            packageName.startsWith("android.") ||
            packageName.startsWith("com.google.android.")) {
            return true
        }
        
        // Allow common app lock packages
        val commonAppLockPackages = listOf(
            "com.applock",
            "applock",
            "lock",
            "protector"
        )
        
        if (commonAppLockPackages.any { packageName.contains(it, ignoreCase = true) }) {
            return true
        }
        
        // Allow game engine overlays
        val gameEngineOverlays = listOf(
            "com.unity3d",
            "com.epicgames",
            "com.unrealengine",
            "unity",
            "unreal"
        )
        
        if (gameEngineOverlays.any { packageName.contains(it, ignoreCase = true) }) {
            return true
        }
        
        // Allow overlay packages
        if (packageName.contains("overlay", ignoreCase = true) ||
            packageName.contains("floating", ignoreCase = true) ||
            packageName.contains("popup", ignoreCase = true)) {
            return true
        }
        
        // Check whitelist
        val whitelist = getWhitelist(this)
        return whitelist.contains(packageName)
    }

    private fun returnToLauncher() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        
        // Also perform global action to go home (backup)
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    override fun onInterrupt() {
        Log.d("AccessibilityService", "Service interrupted")
    }

    /**
     * Periodic check to catch apps that were opened from recents/notifications
     * or apps that were already open when kid mode was enabled
     */
    private fun checkForegroundAppPeriodically() {
        val currentForeground = getCurrentForegroundPackageFromUsageStats()
        
        if (currentForeground == null || currentForeground == this.packageName) {
            // If launcher is in foreground, clear any tracking
            appsInGracePeriod.clear()
            lastCheckedForegroundPackage = currentForeground
            return
        }
        
        // Settings app - block immediately without any grace period
        if (isSettingsApp(currentForeground)) {
            Log.w("AccessibilityService", "Periodic check: Settings app detected - blocking immediately: $currentForeground")
            appsInGracePeriod.remove(currentForeground) // Remove from grace period
            returnToLauncher()
            return
        }
        
        // Skip if we just checked this package (unless grace period expired)
        val wasInGracePeriod = appsInGracePeriod.containsKey(currentForeground)
        if (currentForeground == lastCheckedForegroundPackage && !wasInGracePeriod) {
            return
        }
        
        lastCheckedForegroundPackage = currentForeground
        
        // If app is in grace period, wait for it
        if (wasInGracePeriod) {
            return
        }
        
        // Check if app is allowed
        if (isPackageAllowed(currentForeground)) {
            // Clear any tracking for allowed apps
            appsInGracePeriod.remove(currentForeground)
            return
        }
        
        // App is not allowed and not in grace period - block it immediately
        Log.w("AccessibilityService", "Periodic check: Blocking non-whitelisted app: $currentForeground")
        returnToLauncher()
    }
    
    private fun startPeriodicCheck() {
        if (!isPeriodicCheckRunning && getKidMode(this)) {
            isPeriodicCheckRunning = true
            handler.post(periodicCheckRunnable)
            Log.d("AccessibilityService", "Started periodic foreground app checking")
        }
    }
    
    private fun stopPeriodicCheck() {
        if (isPeriodicCheckRunning) {
            isPeriodicCheckRunning = false
            handler.removeCallbacks(periodicCheckRunnable)
            Log.d("AccessibilityService", "Stopped periodic foreground app checking")
        }
    }
    
    /**
     * Immediately check the current foreground app (called when service connects or kid mode enabled)
     */
    private fun checkCurrentForegroundAppImmediately() {
        if (!getKidMode(this)) {
            return
        }
        
        val currentForeground = getCurrentForegroundPackageFromUsageStats()
        
        if (currentForeground == null || currentForeground == this.packageName) {
            return
        }
        
        // Settings app - block immediately without grace period
        if (isSettingsApp(currentForeground)) {
            Log.w("AccessibilityService", "Settings app detected on service start - blocking immediately: $currentForeground")
            appsInGracePeriod.remove(currentForeground) // Remove from grace period if it was there
            returnToLauncher()
            // Check again multiple times to ensure it's blocked
            handler.postDelayed({
                val stillForeground = getCurrentForegroundPackageFromUsageStats()
                if (stillForeground == currentForeground || isSettingsApp(stillForeground ?: "")) {
                    Log.w("AccessibilityService", "Settings still open, blocking again (first check)")
                    returnToLauncher()
                }
            }, 100L)
            handler.postDelayed({
                val stillForeground = getCurrentForegroundPackageFromUsageStats()
                if (stillForeground == currentForeground || isSettingsApp(stillForeground ?: "")) {
                    Log.w("AccessibilityService", "Settings still open, blocking again (second check)")
                    returnToLauncher()
                }
            }, 500L)
            return
        }
        
        // If it's allowed, nothing to do
        if (isPackageAllowed(currentForeground)) {
            return
        }
        
        // Start grace period for the currently open app (in case user just enabled kid mode)
        if (!appsInGracePeriod.containsKey(currentForeground)) {
            val now = System.currentTimeMillis()
            appsInGracePeriod[currentForeground] = now
            Log.d("AccessibilityService", "Current foreground app $currentForeground not whitelisted, starting grace period")
            
            handler.postDelayed({
                checkAndBlockAfterGracePeriod(currentForeground)
            }, gracePeriodMs)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("AccessibilityService", "Service connected")
        
        // Start periodic checking if kid mode is active
        if (getKidMode(this)) {
            startPeriodicCheck()
            // Check current foreground app immediately (multiple times to catch edge cases)
            handler.postDelayed({
                checkCurrentForegroundAppImmediately()
                // Check again after a delay to catch apps that might have been opening
                handler.postDelayed({
                    checkCurrentForegroundAppImmediately()
                }, 1000L)
            }, 500L) // Small delay to ensure service is fully initialized
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopPeriodicCheck()
        handler.removeCallbacksAndMessages(null)
        appsInGracePeriod.clear()
        recentlyCheckedPackages.clear()
        lastCheckedForegroundPackage = null
    }
}
