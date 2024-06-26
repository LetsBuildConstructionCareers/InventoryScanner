package camp.letsbuild.toolshedmanager

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
import camp.letsbuild.inventoryscanner.DisplayItemsCheckedOutByUserActivity
import camp.letsbuild.inventoryscanner.ToolshedCheckinActivity
import camp.letsbuild.inventoryscanner.ToolshedCheckoutActivity
import camp.letsbuild.inventoryscanner.launchDisplayUsersWithOutstandingToolshedCheckoutsActivity
import camp.letsbuild.inventoryscanner.scannerForViewFullLocationOfItem
import camp.letsbuild.toolshedmanager.ui.theme.InventoryScannerTheme
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val scannerForViewFullLocationOfItemActivity = scannerForViewFullLocationOfItem(this)
        super.onCreate(savedInstanceState)
        setContent {
            InventoryScannerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ToolshedManagerUI(scannerForViewFullLocationOfItemActivity, this)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolshedManagerUI(scannerForViewFullLocationOfItemActivity: ActivityResultLauncher<ScanOptions>, componentActivity: ComponentActivity, modifier: Modifier = Modifier
    .fillMaxSize()
    .wrapContentSize(Alignment.Center)) {
    Scaffold(topBar = {
        TopAppBar(title = { Text(componentActivity.getString(R.string.app_name)) })
    }) { padding ->
        Column(
            modifier = modifier.verticalScroll(rememberScrollState()).padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                val intent = Intent(componentActivity, ToolshedCheckoutActivity::class.java)
                componentActivity.startActivity(intent)
            }) {
                Text("Tool Shed - Check-Out")
            }
            Button(onClick = {
                val intent = Intent(componentActivity, ToolshedCheckinActivity::class.java)
                componentActivity.startActivity(intent)
            }) {
                Text("Tool Shed - Check-In")
            }
            Button(onClick = { launchDisplayUsersWithOutstandingToolshedCheckoutsActivity(componentActivity) }) {
                Text("Display Users with Outstanding Toolshed Checkouts")
            }
            Button(onClick = {
                val intent = Intent(componentActivity, DisplayItemsCheckedOutByUserActivity::class.java)
                componentActivity.startActivity(intent)
            }) {
                Text("Display Items Checked-Out by User")
            }
            Button(onClick = { scannerForViewFullLocationOfItemActivity.launch(ScanOptions()) }) {
                Text("View Full Location")
            }
        }
    }
}