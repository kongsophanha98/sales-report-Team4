
package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.SaleTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CsvTransactionParserTest {

    private CsvTransactionParser parser;

    @BeforeEach
    void setUp() {
        parser = new CsvTransactionParser();
    }

    @Test
    void parsesValidRow() throws InvalidRowException {
        String line = "PNH,2026-09-01,PNH-000001,SKU-1001,"
                + "Jasmine Rice 5kg,Grocery,2,6.50,0.00,KHQR";

        SaleTransaction transaction = parser.parse(line, 2);

        assertNotNull(transaction);
        assertEquals("PNH", transaction.branch());
        assertEquals(2, transaction.quantity());
    }

    @Test
    void rejectsEmptyLine() {
        assertInvalidRow("", "Empty line");
    }

    @Test
    void rejectsIncorrectNumberOfFields() {
        assertInvalidRow(
                "PNH,2026-09-01,PNH-000001",
                "Expected 10 fields, got 3"
        );
    }

    @Test
    void rejectsUnknownBranch() {
        assertInvalidRow(
                "XXX,2026-09-01,R-001,SKU-1,Product,Grocery,1,5.00,0.00,CASH",
                "Unknown branch: XXX"
        );
    }

    @Test
    void rejectsInvalidDate() {
        assertInvalidRow(
                "PNH,not-a-date,R-001,SKU-1,Product,Grocery,1,5.00,0.00,CASH",
                "Invalid date: not-a-date"
        );
    }

    @Test
    void rejectsUnknownCategory() {
        assertInvalidRow(
                "PNH,2026-09-01,R-001,SKU-1,Product,Unknown,1,5.00,0.00,CASH",
                "Unknown category: Unknown"
        );
    }

    @Test
    void rejectsInvalidQuantity() {
        assertInvalidRow(
                "PNH,2026-09-01,R-001,SKU-1,Product,Grocery,abc,5.00,0.00,CASH",
                "Invalid quantity: abc"
        );
    }

    @Test
    void rejectsZeroQuantity() {
        assertInvalidRow(
                "PNH,2026-09-01,R-001,SKU-1,Product,Grocery,0,5.00,0.00,CASH",
                "quantity must be positive: 0"
        );
    }

    @Test
    void rejectsNegativeQuantity() {
        assertInvalidRow(
                "PNH,2026-09-01,R-001,SKU-1,Product,Grocery,-2,5.00,0.00,CASH",
                "quantity must be positive: -2"
        );
    }

    @Test
    void rejectsNegativeUnitPrice() {
        assertInvalidRow(
                "PNH,2026-09-01,R-001,SKU-1,Product,Grocery,1,-5.00,0.00,CASH",
                "Amount cannot be negative"
        );
    }

    @Test
    void rejectsNegativeDiscount() {
        assertInvalidRow(
                "PNH,2026-09-01,R-001,SKU-1,Product,Grocery,1,5.00,-1.00,CASH",
                "Amount cannot be negative"
        );
    }

    @Test
    void rejectsDiscountExceedingLineAmount() {
        assertInvalidRow(
                "PNH,2026-09-01,R-001,SKU-1,Product,Grocery,2,5.00,11.00,CASH",
                "Discount exceeds line amount"
        );
    }

    @Test
    void rejectsUnknownPaymentMethod() {
        assertInvalidRow(
                "PNH,2026-09-01,R-001,SKU-1,Product,Grocery,1,5.00,0.00,UNKNOWN",
                "Unknown payment method: UNKNOWN"
        );
    }

    private void assertInvalidRow(String line, String expectedMessage) {
        InvalidRowException exception = assertThrows(
                InvalidRowException.class,
                () -> parser.parse(line, 2)
        );

        assertEquals(expectedMessage, exception.getMessage());
    }
}
