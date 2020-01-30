package hr.banic.wuhancoronavirusinfo

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

            val views = RemoteViews(
                context.packageName,
                R.layout.coronavirus_info_appwidget
            )

            views.setOnClickPendingIntent(R.id.ll_root, pendingRefreshIntent)
            views.setTextViewText(
                R.id.tv_last_updated,
                context.getString(R.string.updating)
            )
            views.setTextViewText(
                R.id.tv_currently_infected_countries,
                context.getString(R.string.currently_infected_countries_updating)
            )
            showProgressBars(views, true)
            appWidgetManager.updateAppWidget(appWidgetId, views)

            QQWuhanCoronavirusService.instance
                .getDiseaseH5()
                .enqueue(object : Callback<QQDiseaseH5> {
                    override fun onFailure(call: Call<QQDiseaseH5>, t: Throwable) {
                        views.setTextViewText(
                            R.id.tv_last_updated,
                            context.getString(R.string.error_occurred)
                        )
                        views.setTextViewText(
                            R.id.tv_currently_infected_countries,
                            context.getString(R.string.currently_infected_countries_error)
                        )

                        showProgressBars(views, false)
                        arrayOf(
                            R.id.tv_confirmed_cases,
                            R.id.tv_suspected_cases,
                            R.id.tv_deaths,
                            R.id.tv_recoveries
                        ).forEach {
                            views.setTextViewText(it, "Error")
                        }
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                        call.cancel()
                        Log.e("Retrofit", "getQQDiseaseH5()", t)
                    }

                    override fun onResponse(
                        call: Call<QQDiseaseH5>,
                        response: Response<QQDiseaseH5>
                    ) {
                        response.body()?.disease?.let { disease ->
                            views.setTextViewText(
                                R.id.tv_confirmed_cases,
                                disease.global.confirm.toString()
                            )
                            views.setTextViewText(
                                R.id.tv_suspected_cases,
                                disease.global.suspect.toString()
                            )
                            views.setTextViewText(
                                R.id.tv_deaths,
                                disease.global.dead.toString()
                            )
                            views.setTextViewText(
                                R.id.tv_recoveries,
                                disease.global.heal.toString()
                            )

                            views.setTextViewText(
                                R.id.tv_currently_infected_countries,
                                context.getString(
                                    R.string.currently_infected_countries,
                                    disease.areaTree.filter {
                                        it.total.confirm > 0
                                    }.sortedByDescending {
                                        it.total.confirm
                                    }.joinToString(", ") {
                                        context.getString(
                                            R.string.currently_infected_country_detail,
                                            it.name,
                                            it.total.confirm
                                        )
                                    }
                                )
                            )
                            views.setTextViewText(
                                R.id.tv_last_updated,
                                context.getString(R.string.last_updated_at, getLocalTime())
                            )
                        } ?: views.setTextViewText(
                            R.id.tv_last_updated,
                            context.getString(R.string.error_occurred)
                        )

                        showProgressBars(views, false)
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
}