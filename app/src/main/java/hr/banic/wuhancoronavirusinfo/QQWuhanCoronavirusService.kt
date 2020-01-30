package hr.banic.wuhancoronavirusinfo

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface QQWuhanCoronavirusService {
    @GET("g2/getOnsInfo?name=disease_h5")
    fun getDiseaseH5(): Call<QQDiseaseH5>

    companion object {
        private val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("https://view.inews.qq.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val instance: QQWuhanCoronavirusService by lazy {
            retrofit.create(QQWuhanCoronavirusService::class.java)
        }
    }
}