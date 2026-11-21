package de.geosphere.congregationplaner.importer.data

import de.geosphere.congregationplaner.importer.domain.model.ImportRow
import de.geosphere.congregationplaner.importer.domain.model.ImportTable
import de.geosphere.congregationplaner.importer.domain.planning.excelSerialToDate

private const val WORKBOOK = "xl/workbook.xml"
private const val WORKBOOK_RELS = "xl/_rels/workbook.xml.rels"
private const val SHARED_STRINGS = "xl/sharedStrings.xml"
private const val SHEET = "xl/worksheets/sheet1.xml"
private const val STYLES = "xl/styles.xml"

// Index into cellXfs of STYLES_XML: built-in number format 14, the short date of the user's locale.
private const val DATE_STYLE = 1
private const val LETTERS = 26
private const val SHEET_NAME = "Tabelle1"
private val PLAIN_NUMBER = Regex("-?(0|[1-9][0-9]{0,14})")

private typealias SheetRow = Pair<Int, Map<Int, String>>

/**
 * Reads and writes the first sheet of an `.xlsx` workbook. An `.xlsx` is a ZIP of XML files; the platform file sources
 * unzip and zip it, this object works on the files inside (path -> bytes), so it runs on every platform.
 */
object Xlsx {
    /**
     * The first sheet as an [ImportTable]. Cell values come back as the text Excel stores, so dates are still serial
     * numbers (see [excelSerialToDate]). The first row is the header; columns without a header are ignored.
     */
    fun readFirstSheet(files: Map<String, ByteArray>): ImportTable {
        val sheetPath = firstSheetPath(files)
        val sharedStrings = files[SHARED_STRINGS]?.let { parseSharedStrings(it.decodeToString()) }.orEmpty()
        val sheet = files[sheetPath] ?: error("Das Tabellenblatt $sheetPath fehlt in der Datei.")
        return toTable(parseSheet(sheet.decodeToString(), sharedStrings))
    }

    /**
     * A workbook with one sheet: [table]'s headers in the first row, then its rows in order. Whole numbers without a
     * leading zero become number cells, everything else text cells. Number cells in the columns [dateHeaders] (Excel
     * serial dates, see [excelSerialToDate]) are shown as dates.
     */
    fun write(table: ImportTable, dateHeaders: Set<String> = emptySet()): Map<String, ByteArray> = mapOf(
        "[Content_Types].xml" to CONTENT_TYPES,
        "_rels/.rels" to ROOT_RELS,
        WORKBOOK to WORKBOOK_XML,
        WORKBOOK_RELS to WORKBOOK_RELS_XML,
        STYLES to STYLES_XML,
        SHEET to sheetXml(table, dateHeaders),
    ).mapValues { it.value.encodeToByteArray() }

    private fun sheetXml(table: ImportTable, dateHeaders: Set<String>): String = buildString {
        append(XML_HEADER)
        append("<worksheet xmlns=\"$MAIN_NS\"><sheetData>")
        val dateColumns = table.headers.indices.filter { table.headers[it] in dateHeaders }.toSet()
        appendRow(1, table.headers, emptySet())
        table.rows.forEachIndexed { index, row ->
            appendRow(index + 2, table.headers.map { row.text(it).orEmpty() }, dateColumns)
        }
        append("</sheetData></worksheet>")
    }

    private fun StringBuilder.appendRow(number: Int, values: List<String>, dateColumns: Set<Int>) {
        append("<row r=\"$number\">")
        values.forEachIndexed { column, value ->
            if (value.isEmpty()) return@forEachIndexed
            val reference = columnName(column) + number
            if (PLAIN_NUMBER.matches(value)) {
                val style = if (column in dateColumns) " s=\"$DATE_STYLE\"" else ""
                append("<c r=\"$reference\"$style><v>$value</v></c>")
            } else {
                append("<c r=\"$reference\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                append(escape(value))
                append("</t></is></c>")
            }
        }
        append("</row>")
    }

    /** The first `<sheet>` of the workbook, resolved through its relationship id to the worksheet file. */
    private fun firstSheetPath(files: Map<String, ByteArray>): String {
        val workbook = files[WORKBOOK] ?: error("Keine gültige .xlsx-Datei (xl/workbook.xml fehlt).")
        val relId = xmlEvents(workbook.decodeToString())
            .firstNotNullOfOrNull { (it as? XmlEvent.Start)?.takeIf { e -> e.name == "sheet" }?.attributes?.get("id") }
            ?: error("Die Datei enthält kein Tabellenblatt.")
        val rels = files[WORKBOOK_RELS] ?: error("Keine gültige .xlsx-Datei (Beziehungen fehlen).")
        val target = xmlEvents(rels.decodeToString())
            .filterIsInstance<XmlEvent.Start>()
            .firstOrNull { it.name == "Relationship" && it.attributes["Id"] == relId }
            ?.attributes?.get("Target")
            ?: error("Das Tabellenblatt der Datei ist nicht auffindbar.")
        return "xl/" + target.removePrefix("/xl/")
    }

    private fun parseSharedStrings(xml: String): List<String> {
        val strings = mutableListOf<String>()
        var current: StringBuilder? = null
        var inText = false
        xmlEvents(xml).forEach { event ->
            when (event) {
                is XmlEvent.Start -> when (event.name) {
                    "si" -> current = StringBuilder()
                    "t" -> inText = true
                }
                is XmlEvent.Text -> if (inText) current?.append(event.text)
                is XmlEvent.End -> when (event.name) {
                    "t" -> inText = false
                    "si" -> {
                        strings += current?.toString().orEmpty()
                        current = null
                    }
                }
            }
        }
        return strings
    }

    /** Rows as column index -> text; shared strings are resolved, numbers stay as stored. */
    private fun parseSheet(xml: String, sharedStrings: List<String>): List<SheetRow> {
        val rows = mutableListOf<SheetRow>()
        val reader = SheetReader(sharedStrings)
        xmlEvents(xml).forEach { event -> reader.onEvent(event)?.let(rows::add) }
        return rows
    }

    private fun toTable(rows: List<SheetRow>): ImportTable {
        val headers = rows.firstOrNull()?.second.orEmpty()
            .mapValues { it.value.trim() }
            .filterValues { it.isNotEmpty() }
            .entries.sortedBy { it.key }
        val dataRows = rows.drop(1).map { (number, cells) ->
            ImportRow(number, headers.mapNotNull { (index, name) -> cells[index]?.let { name to it } }.toMap())
        }
        return ImportTable(headers.map { it.value }, dataRows)
    }
}

/** Collects the cells of `<row>` elements; [onEvent] returns a row when it ends. */
private class SheetReader(private val sharedStrings: List<String>) {
    private var rowNumber = 0
    private var cells = mutableMapOf<Int, String>()
    private var column = 0
    private var type: String? = null
    private var text: StringBuilder? = null
    private var inText = false

    fun onEvent(event: XmlEvent): SheetRow? {
        when (event) {
            is XmlEvent.Start -> onStart(event)
            is XmlEvent.Text -> if (inText) text?.append(event.text)
            is XmlEvent.End -> return onEnd(event.name)
        }
        return null
    }

    private fun onStart(event: XmlEvent.Start) {
        when (event.name) {
            "row" -> {
                rowNumber = event.attributes["r"]?.toIntOrNull() ?: (rowNumber + 1)
                cells = mutableMapOf()
            }
            "c" -> {
                column = columnIndex(event.attributes["r"].orEmpty())
                type = event.attributes["t"]
            }
            "v" -> {
                text = StringBuilder()
                inText = true
            }
            "is" -> text = StringBuilder()
            "t" -> inText = text != null
        }
    }

    private fun onEnd(name: String): SheetRow? {
        when (name) {
            "v" -> {
                val raw = text.toString()
                cells[column] = if (type == "s") sharedStrings.getOrElse(raw.trim().toInt()) { "" } else raw
                endText()
            }
            "t" -> inText = false
            "is" -> {
                cells[column] = text.toString()
                endText()
            }
            "row" -> return rowNumber to cells
        }
        return null
    }

    private fun endText() {
        text = null
        inText = false
    }
}

/** `A1` -> 0, `B7` -> 1, `AA3` -> 26. */
private fun columnIndex(reference: String): Int = reference.takeWhile(Char::isLetter)
    .fold(0) { acc, letter -> acc * LETTERS + (letter.uppercaseChar() - 'A' + 1) } - 1

/** 0 -> `A`, 25 -> `Z`, 26 -> `AA`. */
private fun columnName(index: Int): String {
    var rest = index + 1
    return buildString {
        while (rest > 0) {
            val letter = (rest - 1) % LETTERS
            insert(0, 'A' + letter)
            rest = (rest - 1) / LETTERS
        }
    }
}

/** Escapes text for XML and drops control characters XML 1.0 does not allow. */
private fun escape(text: String): String = buildString {
    text.forEach { char ->
        when {
            char == '&' -> append("&amp;")
            char == '<' -> append("&lt;")
            char == '>' -> append("&gt;")
            char == '"' -> append("&quot;")
            char < ' ' && char != '\t' && char != '\n' && char != '\r' -> Unit
            else -> append(char)
        }
    }
}

private const val XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
private const val MAIN_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
private const val REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

private const val CONTENT_TYPES = XML_HEADER +
    "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
    "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
    "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
    "<Override PartName=\"/xl/workbook.xml\" " +
    "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
    "<Override PartName=\"/xl/styles.xml\" " +
    "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>" +
    "<Override PartName=\"/xl/worksheets/sheet1.xml\" " +
    "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
    "</Types>"

private const val ROOT_RELS = XML_HEADER +
    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
    "<Relationship Id=\"rId1\" Type=\"$REL_NS/officeDocument\" Target=\"xl/workbook.xml\"/>" +
    "</Relationships>"

private const val WORKBOOK_XML = XML_HEADER +
    "<workbook xmlns=\"$MAIN_NS\" xmlns:r=\"$REL_NS\">" +
    "<sheets><sheet name=\"$SHEET_NAME\" sheetId=\"1\" r:id=\"rId1\"/></sheets>" +
    "</workbook>"

private const val WORKBOOK_RELS_XML = XML_HEADER +
    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
    "<Relationship Id=\"rId1\" Type=\"$REL_NS/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
    "<Relationship Id=\"rId2\" Type=\"$REL_NS/styles\" Target=\"styles.xml\"/>" +
    "</Relationships>"

private const val STYLES_XML = XML_HEADER +
    "<styleSheet xmlns=\"$MAIN_NS\">" +
    "<fonts count=\"1\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>" +
    "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill>" +
    "<fill><patternFill patternType=\"gray125\"/></fill></fills>" +
    "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
    "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
    "<cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
    "<xf numFmtId=\"14\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/></cellXfs>" +
    "</styleSheet>"
