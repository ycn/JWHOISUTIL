package com.jwhois.util;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.jwhois.core.Utility;
import com.jwhois.core.WhoisMap;

public class DBHelper {

	private boolean		created;
	private Connection	outconn;
	private String		pre	= "";

	public DBHelper(Connection conn) {
		this( conn, "" );
	}

	public DBHelper(Connection conn, String pre) {
		this.outconn = conn;
		if (pre != null)
			this.pre = pre;
		this.created = false;
	}

	public String getDBPrefix() {
		return pre;
	}

	private String getPre(String tablename) {
		return pre + tablename;
	}

	public void initDB() {
		if (created || outconn == null)
			return;

		updateDB( "DROP TABLE IF EXISTS " + getPre( "domainname" ) );
		StringBuilder sql = new StringBuilder();
		sql.append( "CREATE TABLE " + getPre( "domainname" ) + "(" );
		sql.append( "`id`               int unsigned NOT NULL primary key auto_increment," );
		sql.append( "`domain`           varchar(100) NOT NULL," );
		sql.append( "`rawdata`          text NOT NULL," );
		sql.append( "`querydate`        datetime NOT NULL," );
		sql.append( "INDEX ( `domain` )" );
		sql.append( ") ENGINE=MyISAM DEFAULT CHARSET=utf8" );
		updateDB( sql.toString() );

		updateDB( "DROP TABLE IF EXISTS " + getPre( "regyinfo" ) );
		sql = new StringBuilder();
		sql.append( "CREATE TABLE " + getPre( "regyinfo" ) + "(" );
		sql.append( "`id`               int unsigned NOT NULL primary key auto_increment," );
		sql.append( "`domID`            int unsigned NOT NULL," );
		sql.append( "`type`             char(10) NOT NULL," );
		sql.append( "`registrar`        text," );
		sql.append( "`referrer`         text," );
		sql.append( "`whoisserver`      varchar(100)," );
		sql.append( "INDEX ( `domID` )" );
		sql.append( ") ENGINE=MyISAM DEFAULT CHARSET=utf8" );
		updateDB( sql.toString() );

		updateDB( "DROP TABLE IF EXISTS " + getPre( "domain" ) );
		sql = new StringBuilder();
		sql.append( "CREATE TABLE " + getPre( "domain" ) + "(" );
		sql.append( "`id`               int unsigned NOT NULL primary key auto_increment," );
		sql.append( "`domID`            int unsigned NOT NULL," );
		sql.append( "`name`             varchar(100) NOT NULL," );
		sql.append( "`created`          varchar(100)," );
		sql.append( "`changed`          varchar(100)," );
		sql.append( "`expires`          varchar(100)," );
		sql.append( "`status`           varchar(200)," );
		sql.append( "`sponsor`          varchar(100)," );
		sql.append( "`nserver`          text," );
		sql.append( "`ip`               varchar(50)," );
		sql.append( "`country`          varchar(100)," );
		sql.append( "`countrycode`      varchar(20)," );
		sql.append( "INDEX ( `domID` )" );
		sql.append( ") ENGINE=MyISAM DEFAULT CHARSET=utf8" );
		updateDB( sql.toString() );

		initContactDB( "owner" );
		initContactDB( "admin" );
		initContactDB( "bill" );
		initContactDB( "tech" );
		initContactDB( "zone" );
		initContactDB( "abuse" );
		initContactDB( "network" );
	}

	private void initContactDB(String name) {
		updateDB( "DROP TABLE IF EXISTS " + getPre( name ) );
		StringBuilder sql = new StringBuilder();
		sql.append( "CREATE TABLE " + getPre( name ) + "(" );
		sql.append( "`id`               int unsigned NOT NULL primary key auto_increment," );
		sql.append( "`domID`            int unsigned NOT NULL," );
		sql.append( "`name`             varchar(100) NOT NULL," );
		sql.append( "`email`            varchar(100)," );
		sql.append( "`phone`            varchar(100)," );
		sql.append( "`fax`              varchar(100)," );
		sql.append( "`organization`     varchar(200)," );
		sql.append( "`address`          text," );
		sql.append( "`info`             text," );
		sql.append( "`ip`               varchar(50)," );
		sql.append( "`created`          varchar(100)," );
		sql.append( "`changed`          varchar(100)," );
		sql.append( "INDEX ( `domID` )" );
		sql.append( ") ENGINE=MyISAM DEFAULT CHARSET=utf8" );
		updateDB( sql.toString() );
	}

	// The main entry to save whois info.

	@SuppressWarnings("unchecked")
	public int insertDB(String dom, WhoisMap whoisMap) {
		int domID = 0;

		if (!Utility.isValidDom( dom ) || whoisMap == null || whoisMap.isEmpty())
			return domID;

		//begin to insert database

		String sql = "";

		List<String> rawdata = ( List<String> ) whoisMap.get( "rawdata" );

		sql = "INSERT INTO " + getPre( "domainname" ) + "(domain,rawdata,querydate) VALUES('%1$s','%2$s',NOW())";
		sql = String.format( sql, escapeQuotes( dom ), list2string( rawdata ) );
		domID = updateDB( sql );

		if (0 == domID)
			return domID;

		//update regyinfo table

		Map<String, Object> regyinfo = ( Map<String, Object> ) whoisMap.get( "regyinfo" );
		if (!Utility.isEmpty( regyinfo )) {
			String type, registrar, referrer, whoisserver;

			type = getString( regyinfo.get( "type" ), false );
			registrar = getString( regyinfo.get( "registrar" ), false );
			referrer = getString( regyinfo.get( "referrer" ), false );
			whoisserver = getString( regyinfo.get( "servers" ), false );

			sql = "INSERT INTO " + getPre( "regyinfo" )
					+ "(domID,type,registrar,referrer,whoisserver) VALUES(%1$s,'%2$s','%3$s','%4$s','%5$s')";
			sql = String.format( sql, domID, type, registrar, referrer, whoisserver );
			updateDB( sql );
		}

		Map<String, Object> regrinfo = ( Map<String, Object> ) whoisMap.get( "regrinfo" );
		if (Utility.isEmpty( regrinfo ))
			return domID;

		//update domain table

		String[] geo = ( String[] ) whoisMap.get( "geoip" ); //get GeoIP
		if (geo == null) {
			geo = new String[3];
			Arrays.fill( geo, "" );
		}

		Map<String, Object> domainData = ( Map<String, Object> ) regrinfo.get( "domain" );
		if (!Utility.isEmpty( domainData )) {
			String name, created, changed, expires, status, sponsor, nserver;

			name = getString( domainData.get( "name" ), false );
			created = getString( domainData.get( "created" ), false );
			changed = getString( domainData.get( "changed" ), false );
			expires = getString( domainData.get( "expires" ), false );
			status = getString( domainData.get( "status" ), false );
			sponsor = getString( domainData.get( "sponsor" ), false );
			nserver = getString( domainData.get( "nserver" ), false );

			sql = "INSERT INTO "
					+ getPre( "domain" )
					+ "(domID,name,created,changed,expires,status,sponsor,nserver,ip,country,countrycode) VALUES(%1$s,'%2$s','%3$s','%4$s','%5$s','%6$s','%7$s','%8$s','%9$s','%10$s','%11$s')";
			sql = String.format( sql, domID, name, created, changed, expires, status, sponsor, nserver, geo[0], geo[1],
					geo[2] );
			updateDB( sql );
		}

		//update owner table
		updateContact( regrinfo, "owner", domID );

		//update admin table
		updateContact( regrinfo, "admin", domID );

		//update tech table 
		updateContact( regrinfo, "tech", domID );

		//update bill table
		updateContact( regrinfo, "bill", domID );

		//update zone table
		updateContact( regrinfo, "zone", domID );

		//update abuse table
		updateContact( regrinfo, "abuse", domID );

		//update network table 
		updateContact( regrinfo, "network", domID );

		return domID;
	}

	public void updateGeoIP(int domID, String[] geo) {
		if (domID > 0) {
			String sql = "UPDATE " + getPre( "domain" )
					+ " SET ip='%1$s',country='%2$s',countrycode='%3$s' WHERE domID=" + domID;
			sql = String.format( sql, escapeQuotes( geo[0] ), escapeQuotes( geo[1] ), escapeQuotes( geo[2] ) );
			updateDB( sql );
		}
	}

	@SuppressWarnings("unchecked")
	private void updateContact(Map<String, Object> regrinfo, String contact, int domID) {
		Map<String, Object> map = null;

		Object info_c = regrinfo.get( contact );
		if (info_c != null) {

			if (info_c instanceof List) {
				map = new HashMap<String, Object>();
				map.put( "info", info_c );
			}
			else if (info_c instanceof Map) {
				map = ( Map<String, Object> ) info_c;
			}
			else {
				return;
			}

			if (Utility.isEmpty( map ))
				return;

			String name, email, fax, phone, address, org, info, created, changed;

			name = getString( map.get( "name" ), false );
			email = getString( map.get( "email" ), true );
			fax = getString( map.get( "fax" ), true );
			phone = getString( map.get( "phone" ), true );
			address = getAddress( map.get( "address" ) );
			org = getString( map.get( "organization" ), false );
			info = getString( map.get( "info" ), false );
			created = getString( map.get( "created" ), false );
			changed = getString( map.get( "changed" ), false );

			String sql = "INSERT INTO "
					+ getPre( contact )
					+ "(domID,name,email,fax,phone,address,organization,info,created,changed) VALUES(%1$s,'%2$s','%3$s','%4$s','%5$s','%6$s','%7$s','%8$s','%9$s','%10$s')";
			sql = String.format( sql, domID, name, email, fax, phone, address, org, info, created, changed );
			updateDB( sql );
		}
	}

	private int updateDB(String sql) {
		int ret = 0;

		if (null == outconn)
			return ret;

		try {
			String domsql = "SELECT LAST_INSERT_ID()";
			Statement st = outconn.createStatement();
			st.execute( sql );
			ResultSet rt = st.executeQuery( domsql );
			if (rt.next())
				ret = rt.getInt( 1 );
			st.close();
		}
		catch (SQLException e) {
			Utility.logWarn( "DBHelper::updateDB: <sql:" + sql + ">", e );
			ret = 0;
		}

		return ret;
	}

	@SuppressWarnings("unchecked")
	private String getString(Object obj, boolean check) {
		String ret = "";

		if (null == obj)
			return ret;

		if (obj instanceof String) {
			ret = obj.toString();
		}
		else if (obj instanceof List) {
			if (check) {
				ret = (( List<String> ) obj).get( 0 );
			}
			else {
				ret = list2string( ( List<String> ) obj );
			}
		}
		return escapeQuotes( ret );
	}

	@SuppressWarnings("unchecked")
	private String getAddress(Object obj) {
		String ret = "";

		if (null == obj)
			return ret;

		if (obj instanceof List) {
			ret = list2string( ( List<String> ) obj );
		}
		else if (obj instanceof Map) {
			Map<String, String> ht = ( Map<String, String> ) obj;
			String tmp = "";
			for (String key : ht.keySet()) {
				Object val = ht.get( key );
				if (val instanceof String) {
					tmp += key + ":" + val + "\r\n";
				}
			}
		}
		else if (obj instanceof String) {
			ret = obj.toString();
		}

		return escapeQuotes( ret );
	}

	private String list2string(List<String> list) {
		if (Utility.isEmpty( list ))
			return "";

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < list.size(); i++) {
			sb.append( list.get( i ) + "\r\n" );
		}

		return escapeQuotes( sb.toString() );
	}

	private List<String> string2list(String str) {
		if (Utility.isEmpty( str ))
			return null;

		List<String> list = new ArrayList<String>();
		String[] array = str.split( "\r\n" );
		for (String s : array) {
			list.add( s );
		}

		return list.isEmpty() ? null : list;
	}

	public static String escapeQuotes(String source) {
		if (Utility.isEmpty( source ))
			return "";

		int i = 0;
		/* Escape \ */
		while (i <= source.length() - 1) {
			if (source.charAt( i ) == '\\') {
				String firstPart = source.substring( 0, i );
				String secondPart;
				try {
					secondPart = source.substring( i + 1 );
				}
				catch (Exception e) {
					secondPart = "";
				}
				source = firstPart + "\\\\" + secondPart;
				i++;
			}
			i++;
		}
		/* Escape ' and " */
		i = 0;
		while (i <= source.length() - 1) {
			if (source.charAt( i ) == '\'') {
				String firstPart = source.substring( 0, i );
				String secondPart;
				try {
					secondPart = source.substring( i + 1 );
				}
				catch (Exception e) {
					secondPart = "";
				}
				source = firstPart + "\\\'" + secondPart;
				i++;
			}
			if (source.charAt( i ) == '\"') {
				String firstPart = source.substring( 0, i );
				String secondPart;
				try {
					secondPart = source.substring( i + 1 );
				}
				catch (Exception e) {
					secondPart = "";
				}
				source = firstPart + "\\\"" + secondPart;
				i++;
			}
			i++;
		}

		return source;
	}

	/**
	 * Gets the whois history dates from database.
	 * 
	 * @param domain
	 *            Domain name
	 * @return Whois history date with domID in database.
	 */
	public Map<String, Integer> getWhoisHistoryDates(String dom) {
		Map<String, Integer> map = new LinkedHashMap<String, Integer>();
		if (!Utility.isValidDom( dom ))
			return map;

		dom = dom.trim().toLowerCase();

		String sql = "SELECT id,querydate FROM " + getPre( "domainname" ) + " WHERE domain='" + escapeQuotes( dom )
				+ "' ORDER BY id desc";

		if (null != outconn) {
			try {
				Statement st = outconn.createStatement();
				ResultSet rt = st.executeQuery( sql );
				int domID = 0;
				String date = null;
				Date queryDate;
				while (rt.next()) {
					domID = rt.getInt( 1 );
					queryDate = rt.getDate( 2 );
					if (domID > 0 && null != queryDate) {
						date = queryDate.toString();
						map.put( date, domID );
					}
				}
				st.close();
			}
			catch (SQLException e) {
				Utility.logWarn( "DBHelper::getWhoisHistoryDates SQLException: <sql:" + sql + ">", e );
			}
		}

		return map;
	}

	/**
	 * Gets whois history from database.
	 * 
	 * @param domID
	 *            The query domain ID.
	 * @return Whois history map.
	 */
	public WhoisMap getWhoisHistory(int domID) {
		WhoisMap whoisMap = new WhoisMap();

		if (null == outconn || domID <= 0)
			return whoisMap;

		String sql = "";
		String[] geo = new String[3];
		Arrays.fill( geo, "N/A" );

		WhoisMap map = new WhoisMap();
		Statement st = null;
		ResultSet rt = null;

		// Get the rawdata
		sql = "SELECT rawdata FROM " + getPre( "domainname" ) + " WHERE id=" + domID;
		try {
			st = outconn.createStatement();
			rt = st.executeQuery( sql );
			if (rt.next()) {
				whoisMap.set( "rawdata", string2list( rt.getString( 1 ) ) );
			}
			st.close();
		}
		catch (SQLException e) {
			Utility.logWarn( "DBHelper::getWhoisHistory SQLException: <sql:" + sql + ">", e );
		}

		if (null == outconn)
			return whoisMap;

		// Get the domain data
		sql = "SELECT name,created,changed,expires,status,sponsor,nserver,ip,country,countrycode FROM "
				+ getPre( "domain" ) + " WHERE domID=" + domID;
		try {
			st = outconn.createStatement();
			rt = st.executeQuery( sql );
			if (rt.next()) {
				map.set( "name", rt.getString( 1 ) );
				map.set( "created", rt.getString( 2 ) );
				map.set( "changed", rt.getString( 3 ) );
				map.set( "expires", rt.getString( 4 ) );
				map.set( "status", rt.getString( 5 ) );
				map.set( "sponsor", rt.getString( 6 ) );
				map.set( "nserver", rt.getString( 7 ) );
				geo[0] = rt.getString( 8 );
				geo[1] = rt.getString( 9 );
				geo[2] = rt.getString( 10 );
			}
			st.close();
		}
		catch (SQLException e) {
			Utility.logWarn( "DBHelper::getWhoisHistory SQLException: <sql:" + sql + ">", e );
		}

		if (null == outconn)
			return whoisMap;

		if (!map.getMap().isEmpty())
			whoisMap.set( "regrinfo.domain", map.getMap() );

		whoisMap.set( "geoip", geo );

		// Get owner contact map
		map = getContactFromDB( outconn, domID, "owner" );
		if (!map.getMap().isEmpty())
			whoisMap.set( "regrinfo.owner", map.getMap() );

		// Get admin contact map
		map = getContactFromDB( outconn, domID, "admin" );
		if (!map.getMap().isEmpty())
			whoisMap.set( "regrinfo.admin", map.getMap() );

		// Get tech contact map
		map = getContactFromDB( outconn, domID, "tech" );
		if (!map.getMap().isEmpty())
			whoisMap.set( "regrinfo.tech", map.getMap() );

		// Get bill contact map
		map = getContactFromDB( outconn, domID, "bill" );
		if (!map.getMap().isEmpty())
			whoisMap.set( "regrinfo.bill", map.getMap() );

		// Get zone contact map
		map = getContactFromDB( outconn, domID, "zone" );
		if (!map.getMap().isEmpty())
			whoisMap.set( "regrinfo.zone", map.getMap() );

		// Get network contact map
		map = getContactFromDB( outconn, domID, "network" );
		if (!map.getMap().isEmpty())
			whoisMap.set( "regrinfo.network", map.getMap() );

		// Get abuse contact map
		map = getContactFromDB( outconn, domID, "abuse" );
		if (!map.isEmpty())
			whoisMap.set( "regrinfo.abuse", map.getMap() );

		return whoisMap;
	}

	private WhoisMap getContactFromDB(Connection conn, int domID, String contact) {
		WhoisMap map = new WhoisMap();

		String sql = "SELECT name,email,phone,fax,organization,address,info,created,changed FROM " + getPre( contact )
				+ " WHERE domID=" + domID;
		try {
			Statement st = conn.createStatement();
			ResultSet rt = st.executeQuery( sql );
			if (rt.next()) {
				map.set( "name", rt.getString( 1 ) );
				map.set( "email", rt.getString( 2 ) );
				map.set( "phone", rt.getString( 3 ) );
				map.set( "fax", rt.getString( 4 ) );
				map.set( "organization", rt.getString( 5 ) );
				map.set( "address", rt.getString( 6 ) );
				map.set( "info", rt.getString( 7 ) );
				map.set( "created", rt.getString( 8 ) );
				map.set( "changed", rt.getString( 9 ) );
			}
			st.close();
		}
		catch (SQLException e) {
			Utility.logWarn( "DBHelper::getContactFromDB SQLException: <sql:" + sql + ">", e );
		}

		return map;
	}

}
