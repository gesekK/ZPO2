package com.example.dziennik_diabetyka_zpo.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.dziennik_diabetyka_zpo.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ActivityChartsActivity : AppCompatActivity() {

    private lateinit var weightChart: BarChart
    private lateinit var activityChart: BarChart
    private lateinit var sleepChart: BarChart

    private val weightEntries = mutableListOf<BarEntry>()
    private val activityEntries = mutableListOf<BarEntry>()
    private val sleepEntries = mutableListOf<BarEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charts)

        val addActivityBtn: FloatingActionButton = findViewById(R.id.add_activity_btn)
        addActivityBtn.setOnClickListener {
            val intent = Intent(this, ActivityUploadActivity::class.java)
            startActivity(intent)
        }


        weightChart = findViewById(R.id.actChart_weight)
        activityChart = findViewById(R.id.actChart_activity)
        sleepChart = findViewById(R.id.actChart_sleep)

        setupChart(weightChart)
        setupChart(activityChart)
        setupChart(sleepChart)

        val currentUser = FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email

        if (userEmail != null) {
            val firestore = FirebaseFirestore.getInstance()
            loadChartData(userEmail, firestore)
        }
    }

    private fun setupChart(chart: BarChart) {
        chart.setTouchEnabled(false)
        chart.isDragEnabled = false
        chart.setScaleEnabled(true)
        chart.setPinchZoom(false)
        chart.description.isEnabled = true
        chart.legend.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.axisLeft.setDrawGridLines(false)
    }

    private fun loadChartData(userEmail: String, firestore: FirebaseFirestore) {
        val sdf = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault())

        val calendar = Calendar.getInstance()
        calendar.time = Date()

        val xValues = mutableListOf<String>() // Lista etykiet dla osi x

        for (i in 6 downTo 0) {
            val currentDate = calendar.time
            val currentDateString = sdf.format(currentDate)

            val dayOfWeek = getDayOfWeek(currentDate) // Pobranie skrótu dnia tygodnia
            xValues.add(0, dayOfWeek) // Dodanie skrótu dnia tygodnia na początek listy etykiet

            firestore.collection("activity")
                .document(userEmail)
                .collection(currentDateString)
                .document("data")
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val weight = document.getDouble("weight")?.toFloat() ?: 0f
                        weightEntries.add(BarEntry(i.toFloat(), weight))
                        Log.d("FirebaseData", "Weight: $weight")
                    } else {
                        weightEntries.add(BarEntry(i.toFloat(), 0f))
                    }
                    setupWeightChart(xValues) // Przekazanie listy etykiet do metody setupWeightChart
                }
                .addOnFailureListener { exception ->
                    weightEntries.add(BarEntry(i.toFloat(), 0f))
                    Log.e("FirebaseError", "Error retrieving weight data", exception)
                    setupWeightChart(xValues) // Przekazanie listy etykiet do metody setupWeightChart
                }

            firestore.collection("activity")
                .document(userEmail)
                .collection(currentDateString)
                .document("data")
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val activityLevel = document.getDouble("activity_level")?.toFloat() ?: 0f
                        activityEntries.add(BarEntry(i.toFloat(), activityLevel))
                        Log.d("FirebaseData", "Activity Level: $activityLevel")
                    } else {
                        activityEntries.add(BarEntry(i.toFloat(), 0f))
                    }
                    setupActivityChart(xValues) // Przekazanie listy etykiet do metody setupActivityChart
                }
                .addOnFailureListener { exception ->
                    activityEntries.add(BarEntry(i.toFloat(), 0f))
                    Log.e("FirebaseError", "Error retrieving activity level data", exception)
                    setupActivityChart(xValues) // Przekazanie listy etykiet do metody setupActivityChart
                }

            firestore.collection("activity")
                .document(userEmail)
                .collection(currentDateString)
                .document("data")
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val sleepMinutes = document.getDouble("sleep_minutes")?.toFloat() ?: 0f
                        val sleepHours = sleepMinutes / 60 // Przeliczenie minut snu na godziny
                        sleepEntries.add(BarEntry(i.toFloat(), sleepHours))
                        Log.d("FirebaseData", "Sleep Minutes: $sleepMinutes")
                    } else {
                        sleepEntries.add(BarEntry(i.toFloat(), 0f))
                    }
                    setupSleepChart(xValues) // Przekazanie listy etykiet do metody setupSleepChart
                }
                .addOnFailureListener { exception ->
                    sleepEntries.add(BarEntry(i.toFloat(), 0f))
                    Log.e("FirebaseError", "Error retrieving sleep minutes data", exception)
                    setupSleepChart(xValues) // Przekazanie listy etykiet do metody setupSleepChart
                }

            calendar.add(Calendar.DAY_OF_YEAR, -1) // Odejmowanie 1 dnia
        }
    }


    private fun getDayOfWeek(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        return when (dayOfWeek) {
            Calendar.MONDAY -> "PN"
            Calendar.TUESDAY -> "WT"
            Calendar.WEDNESDAY -> "ŚR"
            Calendar.THURSDAY -> "CZ"
            Calendar.FRIDAY -> "PT"
            Calendar.SATURDAY -> "SB"
            Calendar.SUNDAY -> "ND"
            else -> ""
        }
    }

    private fun setupWeightChart(xValues: List<String>) {
        if (weightEntries.size == 7) {
            val weightDataSet = BarDataSet(weightEntries, "Weight")
            weightDataSet.color = resources.getColor(R.color.green, null)
            weightDataSet.setDrawValues(false) // Ukrycie wartości Y

            val weightData = BarData(weightDataSet)

            weightChart.data = weightData
            weightChart.xAxis.valueFormatter = IndexAxisValueFormatter(xValues)
            weightChart.invalidate()
        }
    }

    private fun setupActivityChart(xValues: List<String>) {
        if (activityEntries.size == 7) {
            val activityDataSet = BarDataSet(activityEntries, "Activity Level")
            activityDataSet.color = resources.getColor(R.color.blue, null)
            activityDataSet.setDrawValues(false) // Ukrycie wartości Y

            val activityData = BarData(activityDataSet)

            activityChart.data = activityData
            activityChart.xAxis.valueFormatter = IndexAxisValueFormatter(xValues)
            activityChart.invalidate()
        }
    }

    private fun setupSleepChart(xValues: List<String>) {
        if (sleepEntries.size == 7) {
            val sleepDataSet = BarDataSet(sleepEntries, "Sleep Hours")
            sleepDataSet.color = resources.getColor(R.color.orange, null)
            sleepDataSet.setDrawValues(false) // Ukrycie wartości Y

            val sleepData = BarData(sleepDataSet)

            sleepChart.data = sleepData
            sleepChart.xAxis.valueFormatter = IndexAxisValueFormatter(xValues)
            sleepChart.invalidate()
        }
    }
}
