package prtech.com.pokerpulse;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import prtech.com.pokerpulse.model.card.Deck;
import prtech.com.pokerpulse.model.player.Player;
import prtech.com.pokerpulse.repository.PlayerRepository;

@SpringBootApplication
public class PokerPulseApplication {

	public static void main(String[] args) {
		SpringApplication.run(PokerPulseApplication.class, args);
	}

//	@Bean
//	CommandLineRunner commandLineRunner(PlayerRepository repository){
//		return args -> {
//			System.out.println(repository.findAll());
////			Player player = new Player("adasad", "testPass");
////			repository.save(player);
//			System.out.println(repository.findAll());
//		};
//	}



}
