package edu.itc.salesreport.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class InputRulesTest {
    // "Today" is 2026-09-27, so 2026-10 is in the future.
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-27T03:00:00Z"), ZoneId.of("Asia/Phnom_Penh"));

    @Test
    void validMonthIsAcceptedAndTrimmed() {
        assertEquals(new Validation.Valid<>(YearMonth.of(2026, 9)),
                InputRules.month("  2026-09 ", CLOCK));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "2026/09", "2026-9", "Sept", "2026-13"})
    void malformedMonthIsRejected(String text) {
        assertInstanceOf(Validation.Invalid.class, InputRules.month(text, CLOCK));
    }

    @Test
    void futureMonthIsRejectedWithMessage() {
        assertEquals(new Validation.Invalid<>("2026-10 is in the future"),
                InputRules.month("2026-10", CLOCK));
    }

    @Test
    void branchIsNormalisedAndUnknownBranchRejected() {
        assertEquals(new Validation.Valid<>("PNH"), InputRules.branch(" pnh "));
        assertEquals(new Validation.Invalid<>("Unknown branch 'XYZ': use ALL, PNH, REP or BTB"),
                InputRules.branch("xyz"));
    }
}