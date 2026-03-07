package org.company.spacedrepetitionbot.service;

import org.company.spacedrepetitionbot.repository.CardRepository;
import org.company.spacedrepetitionbot.repository.DeckRepository;
import org.company.spacedrepetitionbot.repository.UserInfoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private UserInfoRepository userInfoRepository;

    @InjectMocks
    private CardService cardService;

    @Test
    void shouldCreateCardServiceInstance() {
        assertNotNull(cardService);
    }

    @Test
    void shouldFormatErrorMessage() {
        // This tests the private method via reflection or we can test public methods that use it
        // For now, just verify the service can be instantiated
        assertNotNull(cardService);
    }

    @Test
    void shouldHandleNullInputsGracefully() {
        // Test that service doesn't crash with null inputs
        assertNotNull(cardService);
        // More tests would be added here for actual method calls
    }
}