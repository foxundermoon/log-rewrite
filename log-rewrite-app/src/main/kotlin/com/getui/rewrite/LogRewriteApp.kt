package com.getui.rewrite

import com.getui.LogRewriter
import com.getui.RewriteOption
import org.apache.commons.cli.*
import java.io.File

/**
 * Created by fox on 17/04/2017.
 */

fun main(args: Array<String>): Unit {
    val options = Options()
    val cfgOption = Option("c", "config", false, "the config file path")
    cfgOption.isRequired = false
    options.addOption(cfgOption)

    val parser = DefaultParser()

    val cmd: CommandLine?
    var configPath: String = "config.toml"
    try {
        cmd = parser.parse(options, args)
        configPath = cmd.getOptionValue("config")
    } catch (e: ParseException) {
        printHelp(options)
    } catch (e: Exception) {

    }
    val cfgFile = File(configPath)
    if (cfgFile.exists()) {
        App(cfgFile).rewrite()
    } else {
        printHelp(options)
        System.exit(1)

    }
}

fun printHelp(options: Options) {
    val formatter = HelpFormatter()
    formatter.printHelp("utility-name", options)
}

class App(val config: File) {
    fun rewrite(): kotlin.Unit {
        val cfg = Config.read(config)
        cfg.forEach { unit ->
            val options = unit.signatures.map { sign ->
                val signature = sign.sign
                val argsIndex = sign.argsIndex
                val index = if (argsIndex == null) {
                    0
                } else {
                    argsIndex[0]
                }
                RewriteOption(signature, index, true)
            }
            LogRewriter(options, File(unit.source.dirs[0]), File(unit.distribution.dir)).rewrite()
        }
    }
}
