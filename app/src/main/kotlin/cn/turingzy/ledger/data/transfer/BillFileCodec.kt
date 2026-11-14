package cn.turingzy.ledger.data.transfer

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

object BillFileCodec {
    const val MAX_BYTES = 32 * 1024 * 1024
    private const val MAX_ROWS = 100_000

    fun read(bytes: ByteArray): List<List<String>> {
        require(bytes.size <= MAX_BYTES) { "文件不能超过 32 MB" }
        return if (bytes.take(2) == listOf(80.toByte(), 75.toByte())) readWorkbook(bytes) else {
            require(!bytes.take(2).equals(listOf(0xD0.toByte(), 0xCF.toByte()))) { "请将旧版 XLS 另存为 XLSX 或 CSV" }
            val utf8 = bytes.toString(Charsets.UTF_8)
            readCsv(if ('\uFFFD' in utf8) bytes.toString(charset("GB18030")) else utf8)
        }
    }

    fun readCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val value = StringBuilder()
        var quoted = false
        var index = 0
        val input = text.removePrefix("\uFEFF")
        while (index < input.length) {
            val char = input[index]
            when {
                char == '"' && quoted && input.getOrNull(index + 1) == '"' -> { value.append('"'); index++ }
                char == '"' -> quoted = !quoted
                char == ',' && !quoted -> { row.add(value.toString()); value.clear() }
                (char == '\n' || char == '\r') && !quoted -> {
                    row.add(value.toString()); value.clear(); rows.add(row); row = mutableListOf()
                    if (char == '\r' && input.getOrNull(index + 1) == '\n') index++
                    require(rows.size <= MAX_ROWS) { "账单行数过多" }
                }
                else -> value.append(char)
            }
            index++
        }
        require(!quoted) { "CSV 引号未闭合" }
        if (value.isNotEmpty() || row.isNotEmpty()) { row.add(value.toString()); rows.add(row) }
        return rows
    }

    fun csv(rows: List<List<String>>): ByteArray = ("\uFEFF" + rows.joinToString("\r\n") { row ->
        row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
    }).toByteArray(Charsets.UTF_8)

    fun workbook(rows: List<List<String>>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            fun entry(name: String, text: String) {
                zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray(Charsets.UTF_8)); zip.closeEntry()
            }
            entry("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>""")
            entry("_rels/.rels", """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
            entry("xl/workbook.xml", """<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="账单" sheetId="1" r:id="rId1"/></sheets></workbook>""")
            entry("xl/_rels/workbook.xml.rels", """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>""")
            entry("xl/worksheets/sheet1.xml", buildString {
                append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
                rows.forEachIndexed { index, row ->
                    append("<row r=\"${index + 1}\">")
                    row.forEach { append("<c t=\"inlineStr\"><is><t xml:space=\"preserve\">${xml(it)}</t></is></c>") }
                    append("</row>")
                }
                append("</sheetData></worksheet>")
            })
        }
        return output.toByteArray()
    }

    private fun xml(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .filter { it >= ' ' || it == '\n' || it == '\r' || it == '\t' }

    private fun readWorkbook(bytes: ByteArray): List<List<String>> {
        val parts = mutableMapOf<String, ByteArray>()
        var total = 0
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val data = zip.readBytesLimited(MAX_BYTES - total)
                    total += data.size
                    parts[entry.name] = data
                }
                require(parts.size <= 1000) { "Excel 文件内容过多" }
                entry = zip.nextEntry
            }
        }
        fun document(data: ByteArray): org.w3c.dom.Document {
            require(data.none { it == 0.toByte() }) { "请使用 UTF-8 编码的 Excel XML" }
            require(!data.toString(Charsets.UTF_8).contains("<!DOCTYPE", ignoreCase = true)) { "不支持包含 DTD 的文件" }
            val factory = DocumentBuilderFactory.newInstance()
            factory.isExpandEntityReferences = false
            val builder = factory.newDocumentBuilder()
            builder.setEntityResolver { _, _ -> throw org.xml.sax.SAXException("不支持外部实体") }
            return builder.parse(ByteArrayInputStream(data))
        }
        val strings = parts["xl/sharedStrings.xml"]?.let { data ->
            val nodes = document(data).getElementsByTagName("si")
            (0 until nodes.length).map { nodes.item(it).textContent }
        }.orEmpty()
        val book = document(requireNotNull(parts["xl/workbook.xml"]) { "无效的 XLSX 文件" })
        require(book.getElementsByTagName("workbookPr").let { nodes ->
            nodes.length == 0 || (nodes.item(0) as org.w3c.dom.Element).getAttribute("date1904") !in setOf("1", "true")
        }) { "请将 Excel 日期转换为文本后导入（不支持 1904 日期系统）" }
        val first = book.getElementsByTagName("sheet").item(0) as? org.w3c.dom.Element
        val relationId = first?.getAttribute("r:id")
        val relations = document(requireNotNull(parts["xl/_rels/workbook.xml.rels"])).getElementsByTagName("Relationship")
        val target = (0 until relations.length).map { relations.item(it) as org.w3c.dom.Element }
            .firstOrNull { it.getAttribute("Id") == relationId }?.getAttribute("Target")
            ?: error("Excel 中没有工作表")
        val path = if (target.startsWith('/')) target.removePrefix("/") else "xl/$target"
        val sheet = document(requireNotNull(parts[path]) { "找不到 Excel 工作表" })
        val rowNodes = sheet.getElementsByTagName("row")
        val rows = mutableListOf<List<String>>()
        for (index in 0 until rowNodes.length) {
            val row = rowNodes.item(index) as org.w3c.dom.Element
            val rowNumber = row.getAttribute("r").toIntOrNull() ?: (rows.size + 1)
            require(rowNumber in 1..MAX_ROWS) { "Excel 行号超出范围" }
            require(rowNumber > rows.size) { "Excel 行号重复或顺序无效" }
            while (rows.size < rowNumber - 1) rows.add(emptyList())
            val cells = row.getElementsByTagName("c")
            val values = mutableListOf<String>()
            for (cellIndex in 0 until cells.length) {
                val cell = cells.item(cellIndex) as org.w3c.dom.Element
                val letters = cell.getAttribute("r").takeWhile { it.isLetter() }
                val column = if (letters.isEmpty()) values.size else letters.fold(0) { acc, c -> acc * 26 + (c - 'A' + 1) } - 1
                require(column in 0..16383) { "Excel 列号超出范围" }
                while (values.size <= column) values.add("")
                val raw = cell.getElementsByTagName("v").item(0)?.textContent.orEmpty()
                values[column] = when (cell.getAttribute("t")) {
                    "s" -> strings.getOrNull(raw.toIntOrNull() ?: -1) ?: error("无效的共享字符串")
                    "inlineStr" -> cell.getElementsByTagName("is").item(0)?.textContent.orEmpty()
                    else -> raw
                }
            }
            rows.add(values)
        }
        return rows
    }

    fun amount(value: String): Long = value.trim().replace(",", "").removePrefix("¥").removePrefix("￥")
        .toBigDecimal().movePointRight(2).longValueExact()

    fun timestamp(value: String): Long {
        val text = value.trim()
        val patterns = listOf("uuuu-MM-dd HH:mm:ss", "uuuu-MM-dd HH:mm", "uuuu/MM/dd HH:mm:ss", "uuuu/MM/dd HH:mm", "uuuu.MM.dd HH:mm:ss")
        for (pattern in patterns) {
            val date = runCatching { LocalDateTime.parse(text, DateTimeFormatter.ofPattern(pattern).withResolverStyle(java.time.format.ResolverStyle.STRICT)) }.getOrNull()
            if (date != null) return date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        val iso = runCatching { LocalDateTime.parse(text) }.getOrNull()
        if (iso != null) return iso.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val date = runCatching { LocalDate.parse(text.replace('/', '-').replace('.', '-')) }.getOrNull()
        if (date != null) return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val serial = text.toBigDecimalOrNull() ?: error("无法识别交易时间：$text")
        require(serial >= BigDecimal.ONE && serial < BigDecimal("2958466")) { "无效的 Excel 日期" }
        val seconds = serial.multiply(BigDecimal(86400)).toLong()
        return LocalDate.of(1899, 12, 30).atStartOfDay().plusSeconds(seconds).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}

internal fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        require(output.size() + count <= limit) { "文件内容超过大小限制" }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
