package de.geosphere.congregationplaner.importer.data

/** One event of [xmlEvents]; element and attribute names are local names (without namespace prefix). */
internal sealed interface XmlEvent {
    data class Start(val name: String, val attributes: Map<String, String>) : XmlEvent

    data class End(val name: String) : XmlEvent

    data class Text(val text: String) : XmlEvent
}

private val ATTRIBUTE = Regex("""([^\s=]+)\s*=\s*(?:"([^"]*)"|'([^']*)')""")
private val ENTITY = Regex("&(#x[0-9a-fA-F]+|#[0-9]+|amp|lt|gt|quot|apos);")
private const val CDATA_START = "<![CDATA["
private const val HEX = 16

/**
 * A small pull parser for the machine-written XML inside an `.xlsx`: elements, attributes, text, the predefined and
 * numeric entities and CDATA. Processing instructions and comments are skipped; a DTD is rejected, so no external
 * entities are ever resolved. Malformed input fails with an [IllegalStateException].
 */
internal fun xmlEvents(xml: String): Sequence<XmlEvent> = sequence {
    var index = 0
    while (index < xml.length) {
        if (xml[index] != '<') {
            val end = xml.indexOf('<', index).takeIf { it >= 0 } ?: xml.length
            yield(XmlEvent.Text(decodeEntities(xml.substring(index, end))))
            index = end
            continue
        }
        index = when {
            xml.startsWith("<?", index) -> after(xml, "?>", index)
            xml.startsWith("<!--", index) -> after(xml, "-->", index)
            xml.startsWith(CDATA_START, index) -> {
                val end = after(xml, "]]>", index)
                yield(XmlEvent.Text(xml.substring(index + CDATA_START.length, end - "]]>".length)))
                end
            }
            xml.startsWith("<!", index) -> error("Die Datei enthält eine nicht unterstützte DTD.")
            xml.startsWith("</", index) -> {
                val end = after(xml, ">", index)
                yield(XmlEvent.End(localName(xml.substring(index + 2, end - 1).trim())))
                end
            }
            else -> {
                val end = tagEnd(xml, index)
                val body = xml.substring(index + 1, end).trim()
                val selfClosing = body.endsWith("/")
                val content = body.removeSuffix("/").trim()
                val name = content.takeWhile { !it.isWhitespace() }
                check(name.isNotEmpty()) { "Ungültiges XML an Position $index." }
                val attributes = ATTRIBUTE.findAll(content.substring(name.length)).associate { match ->
                    val (attribute, doubleQuoted, singleQuoted) = match.destructured
                    // An empty double-quoted value also lands here; then the single-quoted group is empty too.
                    localName(attribute) to decodeEntities(doubleQuoted.ifEmpty { singleQuoted })
                }
                yield(XmlEvent.Start(localName(name), attributes))
                if (selfClosing) yield(XmlEvent.End(localName(name)))
                end + 1
            }
        }
    }
}

/** Index just behind the next [marker] from [from]. */
private fun after(xml: String, marker: String, from: Int): Int {
    val found = xml.indexOf(marker, from)
    check(found >= 0) { "Ungültiges XML: \"$marker\" fehlt." }
    return found + marker.length
}

/** Index of the `>` closing the tag at [from]; a `>` inside a quoted attribute value does not count. */
private fun tagEnd(xml: String, from: Int): Int {
    var quote: Char? = null
    for (i in from + 1 until xml.length) {
        val char = xml[i]
        when {
            quote != null -> if (char == quote) quote = null
            char == '"' || char == '\'' -> quote = char
            char == '>' -> return i
        }
    }
    error("Ungültiges XML: ein Tag ist nicht geschlossen.")
}

private fun localName(name: String): String = name.substringAfter(':')

private fun decodeEntities(text: String): String = ENTITY.replace(text) { match ->
    when (val entity = match.groupValues[1]) {
        "amp" -> "&"
        "lt" -> "<"
        "gt" -> ">"
        "quot" -> "\""
        "apos" -> "'"
        else -> {
            val code = if (entity.startsWith("#x")) entity.drop(2).toInt(HEX) else entity.drop(1).toInt()
            codePointToString(code)
        }
    }
}

private const val SUPPLEMENTARY_START = 0x10000
private const val HIGH_SURROGATE = 0xD800
private const val LOW_SURROGATE = 0xDC00
private const val SURROGATE_BITS = 10
private const val LOW_MASK = 0x3FF

/** A Unicode code point as a string; above the basic plane as a UTF-16 surrogate pair. */
private fun codePointToString(code: Int): String {
    if (code < SUPPLEMENTARY_START) return code.toChar().toString()
    val offset = code - SUPPLEMENTARY_START
    val high = (HIGH_SURROGATE + (offset shr SURROGATE_BITS)).toChar()
    val low = (LOW_SURROGATE + (offset and LOW_MASK)).toChar()
    return charArrayOf(high, low).concatToString()
}
