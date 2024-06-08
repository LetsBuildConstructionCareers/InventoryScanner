package camp.letsbuild.inventoryscanner

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val TAG = "AddItemsToContainerActivity"

class AddItemsToContainerLandingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scannerLauncher = scannerForNewActivity(this, AddItemsToContainerActivity::class.java)
        setContent {
            Column (modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = { scannerLauncher.launch(ScanOptions()) }) {
                    Text("Scan Container")
                }
            }
        }
    }
}

class AddItemsToContainerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val containerId = intent.getStringExtra("barcode_id") ?: return
        val intent = Intent(this, AddSingleItemToContainerActivity::class.java)
        intent.putExtra("containerId", containerId)
        val scannerForAddSingleItem = registerForActivityResult(ScanContract()) { scannedBarcode: ScanIntentResult ->
            run {
                if (scannedBarcode.contents == null) {
                    Toast.makeText(this@AddItemsToContainerActivity, "Cancelled", Toast.LENGTH_LONG).show()
                } else {
                    val itemId = scannedBarcode.contents
                    getInventoryApiInstance(this).getParentOfItem(itemId).enqueue(object : Callback<String> {
                        override fun onResponse(call: Call<String>, response: Response<String>) {
                            Log.d(TAG, "Getting parent of item")
                            if (response.isSuccessful && response.body() != null) {
                                val itemParent = response.body()!!
                                Log.d(TAG, itemParent)
                                if (itemParent.isNotBlank()) {
                                    Toast.makeText(this@AddItemsToContainerActivity, "Item is already in a container!!", Toast.LENGTH_LONG).show()
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

@Composable
fun AddItemsToContainerUI(containerId: String, containerShortId: String,
                          scannerForAddSingleItem: ActivityResultLauncher<ScanOptions>,
                          componentActivity: ComponentActivity,
                          modifier: Modifier = Modifier
                              .fillMaxSize()
                              .wrapContentSize(Alignment.Center)) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Container: $containerShortId")
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(getItemPictureUrl(containerId))
                .addHeader(AUTHORIZATION, getAuthorization(componentActivity))
                .crossfade(true)
                .build(),
            contentDescription = containerShortId,
            loading = { CircularProgressIndicator() }
        )
        Button(onClick = { scannerForAddSingleItem.launch(ScanOptions())}) {
            Text("Scan Item into Container")
        }
        Button(onClick = { componentActivity.finish() }) {
            Text("Finish (No More Items to Add)")
        }
    }
}

class AddSingleItemToContainerActivity : ComponentActivity() {
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
                {inventoryApi, containerId, itemIds -> inventoryApi.addItemsToContainer(containerId, itemIds)})
        }
    }
}

@Composable
fun AddSingleItemToContainerUI(itemId: String, containerId: String,
                               componentActivity: ComponentActivity,
                               addItems: (inventoryApi: InventoryApi, containerId: String, itemIds: List<String>) -> Call<ResponseBody>,
                               modifier: Modifier = Modifier
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
            contentDescription = "Picture of Item",
            loading = { CircularProgressIndicator() }
        )
        var waitingOnNetwork by remember { mutableStateOf(false) }
        if (waitingOnNetwork) {
            CircularProgressIndicator()
        } else {
            Button(onClick = {
                waitingOnNetwork = true
                addItems(getInventoryApiInstance(componentActivity), containerId, listOf(itemId)).enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        val message = getStatusText(response.code())
                        Toast.makeText(componentActivity, message, Toast.LENGTH_LONG).show()
                        componentActivity.finish()
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        TODO("Not yet implemented")
                    }
                })
            }) {
                Text("Add Item to Container")
            }
        }
    }
}