package hr.banic.wuhancoronavirusinfo

import com.google.gson.Gson


data class WuweiWWAreaCountsResponse(
    val ret: Int = -1,
    val data: String = ""
) {
    @delegate:Transient
    val stats: List<AreaStats> by lazy {
        gson.fromJson(data, Array<AreaStats>::class.java).toList()
    }

    @delegate:Transient
    val countryStats: List<CountryStats> by lazy {
        stats.groupingBy {
            it.country
        }.fold({ _, _ ->
            Stats()
        }, { _, countryStats, stats ->
            countryStats.apply {
                confirm += stats.confirm
                suspect += stats.suspect
                dead += stats.dead
                heal += stats.heal
            }
        }).map {
            CountryStats(
                it.key,
                it.value.confirm,
                it.value.suspect,
                it.value.dead,
                it.value.heal
            )
        }
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

    data class AreaStats(
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

    data class Stats(
        var confirm: Int = 0,
        var suspect: Int = 0,
        var dead: Int = 0,
        var heal: Int = 0
    )

    data class CountryStats(
        val country: String,
        val confirm: Int = 0,
        val suspect: Int = 0,
        val dead: Int = 0,
        val heal: Int = 0
    )
}
