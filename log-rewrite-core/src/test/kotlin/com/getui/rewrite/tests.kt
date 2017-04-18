package com.getui.rewrite

import com.getui.LogRewriter
import com.getui.RewriteOption
import org.junit.Test
import java.nio.file.Paths

/**
 * Created by fox on 17/04/2017.
 */

class TestRewriteSource {
    val rootProjectPath = "/Users/fox/workspace/getui/tools/log-rewrite"
    @Test fun rewriteByDir(): Unit {
        val source = Paths.get(rootProjectPath, "rewrite-test-java", "src", "main", "java")
        val dist = Paths.get(rootProjectPath, "rewrite-test-java", "build", "rewrite", "src", "main", "java")
        val logRewriter = LogRewriter(
                listOf(RewriteOption(signature = "com.getui.rewrite.test.LogUtil log(String)", argumentIndex = 0)
                        , RewriteOption(signature = "com.getui.rewrite.test.LogUtil log(String,String)", argumentIndex = 1)
                        , RewriteOption(signature = "com.getui.rewrite.test.LogUtil debug(String)", argumentIndex = 0)
                )
                , source.toFile()
                , dist.toFile()
        )
        logRewriter.rewrite()
    }
}