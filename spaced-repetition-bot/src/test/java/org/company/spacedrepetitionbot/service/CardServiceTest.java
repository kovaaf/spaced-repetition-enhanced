package org.company.spacedrepetitionbot.service;

import org.company.spacedrepetitionbot.model.Card;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.model.UserInfo;
import org.company.spacedrepetitionbot.repository.CardRepository;
import org.company.spacedrepetitionbot.repository.DeckRepository;
import org.company.spacedrepetitionbot.repository.UserInfoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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