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
* Analyze panic data as compared to normal data.
* Look for overrepresentation in panic data.
*/
public class VaccineAnalysis {
	// Open database connection.
        public static Connection conn = connectDB();

	public static void main(String[] args) throws IOException {
        	// Check input.
		if (args.length < 1) {
			usage();
			return;
		}

		// Run requested action.
		if (args[0].equals("overrep")) {
			int status = overRep();

		} else {
			usage();
			return;

		}
        
	}

	// Display program usage.
	public static void usage() {
		System.err.println("Usage: java VaccineAnalysis overrep");

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

	public static int overRep() {
		HashMap<String, Integer> panic = new HashMap<>();
		HashMap<String, Integer> normal = new HashMap<>();

		try {
			// Get all hashes for users that triggered a panic alert.
			String sql = "SELECT hash, COUNT(hash) AS hash_count FROM hash h WHERE h.userid IN (SELECT userid FROM panic) GROUP BY h.hash;";
                	Statement stmt = conn.createStatement();
                	stmt.executeQuery(sql);
                	ResultSet rs = stmt.executeQuery(sql);

                	while (rs.next()) {
				panic.put( rs.getString("hash"), Integer.parseInt(rs.getString("hash_count")) );
				//System.out.println( rs.getString("hash") + "\t" + rs.getString("hash_count") );
                	}

			// Get all hashes for users that did NOT trigger a panic alert.
			sql = "SELECT hash, COUNT(hash) AS hash_count FROM hash h WHERE h.userid NOT IN (SELECT userid FROM panic) GROUP BY h.hash;";
			stmt = conn.createStatement();
			stmt.executeQuery(sql);
			rs = stmt.executeQuery(sql);

			while (rs.next()) {
				normal.put( rs.getString("hash"), Integer.parseInt(rs.getString("hash_count")) );
				//System.out.println( rs.getString("hash") + "\t" + rs.getString("hash_count") );
			}

			// Find hashes overrepresented in panic group.
			System.out.println("Hash\tOverrepresentation");
			System.out.println("----\t-------------------");
			for (String key : panic.keySet()) {
				if (normal.containsKey(key)) { // Does the hash exist in the normal data set?
					int diff = panic.get(key) - normal.get(key);

					// If hash is overrepresented in panic group, display the difference.
					if (diff > 0) {
						System.out.println(key + "\t" + diff);
					}

				} else { // If key is not in the normal group, then it is overrepresented by the number of times it exists in the panic group.
					System.out.println(key + "\t" + panic.get(key));

				}
			}

		} catch (Exception e) {
			System.out.println("Error generating overrepresentation report: " + e);
			return 1;

		}

		return 0;

	}
}

