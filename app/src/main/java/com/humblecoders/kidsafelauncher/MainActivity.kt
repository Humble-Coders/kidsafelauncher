package com.humblecoders.kidsafelauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable
)

data class LauncherSettings(
    val iconSize: Int = 56,
    val dockApps: Set<String> = emptySet()
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set window flag to show wallpaper behind content (no permissions needed)
        // This works on all Android versions and is the recommended approach
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER,
            WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER
        )
        // Make window background transparent so wallpaper shows through
        window.setBackgroundDrawableResource(android.R.color.transparent)

        // Always initialize the UI - never finish in onCreate when we're the default launcher
        // This prevents crash loops when the app is set as default launcher
        requestPermissionsIfNeeded()

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF6366F1), // Modern indigo - professional and vibrant
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFE0E7FF),
                    onPrimaryContainer = Color(0xFF1E1B4B),
                    secondary = Color(0xFF8B5CF6), // Purple - friendly and modern
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFEDE9FE),
                    onSecondaryContainer = Color(0xFF3B1F5F),
                    tertiary = Color(0xFFEC4899), // Pink - playful but professional
                    onTertiary = Color(0xFFFFFFFF),
                    tertiaryContainer = Color(0xFFFCE7F3),
                    onTertiaryContainer = Color(0xFF4A1E3A),
                    background = Color(0xFFFFFFFF), // Pure white
                    onBackground = Color(0xFF1F2937), // Dark gray for readability
                    surface = Color(0xFFF9FAFB), // Very light gray for depth
                    onSurface = Color(0xFF1F2937),
                    surfaceVariant = Color(0xFFF3F4F6),
                    onSurfaceVariant = Color(0xFF6B7280),
                    error = Color(0xFFEF4444),
                    onError = Color(0xFFFFFFFF),
                    errorContainer = Color(0xFFFEE2E2),
                    onErrorContainer = Color(0xFF991B1B),
                    outline = Color(0xFFE5E7EB),
                    outlineVariant = Color(0xFFD1D5DB)
                )
            ) {
                LauncherHomeScreen()
            }
        }
        
        // Update system UI based on kid mode
        updateSystemUI()
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        
        // Don't finish on HOME intents - always show the UI
        // The parent mode screen will show settings, and parents can change launcher from there
    }
    
    private fun updateSystemUI() {
        val isKidMode = getKidMode(this)
        
        if (isKidMode) {
            // Hide notification bar and system UI in kid mode using immersive mode
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            // Use immersive sticky mode to prevent notification bar access
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController?.let { controller ->
                // Hide status bar and navigation bar
                controller.hide(WindowInsetsCompat.Type.statusBars())
                controller.hide(WindowInsetsCompat.Type.navigationBars())
                // Use sticky behavior so bars stay hidden even after swipe
                controller.systemBarsBehavior = 
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            
            // Additional flags for older Android versions
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
            }
            
            // Keep screen on
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            // Handle display cutout for notched devices
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode = 
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        } else {
            // Show system UI in parent mode
            WindowCompat.setDecorFitsSystemWindows(window, true)
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController?.let { controller ->
                controller.show(WindowInsetsCompat.Type.statusBars())
                controller.show(WindowInsetsCompat.Type.navigationBars())
            }
            
            // Clear immersive mode flags for older Android
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
            }
            
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun requestPermissionsIfNeeded() {
        // Request Usage Stats permission
        if (!hasUsageStatsPermission()) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        }

        // Request Accessibility Service
        if (!isAccessibilityServiceEnabled()) {
            requestAccessibilityPermission()
        }
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        android.widget.Toast.makeText(
            this,
            "Please enable Accessibility Service for app blocking",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(packageName) == true
    }

    private fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = getSystemService(android.content.Context.APP_OPS_SERVICE) as? android.app.AppOpsManager
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
            android.util.Log.w("KidsafeLauncher", "Error checking usage stats permission: ${e.message}")
            false
        } catch (e: Exception) {
            // Handle any other exceptions
            android.util.Log.w("KidsafeLauncher", "Unexpected error checking usage stats permission: ${e.message}")
            false
        }
    }

    override fun onResume() {
        super.onResume()
        updateSystemUI() // Update system UI when resuming
        if (getKidMode(this)) {
            startMonitoringService()
        } else {
            stopMonitoringService()
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (getKidMode(this)) {
            // In kid mode, prevent back button from doing anything
            // Just consume the back press without any action
            return
        } else {
            // In parent mode, allow normal back button behavior
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        // Removed old reactive approach - accessibility service handles app blocking now
    }

    private fun startMonitoringService() {
        val intent = Intent(this, AppMonitorService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopMonitoringService() {
        val intent = Intent(this, AppMonitorService::class.java)
        stopService(intent)
    }
}

@Composable
fun WallpaperBackground() {
    // Note: Wallpaper is now shown using FLAG_SHOW_WALLPAPER window flag in MainActivity
    // This avoids permission issues on Android 13+ and is Play Store compliant
    // We just provide a semi-transparent overlay for better readability
    Box(modifier = Modifier.fillMaxSize()) {
        // Light overlay for better readability over wallpaper in light mode
        // The actual wallpaper shows through due to FLAG_SHOW_WALLPAPER flag
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.75f))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherPromptDialog(onDismiss: () -> Unit, onSetAsDefault: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                "Set as Default Launcher",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                "To use this app as your launcher, you need to set it as the default home app. This will allow the launcher to control your home screen.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onSetAsDefault,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Set as Default")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherHomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isKidMode by remember { mutableStateOf(getKidMode(context)) }
    var showSettings by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showLauncherPrompt by remember { mutableStateOf(false) }
    var showKidModeLauncherPrompt by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var settings by remember { mutableStateOf(getLauncherSettings(context)) }
    
    // Check if default launcher status changed when returning from settings
    DisposableEffect(showKidModeLauncherPrompt) {
        if (showKidModeLauncherPrompt) {
            // When dialog is shown, periodically check if launcher was set
            val job = scope.launch {
                while (showKidModeLauncherPrompt) {
                    delay(500) // Check every 500ms
                    if (isDefaultLauncher(context)) {
                        // Launcher was set, enable kid mode
                        showKidModeLauncherPrompt = false
                        isKidMode = true
                        saveKidMode(context, true)
                        isLoading = true
                        apps = loadInstalledApps(context, true)
                        delay(300)
                        isLoading = false
                    }
                }
            }
            // Cancel the job when dialog is dismissed
            onDispose {
                job.cancel()
            }
        } else {
            onDispose { }
        }
    }
    
    // Update system UI when kid mode changes
    LaunchedEffect(isKidMode) {
        (context as? ComponentActivity)?.let { activity ->
            activity.runOnUiThread {
                // Update system UI flags using the same method as MainActivity
                if (isKidMode) {
                    WindowCompat.setDecorFitsSystemWindows(activity.window, false)
                    val windowInsetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                    windowInsetsController?.let { controller ->
                        controller.hide(WindowInsetsCompat.Type.statusBars())
                        controller.hide(WindowInsetsCompat.Type.navigationBars())
                        controller.systemBarsBehavior = 
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                    
                    // Additional flags for older Android versions
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        @Suppress("DEPRECATION")
                        activity.window.decorView.systemUiVisibility = (
                            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        )
                    }
                } else {
                    WindowCompat.setDecorFitsSystemWindows(activity.window, true)
                    val windowInsetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                    windowInsetsController?.let { controller ->
                        controller.show(WindowInsetsCompat.Type.statusBars())
                        controller.show(WindowInsetsCompat.Type.navigationBars())
                    }
                    
                    // Clear immersive mode flags for older Android
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        @Suppress("DEPRECATION")
                        activity.window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
                    }
                }
            }
        }
    }
    
    // Continuously monitor and re-hide system UI in kid mode
    DisposableEffect(isKidMode) {
        if (isKidMode) {
            val activity = context as? ComponentActivity
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            val checkRunnable = object : Runnable {
                override fun run() {
                    if (isKidMode && activity != null) {
                        activity.runOnUiThread {
                            // Re-hide system UI if it becomes visible
                            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
                            val windowInsetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                            windowInsetsController?.let { controller ->
                                controller.hide(WindowInsetsCompat.Type.statusBars())
                                controller.hide(WindowInsetsCompat.Type.navigationBars())
                            }
                            
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                                @Suppress("DEPRECATION")
                                activity.window.decorView.systemUiVisibility = (
                                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                )
                            }
                        }
                        handler.postDelayed(this, 500) // Check every 500ms
                    }
                }
            }
            handler.post(checkRunnable)
            
            onDispose {
                handler.removeCallbacks(checkRunnable)
            }
        } else {
            onDispose { }
        }
    }

    // Load apps on startup - only in kid mode
    LaunchedEffect(isKidMode) {
        if (isKidMode) {
            isLoading = true
            delay(100) // Brief delay to show loading
            apps = loadInstalledApps(context, true)
            delay(300) // Smooth loading experience
            isLoading = false
        } else {
            // In parent mode, load apps only for settings
            isLoading = true
            delay(100)
            apps = loadInstalledApps(context, false)
            delay(200)
            isLoading = false
        }
    }

    // Filter apps based on search
    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isEmpty()) {
            apps
        } else {
            apps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Separate dock and regular apps
    val dockApps = remember(filteredApps, settings) {
        if (searchQuery.isEmpty()) {
            filteredApps.filter { settings.dockApps.contains(it.packageName) }
        } else emptyList()
    }

    val regularApps = remember(filteredApps, settings) {
        filteredApps.filter { !settings.dockApps.contains(it.packageName) }
    }

    // Dialog for enabling kid mode when not default launcher
    if (showKidModeLauncherPrompt) {
        AlertDialog(
            onDismissRequest = { showKidModeLauncherPrompt = false },
            icon = {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    "Set as Default Launcher Required",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    "To enable Kid Mode, this app must be set as the default launcher. Please set it as default using the button below, then try again.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        setAsDefaultLauncher(context)
                        showKidModeLauncherPrompt = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Open Launcher Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showKidModeLauncherPrompt = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        WallpaperBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                AnimatedVisibility(
                    visible = !isKidMode,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 2.dp
                    ) {
                        TopAppBar(
                            title = {
                                Text(
                                    "Parent Mode",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            actions = {},
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                titleContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box {
                        FloatingActionButton(
                            onClick = {
                                if (!isLoading) {
                                    if (isKidMode) {
                                        showPinDialog = true
                                    } else {
                                        // Check if app is default launcher before enabling kid mode
                                        if (!isDefaultLauncher(context)) {
                                            showKidModeLauncherPrompt = true
                                        } else {
                                            scope.launch {
                                                isLoading = true
                                                isKidMode = true
                                                saveKidMode(context, true)
                                                delay(200) // Brief delay for state update
                                                apps = loadInstalledApps(context, true)
                                                delay(300)
                                                isLoading = false
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.size(if (isKidMode) 56.dp else 64.dp),
                            containerColor = if (isKidMode)
                                MaterialTheme.colorScheme.secondary
                            else
                                MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(if (isKidMode) 24.dp else 28.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = if (isKidMode) Icons.Filled.Person else Icons.Filled.ChildCare,
                                    contentDescription = if (isKidMode) "Parent Mode" else "Kid Mode",
                                    modifier = Modifier.size(if (isKidMode) 24.dp else 28.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) { padding ->
            if (isKidMode) {
                // Kid Mode - Show apps
                // Get system insets for notch/display cutout in kid mode
                val systemInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout)
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(systemInsets) // Add padding for notch/status bar area
                        .padding(padding)
                ) {
                    // Loading Indicator
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 4.dp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Loading apps...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else if (regularApps.isEmpty() && dockApps.isEmpty()) {
                        // Empty State
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    "No apps available",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Add apps from Parent Mode settings",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // App Grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(if (isLandscape) 6 else 4),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = if (dockApps.isNotEmpty()) 100.dp else 16.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            items(regularApps, key = { it.packageName }) { app ->
                                AnimatedAppIcon(app = app, iconSize = settings.iconSize)
                            }
                        }
                    }

                    // Dock
                    AnimatedVisibility(
                        visible = dockApps.isNotEmpty() && !isLoading,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            tonalElevation = 8.dp,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                dockApps.take(5).forEach { app ->
                                    DockAppIcon(app = app, iconSize = settings.iconSize + 8)
                                }
                            }
                        }
                    }
                }
            } else {
                // Parent Mode - Show Settings
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Change Default Launcher Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        tonalElevation = 2.dp,
                    ) {
                        Button(
                            onClick = { setAsDefaultLauncher(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Change Default Launcher",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Settings Content
                    if (isLoading && apps.isEmpty()) {
                        // Loading apps for settings
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 4.dp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Loading apps...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            tonalElevation = 4.dp,
                        ) {
                            EnhancedSettingsContent(
                                apps = apps,
                                settings = settings,
                                onSave = { selectedApps, newSettings ->
                                    scope.launch {
                                        isLoading = true
                                        delay(200)
                                        saveWhitelist(context, selectedApps)
                                        saveLauncherSettings(context, newSettings)
                                        settings = newSettings
                                        if (isKidMode) {
                                            apps = loadInstalledApps(context, isKidMode)
                                            delay(300)
                                        }
                                        isLoading = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // PIN Dialog
    if (showPinDialog) {
        PinDialog(
            onDismiss = { showPinDialog = false },
            onSuccess = {
                isKidMode = false
                saveKidMode(context, false)
                showPinDialog = false
                // Reload apps for settings in parent mode
                scope.launch {
                    isLoading = true
                    delay(100)
                    apps = loadInstalledApps(context, false)
                    delay(200)
                    isLoading = false
                }
                // Don't finish the activity - show parent mode settings instead
                // Parents can change launcher from the settings screen
            }
        )
    }

    // Settings Dialog (kept for backward compatibility if needed)
    if (showSettings) {
        EnhancedSettingsDialog(
            apps = apps,
            settings = settings,
            onDismiss = { showSettings = false },
            onSave = { selectedApps, newSettings ->
                saveWhitelist(context, selectedApps)
                saveLauncherSettings(context, newSettings)
                settings = newSettings
                showSettings = false
                scope.launch {
                    isLoading = true
                    apps = loadInstalledApps(context, isKidMode)
                    delay(300)
                    isLoading = false
                }
            }
        )
    }
}

@Composable
fun AnimatedAppIcon(app: AppInfo, iconSize: Int = 56) {
    val context = LocalContext.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    launchApp(context, app.packageName)
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(iconSize.dp),
            shape = RoundedCornerShape(iconSize / 4),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            tonalElevation = 4.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = app.icon.toBitmap(iconSize, iconSize).asImageBitmap(),
                    contentDescription = app.label,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = app.label,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun DockAppIcon(app: AppInfo, iconSize: Int = 64) {
    val context = LocalContext.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Box(
        modifier = Modifier
            .size(iconSize.dp)
            .scale(scale)
            .clip(RoundedCornerShape(iconSize / 3))
            .clickable(
                onClick = {
                    launchApp(context, app.packageName)
                }
            )
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = app.icon.toBitmap(iconSize, iconSize).asImageBitmap(),
            contentDescription = app.label,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun PinDialog(onDismiss: () -> Unit, onSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val correctPin = remember { getPin(context) }

    LaunchedEffect(showError) {
        if (showError) {
            delay(500)
            showError = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                "Enter Parent PIN",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    "Enter PIN to access Parent Mode",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4) {
                            pin = it
                            showError = false
                        }
                    },
                    label = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = showError,
                    enabled = !isLoading,
                    supportingText = if (showError) {
                        { Text("Incorrect PIN", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        delay(300) // Simulate PIN check delay
                        if (pin == correctPin) {
                            isLoading = false
                            onSuccess()
                        } else {
                            isLoading = false
                            showError = true
                            pin = ""
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading && pin.length == 4
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSettingsContent(
    apps: List<AppInfo>,
    settings: LauncherSettings,
    onSave: (Set<String>, LauncherSettings) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initialWhitelist = remember { getWhitelist(context) }
    val initialDockApps = remember { settings.dockApps }
    val initialIconSize = remember { settings.iconSize }
    val initialPin = remember { getPin(context) }
    
    var whitelistSet by remember { mutableStateOf(initialWhitelist.toMutableSet()) }
    var iconSize by remember { mutableStateOf(initialIconSize) }
    var dockApps by remember { mutableStateOf(initialDockApps.toMutableSet()) }
    var selectedTab by remember { mutableStateOf(0) }
    var parentPin by remember { mutableStateOf(initialPin) }
    var searchQuery by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    
    // Check if any changes were made
    val hasChanges = remember(whitelistSet, dockApps, iconSize, parentPin) {
        whitelistSet != initialWhitelist.toSet() ||
        dockApps != initialDockApps ||
        iconSize != initialIconSize ||
        (parentPin.length == 4 && parentPin != initialPin)
    }
    
    // Filter apps based on search
    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isEmpty()) {
            apps
        } else {
            apps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
    }

    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxWidth()) {
        // Title
        Text(
            "Kid Mode Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(24.dp).padding(bottom = 0.dp)
        )

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Apps") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Dock") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Display") }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Security") }
            )
        }

        // Content - Use Box to constrain height, let each tab handle its own scrolling
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search Bar
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = {
                                        Text(
                                            "Search apps...",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    ),
                                    singleLine = true
                                )
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(1),
                            contentPadding = PaddingValues(bottom = if (hasChanges && !isSaving) 80.dp else 16.dp)
                        ) {
                            items(filteredApps) { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (whitelistSet.contains(app.packageName)) {
                                                whitelistSet = whitelistSet.toMutableSet().apply { remove(app.packageName) }
                                            } else {
                                                whitelistSet = whitelistSet.toMutableSet().apply { add(app.packageName) }
                                            }
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = whitelistSet.contains(app.packageName),
                                        onCheckedChange = { checked ->
                                            whitelistSet = if (checked) {
                                                whitelistSet.toMutableSet().apply { add(app.packageName) }
                                            } else {
                                                whitelistSet.toMutableSet().apply { remove(app.packageName) }
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Image(
                                        bitmap = app.icon.toBitmap(48, 48).asImageBitmap(),
                                        contentDescription = app.label,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        app.label,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    val dockEligibleApps = remember(whitelistSet, apps) {
                        apps.filter { whitelistSet.contains(it.packageName) }
                    }
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        contentPadding = PaddingValues(bottom = if (hasChanges && !isSaving) 80.dp else 16.dp)
                    ) {
                        items(dockEligibleApps) { app ->
                            val isChecked = dockApps.contains(app.packageName)
                            val canCheck = isChecked || dockApps.size < 5
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (isChecked) {
                                            dockApps = dockApps.toMutableSet().apply { remove(app.packageName) }
                                        } else if (canCheck) {
                                            dockApps = dockApps.toMutableSet().apply { add(app.packageName) }
                                        }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        dockApps = if (checked && canCheck) {
                                            dockApps.toMutableSet().apply { add(app.packageName) }
                                        } else if (!checked) {
                                            dockApps.toMutableSet().apply { remove(app.packageName) }
                                        } else {
                                            dockApps
                                        }
                                    },
                                    enabled = canCheck
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Image(
                                    bitmap = app.icon.toBitmap(48, 48).asImageBitmap(),
                                    contentDescription = app.label,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        app.label,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (!canCheck && !isChecked) {
                                        Text(
                                            "Dock is full (max 5 apps)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            "Icon Size: ${iconSize}dp",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = iconSize.toFloat(),
                            onValueChange = { iconSize = it.toInt() },
                            valueRange = 40f..80f,
                            steps = 7,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Preview",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (apps.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                apps.take(3).forEach { app ->
                                    Image(
                                        bitmap = app.icon.toBitmap(iconSize, iconSize).asImageBitmap(),
                                        contentDescription = app.label,
                                        modifier = Modifier
                                            .size(iconSize.dp)
                                            .clip(RoundedCornerShape(iconSize / 4))
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(if (hasChanges && !isSaving) 80.dp else 16.dp))
                    }
                }
                3 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            "Parent PIN",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Set a 4-digit PIN to access Parent Mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = parentPin,
                            onValueChange = {
                                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                    parentPin = it
                                }
                            },
                            label = { Text("PIN (4 digits)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = {
                                if (parentPin.length < 4 && parentPin.isNotEmpty()) {
                                    Text(
                                        "PIN must be 4 digits",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else if (parentPin.length == 4) {
                                    Text("PIN is set")
                                }
                            },
                            isError = parentPin.isNotEmpty() && parentPin.length < 4
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Current PIN: ${if (parentPin.isEmpty()) "Not set" else "••••"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(if (hasChanges && !isSaving) 80.dp else 16.dp))
                    }
                }
            }
        }

        // Save Button - Position outside the scrollable content area
        AnimatedVisibility(
            visible = hasChanges && !isSaving,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 88.dp), // Extra padding to clear FAB
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            delay(300) // Show loading state
                            
                            val newSettings = LauncherSettings(
                                iconSize = iconSize,
                                dockApps = dockApps.toSet()
                            )
                            // Save PIN if it's valid (4 digits)
                            if (parentPin.length == 4) {
                                savePin(context, parentPin)
                            }
                            onSave(whitelistSet, newSettings)
                            
                            isSaving = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = (parentPin.isEmpty() || parentPin.length == 4) && !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Save Settings")
                }
            }
        }
        
        // Loading indicator when saving
        if (isSaving) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 88.dp), // Extra padding to clear FAB
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Saving...")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSettingsDialog(
    apps: List<AppInfo>,
    settings: LauncherSettings,
    onDismiss: () -> Unit,
    onSave: (Set<String>, LauncherSettings) -> Unit
) {
    val context = LocalContext.current
    val whitelist = remember { mutableStateOf(getWhitelist(context)) }
    val whitelistSet = whitelist.value.toMutableSet()
    var iconSize by remember { mutableStateOf(settings.iconSize) }
    var dockApps by remember { mutableStateOf(settings.dockApps.toMutableSet()) }
    var selectedTab by remember { mutableStateOf(0) }
    var parentPin by remember { mutableStateOf(getPin(context)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Title
                Text(
                    "Launcher Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(24.dp).padding(bottom = 0.dp)
                )

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Apps") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Dock") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Display") }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Security") }
                    )
                }

                // Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .padding(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(1)
                            ) {
                                items(apps) { app ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                if (whitelistSet.contains(app.packageName)) {
                                                    whitelistSet.remove(app.packageName)
                                                } else {
                                                    whitelistSet.add(app.packageName)
                                                }
                                                whitelist.value = whitelistSet.toSet()
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = whitelistSet.contains(app.packageName),
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    whitelistSet.add(app.packageName)
                                                } else {
                                                    whitelistSet.remove(app.packageName)
                                                }
                                                whitelist.value = whitelistSet.toSet()
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Image(
                                            bitmap = app.icon.toBitmap(48, 48).asImageBitmap(),
                                            contentDescription = app.label,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            app.label,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(1)
                            ) {
                                items(apps.filter { whitelistSet.contains(it.packageName) }) { app ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                if (dockApps.contains(app.packageName)) {
                                                    dockApps.remove(app.packageName)
                                                } else if (dockApps.size < 5) {
                                                    dockApps.add(app.packageName)
                                                }
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = dockApps.contains(app.packageName),
                                            onCheckedChange = { checked ->
                                                if (checked && dockApps.size < 5) {
                                                    dockApps.add(app.packageName)
                                                } else if (!checked) {
                                                    dockApps.remove(app.packageName)
                                                }
                                            },
                                            enabled = dockApps.contains(app.packageName) || dockApps.size < 5
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Image(
                                            bitmap = app.icon.toBitmap(48, 48).asImageBitmap(),
                                            contentDescription = app.label,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                app.label,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            if (dockApps.size >= 5 && !dockApps.contains(app.packageName)) {
                                                Text(
                                                    "Dock is full (max 5 apps)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    "Icon Size: ${iconSize}dp",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Slider(
                                    value = iconSize.toFloat(),
                                    onValueChange = { iconSize = it.toInt() },
                                    valueRange = 40f..80f,
                                    steps = 7,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    "Preview",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                if (apps.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        apps.take(3).forEach { app ->
                                            Image(
                                                bitmap = app.icon.toBitmap(iconSize, iconSize).asImageBitmap(),
                                                contentDescription = app.label,
                                                modifier = Modifier
                                                    .size(iconSize.dp)
                                                    .clip(RoundedCornerShape(iconSize / 4))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        3 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    "Parent PIN",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Set a 4-digit PIN to access Parent Mode",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = parentPin,
                                    onValueChange = {
                                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                            parentPin = it
                                        }
                                    },
                                    label = { Text("PIN (4 digits)") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    supportingText = {
                                        if (parentPin.length < 4 && parentPin.isNotEmpty()) {
                                            Text(
                                                "PIN must be 4 digits",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        } else if (parentPin.length == 4) {
                                            Text("PIN is set")
                                        }
                                    },
                                    isError = parentPin.isNotEmpty() && parentPin.length < 4
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    "Current PIN: ${if (parentPin.isEmpty()) "Not set" else "••••"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newSettings = LauncherSettings(
                                iconSize = iconSize,
                                dockApps = dockApps.toSet()
                            )
                            // Save PIN if it's valid (4 digits)
                            if (parentPin.length == 4) {
                                savePin(context, parentPin)
                            }
                            onSave(whitelistSet, newSettings)
                        },
                        shape = RoundedCornerShape(12.dp),
                        enabled = parentPin.isEmpty() || parentPin.length == 4
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// Helper Functions
fun isDefaultLauncher(context: android.content.Context): Boolean {
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
    }
    val pm = context.packageManager
    val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return resolveInfo?.activityInfo?.packageName == context.packageName
}
fun setAsDefaultLauncher(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_HOME_SETTINGS)
    context.startActivity(intent)
}
fun loadInstalledApps(context: android.content.Context, isKidMode: Boolean): List<AppInfo> {
    val pm: PackageManager = context.packageManager
    val whitelist = if (isKidMode) getWhitelist(context) else null
    
    // Use queryIntentActivities instead of getInstalledApplications to avoid QUERY_ALL_PACKAGES
    // This works with the <queries> tag in manifest
    val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    
    // Use 0 instead of MATCH_DEFAULT_ONLY to get ALL apps with launcher activities
    // MATCH_DEFAULT_ONLY only returns apps with default activities, which is too restrictive
    val resolvedActivities = pm.queryIntentActivities(launcherIntent, 0)
    
    // Group by package name to get unique packages
    val packageMap = mutableMapOf<String, android.content.pm.ResolveInfo>()
    for (resolveInfo in resolvedActivities) {
        val packageName = resolveInfo.activityInfo.packageName
        // Exclude our own app
        if (packageName == context.packageName) continue
        
        // Apply whitelist filter in kid mode
        if (isKidMode && whitelist != null && !whitelist.contains(packageName)) {
            continue
        }
        
        // Keep the first resolve info for each package (or you could pick the main activity)
        if (!packageMap.containsKey(packageName)) {
            packageMap[packageName] = resolveInfo
        }
    }
    
    return packageMap.values
        .mapNotNull { resolveInfo ->
            try {
                val packageName = resolveInfo.activityInfo.packageName
                // Get label and icon from resolve info or package manager
                val label = resolveInfo.loadLabel(pm).toString()
                val icon = resolveInfo.loadIcon(pm)
                
                AppInfo(
                    label = label,
                    packageName = packageName,
                    icon = icon
                )
            } catch (e: Exception) {
                // If we can't get app info, skip it
                android.util.Log.w("KidsafeLauncher", "Could not load app info: ${e.message}")
                null
            }
        }
        .sortedBy { it.label.lowercase() }
}
fun launchApp(context: android.content.Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (intent != null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
            android.util.Log.d("KidsafeLauncher", "Launched app: $packageName")
        } catch (e: Exception) {
            android.util.Log.e("KidsafeLauncher", "Error launching app: $packageName", e)
        }
    }
}
fun getWhitelist(context: android.content.Context): Set<String> {
    val prefs = context.getSharedPreferences("launcher_prefs", android.content.Context.MODE_PRIVATE)
    return prefs.getStringSet("whitelist", emptySet()) ?: emptySet()
}
fun saveWhitelist(context: android.content.Context, whitelist: Set<String>) {
    val prefs = context.getSharedPreferences("launcher_prefs", android.content.Context.MODE_PRIVATE)
    prefs.edit().putStringSet("whitelist", whitelist).apply()
}
fun getKidMode(context: android.content.Context): Boolean {
    val prefs = context.getSharedPreferences("launcher_prefs", android.content.Context.MODE_PRIVATE)
    return prefs.getBoolean("kid_mode", false)
}
fun saveKidMode(context: android.content.Context, isKidMode: Boolean) {
    val prefs = context.getSharedPreferences("launcher_prefs", android.content.Context.MODE_PRIVATE)
    prefs.edit().putBoolean("kid_mode", isKidMode).apply()
}
fun getLauncherSettings(context: android.content.Context): LauncherSettings {
    val prefs = context.getSharedPreferences("launcher_prefs", android.content.Context.MODE_PRIVATE)
    return LauncherSettings(
        iconSize = prefs.getInt("icon_size", 56),
        dockApps = prefs.getStringSet("dock_apps", emptySet()) ?: emptySet()
    )
}
fun saveLauncherSettings(context: android.content.Context, settings: LauncherSettings) {
    val prefs = context.getSharedPreferences("launcher_prefs", android.content.Context.MODE_PRIVATE)
    prefs.edit().apply {
        putInt("icon_size", settings.iconSize)
        putStringSet("dock_apps", settings.dockApps)
        apply()
    }
}
fun getPin(context: android.content.Context): String {
    val prefs = context.getSharedPreferences("launcher_prefs", android.content.Context.MODE_PRIVATE)
    return prefs.getString("parent_pin", "1234") ?: "1234"
}
fun savePin(context: android.content.Context, pin: String) {
    val prefs = context.getSharedPreferences("launcher_prefs", android.content.Context.MODE_PRIVATE)
    prefs.edit().putString("parent_pin", pin).apply()
}

