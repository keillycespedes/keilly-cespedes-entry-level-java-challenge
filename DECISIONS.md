# Decisions

## 1. Which endpoints did you add, and why does this system need them?

I added `GET /api/v1/employee/search?name={name}`. Consumers may know an
employee's name without knowing their UUID, so this endpoint supports partial,
case-insensitive matches against first, last, and full names. Results are sorted
by full name to keep the response predictable.

## 2. Did you add any dependency? What does it buy, and what does it cost?

I did not add any dependencies. Spring Boot already provides the web and test
support needed for this implementation. Avoiding another dependency keeps the
build and maintenance requirements small.

## 3. How did you handle errors and logging, and why those mechanisms?

Invalid employee data produces `400 Bad Request`, and a valid UUID that does not
identify an employee produces `404 Not Found`. I used Spring's
`ResponseStatusException` because the API is small and does not need a separate
exception hierarchy yet.

I left application logging out of this in-memory version rather than logging
personal employee data. In production, I would add structured logs for request
IDs, endpoint names, response status, latency, and failures. UUIDs could be
logged when needed for investigation, but names, email addresses, and salary
data should not be included.

## 4. Where the brief left a decision to you, what did you decide?

Employees are stored in a `ConcurrentHashMap` keyed by UUID. This keeps the
in-memory implementation safe when requests arrive concurrently and makes UUID
lookups direct. The service starts with three mock employees so the GET routes
are useful immediately.

For creation, first name, last name, salary, age, job title, email, and hire date
are required. Salary must be positive, age must be between 16 and 100, email
must have a basic valid shape, and a termination date cannot be before the hire
date. Full name is generated from first and last name so the three values cannot
contradict one another.

## 5. What did you deliberately choose *not* to do?

I did not add a database, authentication provider, or external service because
the challenge keeps infrastructure out of scope. A production API would need
persistent storage and authentication before handling real employee data.

I also avoided complex email validation because fully validating an email
address is better handled by verification rather than a large regular
expression. The current check catches obvious malformed input while keeping the
rule easy to understand.
