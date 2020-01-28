package hr.banic.wuhancoronavirusinfo

import com.google.gson.Gson


data class WuweiWWAreaCountsResponse(
    val ret: Int,
    val data: String
) {
    val stats: List<Stats>
        get() = gson.fromJson(data, Array<Stats>::class.java).toList()


    companion object {
        val gson: Gson = Gson()
    }

    data class Stats(
        val country: String,
        val area: String,
        val city: String,
        val confirm: Int,
        val suspect: Int,
        val dead: Int,
        val heal: Int
    )
}
