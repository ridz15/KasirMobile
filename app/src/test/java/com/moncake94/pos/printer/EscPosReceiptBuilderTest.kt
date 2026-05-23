package com.moncake94.pos.printer

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class EscPosReceiptBuilderTest {
    @Test
    fun testReceiptContainsStoreAndVariationItems() {
        val text = String(EscPosReceiptBuilder().buildTestReceipt(), Charset.forName("CP437"))

        assertTrue(text.contains("moncake94"))
        assertTrue(text.contains("IG: @moncake94"))
        assertTrue(text.contains("WA: 087877530387"))
        assertTrue(text.contains("Marmer Cake - Whole"))
        assertTrue(text.contains("Bolu Tape"))
    }
}
