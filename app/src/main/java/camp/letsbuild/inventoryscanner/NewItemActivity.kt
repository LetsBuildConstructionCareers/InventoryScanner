package camp.letsbuild.inventoryscanner

import android.content.Intent
import android.graphics.BitmapFactory
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import camp.letsbuild.inventoryscanner.ui.theme.InventoryScannerTheme
import com.journeyapps.barcodescanner.ScanOptions
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
        getInventoryApiInstance()
        Intent(MediaStore.ACTION_IMAGE_CAPTURE)
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
        setContent {
            InventoryScannerTheme {
                // A surface container using the 'background' color from the theme
                NewItemUI(this, file)
            }
        }
    }
}

@Composable
fun NewItemUI(componentActivity: ComponentActivity,
              imageFile: File,
              modifier: Modifier = Modifier
                  .fillMaxSize()
                  .wrapContentSize(Alignment.Center)) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val imageView = ImageView(componentActivity)
        imageView.setImageBitmap(BitmapFactory.decodeFile(imageFile.path))
        var nameInput by remember { mutableStateOf("") }
        TextField(value = nameInput, onValueChange = {nameInput = it}, label = { Text("Item Name") }, placeholder = { Text("Enter Name for Item") })
        Button(onClick = {
            val inventoryApi = getInventoryApiInstance()
            val barcodeId = componentActivity.intent.getStringExtra("barcode_id")
            if (barcodeId != null) {
                uploadItemToInventory(inventoryApi, barcodeId, nameInput, imageFile).enqueue(object : Callback<ResponseBody> {
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
            }
        }) {
            Text("Finish")
        }
    }
}