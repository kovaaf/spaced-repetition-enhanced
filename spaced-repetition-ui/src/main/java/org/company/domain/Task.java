package org.company.domain;

/**
 * Длительная задача, выполняемая в фоновом потоке.
 *
 * @param <T> тип результата задачи
 */
@FunctionalInterface
public interface Task<T> {
    /**
     * Выполняет задачу.
     * @param context контекст для публикации промежуточных данных и проверки отмены
     * @return результат выполнения
     * @throws Exception если в процессе произошла ошибка
     */
    T execute(TaskContext context) throws Exception;
}