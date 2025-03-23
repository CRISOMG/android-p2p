package com.devcrisomg.wifip2p_custom_app.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader


open class LogsViewModel : ViewModel() {

    // Holds the logs as a list
    private val _logs = mutableStateListOf<String>()
    val logs: List<String> = _logs

    // Adds a new log
    fun addLog(message: String) {
        _logs.add(message)
    }

    // Clears all logs
    fun clearLogs() {
        _logs.clear()
    }
}

@Composable
fun LogsScreen(modifier: Modifier = Modifier, logsViewModel: LogsViewModel = viewModel()) {
    val logs = logsViewModel.logs // Observe the logs from ViewModel

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Log list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            items(logs.size) { index ->
                Text(text = logs[index], style = MaterialTheme.typography.bodyMedium)
                Divider() // Separate log entries
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Buttons for adding and clearing logs
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            CustomButton(
                onClick = { logsViewModel.addLog("Log at ${System.currentTimeMillis()}") },
                text = "Add Log"
            )
            CustomButton(onClick = { logsViewModel.clearLogs() }, text = "Clear Logs")
        }
    }
}

fun handleSetLogcat(logOutput: MutableState<String>) {
    GlobalScope.launch(Dispatchers.IO) {
        captureLogcat { log ->
            // Update the log output in a thread-safe manner
            val t = log.split(" ", limit = 4)
            val logTag = t.getOrNull(3)?.split(" ")?.getOrNull(2)
            val msj = t.getOrNull(3)?.split(':', limit = 2)?.getOrNull(1)
            logOutput.value += "${logTag ?: ""} ${msj}\n\n" // Append new log line
        }
    }
}

fun captureLogcat(outputCallback: (String) -> Unit) {
    try {
        // Start a new process to run the logcat command

        val packageName = "com.example.myapplication" // Replace with your package name
        val tags = listOf(
//            "WiFiP2P",
//            "WiFiDirect",
            "nsdmanager",
            "WiFiDirectConnection",
            "SocketInit",
            "SocketManager",
            "WifiP2pManager",
            "GENERALLOG",
        )
//        val logcatCommand = mutableListOf("logcat", "-d", "-s") // Use "-d" for dump and exit, or omit for continuous logs
        val logcatCommand =
            mutableListOf("logcat", "-s") // Use "-d" for dump and exit, or omit for continuous logs
        logcatCommand.addAll(tags.map { "$it:D" })
        Log.d("LOGMANAGER", "$logcatCommand")

        val process = ProcessBuilder()
            .command(logcatCommand)
            .redirectErrorStream(true)
            .start()
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                outputCallback(line ?: "")
            }
        }
    } catch (e: Exception) {
        outputCallback("Error: ${e.message}")
    }
}


class FakeLogsViewModel : LogsViewModel() {
    init {
        addLog("Sample Log 1")
        addLog("Sample Log 2")
        addLog("Sample Log 3")
    }
}

@Composable
fun LogCatList(modifier: Modifier = Modifier) {
    val output = remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("LOGCAT:")

        val scrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            output.value = ""
            handleSetLogcat(output)
        }


        LaunchedEffect(output.value) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(8.dp)
        ) {
            Text(output.value, style = MaterialTheme.typography.bodySmall)
        }

        Row {
            CustomButton(onClick = { output.value = "" }, text = "Clear")
            Spacer(modifier = Modifier.width(16.dp))
            CustomButton(onClick = {
                coroutineScope.launch {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }, text = "Down")
        }
    }

}

@Preview(showBackground = true)
@Composable
fun LogsScreenPreview() {
    val fakeViewModel = FakeLogsViewModel()

    MaterialTheme {
        LogsScreen(logsViewModel = fakeViewModel)
    }
}