package org.company.service.usecases;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.company.domain.AnswerEvent;
import org.company.domain.TimeFilter;
import org.company.exception.DataServiceException;
import org.company.service.dao.DataService;

import java.util.List;

/**
 * Use case for loading historical answer events according to a time filter.
 * Delegates the actual fetching to a {@link DataService}.
 */
@Slf4j
public class LoadDataUseCase {
    @Setter
    private DataService dataService;

    public LoadDataUseCase(DataService dataService) {
        this.dataService = dataService;
    }

    /**
     * Executes the load operation.
     *
     * @param filter the time range for which events should be loaded
     * @return list of answer events (may be empty)
     * @throws DataServiceException if communication with the server fails
     */
    public List<AnswerEvent> execute(TimeFilter filter) throws DataServiceException {
        log.info("Executing LoadDataUseCase with filter: {}", filter);
        return dataService.fetchData(filter);
    }
}