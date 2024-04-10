package camp.letsbuild.inventoryscanner

import android.util.Log
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import java.io.File

private const val TAG = "InventoryClient"

data class Item(
    val barcode_id: String,
    val picture_path: String,
)

interface InventoryApi {
    @GET("/inventory/api/v1.0/items/{barcode_id}")
    fun getItem(@Path("barcode_id") barcodeId: String): Call<Item>

    @Multipart
    @POST("/inventory/api/v1.0/items/{barcode_id}")
    fun uploadItem(@Path("barcode_id") barcodeId: String,
                   @Part file: MultipartBody.Part): Call<ResponseBody>

    @GET("/inventory/api/v1.0/items")
    fun getItems(): Call<List<Item>>
}

fun getInventoryApiInstance(url: String = "http://10.23.1.254:5000"): InventoryApi {
    return Retrofit.Builder().baseUrl(url).addConverterFactory(GsonConverterFactory.create()).build().create(InventoryApi::class.java)
}

fun uploadItemToInventory(inventoryApi: InventoryApi, barcodeId: String, picture: File): Call<ResponseBody> {
    val requestBody = RequestBody.create(MediaType.parse("image/*"), picture)
    Log.d(TAG, "Uploading " + picture.name)
    val body = MultipartBody.Part.createFormData("picture", picture.name, requestBody)
    return inventoryApi.uploadItem(barcodeId, body)
}