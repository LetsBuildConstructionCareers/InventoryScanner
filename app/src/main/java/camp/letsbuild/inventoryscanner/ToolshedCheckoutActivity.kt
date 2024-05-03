package camp.letsbuild.inventoryscanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.journeyapps.barcodescanner.ScanOptions
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ToolshedCheckoutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scannerForBadge = scannerForNewActivity(this, ToolshedCheckoutItemForUserActivity::class.java)
        setContent {
            Button(onClick = { scannerForBadge.launch(ScanOptions())}) {
                Text("Scan Badge")
            }
        }
    }
}

class ToolshedCheckoutItemForUserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = intent.getStringExtra("barcode_id")
        val intent = Intent(this, ToolshedCheckoutItemFinalizeActivity::class.java)
        intent.putExtra("userId", userId)
        val scannerForToolshedCheckoutFinalize = scannerForNewActivity(this, intent, "itemId")
        if (userId.isNullOrBlank()) {
            return
        }
        setContent {
            Column {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(getUserPictureUrl(userId))
                        .crossfade(true)
                        .build(),
                    contentDescription = ""
                )
                Button(onClick = { scannerForToolshedCheckoutFinalize.launch(ScanOptions()) }) {
                    Text("Scan Item")
                }
                Button(onClick = { this@ToolshedCheckoutItemForUserActivity.finish() }) {
                    Text("Finish Checkout")
                }
            }
        }
    }
}

class ToolshedCheckoutItemFinalizeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = intent.getStringExtra("userId")
        val itemId = intent.getStringExtra("itemId")
        if (userId.isNullOrBlank() || itemId.isNullOrBlank()) {
            return
        }
        val inventoryApi = getInventoryApiInstance()
        setContent {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(getItemPictureUrl(itemId))
                    .crossfade(true)
                    .build(),
                contentDescription = ""
            )
            Button(onClick = { inventoryApi.checkoutFromToolshed(ToolshedCheckout(itemId, userId)).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    this@ToolshedCheckoutItemFinalizeActivity.finish()
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    TODO("Not yet implemented")
                }
            }) }) {
                Text("Checkout Item")
            }
        }
    }
}