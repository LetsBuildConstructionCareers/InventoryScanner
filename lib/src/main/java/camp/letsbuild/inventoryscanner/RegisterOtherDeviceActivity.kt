package camp.letsbuild.inventoryscanner

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val TAG = "RegisterOtherDeviceActivity"

fun launchRegisterOtherDeviceActivity(componentActivity: ComponentActivity) {
    getInventoryApiInstance(componentActivity).getUnregisteredDevices().enqueue(object : Callback<List<String>> {
        override fun onResponse(call: Call<List<String>>, response: Response<List<String>>) {
            if (response.isSuccessful && response.body() != null) {
                val intent = Intent(componentActivity, RegisterOtherDeviceActivity::class.java)
                val unregisteredDevices = response.body()!!.toTypedArray()
                Log.d(TAG, "unregisteredDevices: ${unregisteredDevices.toList()}")
                intent.putExtra("unregisteredDevices", unregisteredDevices)
                componentActivity.startActivity(intent)
            }
        }

        override fun onFailure(call: Call<List<String>>, t: Throwable) {
            Log.e(TAG, t.toString())
            TODO("Not yet implemented")
        }
    })
}
class RegisterOtherDeviceActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val unregisteredDevices = intent.getSerializableExtra("unregisteredDevices") as Array<String>? ?: emptyArray()
        Log.d(TAG, "unregisteredDevices: $unregisteredDevices")
        val intent = Intent(this, FinalizeRegisterOtherDeviceActivity::class.java)
        val scannerForNewDevice = registerForActivityResult(ScanContract()) { scannedBarcode: ScanIntentResult ->
            run {
                if (scannedBarcode.contents == null) {
                    Toast.makeText(this@RegisterOtherDeviceActivity, "Cancelled", Toast.LENGTH_LONG).show()
                } else {
                    intent.putExtra("newDeviceBarcodeId", scannedBarcode.contents)
                    startActivity(intent)
                    finish()
                }
            }
        }
        setContent {
            Column (verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.verticalScroll(
                rememberScrollState()
            )) {
                var selectedDevice by remember { mutableStateOf("") }
                Text("Select Device to Register:")
                for (unregisteredDevice in unregisteredDevices) {
                    Log.d(TAG, "unregisteredDevice: $unregisteredDevice")
                    Card(onClick = { selectedDevice = unregisteredDevice }) {
                        if (unregisteredDevice == selectedDevice) {
                            Text("$unregisteredDevice ✓")
                        } else {
                            Text(unregisteredDevice)
                        }
                    }
                }
                Button(onClick = {
                    intent.putExtra("selectedDeviceAndroidId", selectedDevice)
                    scannerForNewDevice.launch(ScanOptions())
                }, enabled = selectedDevice != "") {
                    Text("OK")
                }
            }
        }
    }
}

class FinalizeRegisterOtherDeviceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val selectedDeviceAndroidId = intent.getStringExtra("selectedDeviceAndroidId") ?: return
        val selectedDeviceBarcodeId = intent.getStringExtra("newDeviceBarcodeId") ?: return
        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Registering Following Device:")
                Text("Hardware ID: $selectedDeviceAndroidId")
                Text("Barcode ID: $selectedDeviceBarcodeId")
                Button(onClick = { getInventoryApiInstance(this@FinalizeRegisterOtherDeviceActivity)
                    .registerDeviceId(selectedDeviceAndroidId, selectedDeviceBarcodeId)
                    .enqueue(object : Callback<ResponseBody> {
                        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                            finish()
                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            TODO("Not yet implemented")
                        }
                    }) }) {
                    Text("Finish")
                }
            }
        }
    }
}