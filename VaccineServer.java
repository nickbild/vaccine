import java.net.*;
import java.io.*;
import java.util.*;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.DatabaseMetaData;

import org.sqlite.JDBC;

/*
* Nick Bild
* 2018-03-03
* Listen for client connections.
* Store application hashes and handle panic signals.
*/
public class VaccineServer {

	public static void main(String[] args) throws IOException {
        	// Check input.
		if (args.length != 1) {
			System.err.println("Usage: java VaccineServer <port>");
			System.exit(1);
		}
        
        	int port = Integer.parseInt(args[0]);

		// Set listening port.
		ServerSocket serverSocket = new ServerSocket(port);
		Socket clientSocket = null;

		System.out.println("Starting Server on port: " + port);

		// Infinite loop to accept new connections.
		while (true) {
	        	try {
				// Open a new client connection.
				clientSocket = serverSocket.accept();

				// Spawn new thread to handle client.
				Runnable clientHandler = new ClientHandler(clientSocket);
				Thread t = new ClientHandler(clientSocket);
				t.start();

			} catch (Exception e) { // Catch errors and clean up.
				clientSocket.close();
	                	e.printStackTrace();
			}
		}
	}
}

/*
* Nick Bild
* 2018-03-03
* ClientHandler is ran in a thread for each individual client connection.
* It handles all server interaction with the clients.
*/
class ClientHandler extends Thread {
	// Open database connection.
        public static Connection conn = connectDB();

	private final Socket clientSocket;

	public ClientHandler(Socket clientSocket) {
	        this.clientSocket = clientSocket;
	}

	public void run() {
		try {
			// Open readers and writers on socket.
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

			// The first line is the command.
			String line = in.readLine();
			line = line.replace("\n", "").replace("\r", ""); // Remove newlines.

			// User is sending hashes.
			if (line.equals("HASHES-BEGIN")) {
				String id = "";
				boolean firsttime = true;

				// All remaining lines are hashes for this user.
				while (!(line = in.readLine().replace("\n", "").replace("\r", "")).equals("HASHES-END")) {

					// User ID is on first line.
					if (firsttime) {
						firsttime = false;
						id = line;
						continue;
					}

					// Insert hashes into database.
                                	// Ignore hashes already in the database.
                                	String sql = "INSERT OR IGNORE INTO hash (hash, userid) VALUES ('" + line + "', '" + id + "');";
                                	Statement stmt = conn.createStatement();
                                	stmt.executeUpdate(sql);
				}

				System.out.println("Hashes received from user: " + id);
				out.println("Server response: Hashes received.");

			}

			// User is reporting a panic situation.
			else if (line.equals("PANIC-BEGIN")) {
				String id = in.readLine(); // Next line is user ID.
				id = id.replace("\n", "").replace("\r", ""); // Remove newlines.

				// Record panic in database.
				String sql = "INSERT OR IGNORE INTO panic (userid) VALUES ('" + id + "');";
				Statement stmt = conn.createStatement();
				stmt.executeUpdate(sql);

				System.out.println("Panic signal received from user: " + id);
				out.println("Server response: Panic signal received.");

			}

			out.println("Goodbye.");

			clientSocket.close();

		} catch (Exception e) {
			System.out.println(e.getMessage());

		}

	}

	// SQLite connection creation.
        private static Connection connectDB() {
                String url = "jdbc:sqlite:vaccine_server.db";
                Connection conn = null;
                try {
                        Class.forName("org.sqlite.JDBC");
                        conn = DriverManager.getConnection(url);

                } catch (Exception e) {
                        System.out.println(e.getMessage());

                }

                return conn;
        }

}

