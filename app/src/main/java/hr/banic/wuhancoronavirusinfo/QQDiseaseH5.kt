package hr.banic.wuhancoronavirusinfo

import android.annotation.SuppressLint
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.util.*

data class QQDiseaseH5(
    val ret: Int = -1,
    val data: String = ""
) {

    @delegate:Transient
    val disease: Disease by lazy {
        gson.fromJson(data, Disease::class.java)
    }

    data class Disease(
        val chinaTotal: Daily = Daily("", "", "", "", ""),
        val lastUpdateTime: String = "",
        val areaTree: List<Area> = listOf(),
        val chinaDayList: List<Daily> = listOf()
    ) {
        @delegate:Transient
        val global: Data by lazy {
            areaTree.filter {
                it.name != "China"
            }.fold(Data(0, 0, 0, 0)) { data, area ->
                data.apply {
                    confirm += area.total.confirm
                    suspect += area.total.suspect
                    dead += area.total.dead
                    heal += area.total.heal
                }
            }.apply {
                confirm += chinaTotal.confirm
                suspect += chinaTotal.suspect
                dead += chinaTotal.dead
                heal += chinaTotal.heal
            }
        }

        data class Daily(
            val date: String = "null",
            @SerializedName("confirm")
            val internalConfirm: String = "-1",
            @SerializedName("suspect")
            val internalSuspect: String = "-1",
            @SerializedName("dead")
            val internalDead: String = "-1",
            @SerializedName("heal")
            val internalHeal: String = "-1"
        ) {
            val confirm: Int by lazy {
                internalConfirm.toInt()
            }
            val suspect: Int by lazy {
                internalSuspect.toInt()
            }
            val dead: Int by lazy {
                internalDead.toInt()
            }
            val heal: Int by lazy {
                internalHeal.toInt()
            }
        }

        data class Area(
            @SerializedName("name")
            val internalName: String = "null",
            val total: Data = Data(),
            val today: Data = Data()
        ) {
            @delegate:Transient
            val name: String by lazy {
                translationMap[internalName] ?: internalName
            }
        }

        data class Data(
            var confirm: Int = -1,
            var suspect: Int = -1,
            var dead: Int = -1,
            var heal: Int = -1
        )
    }

    companion object {
        val gson: Gson = Gson()

        @SuppressLint("ConstantLocale")
        val translationMap: Map<String, String> = Locale.getAvailableLocales().map {
            it.getDisplayCountry(Locale.SIMPLIFIED_CHINESE) to it.getDisplayCountry(Locale.getDefault())
        }.toMap().toMutableMap().apply {
            // Replace UAE with proper translation
            this.remove("阿拉伯联合酋长国")
            this["阿联酋"] = "United Arab Emirates"
        }.also { map ->
            map.forEach {
                Log.d("Corona", "${it.key} => ${it.value}")
            }
        }
    }
}