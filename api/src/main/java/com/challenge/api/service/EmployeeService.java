package com.challenge.api.service;

import com.challenge.api.model.CreateEmployeeRequest;
import com.challenge.api.model.DefaultEmployee;
import com.challenge.api.model.Employee;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmployeeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmployeeService.class);

    private final Map<UUID, Employee> employees = new ConcurrentHashMap<>();

    public EmployeeService() {
        addMockEmployee("Jane", "Doe", 90000, 32, "Software Engineer", "jane.doe@example.com");
        addMockEmployee("John", "Smith", 85000, 29, "Product Analyst", "john.smith@example.com");
        addMockEmployee("Maria", "Garcia", 105000, 41, "Engineering Manager", "maria.garcia@example.com");
    }

    public List<Employee> getAllEmployees() {
        List<Employee> result = new ArrayList<>(employees.values());
        result.sort(Comparator.comparing(Employee::getFullName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public Optional<Employee> getEmployeeByUuid(UUID uuid) {
        Optional<Employee> employee = Optional.ofNullable(employees.get(uuid));
        if (employee.isEmpty()) {
            LOGGER.warn("Employee lookup failed for UUID {}", uuid);
        }
        return employee;
    }

    public List<Employee> searchEmployeesByName(String name) {
        if (isBlank(name)) {
            throw badRequest("Search name is required");
        }

        String searchTerm = name.trim().toLowerCase(Locale.ROOT);
        List<Employee> matches = employees.values().stream()
                .filter(employee -> employee.getFirstName()
                                .toLowerCase(Locale.ROOT)
                                .contains(searchTerm)
                        || employee.getLastName().toLowerCase(Locale.ROOT).contains(searchTerm)
                        || employee.getFullName().toLowerCase(Locale.ROOT).contains(searchTerm))
                .sorted(Comparator.comparing(Employee::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        LOGGER.debug("Employee search returned {} result(s)", matches.size());
        return matches;
    }

    public Employee createEmployee(CreateEmployeeRequest request) {
        validate(request);

        UUID uuid = UUID.randomUUID();
        Employee employee = new DefaultEmployee(
                uuid,
                request.getFirstName().trim(),
                request.getLastName().trim(),
                request.getFirstName().trim() + " " + request.getLastName().trim(),
                request.getSalary(),
                request.getAge(),
                request.getJobTitle().trim(),
                request.getEmail().trim(),
                request.getContractHireDate(),
                request.getContractTerminationDate());

        employees.put(uuid, employee);
        LOGGER.info("Created employee with UUID {}", uuid);
        return employee;
    }

    private void addMockEmployee(
            String firstName, String lastName, Integer salary, Integer age, String jobTitle, String email) {
        UUID uuid = UUID.randomUUID();
        employees.put(
                uuid,
                new DefaultEmployee(
                        uuid,
                        firstName,
                        lastName,
                        firstName + " " + lastName,
                        salary,
                        age,
                        jobTitle,
                        email,
                        Instant.now(),
                        null));
    }

    private void validate(CreateEmployeeRequest request) {
        if (request == null) {
            throw badRequest("Request body is required");
        }
        if (isBlank(request.getFirstName())) {
            throw badRequest("First name is required");
        }
        if (isBlank(request.getLastName())) {
            throw badRequest("Last name is required");
        }
        if (isBlank(request.getJobTitle())) {
            throw badRequest("Job title is required");
        }
        if (isBlank(request.getEmail()) || !isValidEmail(request.getEmail())) {
            throw badRequest("A valid email is required");
        }
        if (request.getSalary() == null || request.getSalary() <= 0) {
            throw badRequest("Salary must be positive");
        }
        if (request.getAge() == null || request.getAge() < 16 || request.getAge() > 100) {
            throw badRequest("Age must be between 16 and 100");
        }
        if (request.getContractHireDate() == null) {
            throw badRequest("Contract hire date is required");
        }
        if (request.getContractTerminationDate() != null
                && request.getContractTerminationDate().isBefore(request.getContractHireDate())) {
            throw badRequest("Contract termination date cannot be before the hire date");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isValidEmail(String email) {
        String trimmedEmail = email.trim();
        int atIndex = trimmedEmail.indexOf('@');
        return atIndex > 0 && atIndex == trimmedEmail.lastIndexOf('@') && atIndex < trimmedEmail.length() - 1;
    }

    private ResponseStatusException badRequest(String message) {
        LOGGER.warn("Rejected employee request: {}", message);
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
