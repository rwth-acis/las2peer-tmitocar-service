package services.tmitocar.config;
import java.util.List;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
 
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAIServiceconfig() {
        Server sv = new Server();
        sv.setUrl("http://localhost:8080");
        sv.setDescription("Development environment URL");
        Contact contact = new Contact();
        contact.email("yue.yin@rwth-aachen.de");
        contact.name("Alexander Tobias Neumann, Yue Yin");
        License lis = new License().name("ACIS License (BSD3)").url("https://github.com/rwth-acis/las2peer-tmitocar-Service/blob/master/LICENSE");
        Info info = new Info()
            .title("tmitocar Service")
            .version("3.0.0")
            .contact(contact)
            .description("A tmitocar wrapper service for analyzing/evaluating texts.")
            .license(lis)
            .termsOfService("https://tech4comp.de/");
        return new OpenAPI().info(info).servers(List.of(sv));
    }
    
}
