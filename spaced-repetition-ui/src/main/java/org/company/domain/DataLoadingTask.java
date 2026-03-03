package org.company.domain;

import lombok.extern.slf4j.Slf4j;

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
    public List<AnswerEvent> execute(TaskContext context) throws Exception {
        log.info("Загрузка данных для фильтра: {}", filter);
        if (context.isCancelled()) {
            log.info("Загрузка отменена");
            return null;
        }
        try {
            List<AnswerEvent> data = dataService.fetchData(filter);
            log.info("Загружено {} событий", data.size());
            return data;
        } catch (Exception e) {
            log.error("Ошибка при загрузке данных: {}", e.getMessage(), e);
            throw e;
        }
    }
}