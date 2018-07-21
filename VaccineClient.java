import java.io.*;
import java.net.*;
import java.lang.Runtime;

import java.util.Random;

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
* A simple client used to pass a JSON string to the specified server.
*/
public class VaccineClient {
	// Open database connection.
        public static Connection conn = connectDB();

	public static void main(String[] args) throws IOException {
       		// Check input.
		if (args.length < 2) {
			usage();
			System.exit(1);
	        }

		// Run action chosen by user.
		if (args[0].equals("runHash")) { // Hash executables.
			int status = hashDir(args[1]);

			if (status == 0) {
				System.out.println("Hashing complete.");

			} else {
				System.out.println("Error running hashing!");
				return;

			}
			
		} else if (args[0].equals("transmitHash")) { // Send hashes to server.
	        	String host = args[1];
	        	int port = Integer.parseInt(args[2]);

			int status = transmitHashes(host, port);

			if (status == 0) {
                                System.out.println("Hashes transmitted.");

                        } else {
                                System.out.println("Error transmitting hashes!");
                                return;

                        }

		} else if (args[0].equals("panic")) { // Panic!
			String host = args[1];
			int port = Integer.parseInt(args[2]);

			int status = transmitPanic(host, port);

                        if (status == 0) {
                                System.out.println("Server alerted.");

                        } else {
                                System.out.println("Error alerting server!");
                                return;

                        }
	
		} else { // Display program usage.
			usage();
			return;

		}
	}

	// Program usage.
	public static void usage() {
		System.err.println("Usage: java VaccineClient transmitHash <host> <port>");
		System.err.println("Usage: java VaccineClient runHash <directory>");
		System.err.println("Usage: java VaccineClient panic <host> <port>");
	}

	// SQLite connection creation.
        private static Connection connectDB() {
                String url = "jdbc:sqlite:vaccine.db";
                Connection conn = null;
                try {
                        Class.forName("org.sqlite.JDBC");
                        conn = DriverManager.getConnection(url);

                } catch (Exception e) {
                        System.out.println(e.getMessage());

                }

                return conn;
        }

	// Hash all executables in a directory.
	// Store hashes in a database.
	public static int hashDir(String dir) {
		Process process = null;

		try {
			// Find files and hash them.
			process = Runtime.getRuntime().exec(new String[]{"bash", "-c", "find " + dir + " -executable -type f -exec md5sum \"{}\" +"});

		} catch (Exception e) {
			System.out.println("Error hashing files: " + e);
			return 1;

		}

		// Store results.
		try {
                	BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				// Split components of string.
				String[] split = line.split("\\s+");
				String hash = split[0];
				String exe = split[1];

				// Insert hashes into database.
				// Ignore hashes already in the database.
				// Mark any new addition as not yet transferred to server.
				String sql = "INSERT OR IGNORE INTO hash (hash, exe, txflag) VALUES ('" + hash + "', '" + exe + "', 0);";
                	        Statement stmt = conn.createStatement();
                        	stmt.executeUpdate(sql);

			}

		} catch (Exception e) {
			System.out.println("Error adding hashes to database: " + e);
			return 1;

		}

		return 0;

	}

	// Transmit panic signal to remote server.
	public static int transmitPanic(String host, int port) {
		try {   // Try to open socket connection.
                        // Establish a socket connection with the server.
                        Socket serverConn = new Socket(host, port);

                        // Open readers and writers on the socket.
                        PrintWriter out = new PrintWriter(serverConn.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(serverConn.getInputStream()));

                        // Socket opened successfully.
                        // Get unique ID of this computer.
                        String id = getID();

			// Send data to server.
                        out.println("PANIC-BEGIN");
                        out.println(id);

			// Wait for server response.
			System.out.println(in.readLine());

			// Clean up.
                        serverConn.close();

		} catch (UnknownHostException e) { // Host connection error.
                        System.err.println("Failed to connect to host: " + host);
                        return 1;

                } catch (IOException e) { // Socket I/O error.
                        System.err.println("Error getting I/O from: " + host);
                        return 1;

                } catch (Exception e) {
                        System.err.println("Error transmitting panic to server: " + e);
                        return 1;

                }

                return 0;

        }

	// Transmit hashes to remote server.
	public static int transmitHashes(String host, int port) {
		try { 	// Try to open socket connection.
                	// Establish a socket connection with the server.
                        Socket serverConn = new Socket(host, port);

                        // Open readers and writers on the socket.
                        PrintWriter out = new PrintWriter(serverConn.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(serverConn.getInputStream()));

                	// Socket opened successfully.
			// Get unique ID of this computer.
			String id = getID();

			// Prepare data for sending.

                        // Send data to server.
			out.println("HASHES-BEGIN");
			out.println(id);

			// Get hashes from DB.
			String sql = "SELECT hash FROM hash WHERE txflag=0;";
                        Statement stmt = conn.createStatement();
                        stmt.executeQuery(sql);
                        ResultSet rs = stmt.executeQuery(sql);

			// Send each hash.
			while (rs.next()) {
                        	out.println(rs.getString("hash"));
			}

			out.println("HASHES-END");

			// Wait for server response.
			System.out.println(in.readLine());

                        // Clean up.
                        serverConn.close();

			// Mark hashes as transmitted.
			sql = "UPDATE hash SET txflag=1 WHERE txflag=0;";
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);

                } catch (UnknownHostException e) { // Host connection error.
                        System.err.println("Failed to connect to host: " + host);
                        return 1;

                } catch (IOException e) { // Socket I/O error.
                        System.err.println("Error getting I/O from: " + host);
                        return 1;

                } catch (Exception e) {
			System.err.println("Error transmitting hashes to server: " + e);
			return 1;

		}

		return 0;

	}

	// Get this computer's unique ID.
	// Assign an ID if it doesn't have one yet.
	public static String getID() {
		String id = "";

		try {
			// Check for existing ID.
			String sql = "SELECT COUNT(*) AS count FROM computerid;";
        	        Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
                        int count = rs.getInt("count");

			// If an ID exists, get it.
			if (count > 0) {
				sql = "SELECT id FROM computerid;";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				id = rs.getString("id");
			}

			// If the ID hasn't been defined yet...
			if (id.equals("")) {
				// Get random alphanumeric string.
				id = getRandomAlphaNum(16);

				// Store ID.
				sql = "INSERT INTO computerid (id) VALUES ('" + id + "');";
				stmt = conn.createStatement();
                        	stmt.executeUpdate(sql);
			}

		} catch (Exception e) {
			System.out.println("Error getting computer ID: " + e);
			return "";

		}

		return id;

	}

	// Random alphanumeric string generator.
        public static String getRandomAlphaNum(int size) {
                String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
                StringBuilder charsSB = new StringBuilder();
                Random rnd = new Random();

                while (charsSB.length() < size) {
                        int index = (int) (rnd.nextFloat() * chars.length());
                        charsSB.append(chars.charAt(index));
                }

                String charsStr = charsSB.toString();
                return charsStr;

        }

}

