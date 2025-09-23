CREATE TABLE IF NOT EXISTS player (
  player_id serial not null,
  username VARCHAR(100) UNIQUE not null,
  password VARCHAR(100) not null,
	primary key (player_id)
);