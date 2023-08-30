package com.sportsintercative.mapbox

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.*
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.gestures.*
import com.mapbox.maps.plugin.locationcomponent.*
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.lifecycle.requireMapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider

//pk.eyJ1Ijoib21rYXItcyIsImEiOiJjbGx2dmNpY2MwMDFhM2xwNnhkYjdzOTdnIn0.NogI6SNoz5uzrXMsCCvIVQ
// Token - sk.eyJ1Ijoib21rYXItcyIsImEiOiJjbGx2eG9xODkxaXAxM2ZvaHNkdXY5d2lrIn0.32sS_DonA7-kFSVUVluZhw

class MainActivity : AppCompatActivity() {
    private var tempEnhancedLocation: Location? = null
    private var mapView: MapView? = null
    private var floatingActionButton: FloatingActionButton? = null
    var enhancedLocation: Location? = null
    private lateinit var mapboxMap: MapboxMap
    private val navigationLocationProvider = NavigationLocationProvider()
    private val mapboxNavigation: MapboxNavigation by requireMapboxNavigation(
        onResumedObserver = object : MapboxNavigationObserver {
            @SuppressLint("MissingPermission")
            override fun onAttached(mapboxNavigation: MapboxNavigation) {
                mapboxNavigation.registerLocationObserver(locationObserver)
                mapboxNavigation.startTripSession()
            }

            override fun onDetached(mapboxNavigation: MapboxNavigation) {
                mapboxNavigation.unregisterLocationObserver(locationObserver)
            }
        },
        onInitialize = this::initNavigation
    )
    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {
            // Not implemented in this example. However, if you want you can also
            // use this callback to get location updates, but as the name suggests
            // these are raw location updates which are usually noisy.
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            Log.d("locationMatcherResult", "$locationMatcherResult")
            enhancedLocation = locationMatcherResult.enhancedLocation
            navigationLocationProvider.changePosition(
                enhancedLocation!!,
                locationMatcherResult.keyPoints,
            )
//            if (tempEnhancedLocation != enhancedLocation) {
//                floatingActionButton!!.backgroundTintList =
//                    getColorStateList(com.mapbox.maps.R.color.mapbox_gray)
//            } else {
//                floatingActionButton!!.backgroundTintList =
//                    getColorStateList(R.color.fabLocationColor)
//            }
            // Invoke this method to move the camera to your current location.
            updateCamera(enhancedLocation!!)
        }
    }

    private val activityResultLauncher = registerForActivityResult<String, Boolean>(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        if (result) {
            Toast.makeText(this@MainActivity, "Permission granted!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapView = findViewById(R.id.mapView1)
        mapView?.getMapboxMap()
            ?.loadStyleUri(Style.TRAFFIC_DAY)
        floatingActionButton = findViewById(R.id.focusLocation)
        floatingActionButton!!.setOnClickListener {
            tempEnhancedLocation = enhancedLocation
            updateCamera(enhancedLocation!!)
        }
        mapboxMap = mapView!!.getMapboxMap()
        mapboxMap.loadStyle(
            style(Style.TRAFFIC_NIGHT) {
                +geoJsonSource(BOUNDS_ID) {
                    featureCollection(FeatureCollection.fromFeatures(listOf()))
                }
            }
        ) { setupBounds(OFFICE_BOUND) }
        showCrosshair()

        val annotationApi = mapView?.annotations
        val circleAnnotationManager = annotationApi?.createCircleAnnotationManager(mapView!!)
        // Set options for the resulting circle layer.
        val circleAnnotationOptions: CircleAnnotationOptions = CircleAnnotationOptions()
            // Define a geographic coordinate.
            .withPoint(Point.fromLngLat(73.78611107643127, 18.54243502525792))
            // Style the circle that will be added to the map.
            .withCircleRadius(8.0)
            .withCircleColor("#ee4e8b")
            .withCircleStrokeWidth(2.0)
            .withCircleStrokeColor("#ffffff")
// Add the resulting circle to the map.
        circleAnnotationManager?.create(circleAnnotationOptions)
    }

    companion object {
        private const val BOUNDS_ID = "BOUNDS_ID"
        private val SAN_FRANCISCO_BOUND: CameraBoundsOptions = CameraBoundsOptions.Builder()
            .bounds(
                CoordinateBounds(
                    Point.fromLngLat(73.78479142959843, 18.54368616252548),
                    Point.fromLngLat(73.78770581338807, 18.53997565870587),
                    false
                )
            )
            .minZoom(10.0)
            .build()
        private val OFFICE_BOUND: CameraBoundsOptions = CameraBoundsOptions.Builder()
            .bounds(
                CoordinateBounds(
                    Point.fromLngLat(73.78601163456085, 18.54137340267236),
                    Point.fromLngLat(73.78796500881278, 18.54037285956325),
                    false
                )
            )
            .minZoom(10.0)
            .build()

        private val ALMOST_WORLD_BOUNDS: CameraBoundsOptions = CameraBoundsOptions.Builder()
            .bounds(
                CoordinateBounds(
                    Point.fromLngLat(-170.0, -20.0),
                    Point.fromLngLat(170.0, 20.0),
                    false
                )
            )
            .minZoom(2.0)
            .build()

        @SuppressLint("Range")
        private val CROSS_IDL_BOUNDS: CameraBoundsOptions = CameraBoundsOptions.Builder()
            .bounds(
                CoordinateBounds(
                    Point.fromLngLat(170.0202020, -20.0),
                    Point.fromLngLat(190.0, 20.0),
                    false
                )
            )
            .minZoom(2.0)
            .build()

        private val INFINITE_BOUNDS: CameraBoundsOptions = CameraBoundsOptions.Builder()
            .bounds(
                CoordinateBounds(
                    Point.fromLngLat(0.0, 0.0),
                    Point.fromLngLat(0.0, 0.0),
                    true
                )
            )
            .build()
    }

    private fun setupBounds(bounds: CameraBoundsOptions) {
        mapboxMap.setBounds(bounds)
        showBoundsArea(bounds)
    }

    private fun showCrosshair() {
        val crosshair = View(this)
        crosshair.layoutParams = FrameLayout.LayoutParams(10, 10, Gravity.CENTER)
        crosshair.setBackgroundColor(Color.BLUE)
        mapView!!.addView(crosshair)
    }

    private fun showBoundsArea(boundsOptions: CameraBoundsOptions) {
        val source = mapboxMap.getStyle()!!.getSource(BOUNDS_ID) as GeoJsonSource
        val bounds = boundsOptions.bounds
        val list = mutableListOf<List<Point>>()
        bounds?.let {
            if (!it.infiniteBounds) {
                val northEast = it.northeast
                val southWest = it.southwest
                val northWest = Point.fromLngLat(southWest.longitude(), northEast.latitude())
                val southEast = Point.fromLngLat(northEast.longitude(), southWest.latitude())
                list.add(
                    mutableListOf(northEast, southEast, southWest, northWest, northEast)
                )
            }
        }
        source.geometry(
            Polygon.fromLngLats(
                list
            )
        )
    }


    private fun updateCamera(location: Location) {
        val mapAnimationOptions = MapAnimationOptions.Builder().duration(1500L).build()
        mapView!!.camera.easeTo(
            CameraOptions.Builder()
                // Centers the camera to the lng/lat specified.
                .center(Point.fromLngLat(location.longitude, location.latitude))
                // specifies the zoom value. Increase or decrease to zoom in or zoom out
                .zoom(12.0)
                // specify frame of reference from the center.
                .padding(EdgeInsets(0.0, 0.0, 0.0, 0.0))
                .build(),
            mapAnimationOptions
        )
    }

    private fun initNavigation() {
        MapboxNavigationApp.setup(
            NavigationOptions.Builder(this)
                .accessToken(getString(R.string.mapbox_access_token))
                .build()
        )
        // Instantiate the location component which is the key component to fetch location updates.
        mapView!!.location.apply {
            setLocationProvider(navigationLocationProvider)
            // Uncomment this block of code if you want to see a circular puck with arrow.
/*
            locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.mapbox_navigation_puck_icon
                )
            )
*/
            // When true, the blue circular puck is shown on the map. If set to false, user
            // location in the form of puck will not be shown on the map.
            enabled = true
        }
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }
}