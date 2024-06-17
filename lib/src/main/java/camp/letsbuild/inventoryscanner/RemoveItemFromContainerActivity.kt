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

private const val TAG = "RemoveItemFromContainer"

fun <T> scannerForRemoveItemFromX(componentActivity: ComponentActivity, activityClass: Class<*>, getParentOfItem: (inventoryApi: InventoryApi, itemId: String) -> Call<T>, transformResult: (result: T) -> String): ActivityResultLauncher<ScanOptions> {
    val intent = Intent(componentActivity, activityClass)
    return componentActivity.registerForActivityResult(ScanContract()) { scannedBarcode: ScanIntentResult ->
        run {
            if (scannedBarcode.contents == null) {
                Toast.makeText(componentActivity, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                val barcodeId = scannedBarcode.contents
                intent.putExtra("barcode_id", barcodeId)
                getParentOfItem(getInventoryApiInstance(componentActivity), barcodeId).enqueue(object : Callback<T> {
                    override fun onResponse(call: Call<T>, response: Response<T>) {
                        if (response.isSuccessful && response.body() != null) {
                            val containerId = transformResult(response.body()!!)
                            Log.d(TAG, "Found item in container: $containerId")
                            intent.putExtra("containerId", containerId)
                            componentActivity.startActivity(intent)
                        } else {
                            TODO("Not yet implemented")
                        }
                    }

                    override fun onFailure(call: Call<T>, t: Throwable) {
                        TODO("Not yet implemented")
                    }
                })
            }
        }
    }
}

class RemoveItemFromContainerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val itemId = intent.getStringExtra("barcode_id") ?: return
        val containerId = intent.getStringExtra("containerId") ?: return
        setContent {
            RemoveItemFromContainerUI(itemId, containerId, this) { inventoryApi, containerId, itemId ->
                inventoryApi.removeItemFromContainer(containerId, itemId)
            }
        }
    }
}

@Composable
fun RemoveItemFromContainerUI(itemId: String, containerId: String, componentActivity: ComponentActivity, removeItem: (inventoryApi: InventoryApi, containerId: String, itemId: String) -> Call<ResponseBody>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(getItemPictureUrl(itemId))
                .addHeader(AUTHORIZATION, getAuthorization(componentActivity))
                .crossfade(true)
                .build(),
            contentDescription = "",
            loading = { CircularProgressIndicator() }
        )
        var waitingOnNetwork by remember { mutableStateOf(false) }
        if (waitingOnNetwork) {
            CircularProgressIndicator()
        } else {
            Button(onClick = {
                waitingOnNetwork = true
                removeItem(getInventoryApiInstance(componentActivity), containerId, itemId).enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        componentActivity.finish()
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        TODO("Not yet implemented")
                    }
                })
            }) {
                Text("Confirm Remove Item")
            }
        }
    }
}