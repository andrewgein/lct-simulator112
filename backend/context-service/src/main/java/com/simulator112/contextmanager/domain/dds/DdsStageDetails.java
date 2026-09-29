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
    private java.util.List<DdsCompletionTrigger> completionTriggers;
    private boolean failOnTimeout;

    public DdsStageDetails(DdsStageType type, Integer timeLimitSeconds, String expectedComment, String comment,
                           IncidentStatus actualStatus, java.util.List<DdsCompletionTrigger> completionTriggers) {
        this(type, timeLimitSeconds, expectedComment, comment, actualStatus, completionTriggers, false);
    }

    public DdsStageDetails(DdsStageType type, Integer timeLimitSeconds, String expectedComment, String comment,
                           IncidentStatus actualStatus) {
        this(type, timeLimitSeconds, expectedComment, comment, actualStatus, type == DdsStageType.ASSIGN_BRIGADE
                ? java.util.List.of(DdsCompletionTrigger.TIME, DdsCompletionTrigger.STATUS)
                : java.util.List.of(DdsCompletionTrigger.TIME));
    }

    public DdsStageDetails(DdsStageType type, Integer timeLimitSeconds, String expectedComment, String comment) {
        this(type, timeLimitSeconds, expectedComment, comment, null);
    }
}
