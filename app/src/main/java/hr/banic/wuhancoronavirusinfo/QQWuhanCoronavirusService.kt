package hr.banic.wuhancoronavirusinfo

import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

interface QQWuhanCoronavirusService {
    @GET("g2/getOnsInfo?name=disease_h5")
    fun getDiseaseH5(): Call<QQDiseaseH5>

    companion object {
        private val client: OkHttpClient = OkHttpClient.Builder()
            .readTimeout(16, TimeUnit.SECONDS)
            .connectTimeout(16, TimeUnit.SECONDS)
            .build()

        private val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("https://view.inews.qq.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val instance: QQWuhanCoronavirusService by lazy {
            retrofit.create(QQWuhanCoronavirusService::class.java)
        }
    }
}