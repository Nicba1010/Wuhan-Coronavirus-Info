package hr.banic.wuhancoronavirusinfo

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

interface WufluService {
    @GET("john_hopkins_csse_data.json")
    fun getJohnHopkinsCSSEData(): Call<Disease>

    companion object {
        private val gson: Gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm z")
            .create()

        private val client: OkHttpClient = OkHttpClient.Builder()
            .readTimeout(16, TimeUnit.SECONDS)
            .connectTimeout(16, TimeUnit.SECONDS)
            .build()

        private val retrofit: Retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl("https://wuflu.banic.stream/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val instance: WufluService by lazy {
            retrofit.create(WufluService::class.java)
        }
    }
}