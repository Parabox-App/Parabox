package com.ojhdtapp.parabox.data.remote.dto.server.content

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

class ContentTypeAdapter : TypeAdapter<Content>() {
    override fun write(out: JsonWriter, value: Content?) {
        out.beginObject()
        when (value) {
            is Text -> {
                out.name("text").value(value.text)
                out.name("type").value(value.type)
            }
            is Image -> {
                out.name("url").value(value.url)
                out.name("cloud_type").value(value.cloud_type)
                out.name("cloud_id").value(value.cloud_id)
                out.name("file_name").value(value.file_name)
                out.name("type").value(value.type)
            }
            else -> {
                throw Exception("Unknown type")
            }
        }
        out.endObject()
    }

    override fun read(json: JsonReader): Content {
        json.beginObject()
        while (json.hasNext()) {
            val name = json.nextName()
            if (name == "type") {
                val type = json.nextInt()
                return when (type) {
                    0 -> {
                        var text = ""
                        while (json.hasNext()) {
                            val name = json.nextName()
                            if (name == "text") {
                                text = json.nextString()
                            }
                        }
                        json.endObject()
                        Text(text)
                    }
                    1 -> {
                        var url = ""
                        var cloud_type = 0
                        var cloud_id = ""
                        var file_name = ""
                        while (json.hasNext()) {
                            val name = json.nextName()
                            when (name) {
                                "url" -> url = json.nextString()
                                "cloud_type" -> cloud_type = json.nextInt()
                                "cloud_id" -> cloud_id = json.nextString()
                                "file_name" -> file_name = json.nextString()
                                else -> json.skipValue()
                            }
                        }
                        json.endObject()
                        Image(url, cloud_type, cloud_id, file_name)
                    }
                    else -> throw Exception("Unknown type")
                }
            }
        }
        throw Exception("No type found")
    }
}