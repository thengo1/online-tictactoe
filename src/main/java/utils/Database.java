package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import models.GameBoard;
import models.Move;
import models.Player;

public class Database {
  
  /**
   * Main method.
   * @param args represents args in command line
   */
  public static void main(String[] args) {
    
    Database jdbc = new Database();
    
    Connection conn = jdbc.createConnection();
    jdbc.createTable(conn, "ASE_I3_MOVE");
    
  }
  
  /**
   * Creates a new connection.
   * @return Connection object
   */
  public Connection createConnection() {
    Connection conn = null; 
    
    try {
      Class.forName("org.sqlite.JDBC"); 
      conn = DriverManager.getConnection("jdbc:sqlite:ase.db"); 
      
    } catch (Exception e) {
      System.out.println(e.getClass().getName() + ":" + e.getMessage()); 
    }
    
    System.out.println("Opened database successfully"); 
    return conn;
  }
  
  /**
   * Creates new table for moves.
   * @param conn is a Connection Object 
   */
  public boolean createTable(Connection conn, String tableName) {
    Statement stmt = null; 
    
    try {
      stmt = conn.createStatement(); 
      String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " "
          + "(PLAYER_ID INT NOT NULL,"
          + " TYPE CHAR(1) NOT NULL,"
          + " MOVE_X INT NOT NULL,"
          + " MOVE_Y INT NOT NULL)";
      stmt.executeUpdate(sql);
      stmt.close(); 
    } catch (Exception e) {
      System.out.println(e.getClass().getName() + ":" + e.getMessage());
      return false; 
    } finally {
      try {
        if (stmt != null) {
          stmt.close();
        } 
      } catch (Exception e) {
        System.out.println(e);
      }
      
      try {
        if (conn != null) {
          conn.close();
        } 
      } catch (Exception e) {
        System.out.println(e);
      }
    }
    System.out.println("Table created successfully"); 
    return true; 
  }
  
  /**
   * Adds a successful move to the specified table.
   * @param conn is a Connection object
   * @param move is a Move object containing data
   * @return Boolean that indicates whether or not the move was added
   */
  public boolean addMoveData(Connection conn, String tableName, Move move) {
    Statement stmt = null; 
    
    try {
      conn.setAutoCommit(false);
      System.out.println("Opened database successfully.");
      
      stmt = conn.createStatement(); 
      String sql = "INSERT INTO ASE_I3_MOVE (PLAYER_ID, TYPE, MOVE_X, MOVE_Y) "
          + "VALUES (" 
          + move.getPlayer().getId() 
          + ", \'" + Character.toString(move.getPlayer().getType()) + "\', " 
          + move.getMoveX() + ", " + move.getMoveY() 
          + " );";
      stmt.executeUpdate(sql); 
      stmt.close(); 
      conn.commit(); 
      
    } catch (Exception e) {
      System.out.println(e.getClass().getName() + ":" + e.getMessage()); 
      return false; 
    } finally {
      try {
        if (stmt != null) {
          stmt.close();
        } 
      } catch (Exception e) {
        System.out.println(e);
      }
      
      try {
        if (conn != null) {
          conn.close();
        } 
      } catch (Exception e) {
        System.out.println(e);
      }
    }
    
    System.out.println("Move successfully added"); 
    return true;
  }
  
  /**
   * Removes the specified table from the database. 
   * @param conn is a Connection object
   * @param tableName is a String representing the table to be dropped
   * @return Boolean indicating whether or not the sql query was successful
   */
  public boolean dropTable(Connection conn, String tableName) {
    Statement stmt = null; 
    try {
      stmt = conn.createStatement(); 
      String sql =  "DROP Table IF EXISTS " + tableName;  
      stmt.execute(sql); 
      stmt.close(); 
    } catch (Exception e) {
      System.out.println(e.getClass().getName() + ":" + e.getMessage()); 
      return false; 
    } finally {
      try {
        if (stmt != null) {
          stmt.close();
        } 
      } catch (Exception e) {
        System.out.println(e);
      }
      
      try {
        if (conn != null) {
          conn.close();
        } 
      } catch (Exception e) {
        System.out.println(e);
      }
    }
    System.out.println("Table dropped successfully"); 
    return true; 
  }
  
  /**
   * Construct game board out of database entries.
   * @param conn represents a connection
   * @param tableName represents the Moves table
   * @return
   */
  public GameBoard getBoard(Connection conn, String tableName) {
    Statement stmt = null;
    
    GameBoard gameBoard = null;
    Player p1 = null;
    Player p2 = null;
    ResultSet rs = null;
    
    try {
      conn.setAutoCommit(false);
      stmt = conn.createStatement();
      
      rs = stmt.executeQuery("SELECT * FROM " + tableName);
      
      gameBoard = new GameBoard();
      
      if (rs.next()) {
        gameBoard.setGameStart(true);
        int id = rs.getInt("PLAYER_ID");
        char type = rs.getString("TYPE").charAt(0);
        p1 = new Player(id, type);
        gameBoard.setP1(p1);
        if (p1.getType() == 'X') {
          p2 = new Player(2, 'O');
        } else if (p1.getType() == 'O') {
          p2 = new Player(2, 'X');
        } else {
          System.out.println("Should not be here");
        }
        gameBoard.setP2(p2);
      }
      
      boolean isWinner = false;
      while (rs.next()) {
        if (rs.getInt("MOVE_X") < 0 || rs.getInt("MOVE_X") > 2) {
          if (rs.getInt("MOVE_Y") < 0 || rs.getInt("MOVE_Y") > 2) {
            continue;
          }
        }
        int id = rs.getInt("PLAYER_ID");
        Player currPlayer = (id == 1) ? p1 : p2;
        Move move = new Move(currPlayer, rs.getInt("MOVE_X"), rs.getInt("MOVE_Y"));
        gameBoard.makeMove(move);
        isWinner = gameBoard.checkWinner(move);
      }
      if (isWinner) {
        conn.close();
        stmt.close();
      }
      
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
    } finally {
      try {
        if (stmt != null) {
          stmt.close();
        } 
      } catch (Exception e) {
        System.out.println(e);
      }
      
      try {
        if (conn != null) {
          conn.close();
        } 
      } catch (Exception e) {
        System.out.println(e);
      }
      
      try {
        if (rs != null) {
          rs.close();
        } 
      } catch (Exception e) {
        System.out.println(e);
      }
    }  
    return gameBoard;
  }


}