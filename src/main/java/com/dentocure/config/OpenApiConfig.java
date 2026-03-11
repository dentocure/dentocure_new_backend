package com.dentocure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dentaFlowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DentaFlow EMR API")
                        .description("""
                                REST API for the **DentaFlow Dental Clinic EMR SaaS** platform.

                                Manages core clinic operations including:
                                - **Doctors & Staff** — profiles, working hours, schedules, leave
                                - **Patients** — registration, medical history, allergy alerts
                                - **Appointments** — scheduling with conflict detection, status tracking, available slots

                                ---
                                **Response envelope:**
                                ```json
                                { "data": { ... }, "meta": { "page": 1, "limit": 20, "total": 42 } }
                                ```
                                **Error shape:**
                                ```json
                                { "error": { "code": "NOT_FOUND", "message": "..." } }
                                ```

                                **Sample data IDs:** Doctors `DR001–DR005` · Patients `PT001–PT010` · Appointments `AP001–AP015`
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("DentaFlow Engineering")
                                .email("dev@dentaflow.in"))
                        .license(new License()
                                .name("Private — Internal Use Only")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development")))
                .tags(List.of(
                        new Tag().name("Doctors").description("Doctor and staff profile management"),
                        new Tag().name("Patients").description("Patient registration and medical records"),
                        new Tag().name("Appointments").description("Appointment scheduling and status tracking")));
    }
}
