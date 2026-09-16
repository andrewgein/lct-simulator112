package com.simulator112.contextmanager.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.simulator112.contextmanager.service.ContextService;
import com.simulator112.contextmanager.service.SolutionService;
import com.simulator112.contextmanager.dto.request.SolutionContextRequest;
import com.simulator112.contextmanager.dto.response.SolutionContextSaveResponse;
import com.simulator112.contextmanager.dto.response.SolutionContextView;

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

import com.simulator112.contextmanager.dto.request.CreateContextDto;
import com.simulator112.contextmanager.model.entity.Context;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/context")
@RequiredArgsConstructor
public class ContextController {

    private final ContextService contextService;
    private final SolutionService solutionService;

    @PostMapping
    public ResponseEntity<UUID> create(@RequestHeader("X-User-Id") UUID userId, @RequestBody CreateContextDto body) {
        Context context = contextService.create(userId, body.levelId());
        log.info("Контекст {} для пользователя {} создан", context.getUuid(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(context.getUuid());
    }

    @PostMapping("/{uuid}/dialups/{dialupId}/cards")
    public ResponseEntity<SolutionContextSaveResponse> createCardForDialup(
            @PathVariable UUID uuid,
            @PathVariable UUID dialupId,
            @RequestBody SolutionContextRequest solutionContext) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(solutionService.createCardForDialup(uuid, dialupId, solutionContext));
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

    @PostMapping("/{uuid}/close")
    public ResponseEntity<Void> close(@PathVariable UUID uuid) {
        contextService.closeContext(uuid);
        log.info("Контекст {} закрыт оператором", uuid);
        return ResponseEntity.accepted().build();
    }

}
