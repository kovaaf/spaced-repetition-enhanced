package org.company.ui.application.port.input;

import org.company.ui.domain.entity.AnswerEvent;
import org.company.ui.domain.entity.TimeFilter;
import org.company.ui.exception.DataServiceException;

import java.util.List;

public interface LoadData {
    List<AnswerEvent> execute(TimeFilter filter) throws DataServiceException;
}