package com.getui

import com.netflix.rewrite.ast.Expression
import com.netflix.rewrite.ast.Tr
import com.netflix.rewrite.parse.OracleJdkParser
import com.netflix.rewrite.parse.Parser
import com.netflix.rewrite.refactor.Refactor
import org.apache.commons.lang.StringUtils
import org.jetbrains.kotlin.com.intellij.util.containers.ConcurrentMultiMap
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer
import java.util.regex.Pattern
import kotlin.streams.asStream

/**
 * Created by fox on 12/04/2017.
 */
class LogRewriter(val options: Collection<RewriteOption>, val source: File, val dist: File) {
    val parser: Parser = OracleJdkParser()

    fun rewrite(): Unit {
        val treeWalk = source.walk()
        val javaSources = treeWalk.filter { it.name.endsWith(".java", true) }
                .map { Paths.get(it.toURI()) }
                .asStream().parallel()
                .map { path ->
                    val parserResult = parser.parse(listOf(path))
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
|on file :[$path]
|on class: [${clazz.javaClass.canonicalName}]
|   the method [${mc.type?.declaringType?.fullyQualifiedName}] found by signature :[${option.signature}]
|   the argument maxIndex:[${args.lastIndex}] less than your config :[${option.argumentIndex}]
|-> ${mc.simpleName}
""".trimMargin())
                                                            val expression = args[option.argumentIndex]
                                                            LogMapping.refactor(clazz, expression, tx)
                                                        }
                                            }
                                        }
                                    }
                                    val fix = tx.fix()
                                    val fullPathStr = path.toFile().absolutePath
                                    val newPathStr = fullPathStr.replace(source.absolutePath, dist.absolutePath)
                                    val newFile = File(newPathStr)
                                    val dir = newFile.parentFile
                                    if (!dir.exists()) dir.mkdirs()
                                    newFile.writeText(fix.print())
                                })
                            }
                    return@map parserResult
                }
                .forEach {}
    }
}


object LogMapping {
    const val PREFIX = "<!--["
    const val POSTFIX = "]-->"
    val FORMAT_PREFIX = fun(id: Long): String { return "<!--$id{" }
    const val FORMAT_POSTFIX = "}-->"

    const val IDENT_PREFIX = "<!--("
    const val IDENT_POSTFIX = ")-->"

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
                            return@changeLiteral "$PREFIX$id$POSTFIX"
                        }
                        else -> {
                            originSb.append(t)
                            return@changeLiteral t
                        }
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
                            if (StringUtils.equals("locale", arg0.simpleName)) {
                                argFormat = target.args.args[1] as Tr.Literal
                            }
                        }
                        is Tr.Literal -> argFormat = arg0
                    }
                    if (argFormat != null) {
                        refactor.changeLiteral(argFormat) { t ->
                            t as String
                            val id = pushMapping(clazz, t)

                            val matcher = Pattern.compile("(%(?:[scbdxofaegh%n]|tx))").matcher(t)
                            val builder = StringBuilder()
                            while (matcher.find()) {
                                for (i in 1..matcher.groupCount()) {
                                    builder.append(FORMAT_PREFIX(id))
                                            .append(matcher.group(i))
                                            .append(FORMAT_POSTFIX)
                                }
                                originSb.append("String::format($t ,...)")
                            }
                            return@changeLiteral builder.toString()
                            return@changeLiteral t
                        }
                    }
                }
            }
        }
    }
}


class RewriteOptionIllegalException(override val message: String?) : RuntimeException()


data class RewriteOption(val signature: String
                         , val argumentIndex: Int = 0
                         , val isStaticMethod: Boolean = true)