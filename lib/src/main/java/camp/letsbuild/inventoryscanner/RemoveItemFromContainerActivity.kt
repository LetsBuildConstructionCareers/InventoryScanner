package camp.letsbuild.inventoryscanner

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RemoveItemFromContainerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val itemId = intent.getStringExtra("barcode_id") ?: return
        val inventoryApi = getInventoryApiInstance(this)
        var containerId = ""
        inventoryApi.getParentOfItem(itemId).enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful && response.body() != null) {
                    containerId = response.body()!!
                    if (containerId.isBlank()) {
                        Toast.makeText(this@RemoveItemFromContainerActivity, "Item is not in a container!!", Toast.LENGTH_LONG).show()
                        this@RemoveItemFromContainerActivity.finish()
                    }
                } else {
                    this@RemoveItemFromContainerActivity.finish()
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                TODO("Not yet implemented")
            }
        })
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