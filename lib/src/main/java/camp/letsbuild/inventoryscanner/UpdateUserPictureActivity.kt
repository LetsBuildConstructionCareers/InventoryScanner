package camp.letsbuild.inventoryscanner

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import coil.compose.SubcomposeAsyncImage
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

private const val TAG = "UpdateUserPictureActivity"

class UpdateUserPictureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scannerLauncher = registerForActivityResult(ScanContract()) { scannedBarcode: ScanIntentResult ->
            run {
                if (scannedBarcode.contents == null) {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
                } else {
                    getInventoryApiInstance(this).getUser(scannedBarcode.contents).enqueue(object : Callback<User> {
                        override fun onResponse(call: Call<User>, response: Response<User>) {
                            if (response.isSuccessful) {
                                val intent = Intent(this@UpdateUserPictureActivity, DisplayUsersCurrentPictureActivity::class.java)
                                intent.putExtra("user", response.body())
                                startActivity(intent)
                                finish()
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

class DisplayUsersCurrentPictureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = intent.getSerializableExtra("user") as User?
        val intent = Intent(this, ConfirmUsersNewPictureActivity::class.java)
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
                    } else if (user?.barcode_id?.isNotBlank() == true) {
                        Log.e(TAG, "barcodeId store failed.")
                    } else {
                        Log.e(TAG, "Unknown error.")
                    }
                }
            }
        }
        setContent {
            Column(modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (user != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(getUserPictureUrl(user.barcode_id))
                            .addHeader(AUTHORIZATION, getAuthorization(this@DisplayUsersCurrentPictureActivity))
                            .crossfade(true)
                            .build(),
                        contentDescription = user.name,
                        loading = { CircularProgressIndicator() },
                        error = { Text("Cannot display picture.") }
                    )
                    Text(user.name)
                    Button(onClick = { cameraLauncher.launch(uri) }) {
                        Text("Take User Picture")
                    }
                } else {
                    Text("User does not exist!!")
                }
            }
        }
    }
}

class ConfirmUsersNewPictureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val file = intent.getSerializableExtra("picture_file") as File? ?: return
        val user = intent.getSerializableExtra("user") as User? ?: return

        setContent {
            Column(modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var waitingOnNetwork by remember { mutableStateOf(false) }
                if (waitingOnNetwork) {
                    CircularProgressIndicator()
                } else {
                    Button(onClick = {
                        waitingOnNetwork = true
                        uploadUserPictureToInventory(getInventoryApiInstance(this@ConfirmUsersNewPictureActivity), user.barcode_id, file)
                            .enqueue(object : Callback<ResponseBody> {
                                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                    waitingOnNetwork = false
                                    val message = getStatusText(response.code())
                                    Log.i(TAG, message)
                                    Toast.makeText(this@ConfirmUsersNewPictureActivity, message, Toast.LENGTH_LONG).show();
                                    finish()
                            }

                                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                    waitingOnNetwork = false
                                    Log.i(TAG, t.message, t)
                                    Toast.makeText(this@ConfirmUsersNewPictureActivity, t.message, Toast.LENGTH_LONG).show();
                            }
                        })
                    }) {
                        Text("Confirm New Picture")
                    }
                }
            }
        }
    }
}