package com.example.dziennik_diabetyka_zpo.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dziennik_diabetyka_zpo.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GlucoseUploadActivity : AppCompatActivity() {

    private lateinit var glucoseEditText: EditText
    private lateinit var insulinEditText: EditText
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_glucose_upload)

        glucoseEditText = findViewById(R.id.upload_tvglucose)
        insulinEditText = findViewById(R.id.upload_tvinsulin)
        saveButton = findViewById(R.id.upload_save_button)

        saveButton.setOnClickListener {
            saveGlucoseAndInsulinResult()
        }
    }

    private fun saveGlucoseAndInsulinResult() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email

        if (userEmail != null) {
            val firestore = FirebaseFirestore.getInstance()

            val currentDate = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault()).format(Date())
            val currentHour = SimpleDateFormat("H", Locale.getDefault()).format(Date()).toInt()

            val glucoseText = glucoseEditText.text.toString()
            val insulinText = insulinEditText.text.toString()

            val glucoseLevel = glucoseText.toDoubleOrNull()
            val insulinLevel = insulinText.toDoubleOrNull()

            val data = hashMapOf<String, Any>()

            glucoseLevel?.let { data["glucose"] = it }
            insulinLevel?.let { data["insulin"] = it }

            if (data.isNotEmpty()) {
                val documentPath = "results/$userEmail/$currentDate/$currentHour"

                firestore.document(documentPath)
                    .get()
                    .addOnSuccessListener { documentSnapshot: DocumentSnapshot? ->
                        if (documentSnapshot?.exists() == true) {
                            Toast.makeText(
                                this,
                                "Dane o tej godzinie zostały już dodane.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            firestore.document(documentPath)
                                .set(data)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "Dane zostały zapisane do Firebase.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this,
                                        "Wystąpił błąd podczas zapisywania danych do Firebase: $e",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Wystąpił błąd podczas sprawdzania danych w Firebase: $e",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } else {
                Toast.makeText(this, "Nie wprowadzono żadnych danych.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Użytkownik nie jest zalogowany.", Toast.LENGTH_SHORT).show()
        }
    }
}
