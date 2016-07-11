package com.fishpan1209.mailbox_v3;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class MysqlConnection {
	
	private String db_driver;
	private String dbURL;
	private String user;
	private String password;
	private Connection connection;
	private Properties properties;
	
	public MysqlConnection(String db_driver, String dbURL, String user, String password){
		this.db_driver = db_driver;
		this.dbURL = dbURL;
		this.user = user;
		this.password = password;
	}
	
	private Properties getProperties() {
	    if (properties == null) {
	        properties = new Properties();
	        properties.setProperty("user", user);
	        properties.setProperty("password", password);
	    }
	    return properties;
	}
	
	public Connection connect() {
	    if (connection == null) {
	        try {
	            Class.forName(db_driver);
	            connection = DriverManager.getConnection(dbURL, getProperties());
	        } catch (ClassNotFoundException | SQLException e) {
	            // Java 7+
	            e.printStackTrace();
	        }
	    }
	    return connection;
	}
	
	public void disconnect(){
		if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String db_driver = "";
		String dbURL = "";
		String user = "";
		String password = "";
		MysqlConnection conn = new MysqlConnection(db_driver, dbURL, user, password);
		try {
			Statement stmt = conn.connect().createStatement();
			String sql = "";
			ResultSet rs = stmt.executeQuery(sql);
			String mailslot_path = rs.getString(0);
			stmt.close();
			conn.disconnect();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

}
