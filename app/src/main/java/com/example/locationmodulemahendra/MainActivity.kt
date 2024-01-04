package com.example.locationmodulemahendra
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.errors.ApiException
import com.google.maps.errors.OverQueryLimitException
import com.google.maps.model.DirectionsResult
import com.google.maps.model.TravelMode
import java.io.IOException


//activity for show distance,route and location icon
class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
   private  val apiKey = "AIzaSyCR_3j59lgikagDIvdXRrLYkQjt8R8bN8U"



    private val origin = createLocation(19.99727000, 73.79096000,"Nashik")
    private val deviceLocations = listOf(
        createLocation(20.20000000, 73.83305556, "Dindori"),
        createLocation(18.520430, 73.856743, "Pune"),
        createLocation(42.08320000, 42.52163000, "Vani"),
        createLocation(19.218330, 72.978088, "Thane"),
        createLocation(28.535517, 77.391029, "Noida"),
        )
    private lateinit var mMap: GoogleMap

    private lateinit var near:TextView
    data class PolylineInfo(val note: String, val distance: String)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        near=findViewById(R.id.distanceNearest)
        if (isLocationPermissionGranted()) {
            initializeMap()
        } else {
            requestLocationPermission()
        }

    }





    @SuppressLint("SetTextI18n", "PotentialBehaviorOverride")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        try {
    val nearestLocation = findNearestLocation(origin,deviceLocations)
    if(nearestLocation!=null){
        Toast.makeText(this, "Nearest Location not null", Toast.LENGTH_SHORT).show()
        //move Camera to Origin
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origin.first, 8f))
        //add marker
        addMarker()
        //draw route from origin to multiple location
        val dis=calculateDistancefromOriginToDest(origin.first,nearestLocation.first)
        val distanceText = "Nearest Location: ${nearestLocation.second},distance : $dis "
        near.text=distanceText
        Toast.makeText(this, distanceText, Toast.LENGTH_SHORT).show()
        for (location in deviceLocations) {
            val result = getDirections(origin.first, location.first)
            if (result.first != null && result.second != null) {
                val flag = if (location.first == nearestLocation.first) 1 else 0
                drawRoute(result.first!!, location.second, flag)
                val distance = calculateDistance(result.first!!, result.second)
                Toast.makeText(this, "Distance between ${origin.second} and ${location.second}: $distance km", Toast.LENGTH_SHORT).show()
                //Toast.makeText(this, "${result.second}", Toast.LENGTH_SHORT).show()
            }
        }
        //set on click listner on route
        mMap.setOnPolylineClickListener { polylines2 ->
            val polylineInfo = polylines2.tag as? PolylineInfo
            polylineInfo?.let {
                val distance = it.distance
                val note = it.note
                val distanceText2 = "Distance: $distance km, Name: $note"
                Toast.makeText(this@MainActivity, distanceText2, Toast.LENGTH_SHORT).show()
            }
        }
    }
    else{
        Toast.makeText(this, "Nearest Location is Null", Toast.LENGTH_SHORT).show()
    }
      }catch(e:Exception){
    Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
     }
    }
    private fun addMarker() {
        mMap.addMarker(MarkerOptions().position(origin.first).title("origin"))
        for (loc in deviceLocations) {
            mMap.addMarker(MarkerOptions().position(loc.first).title(loc.second))
        }
    }
    //for creat location
   private  fun createLocation(latitude: Double, longitude: Double, name: String): Pair<LatLng, String> {
        return LatLng(latitude, longitude) to name
    }
    //for get Direction
    private fun getDirections(origin: LatLng, destination: LatLng): Pair<DirectionsResult?, Double?> {
        val context = GeoApiContext.Builder()
            .apiKey("$apiKey")
            .build()

        return try {
            val directions = DirectionsApi.newRequest(context)
                .mode(TravelMode.DRIVING)
                .origin(com.google.maps.model.LatLng(origin.latitude, origin.longitude))
                .destination(com.google.maps.model.LatLng(destination.latitude, destination.longitude))
                .await()

            val distanceInKm =
                directions?.routes?.getOrNull(0)?.legs?.getOrNull(0)?.distance?.inMeters?.toDouble()
                    ?.div(1000)

            directions to distanceInKm
        } catch (e: ApiException) {
            println("ApiException: ${e.message}")
            Toast.makeText(this, "ApiException${e.message}", Toast.LENGTH_SHORT).show()
            null to null
        } catch (e: OverQueryLimitException) {
            Toast.makeText(this, "OverQueryLimitException${e.message}", Toast.LENGTH_SHORT).show()

            println("OverQueryLimitException: ${e.message}")
            null to null
        } catch (e: IOException) {
            Toast.makeText(this, "IOException${e.message}", Toast.LENGTH_SHORT).show()

            println("IOException: ${e.message}")
            null to null
        } catch (e: Exception) {
            Toast.makeText(this, "Exception${e.message}", Toast.LENGTH_SHORT).show()

            println("Exception: ${e.message}")
            null to null
        }
    }
    //for draw  route
    private fun drawRoute(directionsResult: DirectionsResult, note: String, flag: Int) {
        val polylineOptions = PolylineOptions()
            .width(5f)
            .clickable(true)
            .color(when (flag) {
                0 -> Color.RED
                1 -> Color.BLUE
                else -> Color.BLUE
            })
        val legs = directionsResult.routes[0].legs
        for (leg in legs) {
            val steps = leg.steps
            steps.forEach { step ->
                val points = step.polyline.decodePath()
                points.forEach { point ->
                    polylineOptions.add(LatLng(point.lat, point.lng))
                }
            }
        }
        val polylines2 = mMap.addPolyline(polylineOptions)
        //for find distance
        try {
            val distance2 = directionsResult.routes[0].legs.sumOf {
                it.distance.inMeters.toDouble() / 1000
            }.toFloat()
            polylines2.tag = PolylineInfo(note, distance2.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun calculateDistance(directionsResult: DirectionsResult, distanceInKm: Double?): Number {
        return distanceInKm ?: directionsResult.routes[0].legs.sumOf {
            it.distance.inMeters.toDouble() / 1000
        }.toFloat()
    }
    //calculate Distance between origin and destination
    private fun calculateDistancefromOriginToDest(origin: LatLng, destination: LatLng): Double? {
        val context = GeoApiContext.Builder()
            .apiKey("$apiKey")
            .build()

        return try {
            val directions = DirectionsApi.newRequest(context)
                .mode(TravelMode.DRIVING)
                .origin(com.google.maps.model.LatLng(origin.latitude, origin.longitude))
                .destination(com.google.maps.model.LatLng(destination.latitude, destination.longitude))
                .await()

            directions?.routes?.getOrNull(0)?.legs?.getOrNull(0)?.distance?.inMeters?.toDouble()?.div(1000)
        } catch (e: ApiException) {
            println("ApiException: ${e.message}")
            null
        } catch (e: OverQueryLimitException) {
            println("OverQueryLimitException: ${e.message}")
            null
        } catch (e: IOException) {
            println("IOException: ${e.message}")
            null
        } catch (e: Exception) {
            println("Exception: ${e.message}")
            null
        }
    }

     //for find Nearest Location
    private fun findNearestLocation(
        origin: Pair<LatLng, String>,
        destinations: List<Pair<LatLng, String>>
    ): Pair<LatLng, String>? {
        if (destinations.isEmpty()) return null

        var nearestLocation = destinations.first()
        var minDistance = calculateDistancefromOriginToDest(origin.first, nearestLocation.first)

        for (destination in destinations) {
            val distance = calculateDistancefromOriginToDest(origin.first, destination.first)
            if (distance != null && minDistance != null) {
                if (distance < minDistance) {
                    minDistance = distance
                    nearestLocation = destination
                }
            }
        }

        return nearestLocation
    }
    //for check permission
    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    //request Location Permission
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }
    //Handle Permission if user allow , then initialize map otherwise close app
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, you can proceed
                    // Start using location-related functionality
                    initializeMap()
                } else {
                    // Permission denied, inform the user
                    // You might want to display a dialog or request the permission again
                    Toast.makeText(this, "Permission denied. Closing the app.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
   //For Initialize Map
    private fun initializeMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        Toast.makeText(this, "Initialize Map", Toast.LENGTH_SHORT).show()
    }
}

//    private fun findNearestLocation(origin: Pair<LatLng,String>, destinations: List<Pair<LatLng, String>>):Pair<LatLng,String>{
//        var nearestLocation = destinations.first()
//        var minDistance = calculateDistancefromOriginToDest(origin.first, nearestLocation.first)
//
//        for (destination in destinations) {
//            val distance = calculateDistancefromOriginToDest(origin.first, destination.first)
//            if (distance != null) {
//                if (distance < minDistance!!) {
//                    minDistance = distance
//                    nearestLocation = destination
//                }
//            }
//        }
//
//        return nearestLocation
//    }


