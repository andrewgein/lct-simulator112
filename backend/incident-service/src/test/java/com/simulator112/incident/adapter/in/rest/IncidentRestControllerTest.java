package com.simulator112.incident.adapter.in.rest;

import com.simulator112.incident.application.port.in.CreateIncidentUseCase;
import com.simulator112.incident.application.port.in.DeleteIncidentUseCase;
import com.simulator112.incident.application.port.in.FindAvailableIncidentsUseCase;
import com.simulator112.incident.application.port.in.GetIncidentUseCase;
import com.simulator112.incident.application.port.in.UpdateIncidentUseCase;
import com.simulator112.incident.domain.common.exception.IncidentNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IncidentRestControllerTest {
    private final DeleteIncidentUseCase deleteIncident = mock(DeleteIncidentUseCase.class);
    private final IncidentRestController controller = new IncidentRestController(
            mock(CreateIncidentUseCase.class), mock(UpdateIncidentUseCase.class), deleteIncident,
            mock(GetIncidentUseCase.class), mock(FindAvailableIncidentsUseCase.class), mock(IncidentRestMapper.class));

    @Test
    void deletesIncidentWithNoContentResponse() throws Exception {
        UUID incidentId = UUID.randomUUID();
        MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new IncidentExceptionHandler()).build()
                .perform(delete("/api/v1/incidents/{incidentId}", incidentId)).andExpect(status().isNoContent());

        verify(deleteIncident).deleteIncident(incidentId);
    }

    @Test
    void returnsNotFoundForMissingIncident() throws Exception {
        UUID incidentId = UUID.randomUUID();
        doThrow(new IncidentNotFoundException(incidentId)).when(deleteIncident).deleteIncident(incidentId);

        MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new IncidentExceptionHandler()).build()
                .perform(delete("/api/v1/incidents/{incidentId}", incidentId)).andExpect(status().isNotFound());
    }
}
