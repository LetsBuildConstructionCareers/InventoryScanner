package camp.letsbuild.inventoryscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ViewItemsInContainerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val containerId = intent.getStringExtra("barcode_id") ?: return
        getInventoryApiInstance(this).getItemsInContainer(containerId).enqueue(object : Callback<List<Item>> {
            override fun onResponse(call: Call<List<Item>>, response: Response<List<Item>>) {
                val itemList = response.body()
                if (response.isSuccessful && itemList != null) {
                    setContent { DisplayItemsUI(itemList, this@ViewItemsInContainerActivity) }
                }
            }

            override fun onFailure(call: Call<List<Item>>, t: Throwable) {
                TODO("Not yet implemented")
            }
        })
    }
}

@Composable
fun DisplayItemsUI(itemList: List<Item>, componentActivity: ComponentActivity, modifier: Modifier = Modifier
    .fillMaxSize().wrapContentSize(Alignment.Center)) {
    Column (verticalArrangement = Arrangement.SpaceBetween, modifier = modifier.verticalScroll(rememberScrollState())) {
        for (item in itemList) {
            Row(modifier = Modifier.fillMaxWidth()) {
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
            Text("Done")
        }
    }
}