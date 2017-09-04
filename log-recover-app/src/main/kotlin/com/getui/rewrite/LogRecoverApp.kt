package com.getui.rewrite

import com.xenomachina.argparser.ArgParser
import org.jetbrains.kotlin.com.intellij.util.containers.MultiMap
import java.io.File
import java.util.*
import java.util.regex.Pattern

/**
 * Created by fox on 24/04/2017.
 */

fun main(args: Array<String>): Unit {
    val parser = ArgParser(args)
    val app = App(parser)
    app.run {
        recover()
    }
}

class App(parser: ArgParser) {
    val verbose by parser.flagging("-v", "--verbose", help = "enable verbose mode")
    val mappingFiles by parser.adding("-m", "--mapping", help = "the mapping files") { File(this) }
    val logFiles by parser.adding("-l", "--log", help = "the log files") { File(this) }
    val distDir by parser.storing("-d", "--dist", help = "the dist dir") { File(this) }

    //        val version by parser.storing("-V", "--version", help = "target version code [number]") { toInt() }
    val mapping = LinkedHashMap<String, MultiMap<Long, MappingItem>>()

    //https://regex101.com/r/a3QaYz/1
    val normalPattern = Pattern.compile("(<!--\\[([^|]+)\\|(\\d+)_(\\d+):(\\d+)]-->)")

    //https://regex101.com/r/vTJGaw/4
    val formatPattern = Pattern.compile("(<!--([^|]+)\\|(\\d)+_(\\d+):(\\d+)\\{(.*?)}-->)")

    val formatArgsPattern = Pattern.compile("(%(?:[scbdxofaegh%n]|tx))")
    private fun fullFormatPattern(mappingItem: MappingItem): Regex {
        //https://regex101.com/r/RFrFuT/4
        return Regex("(?:<!--${mappingItem.projectName}\\|${mappingItem.appVersion}_${mappingItem.targetVersion}:${mappingItem.id}\\{(.*?)\\}-->)+")
    }

    fun recover(): Unit {
        readMapping()

        logFiles.forEach { file ->
            file.bufferedReader().use { reader ->
                File(distDir, file.name).bufferedWriter().use { writer ->
                    var line: String
                    while (true) {
                        line = reader.readLine()
                        if (line == null) break
                        val matcher = normalPattern.matcher(line)
                        var newLine: String = line + ""
                        while (matcher.find()) { //normal recover
                            val token = matcher.group(1)
                            val projectName = matcher.group(2)
                            val appVersion = matcher.group(3).toInt()
                            val targetVersion = matcher.group(4).toInt()
                            val id = matcher.group(5).toLong()
                            val replaceMent = findReplacement(projectName, appVersion, targetVersion, id) ?: "<------can not find check mapping or report bug----->"
                            newLine = newLine.replace(token, replaceMent)
                        }
                        // recover format
                        val formatMatcher = formatPattern.matcher(line)
                        var formatTokenList = LinkedList<MappingItem>()
                        while (formatMatcher.find()) {
                            val token = formatMatcher.group(1)
                            val projectName = formatMatcher.group(2)
                            val appVersion = formatMatcher.group(3).toInt()
                            val targetVersion = formatMatcher.group(4).toInt()
                            val id = formatMatcher.group(5).toLong()
                            val replaceMent = formatMatcher.group(6)
                            formatTokenList.add(MappingItem(id, replaceMent, projectName, appVersion, targetVersion))
                        }
                        formatTokenList.groupBy { it.id }.forEach { id, items ->
                            val first = items.first()
                            val mappingReplacement = findReplacement(first.projectName, first.appVersion, first.targetVersion, id)
                            if (mappingReplacement != null) {
                                val argsMattcher = formatArgsPattern.matcher(mappingReplacement)
                                var newReplace = mappingReplacement + ""
                                var index = 0
                                while (argsMattcher.find()) {
                                    newReplace = newReplace.replaceFirst(argsMattcher.group(1), items[index++].replaceMent)
                                }
                                newLine = newLine.replace(fullFormatPattern(first), newReplace)
                            }
                        }
                        writer.write(newLine)
                        writer.newLine()
                    }
                }
            }
        }

    }

    private fun findReplacement(projectName: String?, appVersion: Int, targetVersion: Int, id: Long): String? {
        val projectMapping = mapping.get(projectName)
        return projectMapping?.get(id)?.filter { item ->
            item.appVersion == appVersion
                    && item.targetVersion == targetVersion
        }?.first()?.replaceMent
    }

    private fun readMapping() {
        mappingFiles.forEach { file ->
            file.bufferedReader().use { reader ->
                var first = reader.readLine()
                if (!first.startsWith("#") && !first.contains(":")) {
                    throw RuntimeException("the mapping file has  error format,without header. [${file.absolutePath}]")
                }
                first = first.trimStart('#')
                val meta = first.split(":")
                val projectName = meta[0]
                val appVersion = meta[1]
                val targetVersion = meta[2]
                var line: String?
                while (true) {
                    line = reader.readLine()
                    if (line == null) break
                    if (!line.startsWith("#")) {
                        try {
                            val pair = line.split("=", limit = 2)
                            val id = pair[0].toLong()
                            val replaceMent = pair[1]
                            var projMapping = mapping.get(projectName)
                            if (projMapping == null) {
                                projMapping = MultiMap<Long, MappingItem>()
                                mapping.put(projectName, projMapping)
                            }
                            projMapping.putValue(id, MappingItem(id, replaceMent, projectName, appVersion.toInt(), targetVersion.toInt()))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    class MappingItem(val id: Long, val replaceMent: String, val projectName: String, val appVersion: Int, val targetVersion: Int)
}