package hr.banic.wuhancoronavirusinfo

import java.util.*

data class Disease(
    val timestampedData: List<TimestampedData> = listOf()
) {
    data class TimestampedData(
        val date: Date = Calendar.getInstance().time,
        val areas: List<Area> = listOf()
    ) {
        @delegate:Transient
        val confirmed: Int by lazy {
            areas.sumBy { it.confirmed }
        }

        @delegate:Transient
        val suspected: Int by lazy {
            areas.sumBy { it.suspected }
        }

        @delegate:Transient
        val deaths: Int by lazy {
            areas.sumBy { it.deaths }
        }

        @delegate:Transient
        val recoveries: Int by lazy {
            areas.sumBy { it.recoveries }
        }
    }

    data class Area(
        val name: String = "N/A",
        val areas: List<Area> = listOf(),
        val stats: Stats? = null
    ) {
        @delegate:Transient
        val flag: String? by lazy {
            localeMap[name]?.flagEmoji
        }

        @delegate:Transient
        val confirmed: Int by lazy {
            stats?.confirmed ?: areas.sumBy { it.confirmed }
        }

        @delegate:Transient
        val suspected: Int by lazy {
            stats?.suspected ?: areas.sumBy { it.suspected }
        }

        @delegate:Transient
        val deaths: Int by lazy {
            stats?.deaths ?: areas.sumBy { it.deaths }
        }

        @delegate:Transient
        val recoveries: Int by lazy {
            stats?.recoveries ?: areas.sumBy { it.recoveries }
        }

        data class Stats(
            val confirmed: Int? = null,
            val suspected: Int? = null,
            val deaths: Int? = null,
            val recoveries: Int? = null
        )
    }

    companion object {
        val localeMap: Map<String, Locale> = Locale.getAvailableLocales().map {
            it.getDisplayCountry(Locale.ENGLISH) to it
        }.toMap().toMutableMap()
    }
}