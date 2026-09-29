package com.simulator112.incident.adapter.out.persistence;

import com.simulator112.incident.adapter.out.persistence.entity.common.IncidentJpaEntity;
import com.simulator112.incident.adapter.out.persistence.repository.SpringDataIncidentRepository;
import com.simulator112.incident.domain.common.Incident;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class IncidentPersistenceAdapterTest {
    private final SpringDataIncidentRepository repository = mock(SpringDataIncidentRepository.class);
    private final IncidentPersistenceMapper mapper = mock(IncidentPersistenceMapper.class);
    private final IncidentPersistenceAdapter adapter = new IncidentPersistenceAdapter(repository, mapper);

    @Test
    void deletingKeepsIncidentAccessibleByIdButRemovesItFromAvailableList() {
        var id = UUID.randomUUID();
        var entity = new IncidentJpaEntity();
        entity.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(mock(Incident.class));

        adapter.deleteById(id);

        assertThat(entity.isDeleted()).isTrue();
        assertThat(adapter.findById(id)).isPresent();
        adapter.findAvailable(null, null);
        verify(repository).findAllByDeletedFalse();
        verify(repository, never()).deleteById(any());
    }

    @Test
    void savingAnExistingDeletedIncidentDoesNotMakeItAvailableAgain() {
        var id = UUID.randomUUID();
        var existing = new IncidentJpaEntity();
        existing.setDeleted(true);
        var replacement = new IncidentJpaEntity();
        var incident = mock(Incident.class);
        when(incident.id()).thenReturn(id);
        when(mapper.toEntity(incident)).thenReturn(replacement);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(replacement)).thenReturn(replacement);

        adapter.save(incident);

        assertThat(replacement.isDeleted()).isTrue();
    }
}
