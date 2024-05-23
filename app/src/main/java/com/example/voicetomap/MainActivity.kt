package com.example.voicetomap

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.voicetomap.R.menu
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import java.util.Locale

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    private lateinit var mGoogleMap: GoogleMap
    private lateinit var lastLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var autocompleteFragment: AutocompleteSupportFragment
    private lateinit var markerLocation: LatLng
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechRecognizerIntent: Intent

    // Vibration
    fun vibration(v: View){
        val vibrate = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrate.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    companion object {
        private const val LOCATION_REQUEST_CODE = 1
        private const val SPEECH_REQUEST_CODE = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Places.initialize(applicationContext, getString(R.string.google_map_api_key))
        autocompleteFragment = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG))
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onError(p0: Status) {
                //Toast.makeText(this@MainActivity, "", Toast.LENGTH_SHORT).show()
            }

            override fun onPlaceSelected(place: Place) {
                val latLng = place.latLng!!
                markerLocation = latLng // Seçilen konumu markerLocation'a ata
                zoomOnMap(latLng)
            }
        })

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val mapOptionButton: ImageButton = findViewById(R.id.mapOptionsMenu)
        val popupMenu = PopupMenu(this, mapOptionButton)
        popupMenu.menuInflater.inflate(menu.map_options, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            changeMap(menuItem.itemId)
            true
        }

        mapOptionButton.setOnClickListener {
            popupMenu.show()
        }

        val micOptionButton: ImageButton = findViewById(R.id.micOptionsMenu)
        micOptionButton.setOnClickListener {
            startListening()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.create().apply {
            interval = 1000 // 1 second delay
            fastestInterval = 500 // 0.5 second delay
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                for (location in locationResult.locations){
                    if (location != null) {
                        //mGoogleMap.clear()
                        val current_Lat_Long = LatLng(location.latitude, location.longitude)
                        //placeMarkerOnMap(current_Lat_Long)

                        // Söylenen kelimeye göre hangi konumun işaretleneceğinin kontrolü
                        val command = intent.getStringExtra("command")
                        if (command == "ev") {
                            markerLocation = LatLng(41.0082, 28.9784) // Ev kelimesine ait konum
                            placeMarkerOnMap(markerLocation)
                        } else if (command == "okul") {
                            markerLocation = LatLng(40.82348954171715, 29.92532549420805) // Okul kelimesine ait konum
                            placeMarkerOnMap(markerLocation)
                        }

                        //mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(current_Lat_Long))
                    }
                }
            }
        }

        val startNavigationButton = findViewById<Button>(R.id.startNavigationButton)
        startNavigationButton.setOnClickListener {
            vibration(it)
            if (::markerLocation.isInitialized) {
                startNavigation(markerLocation)
            } else {
                Toast.makeText(this, "Lütfen bir konum seçin.", Toast.LENGTH_SHORT).show()
            }
        }

        // Initialize Speech Recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {}
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    autocompleteFragment.setText(matches[0])
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        speechRecognizer.startListening(speechRecognizerIntent)
    }

    private fun startNavigation(location: LatLng) {
        val gmmIntentUri = Uri.parse("google.navigation:q=${location.latitude},${location.longitude}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(packageManager) != null) {
            try {
                startActivity(mapIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Google Haritalar uygulaması açılamadı: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Google Haritalar uygulaması bulunamadı.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap
        mGoogleMap.uiSettings.isZoomControlsEnabled = true
        mGoogleMap.setOnMarkerClickListener(this)
        setUpMap()
    }

    private fun changeMap(itemId: Int) {
        when (itemId) {
            R.id.normal_map -> mGoogleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            R.id.hybrid_map_map -> mGoogleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            R.id.satallite_map -> mGoogleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            R.id.terrain_map -> mGoogleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
        }
    }

    private fun zoomOnMap(latLng: LatLng) {
        val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(latLng, 12f)
        mGoogleMap.animateCamera(newLatLngZoom)
        placeMarkerOnMap(latLng)
    }

    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
            return
        }
        mGoogleMap.isMyLocationEnabled = true
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null /* Looper */)
    }

    private fun placeMarkerOnMap(latLng: LatLng) {
        val markerOptions = MarkerOptions().position(latLng)
        mGoogleMap.addMarker(markerOptions)
    }

    override fun onMarkerClick(p0: Marker) = false
}







