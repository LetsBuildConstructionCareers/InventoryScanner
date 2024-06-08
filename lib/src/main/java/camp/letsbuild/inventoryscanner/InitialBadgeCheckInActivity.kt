package camp.letsbuild.inventoryscanner

import android.content.Intent
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

private const val TAG = "InitialBadgeCheckInActivity"

class InitialBadgeCheckInLandingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scannerLauncher = registerForActivityResult(ScanContract()) { scannedBarcode: ScanIntentResult ->
            run {
                if (scannedBarcode.contents == null) {
                    Toast.makeText(this@InitialBadgeCheckInLandingActivity, "Cancelled", Toast.LENGTH_LONG).show();
                } else {
                    val intent = Intent(this@InitialBadgeCheckInLandingActivity, InitialBadgeCheckInTakePictureActivity::class.java)
                    val userId = scannedBarcode.contents
                    intent.putExtra("barcode_id", userId)
                    getInventoryApiInstance(this).getUser(userId).enqueue(object : Callback<User> {
                        override fun onResponse(call: Call<User>, response: Response<User>) {
                            Log.d(TAG, response.toString())
                            Log.d(TAG, response.body().toString())
                            if (response.isSuccessful && response.body() != null) {
                                val user = response.body()
                                @Suppress("SENSELESS_COMPARISON")
                                if (user != null && (user.picture_path == null || user.picture_path.isBlank())) {
                                    intent.putExtra("user", user)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(this@InitialBadgeCheckInLandingActivity, "User has already checked-in.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(this@InitialBadgeCheckInLandingActivity, "User is not pre-registered.", Toast.LENGTH_LONG).show()
                            }
                        }

                        override fun onFailure(call: Call<User>, t: Throwable) {
                            TODO("Not yet implemented")
                        }
                    })
                }
            }
        }
        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = { scannerLauncher.launch(ScanOptions()) }) {
                    Text("Scan User")
                }
            }
        }
    }
}

class InitialBadgeCheckInTakePictureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = intent.getSerializableExtra("user") as User? ?: return
        val barcodeId = user.barcode_id
        val intent = Intent(this, InitialBadgeCheckInActivity::class.java)
        intent.putExtra("user", user)
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imagePath = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            imagePath)
        val uri = FileProvider.getUriForFile(this, applicationContext.packageName + ".fileprovider", file)
        val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            run {
                if (success) {
                    Log.d(TAG, "updating pictureTaken")
                    intent.putExtra("picture_file", file)
                    startActivity(intent)
                    finish()
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
        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = { cameraLauncher.launch(uri) }) {
                    Text("Take User Picture")
                }
            }
        }
    }
}

class InitialBadgeCheckInActivity : ComponentActivity() {
    private var pictureTaken = false

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run { putBoolean("pictureTaken", pictureTaken) }
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            with(savedInstanceState) { pictureTaken = getBoolean("pictureTaken") }
        }
        val user = intent.getSerializableExtra("user") as User? ?: return
        val file = intent.getSerializableExtra("picture_file") as File? ?: return

        setContent { InitialBadgeCheckInUI(this, file, user) }
    }
}

@Composable
fun InitialBadgeCheckInUI(componentActivity: ComponentActivity,
                          imageFile: File,
                          user: User,
              modifier: Modifier = Modifier
                  .fillMaxSize()
                  .wrapContentSize(Alignment.Center)) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(user.initial_checkin_info)
        Text(user.description)
        var waitingOnNetwork by remember { mutableStateOf(false) }
        if (waitingOnNetwork) {
            CircularProgressIndicator()
        } else {
            Button(onClick = {
                waitingOnNetwork = true
                val inventoryApi = getInventoryApiInstance(componentActivity)
                val userId = user.barcode_id
                val checkinCall = inventoryApi.checkinUser(userId)
                uploadUserPictureToInventory(inventoryApi, userId, imageFile).enqueue(object :
                    Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        checkinCall.enqueue(object : Callback<ResponseBody> {
                            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                waitingOnNetwork = false
                                val message = getStatusText(response.code())
                                Log.i(TAG, message)
                                Toast.makeText(componentActivity, message, Toast.LENGTH_LONG).show();
                                componentActivity.finish()
                            }

                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                waitingOnNetwork = false
                                Log.i(TAG, t.message, t)
                                Toast.makeText(componentActivity, t.message, Toast.LENGTH_LONG).show();
                            }
                        })

                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        waitingOnNetwork = false
                        Log.i(TAG, t.message, t)
                        Toast.makeText(componentActivity, t.message, Toast.LENGTH_LONG).show();
                    }
                })
            }) {
                Text("Confirm Check-In")
            }
        }
    }
}