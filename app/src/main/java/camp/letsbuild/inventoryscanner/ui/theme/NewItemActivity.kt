package camp.letsbuild.inventoryscanner.ui.theme

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import camp.letsbuild.inventoryscanner.getInventoryApiInstance
import camp.letsbuild.inventoryscanner.uploadItemToInventory
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

private const val TAG = "NewItemActivity"

class NewItemActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val barcodeId = intent.getStringExtra("barcode_id")
        val inventoryApi = getInventoryApiInstance()
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imagePath = getExternalFilesDir(DIRECTORY_PICTURES) //File(filesDir, "images")
        val file = File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            imagePath)
        val uri = FileProvider.getUriForFile(this, applicationContext.packageName + ".fileprovider", file)
        val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) {success ->
            run {
                if (success && barcodeId?.isNotBlank() == true) {
                    uploadItemToInventory(inventoryApi, barcodeId, file).enqueue(object : Callback<ResponseBody> {
                        override fun onResponse(
                            call: Call<ResponseBody>,
                            response: Response<ResponseBody>
                        ) {
                            Log.i(TAG, response.message())
                            Toast.makeText(this@NewItemActivity, response.message(), Toast.LENGTH_LONG).show();
                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            Log.i(TAG, t.message, t)
                            Toast.makeText(this@NewItemActivity, t.message, Toast.LENGTH_LONG).show();
                        }

                    })
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
    }
}

fun postNewItem(barcodeId: String, uri: Uri) {}