package hr.banic.wuhancoronavirusinfo

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*


class CoronavirusInfoWidgetProvider : AppWidgetProvider() {


    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        chineseToDefaultCountryNameMap.forEach {
            Log.d(this.javaClass.toString(), it.key + " -> " + it.value)
        }

        appWidgetIds.forEach { appWidgetId ->
            val intentSync = Intent(context, CoronavirusInfoWidgetProvider::class.java)
            intentSync.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            intentSync.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)

            val pendingRefreshIntent = PendingIntent.getBroadcast(
                context,
                0,
                intentSync,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            val views: RemoteViews = RemoteViews(
                context.packageName,
                R.layout.coronavirus_info_appwidget
            )

            views.setOnClickPendingIntent(R.id.ll_root, pendingRefreshIntent)
            views.setTextViewText(
                R.id.tv_last_updated,
                "Updating..."
            )
            showProgressBars(views, true)
            appWidgetManager.updateAppWidget(appWidgetId, views)

            QQWuhanCoronavirusService.instance
                .getWuweiWWAreaCounts()
                .enqueue(object : Callback<WuweiWWAreaCountsResponse> {
                    override fun onFailure(call: Call<WuweiWWAreaCountsResponse>, t: Throwable) {
                        views.setTextViewText(
                            R.id.tv_last_updated,
                            "An error occurred... (click to refresh)"
                        )

                        showProgressBars(
                            views,
                            false,
                            arrayOf(
                                R.id.tv_confirmed_cases,
//                                R.id.tv_suspected_cases,
                                // TODO: If they fix API reenable this
                                R.id.tv_deaths,
                                R.id.tv_recoveries
                            ),
                            arrayOf(
                                R.id.pb_confirmed_cases,
//                                R.id.pb_suspected_cases,
                                // TODO: If they fix API reenable this
                                R.id.pb_deaths,
                                R.id.pb_recoveries
                            )
                        )
                        arrayOf(
                            R.id.tv_confirmed_cases,
//                            R.id.tv_suspected_cases,
                            // TODO: If they fix API reenable this
                            R.id.tv_deaths,
                            R.id.tv_recoveries
                        ).forEach {
                            views.setTextViewText(it, "Error")
                        }
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                        call.cancel()
                        Log.e("Retrofit", "getWuweiWWAreaCounts()", t)
                    }

                    override fun onResponse(
                        call: Call<WuweiWWAreaCountsResponse>,
                        response: Response<WuweiWWAreaCountsResponse>
                    ) {
                        response.body()?.stats?.let { statsList ->
                            views.setTextViewText(
                                R.id.tv_confirmed_cases,
                                statsList.sumBy { it.confirm }.toString()
                            )
//                            views.setTextViewText(
//                                R.id.tv_suspected_cases,
//                                statsList.sumBy { it.suspect }.toString()
//                            )
                            // TODO: If they fix API reenable this
                            views.setTextViewText(
                                R.id.tv_deaths,
                                statsList.sumBy { it.dead }.toString()
                            )
                            views.setTextViewText(
                                R.id.tv_recoveries,
                                statsList.sumBy { it.heal }.toString()
                            )
                            views.setTextViewText(
                                R.id.tv_currently_infected_countries,
                                "Currently infected countries: " + statsList.distinctBy { stats ->
                                    stats.country
                                }.map { stats ->
                                    stats.country to statsList.filter {
                                        it.country == stats.country
                                    }.sumBy {
                                        it.confirm
                                    }
                                }.filter {
                                    it.second > 0
                                }.sortedByDescending {
                                    it.second
                                }.joinToString(", ") {
                                    (chineseToDefaultCountryNameMap[it.first] ?: it.first) +
                                            " (${it.second})"
                                }
                            )
                            views.setTextViewText(
                                R.id.tv_last_updated,
                                "Last updated at ${getLocalTime()} (click to refresh)"
                            )
                        } ?: views.setTextViewText(
                            R.id.tv_last_updated,
                            "An error occurred... (click to refresh)"
                        )

                        showProgressBars(
                            views,
                            false,
                            arrayOf(
                                R.id.tv_confirmed_cases,
//                                R.id.tv_suspected_cases,
                                // TODO: If they fix API reenable this
                                R.id.tv_deaths,
                                R.id.tv_recoveries
                            ),
                            arrayOf(
                                R.id.pb_confirmed_cases,
//                                R.id.pb_suspected_cases,
                                // TODO: If they fix API reenable this
                                R.id.pb_deaths,
                                R.id.pb_recoveries
                            )
                        )
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                })
            QQWuhanCoronavirusService.instance
                .getWuweiWWGlobalVars()
                .enqueue(object : Callback<WuweiWWGlobalVars> {
                    override fun onFailure(call: Call<WuweiWWGlobalVars>, t: Throwable) {
                        views.setTextViewText(
                            R.id.tv_last_updated,
                            "An error occurred... (click to refresh)"
                        )
                        showProgressBars(
                            views,
                            false,
                            arrayOf(R.id.tv_suspected_cases),
                            arrayOf(R.id.pb_suspected_cases)
                        )
                        arrayOf(
                            R.id.tv_suspected_cases
                        ).forEach {
                            views.setTextViewText(it, "Error")
                        }
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                        call.cancel()
                        Log.e("Retrofit", "getWuweiWWGlobalVars()", t)
                    }

                    override fun onResponse(
                        call: Call<WuweiWWGlobalVars>,
                        response: Response<WuweiWWGlobalVars>
                    ) {
                        response.body()?.stats?.let { stats ->
                            views.setTextViewText(
                                R.id.tv_suspected_cases,
                                "~${stats.suspectCount}"
                            )
                        } ?: views.setTextViewText(
                            R.id.tv_last_updated,
                            "An error occurred... (click to refresh)"
                        )
                        showProgressBars(
                            views,
                            false,
                            arrayOf(R.id.tv_suspected_cases),
                            arrayOf(R.id.pb_suspected_cases)
                        )
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                })
        }
    }

    fun showProgressBars(
        views: RemoteViews,
        show: Boolean,
        tvs: Array<Int> = arrayOf(
            R.id.tv_confirmed_cases,
            R.id.tv_suspected_cases,
            R.id.tv_deaths,
            R.id.tv_recoveries
        ),
        pbs: Array<Int> = arrayOf(
            R.id.pb_confirmed_cases,
            R.id.pb_suspected_cases,
            R.id.pb_deaths,
            R.id.pb_recoveries
        )
    ) {
        tvs.forEach {
            views.setViewVisibility(it, if (show) View.GONE else View.VISIBLE)
        }
        pbs.forEach {
            views.setViewVisibility(it, if (show) View.VISIBLE else View.GONE)
        }
    }

    private fun getLocalTime(): String {
        return SimpleDateFormat(
            "HH:mm:ss",
            Locale.getDefault()
        ).format(Date())
    }

    companion object {
        @SuppressLint("ConstantLocale")
        val chineseToDefaultCountryNameMap: Map<String, String> = Locale.getAvailableLocales().map {
            it.getDisplayCountry(Locale.SIMPLIFIED_CHINESE) to it.getDisplayCountry(Locale.getDefault())
        }.toMap()
    }
}