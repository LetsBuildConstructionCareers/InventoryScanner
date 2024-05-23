package camp.letsbuild.inventoryscanner

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ToolshedCheckoutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scannerForBadge = registerForActivityResult(ScanContract()) { scannedBarcode: ScanIntentResult ->
            run {
                if (scannedBarcode.contents == null) {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
                } else {
                    val userId = scannedBarcode.contents
                    getInventoryApiInstance(this@ToolshedCheckoutActivity).getUser(userId).enqueue(object : Callback<User> {
                        override fun onResponse(call: Call<User>, response: Response<User>) {
                            if (response.isSuccessful && response.body() != null && !(response.body()!!.name.isNullOrBlank())) {
                                val intent = Intent(this@ToolshedCheckoutActivity, ToolshedCheckoutItemForUserActivity::class.java)
                                intent.putExtra("barcode_id", userId)
                                startActivity(intent)
                            } else {
                                Toast.makeText(this@ToolshedCheckoutActivity, "Could not find valid user!!", Toast.LENGTH_LONG).show()
                            }
                        }

                        override fun onFailure(call: Call<User>, t: Throwable) {
                            TODO("Not yet implemented")
                        }
                    })
                }
            }
        }//scannerForNewActivity(this, ToolshedCheckoutItemForUserActivity::class.java)
        setContent {
            Column(modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
                   horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = { scannerForBadge.launch(ScanOptions()) }) {
                    Text("Scan Badge")
                }
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
        val scannerForToolshedCheckoutFinalize = registerForActivityResult(ScanContract()) { scannedBarcode: ScanIntentResult ->
            run {
                if (scannedBarcode.contents == null) {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
                } else {
                    intent.putExtra("itemId", scannedBarcode.contents)
                    getInventoryApiInstance(this@ToolshedCheckoutItemForUserActivity).getLastOutstandingCheckout(scannedBarcode.contents).enqueue(object : Callback<ToolshedCheckout> {
                        override fun onResponse(call: Call<ToolshedCheckout>, response: Response<ToolshedCheckout>) {
                            if (response.isSuccessful) {
                                if (response.body() != null && response.body()!!.item_id.isNotBlank()) {
                                    Toast.makeText(this@ToolshedCheckoutItemForUserActivity, "Item has already been checked out!!", Toast.LENGTH_LONG).show()
                                } else {
                                    startActivity(intent)
                                }
                            }
                        }

                        override fun onFailure(call: Call<ToolshedCheckout>, t: Throwable) {
                            TODO("Not yet implemented")
                        }
                    })
                }
            }
        }//scannerForNewActivity(this, intent, "itemId")
        if (userId.isNullOrBlank()) {
            return
        }
        setContent {
            Column(modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
                   horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(getUserPictureUrl(userId))
                        .addHeader(AUTHORIZATION, getAuthorization(this@ToolshedCheckoutItemForUserActivity))
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
        val inventoryApi = getInventoryApiInstance(this)
        setContent {
            Column(modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(getItemPictureUrl(itemId))
                        .crossfade(true)
                        .build(),
                    contentDescription = ""
                )
                Button(onClick = {
                    inventoryApi.checkoutFromToolshed(ToolshedCheckout("", itemId, userId, System.currentTimeMillis() / 1000))
                        .enqueue(object : Callback<ResponseBody> {
                            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                this@ToolshedCheckoutItemFinalizeActivity.finish()
                            }

                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                TODO("Not yet implemented")
                            }
                        })
                }) {
                    Text("Checkout Item")
                }
            }
        }
    }
}