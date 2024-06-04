package camp.letsbuild.inventoryscanner

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val TAG = "AddItemsToVehicleActivity"

class AddItemsToLocationLandingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scannerLauncher = scannerForNewActivity(this, AddItemsToLocationActivity::class.java)
        setContent {
            Column (modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = { scannerLauncher.launch(ScanOptions()) }) {
                    Text("Scan Location")
                }
            }
        }
    }
}

class AddItemsToLocationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val containerId = intent.getStringExtra("barcode_id") ?: return
        val intent = Intent(this, AddSingleItemToLocationActivity::class.java)
        intent.putExtra("containerId", containerId)
        val scannerForAddSingleItem = registerForActivityResult(ScanContract()) { scannedBarcode: ScanIntentResult ->
            run {
                if (scannedBarcode.contents == null) {
                    Toast.makeText(this@AddItemsToLocationActivity, "Cancelled", Toast.LENGTH_LONG).show()
                } else {
                    val itemId = scannedBarcode.contents
                    getInventoryApiInstance(this).getParentOfItem(itemId).enqueue(object :
                        Callback<String> {
                        override fun onResponse(call: Call<String>, response: Response<String>) {
                            Log.d(TAG, "Getting parent of item")
                            if (response.isSuccessful && response.body() != null) {
                                val itemParent = response.body()!!
                                Log.d(TAG, itemParent)
                                if (itemParent.isNotBlank()) {
                                    Toast.makeText(this@AddItemsToLocationActivity, "Item is already in a container!!", Toast.LENGTH_LONG).show()
                                } else {
                                    intent.putExtra("itemId", itemId)
                                    startActivity(intent)
                                }
                            } else {
                                TODO("Not yet implemented")
                            }
                        }

                        override fun onFailure(call: Call<String>, t: Throwable) {
                            TODO("Not yet implemented")
                        }
                    })
                }
            }
        }
        setContent {
            AddItemsToContainerUI(
                containerId,
                containerId,
                scannerForAddSingleItem,
                this
            )
        }
    }
}

class AddSingleItemToLocationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val itemId = intent.getStringExtra("itemId")
        val containerId = intent.getStringExtra("containerId")
        if (itemId == null || containerId == null) {
            Log.e(TAG, "itemId or containerId is null")
            return
        }
        setContent {
            AddSingleItemToContainerUI(itemId, containerId, this,
                {inventoryApi, containerId, itemIds -> inventoryApi.addItemsToLocation(containerId, itemIds)})
        }
    }
}