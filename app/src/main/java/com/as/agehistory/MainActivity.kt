package com.as.agehistory

import android.app.*
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.*
import android.content.Context
import java.time.*
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : Activity() {

    private val gold = Color.rgb(214, 182, 111)
    private val black = Color.rgb(16, 16, 16)
    private val panel = Color.rgb(35, 31, 25)
    private val white = Color.WHITE

    private lateinit var root: LinearLayout
    private lateinit var birthResult: TextView
    private lateinit var appointments: LinearLayout

    private val prefs by lazy {
        getSharedPreferences("AS_DATA", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = black
        window.navigationBarColor = black
        showHome()
    }

    private fun text(value: String, size: Float, color: Int): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            setPadding(12, 10, 12, 10)
        }
    }

    private fun button(value: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = value
            setTextColor(black)
            textSize = 15f
            setBackgroundColor(gold)
            setOnClickListener { action() }
        }
    }

    private fun card(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 14, 14, 14)
            setBackgroundColor(panel)
        }
    }

    private fun addSpace() {
        root.addView(android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, 12)
        })
    }

    private fun showHome() {
        val scroll = ScrollView(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 24)
            setBackgroundColor(black)
        }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(text("تاريخ العمر", 30f, gold).apply {
            typeface = Typeface.DEFAULT_BOLD
        })
        root.addView(text("A ♥ S", 28f, white))
        root.addView(text("كل يوم حكاية وكل تاريخ ذكرى", 14f, gold))

        addSpace()

        val today = LocalDate.now()
        val gregorian = today.format(
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.forLanguageTag("ar"))
        )
        val hijri = HijrahDate.from(today)

        val dateCard = card()
        dateCard.addView(text("التاريخ الميلادي", 18f, gold))
        dateCard.addView(text(gregorian, 22f, white))
        dateCard.addView(text("التاريخ الهجري", 18f, gold))
        dateCard.addView(text(
            "${hijri.dayOfMonth} / ${hijri.monthValue} / ${hijri.year}",
            22f, white
        ))
        root.addView(dateCard)

        addSpace()

        val ageCard = card()
        ageCard.addView(text("♥ حساب العمر ♥", 22f, gold))
        birthResult = text("اختر تاريخ ميلادك لمعرفة عمرك", 17f, white)
        ageCard.addView(birthResult)
        ageCard.addView(button("اختيار تاريخ الميلاد") {
            val saved = prefs.getString("birth", "")
            val initial = try {
                LocalDate.parse(saved)
            } catch (e: Exception) {
                LocalDate.of(2000, 1, 1)
            }

            DatePickerDialog(this, { _, y, m, d ->
                val birth = LocalDate.of(y, m + 1, d)
                prefs.edit().putString("birth", birth.toString()).apply()
                calculateAge(birth)
            }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
        })
        root.addView(ageCard)

        addSpace()

        val birthdayCard = card()
        birthdayCard.addView(text("🎂 عيد الميلاد القادم", 20f, gold))
        val birthday = prefs.getString("birth", "")
        if (!birthday.isNullOrEmpty()) {
            try {
                val birth = LocalDate.parse(birthday)
                val next = nextBirthday(birth)
                birthdayCard.addView(text(
                    "باقي ${ChronoUnit.DAYS.between(today, next)} يوماً",
                    22f, white
                ))
            } catch (_: Exception) {}
        } else {
            birthdayCard.addView(text("أدخل تاريخ ميلادك أولاً", 16f, white))
        }
        root.addView(birthdayCard)

        addSpace()

        val appointmentCard = card()
        appointmentCard.addView(text("♥ مواعيدي المهمة ♥", 22f, gold))
        appointments = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        appointmentCard.addView(appointments)
        appointmentCard.addView(button("＋ إضافة موعد جديد") {
            addAppointment()
        })
        root.addView(appointmentCard)

        loadAppointments()
        if (!birthday.isNullOrEmpty()) {
            try { calculateAge(LocalDate.parse(birthday)) } catch (_: Exception) {}
        }
    }

    private fun nextBirthday(birth: LocalDate): LocalDate {
        val now = LocalDate.now()
        var next = try {
            LocalDate.of(now.year, birth.monthValue, birth.dayOfMonth)
        } catch (e: Exception) {
            LocalDate.of(now.year, 2, 28)
        }
        if (next.isBefore(now)) {
            next = try {
                LocalDate.of(now.year + 1, birth.monthValue, birth.dayOfMonth)
            } catch (e: Exception) {
                LocalDate.of(now.year + 1, 2, 28)
            }
        }
        return next
    }

    private fun calculateAge(birth: LocalDate) {
        val now = LocalDate.now()
        if (birth.isAfter(now)) {
            birthResult.text = "تاريخ الميلاد لا يمكن أن يكون في المستقبل"
            return
        }
        val age = Period.between(birth, now)
        birthResult.text =
            "عمرك: ${age.years} سنة و ${age.months} شهر و ${age.days} يوم"
    }

    private fun addAppointment() {
        val input = EditText(this).apply {
            hint = "اكتب اسم الموعد"
            setTextColor(white)
            setHintTextColor(gold)
        }

        AlertDialog.Builder(this)
            .setTitle("موعد جديد")
            .setView(input)
            .setPositiveButton("التالي") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "اكتب اسم الموعد", Toast.LENGTH_SHORT).show()
                } else {
                    chooseAppointmentDate(name)
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun chooseAppointmentDate(name: String) {
        val now = LocalDate.now()
        DatePickerDialog(this, { _, y, m, d ->
            val date = LocalDate.of(y, m + 1, d)
            val old = prefs.getString("appointments", "") ?: ""
            val item = "$name|$date"
            prefs.edit().putString(
                "appointments",
                if (old.isEmpty()) item else "$old\n$item"
            ).apply()
            loadAppointments()
            Toast.makeText(this, "تم حفظ الموعد", Toast.LENGTH_SHORT).show()
        }, now.year, now.monthValue - 1, now.dayOfMonth).show()
    }

    private fun loadAppointments() {
        appointments.removeAllViews()
        val saved = prefs.getString("appointments", "") ?: ""

        if (saved.isEmpty()) {
            appointments.addView(text("لا توجد مواعيد حالياً", 15f, white))
            return
        }

        saved.lines().filter { it.contains("|") }.forEachIndexed { index, item ->
            val parts = item.split("|", limit = 2)
            if (parts.size == 2) {
                try {
                    val date = LocalDate.parse(parts[1])
                    val days = ChronoUnit.DAYS.between(LocalDate.now(), date)
                    val line = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }
                    val info = text(
                        "${parts[0]}\n${date}\n${if (days > 0) "باقي $days يوم" else if (days == 0L) "الموعد اليوم" else "انتهى الموعد"}",
                        15f, white
                    )
                    info.layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                    line.addView(info)
                    line.addView(button("حذف") {
                        val current = prefs.getString("appointments", "") ?: ""
                        val updated = current.lines().filterIndexed { i, _ -> i != index }
                        prefs.edit().putString("appointments", updated.joinToString("\n")).apply()
                        loadAppointments()
                    })
                    appointments.addView(line)
                } catch (_: Exception) {}
            }
        }
    }
}
