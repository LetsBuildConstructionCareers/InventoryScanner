package camp.letsbuild.inventoryscanner

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import okhttp3.ResponseBody
import okhttp3.internal.toImmutableMap
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val TAG = "ToolshedCheckinActivity"

class ToolshedCheckinActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scannerLauncher = registerForActivityResult(ScanContract()) { scannedBarcode: ScanIntentResult ->
            run {
                if (scannedBarcode.contents == null) {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
                } else {
                    val intent = Intent(this, ToolshedCheckinItemActivity::class.java)
                    val itemId = scannedBarcode.contents
                    intent.putExtra("barcode_id", itemId)
                    getInventoryApiInstance().getItemsInContainer(itemId).enqueue(object : Callback<List<Item>> {
                        override fun onResponse(call: Call<List<Item>>, response: Response<List<Item>>) {
                            if (response.isSuccessful && response.body() != null) {
                                val itemContents = response.body()!!
                                Log.d(TAG, itemContents.toString())
                                intent.putExtra("itemContents", itemContents.toTypedArray())
                                startActivity(intent)
                            }
                        }

                        override fun onFailure(call: Call<List<Item>>, t: Throwable) {
                            TODO("Not yet implemented")
                        }
                    })
                }
            }
        }
        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = { scannerLauncher.launch(ScanOptions()) }) {
                    Text("Scan Item")
                }
            }
        }
    }
}

class ToolshedCheckinItemActivity : ComponentActivity() {
    private val scannedMap = mutableStateMapOf<String, Boolean>()

    override fun onSaveInstanceState(outState: Bundle) {
        val savableScannedMap = HashMap(scannedMap.toImmutableMap())
        outState.run {
            putSerializable("scannedMap", savableScannedMap)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "Starting activity")
        if (savedInstanceState != null) {
            with(savedInstanceState) {
                val savableScannedMap = getSerializable("scannedMap") as HashMap<String, Boolean>?
                if (savableScannedMap != null) scannedMap.putAll(savableScannedMap)
            }
        }
        val barcodeId = intent.getStringExtra("barcode_id") ?: return
        val inventoryApi = getInventoryApiInstance()
        val itemContents: Array<Item> = intent.getSerializableExtra("itemContents") as Array<Item>
        val call = inventoryApi.getItemsInContainer(barcodeId)
        /*
        call.enqueue(object : Callback<List<Item>> {
            override fun onResponse(call: Call<List<Item>>, response: Response<List<Item>>) {
                if (response.isSuccessful && response.body() != null) {
                    itemContents = response.body()!!
                    Log.d(TAG, itemContents.toString())
                }
            }

            override fun onFailure(call: Call<List<Item>>, t: Throwable) {
                TODO("Not yet implemented")
            }
        })
        if (!call.isExecuted) return
        Log.d(TAG, itemContents.toString())
         */

        if (itemContents.isEmpty()) {
            Log.d(TAG, "Checkin single item")
            setContent {
                ToolshedAddSingleItemCheckinUI(barcodeId, this)
            }
        } else {
            Log.d(TAG, "Checkin multiple items")

            for (item in itemContents) {
                if (!scannedMap.containsKey(item.barcode_id)) {
                    scannedMap[item.barcode_id] = false
                }
            }
            val scannerLauncher = registerForActivityResult(ScanContract()) { scannedBarcode: ScanIntentResult ->
                run {
                    Log.d(TAG, scannedMap.toString())
                    if (scannedMap.containsKey(scannedBarcode.contents)) {
                        scannedMap[scannedBarcode.contents] = true
                    } else {
                        Toast.makeText(this@ToolshedCheckinItemActivity, "Item should not be in container!!", Toast.LENGTH_LONG).show()
                    }
                }
            }
            setContent {
                ToolshedAddMultipleItemsCheckinUI(
                    barcodeId,
                    itemContents.asList(),
                    scannedMap,
                    scannerLauncher,
                    this
                )
            }
        }
    }
}

fun finishCheckinToToolshed(itemId: String, componentActivity: ComponentActivity) {
    getInventoryApiInstance().checkinToToolshed(ToolshedCheckin(itemId, "", ""))
        .enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                componentActivity.finish()
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                TODO("Not yet implemented")
            }
        })
}

@Composable
fun ToolshedAddSingleItemCheckinUI(itemId: String, componentActivity: ComponentActivity, modifier: Modifier = Modifier
    .fillMaxSize()
    .wrapContentSize(Alignment.Center)) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(getItemPictureUrl(itemId))
                .crossfade(true)
                .build(),
            contentDescription = itemId
        )
        Button(onClick = { finishCheckinToToolshed(itemId, componentActivity) }) {
            Text("Confirm Check-In")
        }
    }
}

@Composable
fun ToolshedAddMultipleItemsCheckinUI(
    containerId: String, childItems: List<Item>, scannedMap: SnapshotStateMap<String, Boolean>, scannerLauncher: ActivityResultLauncher<ScanOptions>, componentActivity: ComponentActivity, modifier: Modifier = Modifier
        .fillMaxSize()
        .wrapContentSize(Alignment.Center)) {
    Column (verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.verticalScroll(
        rememberScrollState()
    )) {
        for (item in childItems) {
            Row {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(getItemPictureUrl(item.barcode_id))
                        .crossfade(true)
                        .build(),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(0.25f)
                )
                Text(item.name)
                if (scannedMap[item.barcode_id]!!) {
                    Log.d(TAG, "Making checkmark visible.")
                    Text("✓")
                } else {
                    Log.d(TAG, "Making checkmark invisible.")
                }
            }
        }
        Button(onClick = { scannerLauncher.launch(ScanOptions()) }, enabled = !scannedMap.values.reduce { left, right -> left && right }) {
            Text("Scan item in container")
        }
        Button(onClick = { finishCheckinToToolshed(containerId, componentActivity) }, enabled = scannedMap.values.reduce { left, right -> left && right }) {
            Text("Confirm Check-In")
        }
    }
}