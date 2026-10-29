package edu.itc.salesreport.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(controllers = ReportWebController.class,
        properties = "salesreport.data-dir=target/test-data")
class ReportWebControllerTest {
    private static final String HEADER = "branch,date,receipt_no,sku,"
            + "product_name,category,quantity,unit_price,discount,payment_method";

    @Autowired MockMvcTester mvc;

    @BeforeAll
    static void writeData() throws IOException {
        Path month = Files.createDirectories(Path.of("target/test-data/2026-09"));
        Files.writeString(month.resolve("PNH-2026-09-01.csv"), HEADER + "\n"
                + "PNH,2026-09-01,PNH-000001,SKU-1001,Jasmine Rice 5kg,Grocery,2,6.50,0.00,KHQR\n");
        Files.writeString(month.resolve("REP-2026-09-01.csv"), HEADER + "\n"
                + "REP,2026-09-01,REP-000001,SKU-4003,Sunscreen SPF50,Personal Care,3,7.50,1.50,CASH\n");
    }

    @Test
    void rendersSummaryOfOneBranch() {
        assertThat(mvc.get().uri("/report?month=2026-09&branch=PNH"))
                .hasStatusOk()
                .hasViewName("report")
                .bodyText().contains("Revenue USD 13.00", "Jasmine Rice 5kg");
    }

    @Test
    void invalidMonthShowsMessageNotAnError() {
        assertThat(mvc.get().uri("/report?month=2026-13"))
                .hasStatusOk()
                .bodyText().contains("is not a month in the form yyyy-MM");
    }
}
