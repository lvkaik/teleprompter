package com.yourname.teleprompter.ui.editor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.teleprompter.data.local.entity.ScriptEntity
import com.yourname.teleprompter.ui.theme.TeleprompterTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditorActivity : ComponentActivity() {

    private val vm: EditorViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra(EXTRA_ID) ?: run { finish(); return }
        vm.load(id)

        setContent {
            TeleprompterTheme {
                val script by vm.script.collectAsState()
                var title by remember { mutableStateOf("") }
                var content by remember { mutableStateOf("") }
                var fontSp by remember { mutableStateOf(22f) }
                var speed by remember { mutableStateOf(30f) }

                LaunchedEffect(script) {
                    script?.let {
                        title = it.title
                        content = it.content
                        fontSp = it.fontSizeSp
                        speed = it.speedPxPerSec
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("编辑文稿") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, "返回")
                                }
                            },
                            actions = {
                                IconButton(onClick = {
                                    script?.let {
                                        vm.save(it.copy(
                                            title = title,
                                            content = content,
                                            fontSizeSp = fontSp,
                                            speedPxPerSec = speed
                                        ))
                                    }
                                    finish()
                                }) { Icon(Icons.Default.Save, "保存") }
                            }
                        )
                    }
                ) { padding ->
                    Column(
                        Modifier
                            .padding(padding)
                            .padding(16.dp)
                            .fillMaxSize()
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("标题") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            label = { Text("提词内容") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("字号: ${fontSp.toInt()} sp")
                        Slider(
                            value = fontSp,
                            onValueChange = { fontSp = it },
                            valueRange = 12f..48f
                        )
                        Text("滚动速度: ${speed.toInt()} px/s")
                        Slider(
                            value = speed,
                            onValueChange = { speed = it },
                            valueRange = 1f..200f
                        )
                        Text("字数: ${content.length}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_ID = "extra_script_id"
    }
}