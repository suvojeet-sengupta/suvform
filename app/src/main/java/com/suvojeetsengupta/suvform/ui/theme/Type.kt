package com.suvojeetsengupta.suvform.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.suvojeetsengupta.suvform.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

// Fraunces — editorial serif for headlines, wordmark, big numbers.
val Fraunces = FontFamily(
    Font(GoogleFont("Fraunces"), provider, FontWeight.Light),
    Font(GoogleFont("Fraunces"), provider, FontWeight.Normal),
    Font(GoogleFont("Fraunces"), provider, FontWeight.Medium),
    Font(GoogleFont("Fraunces"), provider, FontWeight.Light, FontStyle.Italic),
    Font(GoogleFont("Fraunces"), provider, FontWeight.Normal, FontStyle.Italic),
    Font(GoogleFont("Fraunces"), provider, FontWeight.Medium, FontStyle.Italic),
)

// Geist — clean grotesque sans for body & UI.
val Geist = FontFamily(
    Font(GoogleFont("Geist"), provider, FontWeight.Light),
    Font(GoogleFont("Geist"), provider, FontWeight.Normal),
    Font(GoogleFont("Geist"), provider, FontWeight.Medium),
    Font(GoogleFont("Geist"), provider, FontWeight.SemiBold),
    Font(GoogleFont("Geist"), provider, FontWeight.Bold),
)

// JetBrains Mono — meta labels, counts, codes.
val Mono = FontFamily(
    Font(GoogleFont("JetBrains Mono"), provider, FontWeight.Normal),
    Font(GoogleFont("JetBrains Mono"), provider, FontWeight.Medium),
)

val Typography = Typography(
    // Display / headlines use Fraunces serif.
    displayLarge = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Normal, fontSize = 48.sp, lineHeight = 50.sp, letterSpacing = (-1).sp),
    displayMedium = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 42.sp, letterSpacing = (-1).sp),
    headlineLarge = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Normal, fontSize = 30.sp, lineHeight = 32.sp, letterSpacing = (-0.6).sp),
    headlineMedium = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Normal, fontSize = 26.sp, lineHeight = 28.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.4).sp),
    // Titles use Geist sans.
    titleLarge = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp),
    titleMedium = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.1).sp),
    titleSmall = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    // Body uses Geist.
    bodyLarge = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 13.5.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    // Labels use Geist; mono is applied explicitly where needed.
    labelLarge = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp),
    labelMedium = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
    labelSmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.4.sp),
)
