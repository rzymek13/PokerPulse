CREATE TABLE IF NOT EXISTS players (
  player_id serial not null,
  username VARCHAR(100) UNIQUE not null,
  password VARCHAR(100) not null,
	primary key (player_id)
);

CREATE TABLE IF NOT EXISTS game_rooms (
  game_room_id serial not null,
  room_name VARCHAR(100) UNIQUE not null,
	primary key (game_room_id)
);
