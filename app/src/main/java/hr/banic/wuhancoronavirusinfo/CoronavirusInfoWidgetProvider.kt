package hr.banic.wuhancoronavirusinfo

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*


class CoronavirusInfoWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action !in listOf(
                ACTION_INFECTED_COUNTRIES_EXPAND,
                ACTION_INFECTED_COUNTRIES_COLLAPSE
            )
        ) {
            return
        }

        val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
        intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)?.forEach { appWidgetId ->
            val views = RemoteViews(
                context.packageName,
                getTheme(context, appWidgetId)
            )

            when (intent.action) {
                ACTION_INFECTED_COUNTRIES_EXPAND -> expandInfectedCountries(views)
                ACTION_INFECTED_COUNTRIES_COLLAPSE -> collapseInfectedCountries(views)
                else -> return@forEach
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(
                context.packageName,
                getTheme(context, appWidgetId)
            )

            val configureIntent = getConfigureIntent(context, appWidgetId)
            val refreshIntent = getRefreshIntent(context, appWidgetId)
            val countriesExpandIntent = getViewUpdateIntent(
                context, appWidgetId, ACTION_INFECTED_COUNTRIES_EXPAND
            )
            val countriesCollapseIntent = getViewUpdateIntent(
                context, appWidgetId, ACTION_INFECTED_COUNTRIES_COLLAPSE
            )
            mapOf(
                Pair(R.id.ll_root, configureIntent),
                Pair(R.id.tv_last_updated, refreshIntent),
                Pair(R.id.tv_currently_infected_countries_collapsed, countriesExpandIntent),
                Pair(R.id.tv_currently_infected_countries_expanded, countriesCollapseIntent)
            ).forEach {
                views.setOnClickPendingIntent(it.key, it.value)
            }

            updateWidgetData(views, context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidgetData(
        views: RemoteViews,
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        views.setTextViewText(
            R.id.tv_last_updated,
            context.getString(R.string.updating)
        )
        listOf(
            R.id.tv_currently_infected_countries_collapsed,
            R.id.tv_currently_infected_countries_expanded
        ).forEach {
            views.setTextViewText(
                it,
                context.getString(R.string.currently_infected_countries_updating)
            )
        }
        showProgressBars(views, true)
        appWidgetManager.updateAppWidget(appWidgetId, views)

        WufluService.instance
            .getQQData()
            .enqueue(object : Callback<Disease> {
                override fun onFailure(call: Call<Disease>, t: Throwable) {
                    setAllTextViewError(views, context)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    call.cancel()
                    Log.e("Retrofit", "getData()", t)
                }

                override fun onResponse(
                    call: Call<Disease>,
                    response: Response<Disease>
                ) {
                    response.body()?.timestampedData?.maxBy { it.date }?.let { data ->
                        updateCountTextViews(views, data, context)
                        updateInfectedCountriesTextViews(data, context, views, appWidgetId)

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

    private fun setAllTextViewError(
        views: RemoteViews,
        context: Context
    ) {
        views.setTextViewText(
            R.id.tv_last_updated,
            context.getString(R.string.error_occurred)
        )
        listOf(
            R.id.tv_currently_infected_countries_collapsed,
            R.id.tv_currently_infected_countries_expanded
        ).forEach {
            views.setTextViewText(
                it,
                context.getString(R.string.currently_infected_countries_error)
            )
        }

        showProgressBars(views, false)
        arrayOf(
            R.id.tv_confirmed,
            R.id.tv_suspected,
            R.id.tv_deaths,
            R.id.tv_recoveries,
            R.id.tv_confirmed_delta,
            R.id.tv_suspected_delta,
            R.id.tv_deaths_delta,
            R.id.tv_recoveries_delta
        ).forEach {
            views.setTextViewText(it, context.getString(R.string.error))
        }
    }

    private fun updateInfectedCountriesTextViews(
        data: Disease.TimestampedData,
        context: Context,
        views: RemoteViews,
        appWidgetId: Int
    ) {
        val flagMode: Boolean = getFlagMode(context, appWidgetId)
        val infectedCountries = data.areas.filter {
            it.confirmed > 0
        }.sortedByDescending {
            it.confirmed
        }.joinToString(", ") {
            context.getString(
                R.string.currently_infected_country_detail,
                if (flagMode) it.flag else it.name,
                it.confirmed
            )
        }
        views.setTextViewText(
            R.id.tv_currently_infected_countries_collapsed,
            context.getString(
                R.string.currently_infected_countries_expand,
                infectedCountries
            )
        )
        views.setTextViewText(
            R.id.tv_currently_infected_countries_expanded,
            context.getString(
                R.string.currently_infected_countries_collapse,
                infectedCountries
            )
        )
    }

    private fun updateCountTextViews(
        views: RemoteViews,
        data: Disease.TimestampedData,
        context: Context
    ) {
        views.setTextViewText(R.id.tv_confirmed, data.confirmedStr)
        views.setTextViewText(R.id.tv_suspected, data.suspectedStr)
        views.setTextViewText(R.id.tv_deaths, data.deathsStr)
        views.setTextViewText(R.id.tv_recoveries, data.recoveriesStr)
        views.setTextViewText(
            R.id.tv_confirmed_delta,
            context.getString(
                R.string.delta_amount,
                data.approximateStats?.deltas?.confirmed ?: -1
            )
        )
        views.setTextViewText(
            R.id.tv_suspected_delta,
            context.getString(
                R.string.delta_amount,
                data.approximateStats?.deltas?.suspected ?: -1
            )
        )
        views.setTextViewText(
            R.id.tv_deaths_delta,
            context.getString(
                R.string.delta_amount,
                data.approximateStats?.deltas?.deaths ?: -1
            )
        )
        views.setTextViewText(
            R.id.tv_recoveries_delta,
            context.getString(
                R.string.delta_amount,
                data.approximateStats?.deltas?.recoveries ?: -1
            )
        )
    }

    fun showProgressBars(
        views: RemoteViews,
        show: Boolean,
        tvs: Array<Int> = arrayOf(
            R.id.ll_confirmed,
            R.id.ll_suspected,
            R.id.ll_deaths,
            R.id.ll_recoveries
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

    private fun collapseInfectedCountries(views: RemoteViews) {
        views.setViewVisibility(
            R.id.tv_currently_infected_countries_collapsed,
            View.VISIBLE
        )
        views.setViewVisibility(
            R.id.tv_currently_infected_countries_expanded,
            View.GONE
        )
    }

    private fun expandInfectedCountries(views: RemoteViews) {
        views.setViewVisibility(
            R.id.tv_currently_infected_countries_collapsed,
            View.GONE
        )
        views.setViewVisibility(
            R.id.tv_currently_infected_countries_expanded,
            View.VISIBLE
        )
    }

    companion object {
        const val ACTION_INFECTED_COUNTRIES_COLLAPSE =
            "hr.banic.wuhancoronavirusinfo.ACTION_INFECTED_COUNTRIES_COLLAPSE"
        const val ACTION_INFECTED_COUNTRIES_EXPAND =
            "hr.banic.wuhancoronavirusinfo.ACTION_INFECTED_COUNTRIES_EXPAND"

        fun getTheme(context: Context, appWidgetId: Int): Int {
            val sp: SharedPreferences = context.getSharedPreferences(appWidgetId)

            return sp.getInt(
                context.getString(R.string.preference_key_theme),
                R.layout.coronavirus_info_appwidget_dark
            )
        }

        fun getFlagMode(context: Context, appWidgetId: Int): Boolean {
            val sp: SharedPreferences = context.getSharedPreferences(appWidgetId)

            return sp.getBoolean(
                context.getString(R.string.preference_key_flag_mode),
                false
            )
        }

        private fun getConfigureIntent(context: Context, appWidgetId: Int): PendingIntent {
            return PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 1,
                Intent(context, CoronavirusInfoAppWidgetConfigureActivity::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                },
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        private fun getRefreshIntent(
            context: Context,
            appWidgetIds: IntArray,
            requestCode: Int = 0
        ): PendingIntent {
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, CoronavirusInfoWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                },
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        private fun getRefreshIntent(context: Context, appWidgetId: Int): PendingIntent {
            return getRefreshIntent(context, intArrayOf(appWidgetId), appWidgetId * 10 + 1)
        }

        private fun getViewUpdateIntent(
            context: Context,
            appWidgetIds: IntArray,
            action: String,
            requestCode: Int = 0
        ): PendingIntent {
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, CoronavirusInfoWidgetProvider::class.java).apply {
                    setAction(action)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                },
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        private fun getViewUpdateIntent(
            context: Context,
            appWidgetId: Int,
            action: String
        ): PendingIntent {
            return getViewUpdateIntent(
                context,
                intArrayOf(appWidgetId),
                action,
                appWidgetId * 10 + 1
            )
        }

        private fun getLocalTime(): String {
            return SimpleDateFormat(
                "HH:mm:ss",
                Locale.getDefault()
            ).format(Date())
        }
    }
}