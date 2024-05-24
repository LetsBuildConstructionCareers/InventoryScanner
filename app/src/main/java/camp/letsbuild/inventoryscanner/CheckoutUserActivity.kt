package camp.letsbuild.inventoryscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
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

class CheckoutUserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = intent.getStringExtra("barcode_id") ?: return
        val inventoryApi = getInventoryApiInstance(this)
        setContent {
            Column(modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(getUserPictureUrl(userId))
                        .addHeader(AUTHORIZATION, getAuthorization(this@CheckoutUserActivity))
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
                        inventoryApi.checkoutUser(userId).enqueue(object : Callback<ResponseBody> {
                            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                this@CheckoutUserActivity.finish()
                            }

                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                TODO("Not yet implemented")
                            }
                        })
                    }) {
                        Text("Confirm Checkout")
                    }
                }
            }
        }
    }
}