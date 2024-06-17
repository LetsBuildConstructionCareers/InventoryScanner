package camp.letsbuild.inventoryscanner

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RemoveItemFromVehicleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val itemId = intent.getStringExtra("barcode_id") ?: return
        val containerId = intent.getStringExtra("containerId") ?: return
        setContent {
            RemoveItemFromContainerUI(itemId, containerId, this) { inventoryApi, containerId, itemId ->
                inventoryApi.removeItemFromVehicle(containerId, itemId)
            }
        }
    }
}