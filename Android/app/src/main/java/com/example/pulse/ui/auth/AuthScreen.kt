package com.example.pulse.ui.auth

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

// ─── Colour palette — zero white ─────────────────────────────────────────────
private val BgBase        = Color(0xFF030A1E)   // near-black navy
private val BgMid         = Color(0xFF071233)   // dark indigo
private val BgTop         = Color(0xFF0D2473)   // deep blue
private val WaveDeep      = Color(0xFF122B6E)   // wave fill 1
private val WaveMid       = Color(0xFF1A3A8F)   // wave fill 2
private val WaveShimmer   = Color(0xFF4D7FE8)   // shimmer line
private val AccentCyan    = Color(0xFF38BDF8)   // icon / active accent
private val AccentBlue    = Color(0xFF3B82F6)
private val MidBlue       = Color(0xFF1D4ED8)
private val CardSurface   = Color(0xFF0B1740)   // dark glass card base
private val CardBorder    = Color(0xFF2A4BAD)   // card glass border
private val FieldSurface  = Color(0xFF0F1E52)   // text-field background
private val FieldBorder   = Color(0xFF1E3A8A)   // text-field border unfocused
private val TextPrimary   = Color(0xFFE8EEFF)   // primary text on dark
private val TextSecondary = Color(0xFF8FA8D8)   // secondary / label
private val TextHint      = Color(0xFF4A6AA0)   // placeholder / hint
private val ErrorBg       = Color(0xFF1A0A0A)
private val ErrorText     = Color(0xFFFF6B6B)

// ─── Wave helper ─────────────────────────────────────────────────────────────
private fun DrawScope.drawWave(
    phase: Float,
    amplitudeRatio: Float,
    yBaseRatio: Float,
    color: Color,
    filled: Boolean = true,
    strokePx: Float = 6f
) {
    val w = size.width
    val h = size.height
    val amp  = h * amplitudeRatio
    val yBase = h * yBaseRatio
    val path = Path()
    val steps = 512
    for (i in 0..steps) {
        val x = w * i / steps.toFloat()
        val y = yBase + amp * sin((2.0 * PI * x / w + phase).toFloat())
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    if (filled) {
        path.lineTo(w, h); path.lineTo(0f, h); path.close()
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(color, color.copy(alpha = 0f)),
                startY = yBase - amp, endY = h
            )
        )
    } else {
        drawPath(path = path, color = color,
            style = Stroke(width = strokePx, cap = StrokeCap.Round))
    }
}

// ─── Animated background (all dark — no white) ───────────────────────────────
@Composable
private fun DarkWaveBackground() {
    val tr = rememberInfiniteTransition(label = "waves")
    val p1 by tr.animateFloat(0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(4500, easing = LinearEasing), RepeatMode.Restart), "p1")
    val p2 by tr.animateFloat(PI.toFloat(), (3 * PI).toFloat(),
        infiniteRepeatable(tween(6200, easing = LinearEasing), RepeatMode.Restart), "p2")
    val p3 by tr.animateFloat((0.5 * PI).toFloat(), (2.5 * PI).toFloat(),
        infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart), "p3")
    val glow by tr.animateFloat(0.18f, 0.38f,
        infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse), "glow")

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height

        // Full-screen dark gradient — bottom stays dark navy, never white
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(BgTop, BgMid, BgBase, BgBase),
                startY = 0f, endY = h
            )
        )

        // Radial glow orb — muted, top-centre
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AccentBlue.copy(alpha = glow), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.10f),
                radius = w * 0.70f
            ),
            radius = w * 0.70f,
            center = Offset(w * 0.5f, h * 0.10f)
        )

        // Wave 1 — deepest fill
        drawWave(p1, 0.048f, 0.46f, WaveDeep.copy(alpha = 0.60f))
        // Wave 2 — mid fill
        drawWave(p2, 0.036f, 0.50f, WaveMid.copy(alpha = 0.50f))
        // Wave 3 — shimmer outline
        drawWave(p3, 0.026f, 0.48f, WaveShimmer.copy(alpha = 0.75f),
            filled = false, strokePx = (w * 0.004f).coerceAtLeast(4f))
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val email           by viewModel.email.collectAsState()
    val password        by viewModel.password.collectAsState()
    val displayName     by viewModel.displayName.collectAsState()
    val username        by viewModel.username.collectAsState()
    val confirmPassword by viewModel.confirmPassword.collectAsState()
    val isSignUp        by viewModel.isSignUp.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()
    val error           by viewModel.error.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()

    var passwordVisible        by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    if (isAuthenticated) { onAuthSuccess() }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

        // Dark animated waves — fills entire screen
        DarkWaveBackground()

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .padding(horizontal = 20.dp, vertical = 28.dp)
                .shadow(
                    elevation    = 40.dp,
                    shape        = RoundedCornerShape(32.dp),
                    ambientColor = AccentBlue.copy(alpha = 0.20f),
                    spotColor    = AccentBlue.copy(alpha = 0.35f)
                )
                // Dark glass card
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CardSurface.copy(alpha = 0.90f),
                            Color(0xFF081230).copy(alpha = 0.95f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            AccentBlue.copy(alpha = 0.55f),
                            CardBorder.copy(alpha = 0.30f),
                            AccentCyan.copy(alpha = 0.20f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(horizontal = 28.dp, vertical = 36.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Logo + wordmark ────────────────────────────────────────────
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter           = painterResource(id = com.example.pulse.R.drawable.logo_pulse),
                    contentDescription = "Pulse Logo",
                    modifier          = Modifier.size(62.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text          = "PULSE",
                    fontSize      = 36.sp,
                    fontWeight    = FontWeight.Black,
                    color         = TextPrimary,
                    letterSpacing = 3.sp
                )
            }

            Text(
                text       = if (isSignUp) "Join the community" else "Welcome back",
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                fontStyle  = FontStyle.Italic,
                color      = TextSecondary,
                modifier   = Modifier.padding(top = 6.dp, bottom = 28.dp)
            )

            // ── Shared field colours — dark style ──────────────────────────
            val tfColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor        = TextPrimary,
                unfocusedTextColor      = TextPrimary,
                focusedContainerColor   = FieldSurface,
                unfocusedContainerColor = FieldSurface.copy(alpha = 0.70f),
                focusedBorderColor      = AccentCyan,
                unfocusedBorderColor    = FieldBorder,
                focusedLabelColor       = AccentCyan,
                unfocusedLabelColor     = TextHint,
                cursorColor             = AccentCyan,
                focusedLeadingIconColor  = AccentCyan,
                unfocusedLeadingIconColor= TextHint,
                focusedTrailingIconColor = AccentCyan,
                unfocusedTrailingIconColor = TextHint,
                focusedPlaceholderColor  = TextHint,
                unfocusedPlaceholderColor= TextHint
            )
            val tfShape = RoundedCornerShape(16.dp)
            val tfMod   = Modifier.fillMaxWidth()

            // ── Sign-up only fields ────────────────────────────────────────
            if (isSignUp) {
                OutlinedTextField(
                    value = displayName, onValueChange = { viewModel.setDisplayName(it) },
                    label = { Text("Display Name") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    colors = tfColors, shape = tfShape, modifier = tfMod, singleLine = true
                )
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = username, onValueChange = { viewModel.setUsername(it) },
                    label = { Text("Username") },
                    leadingIcon = {
                        Text(
                            "@", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            color = AccentCyan,
                            modifier = Modifier.padding(start = 14.dp, end = 4.dp)
                        )
                    },
                    colors = tfColors, shape = tfShape, modifier = tfMod, singleLine = true
                )
                Spacer(Modifier.height(14.dp))
            }

            // ── Email ──────────────────────────────────────────────────────
            OutlinedTextField(
                value = email, onValueChange = { viewModel.setEmail(it) },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                colors = tfColors, shape = tfShape, modifier = tfMod, singleLine = true
            )
            Spacer(Modifier.height(14.dp))

            // ── Password ───────────────────────────────────────────────────
            OutlinedTextField(
                value = password, onValueChange = { viewModel.setPassword(it) },
                label = { Text("Password") },
                leadingIcon  = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                colors = tfColors, shape = tfShape, modifier = tfMod, singleLine = true
            )

            // ── Confirm password (sign-up) ─────────────────────────────────
            if (isSignUp) {
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = confirmPassword, onValueChange = { viewModel.setConfirmPassword(it) },
                    label = { Text("Confirm Password") },
                    leadingIcon  = { Icon(Icons.Default.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    colors = tfColors, shape = tfShape, modifier = tfMod, singleLine = true
                )
            }

            // ── Error box ──────────────────────────────────────────────────
            error?.let {
                Spacer(Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ErrorBg, RoundedCornerShape(12.dp))
                        .border(1.dp, ErrorText.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = it, color = ErrorText, fontSize = 13.sp,
                        fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── CTA ────────────────────────────────────────────────────────
            if (isLoading) {
                CircularProgressIndicator(color = AccentCyan, strokeWidth = 3.dp)
            } else {
                // Gradient Sign In / Create Account button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(14.dp, RoundedCornerShape(18.dp),
                            ambientColor = AccentBlue.copy(alpha = 0.40f),
                            spotColor    = AccentCyan.copy(alpha = 0.30f))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0D2473), MidBlue, AccentCyan.copy(alpha = 0.9f))
                            ),
                            shape = RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { viewModel.authenticate() },
                        colors  = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape   = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text          = if (isSignUp) "Create Account" else "Sign In",
                            color         = Color.White,
                            fontWeight    = FontWeight.Bold,
                            fontSize      = 17.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Divider
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).height(1.dp).background(FieldBorder))
                    Text("  or  ", fontSize = 12.sp, color = TextHint)
                    Box(Modifier.weight(1f).height(1.dp).background(FieldBorder))
                }

                Spacer(Modifier.height(14.dp))

                TextButton(onClick = { viewModel.toggleAuthMode() }) {
                    Text(
                        text       = if (isSignUp) "Already have an account? Sign In"
                                     else "New to Pulse? Create account",
                        color      = AccentCyan,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 14.sp
                    )
                }
            }
        }
    }
}
