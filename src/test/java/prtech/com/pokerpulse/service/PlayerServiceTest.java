package prtech.com.pokerpulse.service;


import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import prtech.com.pokerpulse.model.player.Player;
import prtech.com.pokerpulse.repository.PlayerRepository;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
class PlayerServiceTest {

    @LocalServerPort
    private Integer port;


    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            "postgres:17.5"
    );

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    PlayerRepository repository;

    @Test
    void getAllPlayers() {
        RestAssured.baseURI = "http://localhost:" + port;

        List<Player> players = List.of(
                new Player("user1", "pass1"),
                new Player("user2", "pass2")
        );
        repository.saveAll(players);
        log.info("Saved players: {}", repository.findAll());


        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/auth/players")
                .then()
                .statusCode(200)
                .body(".", hasSize(2));
    }


}


