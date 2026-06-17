package skybook.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import skybook.services.PaymentService;

/**
 * CardPaymentDialog — Stripe TEST-mode card entry modal.
 *
 * Shown right before a booking is finalized. The card is validated for
 * real against Stripe's API (Luhn check, expiry, CVC, decline-test
 * numbers) but NO PaymentIntent is created and NO charge is captured —
 * this only proves the card is well-formed and not a known-declined
 * test card. Nothing about the card is stored or logged.
 *
 * Use showAndValidate(...) which blocks (via Dialog.showAndWait) and
 * returns true only if Stripe confirmed the card is valid.
 */
public class CardPaymentDialog {

    private final PaymentService paymentService = new PaymentService();

    /**
     * Shows the modal and returns true only if the card was successfully
     * validated by Stripe. Returns false if the user cancels or the card
     * fails validation (after they've had a chance to retry).
     */
    public boolean showAndValidate(double amount, String currencyLabel) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Secure Payment");
        dialog.initStyle(javafx.stage.StageStyle.UNDECORATED);

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: #0f172a;");
        pane.getButtonTypes().clear(); // we build our own buttons in content

        VBox root = new VBox(18);
        root.setPadding(new Insets(28));
        root.setPrefWidth(420);
        root.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 14;");

        // ── Header ──────────────────────────────────────────────────────
        VBox header = new VBox(2);
        Label title = new Label("💳  Pay with Card");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;");
        Label subtitle = new Label("Stripe test mode — no real charge will be made");
        subtitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        header.getChildren().addAll(title, subtitle);

        // ── Amount banner ───────────────────────────────────────────────
        HBox amountBox = new HBox();
        amountBox.setPadding(new Insets(14));
        amountBox.setAlignment(Pos.CENTER_LEFT);
        amountBox.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8;");
        Label amountLbl = new Label("Amount due");
        amountLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label amountVal = new Label(currencyLabel + String.format("%.2f", amount));
        amountVal.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 18px; -fx-font-weight: bold;");
        amountBox.getChildren().addAll(amountLbl, sp, amountVal);

        // ── Form fields ─────────────────────────────────────────────────
        TextField nameField = styledField("Name on card");

        TextField cardField = styledField("Card number (e.g. 4242 4242 4242 4242)");
        Label cardErrorLabel = new Label("");
        cardErrorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11px;");
        cardErrorLabel.setVisible(false);
        cardErrorLabel.setManaged(false);

        HBox row2 = new HBox(10);
        TextField expField = styledField("MM/YY");
        TextField cvcField = styledField("CVC");
        expField.setPrefWidth(180);
        cvcField.setPrefWidth(180);
        row2.getChildren().addAll(expField, cvcField);

        // Live validation as the user types the card number: shows an error
        // immediately on an invalid (failed Luhn / too short) number and
        // hides it again as soon as the number becomes valid.
        cardField.textProperty().addListener((obs, oldVal, newVal) -> {
            String digitsOnly = newVal.replaceAll("[^0-9]", "");

            // Reformat with a space every 4 digits, capped at 19 digits
            // (covers all major card schemes including Amex/Diners).
            if (digitsOnly.length() > 19) digitsOnly = digitsOnly.substring(0, 19);
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < digitsOnly.length(); i++) {
                if (i > 0 && i % 4 == 0) formatted.append(' ');
                formatted.append(digitsOnly.charAt(i));
            }
            if (!formatted.toString().equals(newVal)) {
                cardField.setText(formatted.toString());
                cardField.positionCaret(formatted.length());
                return; // listener will re-fire with the corrected text
            }

            if (digitsOnly.isEmpty()) {
                setCardFieldError(cardField, cardErrorLabel, null);
            } else if (digitsOnly.length() < 13) {
                setCardFieldError(cardField, cardErrorLabel, "Card number is too short.");
            } else if (!passesLuhnCheck(digitsOnly)) {
                setCardFieldError(cardField, cardErrorLabel, "That card number looks invalid.");
            } else {
                setCardFieldError(cardField, cardErrorLabel, null);
            }
        });

        // Auto-inserts "/" after MM as the user types, producing MM/YY
        // without them needing to type the slash themselves.
        expField.textProperty().addListener((obs, oldVal, newVal) -> {
            String digitsOnly = newVal.replaceAll("[^0-9]", "");
            if (digitsOnly.length() > 4) digitsOnly = digitsOnly.substring(0, 4);

            String formatted;
            if (digitsOnly.length() <= 2) {
                formatted = digitsOnly;
            } else {
                formatted = digitsOnly.substring(0, 2) + "/" + digitsOnly.substring(2);
            }
            if (!formatted.equals(newVal)) {
                expField.setText(formatted);
                expField.positionCaret(formatted.length());
            }
        });

        Label statusLabel = new Label("");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-font-size: 12px;");

        Label testCardHint = new Label(
                "Test cards: 4242 4242 4242 4242 (success)  ·  4000 0000 0000 0002 (declined)");
        testCardHint.setWrapText(true);
        testCardHint.setStyle("-fx-text-fill: #475569; -fx-font-size: 10px; -fx-font-family: monospace;");

        // ── Buttons ─────────────────────────────────────────────────────
        Button payBtn = primaryBtn("Pay " + currencyLabel + String.format("%.2f", amount));
        Button cancelBtn = secondaryBtn("Cancel");
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(18, 18);
        spinner.setVisible(false);
        spinner.setManaged(false);

        HBox btnRow = new HBox(10, cancelBtn, spinner, payBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        final boolean[] resultHolder = {false};

        payBtn.setOnAction(e -> {
            statusLabel.setText("");

            String name = nameField.getText();
            String rawCard = cardField.getText();
            String exp = expField.getText();
            String cvc = cvcField.getText();

            String[] parsedExp = parseExpiry(exp);
            String cardDigits = rawCard == null ? "" : rawCard.replaceAll("[^0-9]", "");

            if (name == null || name.trim().isEmpty()) {
                showStatus(statusLabel, "⚠ Please enter the name on the card.", false);
                return;
            }
            if (cardDigits.length() < 13 || !passesLuhnCheck(cardDigits)) {
                showStatus(statusLabel, "⚠ Please enter a valid card number.", false);
                return;
            }
            if (parsedExp == null) {
                showStatus(statusLabel, "⚠ Expiry must be in MM/YY format.", false);
                return;
            }
            if (cvc == null || cvc.trim().length() < 3) {
                showStatus(statusLabel, "⚠ Please enter a valid CVC.", false);
                return;
            }

            setFormDisabled(true, payBtn, cancelBtn, nameField, cardField, expField, cvcField, spinner);
            showStatus(statusLabel, "Contacting Stripe…", true);

            String expMonth = parsedExp[0];
            String expYear = parsedExp[1];

            Thread worker = new Thread(() -> {
                PaymentService.PaymentResult result =
                        paymentService.validateCard(cardDigits, expMonth, expYear, cvc.trim(), name.trim());

                Platform.runLater(() -> {
                    setFormDisabled(false, payBtn, cancelBtn, nameField, cardField, expField, cvcField, spinner);
                    if (result.success) {
                        showStatus(statusLabel, "✓ Card validated successfully.", true);
                        resultHolder[0] = true;
                        dialog.setResult(true);
                        dialog.close();
                    } else {
                        showStatus(statusLabel, "✗ " + result.message, false);
                    }
                });
            });
            worker.setDaemon(true);
            worker.start();
        });

        cancelBtn.setOnAction(e -> {
            resultHolder[0] = false;
            dialog.setResult(false);
            dialog.close();
        });

        root.getChildren().addAll(
                header, amountBox,
                fieldLabel("Cardholder Name"), nameField,
                fieldLabel("Card Number"), cardField, cardErrorLabel,
                row2,
                statusLabel,
                testCardHint,
                new Separator() {{ setStyle("-fx-background-color: #1e293b;"); }},
                btnRow
        );

        pane.setContent(root);
        dialog.showAndWait();
        return resultHolder[0];
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static final String FIELD_BASE_STYLE = """
            -fx-background-color: #1e293b;
            -fx-text-fill: #f1f5f9;
            -fx-prompt-text-fill: #475569;
            -fx-border-color: #334155;
            -fx-border-radius: 6;
            -fx-background-radius: 6;
            -fx-padding: 10;
            -fx-font-size: 13px;
        """;

    private static final String FIELD_ERROR_STYLE = """
            -fx-background-color: #1e293b;
            -fx-text-fill: #f1f5f9;
            -fx-prompt-text-fill: #475569;
            -fx-border-color: #f87171;
            -fx-border-radius: 6;
            -fx-background-radius: 6;
            -fx-padding: 10;
            -fx-font-size: 13px;
        """;

    /**
     * Toggles the card field's border to red and shows/hides the inline
     * error message as the user types — live feedback, not just on submit.
     * Pass null to clear the error (valid or empty input).
     */
    private void setCardFieldError(TextField cardField, Label errorLabel, String message) {
        if (message == null) {
            cardField.setStyle(FIELD_BASE_STYLE);
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        } else {
            cardField.setStyle(FIELD_ERROR_STYLE);
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    /**
     * Standard Luhn (mod 10) checksum used by all major card networks.
     * Lets us flag an invalid card number immediately, client-side,
     * before ever contacting Stripe.
     */
    private boolean passesLuhnCheck(String digits) {
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    private void setFormDisabled(boolean disabled, Button payBtn, Button cancelBtn,
                                  TextField name, TextField card, TextField exp, TextField cvc,
                                  ProgressIndicator spinner) {
        payBtn.setDisable(disabled);
        cancelBtn.setDisable(disabled);
        name.setDisable(disabled);
        card.setDisable(disabled);
        exp.setDisable(disabled);
        cvc.setDisable(disabled);
        spinner.setVisible(disabled);
        spinner.setManaged(disabled);
    }

    private void showStatus(Label label, String text, boolean okOrInfo) {
        label.setText(text);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (okOrInfo ? "#34d399" : "#f87171") + ";");
    }

    /** Parses "MM/YY" or "MM/YYYY" into [month, 4-digit year], or null if invalid. */
    private String[] parseExpiry(String exp) {
        if (exp == null) return null;
        String trimmed = exp.trim();
        String[] parts = trimmed.split("/");
        if (parts.length != 2) return null;
        String month = parts[0].trim();
        String year = parts[1].trim();
        if (!month.matches("\\d{1,2}")) return null;
        if (!year.matches("\\d{2}|\\d{4}")) return null;
        int m = Integer.parseInt(month);
        if (m < 1 || m > 12) return null;
        if (year.length() == 2) year = "20" + year;
        return new String[]{String.valueOf(m), year};
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        return l;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle(FIELD_BASE_STYLE);
        return tf;
    }

    private Button primaryBtn(String text) {
        Button b = new Button(text);
        b.setStyle("""
            -fx-background-color: #38bdf8;
            -fx-text-fill: #0f172a;
            -fx-font-weight: bold;
            -fx-padding: 10 20;
            -fx-background-radius: 7;
            -fx-cursor: hand;
        """);
        return b;
    }

    private Button secondaryBtn(String text) {
        Button b = new Button(text);
        b.setStyle("""
            -fx-background-color: #334155;
            -fx-text-fill: #cbd5e1;
            -fx-font-weight: bold;
            -fx-padding: 10 18;
            -fx-background-radius: 7;
            -fx-cursor: hand;
        """);
        return b;
    }
}