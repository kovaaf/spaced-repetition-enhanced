package org.company.spacedrepetitionbot.service;

import lombok.RequiredArgsConstructor;
import org.company.spacedrepetitionbot.model.CardDraft;
import org.company.spacedrepetitionbot.repository.CardDraftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CardDraftService {
    private final CardDraftRepository cardDraftRepository;

    @Transactional
    public void saveDraft(Long chatId, Long deckId, String front, String back) {
        cardDraftRepository.findByChatId(chatId).ifPresentOrElse(
                draft -> {
                    draft.setFront(front);
                    draft.setBack(back);
                    draft.setUpdatedAt(LocalDateTime.now());
                    cardDraftRepository.save(draft);
                },
                () -> cardDraftRepository.save(CardDraft.builder()
                        .chatId(chatId)
                        .deckId(deckId)
                        .front(front)
                        .back(back)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));
    }

    @Transactional
    public void updateFront(Long chatId, String front) {
        cardDraftRepository.findByChatId(chatId).ifPresent(draft -> {
            draft.setFront(front);
            draft.setUpdatedAt(LocalDateTime.now());
            cardDraftRepository.save(draft);
        });
    }

    @Transactional
    public void updateBack(Long chatId, String back) {
        cardDraftRepository.findByChatId(chatId).ifPresent(draft -> {
            draft.setBack(back);
            draft.setUpdatedAt(LocalDateTime.now());
            cardDraftRepository.save(draft);
        });
    }

    @Transactional(readOnly = true)
    public Optional<CardDraft> getDraft(Long chatId) {
        return cardDraftRepository.findByChatId(chatId);
    }

    @Transactional
    public void clearDraft(Long chatId) {
        cardDraftRepository.deleteByChatId(chatId);
    }
}
