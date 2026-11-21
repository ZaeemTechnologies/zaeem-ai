package com.zaeemtechnologies.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.Image
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.zaeemtechnologies.ai.ui.ZaeemTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZaeemTheme { Shell() } }
    }
}

private val tabs = listOf("Home", "Chats", "Tools", "Settings")

@Composable
fun Shell() {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(bottomBar = {
        NavigationBar { tabs.forEachIndexed { i, t ->
            NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = {}, label = { Text(t) },
                alwaysShowLabel = true, modifier = Modifier.heightIn(min = 48.dp)) } }
    }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            if (tab == 0) {
                Image(painterResource(R.drawable.zaeem_emblem), contentDescription = "ZAEEM AI emblem", modifier = Modifier.size(200.dp))
                Text("Hello, I'm Zaeem.", style = MaterialTheme.typography.headlineMedium)
                Text("How may I assist you today?")
                Spacer(Modifier.height(16.dp))
                Text("No provider configured. Setup is not implemented yet (Milestone 2).",
                    color = MaterialTheme.colorScheme.secondary)
            } else Text("${tabs[tab]} — planned, not implemented")
        }
    }
}
