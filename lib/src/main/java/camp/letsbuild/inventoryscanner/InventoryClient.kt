package camp.letsbuild.inventoryscanner

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import org.apache.hc.core5.http.impl.EnglishReasonPhraseCatalog
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import java.io.File
import java.util.Locale

private const val TAG = "InventoryClient"

const val INVENTORY_SERVER = "https://inventory-server-service-wnjkb3ho2q-ul.a.run.app"
//const val INVENTORY_SERVER = "http://10.23.1.254:5000"
const val AUTHORIZATION = "Authorization"

fun getAuthorization(context: Context): String {
    return context.getString(R.string.authorization)
}

fun getStatusText(status: Int): String {
    return EnglishReasonPhraseCatalog.INSTANCE.getReason(status, Locale.US)
}

class ServiceInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().addHeader(AUTHORIZATION, getAuthorization(context)).build()
        return chain.proceed(request)
    }
}

@Serializable
data class Item(
    val barcode_id: String,
    val name: String,
) : java.io.Serializable

@Serializable
data class FullLocation(
    val container_path: List<String>,
    val vehicle: String?,
    val location: String?,
) : java.io.Serializable

@Serializable
data class User(
    val barcode_id: String,
    val name: String,
    val company: String,
    val picture_path: String,
    val user_type: String,
    val description: String,
    val initial_checkin_info: String,
) : java.io.Serializable

@Serializable
data class ToolshedCheckout(
    val checkout_id: String,
    val item_id: String,
    val user_id: String,
    val unix_time: Long,
) : java.io.Serializable

data class ToolshedCheckin(
    val checkin_id: String?,
    val checkout_id: String?,
    val item_id: String,
    val user_id: String,
    val unix_time: Long,
    val override_justification: String?,
    val description: String?
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

    @GET("/inventory/api/v1.0/full-location/{item_id}")
    fun getFullLocationOfItem(@Path("item_id") itemId: String): Call<FullLocation>

    @GET("/inventory/api/v1.0/item-parent/{barcode_id}")
    fun getParentOfItem(@Path("barcode_id") barcodeId: String): Call<String>

    @GET("/inventory/api/v1.0/containers/{container_id}")
    fun getItemsInContainer(@Path("container_id") containerId: String): Call<List<Item>>

    @POST("/inventory/api/v1.0/containers/{container_id}")
    fun addItemsToContainer(@Path("container_id") containerId: String, @Body itemIds: List<String>): Call<ResponseBody>

    @DELETE("/inventory/api/v1.0/containers/{container_id}/{item_id}")
    fun removeItemFromContainer(@Path("container_id") containerId: String, @Path("item_id") itemId: String): Call<ResponseBody>

    @GET("/inventory/api/v1.0/vehicles/{container_id}")
    fun getItemsInVehicle(@Path("container_id") containerId: String): Call<List<Item>>

    @POST("/inventory/api/v1.0/vehicles/{container_id}")
    fun addItemsToVehicle(@Path("container_id") containerId: String, @Body itemIds: List<String>): Call<ResponseBody>

    @DELETE("/inventory/api/v1.0/vehicles/{container_id}/{item_id}")
    fun removeItemFromVehicle(@Path("container_id") containerId: String, @Path("item_id") itemId: String): Call<ResponseBody>

    @GET("/inventory/api/v1.0/locations/{container_id}")
    fun getItemsInLocation(@Path("container_id") containerId: String): Call<List<Item>>

    @POST("/inventory/api/v1.0/locations/{container_id}")
    fun addItemsToLocation(@Path("container_id") containerId: String, @Body itemIds: List<String>): Call<ResponseBody>

    @DELETE("/inventory/api/v1.0/locations/{container_id}/{item_id}")
    fun removeItemFromLocation(@Path("container_id") containerId: String, @Path("item_id") itemId: String): Call<ResponseBody>

    @GET("/inventory/api/v1.0/items")
    fun getItems(): Call<List<Item>>

    @POST("/inventory/api/v1.0/toolshed-checkout")
    fun checkoutFromToolshed(@Body toolshedCheckout: ToolshedCheckout): Call<ResponseBody>

    @GET("/inventory/api/v1.0/toolshed-checkout/{item_id}/last-outstanding")
    fun getLastOutstandingCheckout(@Path("item_id") itemId: String): Call<ToolshedCheckout>

    @POST("/inventory/api/v1.0/toolshed-checkin")
    fun checkinToToolshed(@Body toolshedCheckin: ToolshedCheckin): Call<ResponseBody>

    @GET("/inventory/api/v1.0/users/{user_id}/toolshed-checkout-outstanding")
    fun getItemsCheckedOutByUser(@Path("user_id") userId: String): Call<List<Item>>

    @GET("/inventory/api/v1.0/users-toolshed-checkout-outstanding")
    fun getUsersWithOutstandingToolshedCheckouts(): Call<List<User>>

    @GET("/inventory/api/v1.0/users/{user_id}")
    @Headers("Cache-Control: no-cache")
    fun getUser(@Path("user_id") userId: String): Call<User>

    @GET("/inventory/api/v1.0/user-picture/{user_id}")
    fun getUserPicture(@Path("user_id") userId: String): Call<ResponseBody>

    @Multipart
    @POST("/inventory/api/v1.0/user-picture/{user_id}")
    fun uploadUserPicture(@Path("user_id") userId: String,
                          @Part file: MultipartBody.Part): Call<ResponseBody>

    @POST("/inventory/api/v1.0/user-checkin/{user_id}")
    fun checkinUser(@Path("user_id") userId: String): Call<ResponseBody>

    @POST("/inventory/api/v1.0/user-checkout/{user_id}")
    fun checkoutUser(@Path("user_id") userId: String): Call<ResponseBody>

    @POST("/inventory/api/v1.0/users/")
    fun createUserWithoutPicture(@Body user: User): Call<ResponseBody>
}

fun getInventoryApiInstance(context: Context, url: String = INVENTORY_SERVER): InventoryApi {
    return Retrofit.Builder()
        .baseUrl(url)
        .client(OkHttpClient.Builder().addInterceptor(ServiceInterceptor(context)).build())
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(InventoryApi::class.java)
}

fun getItemPictureUrl(barcodeId: String): String {
    return "$INVENTORY_SERVER/inventory/api/v1.0/item-picture/$barcodeId"
    //return Uri.parse(INVENTORY_SERVER).buildUpon().appendPath("/inventory/api/v1.0/item-picture/").appendPath(barcodeId).toString()
}

fun getUserPictureUrl(barcodeId: String): String {
    return "$INVENTORY_SERVER/inventory/api/v1.0/user-picture/$barcodeId"
    //return Uri.parse(INVENTORY_SERVER).buildUpon().appendPath("/inventory/api/v1.0/user-picture/").appendPath(barcodeId).toString()
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