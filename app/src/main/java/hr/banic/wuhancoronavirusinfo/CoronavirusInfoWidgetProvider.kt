package hr.banic.wuhancoronavirusinfo

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.StrictMode
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*


class CoronavirusInfoWidgetProvider : AppWidgetProvider() {
    private val client = OkHttpClient()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()

        StrictMode.setThreadPolicy(policy)

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
            views.setTextViewText(R.id.tv_last_updated, "Updating...")
            appWidgetManager.updateAppWidget(appWidgetId, views)

            val request: Request = Request.Builder()
                .url(DATA_SOURCE)
                .build()

            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date());
            client.newCall(request).execute().use { response ->
                response.body?.string()?.let { data ->
                    Jsoup.parse(data).let { soup ->
                        val spans = soup.select(".content___2hIPS").select("span")
                        views.setTextViewText(R.id.tv_confirmed_cases, spans[1].text())
                        views.setTextViewText(R.id.tv_suspected_cases, spans[2].text())
                        views.setTextViewText(R.id.tv_deaths, spans[3].text())
                        views.setTextViewText(R.id.tv_recoveries, spans[4].text())
                        views.setTextViewText(R.id.tv_last_updated, "Last updated at $time")
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            }
        }
    }


    companion object {
        const val DATA_SOURCE = "https://3g.dxy.cn/newh5/view/pneumonia?scene=2" +
                "&clicktime=1579582238&enterid=1579582238&from=singlemessage&isappinstalled=0"
    }

}