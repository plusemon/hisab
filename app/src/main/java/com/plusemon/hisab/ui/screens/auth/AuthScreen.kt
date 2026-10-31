package com.plusemon.hisab.ui.screens.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.plusemon.hisab.R
import com.plusemon.hisab.domain.util.AuthDiagnostics
import com.plusemon.hisab.domain.util.Localization
import com.plusemon.hisab.ui.viewmodel.HisabViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
fun AuthScreen(
    viewModel: HisabViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val settings by viewModel.settings.collectAsState()
    val isBn = settings.language == "bn"
    val isLoading by viewModel.isAuthLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()

    var showDiagnosticDialog by remember { mutableStateOf(false) }

    val currentAppSha1 = remember { AuthDiagnostics.getCurrentAppSha1(context) }
    val isSha1Registered = remember { AuthDiagnostics.isSha1RegisteredInFirebase(context) }

    // Diagnostic Dialog for Google Sign-In Assistance
    if (showDiagnosticDialog) {
        AlertDialog(
            onDismissRequest = { showDiagnosticDialog = false },
            icon = {
                Icon(
                    imageVector = if (!isSha1Registered) Icons.Default.Key else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (!isSha1Registered) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (isBn) {
                        if (!isSha1Registered) "গুগল সাইন ইন ও SHA-1 কনফিগারেশন" else "গুগল সাইন ইন সহায়তা"
                    } else {
                        if (!isSha1Registered) "Google Sign-In & SHA-1 Setup" else "Google Sign-In Help"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isSha1Registered) {
                        Text(
                            text = if (isBn)
                                "আপনার ডিভাইসে গুগল অ্যাকাউন্ট থাকা সত্ত্বেও সাইন ইন হচ্ছে না, কারণ বর্তমান অ্যাপটির সাইনিং SHA-1 ফিঙ্গারপ্রিন্ট ফায়ারবেসে (Firebase Console) যুক্ত করা নেই। নিরাপত্তা নিশ্চিত করার জন্য গুগল প্লে সার্ভিসেস এই অনুরোধটি প্রত্যাখ্যান করেছে।"
                            else
                                "Google Sign-In failed although accounts exist on your device because this app's signing SHA-1 certificate is not registered in Firebase Console. Google Play Services enforces this security check.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // SHA-1 Display Card with copy
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isBn) "বর্তমান অ্যাপের SHA-1 ফিঙ্গারপ্রিন্ট:" else "Current App SHA-1 Fingerprint:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentAppSha1,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(currentAppSha1))
                                        Toast.makeText(
                                            context,
                                            if (isBn) "SHA-1 ক্লিপবোর্ডে কপি করা হয়েছে!" else "SHA-1 copied to clipboard!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isBn) "SHA-1 কপি করুন" else "Copy SHA-1",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (isBn)
                                "সমাধান:\n১. Firebase Console > Project Settings এ যান\n২. 'com.plusemon.hisab' অ্যাপের নিচে 'Add fingerprint' এ এই SHA-1 যুক্ত করুন।"
                            else
                                "How to fix:\n1. Open Firebase Console > Project Settings\n2. Under 'com.plusemon.hisab', click 'Add fingerprint' and paste this SHA-1.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = if (isBn)
                                "গুগল সাইন ইন সম্পন্ন করা সম্ভব হয়নি। অনুগ্রহ করে নিশ্চিত করুন যে আপনার ডিভাইসে ইন্টারনেট সংযোগ সক্রিয় রয়েছে এবং গুগল প্লে সার্ভিসেস আপডেট করা আছে।"
                            else
                                "Google Sign-In could not complete. Please ensure your device has an active internet connection and Google Play Services is updated.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDiagnosticDialog = false },
                    modifier = Modifier.testTag("auth_dialog_dismiss_button")
                ) {
                    Text(if (isBn) "ঠিক আছে" else "OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDiagnosticDialog = false
                        try {
                            val intent = Intent(Settings.ACTION_SYNC_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            } catch (e2: Exception) {
                                // Ignore
                            }
                        }
                    },
                    modifier = Modifier.testTag("auth_dialog_check_settings")
                ) {
                    Text(if (isBn) "ডিভাইস সেটিংস" else "Device Settings")
                }
            }
        )
    }

    // Google Sign-In Action Trigger
    val launchGoogleSignIn: () -> Unit = {
        coroutineScope.launch {
            val activity = context as? Activity
            if (activity == null) {
                viewModel.handleGoogleSignInFailure(IllegalStateException("Context must be an Activity"))
                return@launch
            }

            val credentialManager = CredentialManager.create(context)
            val webClientId = try {
                context.getString(R.string.default_web_client_id)
            } catch (e: Exception) {
                viewModel.handleGoogleSignInFailure(e)
                return@launch
            }

            fun isCancellation(t: Throwable): Boolean {
                var curr: Throwable? = t
                while (curr != null) {
                    if (curr is GetCredentialCancellationException) return true
                    val msg = curr.message ?: ""
                    if (msg.contains("cancel", ignoreCase = true) ||
                        msg.contains("user cancelled", ignoreCase = true)
                    ) {
                        return true
                    }
                    curr = curr.cause
                }
                return false
            }

            try {
                var result: GetCredentialResponse? = null

                // 1. Primary Attempt: Use GetSignInWithGoogleOption
                try {
                    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId)
                        .build()
                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(signInOption)
                        .build()
                    result = credentialManager.getCredential(
                        context = activity,
                        request = request
                    )
                } catch (e: Throwable) {
                    if (isCancellation(e)) {
                        Log.i("GoogleSignIn", "User cancelled Google Sign-In picker.")
                        return@launch
                    }
                    Log.w("GoogleSignIn", "GetSignInWithGoogleOption failed: ${e.message}. Trying GetGoogleIdOption fallback...")
                }

                // 2. Fallback: Try GetGoogleIdOption with filterByAuthorizedAccounts = false
                if (result == null) {
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
                        .setAutoSelectEnabled(false)
                        .build()
                    val fallbackRequest = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()
                    result = credentialManager.getCredential(
                        context = activity,
                        request = fallbackRequest
                    )
                }

                val finalResult = result
                    ?: throw NoCredentialException("No credentials returned from Google Sign-In")

                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(finalResult.credential.data)
                val idToken = googleIdTokenCredential.idToken
                val userEmail = googleIdTokenCredential.id
                val userDisplayName = googleIdTokenCredential.displayName
                    ?: googleIdTokenCredential.givenName
                    ?: userEmail.substringBefore("@")
                val credentialPhoto = googleIdTokenCredential.profilePictureUri?.toString()
                val googleId = googleIdTokenCredential.id

                val firebaseUid = signInFirebaseWithGoogleToken(context, idToken)
                val fbPhoto = try {
                    if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) {
                        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()
                    } else null
                } catch (e: Exception) { null }

                val userPhotoUrl = credentialPhoto ?: fbPhoto

                viewModel.signInWithGoogle(
                    uid = firebaseUid ?: googleId,
                    email = userEmail,
                    name = userDisplayName,
                    photoUrl = userPhotoUrl
                )
            } catch (e: Throwable) {
                if (isCancellation(e)) {
                    Log.i("GoogleSignIn", "Google Sign-In cancelled/dismissed by user.")
                } else {
                    Log.e("GoogleSignIn", "Google Sign-In failed: ${e.message}", e)
                    showDiagnosticDialog = true
                    viewModel.handleGoogleSignInFailure(e)
                }
            }
        }
    }

    // Outer Background Box with custom fluid artwork
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7FAF8))
            .drawBehind {
                val w = size.width
                val h = size.height

                // Top-Left Wave 1 (Soft teal wave)
                val topWave1 = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w * 0.46f, 0f)
                    cubicTo(
                        w * 0.38f, h * 0.07f,
                        w * 0.12f, h * 0.11f,
                        0f, h * 0.20f
                    )
                    close()
                }
                drawPath(
                    path = topWave1,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF246857), Color(0xFF388876))
                    )
                )

                // Top-Left Wave 2 (Deep forest emerald wave foreground)
                val topWave2 = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w * 0.35f, 0f)
                    cubicTo(
                        w * 0.27f, h * 0.05f,
                        w * 0.08f, h * 0.09f,
                        0f, h * 0.15f
                    )
                    close()
                }
                drawPath(
                    path = topWave2,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF14473A), Color(0xFF1E5D4F))
                    )
                )

                // Bottom-Left Wave 1 (Soft teal-sage wave extending outwards)
                val botWave1 = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, h * 0.81f)
                    cubicTo(
                        w * 0.15f, h * 0.83f,
                        w * 0.36f, h * 0.90f,
                        w * 0.52f, h * 0.94f
                    )
                    cubicTo(
                        w * 0.65f, h * 0.97f,
                        w * 0.72f, h * 0.99f,
                        w * 0.78f, h
                    )
                    close()
                }
                drawPath(
                    path = botWave1,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF2B7564), Color(0xFF40907E), Color(0xFF5CB1A2))
                    )
                )

                // Bottom-Left Wave 2 (Deep emerald base wave)
                val botWave2 = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, h * 0.84f)
                    cubicTo(
                        w * 0.12f, h * 0.86f,
                        w * 0.28f, h * 0.92f,
                        w * 0.44f, h * 0.97f
                    )
                    cubicTo(
                        w * 0.50f, h * 0.99f,
                        w * 0.56f, h,
                        w * 0.58f, h
                    )
                    close()
                }
                drawPath(
                    path = botWave2,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF134639), Color(0xFF1B5A4B), Color(0xFF267563))
                    )
                )
            }
    ) {
        // Bottom-Right Botanical Leaves Artwork
        Image(
            painter = painterResource(id = R.drawable.ic_auth_botanical),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(width = 175.dp, height = 270.dp)
                .alpha(0.85f),
            contentScale = ContentScale.Fit
        )

        // Main Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Language Selector (Pill button with Globe & Chevron)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    onClick = { viewModel.toggleLanguage() },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFE8F1EC),
                    modifier = Modifier.testTag("auth_language_toggle_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = "Switch Language",
                            tint = Color(0xFF1B3D33),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "বাং" else "EN",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B3D33),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF1B3D33),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // App Icon Logo Squircle
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF127B60), Color(0xFF094C3B))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_app_icon),
                    contentDescription = "Hisab Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Name "Hisab"
            Text(
                text = "Hisab",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 38.sp,
                color = Color(0xFF0E4336),
                textAlign = TextAlign.Center
            )

            // Subtitle "Daily Expense & Money Manager"
            Text(
                text = Localization.getString(Localization.Key.TAGLINE, isBn),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = Color(0xFF566F66),
                modifier = Modifier.padding(top = 4.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(44.dp))

            // Welcome Back Greeting
            Text(
                text = Localization.getString(Localization.Key.WELCOME_BACK, isBn),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = Color(0xFF12231E),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Sign in prompt
            Text(
                text = Localization.getString(Localization.Key.SIGN_IN_GOOGLE_SUBTITLE, isBn),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                fontSize = 14.5.sp,
                color = Color(0xFF677F76),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Primary Google Sign-In Action Pill
            Surface(
                onClick = { if (!isLoading) launchGoogleSignIn() },
                shape = RoundedCornerShape(32.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, Color(0xFFE2ECE7)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .height(58.dp)
                    .testTag("google_signin_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Authentic Google Logo
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    // Thin vertical divider line
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(Color(0xFFE0E7E3))
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    // "Continue with Google"
                    Text(
                        text = Localization.getString(Localization.Key.SIGN_IN_WITH_GOOGLE, isBn),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.5.sp,
                        color = Color(0xFF162B24),
                        modifier = Modifier.weight(1f)
                    )

                    // Right Arrow or Progress Indicator
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color(0xFF146550),
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Continue",
                            tint = Color(0xFF146550),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Error Message Banner (if any)
            AnimatedVisibility(visible = authError != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .padding(top = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = authError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(44.dp))

            // Three Feature Highlights Row (Secure, Fast, Private)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1: Secure & Private
                FeatureHighlightColumn(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = Color(0xFF1E755D),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    line1 = Localization.getString(Localization.Key.FEATURE_SECURE_TITLE, isBn),
                    line2 = Localization.getString(Localization.Key.FEATURE_SECURE_SUB, isBn)
                )

                // Divider 1
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(38.dp)
                        .background(Color(0xFFDCE6E1))
                )

                // Column 2: Fast Access
                FeatureHighlightColumn(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = Color(0xFF1E755D),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    line1 = Localization.getString(Localization.Key.FEATURE_FAST_TITLE, isBn),
                    line2 = Localization.getString(Localization.Key.FEATURE_FAST_SUB, isBn)
                )

                // Divider 2
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(38.dp)
                        .background(Color(0xFFDCE6E1))
                )

                // Column 3: Your Data Stays Yours
                FeatureHighlightColumn(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Cloud,
                            contentDescription = null,
                            tint = Color(0xFF1E755D),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    line1 = Localization.getString(Localization.Key.FEATURE_PRIVACY_TITLE, isBn),
                    line2 = Localization.getString(Localization.Key.FEATURE_PRIVACY_SUB, isBn)
                )
            }

            Spacer(modifier = Modifier.height(46.dp))

            // Footer: — Powered by Hisab —
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(1.5.dp)
                        .background(Color(0xFFA0B9AE))
                )
                Text(
                    text = "  ${Localization.getString(Localization.Key.POWERED_BY_HISAB, isBn)}  ",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B877C)
                )
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(1.5.dp)
                        .background(Color(0xFFA0B9AE))
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun FeatureHighlightColumn(
    icon: @Composable () -> Unit,
    line1: String,
    line2: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Mint circular badge
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFFE5F2EC)),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = line1,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = Color(0xFF456157),
            textAlign = TextAlign.Center,
            lineHeight = 15.sp
        )
        Text(
            text = line2,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = Color(0xFF456157),
            textAlign = TextAlign.Center,
            lineHeight = 15.sp
        )
    }
}

private suspend fun signInFirebaseWithGoogleToken(context: Context, idToken: String): String? {
    return suspendCancellableCoroutine { continuation ->
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid
                    if (continuation.isActive) {
                        continuation.resume(uid)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("GoogleSignIn", "Firebase Auth sign-in failed: ${e.message}")
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
        } catch (e: Exception) {
            Log.w("GoogleSignIn", "Firebase Auth exception: ${e.message}")
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
    }
}
