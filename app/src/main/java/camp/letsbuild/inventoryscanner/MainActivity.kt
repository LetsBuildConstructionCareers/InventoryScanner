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
        val scannerForAddItemsToContainerActivity = scannerForNewActivity(this, AddItemsToContainerActivity::class.java)
        val scannerForInitialBadgeCheckInActivity = scannerForNewActivity(this, InitialBadgeCheckInActivity::class.java)
        setContent {
            InventoryScannerTheme {
                // A surface container using the 'background' color from the theme
                ScannerApp(this, scannerForNewItemActivity, scannerForDisplayItemActivity, scannerForAddItemsToContainerActivity, scannerForInitialBadgeCheckInActivity)
            }
        }
    }
}

fun scannerForNewItemResultLauncher(componentActivity: ComponentActivity): ActivityResultLauncher<ScanOptions> {
    return scannerForNewActivity(componentActivity, NewItemActivity::class.java)
}

fun scannerForDisplayItemResultLauncher(componentActivity: ComponentActivity): ActivityResultLauncher<ScanOptions> {
    return scannerForNewActivity(componentActivity, DisplayItemActivity::class.java)
}

fun scannerForNewActivity(componentActivity: ComponentActivity, intent: Intent, barcodeId: String): ActivityResultLauncher<ScanOptions> {
    return componentActivity.registerForActivityResult(ScanContract()) { scannedBarcode: ScanIntentResult ->
        run {
            if (scannedBarcode.contents == null) {
                Toast.makeText(componentActivity, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                intent.putExtra(barcodeId, scannedBarcode.contents)
                componentActivity.startActivity(intent)
            }
        }
    }
}

fun <T : ComponentActivity> scannerForNewActivity(componentActivity: ComponentActivity, activityClass: Class<T>): ActivityResultLauncher<ScanOptions> {
    return scannerForNewActivity(componentActivity, Intent(componentActivity, activityClass), "barcode_id")
}

@Composable
fun ScanUi(componentActivity: ComponentActivity,
           scannerForNewItem: ActivityResultLauncher<ScanOptions>,
           scannerForDisplayItem: ActivityResultLauncher<ScanOptions>,
           scannerForAddItemsToContainerActivity: ActivityResultLauncher<ScanOptions>,
           scannerForInitialBadgeCheckInActivity: ActivityResultLauncher<ScanOptions>,
           modifier: Modifier = Modifier
               .fillMaxSize()
               .wrapContentSize(Alignment.Center)) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            scannerForNewItem.launch(ScanOptions())
        }) {
            Text("Scan New Item")
        }
        Button(onClick = {scannerForAddItemsToContainerActivity.launch(ScanOptions())}) {
            Text("Add Item to Container")
        }
        Button(onClick = { /*TODO*/ }) {
            Text("Remove Item from Container")
        }
        Button(onClick = { /*TODO*/ }) {
            Text("Add Item or Container to Vehicle")
        }
        Button(onClick = {
            val intent = Intent(componentActivity, ToolshedCheckoutActivity::class.java)
            componentActivity.startActivity(intent)
        }) {
            Text("Tool Shed - Check-Out")
        }
        Button(onClick = { /*TODO*/ }) {
            Text("Tool Shed - Check-In")
        }
        Button(onClick = { scannerForDisplayItem.launch(ScanOptions()) }) {
            Text(text = "Scan Existing Item")
        }
        Button(onClick = { scannerForInitialBadgeCheckInActivity.launch(ScanOptions()) }) {
            Text("Initial Badge Check-In")
        }
        Button(onClick = { /*TODO*/ }) {
            Text("Badge Check-In")
        }
        Button(onClick = { /*TODO*/ }) {
            Text("Badge Check-Out")
        }
    }
}

//@Preview(showBackground = true)
@Composable
fun ScannerApp(componentActivity: ComponentActivity,
               scannerForNewItem: ActivityResultLauncher<ScanOptions>,
               scannerForDisplayItem: ActivityResultLauncher<ScanOptions>,
               scannerForAddItemsToContainerActivity: ActivityResultLauncher<ScanOptions>,
               scannerForInitialBadgeCheckInActivity: ActivityResultLauncher<ScanOptions>) {
    InventoryScannerTheme {
        ScanUi(componentActivity,
            scannerForNewItem,
            scannerForDisplayItem,
            scannerForAddItemsToContainerActivity,
            scannerForInitialBadgeCheckInActivity)
    }
}