package com.madebykunal.doon.data

import com.madebykunal.doon.lib.DateStr
import com.madebykunal.doon.lib.Dates

object Seed {
    private val words = "call plan write review book send fix read buy clean check draft pay".split(" ")
    private val things = "report dentist invoice groceries flights notes garden taxes email car desk".split(" ")
    private val emoji = listOf("🎂", "✈️", "🎉", "💍", "🏠", "🎓", "🚗", "🌴", "🏃", "📅")

    private fun <T> pick(items: List<T>, i: Int) = items[i % items.size]

    fun performance(today: DateStr) {
        Db.tx {
            for (i in 0 until 2000) {
                val date = Dates.addDays(today, (i % 730) - 365)
                val todo = TodoRepo.create(Home.Day(date), "${pick(words, i)} ${pick(things, i * 7)} #$i")
                if (date < today) TodoRepo.setCompleted(listOf(todo.id), System.currentTimeMillis())
            }
            val group = ListRepo.createGroup("Seeded")
            for (i in 0 until 30) {
                val listId = ListRepo.createList("List ${i + 1}", group, pick(emoji, i))
                for (j in 0 until 10) TodoRepo.create(Home.InList(listId), "${pick(words, j)} ${pick(things, i + j)}")
            }
            for (i in 0 until 50) {
                CountdownRepo.create("Event ${i + 1}", pick(emoji, i), if (i % 3 == 0) "yearly" else "until", Dates.addDays(today, i * 9 + 1), null, i < 6, emptyList())
            }
        }
    }
}
