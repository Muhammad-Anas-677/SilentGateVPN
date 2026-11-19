package com.silentgatevpn.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.silentgatevpn.app.R
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silentgatevpn.app.legal.LegalDoc
import com.silentgatevpn.app.legal.LegalDocs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalViewer(doc: LegalDoc, onBack: () -> Unit) {
    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text(doc.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black, titleContentColor = Color.White, navigationIconContentColor = Color.White
                )
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
            doc.body.trim().lines().forEach { raw ->
                val line = raw.trimEnd()
                when {
                    line.isBlank() -> Spacer(Modifier.height(6.dp))
                    line.startsWith("# ") -> Text(
                        line.removePrefix("# "), style = MaterialTheme.typography.titleMedium, color = SgGreen,
                        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
                    )
                    line.startsWith("- ") -> Row(Modifier.padding(start = 6.dp, top = 2.dp)) {
                        Text("•  ", color = SgGreen)
                        Text(line.removePrefix("- "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                    }
                    else -> Text(line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 2.dp))
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun DocLinkRow(doc: LegalDoc, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(doc.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(doc.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ConsentScreen(onOpenDoc: (LegalDoc) -> Unit, onAccept: () -> Unit) {
    var adult by remember { mutableStateOf(false) }
    var agree by remember { mutableStateOf(false) }
    val checkColors = CheckboxDefaults.colors(
        checkedColor = SgGreen, checkmarkColor = Color.Black, uncheckedColor = Color(0xFFB0B0B0)
    )
    val onBg = MaterialTheme.colorScheme.onBackground
    val dim = MaterialTheme.colorScheme.onSurfaceVariant

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(Modifier.height(28.dp))
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "SilentGate VPN",
                    modifier = Modifier.size(112.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Welcome to SilentGate VPN", style = MaterialTheme.typography.headlineMedium,
                    color = onBg, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Please read how the app works and what you agree to. No ads, no accounts, no analytics.",
                    style = MaterialTheme.typography.bodyMedium, color = dim, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp)
                )
            }
            items(LegalDocs.all.size) { i ->
                val doc = LegalDocs.all[i]
                Card(
                    onClick = { onOpenDoc(doc) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SgCard, contentColor = onBg),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(doc.title, style = MaterialTheme.typography.titleMedium, color = onBg)
                            Text(doc.summary, style = MaterialTheme.typography.bodySmall, color = dim, modifier = Modifier.padding(top = 2.dp))
                        }
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = SgGreen)
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth().clickable { adult = !adult }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = adult, onCheckedChange = { adult = it }, colors = checkColors)
                Text("I am 18 years old or older.", style = MaterialTheme.typography.bodyMedium, color = onBg)
            }
            Row(Modifier.fillMaxWidth().clickable { agree = !agree }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = agree, onCheckedChange = { agree = it }, colors = checkColors)
                Text(
                    "I have read and agree to the Terms of Service, Terms of Use, Terms and Conditions, Acceptable Use Policy, Disclaimer and Privacy Policy.",
                    style = MaterialTheme.typography.bodyMedium, color = onBg
                )
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onAccept, enabled = adult && agree, modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SgGreen, contentColor = Color.Black,
                    disabledContainerColor = Color(0xFF1C1C1C), disabledContentColor = Color(0xFF9A9A9A)
                )
            ) { Text("Accept and continue", fontWeight = FontWeight.Bold) }
        }
    }
}
