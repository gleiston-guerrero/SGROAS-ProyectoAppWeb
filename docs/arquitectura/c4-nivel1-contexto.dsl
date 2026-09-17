workspace "SGROAS - Route and Operations Assignment Management System" "Fleet management platform" {

    model {
        u = person "User" "System operator (admin, coordinator, security)"
        s = softwareSystem "SGROAS API" "Management platform for routes, drivers, vehicles and incidents"

        u -> s "Uses the platform via" "REST API / Angular frontend"
    }

    views {
        systemContext s context "Level 1 - System Context Diagram" {
            include *
            autoLayout
        }
    }
}
