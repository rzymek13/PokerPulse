PokerPulse — Agent Guide

Build/Test
- Build/run: `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`)
- Package: `./mvnw -DskipTests package`
- All tests: `./mvnw test`
- Single test class: `./mvnw -Dtest=PlayerServiceTest test`
- Single test method: `./mvnw -Dtest=PlayerServiceTest#login test`

Architecture
- Spring Boot 3.5 (Java 21). Starters: Web, WebSocket, Data JPA, Validation, Test, DevTools; Lombok used.
- REST: rooms API in [GameRoomController.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/main/java/prtech/com/pokerpulse/controller/GameRoomController.java) under `/api/rooms`.
- WebSocket/STOMP: endpoint `/poker`, app prefix `/app`, broker `/topic` per [WebSocketConfig.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/main/java/prtech/com/pokerpulse/config/WebSocketConfig.java); chat handlers in [ChatController.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/main/java/prtech/com/pokerpulse/controller/ChatController.java) and [PokerWebSocketController.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/main/java/prtech/com/pokerpulse/controller/PokerWebSocketController.java).
- Services: in-memory state (no DB by default) via [GameService.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/main/java/prtech/com/pokerpulse/service/GameService.java) and [PlayerService.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/main/java/prtech/com/pokerpulse/service/PlayerService.java).
- Models: cards/game/player/room/chat under `model/*` (e.g., [Deck.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/main/java/prtech/com/pokerpulse/model/card/Deck.java), [GameRoom.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/main/java/prtech/com/pokerpulse/model/room/GameRoom.java)).
- App entry: [PokerPulseApplication.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/main/java/prtech/com/pokerpulse/PokerPulseApplication.java) (excludes DataSource auto-config).

Database
- Optional Postgres via `docker-compose.yml` (pgAdmin included). To enable DB, configure [application.properties](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/main/resources/application.properties) datasource, remove DataSource exclusion, and add entities/repos.

Code Style
- Java 21, package `prtech.com.pokerpulse`; Lombok `@Data`/`@AllArgsConstructor` used for POJOs.
- Controllers return `ResponseEntity` for REST; WebSocket uses STOMP messaging template.
- Errors: throw `IllegalArgumentException`/`RuntimeException` for validation/not-found (consider converting to proper HTTP errors if needed).
- Tests: JUnit 5 in `src/test/java` (e.g., [PlayerServiceTest.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/test/java/prtech/com/pokerpulse/service/PlayerServiceTest.java)).

Meta
- No repo-specific Cursor/Claude/Windsurf/Cline/Goose/Copilot rules found.
