package com.alibou.booknetwork.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

// We need this configuration since we have security. We need to support bearer token and jwt authentication

@OpenAPIDefinition(
        info = @Info(
                contact = @Contact(
                        name = "biglol",
                        email = "asdf@google.com",
                        url = "https://aliboucoding.com/courses"
                ),
                description = "OpenApi documentation for Spring Security",
                title = "OpenApi specification - biglol",
                version = "1.0",
                license = @License(
                        name = "Licence name",
                        url = "https://some-url.com"
                ),
                termsOfService = "Terms of service"
        ),
        servers = {
                @Server(
                        description = "Local ENV",
                        url = "http://localhost:8080/api/v1" // you can send requests to different services
                ),
                @Server(
                        description = "Production ENV",
                        url = "https://aliboucoding.com/api/v1"
                )
        },
        security = {
                @SecurityRequirement(
                        name = "bearerAuth"
                )
        }
)
@SecurityScheme(
        name = "bearerAuth", // name should be the same as the one in the security configuration at the top. If you have another another security scheme, you can specify in @SecurityRequirement. But since we want same security scheme for all resources, we just provide same name
        description = "JWT auth description",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP, // it is security for all http requests. For our case it is bearer token for http
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER // where we are going to put the token. In our case it is in the header
)
public class OpenApiConfig {
}
