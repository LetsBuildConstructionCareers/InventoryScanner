package camp.letsbuild.inventoryscanner

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
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
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

fun launchViewFullLocationOfItemActivity(itemId: String, componentActivity: ComponentActivity) {
    getInventoryApiInstance(componentActivity).getFullLocationOfItem(itemId).enqueue(object : Callback<FullLocation> {
        override fun onResponse(call: Call<FullLocation>, response: Response<FullLocation>) {
            if (response.isSuccessful) {
                val intent = Intent(componentActivity, ViewFullLocationOfItemActivity::class.java)
                intent.putExtra("fullLocation", response.body())
                componentActivity.startActivity(intent)
            } else {
                TODO("Not yet implemented")
            }
        }

        override fun onFailure(call: Call<FullLocation>, t: Throwable) {
            TODO("Not yet implemented")
        }
    })
}

fun scannerForViewFullLocationOfItem(componentActivity: ComponentActivity): ActivityResultLauncher<ScanOptions> {
    return componentActivity.registerForActivityResult(ScanContract()) { scannedBarcode: ScanIntentResult ->
        run {
            if (scannedBarcode.contents == null) {
                Toast.makeText(componentActivity, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                launchViewFullLocationOfItemActivity(scannedBarcode.contents, componentActivity)
            }
        }
    }
}

class ViewFullLocationOfItemActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fullLocation = intent.getSerializableExtra("fullLocation") as FullLocation?
        val ultimateContainer = fullLocation?.location ?: fullLocation?.vehicle
        val fullLocationAsList: List<String> = (fullLocation?.container_path ?: listOf()) + listOfNotNull(ultimateContainer)
        setContent {
            if (fullLocationAsList.isEmpty()) {
                NoContainersForItemUI(this)
            } else {
                DisplayContainerPathForItemUI(fullLocationAsList, this)
            }
        }
    }
}

@Composable
fun NoContainersForItemUI(componentActivity: ComponentActivity, modifier: Modifier = Modifier
    .fillMaxSize()
    .wrapContentSize(Alignment.Center)) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Item is not associated with any container.")
        Button(onClick = { componentActivity.finish() }) {
            Text("OK")
        }
    }
}

@Composable
fun DisplayContainerPathForItemUI(containerPath: List<String>, componentActivity: ComponentActivity, modifier: Modifier = Modifier
    .fillMaxSize()
    .wrapContentSize(Alignment.Center)) {
    Column (verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.verticalScroll(
        rememberScrollState()
    )) {
        for (containerId in containerPath.reversed()) {
            Row {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(getItemPictureUrl(containerId))
                        .addHeader(AUTHORIZATION, getAuthorization(componentActivity))
                        .crossfade(true)
                        .build(),
                    contentDescription = containerId,
                    modifier = Modifier.fillMaxSize(0.25f),
                    loading = { CircularProgressIndicator() }
                )
            }
        }
        Button(onClick = { componentActivity.finish() }) {
            Text("OK")
        }
    }
}