package camp.letsbuild.inventoryscanner

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val TAG = "CreateUserWithoutPictureActivity"

class CreateUserWithoutPictureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val barcodeId = intent.getStringExtra("barcode_id") ?: return
        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var nameInput by remember { mutableStateOf("") }
                TextField(value = nameInput, onValueChange = {nameInput = it}, label = { Text("User Name") }, placeholder = { Text("Enter Name for User") })
                var company by remember { mutableStateOf("") }
                TextField(value = company, onValueChange = {company = it}, label = { Text("Company") }, placeholder = { Text("Enter Company of User") })
                val userTypes = listOf("Camper", "Volunteer", "Admin")
                val (selectedUserType, onOptionSelected) = remember { mutableStateOf(userTypes[0]) }
                userTypes.forEach {userType ->
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (userType == selectedUserType),
                            onClick = { onOptionSelected(userType) })) {
                        RadioButton(selected = (userType == selectedUserType), onClick = { onOptionSelected(userType) })
                        Text(userType)
                    }
                }
                var description by remember { mutableStateOf("") }
                TextField(value = description, onValueChange = { description = it}, label = { Text("Additional Info") }, placeholder = { Text("Enter any additional info") })
                Button(onClick = {
                    getInventoryApiInstance(this@CreateUserWithoutPictureActivity)
                        .createUserWithoutPicture(User(barcodeId, nameInput, company, "", selectedUserType, description))
                        .enqueue(object : Callback<ResponseBody> {
                            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                finish()
                            }

                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                Log.e(TAG, t.toString())
                                TODO("Not yet implemented")
                            }
                        })
                }) {
                    Text("Done")
                }
            }
        }
    }
}