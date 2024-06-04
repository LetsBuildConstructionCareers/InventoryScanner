package camp.letsbuild.inventoryscanner

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RemoveItemFromLocationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val itemId = intent.getStringExtra("barcode_id") ?: return
        val inventoryApi = getInventoryApiInstance(this)
        var containerId = ""
        inventoryApi.getParentOfItem(itemId).enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful && response.body() != null) {
                    containerId = response.body()!!
                    if (containerId.isBlank()) {
                        Toast.makeText(this@RemoveItemFromLocationActivity, "Item is not in a container!!", Toast.LENGTH_LONG).show()
                        this@RemoveItemFromLocationActivity.finish()
                    }
                } else {
                    this@RemoveItemFromLocationActivity.finish()
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                TODO("Not yet implemented")
            }
        })
        setContent {
            RemoveItemFromContainerUI(itemId, containerId, this) { inventoryApi, containerId, itemId ->
                inventoryApi.removeItemFromLocation(containerId, itemId)
            }
        }
    }
}