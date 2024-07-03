package services.tmitocar.config;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
 
@Configuration
public class OpenApiConfig {

    Contact c = new Contact().email("https://tech4comp.dbis.rwth-aachen.de/").name("Alexander Tobias Neumann").url("https://tech4comp.dbis.rwth-aachen.de/");

    License l = new License().name("ACIS License (BSD3)").url("https://github.com/rwth-acis/las2peer-tmitocar-Service/blob/master/LICENSE");

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("tmitocar Service")
                        .version("3.0.0")
                        .description("A tmitocar wrapper service for analyzing/evaluating texts.")
                        .termsOfService("https://tech4comp.de/")
                        .license(l)
                        .contact(c)
                        );
    }
}
