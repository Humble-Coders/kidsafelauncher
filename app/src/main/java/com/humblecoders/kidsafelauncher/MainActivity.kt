package com.humblecoders.kidsafelauncher

import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
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

        requestPermissionsIfNeeded()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF6750A4),
                    secondary = Color(0xFF625B71),
                    tertiary = Color(0xFF7D5260),
                    background = Color(0xFF1C1B1F),
                    surface = Color(0xFF1C1B1F)
                )
            ) {
                LauncherHomeScreen()
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        if (!hasUsageStatsPermission()) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        }

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        // Request MANAGE_EXTERNAL_STORAGE permission for wallpaper access (Android 11+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = android.net.Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback for devices that don't support the above intent
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(android.content.Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    override fun onResume() {
        super.onResume()
        if (getKidMode(this)) {
            startMonitoringService()
        } else {
            stopMonitoringService()
        }
    }

    override fun onPause() {
        super.onPause()
        if (getKidMode(this)) {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (getKidMode(this)) {
                    startActivity(intent)
                }
            }, 100)
        }
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
    val context = LocalContext.current
    val wallpaperBitmap = remember {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            
            // Try different methods to get wallpaper drawable
            val drawable = when {
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M -> {
                    // For API 23+, use getDrawable()
                    wallpaperManager.getDrawable()
                }
                else -> {
                    // For older APIs, use the drawable property
                    wallpaperManager.drawable
                }
            }
            
            if (drawable == null) {
                android.util.Log.w("WallpaperBackground", "Wallpaper drawable is null")
                null
            } else {
                // Get screen dimensions for proper bitmap size
                val displayMetrics = context.resources.displayMetrics
                val width = displayMetrics.widthPixels
                val height = displayMetrics.heightPixels
                
                // Convert drawable to bitmap
                when {
                    drawable is BitmapDrawable && drawable.bitmap != null -> {
                        drawable.bitmap
                    }
                    else -> {
                        // For other drawable types, create a bitmap with screen dimensions
                        val bitmap = android.graphics.Bitmap.createBitmap(
                            width.coerceAtLeast(1),
                            height.coerceAtLeast(1),
                            android.graphics.Bitmap.Config.ARGB_8888
                        )
                        val canvas = android.graphics.Canvas(bitmap)
                        drawable.setBounds(0, 0, width, height)
                        drawable.draw(canvas)
                        bitmap
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission denied - fall back to gradient
            android.util.Log.w("WallpaperBackground", "Permission denied for wallpaper access: ${e.message}", e)
            null
        } catch (e: Exception) {
            // Other errors - fall back to gradient
            android.util.Log.w("WallpaperBackground", "Error loading wallpaper: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (wallpaperBitmap != null) {
            Image(
                bitmap = wallpaperBitmap.asImageBitmap(),
                contentDescription = "Wallpaper",
                modifier = Modifier
                    .fillMaxSize()
                    .blur(8.dp),
                contentScale = ContentScale.Crop
            )
            // Dark overlay for better readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        } else {
            // Gradient background fallback
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1a237e),
                                Color(0xFF0d47a1),
                                Color(0xFF01579b)
                            )
                        )
                    )
            )
        }
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
    var showLauncherPrompt by remember { mutableStateOf(!isDefaultLauncher(context)) }
    var searchQuery by remember { mutableStateOf("") }
    var settings by remember { mutableStateOf(getLauncherSettings(context)) }

    // Load apps on startup
    LaunchedEffect(isKidMode) {
        isLoading = true
        apps = loadInstalledApps(context, isKidMode)
        delay(300) // Smooth loading experience
        isLoading = false
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

    if (showLauncherPrompt && !isKidMode) {
        LauncherPromptDialog(
            onDismiss = { showLauncherPrompt = false },
            onSetAsDefault = {
                setAsDefaultLauncher(context)
                showLauncherPrompt = false
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
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TopAppBar(
                            title = {
                                Text(
                                    "Parent Mode",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            actions = {
                                IconButton(onClick = { showSettings = true }) {
                                    Icon(
                                        Icons.Default.Settings,
                                        "Settings",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent
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
                    FloatingActionButton(
                        onClick = {
                            if (isKidMode) {
                                showPinDialog = true
                            } else {
                                isKidMode = true
                                saveKidMode(context, true)
                                scope.launch {
                                    isLoading = true
                                    apps = loadInstalledApps(context, true)
                                    delay(300)
                                    isLoading = false
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
                        Icon(
                            imageVector = if (isKidMode) Icons.Filled.Person else Icons.Filled.ChildCare,
                            contentDescription = if (isKidMode) "Parent Mode" else "Kid Mode",
                            modifier = Modifier.size(if (isKidMode) 24.dp else 28.dp)
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Search Bar
                AnimatedVisibility(
                    visible = !isKidMode && apps.isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
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
                                ),
                                singleLine = true
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }

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
                                color = Color.White,
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
                                tint = Color.White.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                if (isKidMode) "No apps available" else "No apps found",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (isKidMode)
                                    "Add apps from Parent Mode settings"
                                else
                                    "Try a different search term",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.7f),
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
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        tonalElevation = 8.dp
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
                scope.launch {
                    isLoading = true
                    apps = loadInstalledApps(context, false)
                    delay(300)
                    isLoading = false
                }
            }
        )
    }

    // Settings Dialog
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
            color = Color.White.copy(alpha = 0.1f),
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
            color = Color.White,
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
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
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
    var pin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
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
                    supportingText = if (showError) {
                        { Text("Incorrect PIN", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin == correctPin) {
                        onSuccess()
                    } else {
                        showError = true
                        pin = ""
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
    val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val launcherApps = pm.queryIntentActivities(launcherIntent, 0)
        .map { it.activityInfo.packageName }
        .toSet()

    val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

    return packages
        .filter { appInfo ->
            if (appInfo.packageName == context.packageName) return@filter false

            val hasLauncherIntent = launcherApps.contains(appInfo.packageName)
            val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)

            if (!hasLauncherIntent && launchIntent == null) return@filter false

            if (isKidMode && whitelist != null) {
                return@filter whitelist.contains(appInfo.packageName)
            }

            true
        }
        .mapNotNull { appInfo ->
            try {
                AppInfo(
                    label = pm.getApplicationLabel(appInfo).toString(),
                    packageName = appInfo.packageName,
                    icon = pm.getApplicationIcon(appInfo)
                )
            } catch (e: Exception) {
                null
            }
        }
        .sortedBy { it.label.lowercase() }
}
fun launchApp(context: android.content.Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (intent != null) {
        context.startActivity(intent)
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

