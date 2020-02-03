package hr.banic.wuhancoronavirusinfo

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.android.synthetic.main.settings_activity.*


class CoronavirusInfoAppWidgetConfigureActivity : AppCompatActivity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            TODO("Alert user")
            finish()
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment().apply {
                arguments = Bundle().apply {
                    putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
            })
            .commit()
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        btn_save.setOnClickListener {
            PendingIntent.getBroadcast(
                applicationContext,
                appWidgetId * 10 + 1,
                Intent(applicationContext, CoronavirusInfoWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
                },
                PendingIntent.FLAG_UPDATE_CURRENT
            ).send()

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

        override fun onCreate(savedInstanceState: Bundle?) {
            appWidgetId = arguments?.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                TODO("Alert user")
                requireActivity().finish()
            }

            super.onCreate(savedInstanceState)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.sharedPreferencesName = requireContext().getSharedPreferencesName(
                appWidgetId
            )

            setPreferencesFromResource(R.xml.widget_preferences, rootKey)

            preferenceManager.findPreference<ListPreference>(
                getString(R.string.preference_key_theme)
            )?.setOnPreferenceChangeListener { preference, newValue ->
                (preference as ListPreference).entryValues.indexOf(newValue).let { index ->
                    val layouts = resources.obtainTypedArray(R.array.theme_values)
                    val value = layouts.getResourceId(index, -1)
                    layouts.recycle()

                    preference.preferenceDataStore?.let { dataStore ->
                        dataStore.putInt(preference.key, value)
                    } ?: preferenceManager.sharedPreferences.edit().apply {
                        putInt(preference.key, value)
                    }.apply()
                }
                true
            }
        }
    }
}