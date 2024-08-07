package camp.letsbuild.usermanager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import camp.letsbuild.inventoryscanner.CheckinUserActivity
import camp.letsbuild.inventoryscanner.CheckoutUserActivity
import camp.letsbuild.inventoryscanner.CreateUserWithoutPictureActivity
import camp.letsbuild.inventoryscanner.InitialBadgeCheckInLandingActivity
import camp.letsbuild.inventoryscanner.UpdateUserPictureActivity
import camp.letsbuild.inventoryscanner.launchDisplayCheckedInUsersActivity
import camp.letsbuild.inventoryscanner.scannerForNewActivity
import camp.letsbuild.usermanager.ui.theme.InventoryScannerTheme
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scannerForCheckinUserActivity = scannerForNewActivity(this, CheckinUserActivity::class.java)
        val scannerForCheckoutUserActivity = scannerForNewActivity(this, CheckoutUserActivity::class.java)
        val scannerForCreateUserWithoutPictureActivity = scannerForNewActivity(this, CreateUserWithoutPictureActivity::class.java)
        setContent {
            InventoryScannerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UserManagerUI(scannerForCheckinUserActivity, scannerForCheckoutUserActivity, scannerForCreateUserWithoutPictureActivity, this)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagerUI(scannerForCheckinUserActivity: ActivityResultLauncher<ScanOptions>,
                  scannerForCheckoutUserActivity: ActivityResultLauncher<ScanOptions>,
                  scannerForCreateUserWithoutPictureActivity: ActivityResultLauncher<ScanOptions>,
                  componentActivity: ComponentActivity, modifier: Modifier = Modifier
        .fillMaxSize()
        .wrapContentSize(Alignment.Center)) {
    Scaffold(topBar = {
        TopAppBar(title = { Text(componentActivity.getString(R.string.app_name)) })
    }) { padding ->
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                val intent =
                    Intent(componentActivity, InitialBadgeCheckInLandingActivity::class.java)
                componentActivity.startActivity(intent)
            }) {
                Text("Initial Badge Check-In")
            }
            Button(onClick = { scannerForCheckinUserActivity.launch(ScanOptions()) }) {
                Text("Badge Check-In")
            }
            Button(onClick = { scannerForCheckoutUserActivity.launch(ScanOptions()) }) {
                Text("Badge Check-Out")
            }
            Button(onClick = { launchDisplayCheckedInUsersActivity(componentActivity) }) {
                Text("Display Checked In Users")
            }
            Button(onClick = { scannerForCreateUserWithoutPictureActivity.launch(ScanOptions()) }) {
                Text("Create User without Picture")
            }
            Button(onClick = { componentActivity.startActivity(Intent(componentActivity, UpdateUserPictureActivity::class.java)) }) {
                Text("Update User's Picture")
            }
        }
    }
}