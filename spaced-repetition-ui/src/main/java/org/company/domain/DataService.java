package org.company.domain;

import java.util.List;

public interface DataService {
    List<AnswerEvent> fetchData(TimeFilter filter) throws Exception;
}