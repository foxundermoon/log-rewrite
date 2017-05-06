package com.getui.rewrite

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.moandjiezana.toml.Toml
import java.io.File
import java.io.FileNotFoundException

/**
 * Created by fox on 18/04/2017.
 */

object Config {
    fun read(config: File): Configuration {
        if (!config.exists()) {
            throw ConfigErrorException("config file not found",
                    FileNotFoundException("file on path:[${config.absolutePath}] can not found")
            )
        }
        val toml: Toml
        try {
            toml = Toml().read(config)
            return toml.to(Configuration::class.java)
//            val gson = GsonBuilder()
//                    .create()
//            val jsonTree = gson.toJsonTree(toml.toMap()) as JsonObject
//            val jsonArray = JsonArray()
//            jsonTree.entrySet().forEach { jsonArray.add(it.value) }
//            val unitList = gson.fromJson<List<Unit>>(jsonTree.get("unit"), object : TypeToken<List<Unit>>() {}.type)
//             = toml.to(ConfigUnitList::class.java)
//            return unitList
        } catch (e: Exception) {
            throw ConfigErrorException(e)
        }
    }

    class ConfigErrorException : Exception {
        constructor(msg: String) : super(msg)
        constructor(e: Throwable) : super(e)
        constructor(msg: String, throwable: Throwable) : super(msg, throwable)
    }

    class Configuration(val unit: List<Unit>, val projectShortName: String, val versionCode: Int)
    class Unit(val signatures: Array<Signature>, val source: Source, val distribution: Distribution)
    class Signature(val sign: String, val argsIndex: IntArray = intArrayOf(0), val isStatic: Boolean = true)
    class Source(val dirs: Array<String>, val includeExt: Array<String>?, val excludeExt: Array<String>?)
    data class Distribution(val dir: String, val mappingDir: String? = null)
}