package camp.letsbuild.inventoryscanner

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import camp.letsbuild.inventoryscanner.ui.theme.InventoryScannerTheme
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.await
import retrofit2.awaitResponse

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inventoryApi = getInventoryApiInstance("http://10.23.1.200:5000")
        val barcodeScannerLauncher: ActivityResultLauncher<ScanOptions> = this.registerForActivityResult(ScanContract()) {
                scannedBarcode: ScanIntentResult ->
            run {
                if (scannedBarcode.contents == null) {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
                } else {
                    val itemCall = inventoryApi.getItem(scannedBarcode.contents)
                    itemCall.enqueue(object : Callback<Item> {
                        override fun onFailure(call: Call<Item>, t: Throwable) {
                            Log.i(TAG, t.message, t)
                            Toast.makeText(this@MainActivity, t.message, Toast.LENGTH_LONG).show();
                        }
                        override fun onResponse(call: Call<Item>, response: Response<Item>) {
                            val result = response.body()?.picture_path ?: response.message()
                            Log.i(TAG, result)
                            Toast.makeText(this@MainActivity, result, Toast.LENGTH_LONG).show();
                        }
                    })
                }
            }
        }
        setContent {
            InventoryScannerTheme {
                // A surface container using the 'background' color from the theme
                ScannerApp(barcodeScannerLauncher)
            }
        }
    }
}

@Composable
fun ScanUi(barcodeScannerLauncher: ActivityResultLauncher<ScanOptions>,
           modifier: Modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Button(onClick = {
            barcodeScannerLauncher.launch(ScanOptions())
        }) {
            Text("Scan Barcode")
        }
    }
}

//@Preview(showBackground = true)
@Composable
fun ScannerApp(barcodeScannerLauncher: ActivityResultLauncher<ScanOptions>) {
    InventoryScannerTheme {
        ScanUi(barcodeScannerLauncher)
    }
}