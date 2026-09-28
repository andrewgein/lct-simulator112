package com.simulator112.contextmanager.adapter.in.rest;

import java.util.List;
import java.util.UUID;

import com.simulator112.contextmanager.application.port.in.ContextUseCase;
import com.simulator112.contextmanager.application.port.in.LevelProgressUseCase;
import com.simulator112.contextmanager.application.port.in.ManageSystem112CardUseCase;
import com.simulator112.contextmanager.adapter.in.rest.dto.request.ReactionStatusRequest;
import com.simulator112.contextmanager.adapter.in.rest.dto.request.DdsCommentRequest;
import com.simulator112.contextmanager.domain.common.LevelProgress;
import jakarta.validation.Valid;
import com.simulator112.contextmanager.application.model.system112.SolutionContextRequest;
import com.simulator112.contextmanager.application.model.system112.SolutionContextSaveResponse;
import com.simulator112.contextmanager.application.model.system112.SolutionContextView;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.simulator112.contextmanager.adapter.in.rest.dto.request.CreateContextDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/context")
@RequiredArgsConstructor
public class ContextController {

    private final ContextUseCase contextService;
    private final ManageSystem112CardUseCase solutionService;
    private final LevelProgressUseCase levelProgressService;

    @PostMapping
    public ResponseEntity<UUID> create(@RequestHeader("X-User-Id") UUID userId, @RequestBody CreateContextDto body) {
        UUID contextId = contextService.createContext(userId, body.assignmentId());
        log.info("Контекст {} для пользователя {} создан", contextId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(contextId);
    }

    @PostMapping("/{uuid}/calls/{callId}/cards")
    public ResponseEntity<SolutionContextSaveResponse> createCardForCall(
            @PathVariable UUID uuid,
            @PathVariable UUID callId,
            @RequestBody SolutionContextRequest solutionContext) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(solutionService.createCardForCall(uuid, callId, solutionContext));
    }

    @PostMapping("/{uuid}/cards/{cardId}/revisions")
    @ResponseStatus(HttpStatus.CREATED)
    public SolutionContextSaveResponse saveCardRevision(
            @PathVariable UUID uuid,
            @PathVariable UUID cardId,
            @RequestBody SolutionContextRequest solutionContext) {
        return solutionService.saveCardRevision(uuid, cardId, solutionContext);
    }

    @GetMapping("/{uuid}/cards")
    public List<SolutionContextView> getCards(@PathVariable UUID uuid) {
        return solutionService.getCards(uuid);
    }

    @GetMapping("/{uuid}/progress")
    public LevelProgress getProgress(@PathVariable UUID uuid) {
        return levelProgressService.getProgress(uuid);
    }

    @PostMapping("/{uuid}/dds/incidents/{incidentId}/comments")
    public LevelProgress saveDdsComment(
            @PathVariable UUID uuid,
            @PathVariable UUID incidentId,
            @Valid @RequestBody DdsCommentRequest request) {
        return levelProgressService.saveDdsComment(uuid, incidentId, request.stageId(), request.comment());
    }

    @PostMapping("/{uuid}/dds/incidents/{incidentId}/reaction-status")
    public LevelProgress applyReactionStatus(
            @PathVariable UUID uuid,
            @PathVariable UUID incidentId,
            @Valid @RequestBody ReactionStatusRequest request) {
        return levelProgressService.applyReactionStatus(
                uuid, incidentId, request.serviceCode(), request.status(), request.comment());
    }

    @PostMapping("/{uuid}/close")
    public ResponseEntity<Void> close(@PathVariable UUID uuid) {
        contextService.closeContext(uuid);
        log.info("Контекст {} закрыт оператором", uuid);
        return ResponseEntity.accepted().build();
    }

}
