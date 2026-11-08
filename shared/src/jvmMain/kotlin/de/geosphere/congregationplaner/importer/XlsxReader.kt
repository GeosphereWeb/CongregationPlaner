package de.geosphere.congregationplaner.importer

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader

private const val WORKBOOK = "xl/workbook.xml"
private const val WORKBOOK_RELS = "xl/_rels/workbook.xml.rels"
private const val SHARED_STRINGS = "xl/sharedStrings.xml"
private const val LETTERS = 26

private typealias SheetRow = Pair<Int, Map<Int, String>>

/**
 * Reads the first sheet of an `.xlsx` file into an [ImportTable]. An `.xlsx` is a ZIP of XML files, so the JDK is
 * enough; no spreadsheet library is needed. Cell values come back as the text Excel stores, so dates are still serial
 * numbers (see [excelSerialToDate]). The first row is the header; columns without a header are ignored.
 */
object XlsxReader {
    fun readFirstSheet(bytes: ByteArray): ImportTable {
        val files = unzip(bytes)
        val sheetPath = firstSheetPath(files)
        val sharedStrings = files[SHARED_STRINGS]?.let(::parseSharedStrings).orEmpty()
        val sheet = files[sheetPath] ?: error("Das Tabellenblatt $sheetPath fehlt in der Datei.")
        return toTable(parseSheet(sheet, sharedStrings))
    }

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> {
        val files = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                if (!entry.isDirectory) files[entry.name] = zip.readBytes()
            }
        }
        return files
    }

    private fun reader(bytes: ByteArray): XMLStreamReader {
        val factory = XMLInputFactory.newFactory()
        // The files come from the user's disk, but never resolve DTDs or external entities anyway.
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false)
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
        return factory.createXMLStreamReader(ByteArrayInputStream(bytes))
    }

    /** The first `<sheet>` of the workbook, resolved through its relationship id to the worksheet file. */
    private fun firstSheetPath(files: Map<String, ByteArray>): String {
        val workbook = files[WORKBOOK] ?: error("Keine gültige .xlsx-Datei (xl/workbook.xml fehlt).")
        val relId = firstSheetRelationshipId(workbook) ?: error("Die Datei enthält kein Tabellenblatt.")
        val rels = files[WORKBOOK_RELS] ?: error("Keine gültige .xlsx-Datei (Beziehungen fehlen).")
        val target = relationshipTarget(rels, relId) ?: error("Das Tabellenblatt der Datei ist nicht auffindbar.")
        return "xl/" + target.removePrefix("/xl/")
    }

    private fun firstSheetRelationshipId(workbook: ByteArray): String? {
        val xml = reader(workbook)
        while (xml.hasNext()) {
            if (xml.next() == XMLStreamConstants.START_ELEMENT && xml.localName == "sheet") {
                return xml.attributeByLocalName("id")
            }
        }
        return null
    }

    private fun relationshipTarget(rels: ByteArray, id: String): String? {
        val xml = reader(rels)
        while (xml.hasNext()) {
            val isMatch = xml.next() == XMLStreamConstants.START_ELEMENT &&
                xml.localName == "Relationship" &&
                xml.attributeByLocalName("Id") == id
            if (isMatch) return xml.attributeByLocalName("Target")
        }
        return null
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        val xml = reader(bytes)
        var current: StringBuilder? = null
        while (xml.hasNext()) {
            when (xml.next()) {
                XMLStreamConstants.START_ELEMENT -> when (xml.localName) {
                    "si" -> current = StringBuilder()
                    "t" -> current?.append(xml.elementText)
                }
                XMLStreamConstants.END_ELEMENT -> if (xml.localName == "si") {
                    strings += current?.toString().orEmpty()
                    current = null
                }
            }
        }
        return strings
    }

    /** Rows as column index -> text; shared strings are resolved, numbers stay as stored. */
    private fun parseSheet(bytes: ByteArray, sharedStrings: List<String>): List<SheetRow> {
        val rows = mutableListOf<SheetRow>()
        val xml = reader(bytes)
        var rowNumber = 0
        var cells = mutableMapOf<Int, String>()
        var column = 0
        var type: String? = null
        while (xml.hasNext()) {
            when (xml.next()) {
                XMLStreamConstants.START_ELEMENT -> when (xml.localName) {
                    "row" -> {
                        rowNumber = xml.attributeByLocalName("r")?.toIntOrNull() ?: (rowNumber + 1)
                        cells = mutableMapOf()
                    }
                    "c" -> {
                        column = columnIndex(xml.attributeByLocalName("r").orEmpty())
                        type = xml.attributeByLocalName("t")
                    }
                    "v" -> {
                        val raw = xml.elementText
                        cells[column] = if (type == "s") sharedStrings.getOrElse(raw.toInt()) { "" } else raw
                    }
                    "is" -> cells[column] = readInlineString(xml)
                }
                XMLStreamConstants.END_ELEMENT -> if (xml.localName == "row") rows += rowNumber to cells
            }
        }
        return rows
    }

    private fun readInlineString(xml: XMLStreamReader): String {
        val text = StringBuilder()
        while (xml.hasNext()) {
            val event = xml.next()
            if (event == XMLStreamConstants.START_ELEMENT && xml.localName == "t") text.append(xml.elementText)
            if (event == XMLStreamConstants.END_ELEMENT && xml.localName == "is") break
        }
        return text.toString()
    }

    private fun toTable(rows: List<SheetRow>): ImportTable {
        val headers = rows.firstOrNull()?.second.orEmpty()
            .mapValues { it.value.trim() }
            .filterValues { it.isNotEmpty() }
            .toSortedMap()
        val dataRows = rows.drop(1).map { (number, cells) ->
            ImportRow(number, headers.mapNotNull { (index, name) -> cells[index]?.let { name to it } }.toMap())
        }
        return ImportTable(headers.values.toList(), dataRows)
    }

    /** `A1` -> 0, `B7` -> 1, `AA3` -> 26. */
    private fun columnIndex(reference: String): Int = reference.takeWhile(Char::isLetter)
        .fold(0) { acc, letter -> acc * LETTERS + (letter.uppercaseChar() - 'A' + 1) } - 1

    private fun XMLStreamReader.attributeByLocalName(name: String): String? =
        (0 until attributeCount).firstOrNull { getAttributeLocalName(it) == name }?.let(::getAttributeValue)
}
