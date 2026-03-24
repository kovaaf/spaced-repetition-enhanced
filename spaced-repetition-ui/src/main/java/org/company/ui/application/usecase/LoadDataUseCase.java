package org.company.ui.application.usecase;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.company.ui.application.port.input.LoadData;
import org.company.ui.application.port.output.grpc.DataService;
import org.company.ui.domain.entity.AnswerEvent;
import org.company.ui.domain.entity.TimeFilter;
import org.company.ui.exception.DataServiceException;

import java.util.List;

/**
 * Use case for loading historical answer events according to a time filter.
 * Delegates the actual fetching to a {@link DataService}.
 */
@Slf4j
public class LoadDataUseCase implements LoadData {
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