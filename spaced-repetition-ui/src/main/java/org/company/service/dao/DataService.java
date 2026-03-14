package org.company.service.dao;

import org.company.domain.AnswerEvent;
import org.company.domain.TimeFilter;
import org.company.exception.DataServiceException;

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