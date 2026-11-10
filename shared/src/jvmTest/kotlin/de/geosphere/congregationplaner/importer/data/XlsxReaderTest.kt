package de.geosphere.congregationplaner.importer.data

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class XlsxReaderTest {
    private val workbook = """
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
            xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
          <sheets>
            <sheet name="Erstes" sheetId="1" r:id="rId1"/>
            <sheet name="Zweites" sheetId="2" r:id="rId2"/>
          </sheets>
        </workbook>
    """.trimIndent()

    private val rels = """
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId2" Type="worksheet" Target="worksheets/sheet2.xml"/>
          <Relationship Id="rId1" Type="worksheet" Target="/xl/worksheets/sheet1.xml"/>
        </Relationships>
    """.trimIndent()

    private val sharedStrings = """
        <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <si><t>Nummer</t></si>
          <si><t>Titel</t></si>
          <si><t>Müller &amp; Söhne</t></si>
          <si><r><t>Teil </t></r><r><t>eins</t></r></si>
        </sst>
    """.trimIndent()

    // Row 3 skips column B (sparse cells) and row 4 has a value in a column without a header.
    private val sheet1 = """
        <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <sheetData>
            <row r="1">
              <c r="A1" t="s"><v>0</v></c><c r="B1" t="s"><v>1</v></c><c r="D1" t="inlineStr"><is><t> Datum </t></is></c>
            </row>
            <row r="2">
              <c r="A2"><v>12</v></c><c r="B2" t="s"><v>2</v></c><c r="D2"><v>44927</v></c>
            </row>
            <row r="3">
              <c r="A3"><v>13</v></c><c r="D3" t="inlineStr"><is><t>inline</t></is></c>
            </row>
            <row r="4">
              <c r="A4"><v>14</v></c><c r="B4" t="s"><v>3</v></c><c r="C4"><v>ignored</v></c>
            </row>
          </sheetData>
        </worksheet>
    """.trimIndent()

    private val sheet2 = """
        <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <sheetData><row r="1"><c r="A1" t="inlineStr"><is><t>Anderes</t></is></c></row></sheetData>
        </worksheet>
    """.trimIndent()

    private fun xlsx(files: Map<String, String>): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            files.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    private val standardFiles = mapOf(
        "xl/workbook.xml" to workbook,
        "xl/_rels/workbook.xml.rels" to rels,
        "xl/sharedStrings.xml" to sharedStrings,
        "xl/worksheets/sheet1.xml" to sheet1,
        "xl/worksheets/sheet2.xml" to sheet2,
    )

    @Test
    fun readsHeadersOfTheFirstSheetOnlyAndTrimsThem() {
        val table = XlsxReader.readFirstSheet(xlsx(standardFiles))

        assertEquals(listOf("Nummer", "Titel", "Datum"), table.headers)
    }

    @Test
    fun resolvesSharedStringsInlineStringsAndNumbers() {
        val rows = XlsxReader.readFirstSheet(xlsx(standardFiles)).rows

        assertEquals(3, rows.size)
        assertEquals("12", rows[0].text("Nummer"))
        assertEquals("Müller & Söhne", rows[0].text("Titel"))
        assertEquals("44927", rows[0].text("Datum"))
        assertEquals("Teil eins", rows[2].text("Titel"))
    }

    @Test
    fun sparseCellsAreMissingNotShifted() {
        val row = XlsxReader.readFirstSheet(xlsx(standardFiles)).rows[1]

        assertNull(row.text("Titel"))
        assertEquals("inline", row.text("Datum"))
    }

    @Test
    fun keepsTheRowNumbersOfTheFileAndIgnoresColumnsWithoutHeader() {
        val rows = XlsxReader.readFirstSheet(xlsx(standardFiles)).rows

        assertEquals(listOf(2, 3, 4), rows.map { it.number })
        assertNull(rows[2].text("C"))
    }

    @Test
    fun worksWithoutSharedStringsFile() {
        val table = XlsxReader.readFirstSheet(
            xlsx(
                standardFiles - "xl/sharedStrings.xml" + ("xl/worksheets/sheet1.xml" to sheet2),
            ),
        )

        assertEquals(listOf("Anderes"), table.headers)
        assertEquals(emptyList(), table.rows)
    }

    @Test
    fun rejectsAFileThatIsNoWorkbook() {
        assertFailsWith<IllegalStateException> { XlsxReader.readFirstSheet(xlsx(mapOf("readme.txt" to "hi"))) }
    }

    @Test
    fun rejectsAWorkbookWithoutSheets() {
        val files = standardFiles + ("xl/workbook.xml" to "<workbook xmlns=\"x\"><sheets/></workbook>")

        assertFailsWith<IllegalStateException> { XlsxReader.readFirstSheet(xlsx(files)) }
    }
}
