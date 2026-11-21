package de.geosphere.congregationplaner.importer.data

import de.geosphere.congregationplaner.importer.data.XmlEvent.End
import de.geosphere.congregationplaner.importer.data.XmlEvent.Start
import de.geosphere.congregationplaner.importer.data.XmlEvent.Text
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class XmlEventsTest {
    private fun events(xml: String) = xmlEvents(xml).toList()

    @Test
    fun elementsAttributesAndTextUseLocalNames() {
        val xml = """<?xml version="1.0"?><x:a x:id="1" b='zwei'><c/>Text</x:a>"""

        val expected = listOf(
            Start("a", mapOf("id" to "1", "b" to "zwei")),
            Start("c", emptyMap()),
            End("c"),
            Text("Text"),
            End("a"),
        )
        assertEquals(expected, events(xml))
    }

    @Test
    fun decodesPredefinedAndNumericEntities() {
        val text = events("<t>&amp;&lt;&gt;&quot;&apos;&#228;&#xFC;&#x1F600;</t>")[1]

        assertEquals(Text("&<>\"'äü😀"), text)
    }

    @Test
    fun cdataIsTextAndCommentsAreSkipped() {
        assertEquals(
            listOf(Start("t", emptyMap()), Text("<roh> & ok"), End("t")),
            events("<t><!-- egal --><![CDATA[<roh> & ok]]></t>"),
        )
    }

    @Test
    fun aGreaterThanInsideAnAttributeDoesNotEndTheTag() {
        assertEquals(Start("a", mapOf("f" to "x>y")), events("""<a f="x>y"></a>""").first())
    }

    @Test
    fun rejectsDtdsAndUnclosedTags() {
        assertFailsWith<IllegalStateException> { events("<!DOCTYPE x [<!ENTITY e SYSTEM \"file:///\">]><x/>") }
        assertFailsWith<IllegalStateException> { events("<a b=\"1\"") }
    }
}
