package com.moncake94.pos.printer

import com.moncake94.pos.data.CartItem
import com.moncake94.pos.data.BestSeller
import com.moncake94.pos.data.DailyReport
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.nio.charset.Charset

class EscPosReceiptBuilderTest {
    @Test
    fun testReceiptContainsStoreAndVariationItems() {
        val text = receiptText(EscPosReceiptBuilder().buildTestReceipt())

        assertTrue(text.contains("moncake94"))
        assertTrue(text.contains("IG: @moncake94"))
        assertTrue(text.contains("WA: 087877530387"))
        assertTrue(text.contains("Marmer Cake"))
        assertTrue(text.contains("  Whole"))
        assertTrue(text.contains("Bolu Tape"))
        assertTrue(text.contains("Qty 1 @ Rp120.000"))
        assertTrue(text.contains("TOTAL"))
    }

    @Test
    fun testReceiptLinesStayWithin58mmWidth() {
        val text = receiptText(EscPosReceiptBuilder().buildTestReceipt())
        assertEveryLineFits58mm(text)
    }

    @Test
    fun testLongWordsAreWrappedWithoutDroppingCharacters() {
        val longName = "ProdukSuperPanjangTanpaSpasiABCDE12345"
        val text = receiptText(
            EscPosReceiptBuilder().buildCartReceipt(
                items = listOf(
                    CartItem(
                        key = "long",
                        productId = 1,
                        productName = longName,
                        variationName = "Ukuran",
                        optionName = "WholeSuperPanjangTanpaSpasiABCDE",
                        price = 120_000,
                        quantity = 1
                    )
                ),
                paymentAmount = 120_000,
                changeAmount = 0
            )
        )

        assertTrue(text.contains("ProdukSuperPanjangTanpaSpasiAB"))
        assertTrue(text.contains("E12345"))
        assertTrue(text.contains("  Ukuran: WholeSuperPanjangTanpa"))
        assertTrue(text.contains("  SpasiABCDE"))
        assertEveryLineFits58mm(text)
    }

    @Test
    fun testClosingReportLinesStayWithin58mmWidth() {
        val report = DailyReport(
            dateMillis = 1_779_475_200_000,
            grossSales = 1_500_000,
            discountTotal = 50_000,
            voidTotal = 120_000,
            refundTotal = 30_000,
            netSales = 1_350_000,
            successCount = 12,
            voidCount = 1,
            refundCount = 1,
            tunaiTotal = 850_000,
            qrisTotal = 500_000,
            tunaiCount = 7,
            qrisCount = 5,
            itemSoldCount = 28,
            bestSellers = listOf(BestSeller("BoluKukusKetanHitamKejuLumerSuperPanjang", 12)),
            transactions = emptyList()
        )

        assertEveryLineFits58mm(receiptText(EscPosReceiptBuilder().buildClosingReport(report)))
    }

    private fun receiptText(bytes: ByteArray): String {
        val raw = String(bytes, Charset.forName("CP437"))
        return raw
            .replace(Regex("\u001B[@Ea][\u0000-\u0001]?"), "")
            .replace(Regex("\u001DV[B]\u0000"), "")
    }

    private fun assertEveryLineFits58mm(text: String) {
        val tooLong = text
            .lines()
            .filter { it.isNotBlank() }
            .filter { it.length > 30 }

        if (tooLong.isNotEmpty()) {
            fail("Ada baris struk lebih dari 30 karakter: $tooLong")
        }
    }
}
