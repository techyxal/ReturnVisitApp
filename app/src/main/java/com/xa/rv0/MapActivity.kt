package com.xa.rv0

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.xa.rv0.databinding.ActivityMapBinding
import com.xa.rv0.viewmodel.MapViewModel

class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMapBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var viewModel: MapViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(MapViewModel::class.java)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true

        viewModel.contactsForMap.observe(this) { contacts ->
            googleMap.clear() // Clear existing markers before adding new ones
            for (contact in contacts) {
                if (contact.latitude != 0.0 || contact.longitude != 0.0) {
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(LatLng(contact.latitude, contact.longitude))
                            .title(contact.name)
                            .snippet(contact.address)
                    )
                }
            }

            val firstValidContact = contacts.firstOrNull { it.latitude != 0.0 || it.longitude != 0.0 }
            if (firstValidContact != null) {
                googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(firstValidContact.latitude, firstValidContact.longitude), 12f
                    )
                )
            } else {
                val defaultLocation = LatLng(6.8778, 3.3323) // Example: Lagos, Nigeria (adjust as needed)
                googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        defaultLocation, 10f
                    )
                )
            }
        }
        viewModel.loadContactsForMap() // Trigger loading contacts when map is ready
    }

    override fun finish() {
        super.finish()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
}