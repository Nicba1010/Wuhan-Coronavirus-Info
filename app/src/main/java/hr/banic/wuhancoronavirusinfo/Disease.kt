package hr.banic.wuhancoronavirusinfo

import com.google.gson.annotations.SerializedName
import java.util.*

data class Disease(
    val timestampedData: List<TimestampedData> = listOf()
) {
    data class TimestampedData(
        val date: Date = Calendar.getInstance().time,
        val areas: List<Area> = listOf(),
        @SerializedName("approximate_stats")
        override val approximateStats: Stats? = null
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
        override val suspected: Int by lazy {
            areas.sumBy {
                it.suspected
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
        override val suspected: Int by lazy {
            stats?.suspected?.takeIf {
                it != -1
            } ?: areas.sumBy { it.suspected }
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
        val approximateStats: Stats?
            get() = null

        val confirmed: Int
        val confirmedStr: String
            get() = confirmed.takeUnless { it == -1 }?.toString()
                ?: approximateStats?.confirmed?.takeUnless {
                    it == -1
                }?.let { "~${it}" }
                ?: "N/A"

        val suspected: Int
        val suspectedStr: String
            get() = suspected.takeUnless { it == -1 }?.toString()
                ?: approximateStats?.suspected?.takeUnless {
                    it == -1
                }?.let { "~${it}" }
                ?: "N/A"

        val deaths: Int
        val deathsStr: String
            get() = deaths.takeUnless { it == -1 }?.toString()
                ?: approximateStats?.deaths?.takeUnless {
                    it == -1
                }?.let { "~${it}" }
                ?: "N/A"

        val recoveries: Int
        val recoveriesStr: String
            get() = recoveries.takeUnless { it == -1 }?.toString()
                ?: approximateStats?.recoveries?.takeUnless {
                    it == -1
                }?.let { "~${it}" }
                ?: "N/A"
    }

    class Stats(
        @SerializedName("deltas")
        val deltas: Values? = null
    ) : Values()

    open class Values(
        @SerializedName("confirmed")
        val confirmed_internal: Int? = null,
        @SerializedName("suspected")
        val suspected_internal: Int? = null,
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
        override val suspected: Int by lazy {
            suspected_internal ?: -1
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