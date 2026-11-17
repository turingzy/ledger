package cn.turingzy.ledger.model.transfer

data class BillImportResult(val imported: Int = 0, val duplicates: Int = 0, val possibleDuplicates: Int = 0)
