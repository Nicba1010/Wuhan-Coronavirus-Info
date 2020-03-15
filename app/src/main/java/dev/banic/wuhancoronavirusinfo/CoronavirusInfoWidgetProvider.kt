package dev.banic.wuhancoronavirusinfo

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
        setCounterVisibilities(views, context, appWidgetId)

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
        showProgressBars(context, views, appWidgetId, true)
        appWidgetManager.updateAppWidget(appWidgetId, views)

        getDataSource(context, appWidgetId)
            .enqueue(object : Callback<Disease> {
                override fun onFailure(call: Call<Disease>, t: Throwable) {
                    setAllTextViewError(context, views, appWidgetId)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    call.cancel()
                    Log.e("Retrofit", "getData()", t)
                }

                override fun onResponse(
                    call: Call<Disease>,
                    response: Response<Disease>
                ) {
                    response.body()?.apply {
                        this.timestampedData.sortByDescending { it.date }
                    }?.timestampedData?.let { data ->
                        updateCountTextViews(context, views, appWidgetId, data)
                        updateInfectedCountriesTextViews(data, context, views, appWidgetId)

                        views.setTextViewText(
                            R.id.tv_last_updated,
                            context.getString(R.string.last_updated_at, getLocalTime())
                        )
                    } ?: views.setTextViewText(
                        R.id.tv_last_updated,
                        context.getString(R.string.error_occurred)
                    )

                    showProgressBars(context, views, appWidgetId, false)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            })
    }

    private fun setCounterVisibilities(views: RemoteViews, context: Context, appWidgetId: Int) {
        if (!getConfirmedCasesVisible(context, appWidgetId)) {
            views.setViewVisibility(R.id.tv_confirmed_label, View.GONE)
            views.setViewVisibility(R.id.pb_confirmed_cases, View.GONE)
            views.setViewVisibility(R.id.ll_confirmed, View.GONE)
        } else {
            views.setViewVisibility(R.id.tv_confirmed_label, View.VISIBLE)
        }

        if (!getPickedCountryActiveCasesVisible(context, appWidgetId)) {
            views.setViewVisibility(R.id.tv_active_secondary_label, View.GONE)
            views.setViewVisibility(R.id.pb_active_secondary_cases, View.GONE)
            views.setViewVisibility(R.id.ll_active_secondary, View.GONE)
        } else {
            views.setViewVisibility(R.id.tv_active_secondary_label, View.VISIBLE)
        }
    }

    private fun setAllTextViewError(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int
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

        showProgressBars(context, views, appWidgetId, false)
        arrayOf(
            R.id.tv_active,
            R.id.tv_active_secondary,
            R.id.tv_deaths,
            R.id.tv_recoveries,
            R.id.tv_active_delta,
            R.id.tv_active_secondary_delta,
            R.id.tv_deaths_delta,
            R.id.tv_recoveries_delta
        ).forEach {
            views.setTextViewText(it, context.getString(R.string.error))
        }
    }

    private fun updateInfectedCountriesTextViews(
        data: List<Disease.TimestampedData>,
        context: Context,
        views: RemoteViews,
        appWidgetId: Int
    ) {
        val latestData: Disease.TimestampedData = data.firstOrNull() ?: Disease.TimestampedData()
        val flagMode: Boolean = getFlagMode(context, appWidgetId)
        val infectedCountries = latestData.areas.filter {
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
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        data: List<Disease.TimestampedData>
    ) {
        val latestData: Disease.TimestampedData = data.firstOrNull() ?: Disease.TimestampedData()
        val deltaData: Disease.TimestampedData = latestData.deltasFrom(
            data.getOrNull(1) ?: Disease.TimestampedData()
        )

        views.setTextViewText(R.id.tv_confirmed, latestData.confirmedStr)
        views.setTextViewText(R.id.tv_active, latestData.activeStr)
        views.setTextViewText(R.id.tv_deaths, latestData.deathsStr)
        views.setTextViewText(R.id.tv_recoveries, latestData.recoveriesStr)

        views.setTextViewText(
            R.id.tv_confirmed_delta,
            deltaData.confirmedSignedStr.let {
                context.getString(R.string.delta_amount, it)
            }
        )
        views.setTextViewText(
            R.id.tv_active_delta,
            deltaData.activeSignedStr.let {
                context.getString(R.string.delta_amount, it)
            }
        )
        views.setTextViewText(
            R.id.tv_deaths_delta,
            deltaData.deathsSignedStr.let {
                context.getString(R.string.delta_amount, it)
            }
        )
        views.setTextViewText(
            R.id.tv_recoveries_delta,
            deltaData.recoveriesSignedStr.let {
                context.getString(R.string.delta_amount, it)
            }
        )


        val pickedCountry = getPickedCountry(context, appWidgetId)
        val pickedArea = latestData.areas.find { it.name == pickedCountry }
        val pickedAreaDelta = deltaData.areas.find { it.name == pickedCountry }

        views.setTextViewText(
            R.id.tv_active_secondary_label,
            context.getString(
                R.string.active_secondary, pickedCountry
            )
        )
        views.setTextViewText(R.id.tv_active_secondary, pickedArea?.activeStr ?: "N/A")
        views.setTextViewText(
            R.id.tv_active_secondary_delta,
            deltaData.activeSignedStr.let {
                context.getString(
                    R.string.delta_amount,
                    pickedAreaDelta?.activeSignedStr ?: "N/A"
                )
            }
        )
    }

    fun showProgressBars(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        show: Boolean,
        tvs: MutableList<Int> = mutableListOf(
            R.id.ll_confirmed,
            R.id.ll_active,
            R.id.ll_active_secondary,
            R.id.ll_deaths,
            R.id.ll_recoveries
        ),
        pbs: MutableList<Int> = mutableListOf(
            R.id.pb_confirmed_cases,
            R.id.pb_active_cases,
            R.id.pb_active_secondary_cases,
            R.id.pb_deaths,
            R.id.pb_recoveries
        )
    ) {
        if (!getConfirmedCasesVisible(context, appWidgetId)) {
            tvs.remove(R.id.ll_confirmed)
            pbs.remove(R.id.pb_confirmed_cases)
        }
        if (!getPickedCountryActiveCasesVisible(context, appWidgetId)) {
            tvs.remove(R.id.ll_active_secondary)
            pbs.remove(R.id.pb_active_secondary_cases)
        }


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

        fun getConfirmedCasesVisible(context: Context, appWidgetId: Int): Boolean {
            val sp: SharedPreferences = context.getSharedPreferences(appWidgetId)

            return sp.getBoolean(
                context.getString(R.string.preference_key_confirmed_cases_visible),
                true
            )
        }

        fun getPickedCountryActiveCasesVisible(context: Context, appWidgetId: Int): Boolean {
            val sp: SharedPreferences = context.getSharedPreferences(appWidgetId)

            return sp.getBoolean(
                context.getString(R.string.preference_key_picked_country_active_cases_visible),
                false
            )
        }

        fun getDataSource(context: Context, appWidgetId: Int): Call<Disease> {
            val sp: SharedPreferences = context.getSharedPreferences(appWidgetId)

            return when (sp.getString(
                context.getString(R.string.preference_key_data_source),
                null
            )) {
                "jhcsse" -> WufluService.instance.getJohnHopkinsCSSEData()
                else -> WufluService.instance.getJohnHopkinsCSSEData()
            }
        }

        fun getPickedCountry(context: Context, appWidgetId: Int): String {
            val sp: SharedPreferences = context.getSharedPreferences(appWidgetId)

            return sp.getString(
                context.getString(R.string.preference_key_picked_country),
                null
            ) ?: Locale.getDefault().country
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