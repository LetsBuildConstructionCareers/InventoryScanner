package camp.letsbuild.inventoryscanner

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

fun launchDisplayItemsCheckedOutByUserActivity(userId: String, componentActivity: ComponentActivity) {
    getInventoryApiInstance(componentActivity).getItemsCheckedOutByUser(userId).enqueue(object : Callback<List<Item>> {
        override fun onResponse(call: Call<List<Item>>, response: Response<List<Item>>) {
            if (response.isSuccessful) {
                val intent = Intent(componentActivity, DisplayItemsCheckedOutByUserViewerActivity::class.java)
                intent.putExtra("checkedOutItems", response.body()?.toTypedArray())
                componentActivity.startActivity(intent)
            } else {
                TODO("Not yet implemented")
            }
        }

        override fun onFailure(call: Call<List<Item>>, t: Throwable) {
            TODO("Not yet implemented")
        }
    })
}

class DisplayItemsCheckedOutByUserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scannerForBadge = registerForActivityResult(ScanContract()) { scannedBarcode: ScanIntentResult ->
            run {
                if (scannedBarcode.contents == null) {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
                } else {
                    val userId = scannedBarcode.contents
                    launchDisplayItemsCheckedOutByUserActivity(userId, this@DisplayItemsCheckedOutByUserActivity)
                }
            }
        }
        setContent {
            Column(modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = { scannerForBadge.launch(ScanOptions()) }) {
                    Text("Scan Badge")
                }
            }
        }
    }
}

class DisplayItemsCheckedOutByUserViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val items = intent.getSerializableExtra("checkedOutItems") as Array<Item>?
        if (items.isNullOrEmpty()) {
            setContent { NoItemsCheckedOutUI(this) }
        } else {
            setContent { DisplayItemsCheckedOutUI(items, this) }
        }
    }
}

@Composable
fun NoItemsCheckedOutUI(componentActivity: ComponentActivity, modifier: Modifier = Modifier
    .fillMaxSize()
    .wrapContentSize(Alignment.Center)) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("User has no items checked out.")
        Button(onClick = { componentActivity.finish() }) {
            Text("OK")
        }
    }
}

@Composable
fun DisplayItemsCheckedOutUI(items: Array<Item>, componentActivity: ComponentActivity, modifier: Modifier = Modifier
    .fillMaxSize()
    .wrapContentSize(Alignment.Center)) {
    Column (verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.verticalScroll(
        rememberScrollState()
    )) {
        for (item in items) {
            Row {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(getItemPictureUrl(item.barcode_id))
                        .addHeader(AUTHORIZATION, getAuthorization(componentActivity))
                        .crossfade(true)
                        .build(),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(0.25f),
                    loading = { CircularProgressIndicator() }
                )
                Text(item.name)
            }
        }
        Button(onClick = { componentActivity.finish() }) {
            Text("OK")
        }
    }
}