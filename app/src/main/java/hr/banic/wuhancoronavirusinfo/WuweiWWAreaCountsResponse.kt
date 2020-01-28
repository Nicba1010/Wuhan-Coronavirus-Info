package hr.banic.wuhancoronavirusinfo

import com.google.gson.Gson


data class WuweiWWAreaCountsResponse(
    val ret: Int = -1,
    val data: String = ""
) {
    @delegate:Transient
    val stats: List<Stats> by lazy {
        gson.fromJson(data, Array<Stats>::class.java).toList()
    }

    @delegate:Transient
    val globalStats: GlobalStats by lazy {
        stats.fold(GlobalStats(0, 0, 0, 0)) { globalStats, stats ->
            globalStats.apply {
                confirm += stats.confirm
                suspect += stats.suspect
                dead += stats.dead
                heal += stats.heal
            }
        }
    }


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

    data class GlobalStats(
        var confirm: Int,
        var suspect: Int,
        var dead: Int,
        var heal: Int
    )
}
