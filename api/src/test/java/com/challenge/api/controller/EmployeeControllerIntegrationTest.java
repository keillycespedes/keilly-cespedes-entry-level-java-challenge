package com.challenge.api.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.challenge.api.model.Employee;
import com.challenge.api.service.EmployeeService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeService employeeService;

    @Test
    void returnsAllEmployees() throws Exception {
        mockMvc.perform(get("/api/v1/employee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$[*].fullName", hasItem("Jane Doe")))
                .andExpect(jsonPath("$[*].fullName", hasItem("John Smith")))
                .andExpect(jsonPath("$[*].fullName", hasItem("Maria Garcia")));
    }

    @Test
    void returnsEmployeeByUuid() throws Exception {
        Employee employee = employeeService.searchEmployeesByName("Jane Doe").get(0);

        mockMvc.perform(get("/api/v1/employee/{uuid}", employee.getUuid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", is(employee.getUuid().toString())))
                .andExpect(jsonPath("$.fullName", is("Jane Doe")));
    }

    @Test
    void returnsNotFoundForUnknownEmployee() throws Exception {
        mockMvc.perform(get("/api/v1/employee/{uuid}", UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidUuid() throws Exception {
        mockMvc.perform(get("/api/v1/employee/not-a-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void createsEmployee() throws Exception {
        String requestBody =
                """
                {
                  "firstName": "Keilly",
                  "lastName": "Cespedes",
                  "salary": 95000,
                  "age": 25,
                  "jobTitle": "Software Engineer",
                  "email": "keilly.cespedes@example.com",
                  "contractHireDate": "2026-09-21T12:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").isNotEmpty())
                .andExpect(jsonPath("$.firstName", is("Keilly")))
                .andExpect(jsonPath("$.lastName", is("Cespedes")))
                .andExpect(jsonPath("$.fullName", is("Keilly Cespedes")));
    }

    @Test
    void rejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidEmployeeValues() throws Exception {
        String requestBody =
                """
                {
                  "firstName": "Test",
                  "lastName": "Employee",
                  "salary": -1,
                  "age": 10,
                  "jobTitle": "Tester",
                  "email": "invalid-email",
                  "contractHireDate": "2026-09-21T12:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsTerminationDateBeforeHireDate() throws Exception {
        String requestBody =
                """
                {
                  "firstName": "Test",
                  "lastName": "Employee",
                  "salary": 50000,
                  "age": 30,
                  "jobTitle": "Tester",
                  "email": "test.employee@example.com",
                  "contractHireDate": "2026-09-21T12:00:00Z",
                  "contractTerminationDate": "2026-09-20T12:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchesEmployeesByPartialNameIgnoringCase() throws Exception {
        mockMvc.perform(get("/api/v1/employee/search").queryParam("name", "jAnE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fullName", is("Jane Doe")));
    }

    @Test
    void rejectsBlankSearch() throws Exception {
        mockMvc.perform(get("/api/v1/employee/search").queryParam("name", " ")).andExpect(status().isBadRequest());
    }
}
