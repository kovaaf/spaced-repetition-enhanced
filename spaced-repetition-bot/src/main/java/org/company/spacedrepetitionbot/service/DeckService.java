package org.company.spacedrepetitionbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.config.properties.AppProperties;
import org.company.spacedrepetitionbot.constants.MessageConstants;
import org.company.spacedrepetitionbot.exception.DeckNotFoundException;
import org.company.spacedrepetitionbot.model.Card;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.model.UserInfo;
import org.company.spacedrepetitionbot.repository.CardRepository;
import org.company.spacedrepetitionbot.repository.DeckRepository;
import org.company.spacedrepetitionbot.repository.UserInfoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.company.spacedrepetitionbot.constants.MessageConstants.*;
import static org.company.spacedrepetitionbot.constants.OtherConstants.DEFAULT_DECK_SUFFIX;

@RequiredArgsConstructor
@Slf4j
@Service
public class DeckService {
    private final DeckRepository deckRepository;
    private final UserInfoRepository userInfoRepository;
    private final AppProperties appProperties;
    private final UserInfoService userInfoService;
    private final CardRepository cardRepository;
    @Value("${app.deck.max-cards-display:10}")
    private int maxCardsToDisplay;

    private static String formatErrorMessage(MessageConstants template, Object... args) {
        return String.format(template.getMessage(), args);
    }

    @Transactional(readOnly = true)
    public String getDeckDetails(Long userId, String deckName) {
        return findDeckWithCards(userId, deckName).map(this::formatDeckDetails)
                .orElseGet(() -> String.format(DECK_NOT_FOUND_MESSAGE.getMessage(), deckName));
    }

    /**
     * Создает новую колоду и сохраняет ее в базе данных.
     *
     * @param userChatId идентификатор пользователя, которому будет принадлежать колода
     * @param deckName   имя колоды
     * @throws IllegalArgumentException если пользователь с указанным идентификатором не существует
     */
    @Transactional
    public String addDeck(Long userChatId, String deckName) {
        try {
            UserInfo owner = getUserInfoOrThrow(userChatId);

            if (deckRepository.existsByNameIgnoreCaseAndOwner(deckName, owner)) {
                return formatErrorMessage(DECK_ALREADY_EXISTS_SIMPLE, deckName);
            }

            Deck newDeck = Deck.builder().name(deckName).owner(owner).build();

            deckRepository.save(newDeck);
            return String.format(DECK_ADDED_SUCCESSFULLY.getMessage(), deckName);
        } catch (IllegalArgumentException e) {
            log.error("Ошибка добавления колоды: {}", e.getMessage());
            return formatErrorMessage(ERROR_ADDING_DECK, e);
        }
    }

    @Transactional
    public String deleteDeck(Long userChatId, String deckName) {
        try {
            Deck deck = getDeckByOwnerIdAndNameOrThrow(userChatId, deckName);

            deckRepository.delete(deck);
            return String.format(DECK_DELETED_SUCCESSFULLY.getMessage(), deckName);
        } catch (IllegalArgumentException e) {
            log.error("Ошибка удаления колоды: {}", e.getMessage());
            return formatErrorMessage(ERROR_DELETING_DECK, e);
        }
    }

    @Transactional
    public String renameDeck(Long userChatId, String currentDeckName, String newDeckName) {
        try {
            UserInfo owner = getUserInfoOrThrow(userChatId);
            Deck deck = getDeckOwnerInfoAndNameOrThrow(owner, currentDeckName);

            if (deckRepository.existsByNameIgnoreCaseAndOwner(newDeckName, owner)) {
                return String.format(DECK_ALREADY_EXISTS_SIMPLE.getMessage(), newDeckName);
            }

            deck.setName(newDeckName);
            deckRepository.save(deck);
            return String.format(DECK_RENAMED_SUCCESSFULLY.getMessage(), currentDeckName, newDeckName);
        } catch (IllegalArgumentException e) {
            log.error("Ошибка переименования колоды: {}", e.getMessage());
            return formatErrorMessage(ERROR_RENAMING_DECK, e);
        }
    }

    // TODO доставать все записи тяжело, нужно прикрутить пейджинг(плюс кнопки в меню) и вытаскивать с лимитом
    @Transactional(readOnly = true)
    public String formatUserDecks(Long chatId) {
        List<Deck> userDecks = deckRepository.findAllDecksByOwnerUserChatId(chatId);

        if (userDecks.isEmpty()) {
            return NO_DECKS_FOUND.getMessage();
        }

        return formatDeckList(userDecks);
    }

    private Deck getDeckByOwnerIdAndNameOrThrow(Long userChatId, String deckName) {
        UserInfo owner = getUserInfoOrThrow(userChatId);
        return getDeckOwnerInfoAndNameOrThrow(owner, deckName);
    }

    private UserInfo getUserInfoOrThrow(Long userChatId) {
        return userInfoRepository.findById(userChatId)
                .orElseThrow(() -> new IllegalArgumentException(String.format(
                        USER_NOT_FOUND.getMessage(),
                        userChatId)));
    }

    private Deck getDeckOwnerInfoAndNameOrThrow(UserInfo owner, String deckName) {
        return deckRepository.findByNameIgnoreCaseAndOwner(deckName, owner)
                .orElseThrow(() -> new IllegalArgumentException(String.format(
                        DECK_NOT_FOUND_SIMPLE.getMessage(),
                        deckName)));
    }

    private String formatErrorMessage(MessageConstants constant, Exception e) {
        return String.format("%s%s", constant.getMessage(), e.getMessage());
    }

    private String formatDeckList(List<Deck> decks) {
        String header = String.format(DECK_LIST_HEADER.getMessage(), decks.size());

        String deckItems = IntStream.range(0, decks.size()).mapToObj(i -> {
            Deck deck = decks.get(i);
            return String.format(DECK_ITEM_FORMAT.getMessage(), i + 1, deck.getName(), deck.getCards().size());
        }).collect(Collectors.joining("\n"));

        return header + deckItems;
    }

    private Optional<Deck> findDeckWithCards(Long userId, String deckName) {
        return userInfoRepository.findById(userId)
                .flatMap(user -> deckRepository.findWithCardsByNameIgnoreCaseAndOwner(deckName, user));
    }

    private String formatDeckDetails(Deck deck) {
        return String.format(
                DECK_DETAILS_TEMPLATE.getMessage(),
                deck.getName(),
                deck.getCards().size(),
                formatCardList(deck.getCards()));
    }

    private String formatCardList(Set<Card> cards) {
        if (cards.isEmpty()) {
            return NO_CARDS_IN_DECK.getMessage();
        }

        return cards.stream()
                .limit(maxCardsToDisplay) // Ограничиваем количество показываемых карточек
                .map(card -> String.format(CARD_ITEM_FORMAT.getMessage(), card.getFront()))
                .collect(Collectors.joining("\n"));
    }

    @Transactional(readOnly = true)
    public Optional<Deck> getDeckById(Long deckId) {
        return deckRepository.findById(deckId);
    }

    // TODO доставать все записи тяжело, нужно прикрутить пейджинг(плюс кнопки в меню) и вытаскивать с лимитом
    @Transactional(readOnly = true)
    public List<Deck> getUserDecks(Long chatId) {
        return deckRepository.findAllDecksByOwnerUserChatId(chatId);
    }

    @Transactional(readOnly = true)
    public Optional<Deck> getDeckByIdWithCards(Long deckId) {
        return deckRepository.findWithCardsByDeckId(deckId);
    }

    public Deck getDefaultDeck() {
        return deckRepository.findByName(appProperties.getDefaultDeck().getName())
                .orElseThrow(() -> new DeckNotFoundException("Default deck not found"));
    }

    @Transactional
    public Deck initializeDefaultDeck() {
        return deckRepository.findByName(appProperties.getDefaultDeck().getName()).orElseGet(() -> {
            UserInfo systemUser = userInfoService.getSystemUser();
            Deck newDeck = Deck.builder()
                    .name(appProperties.getDefaultDeck().getName())
                    .isDefault(true)
                    .owner(systemUser)
                    .sourceFolders(appProperties.getDefaultDeck().getRepo().getSourceFolders())
                    .build();
            log.debug("Initializing default deck: {}", newDeck.getName());
            return deckRepository.save(newDeck);
        });
    }

    public Deck getDeckByName(String deckName) {
        return deckRepository.findByName(deckName)
                .orElseThrow(() -> new DeckNotFoundException("Deck '" + deckName + "' not found"));
    }

    public Optional<Deck> getOptionalDeckByName(String deckName) {
        return deckRepository.findByName(deckName);
    }

    public Deck save(Deck deck) {
        return deckRepository.save(deck);
    }

    @Transactional
    public String copyDefaultDeck(Long userChatId, String newDeckName) {
        try {
            UserInfo user = getUserInfoOrThrow(userChatId);

            if (user.isHasCopiedDefaultDeck()) {
                return "Вы уже скопировали дефолтную колоду ранее";
            }

            Deck defaultDeck = getDefaultDeck();
            String fullDeckName = newDeckName + DEFAULT_DECK_SUFFIX.getValue();

            if (deckRepository.existsByNameIgnoreCaseAndOwner(fullDeckName, user)) {
                return "У вас уже есть колода с именем: " + fullDeckName;
            }

            Deck newDeck = Deck.builder().name(fullDeckName).owner(user).build();
            deckRepository.save(newDeck);

            // Копирование карт
            List<Card> cardsToCopy = defaultDeck.getCards()
                    .stream()
                    .map(card -> Card.builder()
                            .front(card.getFront())
                            .back(card.getBack())
                            .originalCardId(card.getCardId())
                            .deck(newDeck)
                            .build())
                    .toList();

            cardRepository.saveAll(cardsToCopy);

            // Исправление: вызов сервисного метода вместо прямого изменения
            userInfoService.markUserCopiedDefaultDeck(userChatId);

            return "✅ Дефолтная колода скопирована как: " + fullDeckName;
        } catch (DeckNotFoundException e) {
            log.error("Дефолтная колода не найдена", e);
            return "❌ Дефолтная колода не найдена в системе";
        } catch (Exception e) {
            log.error("Ошибка копирования дефолтной колоды: {}", e.getMessage(), e);
            return "❌ Произошла ошибка при копировании колоды";
        }
    }

    public List<Deck> findByDefaultDeckSuffix() {
        return deckRepository.findByNameEndingWith(DEFAULT_DECK_SUFFIX.getValue());
    }
}
