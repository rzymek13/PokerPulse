package prtech.com.pokerpulse.unit;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import prtech.com.pokerpulse.model.game.Hand;
import prtech.com.pokerpulse.model.player.Player;

import java.util.List;


@Slf4j
    class HandTest {



    @Test
    void handTest()
    {
        Hand hand = new Hand(List.of(new Player("TestPlayer", "password"), new Player("TestPlayer2", "password2")));

        log.info("Hand created: {}", hand);

        Assertions.assertEquals(5, hand.getCommunityCards().size());
        Assertions.assertNotEquals(hand.getCommunityCards().get(0), hand.getCommunityCards().get(1));
        Assertions.assertEquals(52 - 5 - 2 * 2, hand.getDeck().size());
    }
}
