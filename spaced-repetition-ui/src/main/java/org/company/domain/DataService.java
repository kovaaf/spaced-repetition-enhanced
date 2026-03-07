package org.company.domain;

import org.company.domain.exception.DataServiceException;

import java.util.List;

public interface DataService {
    List<AnswerEvent> fetchData(TimeFilter filter) throws DataServiceException;
}