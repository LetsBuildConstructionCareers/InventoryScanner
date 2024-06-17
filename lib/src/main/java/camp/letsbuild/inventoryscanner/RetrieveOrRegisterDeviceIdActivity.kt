package camp.letsbuild.inventoryscanner

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.provider.Settings.Secure
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import okhttp3.ResponseBody
import org.apache.hc.core5.http.HttpStatus
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val TAG = "RetrieveOrRegisterDeviceIdActivity"

@SuppressLint("HardwareIds")
fun launchRetrieveOrRegisterDeviceIdActivity(componentActivity: ComponentActivity) {
    val androidId = Secure.getString(componentActivity.contentResolver, Secure.ANDROID_ID)
    getInventoryApiInstance(componentActivity).getRegisteredDeviceId(androidId).enqueue(object : Callback<String> {
        override fun onResponse(call: Call<String>, response: Response<String>) {
            if (response.code() == HttpStatus.SC_NOT_FOUND) {
                getInventoryApiInstance(componentActivity).uploadDeviceId(androidId).enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        val intent = Intent(componentActivity, RetrieveOrRegisterDeviceIdActivity::class.java)
                        intent.putExtra("androidId", androidId)
                        componentActivity.startActivity(intent)
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        TODO("Not yet implemented")
                    }
                })
            } else if (response.isSuccessful && response.body() != null) {
                val sharedPreferences = componentActivity.getSharedPreferences("AdminPreferences", MODE_PRIVATE)
                sharedPreferences.edit().putString("device_id", response.body()).apply()
                Log.d(TAG, "Able to retrieve device registration from server")
                Toast.makeText(componentActivity, "Retrieved Device Registration from Server", Toast.LENGTH_LONG).show()
            }
        }

        override fun onFailure(call: Call<String>, t: Throwable) {
            TODO("Not yet implemented")
        }
    })
}
class RetrieveOrRegisterDeviceIdActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val androidId = intent.getStringExtra("androidId") ?: return
        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Hardware ID: $androidId")
                Text("Please RegisterDevice from the AdminApp on another Device.")
                Button(onClick = { getInventoryApiInstance(this@RetrieveOrRegisterDeviceIdActivity)
                    .getRegisteredDeviceId(androidId).enqueue(object : Callback<String> {
                        override fun onResponse(call: Call<String>, response: Response<String>) {
                            if (response.isSuccessful && response.body() != null) {
                                val sharedPreferences = getSharedPreferences("AdminPreferences", MODE_PRIVATE)
                                sharedPreferences.edit().putString("device_id", response.body()).apply()
                                finish()
                            }
                        }

                        override fun onFailure(call: Call<String>, t: Throwable) {
                            TODO("Not yet implemented")
                        }
                    }) }) {
                    Text("Click when Registration is Complete")
                }
            }
        }
    }
}
