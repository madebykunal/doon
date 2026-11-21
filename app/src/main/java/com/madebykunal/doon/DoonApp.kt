package com.madebykunal.doon

import android.app.Application
import android.content.Context
import com.madebykunal.doon.data.Db
import com.madebykunal.doon.data.HabitRepo
import com.madebykunal.doon.data.ListRepo
import com.madebykunal.doon.data.Live
import com.madebykunal.doon.data.T
import com.madebykunal.doon.data.TodoRepo
import com.madebykunal.doon.lib.Dates
import com.madebykunal.doon.notify.Reminders
import com.madebykunal.doon.state.Prefs
import com.madebykunal.doon.state.Snack
import com.madebykunal.doon.state.Ui
import com.madebykunal.doon.widget.Widgets
import java.io.File

class DoonApp : Application() {
    override fun onCreate() {
        super.onCreate()
        context = this
        Prefs.load(this)
        Db.open(this)
        if (Db.openError != null) return
        ready = true
        runCatching { ListRepo.seedDefaults() }
        Live.listen { tables -> if (T.TODOS in tables || T.COUNTDOWNS in tables) Widgets.requestSync() }
        Prefs.listen { key -> if (key == "accent" || key == "customColor") Widgets.requestSync() }
        if (Prefs.rollover) TodoRepo.rollover(Dates.today())
        Reminders.resyncAll()
    }

    companion object {
        lateinit var context: Context
            private set
        var ready = false
            private set
        var foreground = false

        fun dayChanged() {
            if (!ready) return
            Ui.refreshToday()
            if (Prefs.rollover) TodoRepo.rollover(Dates.today())
            Widgets.requestSync()
            Reminders.scheduleMidnight()
        }

        fun eraseDeleted() {
            if (!ready) return
            val before = System.currentTimeMillis() - Snack.UNDO_WINDOW_MS * 2
            runCatching {
                TodoRepo.eraseDeletedText(before)
                HabitRepo.eraseDeleted(before)
            }
        }

        fun clearRecoveryFiles() {
            File(context.cacheDir, "recovery").listFiles()?.forEach { it.delete() }
        }
    }
}
