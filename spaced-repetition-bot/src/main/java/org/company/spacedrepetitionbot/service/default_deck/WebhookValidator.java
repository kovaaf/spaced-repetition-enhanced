package org.company.spacedrepetitionbot.service.default_deck;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.company.spacedrepetitionbot.config.AppProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookValidator {
    private final AppProperties appProperties;

    public void validateSignature(String rawPayload, String signature) {
        String secret = appProperties.getDefaultDeck().getRepo().getWebhookSecret();
        String computedSignature = "sha1=" + new HmacUtils(HmacAlgorithms.HMAC_SHA_1, secret).hmacHex(rawPayload);

        if (!computedSignature.equals(signature)) {
            log.error("Signature mismatch! Computed: {}, Received: {}", computedSignature, signature);
            throw new SecurityException("Invalid webhook signature");
        }
    }
}
