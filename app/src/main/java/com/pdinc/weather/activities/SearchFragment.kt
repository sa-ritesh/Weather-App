package com.pdinc.weather.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pdinc.weather.R
import com.pdinc.weather.databinding.FragmentSearchBinding
import com.pdinc.weather.network.WeatherRemoteDataSource
import com.pdinc.weather.network.WeatherRemoteDataSourceImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {
    private lateinit var bindings: FragmentSearchBinding
    private val searchName = WeatherRemoteDataSourceImpl()
    private val LOCATION_REQUEST_CODE = 2
    val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    )
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var request: LocationRequest
    private lateinit var builder: LocationSettingsRequest.Builder
    private lateinit var locationCallback: LocationCallback

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        bindings = view?.let { FragmentSearchBinding.bind(it) }!!

        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.navigate, menu)
        val searchItem = menu.findItem(R.id.searchPlace)
        val searchView = searchItem.actionView as SearchView
        searchView.onActionViewExpanded()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null && query.trim().isNotEmpty()) {
                    GlobalScope.launch(Dispatchers.IO) {
                        searchByName(query)
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText!!.isNotEmpty()) {
                    GlobalScope.launch(Dispatchers.IO) {
                        searchByName(newText)
                    }
                }
                return false
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                startGPS()
            } else {
                val showRationale = shouldShowRequestPermissionRationale(permissions[0])
                if (!showRationale) {
                    //Never ask again
                    permissionExplanation()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private suspend fun searchByName(query: String) {
        searchName.getCurrentWeatherBySearch(query)
    }

    private fun hasPermissions(context: Context, vararg permissions: String): Boolean =
            permissions.all {
                ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
    private fun openPermissionSetting() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", activity?.packageName, null)
        intent.data = uri
        startActivityForResult(intent, 0)
    }

    private fun permissionExplanation() {

        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MaterialComponents_Dialog_Alert)
        builder.setTitle(getString(R.string.location_required))
        builder.setMessage(getString(R.string.access_to_gps))
        builder.apply {
            setPositiveButton(
                   R.string.ok
            ) { dialog, _ ->
                dialog.dismiss()
                if (shouldShowRequestPermissionRationale(permissions[0]) || shouldShowRequestPermissionRationale(
                                permissions[1]
                        )
                )
                    requestPermissions(
                            permissions,
                            LOCATION_REQUEST_CODE
                    )
                else
                    openPermissionSetting()
            }
            setNegativeButton(
                    R.string.cancel
            ) { dialog, _ ->
                dialog.dismiss()
            }
        }
        builder.create()
        builder.show()

    }

    private fun startGPS() {

        request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

        builder = LocationSettingsRequest.Builder().addLocationRequest(request)

        val result: Task<LocationSettingsResponse> =
                LocationServices.getSettingsClient(context)
                        .checkLocationSettings(builder.build())

        result.addOnFailureListener {
            if (it is ResolvableApiException) {
                try {
                    val resolvable = it
                    resolvable.startResolutionForResult(requireActivity(), 8990)
                } catch (ex: IntentSender.SendIntentException) {
                    ex.printStackTrace()
                }
            }
        }
    }
}