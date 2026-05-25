package com.suvojeetsengupta.suvform.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeetsengupta.suvform.R
import com.suvojeetsengupta.suvform.ui.theme.Fraunces
import com.suvojeetsengupta.suvform.ui.theme.SuvTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val c = SuvTheme.colors

    Scaffold(
        containerColor = c.paper,
        topBar = {
            TopAppBar(
                title = { Text("About", fontFamily = Fraunces, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), "Back", tint = c.ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.paper)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Brand Section
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(c.accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "S",
                    color = c.onAccent,
                    fontFamily = Fraunces,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "SuvForm",
                fontFamily = Fraunces,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = c.ink
            )
            
            Text(
                "The Editorial Form Builder",
                style = MaterialTheme.typography.labelLarge,
                color = c.accent,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(40.dp))

            AboutParagraph(
                title = "The Mission",
                content = "SuvForm is born out of a desire for a clean, efficient, and mobile-first form management tool. It combines the simplicity of a notepad with the power of a professional data collection platform."
            )

            Spacer(Modifier.height(32.dp))

            AboutParagraph(
                title = "AI-Powered",
                content = "Leveraging Gemini and Groq AI, SuvForm lets you build complex forms in seconds from a simple prompt. It handles the fields, logic, and calculations so you can focus on gathering insights."
            )

            Spacer(Modifier.height(32.dp))

            AboutParagraph(
                title = "Offline First",
                content = "Your forms and responses stay cached on your device. Whether you're in the field or in the city, you can always browse and manage your data without an active internet connection."
            )

            Spacer(Modifier.height(48.dp))

            Text(
                "Made with passion by Suvojeet Sengupta",
                style = MaterialTheme.typography.bodySmall,
                color = c.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun AboutParagraph(title: String, content: String) {
    val c = SuvTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontFamily = Fraunces,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = c.ink
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyLarge,
            color = c.muted,
            lineHeight = 24.sp
        )
    }
}
