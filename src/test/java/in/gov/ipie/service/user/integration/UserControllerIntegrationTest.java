package in.gov.ipie.service.user.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import in.gov.ipie.common.testing.containers.ElasticsearchIntegrationTest;
import in.gov.ipie.common.testing.containers.PostgresIntegrationTest;
import in.gov.ipie.service.user.dto.request.CreateUserRequest;
import in.gov.ipie.service.user.dto.request.UpdateUserRequest;

/**
 * End-to-end CRUD flow against a real PostgreSQL instance and a real Elasticsearch instance
 * (master standards doc, section 11: "Integration - Database, messaging and external adapters").
 * Implementing both container mixins makes {@code spring.elasticsearch.uris} present for this
 * test class, so {@code UserSearchIndexConfig} activates the real {@code ElasticsearchUserSearchIndex}
 * - every "search users" assertion in this class therefore exercises create -> index -> search
 * against a real cluster, not the Postgres fallback. JWT authentication is switched off for this
 * test via {@code ipie.security.enabled=false} - it exercises business behaviour, not the
 * security baseline itself.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "ipie.security.enabled=false")
class UserControllerIntegrationTest implements PostgresIntegrationTest, ElasticsearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createGetUpdateDeactivateFlow() throws Exception {
        String createBody = objectMapper.writeValueAsString(
                new CreateUserRequest("jdoe", "jdoe@example.com", "Jane Doe", null, null));

        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("jdoe"))
                .andReturn();

        String userId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jdoe@example.com"));

        mockMvc.perform(get("/api/v1/users").param("username", "jdoe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("jdoe"));

        String updateBody = objectMapper.writeValueAsString(
                new UpdateUserRequest("jdoe2@example.com", "Jane A. Doe", null, null));

        mockMvc.perform(put("/api/v1/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jdoe2@example.com"));

        mockMvc.perform(delete("/api/v1/users/{id}", userId))
                .andExpect(status().isNoContent());

        // Soft delete only - the row still exists with status flipped (master standards doc, section 8).
        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void createUser_withRepeatedIdempotencyKey_doesNotCreateTwice() throws Exception {
        String body = objectMapper.writeValueAsString(
                new CreateUserRequest("asmith", "asmith@example.com", "Alex Smith", null, null));

        mockMvc.perform(post("/api/v1/users")
                        .header("Idempotency-Key", "test-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/users")
                        .header("Idempotency-Key", "test-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("asmith"));

        mockMvc.perform(get("/api/v1/users").param("username", "asmith"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void searchUsers_filtersByStatus_afterDeactivation() throws Exception {
        String body = objectMapper.writeValueAsString(
                new CreateUserRequest("esearch", "esearch@example.com", "ES Search", null, null));
        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        String userId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/users").param("username", "esearch").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(delete("/api/v1/users/{id}", userId)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users").param("username", "esearch").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/v1/users").param("username", "esearch").param("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void searchUsersAfter_paginatesThroughAllUsersWithoutDuplicatesOrGaps() throws Exception {
        String prefix = "cursoruser";
        for (int i = 0; i < 5; i++) {
            String body = objectMapper.writeValueAsString(
                    new CreateUserRequest(prefix + i, prefix + i + "@example.com", "Cursor User " + i, null, null));
            mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated());
        }

        List<String> seenUsernames = new ArrayList<>();
        String cursor = null;
        boolean hasMore = true;
        while (hasMore) {
            MockHttpServletRequestBuilder request = get("/api/v1/users/cursor")
                    .param("username", prefix)
                    .param("size", "2");
            if (cursor != null) {
                request = request.param("cursor", cursor);
            }

            MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
            JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
            json.get("content").forEach(node -> seenUsernames.add(node.get("username").asText()));

            hasMore = json.get("hasMore").asBoolean();
            cursor = hasMore ? json.get("nextCursor").asText() : null;
        }

        assertThat(seenUsernames).containsExactlyInAnyOrder(
                prefix + "0", prefix + "1", prefix + "2", prefix + "3", prefix + "4");
    }

    @Test
    void searchUsers_sortsByAnAllowedField() throws Exception {
        String bodyA = objectMapper.writeValueAsString(
                new CreateUserRequest("sortuserb", "sortuserb@example.com", "Sort User B", null, null));
        String bodyB = objectMapper.writeValueAsString(
                new CreateUserRequest("sortusera", "sortusera@example.com", "Sort User A", null, null));
        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(bodyA))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(bodyB))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/users")
                        .param("username", "sortuser")
                        .param("sortBy", "USERNAME")
                        .param("sortDirection", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("sortusera"))
                .andExpect(jsonPath("$.content[1].username").value("sortuserb"));
    }

    @Test
    void searchUsers_rejectsAnUnrecognizedSortField() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("sortBy", "NOT_A_REAL_COLUMN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("sortBy"));
    }

    @Test
    void createUser_rejectsDuplicateUsername() throws Exception {
        String firstBody = objectMapper.writeValueAsString(
                new CreateUserRequest("dupuser", "dup1@example.com", "Dup One", null, null));
        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(firstBody))
                .andExpect(status().isCreated());

        String secondBody = objectMapper.writeValueAsString(
                new CreateUserRequest("dupuser", "dup2@example.com", "Dup Two", null, null));
        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(secondBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USERNAME_ALREADY_EXISTS"));
    }

    /**
     * Proves the i18n wiring end to end for a real service: this module's own
     * messages_hi.properties (not just common-libs' shared bundle) is picked up automatically via
     * the platform default spring.messages.basename, and GlobalExceptionHandler resolves it
     * through the request's Accept-Language header with no per-endpoint code.
     */
    @Test
    void getUser_notFound_returnsTheHindiTranslationWhenAcceptLanguageAsksForIt() throws Exception {
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/users/{id}", missingId).header("Accept-Language", "hi"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("उपयोगकर्ता नहीं मिला"));
    }

    @Test
    void getUser_notFound_returnsEnglishByDefault() throws Exception {
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/users/{id}", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("User " + missingId + " was not found"));
    }
}

