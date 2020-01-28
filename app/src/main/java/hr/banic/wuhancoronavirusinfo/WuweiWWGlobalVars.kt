package hr.banic.wuhancoronavirusinfo

import com.google.gson.Gson


data class WuweiWWGlobalVars(
    val ret: Int,
    val data: String
) {
    val stats: Stats
        get() = gson.fromJson(data, Array<Stats>::class.java)[0]


    companion object {
        val gson: Gson = Gson()
    }

    data class Stats(
        val confirmCount: Int,
        val suspectCount: Int,
        val deadCount: Int,
        val cure: Int
    )
}
