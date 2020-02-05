package hr.banic.wuhancoronavirusinfo

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import androidx.preference.ListPreference

class LayoutListPreference : ListPreference {
    private var realValues: IntArray
    private var valueSet: Boolean = false
    private var realValue: Int

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        realValues = context.resources.obtainTypedArray(R.array.theme_values).let { array ->
            (0 until array.length()).map { index ->
                array.getResourceId(index, -1)
            }
        }.toIntArray()
        realValue = realValues[0]
    }

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int
    ) : this(context, attrs, defStyleAttr, 0)

    // TODO: Evade asserting
    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs!!,
        getAttr(
            context, androidx.preference.R.attr.dialogPreferenceStyle,
            android.R.attr.dialogPreferenceStyle
        )
    )

    constructor(context: Context) : this(context, null)

    override fun getValue(): String {
        return (entryValues[realValues.indexOf(realValue)] ?: entryValues[0]).toString()
    }

    override fun getEntry(): CharSequence? {
        val index = findIndexOfValue(value)
        return if (index >= 0 && entries != null) entries[index] else null
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        setValue(getPersistedInt((defaultValue as? String)?.toInt() ?: -1))
    }

    override fun setValue(value: String) { // Always persist/notify the first time.
        val realValue = realValues[entryValues.indexOf(value)]

        val changed = this.realValue != realValue
        if (changed || !valueSet) {
            this.realValue = realValue
            this.valueSet = true

            persistInt(realValue)

            if (changed) {
                notifyChanged()
            }
        }
    }

    fun setValue(realValue: Int) { // Always persist/notify the first time.
        if(realValue !in realValues) {
            realValues[0]
        } else {
            realValue
        }.let { value ->
            val changed = this.realValue != value
            if (changed || !valueSet) {
                this.realValue = value
                this.valueSet = true

                persistInt(value)

                if (changed) {
                    notifyChanged()
                }
            }
        }
    }

    companion object {
        fun getAttr(context: Context, attr: Int, fallbackAttr: Int): Int {
            val value = TypedValue()
            context.theme.resolveAttribute(attr, value, true)
            return if (value.resourceId != 0) {
                attr
            } else fallbackAttr
        }
    }
}