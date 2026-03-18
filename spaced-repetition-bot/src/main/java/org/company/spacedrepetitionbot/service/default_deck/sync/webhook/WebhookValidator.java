package org.company.spacedrepetitionbot.service.default_deck.sync.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.company.spacedrepetitionbot.config.properties.AppProperties;
import org.springframework.stereotype.Component;

/**
 * Validates the signature of incoming GitHub webhooks.
 * <p>
 * Uses HMAC‑SHA1 with the configured webhook secret to verify that the payload
 * originated from GitHub and has not been tampered with.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookValidator {
    private final AppProperties appProperties;

    /**
     * Validates the webhook signature.
     *
     * @param rawPayload the raw JSON payload as received from GitHub
     * @param signature  the signature header (e.g., {@code sha1=...})
     * @throws SecurityException if the signature does not match the computed HMAC
     */
    public void validateSignature(String rawPayload, String signature) {
        String secret = appProperties.getDefaultDeck().getRepo().getWebhookSecret();
        String computedSignature = "sha1=" + new HmacUtils(HmacAlgorithms.HMAC_SHA_1, secret).hmacHex(rawPayload);

        if (!computedSignature.equals(signature)) {
            log.error("Signature mismatch! Computed: {}, Received: {}", computedSignature, signature);
            throw new SecurityException("Invalid webhook signature");
        }
    }
}
