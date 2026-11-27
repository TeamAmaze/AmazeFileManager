/*
 * Audit and verify HTML-in-CDATA Android string resources.
 *
 * Usage (Kotlin 1.9+ command line compiler, standard library and JDK only):
 *     kotlin cdata_check.main.kts audit  <res-dir>
 *     kotlin cdata_check.main.kts verify <before.xml> <after.xml> [<source values/strings.xml>]
 *
 * "audit" lists every translation of a string that is CDATA-wrapped in the
 * source file and reports the ones whose CDATA or markup is broken.
 *
 * "verify" proves that an edited translation file only changed markup: the
 * file outside <string> bodies is byte-identical, and every changed string
 * still has exactly the same user-visible text. Exit code 0 means PASS.
 *
 * Parsing is regex based on purpose: broken translation files are often not
 * well-formed XML, and those are exactly the ones that need reading.
 */

import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import java.io.File
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory
import kotlin.system.exitProcess

// Every pattern starts with (?U) so that \s and \b are Unicode aware, as
// they are in Python's re module.
val stringPattern =
    Regex(
        """(?U)(<string\s+name="([^"]+)"[^>]*?(?<!/)>)(.*?)(</string>)""",
        RegexOption.DOT_MATCHES_ALL,
    )

val htmlTags =
    "html|head|body|h[1-6]|p|br|ul|ol|li|b|i|u|em|strong|small|big|font|a|span|div|tt" +
        "|strike|s|sub|sup|blockquote|img|hr|del|cite|dfn"

// A tag written as <x>, <x&gt; or &lt;x&gt; (the last two are the broken forms).
val tagPattern =
    Regex(
        """(?U)(?:<|&lt;)\s*(/?)\s*($htmlTags)\b([^<>]*?)(/?)\s*(?:>|&gt;)""",
        RegexOption.IGNORE_CASE,
    )

val escapedTagPattern =
    Regex(
        """(?U)&lt;\s*/?\s*(?:$htmlTags)\b|<\s*/?\s*(?:$htmlTags)\b[^<>]*?&gt;""",
        RegexOption.IGNORE_CASE,
    )

val hrefPattern =
    Regex(
        """(?U)href\s*=\s*(?:\\?"|&quot;|')?([^"'\\&\s>]+)""",
        RegexOption.IGNORE_CASE,
    )

val entityPattern = Regex("""&(#x[0-9a-fA-F]+|#[0-9]+|quot|apos|lt|gt|nbsp|amp);""")
val namedEntities =
    mapOf("quot" to "\"", "apos" to "'", "lt" to "<", "gt" to ">", "nbsp" to " ", "amp" to "&")

val whitespaceRun = Regex("""(?U)\s+""")
val fourHexDigits = Regex("[0-9a-fA-F]{4}")

val cdataStart = "<![CDATA["
val cdataEnd = "]]>"

// Characters that follow a backslash and stand for a space once rendered.
val whitespaceEscapes = "nt"
val unicodeEscapeLength = 4
val contextBefore = 40
val contextAfter = 60

// ------------------------------------------------------------------ helpers

/** Read [path] as UTF-8 with universal newlines, so CRLF and LF copies compare equal. */
fun read(path: File): String = path.readText(Charsets.UTF_8).replace("\r\n", "\n").replace('\r', '\n')

/** Map string name -> raw body, exactly as written in the file. */
fun strings(content: String): Map<String, String> = stringPattern.findAll(content).associate { it.groupValues[2] to it.groupValues[3] }

/** The file with every <string> body blanked, to detect edits elsewhere. */
fun skeleton(content: String): String = stringPattern.replace(content) { it.groupValues[1] + "\u2026" + it.groupValues[4] }

/** Whether a string [body] is wrapped in a CDATA section. */
fun hasCdata(body: String): Boolean = body.trim().startsWith(cdataStart)

/** Return the content of the CDATA section of [body], or [body] unchanged if it has none. */
fun stripCdata(body: String): String {
    val trimmed = body.trim()
    if (!trimmed.startsWith(cdataStart)) {
        return body
    }
    val inner = trimmed.substring(cdataStart.length)
    val end = inner.lastIndexOf(cdataEnd)
    return if (end >= 0) inner.substring(0, end) else inner
}

/**
 * Decode Android string resource escapes into the text they stand for.
 *
 * \n and \t become a space, because the comparison collapses whitespace
 * anyway, \uXXXX becomes its character, and any other escaped character
 * stands for itself.
 */
fun decodeAndroidEscapes(text: String): String {
    val out = StringBuilder()
    var i = 0
    while (i < text.length) {
        val char = text[i]
        if (char != '\\' || i + 1 >= text.length) {
            out.append(char)
            i += 1
            continue
        }
        val escaped = text[i + 1]
        val hex = text.substring(minOf(i + 2, text.length), minOf(i + 2 + unicodeEscapeLength, text.length))
        when {
            escaped in whitespaceEscapes -> {
                out.append(' ')
                i += 2
            }
            escaped == 'u' && fourHexDigits.matches(hex) -> {
                out.append(hex.toInt(16).toChar())
                i += 2 + unicodeEscapeLength
            }
            else -> {
                out.append(escaped)
                i += 2
            }
        }
    }
    return out.toString()
}

/** Decode numeric character references and the XML and HTML entities strings use. */
fun decodeEntities(text: String): String =
    entityPattern.replace(text) { match ->
        val entity = match.groupValues[1]
        when {
            entity.startsWith("#x") -> String(Character.toChars(entity.substring(2).toInt(16)))
            entity.startsWith("#") -> String(Character.toChars(entity.substring(1).toInt()))
            else -> namedEntities.getValue(entity)
        }
    }

/**
 * Return what a user reads.
 *
 * Markup is removed, Android escapes and entities are decoded, and
 * whitespace is collapsed (HTML rendering collapses it as well).
 */
fun visibleText(body: String): String {
    var text = tagPattern.replace(stripCdata(body), " ")
    text = decodeAndroidEscapes(text)
    text = decodeEntities(text)
    return whitespaceRun.replace(text, " ").trim()
}

/** Count each opening and closing HTML tag in [body], with every form of br counted as one. */
fun tagCounts(body: String): Map<String, Int> {
    val counts = mutableMapOf<String, Int>()
    for (match in tagPattern.findAll(stripCdata(body))) {
        val name = match.groupValues[2].lowercase()
        val key =
            when {
                name == "br" -> "<br/>"
                match.groupValues[1].isNotEmpty() -> "</$name>"
                else -> "<$name>"
            }
        counts[key] = (counts[key] ?: 0) + 1
    }
    return counts
}

/** Describe the tags whose counts differ between source and translation, or "" if none do. */
fun tagDiff(
    sourceBody: String,
    translatedBody: String,
): String {
    val expected = tagCounts(sourceBody)
    val actual = tagCounts(translatedBody)
    return (expected.keys + actual.keys)
        .sorted()
        .filter { (expected[it] ?: 0) != (actual[it] ?: 0) }
        .joinToString(", ") { "$it source=${expected[it] ?: 0} translation=${actual[it] ?: 0}" }
}

/** Return the link targets in [body], whether its quotes are plain, escaped or entities. */
fun hrefs(body: String): List<String> = hrefPattern.findAll(body).map { it.groupValues[1] }.toList()

/**
 * Return why [content] is not well-formed XML, or null if it is.
 *
 * String resources never have a DOCTYPE, so refusing one rules out entity
 * expansion attacks ("billion laughs") and external entities (XXE).
 */
fun wellFormedError(content: String): String? {
    val factory = SAXParserFactory.newInstance()
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    val input = InputSource(ByteArrayInputStream(content.toByteArray(Charsets.UTF_8)))
    return try {
        factory.newSAXParser().parse(input, DefaultHandler())
        null
    } catch (error: org.xml.sax.SAXParseException) {
        "${error.message}: line ${error.lineNumber}, column ${error.columnNumber}"
    } catch (error: org.xml.sax.SAXException) {
        error.message ?: error.toString()
    }
}

/** Return the 1-based line where the string [name] is declared in [content], or -1. */
fun lineOf(
    content: String,
    name: String,
): Int {
    val index = content.indexOf("name=\"$name\"")
    return if (index < 0) -1 else content.substring(0, index).count { it == '\n' } + 1
}

/** Show both texts around the first character where [before] and [after] differ. */
fun firstDifference(
    before: String,
    after: String,
): String {
    var i = 0
    while (i < before.length && i < after.length && before[i] == after[i]) {
        i += 1
    }
    val start = maxOf(0, i - contextBefore)

    fun excerpt(text: String) = text.substring(minOf(start, text.length), minOf(i + contextAfter, text.length))
    return "  before: ...${excerpt(before)}...\n  after:  ...${excerpt(after)}..."
}

// -------------------------------------------------------------------- audit

/**
 * Report the translated strings under [resDir] whose CDATA or markup is broken.
 *
 * Return the exit code: 0 if no string needs attention, 1 otherwise.
 */
fun audit(resDir: File): Int {
    val source = strings(read(File(File(resDir, "values"), "strings.xml")))
    val cdataSource = source.filterValues(::hasCdata).keys
    println("Source strings using CDATA: " + cdataSource.sorted().joinToString(", "))

    val translations =
        resDir.listFiles { file -> file.isDirectory && file.name.startsWith("values-") }
            .orEmpty()
            .map { File(it, "strings.xml") }
            .filter { it.isFile }
            .sortedBy { it.parentFile.name }

    var problems = 0
    for (path in translations) {
        val content = read(path)
        val translated = strings(content)
        val lines = mutableListOf<String>()
        for ((name, body) in translated) {
            val sourceBody = source[name] ?: continue
            val issues = stringIssues(name in cdataSource, sourceBody, body)
            if (issues.isNotEmpty()) {
                lines.add("  $name line ${lineOf(content, name)}: ${issues.joinToString(" ")}")
            }
        }

        val xmlError = wellFormedError(content)
        if (lines.isNotEmpty() || (xmlError != null && cdataSource.any { it in translated })) {
            println("\n${path.parentFile.name}/${path.name}")
            if (xmlError != null) {
                println("  NOT_WELL_FORMED_XML: $xmlError")
            }
            lines.forEach(::println)
            problems += lines.size
        }
    }

    println("\n$problems string(s) need attention.")
    return if (problems == 0) 0 else 1
}

/** Return the issue codes of one translated [body] compared with its [sourceBody]. */
fun stringIssues(
    sourceCdata: Boolean,
    sourceBody: String,
    body: String,
): List<String> {
    val issues = mutableListOf<String>()
    if (sourceCdata && !hasCdata(body)) {
        issues.add("MISSING_CDATA")
    }
    if (!sourceCdata && hasCdata(body)) {
        issues.add("UNEXPECTED_CDATA")
    }
    if (escapedTagPattern.containsMatchIn(stripCdata(body))) {
        issues.add(if (sourceCdata) "ESCAPED_MARKUP" else "ESCAPED_MARKUP_OUTSIDE_CDATA_SCOPE")
    }
    if (hasCdata(body) && cdataEnd in stripCdata(body)) {
        issues.add("NESTED_CDATA_END")
    }
    if (sourceCdata) {
        val diff = tagDiff(sourceBody, body)
        if (diff.isNotEmpty()) {
            issues.add("MARKUP_DIFFERS($diff)")
        }
    }
    return issues
}

// ------------------------------------------------------------------- verify

/**
 * Check that [afterPath] differs from [beforePath] only in the markup of its strings.
 *
 * With [sourcePath], also check the changed strings against the source
 * markup. Return the exit code: 0 for PASS, 1 for FAIL.
 */
fun verify(
    beforePath: File,
    afterPath: File,
    sourcePath: File?,
): Int {
    val before = read(beforePath)
    val after = read(afterPath)
    var failures = 0

    val xmlError = wellFormedError(after)
    if (xmlError != null) {
        println("FAIL not well-formed XML: $xmlError")
        failures += 1
    }

    val beforeSkeleton = skeleton(before)
    val afterSkeleton = skeleton(after)
    if (beforeSkeleton != afterSkeleton) {
        println(
            "FAIL file changed outside <string> bodies (names, attributes, comments or" +
                " other entries):\n" + firstDifference(beforeSkeleton, afterSkeleton),
        )
        failures += 1
    }

    val sourceStrings = sourcePath?.let { strings(read(it)) }.orEmpty()
    val beforeStrings = strings(before)
    var changed = 0
    for ((name, newBody) in strings(after)) {
        val oldBody = beforeStrings[name]
        if (oldBody == null || oldBody == newBody) {
            continue
        }
        changed += 1
        failures += verifyString(name, oldBody, newBody, sourceStrings[name])
    }

    val verdict = if (failures == 0) "PASS" else "FAIL"
    println("$verdict: $changed string(s) changed, $failures problem(s).")
    return if (failures == 0) 0 else 1
}

/** Print the checks for one changed string and return how many failed. */
fun verifyString(
    name: String,
    oldBody: String,
    newBody: String,
    sourceBody: String?,
): Int {
    var failures = 0
    val oldText = visibleText(oldBody)
    val newText = visibleText(newBody)
    if (oldText != newText) {
        println("FAIL $name: user-visible text changed\n${firstDifference(oldText, newText)}")
        failures += 1
    } else {
        println("ok   $name: visible text identical")
    }

    val newLinks = hrefs(newBody)
    val lostLinks = hrefs(oldBody).filter { it !in newLinks }
    if (lostLinks.isNotEmpty()) {
        println("FAIL $name: link target(s) removed: ${lostLinks.joinToString(", ", "[", "]") { "'$it'" }}")
        failures += 1
    }

    if (sourceBody != null && hasCdata(sourceBody)) {
        if (!hasCdata(newBody)) {
            println("FAIL $name: source uses CDATA but the fix does not")
            failures += 1
        }
        if (escapedTagPattern.containsMatchIn(stripCdata(newBody))) {
            println("FAIL $name: still contains escaped markup (&lt; or &gt;)")
            failures += 1
        }
        val diff = tagDiff(sourceBody, newBody)
        if (diff.isNotEmpty()) {
            println("note $name: markup differs from source: $diff")
        }
    }
    return failures
}

/** Run the command named in [argv] and return the process exit code (2 for bad usage). */
fun main(argv: Array<String>): Int {
    if (argv.size >= 2 && argv[0] == "audit") {
        return audit(File(argv[1]))
    }
    if (argv.size >= 3 && argv[0] == "verify") {
        return verify(File(argv[1]), File(argv[2]), argv.getOrNull(3)?.let(::File))
    }
    System.err.println(
        "usage: kotlin cdata_check.main.kts audit <res-dir>\n" +
            "       kotlin cdata_check.main.kts verify <before.xml> <after.xml> [<source.xml>]",
    )
    return 2
}

exitProcess(main(args))
