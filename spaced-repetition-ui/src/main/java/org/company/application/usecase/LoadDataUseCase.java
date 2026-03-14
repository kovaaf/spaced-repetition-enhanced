package org.company.application.usecase;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.company.domain.AnswerEvent;
import org.company.domain.TimeFilter;
import org.company.domain.exception.DataServiceException;
import org.company.infrastructure.data.DataService;

import java.util.List;

@Slf4j
public class LoadDataUseCase {
    @Setter
    private DataService dataService;

    public LoadDataUseCase(DataService dataService) {
        this.dataService = dataService;
    }

    public List<AnswerEvent> execute(TimeFilter filter) throws DataServiceException {
        log.info("Executing LoadDataUseCase with filter: {}", filter);
        return dataService.fetchData(filter);
    }
}