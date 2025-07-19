package net.ccbluex.liquidbounce.config.gson.adapter

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

abstract class IdentifierAsStringAdapter<T : Any>(val registry: Registry<T>) : TypeAdapter<T>() {

    override fun read(source: JsonReader): T? {
        return registry.get(Identifier.tryParse(source.nextString()))
    }

    override fun write(sink: JsonWriter, value: T?) {
        val id = value?.let { registry.getId(it) }
        if (id == null) {
            sink.nullValue()
        } else {
            sink.value(id.toString())
        }
    }

}
