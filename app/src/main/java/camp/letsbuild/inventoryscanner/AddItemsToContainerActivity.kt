package camp.letsbuild.inventoryscanner

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.journeyapps.barcodescanner.ScanOptions
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val TAG = "AddItemsToContainerActivity"

class AddItemsToContainerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val containerId = intent.getStringExtra("barcode_id") ?: return
        val intent = Intent(this, AddSingleItemToContainerActivity::class.java)
        intent.putExtra("containerId", containerId)
        val scannerForAddSingleItem = scannerForNewActivity(this, intent, "itemId")
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
        Text("Container: " + containerShortId)
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(getItemPictureUrl(containerId))
                .crossfade(true)
                .build(),
            contentDescription = containerShortId
        )
        Button(onClick = { scannerForAddSingleItem.launch(ScanOptions())}) {
            Text("Scan Item into Container")
        }
        Button(onClick = { componentActivity.finish() }) {
            Text("Finish Adding Items")
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
            AddSingleItemToContainerUI(itemId, containerId, this)
        }
    }
}

@Composable
fun AddSingleItemToContainerUI(itemId: String, containerId: String,
                               componentActivity: ComponentActivity,
                               modifier: Modifier = Modifier
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
            contentDescription = "Picture of Item"
        )
        Button(onClick = {
            val inventoryApi = getInventoryApiInstance()
            inventoryApi.addItemsToContainer(containerId, listOf(itemId)).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
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