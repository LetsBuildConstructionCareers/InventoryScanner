package camp.letsbuild.inventoryscanner

import android.app.Application.ActivityLifecycleCallbacks
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

private const val TAG = "PerformInventoryActivity"

fun launchPerformInventoryActivity(componentActivity: ComponentActivity) {
    getInventoryApiInstance(componentActivity).getInventoryEvents().enqueue(object : Callback<List<InventoryEvent>> {
        override fun onResponse(call: Call<List<InventoryEvent>>, response: Response<List<InventoryEvent>>) {
            if (response.isSuccessful) {
                val intent = Intent(componentActivity, PerformInventoryActivity::class.java)
                val existingInventories = response.body() ?: emptyList()
                intent.putExtra("existingInventories", existingInventories.toTypedArray())
                componentActivity.startActivity(intent)
            }
        }

        override fun onFailure(call: Call<List<InventoryEvent>>, t: Throwable) {
            TODO("Not yet implemented")
        }
    })
}

fun unixTimeToLocalTime(unixTime: Int): LocalDateTime {
    val instant = Instant.ofEpochSecond(unixTime.toLong())
    val zoneId = ZoneId.systemDefault()
    return instant.atZone(zoneId).toLocalDateTime()
}
class PerformInventoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val existingInventories = (intent.getSerializableExtra("existingInventories") as Array<InventoryEvent>?) ?: return
        setContent {
            Column (verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)) {
                Text("Select Inventory Below to Continue")
                for (inventoryEvent in existingInventories) {
                    Button(onClick = { launchViewItemsToInventoryActivityFromInventoryEvent(this@PerformInventoryActivity, inventoryEvent) }) {
                        val startTimeString = unixTimeToLocalTime(inventoryEvent.start_unix_time).toString()
                        val endTimeString = if (inventoryEvent.complete_unix_time != null) {
                            unixTimeToLocalTime(inventoryEvent.complete_unix_time).toString()
                        } else {
                            "current"
                        }
                        Text("$startTimeString - $endTimeString")
                    }
                }
                Button(onClick = { getInventoryApiInstance(this@PerformInventoryActivity).createNewInventoryEvent().enqueue(object : Callback<InventoryEvent> {
                    override fun onResponse(call: Call<InventoryEvent>, response: Response<InventoryEvent>) {
                        if (response.isSuccessful && response.body() != null) {
                            launchViewItemsToInventoryActivityFromInventoryEvent(this@PerformInventoryActivity, response.body()!!)
                        } else {
                            TODO("Not yet implemented")
                        }
                    }

                    override fun onFailure(call: Call<InventoryEvent>, t: Throwable) {
                        Log.e(TAG, t.toString())
                        TODO("Not yet implemented")
                    }
                }) }) {
                    Text("Start New Inventory")
                }
            }
        }
    }
}

fun launchViewItemsToInventoryActivityFromInventoryEvent(componentActivity: ComponentActivity, inventoryEvent: InventoryEvent) {
    val inventoryApi = getInventoryApiInstance(componentActivity)
    inventoryApi.getAllItemsNotInContainers().enqueue(object : Callback<List<Item>> {
        override fun onResponse(call: Call<List<Item>>, response: Response<List<Item>>) {
            if (response.isSuccessful) {
                val itemList = response.body() ?: emptyList()
                inventoryApi.getAllInventoriedItemsNotInContainers(inventoryEvent.id).enqueue(object : Callback<List<InventoriedItem>> {
                    override fun onResponse(call: Call<List<InventoriedItem>>, response: Response<List<InventoriedItem>>) {
                        if (response.isSuccessful) {
                            val inventoriedItemList = response.body() ?: emptyList()
                            launchViewItemsToInventoryActivityFromItems(componentActivity, inventoryEvent, null, null, itemList, inventoriedItemList)
                        }
                    }

                    override fun onFailure(call: Call<List<InventoriedItem>>, t: Throwable) {
                        TODO("Not yet implemented")
                    }
                })
            }
        }

        override fun onFailure(call: Call<List<Item>>, t: Throwable) {
            TODO("Not yet implemented")
        }
    })
}

fun launchViewItemsToInventoryActivityFromItemId(componentActivity: ComponentActivity, inventoryEvent: InventoryEvent, itemId: String) {
    val inventoryApi = getInventoryApiInstance(componentActivity)
    inventoryApi.getItem(itemId).enqueue(object : Callback<Item> {
        override fun onResponse(call: Call<Item>, response: Response<Item>) {
            if (response.isSuccessful && response.body() != null) {
                val item = response.body()!!
                inventoryApi.getInventoriedItem(inventoryEvent.id, itemId).enqueue(object : Callback<InventoriedItem> {
                    override fun onResponse(call: Call<InventoriedItem>, response: Response<InventoriedItem>) {
                        if (response.isSuccessful) {
                            launchViewItemsToInventoryActivityFromSingleItem(componentActivity, inventoryEvent, item, response.body())
                        } else {
                            TODO("Not yet implemented")
                        }
                    }

                    override fun onFailure(call: Call<InventoriedItem>, t: Throwable) {
                        TODO("Not yet implemented")
                    }
                })
            } else {
                TODO("Not yet implemented")
            }
        }

        override fun onFailure(call: Call<Item>, t: Throwable) {
            TODO("Not yet implemented")
        }
    })
}

fun launchViewItemsToInventoryActivityFromSingleItem(componentActivity: ComponentActivity, inventoryEvent: InventoryEvent, itemToInventory: Item, inventoriedItem: InventoriedItem?) {
    val inventoryApi = getInventoryApiInstance(componentActivity)
    inventoryApi.getItemsInContainer(itemToInventory.barcode_id).enqueue(object : Callback<List<Item>> {
        override fun onResponse(call: Call<List<Item>>, response: Response<List<Item>>) {
            if (response.isSuccessful) {
                val itemList = response.body() ?: emptyList()
                inventoryApi.getAllInventoriedItemsInContainer(inventoryEvent.id, itemToInventory.barcode_id).enqueue(object : Callback<List<InventoriedItem>> {
                    override fun onResponse(call: Call<List<InventoriedItem>>, response: Response<List<InventoriedItem>>) {
                        if (response.isSuccessful) {
                            val inventoriedItemList = response.body() ?: emptyList()
                            launchViewItemsToInventoryActivityFromItems(componentActivity, inventoryEvent, itemToInventory, inventoriedItem, itemList, inventoriedItemList)
                        } else {
                            TODO("Not yet implemented")
                        }
                    }

                    override fun onFailure(call: Call<List<InventoriedItem>>, t: Throwable) {
                        TODO("Not yet implemented")
                    }
                })
            } else {
                TODO("Not yet implemented")
            }
        }

        override fun onFailure(call: Call<List<Item>>, t: Throwable) {
            TODO("Not yet implemented")
        }
    })
}

fun launchViewItemsToInventoryActivityFromItems(componentActivity: ComponentActivity, inventoryEvent: InventoryEvent, itemToInventory: Item?, inventoriedItem: InventoriedItem?, items: List<Item>, inventoriedItems: List<InventoriedItem>) {
    val intent = Intent(componentActivity, ViewItemsToInventoryActivity::class.java)
    intent.putExtra("itemToInventory", itemToInventory)
    intent.putExtra("inventoriedItem", inventoriedItem)
    intent.putExtra("inventoryEvent", inventoryEvent)
    intent.putExtra("itemList", items.toTypedArray())
    intent.putExtra("inventoriedItemList", inventoriedItems.toTypedArray())
    componentActivity.startActivity(intent)
}

fun scannerForViewItemsToInventoryActivity(componentActivity: ComponentActivity, inventoryEvent: InventoryEvent): ActivityResultLauncher<ScanOptions> {
    return componentActivity.registerForActivityResult(ScanContract()) { scannedBarcode: ScanIntentResult ->
        run {
            if (scannedBarcode.contents == null) {
                Toast.makeText(componentActivity, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                launchViewItemsToInventoryActivityFromItemId(componentActivity, inventoryEvent, scannedBarcode.contents)
            }
        }
    }
}

class OnResumeListener {
    var onResume = {}

    fun fireOnResume() {
        onResume()
    }
}

class ViewItemsToInventoryActivity : ComponentActivity() {
    private val onResumeListener = OnResumeListener()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val itemToInventory = intent.getSerializableExtra("itemToInventory") as Item?
        val inventoriedItem = intent.getSerializableExtra("inventoriedItem") as InventoriedItem?
        val inventoryEvent = (intent.getSerializableExtra("inventoryEvent") as InventoryEvent?) ?: return
        val itemList = (intent.getSerializableExtra("itemList") as Array<Item>?) ?: return
        val inventoriedItemList = (intent.getSerializableExtra("inventoriedItemList") as Array<InventoriedItem>?) ?: return
        val scanner = scannerForViewItemsToInventoryActivity(this, inventoryEvent)

        setContent { ViewItemsToInventoryUI(
            itemToInventory,
            inventoriedItem,
            itemList,
            inventoriedItemList,
            inventoryEvent,
            scanner,
            onResumeListener,
            this
        ) }
    }

    override fun onResume() {
        super.onResume()

        onResumeListener.fireOnResume()
    }
}

fun isItemInventoriable(childItems: Array<Item>, childInventoriedItems: Array<InventoriedItem>): Boolean {
    if (childItems.isEmpty()) {
        return true
    }
    val itemIds = childItems.map { item -> item.barcode_id }.toSet()
    return childInventoriedItems.map { item -> item.item_id }.toSet() == itemIds
}

@Composable
fun ManualIdEnterDialog(inventoryEvent: InventoryEvent,
                        componentActivity: ComponentActivity,
                        onDismiss: () -> Unit,
                        modifier: Modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center)) {
    Dialog(onDismissRequest = { onDismiss() }) {
        Column {
            var itemId by remember { mutableStateOf("") }
            TextField(value = itemId, onValueChange = { itemId = it}, label = { Text("Barcode ID") }, placeholder = { Text("Enter barcode ID of item") })
            Row() {
                Button(onClick = { launchViewItemsToInventoryActivityFromItemId(componentActivity, inventoryEvent, itemId) }) {
                    Text("Confirm")
                }
                Button(onClick = { onDismiss() }) {
                    Text("Dismiss")
                }
            }
        }
    }
}

@Composable
fun ViewItemsToInventoryUI(itemToInventory: Item?,
                           inventoriedItemToInventory: InventoriedItem?,
                           childItems: Array<Item>,
                           childInventoriedItems: Array<InventoriedItem>,
                           inventoryEvent: InventoryEvent,
                           scannerForViewItemsToInventory: ActivityResultLauncher<ScanOptions>,
                           onResumeListener: OnResumeListener,
                           componentActivity: ComponentActivity,
                           modifier: Modifier = Modifier
                               .fillMaxSize()
                               .wrapContentSize(Alignment.Center)) {
    var inventoriedItemsRequiresUpdate by remember { mutableStateOf(true) }
    onResumeListener.onResume = { inventoriedItemsRequiresUpdate = true }
    val childInventoriedItemsMap = remember { mutableStateMapOf(*childInventoriedItems.map { item -> item.item_id to item }.toTypedArray()) }
    if (inventoriedItemsRequiresUpdate && itemToInventory != null) {
        getInventoryApiInstance(componentActivity)
            .getAllInventoriedItemsInContainer(inventoryEvent.id, itemToInventory.barcode_id)
            .enqueue(object : Callback<List<InventoriedItem>> {
                override fun onResponse(call: Call<List<InventoriedItem>>, response: Response<List<InventoriedItem>>) {
                    if (response.isSuccessful) {
                        val inventoriedItems = response.body() ?: emptyList()
                        childInventoriedItemsMap.clear()
                        childInventoriedItemsMap.putAll(inventoriedItems.associateBy { inventoriedItem -> inventoriedItem.item_id })
                        inventoriedItemsRequiresUpdate = false
                    } else {
                        TODO("Not yet implemented")
                    }
                }

                override fun onFailure(call: Call<List<InventoriedItem>>, t: Throwable) {
                    TODO("Not yet implemented")
                }
            })
    } else if (inventoriedItemsRequiresUpdate) {
        getInventoryApiInstance(componentActivity)
            .getAllInventoriedItemsNotInContainers(inventoryEvent.id)
            .enqueue(object : Callback<List<InventoriedItem>> {
                override fun onResponse(call: Call<List<InventoriedItem>>, response: Response<List<InventoriedItem>>) {
                    if (response.isSuccessful) {
                        val inventoriedItems = response.body() ?: emptyList()
                        childInventoriedItemsMap.clear()
                        childInventoriedItemsMap.putAll(inventoriedItems.associateBy { inventoriedItem -> inventoriedItem.item_id })
                        inventoriedItemsRequiresUpdate = false
                    } else {
                        TODO("Not yet implemented")
                    }
                }

                override fun onFailure(call: Call<List<InventoriedItem>>, t: Throwable) {
                    TODO("Not yet implemented")
                }
            })
    }
    Column (verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.verticalScroll(
        rememberScrollState()
    )) {
        val inventoryStatusOptions = InventoryStatus.entries.toTypedArray()
        val defaultStatus = inventoriedItemToInventory?.status ?: inventoryStatusOptions[0]
        val (selectedStatus, onOptionSelected) = remember { mutableStateOf(defaultStatus) }
        val defaultNotesText = inventoriedItemToInventory?.notes ?: ""
        var notes by remember { mutableStateOf(defaultNotesText) }
        if (itemToInventory != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(getItemPictureUrl(itemToInventory.barcode_id))
                    .addHeader(AUTHORIZATION, getAuthorization(componentActivity))
                    .crossfade(true)
                    .build(),
                contentDescription = itemToInventory.name,
                modifier = Modifier.fillMaxSize(0.25f),
                loading = { CircularProgressIndicator() },
                error = { Text("Cannot display image!") }
            )
            Text(itemToInventory.name)
            Text(itemToInventory.barcode_id)
            inventoryStatusOptions.forEach { statusOption ->
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (statusOption == selectedStatus),
                        onClick = { onOptionSelected(statusOption) })) {
                    RadioButton(selected = (statusOption == selectedStatus), onClick = { onOptionSelected(statusOption) })
                    Text(statusOption.name)
                }
            }
            TextField(value = notes, onValueChange = { notes = it}, label = { Text("Notes") }, placeholder = { Text("Enter any additional notes") })
        }
        //val childInventoriedItemsMap = childInventoriedItems.associateBy { inventoriedItem -> inventoriedItem.item_id }
        val defaultCardColors: CardColors = CardDefaults.cardColors()
        val goodCardColors = CardDefaults.cardColors(containerColor = Color.Green)
        val badCardColors = CardDefaults.cardColors(containerColor = Color.Red)
        for (item in childItems) {
            val inventoriedItem = childInventoriedItemsMap.getOrDefault(item.barcode_id, null)
            val cardColors = when (inventoriedItem?.status) {
                null -> defaultCardColors
                InventoryStatus.GOOD -> goodCardColors
                else -> badCardColors
            }
            Card(colors = cardColors) {
                Row {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(getItemPictureUrl(item.barcode_id))
                            .addHeader(AUTHORIZATION, getAuthorization(componentActivity))
                            .crossfade(true)
                            .build(),
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(0.25f),
                        loading = { CircularProgressIndicator() },
                        error = { Text("Cannot display image!") }
                    )
                    Text(item.name)
                    Text(item.barcode_id)
                    Button(onClick = {
                        launchViewItemsToInventoryActivityFromSingleItem(
                            componentActivity,
                            inventoryEvent,
                            item,
                            inventoriedItem
                        )
                        inventoriedItemsRequiresUpdate = true
                    }) {
                        Text("Inventory Item")
                    }
                }
            }
        }
        if (childItems.isNotEmpty()) {
            Button(onClick = {
                scannerForViewItemsToInventory.launch(ScanOptions())
                inventoriedItemsRequiresUpdate = true
            }) {
                Text("Scan Item to Inventory")
            }
            var manualEntryDialogIsVisible by remember { mutableStateOf(false) }
            Button(onClick = { manualEntryDialogIsVisible = true }) {
                Text("Manually Enter Item to Inventory")
            }
            if (manualEntryDialogIsVisible) {
                ManualIdEnterDialog(inventoryEvent, componentActivity, { manualEntryDialogIsVisible = false })
                inventoriedItemsRequiresUpdate = true
            }
        }
        if (itemToInventory != null) {
            Button(onClick = {
                getInventoryApiInstance(componentActivity)
                    .addInventoriedItem(InventoriedItem(inventoryEvent.id, itemToInventory.barcode_id, selectedStatus, notes))
                    .enqueue(object : Callback<ResponseBody> {
                        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                            componentActivity.finish()
                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            TODO("Not yet implemented")
                        }
                    }) },
                enabled = isItemInventoriable(childItems, childInventoriedItemsMap.values.toTypedArray())) {
                Text("Item Complete")
            }
        }
    }
}