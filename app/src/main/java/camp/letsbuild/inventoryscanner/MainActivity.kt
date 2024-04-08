package camp.letsbuild.inventoryscanner

import android.os.Bundle
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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val barcodeScannerLauncher: ActivityResultLauncher<ScanOptions> = this.registerForActivityResult(ScanContract()) {
                scannedBarcode: ScanIntentResult ->
            run {
                if (scannedBarcode.contents == null) {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, scannedBarcode.contents, Toast.LENGTH_LONG).show();
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