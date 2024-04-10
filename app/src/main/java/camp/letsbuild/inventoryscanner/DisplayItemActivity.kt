package camp.letsbuild.inventoryscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import camp.letsbuild.inventoryscanner.ui.theme.InventoryScannerTheme
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.journeyapps.barcodescanner.ScanOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DisplayItemActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val barcodeId = intent.getStringExtra("barcode_id")
        var itemName: String = ""
        val inventoryApi = getInventoryApiInstance()
        if (barcodeId != null) {
            val itemCall = inventoryApi.getItem(barcodeId)
            itemCall.enqueue(object : Callback<Item> {
                override fun onResponse(call: Call<Item>, response: Response<Item>) {
                    itemName = response.body()?.name ?: ""
                }

                override fun onFailure(call: Call<Item>, t: Throwable) {
                    TODO("Not yet implemented")
                }
            })
        } else {
            return
        }
        setContent {
            InventoryScannerTheme {
                // A surface container using the 'background' color from the theme
                DisplayItemUI(barcodeId, itemName)
            }
        }
    }
}

@Composable
fun DisplayItemUI(barcodeId: String, itemName: String, modifier: Modifier = Modifier
    .fillMaxSize()
    .wrapContentSize(Alignment.Center)) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(getItemPictureUrl(barcodeId))
                .crossfade(true)
                .build(),
            contentDescription = itemName
        )
    }
}