package prtech.com.pokerpulse.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import prtech.com.pokerpulse.model.card.Card;
import prtech.com.pokerpulse.model.pan.PanChallengeRequest;
import prtech.com.pokerpulse.model.pan.PanGameSnapshot;
import prtech.com.pokerpulse.model.pan.PanDrawRequest;
import prtech.com.pokerpulse.model.pan.PanPlayRequest;
import prtech.com.pokerpulse.service.PanGameService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pan")
@CrossOrigin
public class PanGameController {

    private final PanGameService panGameService;

    public PanGameController(PanGameService panGameService) {
        this.panGameService = panGameService;
    }

    @PostMapping("/{roomId}/start")
    public ResponseEntity<PanGameSnapshot> start(@PathVariable Long roomId) {
        return ResponseEntity.ok(panGameService.startGame(roomId));
    }

    @PostMapping("/{roomId}/play")
    public ResponseEntity<PanGameSnapshot> play(@PathVariable Long roomId, @RequestBody PanPlayRequest request) {
        return ResponseEntity.ok(panGameService.playCards(roomId, request.playerId(), request.cardCodes(), request.declaredRank()));
    }

    @PostMapping("/{roomId}/challenge")
    public ResponseEntity<PanGameSnapshot> challenge(@PathVariable Long roomId, @RequestBody PanChallengeRequest request) {
        return ResponseEntity.ok(panGameService.collectCards(roomId, request.playerId()));
    }

    @PostMapping("/{roomId}/draw")
    public ResponseEntity<PanGameSnapshot> draw(@PathVariable Long roomId, @RequestBody PanDrawRequest request) {
        return ResponseEntity.ok(panGameService.drawCards(roomId, request.playerId()));
    }

    @GetMapping("/{roomId}/state")
    public ResponseEntity<PanGameSnapshot> state(@PathVariable Long roomId) {
        return ResponseEntity.ok(panGameService.getSnapshot(roomId));
    }

    @GetMapping("/{roomId}/hands")
    public ResponseEntity<Map<Long, List<Card>>> hands(@PathVariable Long roomId) {
        return ResponseEntity.ok(panGameService.getPrivateHands(roomId));
    }

    @GetMapping("/{roomId}/hands/{playerId}")
    public ResponseEntity<List<Card>> hand(@PathVariable Long roomId, @PathVariable Long playerId) {
        return ResponseEntity.ok(panGameService.getPrivateHand(roomId, playerId));
    }
}
