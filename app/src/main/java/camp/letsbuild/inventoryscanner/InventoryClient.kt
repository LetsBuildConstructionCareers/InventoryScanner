package camp.letsbuild.inventoryscanner

import android.net.Uri
import android.util.Log
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import java.io.File

private const val TAG = "InventoryClient"

const val INVENTORY_SERVER = "http://10.23.1.254:5000"

data class Item(
    val barcode_id: String,
    val name: String,
)

data class User(
    val barcode_id: String,
    val name: String,
    val company: String,
    val picture_path: String,
    val description: String,
)

data class ToolshedCheckout(
    val item_id: String,
    val user_id: String,
)

data class ToolshedCheckin(
    val itemId: String,
    val userId: String,
    val description: String
)

interface InventoryApi {
    @GET("/inventory/api/v1.0/items/{barcode_id}")
    fun getItem(@Path("barcode_id") barcodeId: String): Call<Item>

    @GET("/inventory/api/v1.0/item-picture/{barcode_id}")
    fun getItemPicture(@Path("barcode_id") barcodeId: String): Call<ResponseBody>

    @Multipart
    @POST("/inventory/api/v1.0/items/{barcode_id}")
    fun uploadItem(@Path("barcode_id") barcodeId: String,
                   @Part("name") name: RequestBody,
                   @Part file: MultipartBody.Part): Call<ResponseBody>

    @POST("/inventory/api/v1.0/containers/{container_id}")
    fun addItemsToContainer(@Path("container_id") containerId: String, @Body itemIds: List<String>): Call<ResponseBody>

    @GET("/inventory/api/v1.0/items")
    fun getItems(): Call<List<Item>>

    @POST("/inventory/api/v1.0/toolshed-checkout")
    fun checkoutFromToolshed(@Body toolshedCheckout: ToolshedCheckout): Call<ResponseBody>

    @POST("/inventory/api/v1.0/toolshed-checkin")
    fun checkinToToolshed(@Body toolshedCheckin: ToolshedCheckin): Call<ResponseBody>

    @GET("/inventory/api/v1.0/users/{user_id}")
    @Headers("Cache-Control: no-cache")
    fun getUser(@Path("user_id") userId: String): Call<User>

    @GET("/inventory/api/v1.0/user-picture/{user_id}")
    fun getUserPicture(@Path("user_id") userId: String): Call<ResponseBody>

    @Multipart
    @POST("/inventory/api/v1.0/user-picture/{user_id}")
    fun uploadUserPicture(@Path("user_id") userId: String,
                          @Part file: MultipartBody.Part): Call<ResponseBody>
}

fun getInventoryApiInstance(url: String = INVENTORY_SERVER): InventoryApi {
    return Retrofit.Builder().baseUrl(url).addConverterFactory(GsonConverterFactory.create()).build().create(InventoryApi::class.java)
}

fun getItemPictureUrl(barcodeId: String): String {
    return Uri.parse(INVENTORY_SERVER).buildUpon().appendPath("/inventory/api/v1.0/item-picture/").appendPath(barcodeId).toString()
}

fun getUserPictureUrl(barcodeId: String): String {
    return Uri.parse(INVENTORY_SERVER).buildUpon().appendPath("/inventory/api/v1.0/user-picture/").appendPath(barcodeId).toString()
}

fun uploadItemToInventory(inventoryApi: InventoryApi, barcodeId: String, name: String, picture: File): Call<ResponseBody> {
    val requestBody = picture.asRequestBody("image/*".toMediaTypeOrNull())
    Log.d(TAG, "Uploading " + picture.name)
    val body = MultipartBody.Part.createFormData("picture", picture.name, requestBody)
    val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
    return inventoryApi.uploadItem(barcodeId, nameBody, body)
}

fun uploadUserPictureToInventory(inventoryApi: InventoryApi, userId: String, picture: File): Call<ResponseBody> {
    val requestBody = picture.asRequestBody("image/*".toMediaTypeOrNull())
    val body = MultipartBody.Part.createFormData("picture", picture.name, requestBody)
    return inventoryApi.uploadUserPicture(userId, body)
}