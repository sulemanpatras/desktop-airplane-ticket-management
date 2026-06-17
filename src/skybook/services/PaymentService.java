package skybook.services;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * PaymentService: Validates card details against the real Stripe TEST-mode API.
 *
 * IMPORTANT — how this stays safe with no backend:
 *   - Only Stripe's PUBLISHABLE key is used (pk_test_...), never a secret key.
 *   - We call POST /v1/payment_methods to create a PaymentMethod from the
 *     raw card fields. This is the same endpoint Stripe.js uses client-side;
 *     it is designed to be called with just the publishable key.
 *   - Stripe performs REAL validation server-side: Luhn check, expiry,
 *     CVC format, and known test-card decline scenarios
 *     (e.g. 4000000000000002 = "card declined").
 *   - We deliberately STOP after creating the PaymentMethod. We never create
 *     a PaymentIntent and never confirm/capture a charge, so no money ever
 *     moves and nothing is billed — this is pure validation.
 *   - No card data is stored or logged anywhere in this app.
 *
 * Demo key below is Stripe's well-known PUBLIC sample test key and will
 * return Stripe-shaped responses but is rate-limited / shared. Replace
 * PUBLISHABLE_KEY with your own pk_test_... key from the Stripe dashboard.
 */
public class PaymentService {

    // TODO: replace with your own Stripe TEST publishable key (pk_test_...)
    private static final String PUBLISHABLE_KEY =
            "pk_test_TYooMQauvdEDq54NiTphI7jx";

    private static final String STRIPE_PAYMENT_METHODS_URL =
            "https://api.stripe.com/v1/payment_methods";

    /** Result of a card validation attempt. */
    public static class PaymentResult {
        public final boolean success;
        public final String paymentMethodId; // non-null only on success
        public final String message;         // human-readable status / error

        private PaymentResult(boolean success, String paymentMethodId, String message) {
            this.success = success;
            this.paymentMethodId = paymentMethodId;
            this.message = message;
        }

        static PaymentResult ok(String pmId) {
            return new PaymentResult(true, pmId, "Card validated successfully.");
        }

        static PaymentResult fail(String message) {
            return new PaymentResult(false, null, message);
        }
    }

    /**
     * Sends card details to Stripe to create a PaymentMethod.
     * This validates the card for real (Luhn, expiry, CVC, decline test
     * numbers) without ever charging it.
     *
     * Runs synchronously — call this from a background thread, not the
     * JavaFX Application Thread.
     */
    public PaymentResult validateCard(String cardNumber, String expMonth,
                                       String expYear, String cvc,
                                       String cardholderName) {
        try {
            String body = "type=card"
                    + "&card[number]="    + enc(cardNumber)
                    + "&card[exp_month]=" + enc(expMonth)
                    + "&card[exp_year]="  + enc(expYear)
                    + "&card[cvc]="       + enc(cvc)
                    + "&billing_details[name]=" + enc(cardholderName);

            HttpURLConnection conn = (HttpURLConnection) new URL(STRIPE_PAYMENT_METHODS_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + PUBLISHABLE_KEY);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(15_000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            String responseBody = readAll(status >= 200 && status < 300
                    ? conn.getInputStream() : conn.getErrorStream());

            if (status >= 200 && status < 300) {
                String pmId = extractJsonField(responseBody, "\"id\"");
                if (pmId == null) {
                    return PaymentResult.fail("Unexpected response from Stripe.");
                }
                return PaymentResult.ok(pmId);
            } else {
                String errMsg = extractJsonField(responseBody, "\"message\"");
                if (errMsg == null) errMsg = "Card validation failed.";
                return PaymentResult.fail(errMsg);
            }

        } catch (IOException e) {
            return PaymentResult.fail("Could not reach Stripe: " + e.getMessage());
        } catch (Exception e) {
            return PaymentResult.fail("Payment error: " + e.getMessage());
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String enc(String s) throws Exception {
        return URLEncoder.encode(s == null ? "" : s.trim(), StandardCharsets.UTF_8.toString());
    }

    private String readAll(InputStream is) throws IOException {
        if (is == null) return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    /**
     * Minimal, dependency-free JSON field extractor.
     * Looks for "key":"value" or "key":value and returns the value as a string.
     * Good enough for Stripe's flat error/id fields without pulling in a JSON lib.
     */
    private String extractJsonField(String json, String quotedKey) {
        int keyIdx = json.indexOf(quotedKey);
        if (keyIdx == -1) return null;
        int colonIdx = json.indexOf(':', keyIdx + quotedKey.length());
        if (colonIdx == -1) return null;

        int i = colonIdx + 1;
        while (i < json.length() && (json.charAt(i) == ' ')) i++;

        if (i < json.length() && json.charAt(i) == '"') {
            int start = i + 1;
            StringBuilder sb = new StringBuilder();
            int j = start;
            while (j < json.length() && json.charAt(j) != '"') {
                if (json.charAt(j) == '\\' && j + 1 < json.length()) j++;
                sb.append(json.charAt(j));
                j++;
            }
            return sb.toString();
        }
        return null;
    }
}
