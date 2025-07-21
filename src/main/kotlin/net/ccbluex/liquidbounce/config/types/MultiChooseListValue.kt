package net.ccbluex.liquidbounce.config.types

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import net.ccbluex.liquidbounce.config.gson.stategies.Exclude
import net.ccbluex.liquidbounce.config.gson.stategies.ProtocolExclude
import java.util.EnumSet

class MultiChooseEnumListValue<T>(
    name: String,
    value: EnumSet<T>,
    choices: Set<T>,
    canBeNone: Boolean = true,
) : MultiChooseListValue<T>(
    name,
    value = value,
    choices = choices,
    canBeNone = canBeNone,
    autoSorting = true // EnumSet is ordered
) where T : Enum<T>, T : NamedChoice {
    override val T.elementName: String
        get() = choiceName
}

class MultiChooseStringListValue(
    name: String,
    value: MutableSet<String>,
    choices: Set<String>,
    canBeNone: Boolean = true,
) : MultiChooseListValue<String>(
    name,
    value = value,
    choices = choices,
    canBeNone = canBeNone,
    autoSorting = false
)

sealed class MultiChooseListValue<T>(
    name: String,
    /**
     * Enabled values. A mutable and unordered [Set].
     */
    value: MutableSet<T>,
    /**
     * All selectable choices. A readonly and ordered [Set].
     */
    @Exclude val choices: Set<T>,

    /**
     * Can deselect all values or enable at least one
     */
    @Exclude val canBeNone: Boolean = true,

    /**
     * If the [value] automatically implements sorting and guarantees order,
     * then set the [autoSorting] to true.
     * Otherwise, if the insertion order is not guaranteed,
     * leave [autoSorting] to false and then the implementation guarantees the order.
     */
    @Exclude @ProtocolExclude private val autoSorting: Boolean
) : Value<MutableSet<T>>(
    name,
    defaultValue = value,
    valueType = ValueType.MULTI_CHOOSE
) {
    init {
        if (!canBeNone) {
            require(choices.isNotEmpty()) {
                "There are no values provided, " +
                    "but at least one must be selected. (required because by canBeNone = false)"
            }

            require(value.isNotEmpty()) {
                "There are no default values enabled, " +
                    "but at least one must be selected. (required because by canBeNone = false)"
            }
        }

        value.retainAll(choices)
    }

    override fun deserializeFrom(gson: Gson, element: JsonElement) {

        val newSet = if (choices.isNotEmpty() && choices.first() is Enum<*>) {
            @Suppress("UNCHECKED_CAST")
            EnumSet.noneOf(choices.first()?.javaClass as Class<out Enum<*>?>?) as MutableSet<T>
        } else {
            mutableSetOf()
        }

        when (element) {
            is JsonArray -> element.forEach { newSet.tryToEnable(it.asString) }
            is JsonPrimitive -> newSet.tryToEnable(element.asString)
        }

        if (!canBeNone && newSet.isEmpty()) {
            newSet.addAll(choices)
        } else {
            newSet.sortIfAutoSortingDisabled()
        }


        set(newSet)
    }

    private fun MutableSet<T>.tryToEnable(name: String) {
        choices.firstOrNull { it.elementName == name }?.let {
            when (this) {
                is EnumSet<*> -> (this as EnumSet<T>).add(it)
                else -> add(it)
            }
        }
    }

    fun toggle(value: T): Boolean {
        require(value in choices) {
            "Provided value is not in the choices: $value"
        }

        val current = get()

        val isActive = value in current

        if (isActive) {
            if (!canBeNone && current.size <= 1) {
                return true
            }

            current.remove(value)
        } else {
            current.add(value)
        }

        current.sortIfAutoSortingDisabled()
        set(current)

        return !isActive
    }

    private fun MutableSet<T>.sortIfAutoSortingDisabled() {
        if (autoSorting) {
            return
        }

        val temp = LinkedHashSet(this)
        clear()

        for (choice in choices) {
            if (temp.contains(choice)) {
                add(choice)
            }
        }
    }

    protected open val T.elementName: String get() = this.toString()

    operator fun contains(choice: T) = get().contains(choice)
}
