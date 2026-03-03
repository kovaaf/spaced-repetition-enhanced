package org.company.domain;

import lombok.extern.slf4j.Slf4j;
import org.company.domain.exception.DataServiceException;

import java.util.List;

@Slf4j
public class DataLoadingTask implements Task<List<AnswerEvent>> {
    private final DataService dataService;
    private final TimeFilter filter;

    public DataLoadingTask(DataService dataService, TimeFilter filter) {
        this.dataService = dataService;
        this.filter = filter;
    }

    @Override
    public List<AnswerEvent> execute(TaskContext context) throws DataServiceException {
        log.info("Загрузка данных для фильтра: {}", filter);
        if (context.isCancelled()) {
            log.info("Загрузка отменена пользователем, возвращаем null");
            return null;
        }
        try {
            List<AnswerEvent> data = dataService.fetchData(filter);
            log.info("Загружено {} событий", data.size());
            return data;
        } catch (DataServiceException e) {
            log.error("Ошибка при загрузке данных через DataService: {}", e.getMessage(), e);
            throw e; // пробрасываем для централизованной обработки в контроллере
        }
    }
}