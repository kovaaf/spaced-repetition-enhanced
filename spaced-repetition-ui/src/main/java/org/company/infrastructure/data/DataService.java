package org.company.infrastructure.data;

import org.company.domain.AnswerEvent;
import org.company.domain.TimeFilter;
import org.company.domain.exception.DataServiceException;

import java.util.List;

public interface DataService {
    List<AnswerEvent> fetchData(TimeFilter filter) throws DataServiceException;
}