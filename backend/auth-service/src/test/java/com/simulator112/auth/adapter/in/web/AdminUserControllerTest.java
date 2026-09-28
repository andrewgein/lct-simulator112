package com.simulator112.auth.adapter.in.web;

import com.simulator112.auth.adapter.config.JwtAuthenticationFilter;
import com.simulator112.auth.adapter.config.SecurityConfig;
import com.simulator112.auth.adapter.out.security.JwtService;
import com.simulator112.auth.application.port.in.DeleteUserUseCase;
import com.simulator112.auth.application.port.in.ValidateSessionUseCase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminUserControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private JwtService jwt;
    @MockitoBean private ValidateSessionUseCase sessions;
    @MockitoBean private DeleteUserUseCase users;

    @Test
    void adminDeletesUsingAuthenticatedPrincipalNotSpoofedHeader() throws Exception {
        var actor = UUID.randomUUID();
        var target = UUID.randomUUID();
        session(actor, "ADMIN");
        mvc.perform(delete("/api/v1/admin/users/" + target).header("Authorization", "Bearer token")
                .header("X-User-Id", UUID.randomUUID().toString())).andExpect(status().isNoContent());
        verify(users).deleteUser(target, actor);
    }

    @Test
    void teacherCannotDelete() throws Exception {
        session(UUID.randomUUID(), "SUPERVISOR");
        mvc.perform(delete("/api/v1/admin/users/" + UUID.randomUUID()).header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());
        verify(users, never()).deleteUser(any(), any());
    }

    @Test
    void unauthenticatedRequestCannotDelete() throws Exception {
        mvc.perform(delete("/api/v1/admin/users/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
        verify(users, never()).deleteUser(any(), any());
    }

    private void session(UUID id, String role) {
        when(jwt.extractUserId("token")).thenReturn(id);
        when(jwt.extractRole("token")).thenReturn(role);
        when(sessions.isSessionValid(id, role)).thenReturn(true);
    }
}
