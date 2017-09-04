package com.getui

import com.netflix.rewrite.ast.Expression
import com.netflix.rewrite.ast.Tr
import com.netflix.rewrite.ast.Type
import com.netflix.rewrite.parse.OracleJdkParser
import com.netflix.rewrite.parse.Parser
import com.netflix.rewrite.refactor.Refactor
import org.apache.commons.lang.StringUtils
import org.jetbrains.kotlin.com.intellij.util.containers.ConcurrentMultiMap
import java.io.File
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer
import java.util.regex.Pattern
import kotlin.streams.asStream

/**
 * Created by fox on 12/04/2017.
 */

const val VERSION_CODE = 1
val formatArgsPattern = Pattern.compile("(%(?:[scbdxofaegh%n]|tx))")

class LogRewriter(val options: Collection<RewriteOption>, val source: File, val dist: File, val distDir: File, val targetVersionCode: Int = 1, val projectName: String = "") {
    val parser: Parser = OracleJdkParser()
    val logMapping = LogMapping(distDir, targetVersionCode, projectName)

    fun rewrite(): Unit {
        val treeWalk = source.walk()
        val javaSources = treeWalk
                .apply {
                    filter { it.isFile && !it.name.endsWith(".java") }
                            .forEach {
                                val newFile = transformDistPath(it, source, dist)
                                it.copyTo(newFile, true)
                            }
                }
                .filter { it.name.endsWith(".java", true) }
                .map { Paths.get(it.toURI()) }
//                .asStream()
//                .parallel()
                .toList()
        val parserResult = parser.parse(javaSources)
        parserResult
                .forEach { unit ->
                    unit.refactor(Consumer { tx ->
                        unit.classes.forEach { clazz ->
                            options.forEach { option ->
                                if (option.isStaticMethod) {
                                    clazz.findMethodCalls(option.signature)
                                            .forEach { mc ->
                                                val args = mc.args.args
                                                if (args.lastIndex < option.argumentIndex) throw RewriteOptionIllegalException(
                                                        """
|on file :[${unit.sourcePath}]
|on class: [${clazz.javaClass.canonicalName}]
|   the method [${mc.type?.declaringType?.fullyQualifiedName}] found by signature :[${option.signature}]
|   the argument maxIndex:[${args.lastIndex}] less than your config :[${option.argumentIndex}]
|-> ${mc.simpleName}
""".trimMargin())
                                                val expression = args[option.argumentIndex]
                                                logMapping.refactor(clazz, expression, tx)
                                            }
                                }
                            }
                        }
                        val fix = tx.fix()
                        val newFile = transformDistPath(File(unit.sourcePath), source, dist)
                        newFile.writeText(fix.print())
                    })
                }
        logMapping.finish()
    }
}

fun transformDistPath(srcFile: File, srcDir: File, dist: File): File {
    val fullPathStr = srcFile.absolutePath
    val newPathStr = fullPathStr.replace(srcDir.absolutePath, dist.absolutePath)
    val newFile = File(newPathStr)
    val dir = newFile.parentFile
    if (!dir.exists()) dir.mkdirs()
    return newFile
}


class LogMapping(val distDir: File, val targetVersionCode: Int, val projectName: String) {
    val TOKEN = "$projectName|${VERSION_CODE}_$targetVersionCode:"
    val PREFIX = "<!--["
    val POSTFIX = "]-->"
    val FORMAT_PREFIX = fun(id: Long): String { return "<!--$TOKEN$id{" }
    val FORMAT_POSTFIX = "}-->"
    private fun rewriteNormal(id: Long): String {
        return "$PREFIX${TOKEN}$id$POSTFIX"
    }

//    val IDENT_PREFIX = "<!--("
//    val IDENT_POSTFIX = ")-->"

    data class Item(val clazz: Tr.ClassDecl, val placement: String, val id: Long)

    val mapping: ConcurrentLinkedDeque<Item> = ConcurrentLinkedDeque()
    val originMapping: ConcurrentMultiMap<Tr.ClassDecl, String> = ConcurrentMultiMap()

    val ID_GENERATOR = AtomicLong(0)
    fun refactor(clazz: Tr.ClassDecl, target: Expression, refactor: Refactor): Unit {
        val originSb = StringBuilder()
        refactor(clazz, target, refactor, originSb)
        originMapping.putValue(clazz, originSb.toString())
    }

    fun finish(): Unit {
//todo write mapping to disk
        val parent = distDir.parentFile
        if (!parent.exists()) {
            parent.mkdirs()
        }
        val prefix = "${projectName}_v${VERSION_CODE}_$targetVersionCode"

        File(distDir, "${prefix}_log_mapping.txt").bufferedWriter().use { out ->
            out.write("#$projectName:$VERSION_CODE:$targetVersionCode")
            out.newLine()
            mapping.groupBy { it.clazz }
                    .forEach { t, u ->
                        out.write("#${(t.type as Type.Class).fullyQualifiedName}")
                        out.newLine()
                        u.forEach {
                            out.write("${it.id}=${it.placement}")
                            out.newLine()
                        }
                    }
        }

        File(distDir, "${prefix}_human_mapping.txt").bufferedWriter().use { out ->
            originMapping.entrySet().forEach { t ->
                out.write((t.key.type as Type.Class).fullyQualifiedName)
                out.newLine()
                t.value?.forEach {
                    out.write("  $it")
                }
            }
        }
    }

    private fun pushMapping(clazz: Tr.ClassDecl, placement: String): Long {
        val id = ID_GENERATOR.getAndIncrement()
        mapping.add(Item(clazz, placement, id))
        return id
    }


    private fun refactor(clazz: Tr.ClassDecl, target: Expression, refactor: Refactor, originSb: StringBuilder): Unit {
        when (target) {
            is Tr.Literal -> {
                refactor.changeLiteral(target) { t ->
                    when (t) {
                        is String -> {
                            val id = pushMapping(clazz, t)
                            originSb.append("$PREFIX$t$POSTFIX")
//                            originMapping.putValue(clazz, originSb.toString())
                            return@changeLiteral rewriteNormal(id)
                        }
                        else -> return@changeLiteral t
                    }
                }
            }
            is Tr.Binary -> {
                refactor(clazz, target.left, refactor, originSb)
                refactor(clazz, target.right, refactor, originSb)
            }
            is Tr.MethodInvocation -> {
                if (StringUtils.equals("java.lang.String", target.type?.declaringType?.fullyQualifiedName)
                        && StringUtils.equals("format", target.type?.name)) {
                    var argFormat: Tr.Literal? = null

                    val arg0 = target.args.args[0]
                    when (arg0) {
                        is Tr.Ident -> {
                            if (isLocaleIdent(arg0)) {
                                argFormat = target.args.args[1] as Tr.Literal
                            }
                        }
                        is Tr.FieldAccess -> {
                            val argTarget = arg0.target
                            when (argTarget) {
                                is Tr.Ident -> {
                                    if (isLocaleIdent(argTarget)) {
                                        argFormat = target.args.args[1] as Tr.Literal
                                    }
                                }
                            }
                        }
                        is Tr.Literal -> argFormat = arg0
                    }
                    if (argFormat != null) {
                        refactor.changeLiteral(argFormat) { t ->
                            t as String
                            val id = pushMapping(clazz, t)

                            val matcher = formatArgsPattern.matcher(t)
                            val builder = StringBuilder()
                            while (matcher.find()) {
                                for (i in 1..matcher.groupCount()) {
                                    builder.append(FORMAT_PREFIX(id))
                                            .append(matcher.group(i))
                                            .append(FORMAT_POSTFIX)
                                }
                                originSb.append("String::format($t ,...)")
                            }
//                            originMapping.putValue(clazz, originSb.toString())
                            return@changeLiteral builder.toString()
                        }
                    }
                }
            }
        }
    }
}


fun isLocaleIdent(target: Tr.Ident): Boolean {
    val fullName = Locale::class.qualifiedName
    val targetName = (target.type as Type.Class).fullyQualifiedName
    return StringUtils.equals(fullName, targetName)
}

class RewriteOptionIllegalException(override val message: String?) : RuntimeException()


data class RewriteOption(val signature: String
                         , val argumentIndex: Int = 0
                         , val isStaticMethod: Boolean = true)