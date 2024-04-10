package camp.letsbuild.inventoryscanner

import android.content.Intent
import android.os.Bundle
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

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scannerForNewItemActivity = scannerForNewItemResultLauncher(this)
        val scannerForDisplayItemActivity = scannerForDisplayItemResultLauncher(this)
        setContent {
            InventoryScannerTheme {
                // A surface container using the 'background' color from the theme
                ScannerApp(scannerForNewItemActivity, scannerForDisplayItemActivity)
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

fun scannerForDisplayItemResultLauncher(componentActivity: ComponentActivity): ActivityResultLauncher<ScanOptions> {
    return componentActivity.registerForActivityResult(ScanContract()) { scannedBarcode: ScanIntentResult ->
        run {
            if (scannedBarcode.contents == null) {
                Toast.makeText(componentActivity, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                val intent = Intent(componentActivity, DisplayItemActivity::class.java)
                intent.putExtra("barcode_id", scannedBarcode.contents)
                componentActivity.startActivity(intent)
            }
        }
    }
}

@Composable
fun ScanUi(scannerForNewItem: ActivityResultLauncher<ScanOptions>,
           scannerForDisplayItem: ActivityResultLauncher<ScanOptions>,
           modifier: Modifier = Modifier
               .fillMaxSize()
               .wrapContentSize(Alignment.Center)) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Button(onClick = { scannerForDisplayItem.launch(ScanOptions()) }) {
            Text(text = "Scan Existing Item")
        }
        Button(onClick = {
            scannerForNewItem.launch(ScanOptions())
        }) {
            Text("Scan New Item")
        }
    }
}

//@Preview(showBackground = true)
@Composable
fun ScannerApp(scannerForNewItem: ActivityResultLauncher<ScanOptions>,
               scannerForDisplayItem: ActivityResultLauncher<ScanOptions>) {
    InventoryScannerTheme {
        ScanUi(scannerForNewItem, scannerForDisplayItem)
    }
}