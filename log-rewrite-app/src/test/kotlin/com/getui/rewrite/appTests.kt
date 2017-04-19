package com.getui.rewrite

import org.junit.Test
import java.io.File

/**
 * Created by fox on 18/04/2017.
 */


class ConfigReadTest {
    @Test fun readConfig(): kotlin.Unit {
//        val configPath = "config.toml"
        val configPath = "config_multi.toml"
        val cfgFile = File(configPath)
        try {
            val cfg = Config.read(cfgFile)
            println(cfg.size)
        } catch (e: Exception) {
            println(e)
        }
    }
}