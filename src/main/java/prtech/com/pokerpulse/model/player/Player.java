package prtech.com.pokerpulse.model.player;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class Player {
    private String id;
    private String username;
    private String password;
}