workspace "SGROAS - Level 2 - Containers" {

    model {
        u = person "User" "System operator"

        sgroas = softwareSystem "SGROAS" "Fleet management platform" {
            frontend = container "Angular Frontend" "SPA application" "Angular 20"
            api = container "Spring Boot REST API" "Java backend" "Spring Boot 3.5 + Java 21"
            db = container "PostgreSQL Database" "Primary storage" "PostgreSQL 16"
            cache = container "Redis" "Distributed cache" "Redis 7"
            jwtService = container "JWT Service" "Authentication and authorization" "jjwt 0.12.6"
        }

        u -> frontend "Browses"
        frontend -> api "HTTP requests" "JSON"
        api -> db "Read/Write" "SQL"
        api -> cache "Query cache" "Redis Protocol"
        api -> jwtService "Token validation"
    }

    views {
        container sgroas containers "Level 2 - Container Diagram" {
            include *
            autoLayout
        }
    }
}
