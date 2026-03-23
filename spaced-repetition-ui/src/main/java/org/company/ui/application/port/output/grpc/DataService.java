package org.company.ui.application.port.output.grpc;

import org.company.ui.domain.entity.AnswerEvent;
import org.company.ui.domain.entity.TimeFilter;
import org.company.ui.exception.DataServiceException;

import java.util.List;

/**
 * Low-level interface for fetching historical answer events from a remote server.
 */
public interface DataService {
    /**
     * Retrieves answer events that fall within the given time filter.
     *
     * @param filter the time range to query
     * @return list of events (may be empty)
     * @throws DataServiceException if communication with the server fails
     */
    List<AnswerEvent> fetchData(TimeFilter filter) throws DataServiceException;
}