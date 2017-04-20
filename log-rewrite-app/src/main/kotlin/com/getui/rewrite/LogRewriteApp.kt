package com.getui.rewrite

import com.getui.LogRewriter
import com.getui.RewriteOption
import com.xenomachina.argparser.ArgParser
import org.apache.commons.lang.StringUtils
//import org.apache.commons.cli.*
import java.io.File

/**
 * Created by fox on 17/04/2017.
 */

fun main(args: Array<String>): Unit {
//    args?.forEach(::println)
    val parser = ArgParser(args)
    val myargs = App.Args(parser)
    var path = "config.toml"
    if (StringUtils.isNotEmpty(myargs.config)) {
        path = myargs.config
    }
    val file = File(path)
    if (file.exists()) {
        App(file).rewrite()
    } else {
        println("config file not found:[${file.absolutePath}]")
        println("usage |-c  --config   the config file path")
    }


    /*
    val options = Options()
    val cfgOption = Option("c", "config", true, "the config file path")
    cfgOption.isRequired = false
    options.addOption(cfgOption)
    val parser = DefaultParser()

    val cmd: CommandLine?
    var configPath: String = "config.toml"
    try {
        cmd = parser.parse(options, args)
        println("opt===:${cmd.getOptionValue(cfgOption.opt)}")
        println("longOpt===:${cmd.getOptionValue(cfgOption.longOpt)}")
        configPath = cmd.getOptionValue(cfgOption.longOpt)
        println("the path is :[$configPath]")
    } catch (e: ParseException) {
        println("parse command  argument failure")
        printHelp(options)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    val cfgFile = File(configPath)
    if (cfgFile.exists()) {
        App(cfgFile).rewrite()
    } else {
        println("config file not found:[${cfgFile.absolutePath}]")
        printHelp(options)
        System.exit(1)

    }
    */
}


/*
fun printHelp(options: Options) {
    val formatter = HelpFormatter()
    formatter.printHelp("utility-name", options)
}
*/


class App(val config: File) {
    class Args(parser: ArgParser) {
        val verbose by parser.flagging("-v", "--verbose", help = "enable verbose mode")
        val config by parser.storing("-c", "--config", help = "the config file path")
    }

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
            val dist = unit.distribution
            val distDir = File(dist.dir)
            val mappingDir = dist.mappingDir
            val distMappingDir = if (StringUtils.isEmpty(mappingDir)) {
                distDir.parentFile
            } else {
                distDir.parentFile
            }
            LogRewriter(options, File(unit.source.dirs[0]), distDir, distMappingDir).rewrite()
        }
    }
}

