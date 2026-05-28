package com.example.ui

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TodoAppShell(
    todoViewModel: TodoViewModel,
    authViewModel: AuthViewModel,
    sharedText: String? = null // Receives shared intent text if present
) {
    val onboardingCompleted by authViewModel.onboardingCompleted.collectAsStateWithLifecycle()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val isGuestMode by authViewModel.isGuestMode.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf("Splash") }

    LaunchedEffect(currentScreen) {
        if (currentScreen == "Splash") {
            delay(1800) // Beautiful cinematic intro timing
            currentScreen = when {
                !onboardingCompleted -> "Onboarding"
                !isLoggedIn && !isGuestMode -> "Auth"
                else -> "Main"
            }
        }
    }

    // Direct routing based on authentication state
    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
        },
        label = "AppScreenTransitions"
    ) { screen ->
        when (screen) {
            "Splash" -> SplashScreen()
            "Onboarding" -> OnboardingCarousel(
                onComplete = {
                    authViewModel.completeOnboarding()
                    currentScreen = "Auth"
                }
            )
            "Auth" -> AuthScreen(
                viewModel = authViewModel,
                onSuccess = { currentScreen = "Main" }
            )
            "Main" -> MainAppShell(
                todoViewModel = todoViewModel,
                authViewModel = authViewModel,
                initialSharedText = sharedText
            )
        }
    }
}

// ==========================================
// 1. SPLASH SCREEN
// ==========================================
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E1E2F),
                        Color(0xFF0F0F1B)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant geometric custom app logo drawing
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF7C4DFF), Color(0xFF00B0FF))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "App Logo Checkmark",
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "T O D O",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 6.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Simplify. Organize. Achieve.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 2.sp
            )
        }
    }
}

// ==========================================
// 2. ONBOARDING CAROUSEL
// ==========================================
@Composable
fun OnboardingCarousel(onComplete: () -> Unit) {
    val carouselSlides = listOf(
        Slide(
            title = "Capture Ideas Instantly",
            desc = "Add tasks in seconds using our Quick-Add view. Type or speak your mind to organize life effortlessly.",
            icon = Icons.Default.Add,
            accentColor = Color(0xFF7C4DFF)
        ),
        Slide(
            title = "Smart NLP Task Parsing",
            desc = "Simply type \"Buy milk tomorrow at 5 PM\" and watch the app auto-parse dates, times, and priority lists.",
            icon = Icons.Default.AutoAwesome,
            accentColor = Color(0xFF00B0FF)
        ),
        Slide(
            title = "Deep Color-Coded Lists",
            desc = "Tag your tasks with customized labels, work boards, or grocery lists so everything coordinates beautifully.",
            icon = Icons.Default.Folder,
            accentColor = Color(0xFFFF9100)
        ),
        Slide(
            title = "Collaborate & Discuss",
            desc = "Share task boards with teammates and coordinate instantly inside dedicated local task comment feeds.",
            icon = Icons.Default.Comment,
            accentColor = Color(0xFF00E676)
        )
    )

    var currentSlideIndex by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040209))
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Upper row: Skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onComplete) {
                    Text("Skip", color = Color.White.copy(alpha = 0.6f))
                }
            }

            // Slide Image & Info
            val slide = carouselSlides[currentSlideIndex]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(
                            slide.accentColor.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = slide.icon,
                        contentDescription = slide.title,
                        tint = slide.accentColor,
                        modifier = Modifier.size(80.dp)
                    )
                }
                Spacer(modifier = Modifier.height(48.dp))
                Text(
                    text = slide.title,
                    fontSize = 24.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = slide.desc,
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    lineHeight = 22.sp
                )
            }

            // Dot Indicator and Next / Complete Fab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    carouselSlides.forEachIndexed { i, _ ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(width = if (i == currentSlideIndex) 20.dp else 8.dp, height = 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (i == currentSlideIndex) slide.accentColor else Color.White.copy(alpha = 0.2f))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        if (currentSlideIndex < carouselSlides.size - 1) {
                            currentSlideIndex++
                        } else {
                            onComplete()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = slide.accentColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("onboarding_next_button")
                ) {
                    Text(
                        text = if (currentSlideIndex == carouselSlides.size - 1) "Get Started" else "Next",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

private data class Slide(
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val accentColor: Color
)

// ==========================================
// 3. AUTHENTICATION & LOGIN SCREEN
// ==========================================
@Composable
fun AuthScreen(viewModel: AuthViewModel, onSuccess: () -> Unit) {
    val error by viewModel.authError.collectAsStateWithLifecycle()
    val forgotPasswordSent by viewModel.forgotPasswordSent.collectAsStateWithLifecycle()

    var isRegisterState by remember { mutableStateOf(false) }
    var forgotPasswordMode by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0E17))
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant Icon Greeting
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF7C4DFF),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when {
                    forgotPasswordMode -> "Reset Password"
                    isRegisterState -> "Create Account"
                    else -> "Welcome Back"
                },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = when {
                    forgotPasswordMode -> "Enter email to receive a recovery link."
                    isRegisterState -> "Sign up to synchronize your tasks across devices."
                    else -> "Log in to check progress on your daily habits."
                },
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Dynamic Inputs Fields
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address", color = Color.White.copy(alpha = 0.6f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_email_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF7C4DFF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            if (!forgotPasswordMode) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = Color.White.copy(alpha = 0.6f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_password_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF7C4DFF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Warning and error handling alerts
            error?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = it, color = Color.Red, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            if (forgotPasswordSent) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFF00E676).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E676))
                ) {
                    Text(
                        text = "Recovery email has been dispatched to $email!",
                        color = Color(0xFF00E676),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = {
                    if (forgotPasswordMode) {
                        viewModel.sendForgotPasswordLink(email)
                    } else {
                        val authSuccess = if (isRegisterState) {
                            viewModel.registerWithEmail(email, password)
                        } else {
                            viewModel.loginWithEmail(email, password)
                        }
                        if (authSuccess) {
                            onSuccess()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("auth_action_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = when {
                        forgotPasswordMode -> "Send Recovery Link"
                        isRegisterState -> "Sign Up"
                        else -> "Sign In"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Forgot Password and Registration toggles
            if (!forgotPasswordMode) {
                TextButton(onClick = { forgotPasswordMode = true }) {
                    Text("Forgot Password?", color = Color(0xFF00B0FF))
                }
            } else {
                TextButton(onClick = {
                    forgotPasswordMode = false
                    viewModel.resetForgotPassword()
                }) {
                    Text("Back to log in", color = Color(0xFF00B0FF))
                }
            }

            if (!forgotPasswordMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRegisterState) "Already have an account?" else "Don't have an account?",
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    TextButton(onClick = { isRegisterState = !isRegisterState }) {
                        Text(
                            text = if (isRegisterState) "Sign In" else "Sign Up",
                            color = Color(0xFF7C4DFF)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            // SSO Single Sign-On and Guest Check-ins
            Button(
                onClick = {
                    viewModel.loginWithGoogle()
                    onSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("sso_google_button"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Google Icon",
                        tint = Color(0xFFECEFF1),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Sign in with Google", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = {
                    viewModel.continueAsGuest()
                    onSuccess()
                },
                modifier = Modifier.testTag("guest_button")
            ) {
                Text(
                    text = "Continue as Guest (Local Storage)",
                    color = Color.White.copy(alpha = 0.7f),
                    textDecoration = TextDecoration.Underline,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ==========================================
// 4. MAIN APP SHELL (DRAWER + BOTTOM NAV + MULTI-SCREEN NAVIGATION)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppShell(
    todoViewModel: TodoViewModel,
    authViewModel: AuthViewModel,
    initialSharedText: String? = null
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var currentBottomTab by remember { mutableStateOf("Tasks") } // "Tasks", "Calendar", "Search", "Settings"
    var activeTaskIdForDetails by remember { mutableStateOf<Long?>(null) } // null = show lists, id = show detail edit view

    val projects by todoViewModel.projects.collectAsStateWithLifecycle()
    val tags by todoViewModel.tags.collectAsStateWithLifecycle()
    val selectedProjectId by todoViewModel.selectedProjectId.collectAsStateWithLifecycle()
    val selectedTagId by todoViewModel.selectedTagId.collectAsStateWithLifecycle()
    val smartView by todoViewModel.smartView.collectAsStateWithLifecycle()

    val userEmail by authViewModel.userEmail.collectAsStateWithLifecycle()
    val isGuestMode by authViewModel.isGuestMode.collectAsStateWithLifecycle()

    // Modals
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var showAddProjectDialog by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }

    // Floating Shared Intent Capture
    var sharedIntentCachedText by remember { mutableStateOf(initialSharedText) }
    val context = LocalContext.current

    LaunchedEffect(sharedIntentCachedText) {
        sharedIntentCachedText?.let { text ->
            todoViewModel.addTaskWithNlp(text)
            Toast.makeText(context, "Parsed & saved shared text!", Toast.LENGTH_LONG).show()
            sharedIntentCachedText = null // Clear cache
        }
    }

    // Intercept Back button inside Task Details view
    if (activeTaskIdForDetails != null) {
        BackHandler {
            activeTaskIdForDetails = null
        }
    }

    Box {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = activeTaskIdForDetails == null,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(300.dp),
                    drawerContainerColor = Color(0xFFFDF8F6)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(20.dp)
                    ) {
                        // Profile Banner in Drawer
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFF8F4C38), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isGuestMode) "G" else userEmail?.take(1)?.uppercase() ?: "U",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isGuestMode) "Guest Profile" else userEmail ?: "Premium User",
                                    color = Color(0xFF1F1A18),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (isGuestMode) "Offline sync" else "Cloud Sync Active",
                                    color = Color(0xFF514441),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFF85736E).copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

                        // Smart Views Navigation lists
                        Text(
                            "Smart Filters",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF514441),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val smartItems = listOf(
                            Triple("Inbox", "Inbox", Icons.Default.Inbox),
                            Triple("Today", "Today", Icons.Default.Today),
                            Triple("Upcoming", "Upcoming", Icons.Default.CalendarMonth),
                            Triple("Completed", "Completed", Icons.Default.TaskAlt)
                        )

                        smartItems.forEach { (viewKey, label, icon) ->
                            val active = smartView == viewKey && selectedProjectId == null && selectedTagId == null
                            NavigationDrawerItem(
                                label = { Text(label, color = if (active) Color(0xFF8F4C38) else Color(0xFF514441)) },
                                selected = active,
                                onClick = {
                                    todoViewModel.setSmartView(viewKey)
                                    currentBottomTab = "Tasks"
                                    coroutineScope.launch { drawerState.close() }
                                },
                                icon = { Icon(icon, contentDescription = null, tint = if (active) Color(0xFF8F4C38) else Color(0xFF514441)) },
                                modifier = Modifier.padding(vertical = 2.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = Color(0xFFFFDBD1),
                                    unselectedContainerColor = Color.Transparent
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Projects Navigation List
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Projects",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF514441)
                            )
                            IconButton(onClick = { showAddProjectDialog = true }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Add Project", tint = Color(0xFF8F4C38), modifier = Modifier.size(16.dp))
                            }
                        }

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(projects) { project ->
                                val active = selectedProjectId == project.id
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) Color(0xFF8F4C38).copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable {
                                            todoViewModel.selectProject(project.id)
                                            currentBottomTab = "Tasks"
                                            coroutineScope.launch { drawerState.close() }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(Color(project.color), shape = CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = project.name,
                                        color = if (active) Color(0xFF8F4C38) else Color(0xFF1F1A18),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { todoViewModel.deleteProject(project) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFF85736E).copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFF85736E).copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

                        // Tags Configuration section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Tags & Labels",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF514441)
                            )
                            IconButton(onClick = { showAddTagDialog = true }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Add Tag", tint = Color(0xFF8F4C38), modifier = Modifier.size(16.dp))
                            }
                        }

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(tags) { tag ->
                                val active = selectedTagId == tag.id
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) Color(0xFF8F4C38).copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable {
                                            todoViewModel.selectTag(tag.id)
                                            currentBottomTab = "Tasks"
                                            coroutineScope.launch { drawerState.close() }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(tag.color), shape = RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = tag.name,
                                        color = if (active) Color(0xFF8F4C38) else Color(0xFF1F1A18),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { todoViewModel.deleteTag(tag) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFF85736E).copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    if (activeTaskIdForDetails == null) {
                        LargeTopAppBar(
                            title = {
                                Text(
                                    text = if (selectedProjectId != null) {
                                        projects.find { it.id == selectedProjectId }?.name ?: "Tasks List"
                                    } else if (selectedTagId != null) {
                                        tags.find { it.id == selectedTagId }?.name ?: "Tagged Tasks"
                                    } else {
                                        smartView
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1F1A18)
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = null, tint = Color(0xFF1F1A18))
                                }
                            },
                            actions = {
                                IconButton(onClick = { currentBottomTab = "Search" }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF1F1A18))
                                }
                            },
                            colors = TopAppBarDefaults.largeTopAppBarColors(
                                containerColor = Color(0xFFFDF8F6),
                                titleContentColor = Color(0xFF1F1A18)
                            )
                        )
                    }
                },
                bottomBar = {
                    if (activeTaskIdForDetails == null) {
                        Surface(
                            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                            color = Color(0xFFF5DED8),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            NavigationBar(
                                containerColor = Color.Transparent,
                                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 8.dp)
                            ) {
                                val navItems = listOf(
                                    Triple("Tasks", "Tasks", Icons.Default.Check),
                                    Triple("Calendar", "Calendar", Icons.Default.CalendarToday),
                                    Triple("Search", "Shared", Icons.Default.Share),
                                    Triple("Settings", "Settings", Icons.Default.Settings)
                                )
                                navItems.forEach { (tabKey, label, icon) ->
                                    NavigationBarItem(
                                        selected = currentBottomTab == tabKey,
                                        onClick = { currentBottomTab = tabKey },
                                        label = { Text(label, color = Color(0xFF1F1A18), fontSize = 11.sp, fontWeight = if (currentBottomTab == tabKey) FontWeight.Bold else FontWeight.Medium) },
                                        icon = { Icon(icon, contentDescription = null, tint = if (currentBottomTab == tabKey) Color(0xFF8F4C38) else Color(0xFF514441)) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color(0xFF8F4C38),
                                            indicatorColor = Color(0xFFFFDBD1) // active tab background pill
                                        )
                                    )
                                }
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (activeTaskIdForDetails == null) {
                        FloatingActionButton(
                            onClick = { showQuickAddDialog = true },
                            containerColor = Color(0xFFFFDBCC),
                            contentColor = Color(0xFF8F4C38),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("fab_add_task")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Quick Add Task")
                        }
                    }
                },
                containerColor = Color(0xFFFDF8F6)
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (activeTaskIdForDetails != null) {
                        TaskDetailPage(
                            taskId = activeTaskIdForDetails!!,
                            todoViewModel = todoViewModel,
                            onBack = { activeTaskIdForDetails = null }
                        )
                    } else {
                        when (currentBottomTab) {
                            "Tasks" -> TasksTabScreen(
                                viewModel = todoViewModel,
                                onSelectTask = { activeTaskIdForDetails = it }
                            )
                            "Calendar" -> CalendarTabScreen(
                                viewModel = todoViewModel,
                                onSelectTask = { activeTaskIdForDetails = it }
                            )
                            "Search" -> SearchTabScreen(
                                viewModel = todoViewModel,
                                onSelectTask = { activeTaskIdForDetails = it }
                            )
                            "Settings" -> SettingsTabScreen(
                                todoViewModel = todoViewModel,
                                authViewModel = authViewModel
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // QUICK ADD MODAL DIALOG (NLP + SPEECH + EXPAND BUTTON)
        // ==========================================
        if (showQuickAddDialog) {
            QuickAddTaskDialog(
                viewModel = todoViewModel,
                onDismiss = { showQuickAddDialog = false }
            )
        }

        // ==========================================
        // ADD NEW PROJECT / TAG MODALS
        // ==========================================
        if (showAddProjectDialog) {
            var name by remember { mutableStateOf("") }
            val colors = listOf(0xFFF44336.toInt(), 0xFF2196F3.toInt(), 0xFF4CAF50.toInt(), 0xFFFF9800.toInt(), 0xFFE91E63.toInt())
            var selectedColor by remember { mutableStateOf(colors[0]) }

            Dialog(onDismissRequest = { showAddProjectDialog = false }) {
                Surface(
                    color = Color(0xFF1E1D2D),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("New Project", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Project Name", color = Color.White.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Color Label", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            colors.forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(c), shape = CircleShape)
                                        .border(if (selectedColor == c) 2.dp else 0.dp, Color.White, CircleShape)
                                        .clickable { selectedColor = c }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAddProjectDialog = false }) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(onClick = {
                                if (name.isNotBlank()) todoViewModel.addProject(name, selectedColor)
                                showAddProjectDialog = false
                            }) { Text("Create") }
                        }
                    }
                }
            }
        }

        if (showAddTagDialog) {
            var name by remember { mutableStateOf("") }
            val colors = listOf(0xFFE91E63.toInt(), 0xFF9C27B0.toInt(), 0xFF3F51B5.toInt(), 0xFF00BCD4.toInt(), 0xFFFFEB3B.toInt())
            var selectedColor by remember { mutableStateOf(colors[0]) }

            Dialog(onDismissRequest = { showAddTagDialog = false }) {
                Surface(
                    color = Color(0xFF1E1D2D),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("New Tab Label", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Label Title", color = Color.White.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Tag Tint", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            colors.forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(c), shape = CircleShape)
                                        .border(if (selectedColor == c) 2.dp else 0.dp, Color.White, CircleShape)
                                        .clickable { selectedColor = c }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAddTagDialog = false }) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(onClick = {
                                if (name.isNotBlank()) todoViewModel.addTag(name, selectedColor)
                                showAddTagDialog = false
                            }) { Text("Add Tag") }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. TASKS LIST VIEW SCREEN (WITH SORT FILTER OVERLAYS)
// ==========================================
@Composable
fun TasksTabScreen(
    viewModel: TodoViewModel,
    onSelectTask: (Long) -> Unit
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Simple search feedback and filters bar
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${tasks.size} Available Tasks",
                color = Color(0xFF514441),
                fontSize = 13.sp
            )

            Box {
                Button(
                    onClick = { showSortMenu = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1F1A18)),
                    border = BorderStroke(1.dp, Color(0xFFF5DED8)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sort: $sortOrder", fontSize = 12.sp)
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    listOf("Date", "Priority", "Alpha", "Custom").forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt, color = Color(0xFF1F1A18)) },
                            onClick = {
                                viewModel.setSortOrder(opt)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAddCheck,
                        contentDescription = "Inbox empty",
                        tint = Color(0xFF85736E).copy(alpha = 0.3f),
                        modifier = Modifier.size(112.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Everything matches! All tasks checked.",
                        color = Color(0xFF514441),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Use '+' to add something fresh.",
                        color = Color(0xFF85736E),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskListItemCard(
                        task = task,
                        onCheckToggle = { viewModel.toggleTaskCompletion(task) },
                        onClick = { onSelectTask(task.id) }
                    )
                }
            }
        }
    }
}

// ==========================================
// TASK INDIVIDUAL LIST CARD ITEM
// ==========================================
@Composable
fun TaskListItemCard(
    task: Task,
    onCheckToggle: () -> Unit,
    onClick: () -> Unit
) {
    val borderTint = when (task.priority) {
        1 -> Color(0xFFD9411E)
        else -> Color(0xFF85736E)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_card_${task.id}")
            .clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(24.dp), // rounded-3xl
        border = BorderStroke(1.dp, Color(0xFFF5DED8))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            IconButton(
                onClick = onCheckToggle,
                modifier = Modifier.size(32.dp).padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Check task status",
                    tint = borderTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(
                        text = task.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F1A18),
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    if (task.priority == 1) {
                         Text(
                             text = "P1",
                             fontSize = 10.sp,
                             fontWeight = FontWeight.Bold,
                             color = Color(0xFFD9411E),
                             modifier = Modifier.background(Color(0xFFFFDAD4), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                         )
                    }
                }
                
                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        fontSize = 13.sp,
                        color = Color(0xFF514441),
                        maxLines = 2
                    )
                }

                // Metadata markers (due dates, recurrence)
                if (task.dueDate != null || task.recurrence != "NONE") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        task.dueDate?.let { due ->
                            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color(0xFFF5DED8), RoundedCornerShape(12.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF514441), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatter.format(Date(due)) + (task.dueTime?.let { " at $it" } ?: ""),
                                    fontSize = 11.sp,
                                    color = Color(0xFF514441)
                                )
                            }
                        }

                        if (task.recurrence != "NONE") {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color(0xFFF5DED8), RoundedCornerShape(12.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF8F4C38), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(task.recurrence, fontSize = 11.sp, color = Color(0xFF8F4C38))
                            }
                        }
                    }
                }
            }

            // Arrow Indicator
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF85736E).copy(alpha = 0.5f))
        }
    }
}

// ==========================================
// 6. CALENDAR TAB VIEW (ROLLING DAYS)
// ==========================================
@Composable
fun CalendarTabScreen(
    viewModel: TodoViewModel,
    onSelectTask: (Long) -> Unit
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()

    val upcomingTasks = tasks.filter { it.dueDate != null && !it.isCompleted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Weekly Schedule Flow", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F1A18))
        Spacer(modifier = Modifier.height(14.dp))

        if (upcomingTasks.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EventBusy, contentDescription = null, tint = Color(0xFF85736E).copy(alpha = 0.3f), modifier = Modifier.size(96.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No future schedules registered.", color = Color(0xFF514441))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(upcomingTasks) { task ->
                    TaskListItemCard(task = task, onCheckToggle = { viewModel.toggleTaskCompletion(task) }, onClick = { onSelectTask(task.id) })
                }
            }
        }
    }
}

// ==========================================
// 7. GLOBAL SEARCH TAB SCREEN
// ==========================================
@Composable
fun SearchTabScreen(
    viewModel: TodoViewModel,
    onSelectTask: (Long) -> Unit
) {
    val search by viewModel.searchQuery.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = search,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search title, priority, notes...", color = Color(0xFF514441)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF514441)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_field"),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF1F1A18), unfocusedTextColor = Color(0xFF1F1A18)),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (search.isBlank()) {
            // Suggest tags keywords search
            Text("Suggested Keywords", fontSize = 12.sp, color = Color(0xFF514441))
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                val suggestions = listOf("Shop", "Meeting", "P1", "Groceries")
                suggestions.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFDBD1), RoundedCornerShape(8.dp))
                            .clickable { viewModel.setSearchQuery(tag) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(tag, color = Color(0xFF8F4C38), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(tasks) { task ->
                TaskListItemCard(task = task, onCheckToggle = { viewModel.toggleTaskCompletion(task) }, onClick = { onSelectTask(task.id) })
            }
        }
    }
}

// ==========================================
// 8. APPSETTINGS & EXPORT TAB SCREEN
// ==========================================
@Composable
fun SettingsTabScreen(
    todoViewModel: TodoViewModel,
    authViewModel: AuthViewModel
) {
    val settings by todoViewModel.appSettings.collectAsStateWithLifecycle()
    val userEmail by authViewModel.userEmail.collectAsStateWithLifecycle()
    val isGuestMode by authViewModel.isGuestMode.collectAsStateWithLifecycle()

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Account Section", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF514441))
        Spacer(modifier = Modifier.height(12.dp))

        Surface(color = Color.White, border = BorderStroke(1.dp, Color(0xFFF5DED8)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isGuestMode) "Logged in as Guest" else "Logged in as: $userEmail",
                    color = Color(0xFF1F1A18),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { authViewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD9411E))
                ) {
                    Text("Logout and Clear Profile", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Appearance Options", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF514441))
        Spacer(modifier = Modifier.height(12.dp))

        Surface(color = Color.White, border = BorderStroke(1.dp, Color(0xFFF5DED8)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dark Theme Default", color = Color(0xFF1F1A18))
                    Switch(
                        checked = settings.isDarkMode ?: true,
                        onCheckedChange = { todoViewModel.updateThemeMode(it) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Custom Accent Style", fontSize = 14.sp, color = Color(0xFF514441))
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val colors = listOf("#6750A4", "#FF5722", "#4CAF50", "#2196F3", "#E91E63")
                    colors.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(android.graphics.Color.parseColor(hex)), shape = CircleShape)
                                .border(if (settings.primaryColorHex == hex) 2.dp else 0.dp, Color(0xFF1F1A18), CircleShape)
                                .clickable { todoViewModel.updateAccentColor(hex) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Backup & Database Sync", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF514441))
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val json = todoViewModel.exportDataAsJson()
                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clipData = android.content.ClipData.newPlainText("Backup JSON", json)
                clipboardManager.setPrimaryClip(clipData)
                Toast.makeText(context, "Exported JSON copied to Clipboard!", Toast.LENGTH_LONG).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF))
        ) {
            Icon(Icons.Default.CloudDownload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Export Data Backup as JSON")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("About & Support", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF514441))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Version 1.0.0 (May 2026)\nBuilt on Android Kotlin Compose Core.\nOffline & Online Intelligent NLP Systems.", fontSize = 12.sp, color = Color(0xFF85736E))
    }
}

// ==========================================
// 9. QUICK ADD DIALOG COMPOSE WIDGET (SPEECH PARSING + EXPAND BTN)
// ==========================================
@Composable
fun QuickAddTaskDialog(
    viewModel: TodoViewModel,
    onDismiss: () -> Unit
) {
    var taskText by remember { mutableStateOf("") }
    var nlpEnabled by remember { mutableStateOf(true) }
    var isSimulatingVoice by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutine = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Quick Task Record",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F1A18)
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = taskText,
                    onValueChange = { taskText = it },
                    placeholder = { Text("E.g., Buy bread tomorrow at 6 PM P1", color = Color(0xFF514441)) },
                    modifier = Modifier.fillMaxWidth().testTag("quick_add_text_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF1F1A18), unfocusedTextColor = Color(0xFF1F1A18)),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                // NLP Toggle switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto Date NLP Parser", fontSize = 13.sp, color = Color(0xFF1F1A18))
                    Switch(checked = nlpEnabled, onCheckedChange = { nlpEnabled = it })
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Voice Dictation Mock
                Button(
                    onClick = {
                        isSimulatingVoice = true
                        coroutine.launch {
                            delay(1000)
                            taskText = "Meet client next week at Noon P1"
                            isSimulatingVoice = false
                            Toast.makeText(context, "Voice input simulated!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1F1A18)),
                    border = BorderStroke(1.dp, Color(0xFFF5DED8))
                ) {
                    Icon(
                        imageVector = if (isSimulatingVoice) Icons.Default.VolumeUp else Icons.Default.Mic,
                        contentDescription = "Voice dictation recording",
                        tint = if (isSimulatingVoice) Color.Red else Color(0xFF1F1A18)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSimulatingVoice) "Recording voice..." else "Tap to Dictate (Voice)")
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF514441))) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (taskText.isNotBlank()) {
                                if (nlpEnabled) {
                                    viewModel.addTaskWithNlp(taskText)
                                } else {
                                    viewModel.addTask(title = taskText)
                                }
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8F4C38))
                    ) {
                        Text("Add Task", color = Color.White)
                    }
                }
            }
        }
    }
}

// ==========================================
// 10. TASK DETAILS SCREEN & RICH COMPOSE ELEMENTS (SUBTASKS + CHAT + GEOFENCING)
// ==========================================
@Composable
fun TaskDetailPage(
    taskId: Long,
    todoViewModel: TodoViewModel,
    onBack: () -> Unit
) {
    var task by remember { mutableStateOf<Task?>(null) }
    val comments by todoViewModel.getComments(taskId).collectAsStateWithLifecycle(emptyList())
    val subtasks by todoViewModel.getSubtasks(taskId).collectAsStateWithLifecycle(emptyList())

    val coroutine = rememberCoroutineScope()
    val context = LocalContext.current

    var commentText by remember { mutableStateOf("") }
    var subtaskText by remember { mutableStateOf("") }

    // Mock Location triggers coordinates setup
    var geofencingEnabled by remember { mutableStateOf(false) }
    var latitude by remember { mutableStateOf("37.4220") }
    var longitude by remember { mutableStateOf("-122.0841") }
    var locationName by remember { mutableStateOf("Googleplex HQ") }

    // Mock Attachments
    var attachmentsList by remember { mutableStateOf(listOf("work_specs.pdf", "receipt_scan.png")) }

    LaunchedEffect(taskId) {
        task = todoViewModel.getTaskById(taskId)
    }

    if (task == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentTask = task!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Upper row info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Details Editor", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
            IconButton(onClick = {
                todoViewModel.deleteTask(currentTask)
                onBack()
            }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Task", tint = Color.Red)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title and notes
        OutlinedTextField(
            value = currentTask.title,
            onValueChange = {
                val updated = currentTask.copy(title = it)
                task = updated
                todoViewModel.updateTaskDetails(updated)
            },
            label = { Text("Task Summary Title") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = currentTask.description,
            onValueChange = {
                val updated = currentTask.copy(description = it)
                task = updated
                todoViewModel.updateTaskDetails(updated)
            },
            label = { Text("Rich Formatting Notes") },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Priorities selectors
        Text("Priority Flag", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val priorities = listOf(
                1 to "P1 (Red)",
                2 to "P2 (Orange)",
                3 to "P3 (Yellow)",
                4 to "P4 (None)"
            )
            priorities.forEach { (p, label) ->
                val active = currentTask.priority == p
                val bg = when (p) {
                    1 -> Color.Red
                    2 -> Color(0xFFFF5722)
                    3 -> Color(0xFFFFEB3B)
                    else -> Color.Gray
                }
                Box(
                    modifier = Modifier
                        .background(if (active) bg else bg.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(1.dp, bg, RoundedCornerShape(8.dp))
                        .clickable {
                            val updated = currentTask.copy(priority = p)
                            task = updated
                            todoViewModel.updateTaskDetails(updated)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(label, color = if (active) Color.Black else bg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recurrence repeats selectors
        Text("Recurrence Repeat Schedule", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val rules = listOf("NONE", "DAILY", "WEEKLY", "MONTHLY", "WEEKDAYS")
            rules.forEach { item ->
                val active = currentTask.recurrence == item
                Box(
                    modifier = Modifier
                        .background(if (active) Color(0xFF00E676).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .border(1.dp, if (active) Color(0xFF00E676) else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable {
                            val updated = currentTask.copy(recurrence = item)
                            task = updated
                            todoViewModel.updateTaskDetails(updated)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(item, color = if (active) Color(0xFF00E676) else Color.White, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Subtasks section checklists + nested progress bar!
        val completedCount = subtasks.filter { it.isCompleted }.size
        val progressPercent = if (subtasks.isNotEmpty()) (completedCount.toFloat() / subtasks.size.toFloat()) else 0f

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Subtasks Tracker Checklists", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text("${completedCount}/${subtasks.size} Items Completed", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progressPercent },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = Color(0xFF00E676),
            trackColor = Color.White.copy(alpha = 0.1f)
        )
        Spacer(modifier = Modifier.height(12.dp))

        subtasks.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = item.isCompleted,
                    onCheckedChange = { todoViewModel.toggleSubtaskCompletion(item) }
                )
                Text(
                    text = item.title,
                    color = if (item.isCompleted) Color.White.copy(alpha = 0.5f) else Color.White,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { todoViewModel.deleteSubtask(item) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = subtaskText,
                onValueChange = { subtaskText = it },
                placeholder = { Text("Add nested subtask info...") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (subtaskText.isNotBlank()) {
                    todoViewModel.addSubtask(currentTask.id, subtaskText)
                    subtaskText = ""
                }
            }) {
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Geofencing GPS Triggers section
        Text("Geofencing Location Alerts", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Spacer(modifier = Modifier.height(10.dp))
        Surface(color = Color(0xFF1E1D2D), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Trigger on coordinates arrival", color = Color.White)
                    Switch(checked = geofencingEnabled, onCheckedChange = { geofencingEnabled = it })
                }
                if (geofencingEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = locationName,
                        onValueChange = { locationName = it },
                        label = { Text("Alert Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = latitude,
                            onValueChange = { latitude = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = longitude,
                            onValueChange = { longitude = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Attachments Area (PDF/JPG Upload)
        Text("Photos & Documents Attachments", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            attachmentsList.forEach { file ->
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .clickable { Toast.makeText(context, "Opening $file...", Toast.LENGTH_SHORT).show() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(file, color = Color.White)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .background(Color(0xFF00B0FF).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .clickable {
                        attachmentsList = attachmentsList + "attachment_${System.currentTimeMillis() % 1000}.png"
                        Toast.makeText(context, "Uploaded attachment!", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("+ Upload", color = Color(0xFF00B0FF), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Localized Collaborative task chat discussion
        Text("Task Internal Team Comments", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Spacer(modifier = Modifier.height(10.dp))

        Surface(color = Color(0xFF1E1D2D), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (comments.isEmpty()) {
                    Text("No coordination remarks posted yet.", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
                } else {
                    comments.forEach { c ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(c.userName, fontWeight = FontWeight.Bold, color = Color(0xFF7C4DFF), fontSize = 12.sp)
                                val sdf = SimpleDateFormat("HH:mm", Locale.US)
                                Text(sdf.format(Date(c.timestamp)), color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                            }
                            Text(c.content, color = Color.White, fontSize = 13.sp)
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Leave team comment...") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                todoViewModel.addComment(currentTask.id, "Cubecraft", commentText)
                                commentText = ""
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send comments", tint = Color(0xFF7C4DFF))
                    }
                }
            }
        }
    }
}
