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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
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
                    val intent = Intent(this, ToolshedCheckinConfirmUserActivity::class.java)
                    val itemId = scannedBarcode.contents
                    val inventoryApi = getInventoryApiInstance(this@ToolshedCheckinActivity)
                    ChainedNetworkRequest.begin(inventoryApi.getItemsInContainer(itemId))
                        .then { itemContents ->
                            Log.d(TAG, "itemContents = $itemContents")
                            intent.putExtra("itemContents", itemContents.toTypedArray())
                            inventoryApi.getItem(itemId)
                        }.then { item ->
                            Log.d(TAG, "item = $item")
                            intent.putExtra("item", item)
                            inventoryApi.getLastOutstandingCheckout(itemId)
                        }.finally { toolshedCheckout: ToolshedCheckout ->
                            Log.d(TAG, "toolshedCheckout = $toolshedCheckout")
                            intent.putExtra("toolshedCheckout", toolshedCheckout)
                            startActivity(intent)
                        }.handleNetworkErrorsWith {_, t ->
                            Log.e(TAG, t.toString())
                            TODO("Not yet implemented")
                        }.handleServerErrorsWith {_, response ->
                            Log.e(TAG, response.toString())
                            TODO("Not yet implemented")
                        }.withNumberOfRetries(5)
                        .execute()
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

class ToolshedCheckinConfirmUserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val toolshedCheckout = intent.getSerializableExtra("toolshedCheckout") as ToolshedCheckout?
        val checkoutId = toolshedCheckout?.checkout_id ?: ""
        val userId = toolshedCheckout?.user_id
        val item = intent.getSerializableExtra("item") as Item
        val itemContents: Array<Item> = intent.getSerializableExtra("itemContents") as Array<Item>
        val checkinItemIntent = Intent(this, ToolshedCheckinItemActivity::class.java)
        checkinItemIntent.putExtra("item", item)
        checkinItemIntent.putExtra("itemContents", itemContents)
        checkinItemIntent.putExtra("toolshedCheckoutId", checkoutId)
        checkinItemIntent.putExtra("userId", userId)
        val overrideUserIntent = Intent(this, ToolshedCheckinOverrideUserActivity::class.java)
        overrideUserIntent.putExtra("item", item)
        overrideUserIntent.putExtra("itemContents", itemContents)
        overrideUserIntent.putExtra("toolshedCheckoutId", checkoutId)
        if (userId == null) {
            overrideUserIntent.putExtra("reasonOverrideIsRequired", "No toolshed checkout is present.")
            startActivity(overrideUserIntent)
        } else {
            setContent {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(getUserPictureUrl(userId))
                            .addHeader(AUTHORIZATION, getAuthorization(this@ToolshedCheckinConfirmUserActivity))
                            .crossfade(true)
                            .build(),
                        contentDescription = "",
                        loading = { CircularProgressIndicator() }
                    )
                    Button(onClick = {
                        startActivity(checkinItemIntent)
                        finish()
                    }) {
                        Text("User's Picture Matches")
                    }
                    Button(onClick = {
                        overrideUserIntent.putExtra("reasonOverrideIsRequired", "Checkin user does not match checkout user.")
                        startActivity(overrideUserIntent)
                        finish()
                    }) {
                        Text("User's Picture Does NOT Match")
                    }
                }
            }
        }
    }
}

class ToolshedCheckinOverrideUserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val item = intent.getSerializableExtra("item") as Item
        val itemContents: Array<Item> = intent.getSerializableExtra("itemContents") as Array<Item>
        val toolshedCheckoutId = intent.getStringExtra("toolshedCheckoutId")
        val reasonOverrideIsRequired = intent.getStringExtra("reasonOverrideIsRequired") ?: "Unknown error!!"
        val checkinItemIntent = Intent(this, ToolshedCheckinItemActivity::class.java)
        checkinItemIntent.putExtra("item", item)
        checkinItemIntent.putExtra("itemContents", itemContents)
        checkinItemIntent.putExtra("toolshedCheckoutId", toolshedCheckoutId)
        val scannerLauncher = registerForActivityResult(ScanContract()) { scannedBarcode: ScanIntentResult ->
            run {
                if (scannedBarcode.contents == null) {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
                } else {
                    checkinItemIntent.putExtra("userId", scannedBarcode.contents)
                    startActivity(checkinItemIntent)
                    finish()
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
                var overrideJustification by remember { mutableStateOf("") }
                Text(reasonOverrideIsRequired)
                TextField(value = overrideJustification, onValueChange = {overrideJustification = it}, label = { Text("Justification for Override") }, placeholder = { Text("Provide justification for why user did not checkout item.") })
                Button(onClick = {
                    checkinItemIntent.putExtra("overrideJustification", overrideJustification)
                    scannerLauncher.launch(ScanOptions())
                }) {
                    Text("Scan User Badge and Continue")
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
        val item = intent.getSerializableExtra("item") as Item
        val barcodeId = item.barcode_id
        val itemContents: Array<Item> = intent.getSerializableExtra("itemContents") as Array<Item>
        val toolshedCheckoutId = intent.getStringExtra("toolshedCheckoutId")
        val overrideJustification = intent.getStringExtra("overrideJustification")
        val userId = intent.getStringExtra("userId") ?: return

        if (itemContents.isEmpty()) {
            Log.d(TAG, "Checkin single item")
            setContent {
                ToolshedAddSingleItemCheckinUI(toolshedCheckoutId, barcodeId, userId, overrideJustification, this)
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
                    toolshedCheckoutId,
                    barcodeId,
                    itemContents.asList(),
                    userId,
                    overrideJustification,
                    scannedMap,
                    scannerLauncher,
                    this
                )
            }
        }
    }
}

fun finishCheckinToToolshed(checkoutId: String?, itemId: String, userId: String, overrideJustification: String?, componentActivity: ComponentActivity) {
    getInventoryApiInstance(componentActivity).checkinToToolshed(ToolshedCheckin(null, checkoutId, itemId, userId, System.currentTimeMillis() / 1000, overrideJustification, null))
        .enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                launchViewFullLocationOfItemActivity(itemId, componentActivity)
                componentActivity.finish()
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                TODO("Not yet implemented")
            }
        })
}

@Composable
fun ToolshedAddSingleItemCheckinUI(checkoutId: String?, itemId: String, userId: String, overrideJustification: String?, componentActivity: ComponentActivity, modifier: Modifier = Modifier
    .fillMaxSize()
    .wrapContentSize(Alignment.Center)) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(getItemPictureUrl(itemId))
                .addHeader(AUTHORIZATION, getAuthorization(componentActivity))
                .crossfade(true)
                .build(),
            contentDescription = itemId,
            loading = { CircularProgressIndicator() }
        )
        var waitingOnNetwork by remember { mutableStateOf(false) }
        if (waitingOnNetwork) {
            CircularProgressIndicator()
        } else {
            Button(onClick = {
                waitingOnNetwork = true
                finishCheckinToToolshed(checkoutId, itemId, userId, overrideJustification, componentActivity)
            }) {
                Text("Confirm Check-In")
            }
        }
    }
}

@Composable
fun ToolshedAddMultipleItemsCheckinUI(
    checkoutId: String?, containerId: String, childItems: List<Item>, userId: String, overrideJustification: String?, scannedMap: SnapshotStateMap<String, Boolean>, scannerLauncher: ActivityResultLauncher<ScanOptions>, componentActivity: ComponentActivity, modifier: Modifier = Modifier
        .fillMaxSize()
        .wrapContentSize(Alignment.Center)) {
    Column (verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.verticalScroll(
        rememberScrollState()
    )) {
        for (item in childItems) {
            Row {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(getItemPictureUrl(item.barcode_id))
                        .addHeader(AUTHORIZATION, getAuthorization(componentActivity))
                        .crossfade(true)
                        .build(),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(0.25f),
                    loading = { CircularProgressIndicator() }
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
        var waitingOnNetwork by remember { mutableStateOf(false) }
        if (waitingOnNetwork) {
            CircularProgressIndicator()
        } else {
            Button(onClick = {
                waitingOnNetwork = true
                finishCheckinToToolshed(checkoutId, containerId, userId, overrideJustification, componentActivity)
            }, enabled = scannedMap.values.reduce { left, right -> left && right }) {
                Text("Confirm Check-In")
            }
        }
    }
}