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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
        val scannerForViewItemsInContainerActivity = scannerForNewActivity(this, ViewItemsInContainerActivity::class.java)
        val scannerForRemoveItemFromContainerActivity = scannerForNewActivity(this, RemoveItemFromContainerActivity::class.java)
        val scannerForViewItemsInVehicleActivity = scannerForNewActivity(this, ViewItemsInVehicleActivity::class.java)
        val scannerForRemoveItemFromVehicleActivity = scannerForNewActivity(this, RemoveItemFromVehicleActivity::class.java)
        val scannerForCheckinUserActivity = scannerForNewActivity(this, CheckinUserActivity::class.java)
        val scannerForCheckoutUserActivity = scannerForNewActivity(this, CheckoutUserActivity::class.java)
        val scannerForCreateUserWithoutPictureActivity = scannerForNewActivity(this, CreateUserWithoutPictureActivity::class.java)
        setContent {
            InventoryScannerTheme {
                // A surface container using the 'background' color from the theme
                ScanUi(this,
                    scannerForNewItemActivity,
                    scannerForDisplayItemActivity,
                    scannerForViewItemsInContainerActivity,
                    scannerForRemoveItemFromContainerActivity,
                    scannerForViewItemsInVehicleActivity,
                    scannerForRemoveItemFromVehicleActivity,
                    scannerForCheckinUserActivity,
                    scannerForCheckoutUserActivity,
                    scannerForCreateUserWithoutPictureActivity)
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
           scannerForViewItemsInContainerActivity: ActivityResultLauncher<ScanOptions>,
           scannerForRemoveItemFromContainerActivity: ActivityResultLauncher<ScanOptions>,
           scannerForViewItemsInVehicleActivity: ActivityResultLauncher<ScanOptions>,
           scannerForRemoveItemFromVehicleActivity: ActivityResultLauncher<ScanOptions>,
           scannerForCheckinUserActivity: ActivityResultLauncher<ScanOptions>,
           scannerForCheckoutUserActivity: ActivityResultLauncher<ScanOptions>,
           scannerForCreateUserWithoutPictureActivity: ActivityResultLauncher<ScanOptions>,
           modifier: Modifier = Modifier
               .fillMaxSize()
               .wrapContentSize(Alignment.Center)) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            scannerForNewItem.launch(ScanOptions())
        }) {
            Text("Scan New Item")
        }
        Button(onClick = {
            val intent = Intent(componentActivity, AddItemsToContainerLandingActivity::class.java)
            componentActivity.startActivity(intent)
        }) {
            Text("Add Item to Container")
        }
        Button(onClick = { scannerForRemoveItemFromContainerActivity.launch(ScanOptions()) }) {
            Text("Remove Item from Container")
        }
        Button(onClick = { scannerForViewItemsInContainerActivity.launch(ScanOptions()) }) {
            Text("View Items in Container")
        }
        Button(onClick = {
            val intent = Intent(componentActivity, AddItemsToVehicleLandingActivity::class.java)
            componentActivity.startActivity(intent)
        }) {
            Text("Add Item or Container to Vehicle")
        }
        Button(onClick = { scannerForRemoveItemFromVehicleActivity.launch(ScanOptions()) }) {
            Text("Remove Item or Container from Vehicle")
        }
        Button(onClick = { scannerForViewItemsInVehicleActivity.launch(ScanOptions()) }) {
            Text("View Items in Vehicle")
        }
        Button(onClick = { /*TODO*/ }) {
            Text("Add Item or Container to Location")
        }
        Button(onClick = { /*TODO*/ }) {
            Text("Remove Item or Container from Location")
        }
        Button(onClick = {
            val intent = Intent(componentActivity, ToolshedCheckoutActivity::class.java)
            componentActivity.startActivity(intent)
        }) {
            Text("Tool Shed - Check-Out")
        }
        Button(onClick = {
            val intent = Intent(componentActivity, ToolshedCheckinActivity::class.java)
            componentActivity.startActivity(intent)
        }) {
            Text("Tool Shed - Check-In")
        }
        Button(onClick = { launchDisplayUsersWithOutstandingToolshedCheckoutsActivity(componentActivity) }) {
            Text("Display Users with Outstanding Toolshed Checkouts")
        }
        Button(onClick = {
            val intent = Intent(componentActivity, DisplayItemsCheckedOutByUserActivity::class.java)
            componentActivity.startActivity(intent)
        }) {
            Text("Display Items Checked-Out by User")
        }
        Button(onClick = { scannerForDisplayItem.launch(ScanOptions()) }) {
            Text(text = "Scan Existing Item")
        }
        Button(onClick = {
            val intent = Intent(componentActivity, InitialBadgeCheckInLandingActivity::class.java)
            componentActivity.startActivity(intent)
        }) {
            Text("Initial Badge Check-In")
        }
        Button(onClick = { scannerForCheckinUserActivity.launch(ScanOptions()) }) {
            Text("Badge Check-In")
        }
        Button(onClick = { scannerForCheckoutUserActivity.launch(ScanOptions()) }) {
            Text("Badge Check-Out")
        }
        Button(onClick = { scannerForCreateUserWithoutPictureActivity.launch(ScanOptions()) }) {
            Text("Create User without Picture")
        }
    }
}