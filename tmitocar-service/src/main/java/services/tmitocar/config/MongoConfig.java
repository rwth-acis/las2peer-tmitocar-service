package services.tmitocar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;

import java.net.UnknownHostException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
@Configuration
@EnableMongoRepositories(basePackages = {"services.tmitocar.repository"})
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${mospring.data.mongodb.database}")
    private String dbName;

    @Value("${spring.data.mongodb.host}")
    private String host;

    @Value("${spring.data.mongodb.port}")
    private Integer port;

    @Value("${spring.data.mongodb.username}")
    private String userName;

    @Value("${spring.data.mongodb.password}")
    private char password;

    @Override
    protected String getDatabaseName() {
        return this.dbName;
    }
    
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoClient(), getDatabaseName());
    }

//     @Override
//     @Bean
//     public MongoTemplate mongoTemplate(String host, int port, String authenticationDB, String database, String user, char[] password) throws UnknownHostException {
//         return new MongoTemplate(
//                 new SimpleMongoDbFactory(
//                         new MongoClient(
//                                 new ServerAddress(host, port),
//                                 Collections.singletonList(
//                                         MongoCredential.createCredential(
//                                                 user,
//                                                 authenticationDB,
//                                                 password
//                                         )
//                                 )
//                         ),
//                         database
//                 )
//         );
//     }
}
