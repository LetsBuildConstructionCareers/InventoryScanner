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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import camp.letsbuild.inventoryscanner.ui.theme.InventoryScannerTheme
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inventoryApi = getInventoryApiInstance()
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
        val scannerForNewItemActivity = scannerForNewItemResultLauncher(this)
        setContent {
            InventoryScannerTheme {
                // A surface container using the 'background' color from the theme
                ScannerApp(scannerForNewItemActivity)
            }
        }
    }
}

fun scannerForNewItemResultLauncher(componentActivity: ComponentActivity): ActivityResultLauncher<ScanOptions> {
    return componentActivity.registerForActivityResult(ScanContract()) { scannedBarcode: ScanIntentResult ->
        run {
            if (scannedBarcode.contents == null) {
                Toast.makeText(componentActivity, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                val intent = Intent(componentActivity, NewItemActivity::class.java)
                intent.putExtra("barcode_id", scannedBarcode.contents)
                componentActivity.startActivity(intent)
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