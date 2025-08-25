# PokerPulse

Aplikacja do tworzenia pokoi pokerowych z czatem w czasie rzeczywistym. Backend: Spring Boot 3 (REST + STOMP/WebSocket). Frontend: Vue 3 + Vite. Deploy: Azure (App Service + Static Web Apps).

## Status
- Zrobione
  - REST pokoje: lista, tworzenie, dołączanie ([GameRoomController.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/main/java/prtech/com/pokerpulse/controller/GameRoomController.java))
  - Czat / realtime (STOMP/SockJS) + start rozdania dla ≥2 graczy ([WebSocketConfig.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/main/java/prtech/com/pokerpulse/config/WebSocketConfig.java), [PokerWebSocketController.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/main/java/prtech/com/pokerpulse/controller/PokerWebSocketController.java))
  - Frontend: logowanie/rejestracja, lobby pokoi, czat; spójny dark UI ([App.vue](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/frontend/src/App.vue), [RoomList.vue](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/frontend/src/components/RoomList.vue), [GameRoom.vue](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/frontend/src/components/GameRoom.vue))
  - Gracze: rejestracja/logowanie (tymczasowo bez JWT), lista graczy ([AuthController.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/main/java/prtech/com/pokerpulse/controller/AuthController.java))
  - Persistencja graczy: Spring Data JDBC + Postgres ([Player.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/main/java/prtech/com/pokerpulse/model/player/Player.java), [PlayerRepository.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/main/java/prtech/com/pokerpulse/repository/PlayerRepository.java), [PlayerService.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/main/java/prtech/com/pokerpulse/service/PlayerService.java))
  - Testy: JUnit 5 (unit) i Playwright + TestNG (E2E) ([BaseTest.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/test/java/prtech/com/pokerpulse/endToEnd/base/BaseTest.java), [RegisterTest.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/test/java/prtech/com/pokerpulse/endToEnd/tests/RegisterTest.java))
- W planie
  - Logika do gry w Pokera
  - Persistencja całego pokoju/gry (room state, karty) w DB
  - Uwierzytelnianie (JWT) i autoryzacja endpointów
  - Broker relay (np. RabbitMQ) lub Azure Web PubSub dla skalowania WebSocketów
  - Historia czatu i stanu po wejściu do pokoju (preload)
  - Poprawa obsługi błędów/HTTP statusów i walidacji

## Architektura
- Backend: Spring Boot 3 (Web, WebSocket, Validation, Data JDBC)
  - STOMP endpoint: `/poker`, app prefix `/app`, broker `/topic` ([WebSocketConfig.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/main/java/prtech/com/pokerpulse/config/WebSocketConfig.java))
  - REST: `/api/rooms` (lista, tworzenie, dołącz) ([GameRoomController.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/main/java/prtech/com/pokerpulse/controller/GameRoomController.java))
  - Realtime: czat i start gry ([PokerWebSocketController.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/main/java/prtech/com/pokerpulse/controller/PokerWebSocketController.java))
  - Logika pokoju/gry ([GameService.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/main/java/prtech/com/pokerpulse/service/GameService.java))
- Frontend: Vue 3 + Vite
  - Widoki: start ([Home.vue](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/frontend/src/components/Home.vue)), logowanie ([Login.vue](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/frontend/src/components/Login.vue)), rejestracja ([Register.vue](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/frontend/src/components/Register.vue)), lobby ([RoomList.vue](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/frontend/src/components/RoomList.vue)), pokój ([GameRoom.vue](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/frontend/src/components/GameRoom.vue))
  - Konfiguracja API i SockJS: ([config.js](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/frontend/src/config.js), [api.js](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/frontend/src/api.js))

## Technologie
- Backend: Java 17, Spring Boot 3, Spring Web, WebSocket/STOMP, Validation, Spring Data JDBC, PostgreSQL
- Frontend: Vue 3, Vite (v5), Vue Router, Axios, SockJS, @stomp/stompjs
- Testy: JUnit 5, Playwright (Java), TestNG, Allure
- CI/CD i Deploy: Maven, GitHub Actions, Azure App Service (API), Azure Static Web Apps (frontend)

## Uruchomienie lokalne
- Backend
  - Wymagany Postgres (lokalnie domyślnie: `jdbc:postgresql://localhost:5432/poker-db`) i dane w [application.properties](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/main/resources/application.properties)
  - Uruchom: `mvnw.cmd spring-boot:run` (Win) lub `./mvnw spring-boot:run`
- Frontend
  - Node 18 (Vite 5)
  - `cd src/frontend && npm install && npm run dev`
  - Domyślnie API: `http://localhost:8080` (można nadpisać: `VITE_API_BASE=https://pokerpulse-<...>.azurewebsites.net npm run dev`)

## Uruchomienie testów
- Jednostkowe (JUnit): `./mvnw test`
- E2E (Playwright + TestNG): uruchamiane z Maven/IDE; konfiguracja w [BaseTest.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/test/java/prtech/com/pokerpulse/endToEnd/base/BaseTest.java) i [RegisterTest.java](file:///c:/Users/Rzymek/IdeaProjects/PokerPulse/src/test/java/prtech/com/pokerpulse/endToEnd/tests/RegisterTest.java)

## Deploy (Azure)
- Backend → Azure App Service (włącz WebSockets) 
  - Ustaw env: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- Frontend → Azure Static Web Apps
  - Build z `src/frontend` (output `dist`).
  - Ustaw `VITE_API_BASE` na URL App Service.

## API i WS (skrót)
- REST
  - `GET /api/rooms` — lista pokoi
  - `POST /api/rooms` — tworzenie (body: text/plain nazwa pokoju)
  - `POST /api/rooms/{roomId}/join` — dołącz (body: text/plain username)
- STOMP
  - Publish: `/app/chat/{roomId}` — wysłanie wiadomości
  - Subscribe: `/topic/room/{roomId}` — odbiór wiadomości
  - Publish: `/app/game/{roomId}/start` — start gry
  - Subscribe: `/topic/room/{roomId}/state` — stan pokoju/gry

## Struktura
- Backend: `src/main/java/prtech/com/pokerpulse/*`
- Frontend: `src/frontend/src/*` (+ static: `src/frontend/public/*`)

## Licencja
MIT (do ustalenia).
