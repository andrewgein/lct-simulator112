package com.simulator112.contextmanager.domain.common;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CallSnapshot {
    private UUID persistenceId;
    private UUID sourceId;
    private Integer position;
    private Integer queuePosition;
    private CallDirection direction;
    private CounterpartyType counterparty;
    private String serviceCode;
    private CallStatus status;
    private List<String> knownFacts = new ArrayList<>();
    private List<String> hiddenFacts = new ArrayList<>();
    private String aiContext;
    private Gender gender;
    private String emotionalState;
    private Person applicant;
}
