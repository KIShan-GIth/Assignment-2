package in.simplifymoney.ledgersync.parse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rupee amounts as banks write them.
 *
 * Handles the prefixes we see in practice - "Rs.", "Rs ", "INR " - and strips
 * the thousands separators before handing back a BigDecimal.
 */
public final class Amounts {

    private Amounts() {}

    // Support amounts with optional decimals (e.g. Rs.5 or Rs.2,499.50)
    private static final Pattern AMOUNT =
            Pattern.compile("(?:Rs\\.?|INR)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE);

    private static final Pattern BALANCE = Pattern.compile(
            "(?:Avl\\s*Bal|Available\\s*Balance|BalAvl|Avl\\s*Limit|Clr\\s*Bal)\\s*:?\\s*"
                    + "(?:Rs\\.?|INR)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE);

    /** The transaction amount: the first rupee figure in the message, excluding stated balances. */
    public static BigDecimal first(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }

        // 1. Strip out the balance phrase to prevent matching stated balances
        String bodyWithoutBalance = BALANCE.matcher(body).replaceAll("");

        // 2. Extract the first currency figure remaining
        Matcher m = AMOUNT.matcher(bodyWithoutBalance);
        if (!m.find()) return null;
        return toDecimal(m.group(1));
    }

    /** The balance the bank quoted, if it quoted one. */
    public static BigDecimal statedBalance(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        Matcher m = BALANCE.matcher(body);
        if (!m.find()) return null;
        return toDecimal(m.group(1));
    }

    private static BigDecimal toDecimal(String raw) {
        String clean = raw.replace(",", "").trim();
        return new BigDecimal(clean).setScale(2, RoundingMode.HALF_UP);
    }
}