package prtech.com.pokerpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import prtech.com.pokerpulse.model.card.Deck;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
public class PokerPulseApplication {

	public static void main(String[] args) {
		SpringApplication.run(PokerPulseApplication.class, args);
		Deck deck = new Deck();
		System.out.println(deck.initializeShuffledDeck());
	}



}
