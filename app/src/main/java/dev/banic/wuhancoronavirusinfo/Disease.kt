package dev.banic.wuhancoronavirusinfo

import com.google.gson.annotations.SerializedName
import java.util.*

data class Disease(
    @SerializedName("timestamped_data")
    val timestampedData: MutableList<TimestampedData> = mutableListOf()
) {
    data class TimestampedData(
        val date: Date = Calendar.getInstance().time,
        val areas: List<Area> = listOf()
    ) : IStats {
        @delegate:Transient
        override val confirmed: Int by lazy {
            areas.sumBy {
                it.confirmed
            }.takeUnless {
                it == 0
            } ?: -1
        }

        @delegate:Transient
        override val deaths: Int by lazy {
            areas.sumBy {
                it.deaths
            }.takeUnless {
                it == 0
            } ?: -1
        }

        @delegate:Transient
        override val recoveries: Int by lazy {
            areas.sumBy {
                it.recoveries
            }.takeUnless {
                it == 0
            } ?: -1
        }

        fun deltasFrom(timestampedData: TimestampedData): TimestampedData {
            return TimestampedData(
                areas = this.areas.map { currentArea ->
                    currentArea to (timestampedData.areas.find { oldArea ->
                        oldArea.name == currentArea.name
                    } ?: Stats(0, 0, 0))
                }.map { pair ->
                    Area(
                        name = pair.first.name,
                        realName = pair.first.realName,
                        areas = listOf(),
                        stats = Stats(
                            pair.first.confirmed - pair.second.confirmed,
                            pair.first.deaths - pair.second.deaths,
                            pair.first.recoveries - pair.second.recoveries
                        )
                    )
                }
            )
        }
    }

    data class Area(
        val name: String = "N/A",
        @SerializedName("real_name")
        val realName: String = "N/A",
        val areas: List<Area> = listOf(),
        val stats: Stats? = null
    ) : IStats {
        @delegate:Transient
        val flag: String? by lazy {
            localeMap[name]?.flagEmoji
        }

        @delegate:Transient
        override val confirmed: Int by lazy {
            stats?.confirmed?.takeIf {
                it != -1
            } ?: areas.sumBy { it.confirmed }
        }

        @delegate:Transient
        override val active: Int by lazy {
            stats?.active?.takeIf {
                it != -1
            } ?: areas.sumBy { it.active }
        }

        @delegate:Transient
        override val deaths: Int by lazy {
            stats?.deaths?.takeIf {
                it != -1
            } ?: areas.sumBy { it.deaths }
        }

        @delegate:Transient
        override val recoveries: Int by lazy {
            stats?.recoveries?.takeIf {
                it != -1
            } ?: areas.sumBy { it.recoveries }
        }
    }

    interface IStats {
        val confirmed: Int
        val confirmedStr: String
            get() = confirmed.takeUnless { it == -1 }?.toString() ?: "N/A"
        val confirmedSignedStr: String
            get() = confirmed.takeUnless { it == -1 }?.let {
                if (it >= 0) {
                    "+$it"
                } else {
                    "$it"
                }
            } ?: "N/A"

        val active: Int
            get() = (confirmed - deaths - recoveries)
        val activeStr: String
            get() = active.takeUnless { it == -1 }?.toString() ?: "N/A"
        val activeSignedStr: String
            get() = active.takeUnless { it == -1 }?.let {
                if (it >= 0) {
                    "+$it"
                } else {
                    "$it"
                }
            } ?: "N/A"

        val deaths: Int
        val deathsStr: String
            get() = deaths.takeUnless { it == -1 }?.toString() ?: "N/A"
        val deathsSignedStr: String
            get() = deaths.takeUnless { it == -1 }?.let {
                if (it >= 0) {
                    "+$it"
                } else {
                    "$it"
                }
            } ?: "N/A"

        val recoveries: Int
        val recoveriesStr: String
            get() = recoveries.takeUnless { it == -1 }?.toString() ?: "N/A"
        val recoveriesSignedStr: String
            get() = recoveries.takeUnless { it == -1 }?.let {
                if (it >= 0) {
                    "+$it"
                } else {
                    "$it"
                }
            } ?: "N/A"
    }

    open class Stats(
        @SerializedName("confirmed")
        val confirmed_internal: Int? = null,
        @SerializedName("deaths")
        val deaths_internal: Int? = null,
        @SerializedName("recoveries")
        val recoveries_internal: Int? = null
    ) : IStats {
        @delegate:Transient
        override val confirmed: Int by lazy {
            confirmed_internal ?: -1
        }
        @delegate:Transient
        override val deaths: Int by lazy {
            deaths_internal ?: -1
        }
        @delegate:Transient
        override val recoveries: Int by lazy {
            recoveries_internal ?: -1
        }
    }

    companion object {
        val localeMap: Map<String, Locale> = Locale.getAvailableLocales().map {
            it.getDisplayCountry(Locale.ENGLISH) to it
        }.toMap().toMutableMap()
    }
}