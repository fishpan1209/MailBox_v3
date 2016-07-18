package com.fishpan1209.mailbox_v3;

import java.sql.*;
import com.mysql.jdbc.Driver;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;

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
	
	public LinkedBlockingQueue<String> getOwnerList() {
		LinkedBlockingQueue<String> owners = new LinkedBlockingQueue<String>();
		Connection conn = connect();
		try {
			Statement stmt = conn.createStatement();
			String selectOwners = "select distinct owner from OWNER";
			ResultSet rs = stmt.executeQuery(selectOwners);
			while(rs.next()){
				owners.add(rs.getString(1));
			}
			stmt.close();
			disconnect();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return owners;
	}
	
	public LinkedBlockingQueue<String> getMailslotList(String owner) {
		LinkedBlockingQueue<String> mailslots = new LinkedBlockingQueue<String>();
		Connection conn = connect();
		try {
			Statement stmt = conn.createStatement();
			String selectOwners = "select distinct mailslotID from OWNER where owner='"+owner+"'";
			ResultSet rs = stmt.executeQuery(selectOwners);
			while(rs.next()){
				mailslots.add(rs.getString(1));
			}
			stmt.close();
			disconnect();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return mailslots;
	}
	
	public String getFullPath(String owner, String mailslot) {
		String fullPathName = "";
		Connection conn = connect();
		try {
			Statement stmt = conn.createStatement();
			String selectOwners = "select fullPathName from OWNER where owner='"+owner+"' and mailslotID = '"
					+mailslot+"'";
			ResultSet rs = stmt.executeQuery(selectOwners);
			
			if(rs.next()) fullPathName = rs.getString(1);
			
			stmt.close();
			disconnect();
		} catch (SQLException e) {

			e.printStackTrace();
		}
		return fullPathName;
	}

/*	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String db_driver = "com.mysql.jdbc.Driver";
		String dbURL = "jdbc:mysql://localhost:3306/MailBox";
		String user = "test";
		String password = "123456";
		MysqlConnection conn = new MysqlConnection(db_driver, dbURL, user, password);
		LinkedBlockingQueue<String> owners = conn.getOwnerList();
		while(!owners.isEmpty()){
			try {
				System.out.println(owners.take());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
		LinkedBlockingQueue<String> mailslots = conn.getMailslotList("allen-p");
		while(!mailslots.isEmpty()){
			try {
				System.out.println(mailslots.take());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
		String fullPathName = conn.getFullPath("allen-p", "1");
		System.out.println(fullPathName);
		
		
	}
*/
}
