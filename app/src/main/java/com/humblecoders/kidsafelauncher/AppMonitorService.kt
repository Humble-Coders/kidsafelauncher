package com.humblecoders.kidsafelauncher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class AppMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 1000L // Check every 1 second (backup to accessibility)
    private var isMonitoring = false

    private val monitorRunnable = object : Runnable {
        override fun run() {
            if (isMonitoring) {
                checkForegroundApp()
                handler.postDelayed(this, checkInterval)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1, notification)

        isMonitoring = true
        handler.post(monitorRunnable)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        handler.removeCallbacks(monitorRunnable)
    }

    private fun checkForegroundApp() {
        val currentApp = getForegroundApp()
        
        if (currentApp != null && currentApp != packageName) {
            // Always block Settings app - no exceptions
            if (isSettingsApp(currentApp)) {
                android.util.Log.w("AppMonitorService", "Blocking Settings app: $currentApp")
                returnToLauncher()
                return
            }
            
            val whitelist = getWhitelist(this)
            
            if (!whitelist.contains(currentApp)) {
                android.util.Log.w("AppMonitorService", "Blocking app: $currentApp")
                returnToLauncher()
            }
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

    private fun getForegroundApp(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            return try {
                // Check permission first to avoid system errors
                if (!hasUsageStatsPermission()) {
                    return null
                }
                
                val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                if (usageStatsManager == null) {
                    return null
                }
                
                val time = System.currentTimeMillis()
                val usageEvents = usageStatsManager.queryEvents(time - 2000, time)
                val event = UsageEvents.Event()

                var lastApp: String? = null
                var lastEventTime = 0L
                
                while (usageEvents.hasNextEvent()) {
                    usageEvents.getNextEvent(event)
                    if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                        if (event.timeStamp > lastEventTime) {
                            lastApp = event.packageName
                            lastEventTime = event.timeStamp
                        }
                    }
                }
                
                lastApp
            } catch (e: SecurityException) {
                // Permission not granted or package verification failed
                android.util.Log.w("AppMonitorService", "SecurityException getting foreground app: ${e.message}")
                null
            } catch (e: Exception) {
                // Handle any other exceptions gracefully
                android.util.Log.w("AppMonitorService", "Error getting foreground app: ${e.message}")
                null
            }
        }
        return null
    }
    
    private fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager
            if (appOps == null) {
                return false
            }
            
            val mode = appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: SecurityException) {
            // Handle SecurityException gracefully - permission not granted or package not found
            android.util.Log.w("AppMonitorService", "Error checking usage stats permission: ${e.message}")
            false
        } catch (e: Exception) {
            // Handle any other exceptions
            android.util.Log.w("AppMonitorService", "Unexpected error checking usage stats permission: ${e.message}")
            false
        }
    }

    private fun returnToLauncher() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Kid Mode Active",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitoring active apps in Kid Mode"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Kid Mode Active")
            .setContentText("Protecting your child from unauthorized apps")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "kid_mode_channel"
    }
}
