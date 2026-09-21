# How I Built the Employee API

This is a walkthrough of how I approached the challenge and why I made the
choices I did. I wanted to keep the solution within the prompt instead of
adding tools or features that were not needed.

## Understanding the assignment

The first thing I did was identify what had to work. The assignment required
three endpoints:

- Get all employees.
- Get one employee using a UUID.
- Create an employee.

It also required tests, a `DECISIONS.md` file, and at least one endpoint of my
own. A database was not required, so I knew I could keep the employee data in
memory.

## How I organized the code

I separated the code into a controller, a service, and models.

The controller is responsible for the API routes. It receives an HTTP request,
calls the service, and returns the correct status. I kept the employee rules out
of the controller so it would not become responsible for too many things.

The service is where employees are stored, searched, and created. I used a
`ConcurrentHashMap` with the employee UUID as the key. A map makes it easy to
look up one employee, and `ConcurrentHashMap` is safer when more than one
request uses the service at the same time.

The existing `Employee` interface describes what an employee should contain. I
created `DefaultEmployee` to provide the actual fields and methods required by
that interface. I also created `CreateEmployeeRequest` for the JSON sent to the
POST endpoint.

I did not allow the POST request to choose its own UUID. The service creates one
with `UUID.randomUUID()`. I also build `fullName` from the first and last name
instead of trusting another value from the request. That prevents a request
from sending values such as `firstName: Jane` and `fullName: John Smith`.

## Building the required endpoints

All employee routes start with `/api/v1/employee`.

`GET /api/v1/employee` asks the service for every employee. The map does not
guarantee an order, so I sort the employees by full name before returning them.
This also makes the response easier to read and test.

`GET /api/v1/employee/{uuid}` looks up the UUID in the map. If the UUID is
valid but is not connected to an employee, the endpoint returns `404 Not
Found`. If the path does not contain a valid UUID at all, Spring returns `400
Bad Request`.

`POST /api/v1/employee` reads a `CreateEmployeeRequest`. If the request passes
validation, the service creates and stores the employee. The endpoint returns
`201 Created` because a new record was added.

## Validation choices

I require first name, last name, salary, age, job title, email, and contract
hire date. Salary must be greater than zero, and age must be between 16 and 100.

For email, I check that there is one `@` and that it has text on both sides. I
did not use a large email regular expression because it would make this small
project harder to follow and still would not prove that an email address is
real.

Contract termination date is optional. If one is provided, it cannot be before
the hire date. Requests that fail these checks return `400 Bad Request`.

## The endpoint I added

I added:

`GET /api/v1/employee/search?name={name}`

The original endpoints only allow someone to find an employee when they
already know the UUID. I thought searching by name would be more useful for a
person using the API.

The search checks first name, last name, and full name. It ignores uppercase
and lowercase letters and allows partial matches. For example, searching for
`jane`, `JANE`, or `Jan` can find Jane Doe. A blank search returns `400 Bad
Request`.

## Error handling and logging

I used Spring's `ResponseStatusException` for errors. It was enough for the
size of this project, and I did not think a custom exception system was needed.

The service logs useful events such as a created employee UUID, an unsuccessful
UUID lookup, a validation failure, and the number of search results. I avoided
logging names, emails, salaries, and search terms because those values are
personal employee information.

## How I tested it

I used Spring Boot's `MockMvc` to test the API as HTTP requests. This checks more
than calling the Java methods directly because it also checks the routes, JSON,
and status codes.

I wrote 10 tests for:

- Getting the employee list.
- Getting one employee by UUID.
- Looking up an unknown UUID.
- Sending an invalid UUID.
- Creating an employee.
- Sending a request with missing fields.
- Sending invalid employee values.
- Sending a termination date before the hire date.
- Searching with a partial name and different capitalization.
- Sending a blank search.

I also started the application and tested the endpoints with `curl`. The full
build and all 10 tests passed.

## Running the project

The project uses Java 17. To run the complete build on my machine, I use:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew clean build
```

To start the API, I use:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew :api:bootRun
```

Then I can check the employee list in another terminal:

```bash
curl http://localhost:8080/api/v1/employee
```

I can also test the search endpoint:

```bash
curl "http://localhost:8080/api/v1/employee/search?name=jane"
```

## What I would do differently in a real system

The employee data disappears when the application stops because it is stored
in memory. A real version would need a database.

It would also need authentication and permission checks because employee data
should not be public. I would add better monitoring, request IDs, pagination,
and a consistent JSON error format. I did not add those here because they were
outside the scope of the challenge.
