package camp.letsbuild.inventoryscanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

private const val TAG = "InitialBadgeCheckInActivity"

class InitialBadgeCheckInActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val barcodeId = intent.getStringExtra("barcode_id") ?: return

        var userInstructions = ""
        getInventoryApiInstance().getUser(barcodeId).enqueue(object : Callback<User> {
            override fun onResponse(call: Call<User>, response: Response<User>) {
                Log.d(TAG, response.toString())
                Log.d(TAG, response.body().toString())
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()
                    @Suppress("SENSELESS_COMPARISON")
                    if (user != null && (user.picture_path == null || user.picture_path.isBlank())) {
                        userInstructions = user.description
                    } else {
                        Toast.makeText(this@InitialBadgeCheckInActivity, "User has already checked-in.", Toast.LENGTH_LONG).show()
                        this@InitialBadgeCheckInActivity.finish()
                    }
                } else {
                    Toast.makeText(this@InitialBadgeCheckInActivity, "User is not pre-registered.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<User>, t: Throwable) {
                TODO("Not yet implemented")
            }
        })

        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imagePath = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            imagePath)
        val uri = FileProvider.getUriForFile(this, applicationContext.packageName + ".fileprovider", file)
        val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            run {
                if (success && barcodeId?.isNotBlank() == true) {
                } else {
                    if (!success) {
                        Log.e(TAG, "Picture taking failed.")
                    } else if (barcodeId?.isNotBlank() == true) {
                        Log.e(TAG, "barcodeId store failed.")
                    } else {
                        Log.e(TAG, "Unknown error.")
                    }
                }
            }
        }
        cameraLauncher.launch(uri)
        setContent { InitialBadgeCheckInUI(this, file, userInstructions) }
    }
}

@Composable
fun InitialBadgeCheckInUI(componentActivity: ComponentActivity,
                          imageFile: File,
                          userInstructions: String,
              modifier: Modifier = Modifier
                  .fillMaxSize()
                  .wrapContentSize(Alignment.Center)) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(userInstructions)
        Button(onClick = {
            val inventoryApi = getInventoryApiInstance()
            val userId = componentActivity.intent.getStringExtra("barcode_id") ?: return@Button
            uploadUserPictureToInventory(inventoryApi, userId, imageFile).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    Log.i(TAG, response.message())
                    Toast.makeText(componentActivity, response.message(), Toast.LENGTH_LONG).show();
                    componentActivity.finish()
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.i(TAG, t.message, t)
                    Toast.makeText(componentActivity, t.message, Toast.LENGTH_LONG).show();
                }
            })
        }) {
            Text("Confirm Check-In")
        }
    }
}