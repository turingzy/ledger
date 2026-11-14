package cn.turingzy.ledger.data.transfer

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BillFileCodecTest {
    @Test fun csvRoundTripPreservesUnicodeQuotesNewlinesAndEmptyCells() {
        val rows = listOf(listOf("交易时间", "金额", "备注"), listOf("2026-11-14", "0.01", "咖啡,\"冰\"\n第二行"), listOf("", "", ""))
        assertEquals(rows, BillFileCodec.read(BillFileCodec.csv(rows)))
    }
    @Test fun workbookRoundTripPreservesExactTextAndMoney() {
        val rows = listOf(listOf("金额", "备注"), listOf("92233720368547758.07", "<测试>&\n备注"), listOf("0.01", ""))
        assertEquals(rows, BillFileCodec.read(BillFileCodec.workbook(rows)))
    }
    @Test fun moneyUsesExactCentsAtLongBoundary() {
        assertEquals(Long.MAX_VALUE, BillFileCodec.amount("92233720368547758.07"))
        assertEquals(123456L, BillFileCodec.amount("￥1,234.56"))
        assertEquals(-1L, BillFileCodec.amount("-0.01"))
        assertThrows(ArithmeticException::class.java) { BillFileCodec.amount("0.001") }
        assertThrows(ArithmeticException::class.java) { BillFileCodec.amount("92233720368547758.08") }
    }
    @Test fun rejectsUnclosedCsvAndLegacyExcel() {
        assertThrows(IllegalArgumentException::class.java) { BillFileCodec.readCsv("a,\"broken") }
        assertThrows(IllegalArgumentException::class.java) { BillFileCodec.read(byteArrayOf(0xD0.toByte(), 0xCF.toByte())) }
    }
    @Test fun excelSerialDatesMatchTextDates() {
        assertEquals(BillFileCodec.timestamp("2024-01-01 12:00:00"), BillFileCodec.timestamp("45292.5"))
    }
    @Test fun readsSharedStringsSparseCellsAndNumericExcelDates() {
        val parts = mapOf(
            "xl/workbook.xml" to """<workbook xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="账单" sheetId="1" r:id="rId7"/></sheets></workbook>""",
            "xl/_rels/workbook.xml.rels" to """<Relationships><Relationship Id="rId7" Target="worksheets/bills.xml"/></Relationships>""",
            "xl/sharedStrings.xml" to """<sst><si><t>交易时间</t></si><si><t>金额</t></si><si><r><t>备注</t></r><r><t>文本</t></r></si></sst>""",
            "xl/worksheets/bills.xml" to """<worksheet><sheetData><row r="1"><c r="A1" t="s"><v>0</v></c><c r="C1" t="s"><v>1</v></c></row><row r="3"><c r="A3"><v>45292.5</v></c><c r="C3"><v>1.23</v></c><c r="D3" t="s"><v>2</v></c></row></sheetData></worksheet>""",
        )
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            parts.forEach { (name, xml) -> zip.putNextEntry(ZipEntry(name)); zip.write(xml.toByteArray()); zip.closeEntry() }
        }
        assertEquals(listOf(listOf("交易时间", "", "金额"), emptyList<String>(), listOf("45292.5", "", "1.23", "备注文本")), BillFileCodec.read(bytes.toByteArray()))
    }
    @Test fun workbookCannotLoadExternalEntities() {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use {
            it.putNextEntry(ZipEntry("xl/workbook.xml"))
            it.write("<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///etc/passwd'>]><workbook>&e;</workbook>".toByteArray())
            it.closeEntry()
        }
        assertThrows(IllegalArgumentException::class.java) { BillFileCodec.read(bytes.toByteArray()) }
    }
}
