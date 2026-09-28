package com.simulator112.contextmanager.domain.dds;

import com.simulator112.contextmanager.domain.common.IncidentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DdsStageDetails {
    private DdsStageType type;
    private Integer timeLimitSeconds;
    private String expectedComment;
    private String comment;
    private IncidentStatus actualStatus;

    public DdsStageDetails(DdsStageType type, Integer timeLimitSeconds, String expectedComment, String comment) {
        this(type, timeLimitSeconds, expectedComment, comment, null);
    }
}
