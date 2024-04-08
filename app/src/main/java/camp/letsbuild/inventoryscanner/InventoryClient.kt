package camp.letsbuild.inventoryscanner

import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

data class Item(
    val barcode_id: String,
    val picture_path: String,
)

interface InventoryApi {
    @GET("/inventory/api/v1.0/items/{barcode_id}")
    fun getItem(@Path("barcode_id") barcodeId: String): Call<Item>

    @GET("/inventory/api/v1.0/items")
    fun getItems(): Call<List<Item>>
}

fun getInventoryApiInstance(url: String): InventoryApi {
    return Retrofit.Builder().baseUrl(url).addConverterFactory(GsonConverterFactory.create()).build().create(InventoryApi::class.java)
}