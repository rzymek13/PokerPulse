CREATE TABLE IF NOT EXISTS players (
  id VARCHAR(64) PRIMARY KEY,
  username VARCHAR(100) UNIQUE NOT NULL,
  password VARCHAR(100) NOT NULL,
  ready BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS game_rooms (
  room_id INT PRIMARY KEY,
  room_name VARCHAR(255),
  creator_username VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS game_room_players (
  room_id INT NOT NULL,
  username VARCHAR(100) NOT NULL,
  PRIMARY KEY (room_id, username),
  FOREIGN KEY (room_id) REFERENCES game_rooms(room_id)
);

CREATE TABLE IF NOT EXISTS game_room_chat (
  room_id INT NOT NULL,
  sender_username VARCHAR(100),
  content VARCHAR(1000),
  timestamp VARCHAR(50),
  FOREIGN KEY (room_id) REFERENCES game_rooms(room_id)
);

