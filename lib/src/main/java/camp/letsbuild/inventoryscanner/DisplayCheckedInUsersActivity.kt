package camp.letsbuild.inventoryscanner

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val TAG = "DisplayCheckedInUsersActivity"

fun launchDisplayCheckedInUsersActivity(componentActivity: ComponentActivity) {
    getInventoryApiInstance(componentActivity).getCheckedInUsers().enqueue(object : Callback<List<User>> {
        override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
            if (response.isSuccessful) {
                val intent = Intent(componentActivity, DisplayCheckedInUsersActivity::class.java)
                intent.putExtra("checkedInUsers", response.body()?.toTypedArray())
                componentActivity.startActivity(intent)
            }
        }

        override fun onFailure(call: Call<List<User>>, t: Throwable) {
            Log.e(TAG, t.toString())
            TODO("Not yet implemented")
        }
    })
}

class DisplayCheckedInUsersActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val usersWithCheckouts = intent.getSerializableExtra("checkedInUsers") as Array<User>?
        setContent {
            if (usersWithCheckouts.isNullOrEmpty()) {
                NoCheckedInUsers(this)
            } else {
                DisplayCheckedInUsersUI(usersWithCheckouts, this)
            }
        }
    }
}

@Composable
fun NoCheckedInUsers(componentActivity: ComponentActivity, modifier: Modifier = Modifier
    .fillMaxSize()
    .wrapContentSize(Alignment.Center)) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No users are currently checked in.")
        Button(onClick = { componentActivity.finish() }) {
            Text("OK")
        }
    }
}

@Composable
fun DisplayCheckedInUsersUI(checkedInUsers: Array<User>, componentActivity: ComponentActivity, modifier: Modifier = Modifier
    .fillMaxSize()
    .wrapContentSize(Alignment.Center)) {
    Column (verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.verticalScroll(
        rememberScrollState()
    )) {
        for (user in checkedInUsers) {
            Row {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(getUserPictureUrl(user.barcode_id))
                        .addHeader(AUTHORIZATION, getAuthorization(componentActivity))
                        .crossfade(true)
                        .build(),
                    contentDescription = user.name,
                    modifier = Modifier.fillMaxSize(0.25f),
                    loading = { CircularProgressIndicator() }
                )
                Text(user.name)
                Button(onClick = { launchCheckoutUserActivity(user.barcode_id, componentActivity) }) {
                    Text("Checkout User")
                }
            }
        }
        Button(onClick = { componentActivity.finish() }) {
            Text("OK")
        }
    }
}