package com.example.voicetomap


import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class SpeechToText : AppCompatActivity() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var editText: EditText? = null
    private var micBtn: ImageView? = null
    private var showMapBtn: Button? = null
    private var btnCamera: Button? = null


    // Vibration
    fun vibration(v: View){
        val vibrate = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrate.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_to_text)

        editText = findViewById(R.id.editText)
        micBtn = findViewById(R.id.ImageView)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        showMapBtn = findViewById(R.id.ShowMap)
        btnCamera = findViewById(R.id.buttonCamera)


        //Haritayı göster
        showMapBtn?.setOnClickListener {
            vibration(it)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        //Çevreyi algıla
        btnCamera?.setOnClickListener {
            vibration(it)
            val intent = Intent(this, ObjectDetection::class.java)
            startActivity(intent)
        }


        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        speechRecognizer!!.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) {}

            override fun onBeginningOfSpeech() {
                editText!!.setText("")
                editText!!.setHint("Listening...")
            }

            override fun onRmsChanged(p0: Float) {}

            override fun onBufferReceived(p0: ByteArray?) {}

            override fun onEndOfSpeech() {}

            override fun onError(p0: Int) {}

            override fun onResults(bundle: Bundle?) {
                micBtn!!.setImageResource(R.drawable.ic_mic_green)
                val data = bundle!!.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                //editText!!.setText(data!![0])
                val recognizedText = data!![0].toLowerCase(Locale.getDefault())
                editText!!.setText(recognizedText)

                // "EV" KELİME KONTROLÜ
                if (data[0].contains("ev")) {
                    val text = "Ev konumu için rota oluşturuldu."
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null)
                    showMapBtn?.performClick()

                    // Haritaya "ev" kelimesini gönderme
                    val intent = Intent(this@SpeechToText, MainActivity::class.java)
                    intent.putExtra("command", "ev") // Söylenen kelime
                    startActivity(intent)
                }
                // "OKUL" KELİME KONTROLÜ
                else if (data[0].contains("okul")) {
                    val text = "Okul konumu için rota oluşturuldu."
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null)
                    showMapBtn?.performClick()

                    // Haritaya "okul" kelimesini gönderme
                    val intent = Intent(this@SpeechToText, MainActivity::class.java)
                    intent.putExtra("command", "okul") // Söylenen kelime
                    startActivity(intent)
                }

                // "DURAK" KELİME KONTROLÜ
                else if (data[0].contains("Durak")) {
                    val text = "Durak konumu için rota oluşturuldu."
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null)
                    showMapBtn?.performClick()

                    // Haritaya "durak" kelimesini gönderme
                    val intent = Intent(this@SpeechToText, MainActivity::class.java)
                    intent.putExtra("command", "Durak") // Söylenen kelime
                    startActivity(intent)
                }

            }


            override fun onPartialResults(p0: Bundle?) {
                TODO("Not yet implemented")
            }

            override fun onEvent(p0: Int, p1: Bundle?) {
                TODO("Not yet implemented")
            }

            // TextToSpeech
            var tts = TextToSpeech(applicationContext) { status -> if (status != TextToSpeech.ERROR) { } }

        })

        micBtn!!.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                speechRecognizer!!.stopListening()
            }
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                micBtn!!.setImageResource(R.drawable.ic_mic_red)
                speechRecognizer!!.startListening(speechRecognizerIntent)
            }
            false
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            checkPermissions()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer!!.destroy()
    }

    private fun checkPermissions() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.RECORD_AUDIO),
            RecordAudioRequestCode
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RecordAudioRequestCode && grantResults.isNotEmpty()) {
            Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val RecordAudioRequestCode = 1
    }
}
