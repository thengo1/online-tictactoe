package controllers;

import com.google.gson.Gson;
import io.javalin.Javalin;
import java.io.IOException;
import java.sql.Connection;
import java.util.Queue;
import models.GameBoard;
import models.Message;
import models.Move;
import models.Player;
import org.eclipse.jetty.websocket.api.Session;
import utils.DatabaseJDBC;

public class PlayGame {

  private static final int PORT_NUMBER = 8080;

  private static Javalin app;
  
  private static Gson gson = new Gson();
  
  private static GameBoard gameBoard = new GameBoard();
  
  private static Player p1;
  
  private static Player p2;
    
  private static Move move;
  
  private static DatabaseJDBC database = new DatabaseJDBC();
  
  private static Connection conn = database.createConnection();
  
  private static String databaseName = "ASE_I3_MOVE";
  
  private static boolean tableCreated = database.createTable(conn, databaseName);

  /** Main method of the application.
   * @param args Command line arguments
   */
  public static void main(final String[] args) {

    app = Javalin.create(config -> {
      config.addStaticFiles("/public");
    }).start(PORT_NUMBER);
    
    // Starts the game and sets the turn to p1 and creates a p1 object for game.
    app.post("/startgame", ctx -> {
      database.dropTable(conn, databaseName);
      database.createTable(conn, databaseName);
      char type = '\u0000';
      if (ctx.body().contains("X")) {
        type = ctx.body().charAt(ctx.body().indexOf('X'));
      }
      if (ctx.body().contains("O")) {
        type = ctx.body().charAt(ctx.body().indexOf('O'));
      }
      p1 = new Player(1, type);
      gameBoard.setTurn(1);
      gameBoard.setP1(p1);
      ctx.result(gson.toJson(gameBoard));
    });
    
    /* Function to process move that p1 makes. First gets request body and then
     * populates gameBoard with the specified move. Also processes errors and winner.
     */
    app.post("/move/:playerId", ctx -> {
      if (gameBoard.checkPlayerNull()) {
        int playerId = Integer.parseInt(ctx.pathParam("playerId"));
        int x = Integer.parseInt(ctx.formParam("x"));
        int y = Integer.parseInt(ctx.formParam("y"));
        move = new Move(gameBoard.getP(playerId), x, y);
        
        if (gameBoard.isValidMove(move)) {
          gameBoard.makeMove(move);
          if (gameBoard.checkWinner(move)) {
            gameBoard.setTurn(0);
          }
          gameBoard.swapTurns();
          String json = gson.toJson(gameBoard);
          sendGameBoardToAllPlayers(json);
          Message message = new Message(true, 100, "");
          json = gson.toJson(message);
          ctx.result(json);
        } else {
          Message message = new Message(false, 101, "Bad Move");
          String json = gson.toJson(gameBoard);
          sendGameBoardToAllPlayers(json);
          json = gson.toJson(message);
          ctx.result(json);
        }
      } else {
        Message message = new Message(false, 102, "Wait for both players");
        String json = gson.toJson(message);
        ctx.result(json);
      }
    });
        
    // Function to start a new game. Resets board in case of previous game.
    app.get("/newgame", ctx -> {
      ctx.redirect("/tictactoe.html");
      gameBoard = new GameBoard();
      String json = gson.toJson(gameBoard);
      sendGameBoardToAllPlayers(json);
    });
    
    // Function to generate p2 and set types.
    app.get("/joingame", ctx -> {
      ctx.redirect("/tictactoe.html?p=2");
      
      char type = gameBoard.getP1().getType() == 'X' ? 'O' : 'X'; 
      p2 = new Player(2, type);
      
      gameBoard.setP2(p2);
      gameBoard.setGameStart(true);
      
      String json = gson.toJson(gameBoard);
      sendGameBoardToAllPlayers(json);
    });
    
    // Function to return JSON of game board
    app.post("/board", ctx -> {
      String json = gson.toJson(gameBoard);
      ctx.result(json);
    });

    // Web sockets - DO NOT DELETE or CHANGE
    app.ws("/gameboard", new UiWebSocket());
  }

  /** Send message to all players.
   * @param gameBoardJson Gameboard JSON
   * @throws IOException Websocket message send IO Exception
   */
  private static void sendGameBoardToAllPlayers(final String gameBoardJson) {
    Queue<Session> sessions = UiWebSocket.getSessions();
    for (Session sessionPlayer : sessions) {
      try {
        sessionPlayer.getRemote().sendString(gameBoardJson);
      } catch (IOException e) {
        // Add logger here
      }
    }
  }

  public static void stop() {
    app.stop();
  }
}
