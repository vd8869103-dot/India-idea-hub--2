package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.R
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import java.util.Locale

// Screen Enumeration to enable fluid and 100% stable routing
enum class Screen {
    Welcome,
    Login,
    SignUp,
    OtpVerification,
    ForgotPassword,
    Home,
    IdeaDetail,
    PostIdea,
    AiAssistant,
    Profile,
    Meetings,
    Notifications,
    AdminPanel
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationContainer(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val isOtpSent by viewModel.isOtpSent.collectAsStateWithLifecycle()

    // Screen navigation stack
    val screenBackstack = remember { mutableStateListOf(Screen.Welcome) }
    val currentScreen = screenBackstack.lastOrNull() ?: Screen.Welcome

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State for creating new meeting
    var showMeetingDialog by remember { mutableStateOf(false) }

    // Intercept system back button to safely navigate our custom state stack
    BackHandler(enabled = screenBackstack.size > 1) {
        screenBackstack.removeLast()
    }

    // Function to navigate safely
    val navigateTo: (Screen) -> Unit = { screen ->
        if (screen == Screen.Welcome || screen == Screen.Home) {
            // Clear backstack and set home/welcome as root
            screenBackstack.clear()
            screenBackstack.add(screen)
        } else {
            screenBackstack.add(screen)
        }
    }

    val goBack: () -> Unit = {
        if (screenBackstack.size > 1) {
            screenBackstack.removeLast()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            // Show bottom bar only when logged in and on a main dashboard screen
            if (currentUser != null && currentScreen in listOf(Screen.Home, Screen.AiAssistant, Screen.PostIdea, Screen.Meetings, Screen.Profile)) {
                BottomNavigationBar(
                    currentScreen = currentScreen,
                    onNavigate = navigateTo,
                    isAdmin = currentUser?.role == "ADMIN"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) togetherWith
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    Screen.Welcome -> WelcomeScreen(onGetStarted = {
                        if (currentUser != null) navigateTo(Screen.Home) else navigateTo(Screen.Login)
                    })
                    Screen.Login -> LoginScreen(
                        viewModel = viewModel,
                        onLoginSuccess = { navigateTo(Screen.Home) },
                        onNavigateToSignUp = { navigateTo(Screen.SignUp) },
                        onNavigateToForgotPassword = { navigateTo(Screen.ForgotPassword) }
                    )
                    Screen.SignUp -> SignUpScreen(
                        viewModel = viewModel,
                        onOtpRequired = { navigateTo(Screen.OtpVerification) },
                        onNavigateToLogin = { navigateTo(Screen.Login) }
                    )
                    Screen.OtpVerification -> OtpVerificationScreen(
                        viewModel = viewModel,
                        onVerificationSuccess = { navigateTo(Screen.Home) },
                        onBackToSignUp = { goBack() }
                    )
                    Screen.ForgotPassword -> ForgotPasswordScreen(
                        onResetSent = {
                            Toast.makeText(context, "OTP Sent to registered email!", Toast.LENGTH_SHORT).show()
                            navigateTo(Screen.Login)
                        },
                        onBackToLogin = { goBack() }
                    )
                    Screen.Home -> HomeScreen(
                        viewModel = viewModel,
                        onIdeaSelected = { idea ->
                            viewModel.selectIdea(idea)
                            navigateTo(Screen.IdeaDetail)
                        },
                        onNavigateToNotifications = { navigateTo(Screen.Notifications) },
                        onNavigateToAdmin = { navigateTo(Screen.AdminPanel) }
                    )
                    Screen.IdeaDetail -> IdeaDetailScreen(
                        viewModel = viewModel,
                        onBack = { goBack() },
                        onRequestMeeting = { showMeetingDialog = true },
                        onRunAiReview = {
                            navigateTo(Screen.AiAssistant)
                        }
                    )
                    Screen.PostIdea -> PostIdeaScreen(
                        viewModel = viewModel,
                        onComplete = {
                            navigateTo(Screen.Home)
                        }
                    )
                    Screen.AiAssistant -> AiAssistantScreen(
                        viewModel = viewModel
                    )
                    Screen.Profile -> ProfileScreen(
                        viewModel = viewModel,
                        onIdeaSelected = { idea ->
                            viewModel.selectIdea(idea)
                            navigateTo(Screen.IdeaDetail)
                        },
                        onLoggedOut = {
                            navigateTo(Screen.Welcome)
                        }
                    )
                    Screen.Meetings -> MeetingsScreen(
                        viewModel = viewModel,
                        onIdeaSelected = { idea ->
                            viewModel.selectIdea(idea)
                            navigateTo(Screen.IdeaDetail)
                        }
                    )
                    Screen.Notifications -> NotificationsScreen(
                        viewModel = viewModel,
                        onBack = { goBack() }
                    )
                    Screen.AdminPanel -> AdminPanelScreen(
                        viewModel = viewModel,
                        onBack = { goBack() },
                        onIdeaSelected = { idea ->
                            viewModel.selectIdea(idea)
                            navigateTo(Screen.IdeaDetail)
                        }
                    )
                }
            }

            // Meeting Scheduler Dialog Form for Investors
            if (showMeetingDialog) {
                MeetingRequestDialog(
                    idea = viewModel.selectedIdea.value!!,
                    onDismiss = { showMeetingDialog = false },
                    onSubmit = { msg, date, time ->
                        viewModel.requestMeeting(viewModel.selectedIdea.value!!, msg, date, time)
                        showMeetingDialog = false
                        Toast.makeText(context, "Meeting request sent to founder!", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
}

// --- SUB-COMPOSABLES ---

// 1. WELCOME / ONBOARDING SCREEN
@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "ChakraRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        LightBackground,
                        Color(0xFFFFF3EC), // Saffron hint
                        Color(0xFFE8F5E9)  // Green hint
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Spinning Ashoka Chakra Graphic to represent the nation's progress and guidance
        Box(
            modifier = Modifier
                .size(180.dp)
                .rotate(rotationAngle),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(160.dp)) {
                val centerOffset = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2
                // Draw Outer Circle
                drawCircle(
                    color = AshokaBlue,
                    radius = radius,
                    style = Stroke(width = 6.dp.toPx())
                )
                // Draw Inner Hub
                drawCircle(
                    color = AshokaBlue,
                    radius = radius * 0.18f
                )
                // Draw 24 Spokes representing the 24 hours of duty and righteousness
                for (i in 0 until 24) {
                    val angleInRad = Math.toRadians((i * 15).toDouble())
                    val startX = centerOffset.x + (radius * 0.18f) * Math.cos(angleInRad).toFloat()
                    val startY = centerOffset.y + (radius * 0.18f) * Math.sin(angleInRad).toFloat()
                    val endX = centerOffset.x + radius * Math.cos(angleInRad).toFloat()
                    val endY = centerOffset.y + radius * Math.sin(angleInRad).toFloat()
                    drawLine(
                        color = AshokaBlue,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }
        }

        // Title & Branding
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "India Idea Hub",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = SaffronPrimary,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.testTag("app_title")
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = AshokaBlue.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "🇮🇳 BHBHART STARTUP SANDBOX 🇮🇳",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AshokaBlue,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Discover, post, and fund groundbreaking startup and social impact ideas across India's tier 1, 2, and 3 sectors.",
                fontSize = 16.sp,
                color = LightTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        // Get Started Button
        Button(
            onClick = onGetStarted,
            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("get_started_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Launch Sandbox Hub", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Filled.ArrowForward, contentDescription = "Launch")
            }
        }
    }
}

// 2. SIGN IN SCREEN
@Composable
fun LoginScreen(
    viewModel: AppViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Namaste!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = SaffronPrimary)
        Text("Sign in to connect with creators & investors", fontSize = 15.sp, color = LightTextSecondary)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = SaffronPrimary) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("email_input"),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = SaffronPrimary) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("password_input"),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onNavigateToForgotPassword) {
                Text("Forgot Password?", color = SaffronPrimary, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (authError != null) {
            Text(authError!!, color = Color.Red, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(context, "Please enter all details!", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.login(email, password) { success ->
                        if (success) {
                            Toast.makeText(context, "Log in successful!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("login_button")
        ) {
            Text("Sign In with Email", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("OR CONTINUTE WITH", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LightTextSecondary)

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign In (mocked securely, elegant button with ripple)
        OutlinedButton(
            onClick = {
                viewModel.login("investor_1@hub.in", "password") { success ->
                    if (success) {
                        Toast.makeText(context, "Google Sign In: Signed in as Investor", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("google_login_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AshokaBlue)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = "Google Logo", tint = AshokaBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign In with Google (Investor Demo)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                viewModel.login("admin@hub.in", "password") { success ->
                    if (success) {
                        Toast.makeText(context, "Signed in as System Admin", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = IndiaGreen)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = "Admin Log", tint = IndiaGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Access Admin Control Panel", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Don't have an account?", color = LightTextSecondary)
            TextButton(onClick = onNavigateToSignUp) {
                Text("Sign Up", color = SaffronPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 3. SIGN UP SCREEN (WITH SEGMENTED ROLE SELECTION)
@Composable
fun SignUpScreen(
    viewModel: AppViewModel,
    onOtpRequired: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("CREATOR") } // CREATOR or INVESTOR
    var bio by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Join the Sandbox!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = SaffronPrimary)
        Text("Empower your vision on India Idea Hub", fontSize = 15.sp, color = LightTextSecondary)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User", tint = SaffronPrimary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = SaffronPrimary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Role Selector: Custom Segmented Buttons
        Text("Choose Your Profile Role", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LightTextSecondary, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { role = "CREATOR" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (role == "CREATOR") SaffronPrimary else Color.LightGray.copy(alpha = 0.3f),
                    contentColor = if (role == "CREATOR") Color.White else LightTextSecondary
                ),
                shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Creator")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Creator", fontWeight = FontWeight.Bold)
                }
            }
            Button(
                onClick = { role = "INVESTOR" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (role == "INVESTOR") AshokaBlue else Color.LightGray.copy(alpha = 0.3f),
                    contentColor = if (role == "INVESTOR") Color.White else LightTextSecondary
                ),
                shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Business, contentDescription = "Investor")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Investor", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Short Professional Bio") },
            placeholder = { Text("e.g. Building sustainable agricultural IoT modules...") },
            maxLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (fullName.isEmpty() || email.isEmpty() || bio.isEmpty()) {
                    Toast.makeText(context, "Please fill all details!", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.signUp(email, fullName, role, bio) {
                        Toast.makeText(context, "OTP Sent successfully!", Toast.LENGTH_SHORT).show()
                        onOtpRequired()
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Send Verification OTP", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Already registered?", color = LightTextSecondary)
            TextButton(onClick = onNavigateToLogin) {
                Text("Sign In", color = SaffronPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 4. OTP VERIFICATION SCREEN
@Composable
fun OtpVerificationScreen(
    viewModel: AppViewModel,
    onVerificationSuccess: () -> Unit,
    onBackToSignUp: () -> Unit
) {
    var enteredCode by remember { mutableStateOf("") }
    val otpCode by viewModel.otpCode.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("OTP Verification", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = SaffronPrimary)
        Spacer(modifier = Modifier.height(10.dp))
        Text("We have simulated sending an SMS/Email OTP code.", fontSize = 14.sp, color = LightTextSecondary, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(24.dp))

        // Demo Helper Card showing generated OTP for frictionless testing
        Card(
            colors = CardDefaults.cardColors(containerColor = AshokaBlue.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("VERIFICATION DEMO MODE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AshokaBlue)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Your sandbox secure OTP is:", fontSize = 14.sp, color = LightTextSecondary)
                Text(otpCode, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = SaffronPrimary, letterSpacing = 4.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = enteredCode,
            onValueChange = { enteredCode = it },
            label = { Text("6-Digit Code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(textAlign = TextAlign.Center)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (authError != null) {
            Text(authError!!, color = Color.Red, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                viewModel.verifyOtp(
                    email = "new_creator@hub.in",
                    fullName = "Startup Builder",
                    role = "CREATOR",
                    bio = "Newly verified sandbox member.",
                    code = enteredCode
                ) { success ->
                    if (success) {
                        Toast.makeText(context, "Account verified successfully!", Toast.LENGTH_SHORT).show()
                        onVerificationSuccess()
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Verify OTP", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = {
            viewModel.resendOtp()
            Toast.makeText(context, "A new OTP code has been generated!", Toast.LENGTH_SHORT).show()
        }) {
            Text("Resend Verification Code", color = SaffronPrimary, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onBackToSignUp) {
            Text("Back to Registration", color = LightTextSecondary)
        }
    }
}

// 5. FORGOT PASSWORD
@Composable
fun ForgotPasswordScreen(onResetSent: () -> Unit, onBackToLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Forgot Password", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = SaffronPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Provide your registered email. We will generate an OTP code to unlock your credential securely.", fontSize = 14.sp, color = LightTextSecondary, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = SaffronPrimary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isEmpty()) {
                    Toast.makeText(context, "Please enter your email!", Toast.LENGTH_SHORT).show()
                } else {
                    onResetSent()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Send Security Unlock Link", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBackToLogin) {
            Text("Back to Sign In", color = SaffronPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

// 6. HOME SCREEN (MAIN BOARD)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onIdeaSelected: (IdeaEntity) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToAdmin: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val publishedIdeas by viewModel.publishedIdeas.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val unreadNotifications = notifications.filter { !it.isRead }.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        // Indian Flag Top Accent trim & App Bar
        item {
            Column {
                // Saffron, White, Green strip representing the Ashoka spirit
                Row(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFFF671F)))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.White))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF046A38)))
                }

                // Custom Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Namaste 🙏", fontSize = 14.sp, color = LightTextSecondary, fontWeight = FontWeight.Medium)
                        Text(currentUser?.fullName ?: "Visitor", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Quick switch to Admin if current user is ADMIN
                        if (currentUser?.role == "ADMIN") {
                            IconButton(onClick = onNavigateToAdmin) {
                                Icon(Icons.Default.Analytics, contentDescription = "Admin Analytics", tint = IndiaGreen)
                            }
                        }

                        // Notification Icon with Badge
                        Box(contentAlignment = Alignment.TopEnd) {
                            IconButton(onClick = onNavigateToNotifications) {
                                Icon(Icons.Default.Notifications, contentDescription = "Alerts", tint = SaffronPrimary, modifier = Modifier.size(28.dp))
                            }
                            if (unreadNotifications > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(Color.Red, shape = CircleShape)
                                        .align(Alignment.TopEnd),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = unreadNotifications.toString(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Search Bar Section
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search startup, agriculture, healthcare ideas...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = SaffronPrimary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SaffronPrimary,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
        }

        // Beautiful Hero Banner Card representing Bharat's Innovation Hub
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner_1782736885009),
                        contentDescription = "India Idea Hub Tech Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Gradient overlay to keep text highly readable
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Ignite Bharat's Innovation 🇮🇳",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Empowering entrepreneurs from agriculture to space tech.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Horizontal Categories List
        item {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(
                    text = "Browse Categories",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightTextPrimary,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(viewModel.categories) { category ->
                        val isSelected = selectedCategory == category
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) SaffronPrimary else Color.White,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) SaffronPrimary else Color.LightGray.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.clickable { viewModel.setCategory(category) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                        )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = category,
                                    color = if (isSelected) Color.White else LightTextSecondary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recommended / Featured Carousel Section
        item {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Trending Sandboxed Ideas 🚀", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)
                }

                if (publishedIdeas.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Empty", tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No startup ideas found matching selection.", color = LightTextSecondary, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(publishedIdeas) { idea ->
                            IdeaCardItem(idea = idea, onClick = { onIdeaSelected(idea) })
                        }
                    }
                }
            }
        }

        // Featured Creators Row
        item {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = "Featured Pioneers 🇮🇳",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightTextPrimary,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        CreatorChip(name = "Aarav Sharma", desc = "AgriTech Specialist", followers = "428", avatar = "avatar_1")
                    }
                    item {
                        CreatorChip(name = "Dr. Priya Iyer", desc = "Ayur AI Lead", followers = "1.2k", avatar = "avatar_2")
                    }
                    item {
                        CreatorChip(name = "Rohan Mehta", desc = "Bharat VC Angel", followers = "3.5k", avatar = "avatar_3")
                    }
                    item {
                        CreatorChip(name = "Ananya Nair", desc = "Ops Director", followers = "110", avatar = "avatar_4")
                    }
                }
            }
        }

        // Latest Feed Header
        item {
            Text(
                text = "Discover Feed",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = LightTextPrimary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
            )
        }

        // Display published ideas as a vertical scrolling feed
        items(publishedIdeas) { idea ->
            IdeaFeedCard(idea = idea, onClick = { onIdeaSelected(idea) })
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// 7. IDEA DETAIL VIEWER (PROBLEM, SOLUTION, BUDGETS, COMMENTS & MEETING OPTIONS)
@Composable
fun IdeaDetailScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onRequestMeeting: () -> Unit,
    onRunAiReview: () -> Unit
) {
    val idea by viewModel.selectedIdea.collectAsStateWithLifecycle()
    val comments by viewModel.ideaComments.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Overview, 1 = Plan, 2 = Feedback (Comments)
    var commentText by remember { mutableStateOf("") }
    val context = LocalContext.current

    if (idea == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val hasLiked by viewModel.isIdeaLiked(idea!!.id).collectAsStateWithLifecycle(initialValue = false)
    val hasBookmarked by viewModel.isIdeaBookmarked(idea!!.id).collectAsStateWithLifecycle(initialValue = false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        // Header Bar
        TopAppBarHeader(
            title = "Idea Pitch",
            onBack = onBack,
            actions = {
                // Saffron Flag details
                Row(modifier = Modifier.padding(end = 12.dp)) {
                    IconButton(onClick = { viewModel.likeIdea(idea!!, hasLiked) }) {
                        Icon(
                            if (hasLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (hasLiked) Color.Red else LightTextPrimary
                        )
                    }
                    IconButton(onClick = { viewModel.toggleBookmark(idea!!, hasBookmarked) }) {
                        Icon(
                            if (hasBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (hasBookmarked) AshokaBlue else LightTextPrimary
                        )
                    }
                }
            }
        )

        // Banner Image Canvas Graphic (Dynamic color matching category)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(getCategoryGradient(idea!!.category)),
            contentAlignment = Alignment.BottomStart
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Surface(
                    color = Color.White.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = idea!!.category.uppercase(Locale.getDefault()),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AshokaBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Title and Creator details
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            Text(
                text = idea!!.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = LightTextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(SaffronPrimary.copy(alpha = 0.15f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = idea!!.creatorName.take(1),
                        fontWeight = FontWeight.Bold,
                        color = SaffronPrimary
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Proposed by ${idea!!.creatorName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = LightTextPrimary
                    )
                    Text(
                        text = "Creator • Verified Entrepreneur",
                        fontSize = 12.sp,
                        color = LightTextSecondary
                    )
                }
            }
        }

        // Custom M3 Tab Row
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = Color.White,
            contentColor = SaffronPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                    color = SaffronPrimary
                )
            }
        ) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                Text("Overview", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                Text("Business Plan", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                Text("Feedback (${comments.size})", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.Bold)
            }
        }

        // Tab Contents
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeTab) {
                0 -> { // Overview Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Executive Pitch", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SaffronPrimary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(idea!!.description, fontSize = 14.sp, color = LightTextSecondary, lineHeight = 20.sp)
                                }
                            }
                        }

                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    modifier = Modifier.weight(1f),
                                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Funding Target", fontSize = 12.sp, color = LightTextSecondary, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(idea!!.estimatedBudget, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IndiaGreen)
                                    }
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    modifier = Modifier.weight(1f),
                                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Views Count", fontSize = 12.sp, color = LightTextSecondary, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("${idea!!.viewsCount} hits", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AshokaBlue)
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Pitch Materials", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { Toast.makeText(context, "Opening simulated PDF pitch deck...", Toast.LENGTH_SHORT).show() }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = "PDF", tint = Color.Red)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Pitch Deck Briefing Document (PDF)", fontSize = 13.sp, color = LightTextSecondary, fontWeight = FontWeight.Medium)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { Toast.makeText(context, "Streaming 60-second founder pitch demo...", Toast.LENGTH_SHORT).show() }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Video", tint = AshokaBlue, modifier = Modifier.rotate(-90f))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Elevator Pitch (60-sec Video Clip)", fontSize = 13.sp, color = LightTextSecondary, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        // AI Evaluator Call-To-Action Button inside Idea
                        item {
                            Button(
                                onClick = {
                                    viewModel.runAiIdeaReview(
                                        idea!!.title, idea!!.description, idea!!.category,
                                        idea!!.problemStatement, idea!!.solution, idea!!.estimatedBudget
                                    )
                                    onRunAiReview()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AshokaBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Panel")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analyze with AI Panel (Gemini)", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                1 -> { // Plan Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("The Problem", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SaffronPrimary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(idea!!.problemStatement, fontSize = 14.sp, color = LightTextSecondary, lineHeight = 20.sp)
                                }
                            }
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("The Solution", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = IndiaGreen)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(idea!!.solution, fontSize = 14.sp, color = LightTextSecondary, lineHeight = 20.sp)
                                }
                            }
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Target Audience & Users", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AshokaBlue)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(idea!!.targetAudience, fontSize = 14.sp, color = LightTextSecondary, lineHeight = 20.sp)
                                }
                            }
                        }
                    }
                }
                2 -> { // Comments Tab
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (comments.isEmpty()) {
                                item {
                                    Text(
                                        "No feedback submitted yet. Be the first to advise the founder!",
                                        color = LightTextSecondary,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 40.dp)
                                    )
                                }
                            } else {
                                items(comments) { comment ->
                                    CommentBubble(comment = comment)
                                }
                            }
                        }

                        // Comment entry bar
                        Surface(
                            shadowElevation = 8.dp,
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = commentText,
                                    onValueChange = { commentText = it },
                                    placeholder = { Text("Write constructive advice...") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                IconButton(
                                    onClick = {
                                        if (commentText.isNotEmpty()) {
                                            viewModel.submitComment(idea!!.id, commentText)
                                            commentText = ""
                                        }
                                    },
                                    modifier = Modifier.background(SaffronPrimary, CircleShape)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom CTAs for Investment
        if (currentUser?.id != idea!!.creatorId) {
            Surface(
                shadowElevation = 8.dp,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Report Abuse/Report Flag button
                    OutlinedButton(
                        onClick = {
                            viewModel.reportIdea(idea!!, "Inappropriate content or potential copyright breach.")
                            Toast.makeText(context, "Idea has been flagged for Admin review.", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(Icons.Default.Report, contentDescription = "Flag")
                    }

                    // Request Meeting Button
                    Button(
                        onClick = onRequestMeeting,
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Request Meeting with Founder", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

// 8. IDEA POSTING SCREEN
@Composable
fun PostIdeaScreen(
    viewModel: AppViewModel,
    onComplete: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Technology") }
    var description by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf("") }
    var solution by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        TopAppBarHeader(title = "Share Startup Idea", onBack = onComplete)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Launch Pad Form 🚀", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Startup / Idea Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Category selector dropdown simulated elegantly
            Text("Select Market Sector", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LightTextSecondary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(viewModel.categories.filter { it != "All" }) { cat ->
                    val isSelected = category == cat
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) SaffronPrimary else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) SaffronPrimary else Color.LightGray.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable { category = cat }
                    ) {
                        Text(cat, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = if (isSelected) Color.White else LightTextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Short Pitch Summary (Max 250 words)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = problem,
                onValueChange = { problem = it },
                label = { Text("What Local Problem Are You Solving?") },
                placeholder = { Text("e.g. Small farmers waste 40% fertilizer...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = solution,
                onValueChange = { solution = it },
                label = { Text("Your Proposed Innovating Solution") },
                placeholder = { Text("e.g. IoT moisture & nitrogen probe sensor...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = target,
                onValueChange = { target = it },
                label = { Text("Target Consumer / Demographics") },
                placeholder = { Text("e.g. 120M small farming families...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = budget,
                onValueChange = { budget = it },
                label = { Text("Estimated Seed Capital Budget (e.g. ₹ 20 Lakh)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // AI Help CTA in posting form
            Card(
                colors = CardDefaults.cardColors(containerColor = SaffronPrimary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = SaffronPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Stuck with pitch grammar?", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SaffronPrimary)
                        Text("Polishing, pitch generation, and name recommendations are available in the 'AI Hub' workspace.", fontSize = 11.sp, color = LightTextSecondary)
                    }
                }
            }

            // Publish Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (title.isEmpty()) {
                            Toast.makeText(context, "Please enter title first!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.saveIdea(
                                title = title, description = description, category = category,
                                problem = problem, solution = solution, target = target, budget = budget, isPublish = false
                            ) {
                                onComplete()
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text("Save Draft", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (title.isEmpty() || description.isEmpty() || problem.isEmpty() || solution.isEmpty()) {
                            Toast.makeText(context, "Please complete core plan fields!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.saveIdea(
                                title = title, description = description, category = category,
                                problem = problem, solution = solution, target = target, budget = budget, isPublish = true
                            ) {
                                onComplete()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(52.dp)
                ) {
                    Text("Publish Sandbox Pitch", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 9. AI ASSISTANT HUB (IDEA AI SANDBOX INTERACTION WORKSPACE)
@Composable
fun AiAssistantScreen(viewModel: AppViewModel) {
    var toolSelection by remember { mutableStateOf(0) } // 0 = Review, 1 = Polish Grammar, 2 = Pitch, 3 = Market, 4 = Business Name
    var input1 by remember { mutableStateOf("") }
    var input2 by remember { mutableStateOf("") }

    val aiLoading by viewModel.aiLoading.collectAsStateWithLifecycle()
    val aiResult by viewModel.aiResult.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        // Simple Top accent flag
        Row(modifier = Modifier.fillMaxWidth().height(3.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFFF671F)))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.White))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF046A38)))
        }

        // Header Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Hub", tint = SaffronPrimary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("AI Incubation Sandbox", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)
        }

        // Horizontal scrolling tool selectors
        LazyRow(
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tools = listOf("AI Panel Review", "Grammar Polisher", "Pitch Hook Maker", "Market Research", "Name Generator")
            items(tools.size) { idx ->
                val selected = toolSelection == idx
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) SaffronPrimary else Color.White,
                    border = BorderStroke(1.dp, if (selected) SaffronPrimary else Color.LightGray.copy(alpha = 0.5f)),
                    modifier = Modifier.clickable {
                        toolSelection = idx
                        viewModel.clearAiResult()
                        input1 = ""
                        input2 = ""
                    }
                ) {
                    Text(tools[idx], modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = if (selected) Color.White else LightTextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Explanatory Tag for the selected workspace tool
            val toolDetails = when (toolSelection) {
                0 -> Pair("AI Panel Evaluation", "Assembles simulated top Indian venture capital reviews utilizing direct Gemini API reasoning.")
                1 -> Pair("Syllable & Grammar Polisher", "Converts loose ideation into polished, boardroom-ready vernacular and English copy.")
                2 -> Pair("VC Pitch deck Creator", "Formulates catchy hooks and structures slides optimized for Indian angel networks.")
                3 -> Pair("Stat & CAGR Market Researcher", "Drives local demographics and growth analysis for the specified sector.")
                else -> Pair("Desi Tech Name Generator", "Recommends evocative Sanskrit-modern portmanteaus with linguistic definitions.")
            }

            Surface(
                color = AshokaBlue.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(toolDetails.first, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AshokaBlue)
                    Text(toolDetails.second, fontSize = 11.sp, color = LightTextSecondary)
                }
            }

            // Dynamically change inputs based on workspace selection
            when (toolSelection) {
                0 -> { // Review
                    OutlinedTextField(
                        value = input1,
                        onValueChange = { input1 = it },
                        label = { Text("Startup Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = input2,
                        onValueChange = { input2 = it },
                        label = { Text("Core Business Concept Summary") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Button(
                        onClick = { input1 = "KrishiSetu"; input2 = "An ultra low cost IoT probe designed for smallholder farmers measuring soil and sending recommendations directly over SMS." },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray.copy(alpha = 0.4f), contentColor = LightTextPrimary)
                    ) {
                        Text("Auto-fill with KrishiSetu details", fontSize = 12.sp)
                    }
                }
                1 -> { // Grammar
                    OutlinedTextField(
                        value = input1,
                        onValueChange = { input1 = it },
                        label = { Text("Unstructured Idea Copy") },
                        placeholder = { Text("we are making tech stuff that helps village school study easily without bad net connections...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                2 -> { // Pitch
                    OutlinedTextField(
                        value = input1,
                        onValueChange = { input1 = it },
                        label = { Text("Startup Concept") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = input2,
                        onValueChange = { input2 = it },
                        label = { Text("Brief Target Demographics") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                3 -> { // Market
                    OutlinedTextField(
                        value = input1,
                        onValueChange = { input1 = it },
                        label = { Text("Target Industry (e.g. Agri-Tech, AI Ayurveda)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = input2,
                        onValueChange = { input2 = it },
                        label = { Text("Core Problem Tackled") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                4 -> { // Business Name
                    OutlinedTextField(
                        value = input1,
                        onValueChange = { input1 = it },
                        label = { Text("Core Keywords or Values (e.g. Solar, Village, Clean, Energy)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Generate Action Button
            Button(
                onClick = {
                    if (input1.isEmpty() && input2.isEmpty()) {
                        Toast.makeText(context, "Please enter details first!", Toast.LENGTH_SHORT).show()
                    } else {
                        when (toolSelection) {
                            0 -> viewModel.runAiIdeaReview(input1, input2, "Misc", "Problem", "Solution", "₹ 25 Lakh")
                            1 -> viewModel.runAiGrammarImprovement(input1)
                            2 -> viewModel.runAiPitchSuggestions(input1, input2)
                            3 -> viewModel.runAiMarketAnalysis(input1, "Sandbox App", input2)
                            4 -> viewModel.runAiBusinessNameSuggestions(input1)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !aiLoading
            ) {
                if (aiLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Spark")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Invoke Gemini AI Sandbox", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Live AI Generation Output Card
            if (aiResult != null || aiLoading) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Result", tint = SaffronPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gemini Sandbox Output", fontWeight = FontWeight.Bold, color = SaffronPrimary)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color.LightGray.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(12.dp))

                        if (aiLoading) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = SaffronPrimary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Assembling statistics and evaluating via Gemini API...", fontSize = 12.sp, color = LightTextSecondary)
                            }
                        } else {
                            Text(
                                text = aiResult ?: "",
                                fontSize = 14.sp,
                                color = LightTextPrimary,
                                lineHeight = 21.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                }
            }
        }
    }
}

// 10. CREATOR OR INVESTOR PROFILE SCREEN
@Composable
fun ProfileScreen(
    viewModel: AppViewModel,
    onIdeaSelected: (IdeaEntity) -> Unit,
    onLoggedOut: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val drafts by viewModel.userDraftIdeas.collectAsStateWithLifecycle()
    val published by viewModel.userPublishedIdeas.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarkedIdeas.collectAsStateWithLifecycle()

    var profileTab by remember { mutableStateOf(0) } // 0 = Published, 1 = Drafts, 2 = Bookmarks

    if (currentUser == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        // Banner profile decor
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFF671F), Color(0xFF046A38))
                        )
                    )
            )
        }

        // Profile Avatar Card details
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular Avatar
                Box(
                    modifier = Modifier
                        .offset(y = (-50).dp)
                        .size(90.dp)
                        .background(Color.White, CircleShape)
                        .border(3.dp, SaffronPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(78.dp)
                            .background(AshokaBlue.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentUser!!.fullName.take(1),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = AshokaBlue
                        )
                    }
                }

                Column(
                    modifier = Modifier.offset(y = (-40).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(currentUser!!.fullName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = if (currentUser!!.role == "INVESTOR") AshokaBlue.copy(alpha = 0.12f) else SaffronPrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = currentUser!!.role,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentUser!!.role == "INVESTOR") AshokaBlue else SaffronPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = currentUser!!.bio,
                        fontSize = 14.sp,
                        color = LightTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Social links & metrics rows
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(currentUser!!.followersCount.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)
                            Text("Followers", fontSize = 12.sp, color = LightTextSecondary)
                        }
                        Divider(modifier = Modifier.height(24.dp).width(1.dp), color = Color.LightGray)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(currentUser!!.followingCount.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)
                            Text("Following", fontSize = 12.sp, color = LightTextSecondary)
                        }
                    }
                }
            }
        }

        // Skill chips list
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                    .offset(y = (-30).dp)
            ) {
                Text("Skills & Focus Areas", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    currentUser!!.skills.split(",").forEach { skill ->
                        if (skill.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = LightBackground,
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                            ) {
                                Text(skill.trim(), fontSize = 12.sp, color = LightTextSecondary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Profile Tab Choices
        item {
            val tabs = if (currentUser!!.role == "INVESTOR") listOf("Bookmarked Ideas", "Bio") else listOf("Published Pitches", "Drafts")
            TabRow(
                selectedTabIndex = profileTab,
                containerColor = Color.White,
                contentColor = SaffronPrimary,
                modifier = Modifier.offset(y = (-20).dp)
            ) {
                tabs.forEachIndexed { idx, name ->
                    Tab(selected = profileTab == idx, onClick = { profileTab = idx }) {
                        Text(name, modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // List contents under tabs
        if (currentUser!!.role == "CREATOR") {
            if (profileTab == 0) { // Published
                if (published.isEmpty()) {
                    item {
                        EmptyProfileFeed("No published ideas yet. Click '+' to share your first startup pitch!")
                    }
                } else {
                    items(published) { idea ->
                        IdeaFeedCard(idea = idea, onClick = { onIdeaSelected(idea) })
                    }
                }
            } else { // Drafts
                if (drafts.isEmpty()) {
                    item {
                        EmptyProfileFeed("No drafts saved currently.")
                    }
                } else {
                    items(drafts) { idea ->
                        IdeaFeedCard(idea = idea, onClick = { onIdeaSelected(idea) })
                    }
                }
            }
        } else { // INVESTOR tabs
            if (profileTab == 0) { // Bookmarked
                if (bookmarks.isEmpty()) {
                    item {
                        EmptyProfileFeed("No saved ideas currently. Browse trending pitches and click bookmark to save.")
                    }
                } else {
                    items(bookmarks) { idea ->
                        IdeaFeedCard(idea = idea, onClick = { onIdeaSelected(idea) })
                    }
                }
            } else { // Investor info details
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.padding(16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Angel Mandate & Preferences", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AshokaBlue)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("• Ticket Size: ₹ 10 Lakh - ₹ 1 Crore\n• Sectors: AgriTech, AI, HealthTech\n• Stage: Seed, MVP ready\n• Geography: Pan India, focusing on Rural & Vernacular initiatives.", fontSize = 14.sp, color = LightTextSecondary, lineHeight = 20.sp)
                        }
                    }
                }
            }
        }

        // Logout Button
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        viewModel.logOut()
                        onLoggedOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = "Log out")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Out Session", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

// 11. MEETINGS TAB MANAGER
@Composable
fun MeetingsScreen(
    viewModel: AppViewModel,
    onIdeaSelected: (IdeaEntity) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val meetings by viewModel.meetingRequests.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        TopAppBarHeader(title = "Meeting Sandbox Scheduler", onBack = {})

        if (meetings.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Empty", tint = Color.Gray, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No connections scheduled yet.", fontWeight = FontWeight.Bold, color = LightTextPrimary)
                    Text("Investors can request connect meetings from any idea overview detail tab.", fontSize = 12.sp, color = LightTextSecondary, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(meetings) { request ->
                    MeetingRequestCard(
                        request = request,
                        currentUserRole = currentUser?.role ?: "CREATOR",
                        onStatusChange = { newStatus ->
                            viewModel.updateMeetingStatus(request, newStatus)
                        }
                    )
                }
            }
        }
    }
}

// 12. NOTIFICATIONS LIST VIEW
@Composable
fun NotificationsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val alerts by viewModel.notifications.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        TopAppBarHeader(title = "Notifications Hub", onBack = onBack)

        if (alerts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Notifications, contentDescription = "Empty", tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No new alerts.", color = LightTextSecondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(alerts) { notification ->
                    NotificationItemCard(notification = notification, onRead = {
                        viewModel.readNotification(notification.id)
                    })
                }
            }
        }
    }
}

// 13. ADMIN PANEL SCREEN
@Composable
fun AdminPanelScreen(viewModel: AppViewModel, onBack: () -> Unit, onIdeaSelected: (IdeaEntity) -> Unit) {
    val reportedList by viewModel.reportedIdeas.collectAsStateWithLifecycle()
    val ideasFeed by viewModel.publishedIdeas.collectAsStateWithLifecycle()
    val usersList by viewModel.allUsersList.collectAsStateWithLifecycle()

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        TopAppBarHeader(title = "Admin Control Panel", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Analytics Dashboard widgets row
            item {
                Text("Platform Analytics Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AnalyticsMiniWidget(title = "Active Ideas", value = ideasFeed.size.toString(), color = SaffronPrimary, modifier = Modifier.weight(1f))
                    AnalyticsMiniWidget(title = "Members", value = usersList.size.toString(), color = AshokaBlue, modifier = Modifier.weight(1f))
                    AnalyticsMiniWidget(title = "Flagged Posts", value = reportedList.size.toString(), color = Color.Red, modifier = Modifier.weight(1f))
                }
            }

            // Category split statistics table
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Vitals & Moderation Ratio", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = IndiaGreen)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• Safe Sandbox environment status: OPTIMAL\n• Automatic keyword filtering: ENABLED\n• User reported queue backlog: ${reportedList.size} cases.", fontSize = 13.sp, color = LightTextSecondary, lineHeight = 20.sp)
                    }
                }
            }

            // Reported posts list
            item {
                Text("Reported Moderation Queue", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)
            }

            if (reportedList.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("Hooray! No reported ideas or flagged abuse. Clean slate.", fontSize = 13.sp, color = LightTextSecondary)
                        }
                    }
                }
            } else {
                items(reportedList) { idea ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(idea.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LightTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Surface(color = Color.Red.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                                    Text("FLAGGED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Red, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Author: ${idea.creatorName}", fontSize = 12.sp, color = LightTextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Report Reason: ${idea.reportReason}", fontSize = 12.sp, color = Color.Red, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = { onIdeaSelected(idea) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Inspect Pitch", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        viewModel.dismissReport(idea.id)
                                        Toast.makeText(context, "Report flag dismissed.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = IndiaGreen),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Dismiss Report", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        viewModel.deleteIdea(idea)
                                        Toast.makeText(context, "Idea pitch removed from platform database.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Remove Post", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- HELPER COMPONENT CHIPS & CARDS ---

@Composable
fun BottomNavigationBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    isAdmin: Boolean
) {
    Surface(
        shadowElevation = 16.dp,
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        NavigationBar(
            containerColor = Color.White,
            contentColor = LightTextSecondary
        ) {
            NavigationBarItem(
                selected = currentScreen == Screen.Home,
                onClick = { onNavigate(Screen.Home) },
                icon = { Icon(if (currentScreen == Screen.Home) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Home") },
                label = { Text("Home", fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = SaffronPrimary, selectedTextColor = SaffronPrimary, indicatorColor = SaffronPrimary.copy(alpha = 0.1f))
            )
            NavigationBarItem(
                selected = currentScreen == Screen.AiAssistant,
                onClick = { onNavigate(Screen.AiAssistant) },
                icon = { Icon(if (currentScreen == Screen.AiAssistant) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome, contentDescription = "AI") },
                label = { Text("AI Sandbox", fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = SaffronPrimary, selectedTextColor = SaffronPrimary, indicatorColor = SaffronPrimary.copy(alpha = 0.1f))
            )
            NavigationBarItem(
                selected = currentScreen == Screen.PostIdea,
                onClick = { onNavigate(Screen.PostIdea) },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Pitch", tint = Color.White, modifier = Modifier.background(SaffronPrimary, CircleShape).padding(4.dp)) },
                label = { Text("Share", fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = SaffronPrimary, selectedTextColor = SaffronPrimary, indicatorColor = Color.Transparent)
            )
            NavigationBarItem(
                selected = currentScreen == Screen.Meetings,
                onClick = { onNavigate(Screen.Meetings) },
                icon = { Icon(if (currentScreen == Screen.Meetings) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth, contentDescription = "Meetings") },
                label = { Text("Meetings", fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = SaffronPrimary, selectedTextColor = SaffronPrimary, indicatorColor = SaffronPrimary.copy(alpha = 0.1f))
            )
            NavigationBarItem(
                selected = currentScreen == Screen.Profile,
                onClick = { onNavigate(Screen.Profile) },
                icon = { Icon(if (currentScreen == Screen.Profile) Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "Profile") },
                label = { Text("Profile", fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = SaffronPrimary, selectedTextColor = SaffronPrimary, indicatorColor = SaffronPrimary.copy(alpha = 0.1f))
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarHeader(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().height(3.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFFF671F)))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.White))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF046A38)))
        }
        TopAppBar(
            title = { Text(title, fontWeight = FontWeight.Bold, color = LightTextPrimary) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )
        Divider(color = Color.LightGray.copy(alpha = 0.3f))
    }
}

@Composable
fun IdeaCardItem(idea: IdeaEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .height(210.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Category gradient banner header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(getCategoryGradient(idea.category))
                    .padding(10.dp)
            ) {
                Surface(color = Color.White.copy(alpha = 0.85f), shape = RoundedCornerShape(6.dp)) {
                    Text(idea.category, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AshokaBlue, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(idea.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, color = LightTextPrimary)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = "Creator", tint = SaffronPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(idea.creatorName.take(12), fontSize = 11.sp, color = LightTextSecondary, maxLines = 1)
                    }

                    Surface(color = IndiaGreen.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text(idea.estimatedBudget, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IndiaGreen, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun IdeaFeedCard(idea: IdeaEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Category accent graphic circle indicator
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(getCategoryGradient(idea.category), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Spark", tint = Color.White, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(idea.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LightTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(idea.category, fontSize = 11.sp, color = AshokaBlue, fontWeight = FontWeight.Bold)
                    Text(" • by ${idea.creatorName}", fontSize = 11.sp, color = LightTextSecondary)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(idea.estimatedBudget, fontWeight = FontWeight.Bold, color = IndiaGreen, fontSize = 13.sp)
                Text("${idea.likesCount} ❤️", fontSize = 10.sp, color = LightTextSecondary)
            }
        }
    }
}

@Composable
fun CreatorChip(name: String, desc: String, followers: String, avatar: String) {
    Card(
        modifier = Modifier.width(140.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(SaffronPrimary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(1), fontWeight = FontWeight.Bold, color = SaffronPrimary, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LightTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(desc, fontSize = 10.sp, color = LightTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Text("$followers connects", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AshokaBlue)
        }
    }
}

@Composable
fun CommentBubble(comment: CommentEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(AshokaBlue.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(comment.userName.take(1), fontWeight = FontWeight.Bold, color = AshokaBlue, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(comment.userName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LightTextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(comment.text, fontSize = 13.sp, color = LightTextSecondary, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
fun NotificationItemCard(notification: NotificationEntity, onRead: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) Color.White else SaffronPrimary.copy(alpha = 0.04f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRead() },
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(
                if (notification.isRead) Icons.Default.Check else Icons.Default.Notifications,
                contentDescription = "Alert",
                tint = if (notification.isRead) IndiaGreen else SaffronPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(notification.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LightTextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(notification.message, fontSize = 12.sp, color = LightTextSecondary, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
fun MeetingRequestCard(
    request: MeetingRequestEntity,
    currentUserRole: String,
    onStatusChange: (String) -> Unit
) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(request.ideaTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LightTextPrimary)
                Surface(
                    color = when (request.status) {
                        "ACCEPTED" -> IndiaGreen.copy(alpha = 0.12f)
                        "REJECTED" -> Color.Red.copy(alpha = 0.12f)
                        else -> Color.Gray.copy(alpha = 0.12f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        request.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (request.status) {
                            "ACCEPTED" -> IndiaGreen
                            "REJECTED" -> Color.Red
                            else -> Color.Gray
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("Investor: ${request.investorName}", fontSize = 12.sp, color = LightTextSecondary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("\"${request.message}\"", fontSize = 13.sp, color = LightTextSecondary, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)

            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Date", tint = SaffronPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Schedule: ${request.date} at ${request.time}", fontSize = 12.sp, color = LightTextPrimary, fontWeight = FontWeight.SemiBold)
            }

            // Meeting actions for the Creator
            if (currentUserRole == "CREATOR" && request.status == "PENDING") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onStatusChange("ACCEPTED") },
                        colors = ButtonDefaults.buttonColors(containerColor = IndiaGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Accept Invite", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onStatusChange("REJECTED") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Decline", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MeetingRequestDialog(
    idea: IdeaEntity,
    onDismiss: () -> Unit,
    onSubmit: (message: String, date: String, time: String) -> Unit
) {
    var message by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("2026-07-05") }
    var time by remember { mutableStateOf("11:00 AM") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color.LightGray)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Request Connection Meeting", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SaffronPrimary)
                Text("Pitch target: ${idea.title}\nFounder: ${idea.creatorName}", fontSize = 12.sp, color = LightTextSecondary)

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Introductory Message / Mandate") },
                    placeholder = { Text("e.g. Loved your agritech probe plan! Let's talk funding terms...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Proposed Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Proposed Time Slot") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSubmit(message, date, time) },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("Request Meeting", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsMiniWidget(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 10.sp, color = LightTextSecondary, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun EmptyProfileFeed(msg: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.padding(16.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(msg, fontSize = 13.sp, color = LightTextSecondary, textAlign = TextAlign.Center, lineHeight = 18.sp)
        }
    }
}

// Helper to determine gradient colors based on startup market sector
fun getCategoryGradient(category: String): Brush {
    return when (category.lowercase(Locale.getDefault())) {
        "agriculture" -> Brush.horizontalGradient(listOf(Color(0xFF046A38), Color(0xFF2E935A)))
        "healthcare" -> Brush.horizontalGradient(listOf(Color(0xFF00ACC1), Color(0xFF00D8F6)))
        "education" -> Brush.horizontalGradient(listOf(Color(0xFFFF9F43), Color(0xFFFFC58D)))
        "environment" -> Brush.horizontalGradient(listOf(Color(0xFF4CAF50), Color(0xFF81C784)))
        "technology", "ai" -> Brush.horizontalGradient(listOf(Color(0xFF06038D), Color(0xFF536DFE)))
        "finance" -> Brush.horizontalGradient(listOf(Color(0xFF673AB7), Color(0xFF9575CD)))
        else -> Brush.horizontalGradient(listOf(Color(0xFFFF671F), Color(0xFFFF9E73)))
    }
}
