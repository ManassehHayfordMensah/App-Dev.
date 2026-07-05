package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.FeedEntity
import com.example.data.JobEntity
import com.example.data.NotificationEntity
import com.example.data.UserEntity
import com.example.R
import com.example.ui.theme.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillJetApp(
    viewModel: SkillJetViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val showOnboarding by viewModel.showOnboarding.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (showOnboarding) {
            OnboardingScreen(
                onFinish = { viewModel.completeOnboarding() }
            )
        } else {
            Crossfade(targetState = authState, label = "AuthCrossfade") { state ->
            when (state) {
                is AuthState.Unauthenticated -> {
                    AuthScreen(
                        onLogin = { email, isWorker -> viewModel.login(email, isWorker) {} },
                        onSignup = { name, email, phone, isWorker -> viewModel.signUp(name, email, phone, isWorker) {} }
                    )
                }
                is AuthState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ElegantDarkPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = state.message, fontWeight = FontWeight.Medium, color = ElegantDarkPrimary)
                        }
                    }
                }
                is AuthState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Error, contentDescription = "Error", tint = GhanaRed, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = state.message, color = GhanaRed, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.logout() }, colors = ButtonDefaults.buttonColors(containerColor = ElegantDarkPrimary, contentColor = ElegantDarkOnPrimary)) {
                                Text("Try Again")
                            }
                        }
                    }
                }
                is AuthState.Authenticated -> {
                    MainScreen(user = state.user, viewModel = viewModel)
                }
            }
        }
    }
}
}

// --- OnboardingScreen Component ---

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val slides = remember {
        listOf(
            OnboardingSlide(
                imageResId = R.drawable.img_onboarding_welcome_1783244106957,
                title = "Welcome to",
                accentWord = "SkillJet",
                description = "Empowering expert local mechanics, plumbers, electricians, and clients across Ghana with high-fidelity, trusted connections."
            ),
            OnboardingSlide(
                imageResId = R.drawable.img_onboarding_connect_1783244124947,
                title = "Connect with",
                accentWord = "Verified Pros",
                description = "Find and hire high-integrity local tradespeople stationed near your specific landmarks. Reliable local service is at your fingertips."
            ),
            OnboardingSlide(
                imageResId = R.drawable.img_onboarding_ai_1783244138205,
                title = "Smart AI",
                accentWord = "Trade Companion",
                description = "Leverage advanced Gemini AI trade mentoring for real-time technical repair advice, pricing guidance, and automated skill verification."
            )
        )
    }

    var currentSlideIndex by remember { mutableStateOf(0) }
    val slide = slides[currentSlideIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ElegantDarkBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Construction,
                    contentDescription = null,
                    tint = ElegantDarkPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SkillJet",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            if (currentSlideIndex < slides.size - 1) {
                Text(
                    text = "Skip",
                    color = ElegantDarkMutedText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onFinish() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Slide Content (Image and Description) with Slide/Fade Transition
        AnimatedContent(
            targetState = currentSlideIndex,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut()
                    )
                }
            },
            label = "SlideTransition",
            modifier = Modifier.weight(4f)
        ) { index ->
            val activeSlide = slides[index]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Illustration Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .border(1.dp, ElegantDarkBorder, RoundedCornerShape(28.dp)),
                    colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = activeSlide.imageResId),
                        contentDescription = "Onboarding Illustration",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Heading with highlighted word
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeSlide.title + " ",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = activeSlide.accentWord,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ElegantDarkPrimary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Text(
                    text = activeSlide.description,
                    fontSize = 14.sp,
                    color = ElegantDarkText,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Bottom Controls (Indicator and Navigation buttons)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Page Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                slides.forEachIndexed { i, _ ->
                    val isActive = i == currentSlideIndex
                    val width by animateDpAsState(
                        targetValue = if (isActive) 24.dp else 8.dp,
                        label = "DotWidth"
                    )
                    val color = if (isActive) ElegantDarkPrimary else ElegantDarkBorder

                    Box(
                        modifier = Modifier
                            .size(width = width, height = 8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            // Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentSlideIndex > 0) {
                    OutlinedButton(
                        onClick = { currentSlideIndex-- },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ElegantDarkPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElegantDarkBorder),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text("Back", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Button(
                    onClick = {
                        if (currentSlideIndex < slides.size - 1) {
                            currentSlideIndex++
                        } else {
                            onFinish()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElegantDarkPrimary,
                        contentColor = ElegantDarkOnPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = if (currentSlideIndex == slides.size - 1) "Get Started" else "Next",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

data class OnboardingSlide(
    val imageResId: Int,
    val title: String,
    val accentWord: String,
    val description: String
)

// --- AuthScreen Component ---

@Composable
fun AuthScreen(
    onLogin: (String, Boolean) -> Unit,
    onSignup: (String, String, String, Boolean) -> Unit
) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var isWorkerRole by remember { mutableStateOf(false) }

    // Form inputs
    var nameInput by remember { mutableStateOf("") }
    var emailOrPhoneInput by remember { mutableStateOf("") } // Supports both email or phone for login
    var emailInput by remember { mutableStateOf("") }       // Signup specific
    var phoneInput by remember { mutableStateOf("") }       // Signup specific
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(ElegantDarkPrimary, ElegantDarkPrimaryContainer)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ElegantDarkBg)
    ) {
        // Upper background layout design
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .background(gradientBrush)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.img_app_logo),
                    contentDescription = "SkillJet logo",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "SkillJet",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Ghana's Premium Skilled Workers Hub",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Form Card Layout
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text(
                        text = if (isSignUpMode) "Create Account" else "Welcome Back",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Segmented Selector for Role (Client vs Skilled Professional)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .border(1.dp, ElegantDarkBorder, RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (!isWorkerRole) ElegantDarkPrimary else Color.Transparent)
                                .clickable { isWorkerRole = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Client",
                                color = if (!isWorkerRole) ElegantDarkOnPrimary else ElegantDarkMutedText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (isWorkerRole) ElegantDarkPrimary else Color.Transparent)
                                .clickable { isWorkerRole = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Skilled Professional",
                                color = if (isWorkerRole) ElegantDarkOnPrimary else ElegantDarkMutedText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                if (isSignUpMode) {
                    item {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = ElegantDarkPrimary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElegantDarkPrimary,
                                focusedLabelColor = ElegantDarkPrimary,
                                unfocusedBorderColor = ElegantDarkBorder,
                                unfocusedLabelColor = ElegantDarkMutedText,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = ElegantDarkText
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    item {
                        val isEmailError = emailInput.isNotEmpty() && !isValidEmail(emailInput)
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Email Address") },
                            isError = isEmailError,
                            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = ElegantDarkPrimary) },
                            supportingText = {
                                if (isEmailError) {
                                    Text("Please enter a valid email address", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                } else if (emailInput.isNotEmpty()) {
                                    Text("Valid email format verified", color = ElegantDarkPrimary, fontSize = 11.sp)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElegantDarkPrimary,
                                focusedLabelColor = ElegantDarkPrimary,
                                unfocusedBorderColor = ElegantDarkBorder,
                                unfocusedLabelColor = ElegantDarkMutedText,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = ElegantDarkText,
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                errorLabelColor = MaterialTheme.colorScheme.error,
                                errorLeadingIconColor = MaterialTheme.colorScheme.error
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    item {
                        val isPhoneError = phoneInput.isNotEmpty() && !isValidGhanaPhone(phoneInput)
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Phone Number") },
                            isError = isPhoneError,
                            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = ElegantDarkPrimary) },
                            supportingText = {
                                if (isPhoneError) {
                                    Text("Must be a valid Ghanaian format (e.g., 024XXXXXXX or +23324XXXXXXX)", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                } else if (phoneInput.isNotEmpty()) {
                                    Text("Valid Ghanaian format verified", color = ElegantDarkPrimary, fontSize = 11.sp)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElegantDarkPrimary,
                                focusedLabelColor = ElegantDarkPrimary,
                                unfocusedBorderColor = ElegantDarkBorder,
                                unfocusedLabelColor = ElegantDarkMutedText,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = ElegantDarkText,
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                errorLabelColor = MaterialTheme.colorScheme.error,
                                errorLeadingIconColor = MaterialTheme.colorScheme.error
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                } else {
                    // Login mode: requires email or phone number and password
                    item {
                        val isEmail = emailOrPhoneInput.contains("@")
                        val isInputValid = emailOrPhoneInput.isEmpty() || (if (isEmail) isValidEmail(emailOrPhoneInput) else isValidGhanaPhone(emailOrPhoneInput))
                        val isInputError = !isInputValid
                        OutlinedTextField(
                            value = emailOrPhoneInput,
                            onValueChange = { emailOrPhoneInput = it },
                            label = { Text("Email or Phone Number") },
                            isError = isInputError,
                            leadingIcon = { Icon(Icons.Filled.ContactPage, contentDescription = null, tint = ElegantDarkPrimary) },
                            supportingText = {
                                if (isInputError) {
                                    val errorMsg = if (isEmail) "Please enter a valid email address" else "Must be a valid email or Ghanaian phone number (e.g., 024XXXXXXX)"
                                    Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                } else if (emailOrPhoneInput.isNotEmpty()) {
                                    Text(if (isEmail) "Valid email format" else "Valid Ghanaian phone format", color = ElegantDarkPrimary, fontSize = 11.sp)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElegantDarkPrimary,
                                focusedLabelColor = ElegantDarkPrimary,
                                unfocusedBorderColor = ElegantDarkBorder,
                                unfocusedLabelColor = ElegantDarkMutedText,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = ElegantDarkText,
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                errorLabelColor = MaterialTheme.colorScheme.error,
                                errorLeadingIconColor = MaterialTheme.colorScheme.error
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                item {
                    val isPasswordError = passwordInput.isNotEmpty() && !isValidPassword(passwordInput)
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        isError = isPasswordError,
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = ElegantDarkPrimary) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = ElegantDarkMutedText
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElegantDarkPrimary,
                            focusedLabelColor = ElegantDarkPrimary,
                            unfocusedBorderColor = ElegantDarkBorder,
                            unfocusedLabelColor = ElegantDarkMutedText,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = ElegantDarkText,
                            errorBorderColor = MaterialTheme.colorScheme.error,
                            errorLabelColor = MaterialTheme.colorScheme.error,
                            errorLeadingIconColor = MaterialTheme.colorScheme.error
                        )
                    )
                    
                    if (passwordInput.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val hasMinLength = passwordInput.length >= 8
                            val hasUpperLower = passwordInput.any { it.isUpperCase() } && passwordInput.any { it.isLowerCase() }
                            val hasDigitOrSpecial = passwordInput.any { it.isDigit() || !it.isLetterOrDigit() }

                            PasswordRequirementRow(text = "At least 8 characters", isValid = hasMinLength)
                            PasswordRequirementRow(text = "Uppercase & lowercase letters", isValid = hasUpperLower)
                            PasswordRequirementRow(text = "At least 1 digit or special character", isValid = hasDigitOrSpecial)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    val isEnabled = if (isSignUpMode) {
                        nameInput.isNotBlank() &&
                        isValidEmail(emailInput) &&
                        isValidGhanaPhone(phoneInput) &&
                        isValidPassword(passwordInput)
                    } else {
                        val isEmail = emailOrPhoneInput.contains("@")
                        val isInputValid = if (isEmail) isValidEmail(emailOrPhoneInput) else isValidGhanaPhone(emailOrPhoneInput)
                        isInputValid && isValidPassword(passwordInput)
                    }

                    Button(
                        onClick = {
                            if (isSignUpMode) {
                                onSignup(nameInput, emailInput, phoneInput, isWorkerRole)
                            } else {
                                onLogin(emailOrPhoneInput, isWorkerRole)
                            }
                        },
                        enabled = isEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElegantDarkPrimary,
                            contentColor = ElegantDarkOnPrimary,
                            disabledContainerColor = ElegantDarkBorder.copy(alpha = 0.5f),
                            disabledContentColor = ElegantDarkMutedText.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = if (isSignUpMode) {
                                "Register as ${if (isWorkerRole) "Professional" else "Client"}"
                            } else {
                                "Login as ${if (isWorkerRole) "Professional" else "Client"}"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = { isSignUpMode = !isSignUpMode }) {
                        Text(
                            text = if (isSignUpMode) "Already have an account? Login" else "Don't have an account? Sign up",
                            color = ElegantDarkPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = ElegantDarkBorder.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "  or connect with  ",
                            color = ElegantDarkMutedText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = ElegantDarkBorder.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Google Sign-In Button
                        OutlinedButton(
                            onClick = {
                                val email = if (isWorkerRole) "google.pro@skilljet.gh" else "google.client@gmail.com"
                                onLogin(email, isWorkerRole)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElegantDarkBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "G",
                                    color = Color(0xFF4285F4),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Google",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Apple Sign-In Button
                        OutlinedButton(
                            onClick = {
                                val email = if (isWorkerRole) "apple.pro@skilljet.gh" else "apple.client@icloud.com"
                                onLogin(email, isWorkerRole)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElegantDarkBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Apple",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Input Validation Helpers for Ghana and Security Formats ---

fun isValidEmail(email: String): Boolean {
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    return email.matches(emailRegex)
}

fun isValidGhanaPhone(phone: String): Boolean {
    val cleaned = phone.replace("\\s|-|\\(|\\)".toRegex(), "")
    // Ghana phone numbers are typically 10 digits starting with 0, or 12/13 digits with 233 country code
    val regex = "^(0|\\+233|233)?[0-9]{9}$".toRegex()
    return cleaned.matches(regex)
}

fun isValidPassword(password: String): Boolean {
    // Minimum 8 characters, at least 1 uppercase, 1 lowercase, 1 digit or special character
    return password.length >= 8 &&
           password.any { it.isUpperCase() } &&
           password.any { it.isLowerCase() } &&
           password.any { it.isDigit() || !it.isLetterOrDigit() }
}

@Composable
fun PasswordRequirementRow(text: String, isValid: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (isValid) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = null,
            tint = if (isValid) ElegantDarkPrimary else ElegantDarkMutedText.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            fontSize = 11.sp,
            color = if (isValid) Color.White else ElegantDarkMutedText
        )
    }
}

// --- MainScreen Layout with Tabs ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    user: UserEntity,
    viewModel: SkillJetViewModel
) {
    var selectedTab by remember { mutableStateOf("feed") }
    val unreadNotifsCount by viewModel.unreadCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Construction, contentDescription = null, tint = ElegantDarkPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SkillJet", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Log out", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ElegantDarkBg)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = ElegantDarkSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == "feed",
                    onClick = { selectedTab = "feed" },
                    icon = { Icon(if (selectedTab == "feed") Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Home") },
                    label = { Text("Feed") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElegantDarkPrimary,
                        selectedTextColor = ElegantDarkPrimary,
                        indicatorColor = ElegantDarkContainer,
                        unselectedIconColor = ElegantDarkMutedText,
                        unselectedTextColor = ElegantDarkMutedText
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == "get_help",
                    onClick = { selectedTab = "get_help" },
                    icon = { Icon(if (selectedTab == "get_help") Icons.Filled.AddCircle else Icons.Outlined.AddCircle, contentDescription = "Get Help") },
                    label = { Text("Get Help") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElegantDarkPrimary,
                        selectedTextColor = ElegantDarkPrimary,
                        indicatorColor = ElegantDarkContainer,
                        unselectedIconColor = ElegantDarkMutedText,
                        unselectedTextColor = ElegantDarkMutedText
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == "jobs",
                    onClick = { selectedTab = "jobs" },
                    icon = { Icon(if (selectedTab == "jobs") Icons.Filled.Work else Icons.Outlined.Work, contentDescription = "Jobs") },
                    label = { Text("Jobs Board") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElegantDarkPrimary,
                        selectedTextColor = ElegantDarkPrimary,
                        indicatorColor = ElegantDarkContainer,
                        unselectedIconColor = ElegantDarkMutedText,
                        unselectedTextColor = ElegantDarkMutedText
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == "ai_hub",
                    onClick = { selectedTab = "ai_hub" },
                    icon = {
                        BadgedBox(badge = {
                            if (unreadNotifsCount > 0) {
                                Badge(containerColor = GhanaRed) { Text(unreadNotifsCount.toString(), color = Color.White) }
                            }
                        }) {
                            Icon(if (selectedTab == "ai_hub") Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome, contentDescription = "AI Hub")
                        }
                    },
                    label = { Text("AI Hub") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElegantDarkPrimary,
                        selectedTextColor = ElegantDarkPrimary,
                        indicatorColor = ElegantDarkContainer,
                        unselectedIconColor = ElegantDarkMutedText,
                        unselectedTextColor = ElegantDarkMutedText
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == "profile",
                    onClick = { selectedTab = "profile" },
                    icon = { Icon(if (selectedTab == "profile") Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElegantDarkPrimary,
                        selectedTextColor = ElegantDarkPrimary,
                        indicatorColor = ElegantDarkContainer,
                        unselectedIconColor = ElegantDarkMutedText,
                        unselectedTextColor = ElegantDarkMutedText
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                "feed" -> FeedTab(user = user, viewModel = viewModel)
                "get_help" -> HelpTab(user = user, viewModel = viewModel, onNavigateToFeed = { selectedTab = "feed" })
                "jobs" -> JobsTab(user = user, viewModel = viewModel)
                "ai_hub" -> AiHubTab(user = user, viewModel = viewModel)
                "profile" -> ProfileTab(user = user, viewModel = viewModel)
            }
        }
    }
}

// --- Feed Tab Components ---

@Composable
fun FeedTab(
    user: UserEntity,
    viewModel: SkillJetViewModel
) {
    val feedList by viewModel.feedItems.collectAsState()
    var postText by remember { mutableStateOf("") }
    var postTags by remember { mutableStateOf("") }
    var showPostDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ElegantDarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ElegantDarkPrimaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Welcome back, ${user.name}!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text(
                            text = if (user.userType == "WORKER") "Browse local job requests and showcase your trade." else "Connect with expert verified local mechanics, plumbers, and builders.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    if (user.isAiVerified) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Filled.Verified, contentDescription = "Verified", tint = ElegantDarkSuccess, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        // Showcase Quick Post Box
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Share your trade update", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showPostDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElegantDarkPrimary, contentColor = ElegantDarkOnPrimary)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Write showcase update...")
                    }
                }
            }
        }

        // Feed content header
        item {
            Text("Trending Updates", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        }

        if (feedList.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No updates in the feed yet.", color = ElegantDarkMutedText)
                }
            }
        } else {
            items(feedList) { feed ->
                FeedCard(feed = feed)
            }
        }
    }

    if (showPostDialog) {
        Dialog(onDismissRequest = { showPostDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Create Showcase Post", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ElegantDarkPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = postText,
                        onValueChange = { postText = it },
                        placeholder = { Text("E.g., Just repaired a deep kitchen leak at Adenta, customer was super happy!") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElegantDarkPrimary,
                            focusedLabelColor = ElegantDarkPrimary,
                            unfocusedBorderColor = ElegantDarkBorder,
                            unfocusedLabelColor = ElegantDarkMutedText,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = ElegantDarkText
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = postTags,
                        onValueChange = { postTags = it },
                        placeholder = { Text("Tags (e.g. plumbing, adenta, pipes)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElegantDarkPrimary,
                            focusedLabelColor = ElegantDarkPrimary,
                            unfocusedBorderColor = ElegantDarkBorder,
                            unfocusedLabelColor = ElegantDarkMutedText,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = ElegantDarkText
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showPostDialog = false }) {
                            Text("Cancel", color = ElegantDarkMutedText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (postText.isNotBlank()) {
                                    val formattedTags = postTags.split(",")
                                        .joinToString(", ") { "#${it.trim().uppercase()}" }
                                    viewModel.addShowcasePost(postText, tags = formattedTags)
                                    postText = ""
                                    postTags = ""
                                    showPostDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElegantDarkPrimary, contentColor = ElegantDarkOnPrimary)
                        ) {
                            Text("Post")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeedCard(feed: FeedEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(ElegantDarkPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        feed.user.take(2).uppercase(),
                        color = ElegantDarkOnPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(feed.user, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        if (feed.rating > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Filled.Star, contentDescription = null, tint = ElegantDarkPrimary, modifier = Modifier.size(16.dp))
                            Text(feed.rating.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Text(feed.time, fontSize = 11.sp, color = ElegantDarkMutedText)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElegantDarkContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = feed.type.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (feed.type) {
                            "review" -> ElegantDarkSecondary
                            "job" -> ElegantDarkPrimary
                            else -> ElegantDarkSuccess
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body text
            Text(feed.text, fontSize = 14.sp, color = ElegantDarkText)

            if (!feed.image.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = feed.image,
                    contentDescription = "Showcase image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            if (feed.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Text(feed.tags, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ElegantDarkPrimary)
                }
            }
        }
    }
}

// --- Help Tab Components ---

@Composable
fun HelpTab(
    user: UserEntity,
    viewModel: SkillJetViewModel,
    onNavigateToFeed: () -> Unit
) {
    var jobTitle by remember { mutableStateOf("") }
    var selectedSkill by remember { mutableStateOf("mechanic") }
    var problemDesc by remember { mutableStateOf("") }
    var landmarkStr by remember { mutableStateOf("") }
    var selectedUrgency by remember { mutableStateOf("now") }
    var budgetValue by remember { mutableStateOf(200f) }

    val skills = listOf(
        "mechanic" to "🚗 Mechanic / Car Trouble",
        "plumber" to "🔧 Plumber / Pipe Burst",
        "electrician" to "⚡ Electrician / Power Issue",
        "cvwriter" to "📄 CV Writing & Application",
        "tech" to "📱 Phone & IT Support",
        "tailor" to "🧵 Tailor / Clothing",
        "builder" to "🏗️ Builder / Masonry"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ElegantDarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ElegantDarkPrimaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Request Assistance Now", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                    Text("Describe what you need and local skilled workers will be alerted.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Job Details", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = jobTitle,
                        onValueChange = { jobTitle = it },
                        label = { Text("What do you need help with?") },
                        placeholder = { Text("E.g., Fix broken kitchen pipe") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElegantDarkPrimary,
                            focusedLabelColor = ElegantDarkPrimary,
                            unfocusedBorderColor = ElegantDarkBorder,
                            unfocusedLabelColor = ElegantDarkMutedText,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = ElegantDarkText
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Required Skill", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ElegantDarkMutedText)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Skill dropdown replacement
                    Column {
                        var expanded by remember { mutableStateOf(false) }
                        val currentText = skills.find { it.first == selectedSkill }?.second ?: "Select skill..."
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ElegantDarkBorder, RoundedCornerShape(4.dp))
                                .clickable { expanded = !expanded }
                                .padding(12.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(currentText, color = Color.White)
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White)
                            }
                        }
                        DropdownMenu(
                            expanded = expanded, 
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(ElegantDarkSurface)
                        ) {
                            skills.forEach { skill ->
                                DropdownMenuItem(
                                    text = { Text(skill.second, color = Color.White) },
                                    onClick = {
                                        selectedSkill = skill.first
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = landmarkStr,
                        onValueChange = { landmarkStr = it },
                        label = { Text("Where are you? (Use a Landmark)") },
                        placeholder = { Text("E.g., Behind Adenta Melcom, near Vodafone booth") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElegantDarkPrimary,
                            focusedLabelColor = ElegantDarkPrimary,
                            unfocusedBorderColor = ElegantDarkBorder,
                            unfocusedLabelColor = ElegantDarkMutedText,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = ElegantDarkText
                        )
                    )
                    Text("Don't give full addresses. Landmarks help workers locate you safely.", fontSize = 11.sp, color = ElegantDarkMutedText, modifier = Modifier.padding(top = 4.dp))

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = problemDesc,
                        onValueChange = { problemDesc = it },
                        label = { Text("Describe the problem in detail") },
                        placeholder = { Text("My kitchen sink pipe burst and water is leaking everywhere. I need it sealed immediately.") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElegantDarkPrimary,
                            focusedLabelColor = ElegantDarkPrimary,
                            unfocusedBorderColor = ElegantDarkBorder,
                            unfocusedLabelColor = ElegantDarkMutedText,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = ElegantDarkText
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Urgency Level", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ElegantDarkMutedText)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("now" to "⚡ NOW", "today" to "⏱ Today", "scheduled" to "📅 Scheduled").forEach { urgency ->
                            val isSelected = selectedUrgency == urgency.first
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, if (isSelected) ElegantDarkPrimary else ElegantDarkBorder, RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ElegantDarkContainer else Color.Transparent)
                                    .clickable { selectedUrgency = urgency.first }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(urgency.second, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) ElegantDarkPrimary else ElegantDarkMutedText)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Your Budget", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        Text("₵${budgetValue.toInt()}", fontWeight = FontWeight.Bold, color = ElegantDarkPrimary, fontSize = 14.sp)
                    }
                    Slider(
                        value = budgetValue,
                        onValueChange = { budgetValue = it },
                        valueRange = 50f..1000f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = ElegantDarkPrimary,
                            activeTrackColor = ElegantDarkPrimary,
                            inactiveTrackColor = ElegantDarkBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (jobTitle.isNotBlank() && landmarkStr.isNotBlank() && problemDesc.isNotBlank()) {
                                viewModel.postJob(
                                    title = jobTitle,
                                    skill = selectedSkill,
                                    description = problemDesc,
                                    landmark = landmarkStr,
                                    urgency = selectedUrgency,
                                    budget = budgetValue.toInt()
                                )
                                // Reset fields
                                jobTitle = ""
                                problemDesc = ""
                                landmarkStr = ""
                                onNavigateToFeed()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElegantDarkPrimary, contentColor = ElegantDarkOnPrimary)
                    ) {
                        Text("Find Skilled Help Now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// --- Jobs Tab Board ---

@Composable
fun JobsTab(
    user: UserEntity,
    viewModel: SkillJetViewModel
) {
    val jobsList by viewModel.openJobs.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredJobs = jobsList.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
        it.description.contains(searchQuery, ignoreCase = true) ||
        it.landmark.contains(searchQuery, ignoreCase = true) ||
        it.skill.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ElegantDarkBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Available Job Board",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Filled.FilterList, contentDescription = "Filter", tint = Color.White)
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by skill, location, or keyword...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = ElegantDarkMutedText) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElegantDarkPrimary,
                focusedLabelColor = ElegantDarkPrimary,
                unfocusedBorderColor = ElegantDarkBorder,
                unfocusedLabelColor = ElegantDarkMutedText,
                focusedTextColor = Color.White,
                unfocusedTextColor = ElegantDarkText
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredJobs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = ElegantDarkMutedText)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No jobs match your search parameters.", color = ElegantDarkMutedText)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredJobs) { job ->
                    JobCard(job = job, user = user, onAccept = { viewModel.acceptJob(job.id) })
                }
            }
        }
    }
}

@Composable
fun JobCard(
    job: JobEntity,
    user: UserEntity,
    onAccept: () -> Unit
) {
    val isWorker = user.userType == "WORKER"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                val emoji = when (job.skill) {
                    "mechanic" -> "🚗"
                    "plumber" -> "🔧"
                    "electrician" -> "⚡"
                    "tailor" -> "🧵"
                    "builder" -> "🏗️"
                    else -> "🛠️"
                }
                Text("$emoji ${job.title}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                
                Box(
                    modifier = Modifier
                        .background(if (job.urgency == "now") ElegantDarkPrimaryContainer else ElegantDarkContainer, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = job.urgency.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = if (job.urgency == "now") ElegantDarkPrimary else ElegantDarkSuccess
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(job.description, fontSize = 13.sp, color = ElegantDarkText)
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Place, contentDescription = null, tint = ElegantDarkPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(job.landmark, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ElegantDarkMutedText)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = ElegantDarkBorder)
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Budget Offered", fontSize = 10.sp, color = ElegantDarkMutedText)
                    Text("₵${job.budget}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ElegantDarkPrimary)
                }
                if (isWorker) {
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = ElegantDarkPrimary, contentColor = ElegantDarkOnPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Accept Job")
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(ElegantDarkContainer, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Posted by ${job.clientName}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ElegantDarkPrimary)
                    }
                }
            }
        }
    }
}

// --- AI Hub (Gemini Studio & Alerts) ---

@Composable
fun AiHubTab(
    user: UserEntity,
    viewModel: SkillJetViewModel
) {
    var activeSubTab by remember { mutableStateOf("chat") }

    Column(modifier = Modifier.fillMaxSize().background(ElegantDarkBg)) {
        // Nested tabs navigation
        TabRow(
            selectedTabIndex = when (activeSubTab) {
                "chat" -> 0
                "verify" -> 1
                "creative" -> 2
                else -> 3
            },
            containerColor = ElegantDarkSurface,
            contentColor = ElegantDarkPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[when (activeSubTab) {
                        "chat" -> 0
                        "verify" -> 1
                        "creative" -> 2
                        else -> 3
                    }]),
                    color = ElegantDarkPrimary
                )
            }
        ) {
            Tab(selected = activeSubTab == "chat", onClick = { activeSubTab = "chat" }) {
                Text("Advisor Chat", modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (activeSubTab == "chat") ElegantDarkPrimary else ElegantDarkMutedText)
            }
            Tab(selected = activeSubTab == "verify", onClick = { activeSubTab = "verify" }) {
                Text("Verification", modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (activeSubTab == "verify") ElegantDarkPrimary else ElegantDarkMutedText)
            }
            Tab(selected = activeSubTab == "creative", onClick = { activeSubTab = "creative" }) {
                Text("Promo Studio", modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (activeSubTab == "creative") ElegantDarkPrimary else ElegantDarkMutedText)
            }
            Tab(selected = activeSubTab == "alerts", onClick = { activeSubTab = "alerts" }) {
                Text("Alerts", modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (activeSubTab == "alerts") ElegantDarkPrimary else ElegantDarkMutedText)
            }
        }

        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when (activeSubTab) {
                "chat" -> AdvisorChatSection(viewModel)
                "verify" -> VerificationSection(user, viewModel)
                "creative" -> CreativePromoSection(viewModel)
                "alerts" -> AlertsSection(viewModel)
            }
        }
    }
}

@Composable
fun AdvisorChatSection(viewModel: SkillJetViewModel) {
    val aiState by viewModel.aiState.collectAsState()
    var userPrompt by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ElegantDarkPrimaryContainer)
        ) {
            Text(
                "You are connected with SkillJet AI Trade Mentor. Ask technical repair advice, pricing questions, or client communication tips.",
                fontSize = 11.sp,
                color = Color.White,
                modifier = Modifier.padding(12.dp),
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (aiState.chatHistory.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.QuestionAnswer, contentDescription = null, modifier = Modifier.size(48.dp), tint = ElegantDarkMutedText)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("How can I help you today?", color = ElegantDarkMutedText, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(aiState.chatHistory) { msg ->
                    val isUser = msg.sender == "USER"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 0.dp,
                                bottomEnd = if (isUser) 0.dp else 16.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUser) ElegantDarkPrimary else ElegantDarkSurface
                            ),
                            modifier = Modifier.widthIn(max = 280.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Text(
                                text = msg.text,
                                modifier = Modifier.padding(12.dp),
                                color = if (isUser) ElegantDarkOnPrimary else Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            if (aiState.chatLoading) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface),
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ElegantDarkPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Thinking...", fontSize = 12.sp, color = ElegantDarkMutedText)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = userPrompt,
                onValueChange = { userPrompt = it },
                placeholder = { Text("Ask Gemini AI Trade advisor...") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElegantDarkPrimary,
                    focusedLabelColor = ElegantDarkPrimary,
                    unfocusedBorderColor = ElegantDarkBorder,
                    unfocusedLabelColor = ElegantDarkMutedText,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = ElegantDarkText
                ),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (userPrompt.isNotBlank()) {
                        viewModel.sendChatMessage(userPrompt)
                        userPrompt = ""
                        coroutineScope.launch {
                            listState.animateScrollToItem(if (aiState.chatHistory.isNotEmpty()) aiState.chatHistory.size else 0)
                        }
                    }
                },
                modifier = Modifier.background(ElegantDarkPrimary, CircleShape).size(48.dp)
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = ElegantDarkOnPrimary)
            }
        }
    }
}

@Composable
fun VerificationSection(user: UserEntity, viewModel: SkillJetViewModel) {
    val aiState by viewModel.aiState.collectAsState()
    var landmarkInput by remember { mutableStateOf("") }
    var detailInput by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("AI-Verified Trade Status", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ElegantDarkPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(if (user.isAiVerified) ElegantDarkPrimaryContainer else ElegantDarkContainer, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (user.isAiVerified) "VERIFIED" else "UNVERIFIED",
                                fontWeight = FontWeight.Bold,
                                color = if (user.isAiVerified) ElegantDarkPrimary else ElegantDarkMutedText
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Verify your craft using photo/video submissions analysed by Gemini Pro.", fontSize = 11.sp, color = ElegantDarkMutedText, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Submit for Verification", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = landmarkInput,
                        onValueChange = { landmarkInput = it },
                        label = { Text("Craft or Landmark Name") },
                        placeholder = { Text("E.g., Adenta Junction Mechanic Workshop") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElegantDarkPrimary,
                            focusedLabelColor = ElegantDarkPrimary,
                            unfocusedBorderColor = ElegantDarkBorder,
                            unfocusedLabelColor = ElegantDarkMutedText,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = ElegantDarkText
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = detailInput,
                        onValueChange = { detailInput = it },
                        label = { Text("Explain your verification/landmark details") },
                        placeholder = { Text("Details of your apprentice masters or workshop verification...") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElegantDarkPrimary,
                            focusedLabelColor = ElegantDarkPrimary,
                            unfocusedBorderColor = ElegantDarkBorder,
                            unfocusedLabelColor = ElegantDarkMutedText,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = ElegantDarkText
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.verifyWorkerCraft(landmarkInput, detailInput, null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElegantDarkPrimary, contentColor = ElegantDarkOnPrimary)
                    ) {
                        Icon(Icons.Filled.BatchPrediction, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyze & Verify with Gemini Pro")
                    }
                }
            }
        }

        if (aiState.verificationStatus != "NOT_STARTED") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Gemini Verification Report:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .background(
                                        when (aiState.verificationStatus) {
                                            "PENDING" -> ElegantDarkContainer
                                            "PASSED" -> ElegantDarkPrimaryContainer
                                            else -> ElegantDarkContainer
                                        },
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    aiState.verificationStatus,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (aiState.verificationStatus) {
                                        "PENDING" -> ElegantDarkSecondary
                                        "PASSED" -> ElegantDarkPrimary
                                        else -> GhanaRed
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(aiState.verificationReport, fontSize = 13.sp, color = ElegantDarkText)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreativePromoSection(viewModel: SkillJetViewModel) {
    val aiState by viewModel.aiState.collectAsState()
    var promoPrompt by remember { mutableStateOf("") }
    var selectedRatio by remember { mutableStateOf("1:1") }
    var selectedSize by remember { mutableStateOf("1K") }
    var useProModel by remember { mutableStateOf(false) }

    val ratios = listOf("1:1", "16:9", "9:16", "3:2", "4:3")
    val sizes = listOf("1K", "2K", "4K")

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ElegantDarkPrimaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("AI Promotional Art Studio", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Text("Workers can generate beautiful high-quality flyers & promos to share on their feeds. Powered by Veo & Gemini Image studio.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = promoPrompt,
                        onValueChange = { promoPrompt = it },
                        label = { Text("Art Prompt") },
                        placeholder = { Text("E.g., A professional plumber repairing copper pipe, vibrant Ghanaian workshop") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElegantDarkPrimary,
                            focusedLabelColor = ElegantDarkPrimary,
                            unfocusedBorderColor = ElegantDarkBorder,
                            unfocusedLabelColor = ElegantDarkMutedText,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = ElegantDarkText
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Aspect Ratio", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElegantDarkMutedText)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ratios.forEach { ratio ->
                            val isSelected = selectedRatio == ratio
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .border(1.dp, if (isSelected) ElegantDarkPrimary else ElegantDarkBorder, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) ElegantDarkContainer else Color.Transparent)
                                    .clickable { selectedRatio = ratio }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(ratio, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) ElegantDarkPrimary else ElegantDarkMutedText)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Image Resolution: ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElegantDarkMutedText)
                        Spacer(modifier = Modifier.width(8.dp))
                        sizes.forEach { size ->
                            val isSelected = selectedSize == size
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .border(1.dp, if (isSelected) ElegantDarkPrimary else ElegantDarkBorder, RoundedCornerShape(4.dp))
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) ElegantDarkContainer else Color.Transparent)
                                    .clickable { selectedSize = size }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(size, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) ElegantDarkPrimary else ElegantDarkMutedText)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = useProModel,
                            onCheckedChange = { useProModel = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = ElegantDarkPrimary,
                                uncheckedColor = ElegantDarkBorder,
                                checkmarkColor = ElegantDarkOnPrimary
                            )
                        )
                        Text("Use Studio-Quality Gemini Pro Image Model", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.generateStudioPromo(promoPrompt, selectedRatio, selectedSize, useProModel) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ElegantDarkPrimary, contentColor = ElegantDarkOnPrimary)
                        ) {
                            Text("Generate Image")
                        }
                        Button(
                            onClick = { viewModel.generateStudioPromoVideo(promoPrompt, selectedRatio) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ElegantDarkSecondary, contentColor = ElegantDarkOnPrimary)
                        ) {
                            Text("Generate Video")
                        }
                    }
                }
            }
        }

        if (aiState.creativeLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ElegantDarkPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Generating beautiful art banner...", color = ElegantDarkMutedText)
                    }
                }
            }
        }

        if (aiState.generatedImageBitmap != null) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Generated Asset Preview:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            bitmap = aiState.generatedImageBitmap!!.asImageBitmap(),
                            contentDescription = "Promo Banner",
                            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        if (!aiState.generatedVideoMsg.isNullOrEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Creative Studio Feedback:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(aiState.generatedVideoMsg!!, fontSize = 13.sp, color = ElegantDarkText)
                    }
                }
            }
        }
    }
}

@Composable
fun AlertsSection(viewModel: SkillJetViewModel) {
    val alerts by viewModel.notifications.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(ElegantDarkBg).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Your Job Alerts", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White, modifier = Modifier.weight(1f))
            TextButton(onClick = { viewModel.markNotificationsAsRead() }) {
                Text("Mark All Read", color = ElegantDarkPrimary)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (alerts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.NotificationsOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = ElegantDarkMutedText)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("You have no unread job alerts.", color = ElegantDarkMutedText)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                items(alerts) { alert ->
                    AlertCard(alert = alert)
                }
            }
        }
    }
}

@Composable
fun AlertCard(alert: NotificationEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.isRead) ElegantDarkSurface else ElegantDarkPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(alert.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                Text(alert.time, fontSize = 10.sp, color = ElegantDarkMutedText)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(alert.message, fontSize = 12.sp, color = ElegantDarkText)
        }
    }
}

// --- Profile Tab Components ---

@Composable
fun ProfileTab(
    user: UserEntity,
    viewModel: SkillJetViewModel
) {
    // We mock reviews as in the HTML template for high visual quality
    val reviews = listOf(
        Triple("Ama Serwaa", 5, "Kwame is simply the best mechanic in Accra! He came to fix my car when I was stranded at Circle."),
        Triple("Kofi Ansah", 5, "I've used Kwame's services three times now. Honest, fairly priced, quality work."),
        Triple("Abena Nyarko", 4, "Fixed my car's electrical issue. Professional and fair price. Arrived a bit late though.")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ElegantDarkBg)
    ) {
        // Hero Header Background
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(ElegantDarkSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .border(4.dp, ElegantDarkPrimary, CircleShape)
                            .background(ElegantDarkContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            user.name.take(2).uppercase(),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElegantDarkPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(user.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Place, contentDescription = null, tint = ElegantDarkPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(user.location, color = ElegantDarkMutedText, fontSize = 12.sp)
                    }
                }
            }
        }

        // Ratings & Badges
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Rating", fontSize = 10.sp, color = ElegantDarkMutedText)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = ElegantDarkSecondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${user.rating}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }
                    }
                }
                Card(
                    modifier = Modifier.weight(1.3f),
                    colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("AI-Verification Status", fontSize = 10.sp, color = ElegantDarkMutedText)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (user.isAiVerified) Icons.Filled.Verified else Icons.Filled.NewReleases,
                                contentDescription = null,
                                tint = if (user.isAiVerified) ElegantDarkPrimary else ElegantDarkMutedText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (user.isAiVerified) "AI-Verified" else "Not Verified",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (user.isAiVerified) ElegantDarkPrimary else ElegantDarkMutedText
                            )
                        }
                    }
                }
            }
        }

        // Skills & Trades
        if (user.userType == "WORKER" && user.skills.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Skills & Services", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            user.skills.split(",").forEach { skill ->
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .background(ElegantDarkContainer, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(skill.trim(), color = ElegantDarkPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Client Contact Cards
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Contact Details", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Phone, contentDescription = null, tint = ElegantDarkPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(user.phone, fontSize = 13.sp, color = ElegantDarkText)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Email, contentDescription = null, tint = ElegantDarkPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(user.email, fontSize = 13.sp, color = ElegantDarkText)
                    }
                }
            }
        }

        // App Walkthrough / Onboarding Launcher Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { viewModel.startOnboarding() },
                colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElegantDarkPrimary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "Walkthrough",
                        tint = ElegantDarkPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("View App Walkthrough", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        Text("Review onboarding sliders, platform goals, and AI assistant features.", fontSize = 11.sp, color = ElegantDarkMutedText)
                    }
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = ElegantDarkMutedText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Reviews section
        if (user.userType == "WORKER") {
            item {
                Text("Client Reviews", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Color.White)
            }

            items(reviews) { review ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(ElegantDarkPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(review.first.take(1), color = ElegantDarkOnPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(review.first, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Row {
                                repeat(review.second) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = ElegantDarkSecondary, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(review.third, fontSize = 12.sp, color = ElegantDarkText)
                    }
                }
            }
        }
    }
}
