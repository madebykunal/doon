package com.madebykunal.doon

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import com.madebykunal.doon.data.Db
import com.madebykunal.doon.data.Home
import com.madebykunal.doon.data.TodoRepo
import com.madebykunal.doon.data.Where
import com.madebykunal.doon.lib.Dates
import com.madebykunal.doon.notify.Reminders
import com.madebykunal.doon.state.Editor
import com.madebykunal.doon.state.Nav
import com.madebykunal.doon.state.Prefs
import com.madebykunal.doon.state.Route
import com.madebykunal.doon.state.Ui
import com.madebykunal.doon.ui.App
import com.madebykunal.doon.ui.MigrationFailedScreen
import com.madebykunal.doon.ui.theme.DoonTheme
import com.madebykunal.doon.ui.theme.colors
import com.madebykunal.doon.widget.Widgets
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Reminders.requestPermission = {
            if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        DoonApp.clearRecoveryFiles()
        if (savedInstanceState == null) handle(intent)
        setContent {
            DoonTheme {
                SystemBars()
                val error = Db.openError
                if (error != null) MigrationFailedScreen(error, ::exportRaw) else App()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    override fun onStart() {
        super.onStart()
        DoonApp.foreground = true
        if (!DoonApp.ready) return
        Ui.resumes++
        Ui.refreshToday()
        if (Prefs.rollover) TodoRepo.rollover(Ui.today)
        Widgets.requestSync()
        DoonApp.eraseDeleted()
    }

    override fun onStop() {
        super.onStop()
        DoonApp.foreground = false
        if (DoonApp.ready) {
            Editor.commitAll()
            DoonApp.eraseDeleted()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Reminders.requestPermission = null
    }

    private fun handle(intent: Intent?) {
        if (!DoonApp.ready) return
        val open = intent?.getStringExtra("open") ?: return
        intent.removeExtra("open")
        val kind = open.substringBefore(":")
        val arg = open.substringAfter(":", "")
        when (kind) {
            "today" -> {
                Ui.selectedDate = Ui.today
                Nav.open(0)
            }
            "add" -> {
                Ui.selectedDate = Ui.today
                Nav.open(0)
                Editor.openComposer(Home.Day(Ui.today), Where.Bottom)
            }
            "countdowns" -> Nav.open(2)
            "countdown" -> Nav.open(2, Route.CountdownDetail(arg))
            "day" -> if (Dates.isDateStr(arg)) {
                Ui.selectedDate = arg
                Nav.open(0)
            }
            "todo", "alarm" -> when (val home = Reminders.homeOf(arg)) {
                is Home.Day -> {
                    Ui.selectedDate = home.date
                    Nav.open(0)
                }
                is Home.InList -> Nav.open(1, Route.ListDetail(home.listId))
                null -> Unit
            }
        }
    }

    private fun exportRaw(onDone: (String) -> Unit) {
        runCatching {
            val db = SQLiteDatabase.openDatabase(Db.rawFile(this).path, null, SQLiteDatabase.OPEN_READONLY)
            val dump = JSONObject()
            db.use {
                val tables = it.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'", null).use { c ->
                    buildList { while (c.moveToNext()) add(c.getString(0)) }
                }
                tables.forEach { table ->
                    val rows = JSONArray()
                    it.rawQuery("SELECT * FROM \"${table.replace("\"", "\"\"")}\"", null).use { c ->
                        while (c.moveToNext()) {
                            val row = JSONObject()
                            for (i in 0 until c.columnCount) {
                                row.put(c.getColumnName(i), if (c.isNull(i)) JSONObject.NULL else c.getString(i))
                            }
                            rows.put(row)
                        }
                    }
                    dump.put(table, rows)
                }
            }
            val dir = File(cacheDir, "recovery").apply { mkdirs() }
            val file = File(dir, "doon-recovery-${Dates.today()}.json")
            file.writeText(dump.toString())
            val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
            val send = Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(send, "Export your data"))
        }.onSuccess {
            onDone("Done. The temporary copy is removed the next time Doon opens, and you can export again.")
        }.onFailure {
            onDone("Export failed. Your data is still on this device.")
        }
    }
}

@Composable
fun SystemBars() {
    val view = LocalView.current
    val c = colors
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as Activity).window
        window.setBackgroundDrawable(ColorDrawable(c.bg.toArgb()))
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !c.dark
            isAppearanceLightNavigationBars = !c.dark
        }
    }
}
