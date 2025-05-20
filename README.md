# InvoiceSender

_The service processes invoices from Raindance and tries to deliver them using Kivra._

## Getting Started

### Prerequisites

- **Java 21 or higher**
- **Maven**
- **MariaDB**
- **Git**
- **[Dependent Microservices](#dependencies)**

### Installation

1. **Clone the repository:**

```bash
git clone https://github.com/Sundsvallskommun/api-service-invoice-sender.git
cd api-service-invoice-sender
```

2. **Configure the application:**

   Before running the application, you need to set up configuration settings.
   See [Configuration](#configuration)

   **Note:** Ensure all required configurations are set; otherwise, the application may fail to start.

3. **Ensure dependent services are running:**

   If this microservice depends on other services, make sure they are up and accessible. See [Dependencies](#dependencies) for more details.

4. **Build and run the application:**

   ```bash
   mvn spring-boot:run
   ```

   **Run the service locally:**

   Start the docker containers for wiremock and samba using the docker-compose file located in `src/test/resources/docker`.

   ```bash
   docker-compose up
   ```

   Place the .7z-files in the Kivra folder here: `src/test/resources/share/Kivra`. This will make the files available for the service via the samba container.

## Dependencies

This microservice depends on the following services:

- **Citizen**
  - **Purpose:** Used to check if a citizen have protected identity.
- **Messaging**
  - **Purpose:** Is used for sending the Kivra requests and also status reports.
  - **Repository:** [https://github.com/Sundsvallskommun/api-service-messaging](https://github.com/Sundsvallskommun/api-service-messaging.git)
  - **Setup Instructions:** See documentation in repository above for installation and configuration steps.
- **Party**
  - **Purpose:** Is used to convert the personal number of the citizens to a partyId.
  - **Repository:** [https://github.com/Sundsvallskommun/api-service-party](https://github.com/Sundsvallskommun/api-service-party.git)
  - **Setup Instructions:** See documentation in repository above for installation and configuration steps.

Ensure that these services are running and properly configured before starting this microservice.

## API Documentation

Access the API documentation via Swagger UI:

- **Swagger UI:** [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

## Usage

### API Endpoints

See the [API Documentation](#api-documentation) for detailed information on available endpoints.

### Example Request

```bash
curl -X POST http://localhost:8080/2281/batches/trigger/2025-01-01
```

## Configuration

Configuration is crucial for the application to run successfully. Ensure all necessary settings are configured in `application.yml`.

### Key Configuration Parameters

- **Server Port:**

```yaml
server:
  port: 8080
```

- **External Service URLs**

```yaml
  integration:
    citizen:
      url: <service-url>
    party:
      url: <service-url>
    messaging:
      url: <service-url>

  spring:
    security:
      oauth2:
        client:
          registration:
            citizen:
              client-id: <client-id>
              client-secret: <client-secret>
            party:
              client-id: <client-id>
              client-secret: <client-secret>
            messaging:
              client-id: <client-id>
              client-secret: <client-secret>

          provider:
            citizen:
              token-uri: <token-url>
            party:
              token-uri: <token-url>
            messaging:
              token-uri: <token-url>
```

### Additional Notes

- **Application Profiles:**

  Use Spring profiles (`dev`, `prod`, etc.) to manage different configurations for different environments.

- **Logging Configuration:**

  Adjust logging levels if necessary.

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](https://github.com/Sundsvallskommun/.github/blob/main/.github/CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the [MIT License](LICENSE).

## Status

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-invoice-sender&metric=alert_status)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-invoice-sender)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-invoice-sender&metric=reliability_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-invoice-sender)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-invoice-sender&metric=security_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-invoice-sender)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-invoice-sender&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-invoice-sender)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-invoice-sender&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-invoice-sender)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-invoice-sender&metric=bugs)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-invoice-sender)

## 

Copyright (c) 2023 Sundsvalls kommun
