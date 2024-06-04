package camp.letsbuild.inventoryscanner

import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions

fun <T : ComponentActivity> scannerForNewActivity(componentActivity: ComponentActivity, activityClass: Class<T>): ActivityResultLauncher<ScanOptions> {
    return scannerForNewActivity(componentActivity, Intent(componentActivity, activityClass), "barcode_id")
}

fun scannerForNewActivity(componentActivity: ComponentActivity, intent: Intent, barcodeId: String): ActivityResultLauncher<ScanOptions> {
    return componentActivity.registerForActivityResult(ScanContract()) { scannedBarcode: ScanIntentResult ->
        run {
            if (scannedBarcode.contents == null) {
                Toast.makeText(componentActivity, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                intent.putExtra(barcodeId, scannedBarcode.contents)
                componentActivity.startActivity(intent)
            }
        }
    }
}