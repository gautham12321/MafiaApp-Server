##Mafia Game Ktor Server
#Overview
The Mafia Game Ktor Server manages the backend logic and state for the Mafia game app. It handles WebSocket connections for real-time communication, managing game rooms, and synchronizing game states among players. The server ensures seamless game progression by executing actions based on player interactions without the need for a physical narrator.

#Features
    Create and join game rooms
    Real-time player and game state synchronization
    Manage game settings and player roles
    Handle various game actions such as voting, role actions, and game state updates
    Disconnect players and clean up resources
    WebSocket Endpoints
    
The server uses WebSockets to handle real-time interactions. Below are the actions supported:

    Create_Room: Creates a new game room
    Request: Create_Room#{playerDetails}
    Join_Room: Joins an existing game room
    Request: Join_Room#{request}
    Role_Revealed: Reveals roles to players
    Request: Role_Revealed#{roomId}
    randomize_roles: Randomizes player roles in a room
    Request: randomize_roles#{roomId}
    game_Settings: Updates game settings
    Request: game_Settings#{request}
    start_game: Starts the game
    Request: start_game#{roomId}
    ExitRoom: Disconnects a player from the room
    Request: ExitRoom
    get_RoomUpdate: Sends the latest room state to a player
    Request: get_RoomUpdate
    role_action: Executes a role-specific action
    Request: role_action#{request}
    vote: Submits a vote
    Request: vote#{request}
    restartGame: Restarts the game
    Request: restartGame#{roomId}
    Search_Room: Searches for a room by ID
    Request: Search_Room#{roomId}
    Sync_Players: Synchronizes player information in a room
    Request: Sync_Players#{roomId}
    DoTasks: Testing endpoint for performing predefined tasks
    Request: DoTasks
    showCurrentPlayers: Shows the current players in a room
    Request: showCurrentPlayers#{roomId}
    doCurrentRole: Executes a role-specific action for testing
    Request: doCurrentRole#{target}
    voteall: Executes votes for testing
    Request: voteall#{target}
    revealed: Reveals roles (testing)
    Request: revealed#{roomId}
    
#Implementation Details
The server is implemented using Ktor with WebSocket support. It manages various actions and game state updates through the MafiaGame class, ensuring all game logic is centralized and easily manageable
