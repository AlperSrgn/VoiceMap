package com.example.voicetomap

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    private lateinit var mGoogleMap: GoogleMap
    private lateinit var lastLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    companion object{
        private const val LOCATION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

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
                        mGoogleMap.clear()
                        val current_Lat_Long = LatLng(location.latitude, location.longitude)
                        placeMarkerOnMap(current_Lat_Long)

                        // Söylenen kelimeye göre hangi konumun işaretleneceğinin kontrolü
                        val command = intent.getStringExtra("command")
                        if (command == "ev") {
                            val home_Lat_Long = LatLng(41.0082, 28.9784) // Ev kelimesine ait konum
                            placeMarkerOnMap(home_Lat_Long)
                        } else if (command == "okul") {
                            val car_Lat_Long = LatLng(40.82348954171715, 29.92532549420805) // Okul kelimesine ait konum
                            placeMarkerOnMap(car_Lat_Long)
                        }

                        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(current_Lat_Long))
                    }
                }
            }
        }


    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap
        mGoogleMap.uiSettings.isZoomControlsEnabled = true
        mGoogleMap.setOnMarkerClickListener ( this )
        setUpMap()
    }

    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
            return
        }
        mGoogleMap.isMyLocationEnabled = true
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null /* Looper */)
    }

    private fun placeMarkerOnMap(current_Lat_Long: LatLng) {
        val markerOptions = MarkerOptions().position(current_Lat_Long)
        markerOptions.title("$current_Lat_Long")
        mGoogleMap.addMarker(markerOptions)
    }

    override fun onMarkerClick(p0: Marker) = false
}
