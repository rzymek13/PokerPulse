package prtech.com.pokerpulse.model.player;

import java.util.ArrayList;
import java.util.List;

import javax.smartcardio.Card;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class Player {
    private String id;
    private String username;
    private String password;
    private boolean ready = false;
    private List<Card> hand = new ArrayList<>();
}