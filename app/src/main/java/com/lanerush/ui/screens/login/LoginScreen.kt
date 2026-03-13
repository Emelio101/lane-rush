package com.lanerush.ui.screens.login

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lanerush.R
import com.lanerush.ui.theme.LaneRushTheme

// ═══════════════════════════════════════════════════════════════════════════
//  ENTRY COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onAuthSuccess: () -> Unit
) {
    val isLoading     by viewModel.isLoading.collectAsState()
    val errorMessage  by viewModel.errorMessage.collectAsState()
    val context        = LocalContext.current
    val snackbarState  = remember { SnackbarHostState() }

    // Collect one-shot navigation event from ViewModel
    LaunchedEffect(Unit) {
        viewModel.navigateToHome.collect { onAuthSuccess() }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarState.showSnackbar(it); viewModel.clearError() }
    }

    LoginScreenContent(
        isLoading          = isLoading,
        snackbarHostState  = snackbarState,
        onSignInWithGoogle = { viewModel.signInWithGoogle(context) },
        onSignInWithEmail  = { e, p -> viewModel.signInWithEmail(e, p) },
        onSignUpWithEmail  = { e, p, n -> viewModel.signUpWithEmail(e, p, n) }
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  MAIN CONTENT
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun LoginScreenContent(
    isLoading: Boolean,
    snackbarHostState: SnackbarHostState,
    onSignInWithGoogle: () -> Unit,
    onSignInWithEmail: (String, String) -> Unit,
    onSignUpWithEmail: (String, String, String) -> Unit
) {
    val inf = rememberInfiniteTransition(label = "bg")
    val glowX by inf.animateFloat(
        0.3f, 0.7f,
        infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse), "gx"
    )
    val glowPulse by inf.animateFloat(
        0.6f, 1f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), "gp"
    )

    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            BackgroundCanvas(glowX, glowPulse)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(80.dp))

                LogoBlock(glowPulse)

                Spacer(Modifier.height(52.dp))

                AuthSection(
                    isLoading          = isLoading,
                    onSignInWithGoogle = onSignInWithGoogle,
                    onSignInWithEmail  = onSignInWithEmail,
                    onSignUpWithEmail  = onSignUpWithEmail
                )

                Spacer(Modifier.height(48.dp))
            }

            // ── Loading blocker ────────────────────────────────────────────
            if (isLoading) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(enabled = false) {},
                    Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color       = colorScheme.primary,
                            strokeWidth = 3.dp,
                            modifier    = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Authenticating…", color = colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  BACKGROUND  – animated ambient glow
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun BackgroundCanvas(glowX: Float, glowPulse: Float) {
    val colorScheme = MaterialTheme.colorScheme
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        // Base gradient
        drawRect(
            Brush.verticalGradient(listOf(colorScheme.background, colorScheme.surfaceVariant))
        )
        // Ambient top glow blob
        drawCircle(
            Brush.radialGradient(
                listOf(colorScheme.primary.copy(alpha = 0.07f * glowPulse), Color.Transparent),
                Offset(size.width * glowX, size.height * 0.15f),
                size.width * 0.7f
            ),
            size.width * 0.7f,
            Offset(size.width * glowX, size.height * 0.15f)
        )
        // Bottom accent
        drawCircle(
            Brush.radialGradient(
                listOf(colorScheme.secondary.copy(alpha = 0.04f), Color.Transparent),
                Offset(size.width * 0.5f, size.height * 0.85f),
                size.width * 0.6f
            ),
            size.width * 0.6f,
            Offset(size.width * 0.5f, size.height * 0.85f)
        )
        // Subtle road-lane lines at bottom
        val laneW = size.width / 3f
        for (i in 1..2) {
            drawLine(
                colorScheme.onBackground.copy(alpha = 0.04f),
                Offset(i * laneW, size.height * 0.6f),
                Offset(i * laneW, size.height),
                2f
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  LOGO  – "LANE / RUSH" with cyan underline accent
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun LogoBlock(glowPulse: Float) {
    val colorScheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Speed line ornament above
        Row(
            Modifier.width(160.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f).height(1.dp).background(colorScheme.primary.copy(alpha = 0.4f)))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(5.dp).clip(RoundedCornerShape(50)).background(colorScheme.primary))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f).height(1.dp).background(colorScheme.primary.copy(alpha = 0.4f)))
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "LANE",
            fontSize      = 58.sp,
            fontWeight    = FontWeight.Black,
            color         = colorScheme.onBackground,
            letterSpacing = 14.sp
        )

        // "RUSH" with gold fill + cyan glow underline
        Box(contentAlignment = Alignment.BottomCenter) {
            Text(
                "RUSH",
                fontSize      = 76.sp,
                fontWeight    = FontWeight.Black,
                color         = colorScheme.secondary,
                letterSpacing = 8.sp
            )
            // Underline glow
            Spacer(
                Modifier
                    .width(160.dp)
                    .height(3.dp)
                    .offset(y = 4.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, colorScheme.primary.copy(alpha = glowPulse), Color.Transparent)
                        ),
                        RoundedCornerShape(50)
                    )
            )
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "STREET RACING CHAMPIONSHIP",
            fontSize      = 10.sp,
            fontWeight    = FontWeight.Medium,
            color         = colorScheme.onSurfaceVariant,
            letterSpacing = 3.sp
        )
    }
}


// ═══════════════════════════════════════════════════════════════════════════
//  AUTH SECTION  (sign in / sign up)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun AuthSection(
    isLoading: Boolean,
    onSignInWithGoogle: () -> Unit,
    onSignInWithEmail: (String, String) -> Unit,
    onSignUpWithEmail: (String, String, String) -> Unit
) {
    var isSignUp          by remember { mutableStateOf(false) }
    var email             by remember { mutableStateOf("") }
    var password          by remember { mutableStateOf("") }
    var name              by remember { mutableStateOf("") }
    var passwordVisible   by remember { mutableStateOf(false) }
    var agreedToTerms     by remember { mutableStateOf(false) }
    var showTermsDialog   by remember { mutableStateOf(false) }

    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colorScheme.surface)
            .drawBehind {
                drawRoundRect(
                    colorScheme.onSurface.copy(alpha = 0.1f),
                    cornerRadius = CornerRadius(24.dp.toPx()),
                    style        = Stroke(1.dp.toPx())
                )
            }
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tab row: Sign In / Sign Up
        AuthTabRow(isSignUp = isSignUp, onToggle = { isSignUp = it })

        Spacer(Modifier.height(24.dp))

        // Name field (sign-up only)
        AnimatedVisibility(
            visible = isSignUp,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut()
        ) {
            Column {
                RaceTextField(
                    value       = name,
                    onValueChange = { name = it },
                    label       = "Racing Name",
                    icon        = Icons.Default.Person,
                    imeAction   = ImeAction.Next,
                    enabled     = !isLoading
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        RaceTextField(
            value         = email,
            onValueChange = { email = it },
            label         = "Email Address",
            icon          = Icons.Default.Email,
            keyboardType  = KeyboardType.Email,
            imeAction     = ImeAction.Next,
            enabled       = !isLoading
        )

        Spacer(Modifier.height(10.dp))

        RaceTextField(
            value             = password,
            onValueChange     = { password = it },
            label             = "Password",
            icon              = Icons.Default.Lock,
            keyboardType      = KeyboardType.Password,
            imeAction         = ImeAction.Done,
            enabled           = !isLoading,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon      = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        null, tint = colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        // Terms checkbox
        AnimatedVisibility(visible = isSignUp, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column {
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked         = agreedToTerms,
                        onCheckedChange = { agreedToTerms = it },
                        colors          = CheckboxDefaults.colors(
                            checkedColor   = colorScheme.secondary,
                            uncheckedColor = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            checkmarkColor = colorScheme.onSecondary
                        ),
                        enabled         = !isLoading
                    )
                    Spacer(Modifier.width(6.dp))
                    val annotated = buildAnnotatedString {
                        append("I agree to the ")
                        withStyle(
                            SpanStyle(
                                color = colorScheme.secondary,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append("Terms & Conditions")
                        }
                    }
                    Text(
                        annotated,
                        color    = colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { showTermsDialog = true }
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Primary action button
        Button(
            onClick  = {
                if (isSignUp) { if (agreedToTerms) onSignUpWithEmail(email, password, name) }
                else          { onSignInWithEmail(email, password) }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = colorScheme.secondary),
            shape    = RoundedCornerShape(14.dp),
            enabled  = !isLoading && (!isSignUp || agreedToTerms)
        ) {
            Text(
                if (isSignUp) "CREATE ACCOUNT" else "SIGN IN",
                color      = colorScheme.onSecondary,
                fontWeight = FontWeight.Black,
                fontSize   = 15.sp,
                letterSpacing = 1.5.sp
            )
        }

        Spacer(Modifier.height(20.dp))

        // OR divider
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(1.dp).background(colorScheme.onSurface.copy(alpha = 0.1f)))
            Text("  OR  ", color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 11.sp, letterSpacing = 1.sp)
            Box(Modifier.weight(1f).height(1.dp).background(colorScheme.onSurface.copy(alpha = 0.1f)))
        }

        Spacer(Modifier.height(16.dp))

        // Google button
        OutlinedButton(
            onClick  = onSignInWithGoogle,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            border   = BorderStroke(1.dp, colorScheme.onSurface.copy(alpha = 0.1f)),
            colors   = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor   = colorScheme.onSurface
            ),
            enabled  = !isLoading
        ) {
            Image(
                painter           = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = null,
                modifier          = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text("Continue with Google", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "By continuing, you agree to Lane Rush's Privacy Policy.",
            color     = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            fontSize  = 10.sp,
            textAlign = TextAlign.Center
        )
    }

    if (showTermsDialog) {
        TermsDialog(onDismiss = { })
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  AUTH TAB ROW  (Sign In / Sign Up toggle)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun AuthTabRow(isSignUp: Boolean, onToggle: (Boolean) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(false to "SIGN IN", true to "SIGN UP").forEach { (tabIsSignUp, label) ->
            val selected = isSignUp == tabIsSignUp
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(3.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) colorScheme.surfaceVariant else Color.Transparent)
                    .drawBehind {
                        if (selected) drawRoundRect(
                            colorScheme.primary.copy(alpha = 0.3f),
                            cornerRadius = CornerRadius(10.dp.toPx()),
                            style        = Stroke(1.dp.toPx())
                        )
                    }
                    .clickable { onToggle(tabIsSignUp) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color         = if (selected) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                    fontSize      = 12.sp,
                    fontWeight    = if (selected) FontWeight.Bold else FontWeight.Normal,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  RACE TEXT FIELD  (custom styled OutlinedTextField)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun RaceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    OutlinedTextField(
        value               = value,
        onValueChange       = onValueChange,
        label               = { Text(label, fontSize = 12.sp) },
        modifier            = Modifier.fillMaxWidth(),
        singleLine          = true,
        enabled             = enabled,
        visualTransformation = visualTransformation,
        keyboardOptions     = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        leadingIcon         = { Icon(icon, null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
        trailingIcon        = trailingIcon,
        colors              = OutlinedTextFieldDefaults.colors(
            focusedTextColor       = colorScheme.onSurface,
            unfocusedTextColor     = colorScheme.onSurface,
            disabledTextColor      = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            focusedContainerColor  = colorScheme.surfaceVariant.copy(alpha = 0.3f),
            unfocusedContainerColor= colorScheme.surfaceVariant.copy(alpha = 0.3f),
            focusedBorderColor     = colorScheme.primary,
            unfocusedBorderColor   = colorScheme.onSurface.copy(alpha = 0.1f),
            focusedLabelColor      = colorScheme.primary,
            unfocusedLabelColor    = colorScheme.onSurfaceVariant,
            cursorColor            = colorScheme.primary
        ),
        shape               = RoundedCornerShape(12.dp)
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  TERMS DIALOG
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun TermsDialog(onDismiss: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colorScheme.surface,
            modifier = Modifier.drawBehind {
                drawRoundRect(
                    colorScheme.secondary.copy(alpha = 0.3f),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style        = Stroke(1.5.dp.toPx())
                )
            }
        ) {
            Column(
                Modifier
                    .padding(28.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📜", fontSize = 32.sp)
                Spacer(Modifier.height(8.dp))
                Text("Terms & Conditions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colorScheme.secondary)
                Spacer(Modifier.height(16.dp))
                Text(
                    """1. Acceptance of Terms
By playing Lane Rush, you agree to these terms.

2. Fair Play
Any form of cheating or exploitation is prohibited.

3. Privacy
Your data is used for leaderboard and authentication purposes only.

4. Account Security
You are responsible for your account's security.

5. Updates
We may update these terms periodically.""",
                    color      = colorScheme.onSurfaceVariant,
                    fontSize   = 13.sp,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick  = onDismiss,
                    colors   = ButtonDefaults.buttonColors(containerColor = colorScheme.secondary),
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("I UNDERSTAND", color = colorScheme.onSecondary, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  PREVIEW
// ═══════════════════════════════════════════════════════════════════════════
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LaneRushTheme {
        LoginScreenContent(
            isLoading          = false,
            snackbarHostState  = remember { SnackbarHostState() },
            onSignInWithGoogle = {},
            onSignInWithEmail  = { _, _ -> },
            onSignUpWithEmail  = { _, _, _ -> }
        )
    }
}