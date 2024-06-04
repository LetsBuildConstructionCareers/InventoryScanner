package camp.letsbuild.inventoryscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ViewItemsInLocationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val containerId = intent.getStringExtra("barcode_id") ?: return
        getInventoryApiInstance(this).getItemsInLocation(containerId).enqueue(object : Callback<List<Item>> {
            override fun onResponse(call: Call<List<Item>>, response: Response<List<Item>>) {
                val itemList = response.body()
                if (response.isSuccessful && itemList != null) {
                    setContent { DisplayItemsUI(itemList, this@ViewItemsInLocationActivity) }
                }
            }

            override fun onFailure(call: Call<List<Item>>, t: Throwable) {
                TODO("Not yet implemented")
            }
        })
    }
}