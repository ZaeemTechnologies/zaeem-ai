package com.zaeemtechnologies.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.zaeemtechnologies.ai.ui.Zaeem
import com.zaeemtechnologies.ai.ui.ZaeemTheme

class MainActivity : ComponentActivity() {
    private var voice: Voice? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val st = AppState(applicationContext)
        voice = Voice(applicationContext, st); st.voice = voice
        setContent { ZaeemTheme { Shell(st) } }
    }
    override fun onDestroy() { voice?.release(); super.onDestroy() }
}

@Composable
fun Glass(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Zaeem.Elevated, Zaeem.Surface)), RoundedCornerShape(16.dp))
        .border(1.dp, Zaeem.Border, RoundedCornerShape(16.dp)).padding(14.dp), content = content)
}

@Composable
fun Tile(icon: String, title: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.heightIn(min = 92.dp).background(Brush.verticalGradient(listOf(Zaeem.Elevated, Zaeem.Surface)), RoundedCornerShape(16.dp))
        .border(1.dp, Zaeem.Border, RoundedCornerShape(16.dp)).clickable { onClick() }.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(icon, fontSize = 22.sp); Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(sub, color = Zaeem.Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

private val tabs = listOf("⌂  Home", "💬  Chats", "▦  Tools", "⚙  Settings")

@Composable
fun Shell(st: AppState) {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(containerColor = Zaeem.Background, topBar = {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ZAEEM AI", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("By Zaeem Technologies", color = Zaeem.Secondary, fontSize = 11.sp)
            }
        }
    }, bottomBar = {
        NavigationBar(containerColor = Color(0xFF050D18)) { tabs.forEachIndexed { i, t ->
            NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = {}, label = { Text(t) },
                alwaysShowLabel = true, modifier = Modifier.heightIn(min = 48.dp)) } }
    }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> Home(st) { tab = 2 }
                1 -> Chats(st) { tab = 0 }
                2 -> Tools(st)
                else -> SettingsScreen(st)
            }
        }
    }
}

@Composable
fun Emblem(st: AppState, size: Dp) {
    val ctx = LocalContext.current
    val reduce = remember { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    val inf = rememberInfiniteTransition(label = "e")
    val pulse by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    val sweep by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "s")
    val p = if (reduce) 0.5f else pulse
    val s = if (reduce) 0.5f else sweep
    val glow = when (st.estate) {
        EState.IDLE -> 0.20f + 0.15f * p
        EState.LISTENING -> 0.25f + 0.75f * st.level
        EState.THINKING -> 0.30f + 0.50f * s
        EState.SPEAKING -> 0.40f + 0.40f * p
        EState.ERROR -> 0.15f
    }
    val tint = if (st.estate == EState.ERROR) Zaeem.Warning else Zaeem.Azure
    val sc = if (st.estate == EState.LISTENING && !reduce) 1f + 0.08f * st.level else 1f
    Box(Modifier.size(size).drawBehind {
        drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = glow), Color.Transparent), radius = this.size.minDimension / 1.5f))
    }, contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.zaeem_emblem), "ZAEEM AI emblem", Modifier.fillMaxSize(0.85f).scale(sc))
    }
}

private fun label(e: EState) = when (e) {
    EState.IDLE -> "Ready"; EState.LISTENING -> "Listening…"; EState.THINKING -> "Thinking…"
    EState.SPEAKING -> "Speaking…"; EState.ERROR -> "Needs attention"
}

@Composable
fun Home(st: AppState, onMemory: () -> Unit = {}) {
    val ctx = LocalContext.current
    var input by remember { mutableStateOf("") }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) st.voice?.listen { st.send(it) } else { st.status = "Microphone permission denied."; st.estate = EState.ERROR }
    }
    val list = rememberLazyListState()
    val msgs = st.current?.msgs ?: emptyList()
    LaunchedEffect(msgs.size, msgs.lastOrNull()?.text?.length) { if (msgs.isNotEmpty()) list.scrollToItem(msgs.size - 1) }
    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Emblem(st, if (msgs.isEmpty()) 200.dp else 96.dp)
        if (st.estate != EState.IDLE) Text(label(st.estate), color = Zaeem.Ice, fontWeight = FontWeight.SemiBold)
        if (st.status.isNotEmpty()) Text(st.status, color = Zaeem.Warning, modifier = Modifier.padding(4.dp))
        if (msgs.isEmpty()) {
            Text("Hello, I'm Zaeem.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(if (st.hasKey()) "How may I assist you today?" else "Add a free ${st.prov.label} API key in Settings to begin.", color = Zaeem.Secondary)
        }
        Row(Modifier.padding(vertical = 8.dp).border(1.dp, Zaeem.Border, RoundedCornerShape(24.dp)).padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(if (st.hasKey()) "☁ Connected to ${st.prov.label}" else "☁ No ${st.prov.label} key", fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(8.dp).background(if (st.hasKey()) Zaeem.Ok else Zaeem.Warning, CircleShape))
        }
        if (msgs.isEmpty()) {
            val tileMod = Modifier.weight(1f)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tile("💬", "AI Chat", "Talk with AI", tileMod) {}
                    Tile("🎙", "Voice", "Voice assistant", tileMod) {
                        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                            st.voice?.listen { st.send(it) } else perm.launch(Manifest.permission.RECORD_AUDIO) }
                    Tile("📷", "Vision", "Coming soon", tileMod) { st.status = "Vision is not implemented yet." }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tile("📄", "Documents", "Coming soon", tileMod) { st.status = "Documents are not implemented yet." }
                    Tile("🧠", "Memory", "Personal context", tileMod) { onMemory() }
                    Tile("▦", "Actions", "Coming soon", tileMod) { st.status = "Device actions are not implemented yet." }
                }
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(msgs) { m ->
                val me = m.role == "user"
                Box(Modifier.fillMaxWidth(), contentAlignment = if (me) Alignment.CenterEnd else Alignment.CenterStart) {
                    Text(m.text.ifEmpty { "…" }, color = if (me) Zaeem.OnAzure else Zaeem.Text,
                        modifier = Modifier.widthIn(max = 300.dp).background(if (me) Zaeem.Azure else Zaeem.Elevated, RoundedCornerShape(18.dp)).padding(12.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Ask ZAEEM AI anything…") }, maxLines = 4,
                shape = RoundedCornerShape(28.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Zaeem.Ice, unfocusedBorderColor = Zaeem.Border))
            Button(onClick = {
                if (st.estate == EState.LISTENING) st.voice?.cancelListen()
                else if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                    st.voice?.listen { st.send(it) } else perm.launch(Manifest.permission.RECORD_AUDIO)
            }, modifier = Modifier.size(52.dp), shape = CircleShape, contentPadding = PaddingValues(0.dp)) { Text(if (st.estate == EState.LISTENING) "■" else "🎙") }
            if (st.busy) Button(onClick = { st.stop() }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Stop") }
            else Button(onClick = { st.send(input); input = "" }, enabled = input.isNotBlank(), modifier = Modifier.heightIn(min = 48.dp)) { Text("Send") }
        }
    }
}

@Composable
fun Chats(st: AppState, openHome: () -> Unit) {
    var q by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(q, { q = it }, Modifier.weight(1f), placeholder = { Text("Search chats") }, singleLine = true)
            Button(onClick = { st.newChat(); openHome() }, modifier = Modifier.heightIn(min = 48.dp)) { Text("New") }
        }
        val shown = st.chats.filter { c -> q.isBlank() || c.title.contains(q, true) || c.msgs.any { it.text.contains(q, true) } }
        if (shown.isEmpty()) Text("No conversations.", color = Zaeem.Secondary, modifier = Modifier.padding(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            items(shown) { c ->
                Row(Modifier.fillMaxWidth().background(Zaeem.Surface, RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(c.title, Modifier.weight(1f).clickable { st.open(c.id); openHome() }.heightIn(min = 32.dp))
                    TextButton({ st.delete(c.id) }) { Text("Delete") }
                }
            }
        }
    }
}

@Composable
fun Tools(st: AppState) {
    var m by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Memory", style = MaterialTheme.typography.titleLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(st.memoryOn, { st.updateMemoryOn(it) }); Spacer(Modifier.width(8.dp))
            Text("Use memory (stored only on this phone; sent to your chosen provider as part of each request)")
        }
        Text("Tip: with memory on, start a message with “remember …” to save it.", color = Zaeem.Secondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(m, { m = it }, Modifier.weight(1f), placeholder = { Text("Add a memory") }, singleLine = true)
            Button({ st.addMemory(m); m = "" }, enabled = m.isNotBlank(), modifier = Modifier.heightIn(min = 48.dp)) { Text("Add") }
        }
        if (st.memories.isEmpty()) Text("No memories saved.", color = Zaeem.Secondary)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(st.memories.size) { i ->
                Row(Modifier.fillMaxWidth().background(Zaeem.Surface, RoundedCornerShape(12.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(st.memories[i], Modifier.weight(1f)); TextButton({ st.delMemory(i) }) { Text("Delete") }
                }
            }
        }
        Text("Documents, vision, research and device actions: not implemented yet.", color = Zaeem.Warning)
    }
}

@Composable
fun SettingsScreen(st: AppState) {
    var key by remember { mutableStateOf("") }
    var model by remember(st.provId, st.keyTick) { mutableStateOf(st.model()) }
    var confirmWipe by remember { mutableStateOf(false) }
    st.keyTick
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("AI Providers", color = Zaeem.Ice, modifier = Modifier.padding(top = 8.dp)) }
        items(PROVS) { p ->
            Row(Modifier.fillMaxWidth().background(Zaeem.Surface, RoundedCornerShape(14.dp)).border(1.dp, if (st.provId == p.id) Zaeem.Ice else Zaeem.Border, RoundedCornerShape(14.dp)).clickable { st.setProv(p.id) }.heightIn(min = 52.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(st.provId == p.id, { st.setProv(p.id) })
                Column { Text(p.label, fontWeight = FontWeight.SemiBold); Text(if (st.hasKey(p.id)) "Key saved ✓" else "Key not set", color = if (st.hasKey(p.id)) Zaeem.Ok else Zaeem.Muted, fontSize = 12.sp) }
            }
        }
        item {
            Text("Get a free key at ${st.prov.keyHelp}. Only the selected provider receives your messages. Free tiers have their own data terms.", color = Zaeem.Secondary)
        }
        item {
            OutlinedTextField(key, { key = it }, Modifier.fillMaxWidth(), label = { Text("${st.prov.label} API key") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ st.saveKey(st.provId, key); key = "" }, enabled = key.isNotBlank(), modifier = Modifier.heightIn(min = 48.dp)) { Text("Save key") }
                OutlinedButton({ st.removeKey(st.provId) }, enabled = st.hasKey(), modifier = Modifier.heightIn(min = 48.dp)) { Text("Remove key") }
            }
        }
        item {
            OutlinedTextField(model, { model = it }, Modifier.fillMaxWidth(), label = { Text("Model name") }, singleLine = true)
            Button({ st.setModel(st.provId, model) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Save model") }
        }
        item {
            OutlinedButton({ st.loadModels() }, enabled = st.hasKey(), modifier = Modifier.heightIn(min = 48.dp)) { Text("Load available models") }
            if (st.modelMsg.isNotEmpty()) Text(st.modelMsg, color = Zaeem.Secondary)
        }
        items(st.modelList) { id ->
            Text(id, Modifier.fillMaxWidth().clickable { model = id; st.setModel(st.provId, id) }.heightIn(min = 40.dp).padding(vertical = 8.dp), color = Zaeem.Ice)
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(st.speak, { st.updateSpeak(it) }); Spacer(Modifier.width(8.dp)); Text("Speak replies aloud")
            }
        }
        item {
            if (!confirmWipe) OutlinedButton({ confirmWipe = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Delete all data…") }
            else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ st.wipeAll(); confirmWipe = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Confirm delete") }
                OutlinedButton({ confirmWipe = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
            }
        }
        item { Text("Version 0.2.0-m2 · untested on a device · wake word not implemented.", color = Zaeem.Muted) }
    }
}
