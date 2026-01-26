package hr.elektropregled.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Elektropregled API")
                        .version("1.0.0")
                        .description("REST API za sinkronizaciju pregleda elektroenergetskih postrojenja s mobilne aplikacije")
                        .contact(new Contact()
                                .name("Elektropregled Tim")
                                .email("admin@elektropregled.hr")
                                .url("https://elektropregled.hr"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")));
    }
}