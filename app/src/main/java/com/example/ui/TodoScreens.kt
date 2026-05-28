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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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

    var splashFinished by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf("Splash") }

    LaunchedEffect(Unit) {
        delay(1800) // Beautiful cinematic intro timing
        splashFinished = true
    }

    LaunchedEffect(splashFinished, onboardingCompleted, isLoggedIn, isGuestMode) {
        if (splashFinished) {
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
                onSuccess = { 
                    val email = authViewModel.userEmail.value
                    if (email != null && !authViewModel.isGuestMode.value) {
                         todoViewModel.enableAutoSync(email)
                    }
                    currentScreen = "Main" 
                }
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
            .background(MaterialTheme.colorScheme.background),
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
                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "App Logo Checkmark",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(64.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "T O D O",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 6.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Simplify. Organize. Achieve.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
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
            accentColor = MaterialTheme.colorScheme.primary
        ),
        Slide(
            title = "Smart NLP Task Parsing",
            desc = "Simply type \"Buy milk tomorrow at 5 PM\" and watch the app auto-parse dates, times, and priority lists.",
            icon = Icons.Default.AutoAwesome,
            accentColor = MaterialTheme.colorScheme.primary
        ),
        Slide(
            title = "Deep Color-Coded Lists",
            desc = "Tag your tasks with customized labels, work boards, or grocery lists so everything coordinates beautifully.",
            icon = Icons.Default.Folder,
            accentColor = MaterialTheme.colorScheme.outline
        ),
        Slide(
            title = "Collaborate & Discuss",
            desc = "Share task boards with teammates and coordinate instantly inside dedicated local task comment feeds.",
            icon = Icons.AutoMirrored.Filled.Comment,
            accentColor = MaterialTheme.colorScheme.secondary
        )
    )

    var currentSlideIndex by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                    Text("Skip", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
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
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = slide.desc,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
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
                                .background(if (i == currentSlideIndex) slide.accentColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
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
                        color = MaterialTheme.colorScheme.onBackground
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
    var showGoogleAccountPicker by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                tint = MaterialTheme.colorScheme.primary,
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
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = when {
                    forgotPasswordMode -> "Enter email to receive a recovery link."
                    isRegisterState -> "Sign up to synchronize your tasks across devices."
                    else -> "Log in to check progress on your daily habits."
                },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Dynamic Inputs Fields
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_email_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            if (!forgotPasswordMode) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_password_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Warning and error handling alerts
            error?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            if (forgotPasswordSent) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                ) {
                    Text(
                        text = "Recovery email has been dispatched to $email!",
                        color = MaterialTheme.colorScheme.secondary,
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Forgot Password and Registration toggles
            if (!forgotPasswordMode) {
                TextButton(onClick = { forgotPasswordMode = true }) {
                    Text("Forgot Password?", color = MaterialTheme.colorScheme.primary)
                }
            } else {
                TextButton(onClick = {
                    forgotPasswordMode = false
                    viewModel.resetForgotPassword()
                }) {
                    Text("Back to log in", color = MaterialTheme.colorScheme.primary)
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
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    TextButton(onClick = { isRegisterState = !isRegisterState }) {
                        Text(
                            text = if (isRegisterState) "Sign In" else "Sign Up",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            // SSO Single Sign-On and Guest Check-ins
            Button(
                onClick = {
                    showGoogleAccountPicker = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("sso_google_button"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Google Icon",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Sign in with Google", color = MaterialTheme.colorScheme.onBackground)
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
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textDecoration = TextDecoration.Underline,
                    fontSize = 13.sp
                )
            }
        }

        // Animated Google Account Picker Overlay Bottom Sheet
        if (showGoogleAccountPicker) {
            var selectedEmailForLoading by remember { mutableStateOf<String?>(null) }
            
            // Dimmed background click overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    .clickable { 
                        if (selectedEmailForLoading == null) {
                            showGoogleAccountPicker = false 
                        }
                    }
            )

            // Bottom centered account list bottom sheet
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = false) {}, // absorb touch events
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = Color.White,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        if (selectedEmailForLoading != null) {
                            // Authentic simulated credential sign in with progress loader
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Signing in to To-Do App...", 
                                    color = Color.Black, 
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    selectedEmailForLoading!!, 
                                    color = Color.DarkGray, 
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "G",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp,
                                    color = Color.DarkGray
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Choose an account",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "to continue to To-Do App",
                                fontSize = 14.sp,
                                color = Color.DarkGray,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            val accounts = listOf(
                                Triple("Cubecraft Developer", "cubecraft627@gmail.com", MaterialTheme.colorScheme.primary),
                                Triple("Cubecraft Secondary", "cubecraft.dev@gmail.com", MaterialTheme.colorScheme.secondary),
                                Triple("Guest Account", "guest.user.todo@gmail.com", Color.Gray)
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                accounts.forEach { (name, emailStr, avatarColor) ->
                                    Surface(
                                        onClick = {
                                            selectedEmailForLoading = emailStr
                                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                viewModel.loginWithGoogle(emailStr, name)
                                                onSuccess()
                                                showGoogleAccountPicker = false
                                                selectedEmailForLoading = null
                                            }, 1200)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color.LightGray)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(avatarColor, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = name.first().toString(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(
                                                    text = name,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.Black,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = emailStr,
                                                    color = Color.DarkGray,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "To continue, Google will share your name, email address, language preference and profile picture with To-Do App. Before using this app, you can review its privacy policy and terms of service.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
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
    var currentBottomTab by remember { mutableStateOf("Tasks") } // "Tasks", "Calendar", "Settings"
    var activeTaskIdForDetails by remember { mutableStateOf<Long?>(null) } // null = show lists, id = show detail edit view
    var calendarSelectedDateMs by remember { mutableStateOf(System.currentTimeMillis()) }

    val projects by todoViewModel.projects.collectAsStateWithLifecycle()
    val tags by todoViewModel.tags.collectAsStateWithLifecycle()
    val selectedProjectId by todoViewModel.selectedProjectId.collectAsStateWithLifecycle()
    val selectedTagId by todoViewModel.selectedTagId.collectAsStateWithLifecycle()
    val smartView by todoViewModel.smartView.collectAsStateWithLifecycle()

    val userEmail by authViewModel.userEmail.collectAsStateWithLifecycle()
    val isGuestMode by authViewModel.isGuestMode.collectAsStateWithLifecycle()

    // Modals
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var showScheduleWorkflow by remember { mutableStateOf(false) }
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
                    drawerContainerColor = MaterialTheme.colorScheme.background
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
                                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isGuestMode) "G" else userEmail?.take(1)?.uppercase() ?: "U",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isGuestMode) "Guest Profile" else userEmail ?: "Premium User",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (isGuestMode) "Offline sync" else "Cloud Sync Active",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

                        // Smart Views Navigation lists
                        Text(
                            "Smart Filters",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                label = { Text(label, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                                selected = active,
                                onClick = {
                                    todoViewModel.setSmartView(viewKey)
                                    currentBottomTab = "Tasks"
                                    coroutineScope.launch { drawerState.close() }
                                },
                                icon = { Icon(icon, contentDescription = null, tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                                modifier = Modifier.padding(vertical = 2.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(onClick = { showAddProjectDialog = true }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Add Project", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
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
                                        .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
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
                                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { todoViewModel.deleteProject(project) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(onClick = { showAddTagDialog = true }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Add Tag", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
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
                                        .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
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
                                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { todoViewModel.deleteTag(tag) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
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
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                                }
                            },
                            colors = TopAppBarDefaults.largeTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }
                },
                bottomBar = {
                    if (activeTaskIdForDetails == null) {
                        Surface(
                            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
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
                                    Triple("Settings", "Settings", Icons.Default.Settings)
                                )
                                navItems.forEach { (tabKey, label, icon) ->
                                    NavigationBarItem(
                                        selected = currentBottomTab == tabKey,
                                        onClick = { currentBottomTab = tabKey },
                                        label = { Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = if (currentBottomTab == tabKey) FontWeight.Bold else FontWeight.Medium) },
                                        icon = { Icon(icon, contentDescription = null, tint = if (currentBottomTab == tabKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer // active tab background pill
                                        )
                                    )
                                }
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (activeTaskIdForDetails == null) {
                        if (currentBottomTab == "Calendar") {
                            FloatingActionButton(
                                onClick = { showScheduleWorkflow = true },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.testTag("fab_schedule_task")
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Schedule Task")
                            }
                        } else {
                            FloatingActionButton(
                                onClick = { showQuickAddDialog = true },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.testTag("fab_add_task")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Quick Add Task")
                            }
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.background
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
                                selectedDateMs = calendarSelectedDateMs,
                                onDateSelected = { calendarSelectedDateMs = it },
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
                onDismiss = { showQuickAddDialog = false },
                onTaskCreated = { newId ->
                    activeTaskIdForDetails = newId
                    showQuickAddDialog = false
                }
            )
        }

        // ==========================================
        // SCHEDULE TASK DIALOG FOR CALENDAR TAB
        // ==========================================
        if (showScheduleWorkflow) {
            ScheduleTaskDialog(
                initialDate = calendarSelectedDateMs,
                viewModel = todoViewModel,
                onDismiss = { showScheduleWorkflow = false },
                onTaskCreated = { newId ->
                    activeTaskIdForDetails = newId
                    showScheduleWorkflow = false
                }
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
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("New Project", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Project Name", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Color Label", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            colors.forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(c), shape = CircleShape)
                                        .border(if (selectedColor == c) 2.dp else 0.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
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
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("New Tab Label", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Label Title", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Tag Tint", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            colors.forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(c), shape = CircleShape)
                                        .border(if (selectedColor == c) 2.dp else 0.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )

            Box {
                Button(
                    onClick = { showSortMenu = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
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
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    listOf("Date", "Priority", "Alpha", "Custom").forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt, color = MaterialTheme.colorScheme.onSurface) },
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
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAddCheck,
                        contentDescription = "Inbox empty",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        modifier = Modifier.size(112.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Everything matches! All tasks checked.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Use '+' to add something fresh.",
                        color = MaterialTheme.colorScheme.outline,
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
        1 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_card_${task.id}")
            .clickable(onClick = onClick),
        color = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        shadowElevation = if (task.isCompleted) 0.dp else 2.dp,
        shape = RoundedCornerShape(24.dp), // rounded-3xl
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .alpha(if (task.isCompleted) 0.7f else 1.0f),
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
                        color = MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    if (task.priority == 1) {
                         Text(
                             text = "P1",
                             fontSize = 10.sp,
                             fontWeight = FontWeight.Bold,
                             color = MaterialTheme.colorScheme.error,
                             modifier = Modifier.background(MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                         )
                    }
                }
                
                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatter.format(Date(due)) + (task.dueTime?.let { " at $it" } ?: ""),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (task.recurrence != "NONE") {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(task.recurrence, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // Arrow Indicator
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        }
    }
}

// ==========================================
// 6. CALENDAR TAB VIEW (ROLLING DAYS)
// ==========================================
@Composable
fun CalendarTabScreen(
    viewModel: TodoViewModel,
    selectedDateMs: Long,
    onDateSelected: (Long) -> Unit,
    onSelectTask: (Long) -> Unit
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()

    val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val selectedYmd = sdf.format(Date(selectedDateMs))

    val scheduledTasksForDay = tasks.filter { task ->
        task.dueDate != null && !task.isCompleted && sdf.format(Date(task.dueDate)) == selectedYmd
    }

    val sdfDay = SimpleDateFormat("E", Locale.getDefault())
    val sdfDate = SimpleDateFormat("dd", Locale.getDefault())
    val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthLabel = sdfMonth.format(Date(selectedDateMs))

    val datesList = remember {
        val list = mutableListOf<Long>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -3) // Start 3 days before today for rolling context
        for (i in 0 until 14) {
            list.add(cal.timeInMillis)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Month / Year Calendar Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = monthLabel,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Rolling 14-days visual Calendar Strip (horizontal)
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(datesList) { dateMs ->
                val isSelected = sdf.format(Date(dateMs)) == selectedYmd
                val dayStr = sdfDay.format(Date(dateMs))
                val dateStr = sdfDate.format(Date(dateMs))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .width(60.dp)
                        .clickable { onDateSelected(dateMs) }
                        .testTag("calendar_day_strip_${dateStr}")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dayStr.uppercase(Locale.getDefault()),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = dateStr,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Active Date Scheduled Tasks Section Title
        Text(
            text = "Scheduled Tasks",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (scheduledTasksForDay.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(96.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "All clear for today!",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No schedules or events planned.",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(scheduledTasksForDay) { task ->
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
// 7. SCHEDULE TASK FLOW MODAL DIALOG
// ==========================================
@Composable
fun ScheduleTaskDialog(
    initialDate: Long,
    viewModel: TodoViewModel,
    onDismiss: () -> Unit,
    onTaskCreated: (Long) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedDateMs by remember { mutableStateOf(initialDate) }
    var selectedTime by remember { mutableStateOf("12:00") }
    var priority by remember { mutableStateOf(4) }

    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDateMs }
    val dateFormated = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDateMs))

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val newCalendar = Calendar.getInstance()
            newCalendar.set(year, month, dayOfMonth)
            selectedDateMs = newCalendar.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val timePickerDialog = android.app.TimePickerDialog(
        context,
        { _, hour, minute ->
            selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        },
        12,
        0,
        true
    )

    val coroutine = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Schedule Task", 
                        fontSize = 20.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text("Task Title", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("What are you planning?", color = MaterialTheme.colorScheme.outline) },
                    modifier = Modifier.fillMaxWidth().testTag("schedule_title_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface, 
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Date & Time Schedule", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Date selector custom card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { datePickerDialog.show() }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(dateFormated, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Time selector custom card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { timePickerDialog.show() }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(selectedTime, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Notes", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Add scheduling notes...", color = MaterialTheme.colorScheme.outline) },
                    modifier = Modifier.fillMaxWidth().testTag("schedule_notes_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground, 
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            coroutine.launch {
                                val newId = viewModel.addTaskAndGetId(
                                    title = title,
                                    description = notes,
                                    dueDate = selectedDateMs,
                                    dueTime = selectedTime,
                                    priority = priority
                                )
                                onTaskCreated(newId)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("schedule_submit_btn"),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = title.isNotBlank()
                ) {
                    Text("Create & Focus Schedule", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
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
    val userDisplayName by authViewModel.userDisplayName.collectAsStateWithLifecycle()
    val isGuestMode by authViewModel.isGuestMode.collectAsStateWithLifecycle()
    val tasks by todoViewModel.tasks.collectAsStateWithLifecycle()
    val syncStatus by todoViewModel.syncStatus.collectAsStateWithLifecycle()

    val completedTasks = tasks.count { it.isCompleted }
    val todayMs = System.currentTimeMillis()
    val sdf = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
    val todayYmd = sdf.format(java.util.Date(todayMs))
    val remainingToday = tasks.count {
        it.dueDate != null && !it.isCompleted && sdf.format(java.util.Date(it.dueDate)) == todayYmd
    }

    val context = LocalContext.current

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                val json = todoViewModel.exportDataAsJson()
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
                Toast.makeText(context, "Backup saved physically to device downloads storage!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save backup: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Account Section", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))

        Surface(color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isGuestMode) "Logged in as Guest" else "Logged in as: ${userDisplayName ?: "User"}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                if (!isGuestMode) {
                    Text(
                        text = userEmail ?: "",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { authViewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Logout and Clear Profile", color = MaterialTheme.colorScheme.onError)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Task Statistics", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))

        Surface(color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                TaskStatisticsGraph(completed = completedTasks, remaining = remainingToday)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Completed",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "$completedTasks",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Remaining",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "$remainingToday",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Appearance Options", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))

        Surface(color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dark Mode", color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = settings.isDarkMode ?: isSystemInDarkTheme(),
                        onCheckedChange = { todoViewModel.updateThemeMode(it) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Custom Accent Style", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val colors = listOf("#6750A4", "#FF5722", "#4CAF50", "#2196F3", "#E91E63")
                    colors.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(android.graphics.Color.parseColor(hex)), shape = CircleShape)
                                .border(if (settings.primaryColorHex == hex) 2.dp else 0.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                .clickable { todoViewModel.updateAccentColor(hex) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

    Text("Backup & Database Sync (Firebase Realtime DB)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (userEmail == null) {
                        Toast.makeText(context, "Log in first!", Toast.LENGTH_SHORT).show()
                    } else {
                        todoViewModel.backupToCloud(userEmail!!)
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Backup to Cloud", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    if (userEmail == null) {
                        Toast.makeText(context, "Log in first!", Toast.LENGTH_SHORT).show()
                    } else {
                        todoViewModel.restoreFromCloud(userEmail!!)
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Restore from Cloud", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Status: $syncStatus", 
            color = MaterialTheme.colorScheme.onSurfaceVariant, 
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                exportLauncher.launch("backup.json")
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Export Data Local File")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("About & Support", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Version 1.0.0 (May 2026)\nBuilt on Android Kotlin Compose Core.\nOffline & Online Intelligent NLP Systems.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
    }
}

// ==========================================
// 9. QUICK ADD DIALOG COMPOSE WIDGET (SPEECH PARSING + EXPAND BTN)
// ==========================================
@Composable
fun QuickAddTaskDialog(
    viewModel: TodoViewModel,
    onDismiss: () -> Unit,
    onTaskCreated: (Long) -> Unit
) {
    var taskText by remember { mutableStateOf("") }
    var nlpEnabled by remember { mutableStateOf(true) }
    var isSimulatingVoice by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutine = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Task Record",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Dialog", tint = MaterialTheme.colorScheme.outline)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Modern Search-Bar-Like Soft Text Area
                OutlinedTextField(
                    value = taskText,
                    onValueChange = { taskText = it },
                    placeholder = { Text("E.g., Buy bread tomorrow at 6 PM P1", color = MaterialTheme.colorScheme.outline, fontSize = 14.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_add_text_input"),
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    maxLines = 3,
                    singleLine = false
                )

                Spacer(modifier = Modifier.height(16.dp))

                // NLP Toggle switch Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto Date NLP Parser", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text("Extract date, time and flags automatically", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Switch(
                        checked = nlpEnabled, 
                        onCheckedChange = { nlpEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onBackground,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Emphasized Voice Dictation Hub (Prominent visual circular trigger)
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                isSimulatingVoice = true
                                coroutine.launch {
                                    delay(1000)
                                    taskText = "Meet client next week at Noon P1"
                                    isSimulatingVoice = false
                                    Toast.makeText(context, "Voice input simulated!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    if (isSimulatingVoice) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                )
                                .testTag("voice_dictation_btn")
                        ) {
                            Icon(
                                imageVector = if (isSimulatingVoice) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.Mic,
                                contentDescription = "Voice dictation recording",
                                tint = if (isSimulatingVoice) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isSimulatingVoice) "Recording voice..." else "Tap to Dictate (Voice)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSimulatingVoice) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Wide, Pill-shaped prominent CTA button spanning full width
                Button(
                    onClick = {
                        if (taskText.isNotBlank()) {
                            coroutine.launch {
                                val newId = if (nlpEnabled) {
                                    viewModel.addTaskWithNlpAndGetId(taskText)
                                } else {
                                    viewModel.addTaskAndGetId(title = taskText)
                                }
                                onTaskCreated(newId)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("quick_add_submit_btn"),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.outline
                    ),
                    enabled = taskText.isNotBlank()
                ) {
                    Text(
                        text = "Add Task",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        letterSpacing = 0.5.sp
                    )
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
    val commentsFlow = remember(taskId) { todoViewModel.getComments(taskId) }
    val comments by commentsFlow.collectAsStateWithLifecycle(emptyList())

    val subtasksFlow = remember(taskId) { todoViewModel.getSubtasks(taskId) }
    val subtasks by subtasksFlow.collectAsStateWithLifecycle(emptyList())

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
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val currentTask = task!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Upper row info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Details Editor", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp)
            IconButton(onClick = {
                todoViewModel.deleteTask(currentTask)
                onBack()
            }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Task", tint = MaterialTheme.colorScheme.error)
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
            modifier = Modifier.fillMaxWidth().testTag("edit_task_title"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp)
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
            modifier = Modifier.fillMaxWidth().height(120.dp).testTag("edit_task_notes"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Priorities selectors
        Text("Priority Flag", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val priorities = listOf(
                1 to "P1",
                2 to "P2",
                3 to "P3",
                4 to "None"
            )
            priorities.forEach { (p, label) ->
                val active = currentTask.priority == p
                val semanticColor = when (p) {
                    1 -> Color(0xFFE53935)
                    2 -> Color(0xFFFF9800)
                    3 -> Color(0xFF2196F3)
                    else -> Color(0xFF9E9E9E)
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        val updated = currentTask.copy(priority = p)
                        task = updated
                        todoViewModel.updateTaskDetails(updated)
                    },
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        containerColor = if (active) semanticColor.copy(alpha = 0.15f) else Color.Transparent,
                        contentColor = semanticColor
                    ),
                    border = BorderStroke(if (active) 2.dp else 1.dp, semanticColor),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recurrence repeats selectors
        Text("Recurrence Repeat Schedule", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val rules = listOf("NONE", "DAILY", "WEEKLY", "MONTHLY", "WEEKDAYS")
            rules.forEach { item ->
                val active = currentTask.recurrence == item
                val label = when (item) {
                    "NONE" -> "None"
                    "DAILY" -> "Daily"
                    "WEEKLY" -> "Weekly"
                    "MONTHLY" -> "Monthly"
                    "WEEKDAYS" -> "Weekdays"
                    else -> item
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        val updated = currentTask.copy(recurrence = item)
                        task = updated
                        todoViewModel.updateTaskDetails(updated)
                    },
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subtasks section checklists + nested progress bar!
        val completedCount = subtasks.filter { it.isCompleted }.size
        val progressPercent = if (subtasks.isNotEmpty()) (completedCount.toFloat() / subtasks.size.toFloat()) else 0f

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Subtasks Tracker", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            Text("${completedCount}/${subtasks.size} Items Completed", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progressPercent },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
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
                    color = if (item.isCompleted) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { todoViewModel.deleteSubtask(item) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = subtaskText,
                onValueChange = { subtaskText = it },
                placeholder = { Text("Add nested subtask info...", color = MaterialTheme.colorScheme.outline) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (subtaskText.isNotBlank()) {
                        todoViewModel.addSubtask(currentTask.id, subtaskText)
                        subtaskText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Add", color = MaterialTheme.colorScheme.onBackground)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Geofencing GPS Triggers section
        Text("Geofencing Location Alerts", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(10.dp))
        Surface(color = MaterialTheme.colorScheme.primaryContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Trigger on coordinates arrival", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
                    Switch(checked = geofencingEnabled, onCheckedChange = { geofencingEnabled = it })
                }
                if (geofencingEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = locationName,
                        onValueChange = { locationName = it },
                        label = { Text("Alert Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = latitude,
                            onValueChange = { latitude = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        OutlinedTextField(
                            value = longitude,
                            onValueChange = { longitude = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Attachments Area (PDF/JPG Upload)
        Text("Photos & Documents Attachments", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            attachmentsList.forEach { file ->
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable { Toast.makeText(context, "Opening $file...", Toast.LENGTH_SHORT).show() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(file, color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .clickable {
                        attachmentsList = attachmentsList + "attachment_${System.currentTimeMillis() % 1000}.png"
                        Toast.makeText(context, "Uploaded attachment!", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("+ Upload", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Localized Collaborative task chat discussion
        Text("Task Internal Team Comments", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(10.dp))

        Surface(color = MaterialTheme.colorScheme.primaryContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (comments.isEmpty()) {
                    Text("No coordination remarks posted yet.", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                } else {
                    comments.forEach { c ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(c.userName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
                                Text(sdf.format(java.util.Date(c.timestamp)), color = MaterialTheme.colorScheme.outline, fontSize = 10.sp)
                            }
                            Text(c.content, color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp)
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Leave team comment...", color = MaterialTheme.colorScheme.outline) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
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
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send comments", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

    @Composable
    fun TaskStatisticsGraph(completed: Int, remaining: Int) {
        val total = completed + remaining
        val percentage = if (total > 0) (completed.toFloat() / total * 100).toInt() else 0
        val progress = if (total > 0) completed.toFloat() / total.toFloat() else 0f
        
        val primaryColor = MaterialTheme.colorScheme.primary
        val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    
        Box(
            modifier = Modifier
                .size(160.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 16.dp.toPx()
                val arcSize = kotlin.math.max(0f, size.minDimension - strokeWidth)
                if (arcSize <= 0f) return@Canvas
                
                val topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2)
                
                // Draw background (remaining) ring
                drawArc(
                    color = surfaceVariantColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
                
                // Draw progress (completed) ring
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = if (total > 0) (completed.toFloat() / total) * 360f else 0f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${percentage}%",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
                Text(
                    text = if (total == completed && total > 0) "Done!" else "Completed",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
    }
}
