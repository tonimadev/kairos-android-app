package digital.tonima.core.viewmodel

import android.content.Context
import androidx.annotation.StringRes

/**
 * Abstraction that allows the ViewModel to reference string resources
 * without directly accessing [Context].
 *
 * The UI layer resolves the actual text via [asString].
 */
sealed class UiText {
    data class DynamicString(val value: String) : UiText()

    data class StringResource(
        @StringRes val resId: Int,
        val args: List<Any> = emptyList(),
    ) : UiText()

    @Suppress("SpreadOperator")
    fun asString(context: Context): String =
        when (this) {
            is DynamicString -> value
            is StringResource -> context.getString(resId, *args.toTypedArray())
        }
}
