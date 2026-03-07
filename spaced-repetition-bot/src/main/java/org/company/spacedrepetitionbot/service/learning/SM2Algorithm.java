package org.company.spacedrepetitionbot.service.learning;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.constants.Quality;
import org.company.spacedrepetitionbot.constants.Status;
import org.company.spacedrepetitionbot.model.Card;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Реализация алгоритма интервального повторения SM2 с модификациями Anki.
 *
 * <p>Особенности реализации:
 * <ul>
 *   <li>Поддержка различных статусов карточек: NEW, LEARNING, REVIEW_YOUNG, REVIEW_MATURE, RELEARNING</li>
 *   <li>Корректная обработка переходов между статусами</li>
 *   <li>Настраиваемые параметры алгоритма через константы</li>
 *   <li>Учет качества ответа при расчете интервалов повторения</li>
 * </ul>
 *
 * @see <a href="https://faqs.ankiweb.net/what-spaced-repetition-algorithm.html">Anki Spaced Repetition Algorithm</a>
 */
@Slf4j
@Component
public class SM2Algorithm {

    // Константы алгоритма
    private static final double MIN_EASINESS_FACTOR = 1.3;
    private static final long MATURE_THRESHOLD_DAYS = 21;

    // Интервалы для фазы Learning (в минутах)
    private static final int LEARNING_STEPS = 2; // 1 мин, 10 мин, 1 день

    // Интервалы для фазы Relearning (в минутах)
    private static final int RELEARNING_STEPS = 2; // 10 мин, 1 день, 3 дн

    // Множители интервалов для разных статусов
    private static final double YOUNG_INTERVAL_FACTOR = 1.0;     // Базовый множитель для молодых карточек
    private static final double MATURE_INTERVAL_FACTOR = 1.5;    // Увеличенный множитель для зрелых карточек

    // Константы для удобства работы со временем
    private static final long MINUTE = 1;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;

    // Параметры переучивания
    // TODO пользователь может включать и настраивать - это процент от последнего интервала
    //  Обратить внимание, что в текущей реализации это множитель на LocalDateTime, а не на интервал
    private final static double RELEARNING_INTERVAL = 1.25;
    private final static boolean USER_RELEARNING_INTERVAL = false;

    /**
     * Обновляет карточку на основе алгоритма интервального повторения.
     *
     * @param card    карточка для обновления
     * @param quality качество ответа пользователя
     * @throws IllegalArgumentException если передан недопустимый статус карточки
     */
    public void updateCardWithSMTwoAlgorithm(Card card, Quality quality) {
        log.debug("Updating card {} with quality {}", card.getCardId(), quality);

        // Для новых карточек устанавливаем статус LEARNING
        if (card.getStatus() == Status.NEW) {
            card.setStatus(Status.LEARNING);
        }

        // Обработка ответа
        if (quality == Quality.AGAIN) {
            handleIncorrectAnswer(card);
        } else {
            handleCorrectAnswer(card, quality);
        }

        updateCardStatus(card);

        log.debug(
                "Card {} updated: status={}, nextReview={}, easiness={}",
                card.getCardId(),
                card.getStatus(),
                card.getNextReviewTime(),
                card.getEasinessFactor());
    }

    /**
     * Обрабатывает неправильный ответ на карточку.
     */
    private void handleIncorrectAnswer(Card card) {
        Status currentStatus = card.getStatus();

        if (currentStatus == Status.REVIEW_YOUNG || currentStatus == Status.REVIEW_MATURE) {
            card.setStatus(Status.RELEARNING);
            setNextReviewIntervalFromNow(card, 15 * MINUTE);
            card.setRepeatCount(0);
        } else {
            setNextReviewIntervalFromNow(card, MINUTE);
        }
    }

    private void handleCorrectAnswer(Card card, Quality quality) {
        Status currentStatus = card.getStatus();

        if (currentStatus != Status.NEW) {
            updateEasinessFactor(card, quality);
        }

        // Логика для разных статусов карточки
        switch (currentStatus) {
            case NEW:
            case LEARNING:
                handleLearning(card, quality);
                break;
            case RELEARNING:
                handleRelearning(card, quality);
                break;
            case REVIEW_YOUNG:
            case REVIEW_MATURE:
                handleReview(card, quality);
                break;
        }
    }

    private void handleLearning(Card card, Quality quality) {
        int step = card.getRepeatCount();
        card.setRepeatCount(step + 1);

        // TODO добавить гибкую настройку количества шагов изучения для пользователя в формате 1m, 10m, 1d, 1m, 1y
        // Для EASY сразу переходим в REVIEW с интервалом 4 дня
        if (quality == Quality.EASY) {
            card.setStatus(Status.REVIEW_YOUNG);
            setNextReviewIntervalFromNow(card, 4 * DAY);
        } else if (step >= LEARNING_STEPS && quality == Quality.GOOD) {
            card.setStatus(Status.REVIEW_YOUNG);
            setNextReviewIntervalFromNow(card, DAY);
        } else if (quality == Quality.GOOD) {
            setNextReviewIntervalFromNow(card, 10 * MINUTE);
        } else if (quality == Quality.HARD) {
            setNextReviewIntervalFromNow(card, 6 * MINUTE);
        } else {
            setNextReviewIntervalFromNow(card, MINUTE);
        }
    }

    private void handleRelearning(Card card, Quality quality) {
        int step = card.getRepeatCount();
        card.setRepeatCount(step + 1);

        long interval = USER_RELEARNING_INTERVAL ? (long) (getPreviousInterval(card) * RELEARNING_INTERVAL) : 3 * DAY;

        // TODO добавить гибкую настройку количества шагов переучивания для пользователя в формате 1m, 10m, 1d, 1m, 1y
        // Для EASY сразу переходим в REVIEW
        if (quality == Quality.EASY) {
            card.setStatus(Status.REVIEW_YOUNG);
            setNextReviewIntervalFromNow(card, interval); // 3 дня или доля от предыдущего прогресса
        } else if (step >= RELEARNING_STEPS && quality == Quality.GOOD) {
            card.setStatus(Status.REVIEW_YOUNG);
            setNextReviewIntervalFromNow(card, DAY);
        } else if (quality == Quality.GOOD) {
            setNextReviewIntervalFromNow(card, 25 * MINUTE);
        } else if (quality == Quality.HARD) {
            setNextReviewIntervalFromNow(card, 15 * MINUTE);
        } else {
            setNextReviewIntervalFromNow(card, MINUTE);
        }
    }

    private void handleReview(Card card, Quality quality) {
        long previousInterval = getPreviousInterval(card);
        long newInterval;
        double intervalFactor;

        if (card.getStatus() == Status.REVIEW_MATURE) {
            intervalFactor = MATURE_INTERVAL_FACTOR;
        } else {
            intervalFactor = YOUNG_INTERVAL_FACTOR;
        }

        if (quality == Quality.EASY) {
            newInterval = (long) (previousInterval * card.getEasinessFactor() * 1.5 * intervalFactor);
        } else if (quality == Quality.GOOD) {
            newInterval = (long) (previousInterval * card.getEasinessFactor() * intervalFactor);
        } else { // HARD
            newInterval = (long) (previousInterval * 1.2);
        }

        // 1 день в минутах
        long minInterval = 24 * 60;
        if (newInterval < minInterval) {
            newInterval = minInterval;
        }

        setNextReviewIntervalFromNow(card, newInterval);
    }

    private void updateCardStatus(Card card) {
        if (card.getStatus() == Status.REVIEW_YOUNG || card.getStatus() == Status.REVIEW_MATURE) {
            long days = Duration.between(LocalDateTime.now(), card.getNextReviewTime()).toDays();

            if (days >= MATURE_THRESHOLD_DAYS) {
                card.setStatus(Status.REVIEW_MATURE);
            } else {
                card.setStatus(Status.REVIEW_YOUNG);
            }
        }
    }

    private void updateEasinessFactor(Card card, Quality quality) {
        double qualityValue = quality.getQuality();
        double newEasiness = card.getEasinessFactor() + 0.1 - (5 - qualityValue) * (0.08 + (5 - qualityValue) * 0.02);
        card.setEasinessFactor(Math.max(MIN_EASINESS_FACTOR, newEasiness));
    }

    private void setNextReviewIntervalFromNow(Card card, long minutes) {
        if (minutes < 0) {
            log.warn(
                    "Attempt to set negative interval for card {}: {} minutes. Using default 1 day.",
                    card.getCardId(),
                    minutes);
            minutes = 24 * 60; // 1 день по умолчанию
        }
        card.setNextReviewTime(LocalDateTime.now().plusMinutes(minutes));
    }

    private long getPreviousInterval(Card card) {
        if (card.getNextReviewTime() == null) {
            return 0;
        }

        Duration duration = Duration.between(LocalDateTime.now(), card.getNextReviewTime());
        long minutes = duration.toMinutes();

        // Если карточка просрочена, возвращаем минимальный положительный интервал
        return minutes < 0 ? 0 : minutes;
    }

    /**
     * Возвращает строковое представление времени следующего повторения на основе качества ответа.
     *
     * @param card    карточка для расчета
     * @param quality качество ответа пользователя
     * @return строка с информацией о следующем повторении (например, "через 10 минут", "через 2 дня", "через 3 месяца")
     */
    public String getNextReviewTimeAsString(Card card, Quality quality) {
        // Создаем временную копию карточки для расчетов
        Card tempCard = new Card();
        tempCard.setStatus(card.getStatus());
        tempCard.setEasinessFactor(card.getEasinessFactor());
        tempCard.setRepeatCount(card.getRepeatCount());
        tempCard.setNextReviewTime(card.getNextReviewTime());

        // Обновляем карточку с учетом качества ответа
        updateCardWithSMTwoAlgorithm(tempCard, quality);

        // Получаем разницу во времени
        Duration duration = Duration.between(LocalDateTime.now(), tempCard.getNextReviewTime());
        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();
        long months = days / 30;
        long years = days / 365;

        // Формируем читаемую строку
        if (minutes < 60) {
            return String.format("через %d %s", minutes, getRussianWordForm(minutes, "минуту", "минуты", "минут"));
        } else if (hours < 24) {
            return String.format("через %d %s", hours, getRussianWordForm(hours, "час", "часа", "часов"));
        } else if (days < 30) {
            return String.format("через %d %s", days, getRussianWordForm(days, "день", "дня", "дней"));
        } else if (years < 1) {
            return String.format("через %d %s", months, getRussianWordForm(months, "месяц", "месяца", "месяцев"));
        } else {
            return String.format("через %d %s", years, getRussianWordForm(years, "год", "года", "лет"));
        }
    }

    /**
     * Вспомогательный метод для склонения слов в русском языке.
     *
     * @param number число
     * @param form1  форма для 1 (1 день)
     * @param form2  форма для 2-4 (2 дня)
     * @param form5  форма для 5-20 (5 дней)
     * @return правильная форма слова для числа
     */
    private String getRussianWordForm(long number, String form1, String form2, String form5) {
        long n = Math.abs(number) % 100;
        long n1 = n % 10;

        if (n > 10 && n < 20) {
            return form5;
        }
        if (n1 > 1 && n1 < 5) {
            return form2;
        }
        if (n1 == 1) {
            return form1;
        }
        return form5;
    }
}
