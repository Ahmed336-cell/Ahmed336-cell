package com.athar.mobile.data

import android.content.Context
import com.athar.engine.Corpus
import org.json.JSONArray
import org.json.JSONObject

object AssetCorpusLoader {
    fun load(context: Context): Corpus {
        val passagesJson = context.assets.open("corpus/passages.json").bufferedReader().use { it.readText() }
        val gradesJson = runCatching {
            context.assets.open("corpus/grades.json").bufferedReader().use { it.readText() }
        }.getOrDefault("{}")
        val metaJson = runCatching {
            context.assets.open("corpus/meta.json").bufferedReader().use { it.readText() }
        }.getOrDefault("{}")

        val arr = JSONArray(passagesJson)
        val passages = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    mapOf(
                        "t" to o.getString("t"),
                        "r" to o.getString("r"),
                        "n" to o.getString("n"),
                        "m" to o.optInt("m", 1)
                    )
                )
            }
        }

        val gradeObj = JSONObject(gradesJson).optJSONObject("map") ?: JSONObject()
        val grades = buildMap {
            val keys = gradeObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = gradeObj.get(k)
                put(
                    k,
                    when (v) {
                        is JSONObject -> mapOf(
                            "g" to v.optString("g", v.optString("label")),
                            "s" to v.optString("s", v.optString("scholar"))
                        )
                        else -> v.toString()
                    }
                )
            }
        }

        val metaObj = JSONObject(metaJson)
        val meta = buildMap<String, Any?> {
            metaObj.keys().forEach { key -> put(key, metaObj.get(key)) }
        }

        return Corpus.fromJsonMaps(passages, grades, meta)
    }
}
