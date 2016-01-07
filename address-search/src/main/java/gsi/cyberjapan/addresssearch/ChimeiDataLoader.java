package gsi.cyberjapan.addresssearch;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.sqlite.SQLiteDataSource;

/**
 * 地名検索用データからSQLiteデータベースを読み込むクラス 
 */
public class ChimeiDataLoader {
	/**
	 * ロガー
	 */
	static private Logger LOGGER = Logger.getLogger(ChimeiDataLoader.class.getName());
	/**
	 * 地名検索用テーブル作成用　DDL
	 */
	static private String CHIMEI_TABLE = 
	"drop table if exists chimei;create table chimei ("+
	"		 geom_id TEXT primary key not null,"+
	"		 datatype TEXT not null,"+
	"		 title TEXT not null,"+
	"		 lgcode TEXT, "+
	"		 x REAL,"+
	"		 y REAL" +
	")";
	/**
	 * 地名検索用索引
	 */
	static private String CHIMEI_INDEX_1 = "create index idx_chimei_title ON chimei(title)";

	/**
	 * 入力ファイルの情報をデータベースに格納
	 * @param inputFile 入力ファイル
	 * @param conn データベース接続
	 * @return 読み込みレコード数
	 * @throws SQLException
	 * @throws IOException
	 */
	static private int convert(File inputFile, Connection conn) throws SQLException,IOException {
		int chimeiId = 1;
		conn.setAutoCommit(false);
		int counter = 0;
		try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(new FileInputStream(inputFile), "UTF-8"))){
			try (PreparedStatement chimeiStatement = conn.prepareStatement("INSERT INTO chimei (geom_id, datatype,title, lgcode, x, y) VALUES (?,?,?,?,?,?)")) {
				String line = null;
				while ((line = reader.readLine()) != null) {
					String type = "";
					String title = "";
					String lgcode = "";
					Double lng = null;
					Double lat = null;
					String[] tokens = line.split(",", -1);
					for (int i = 0; i < tokens.length; i++) {
						String token = tokens[i];
						switch (i) {
						case 0:
							// 空カラム（未使用）
							break;
						case 1:
							// 「地名or公共施設名」のフラグ。地名なら「1」。公共施設名なら「2」
							type = token;
							break;
						case 2:
							// タイトル
							title = token;
							break;
						case 3:
							// 空カラム（未使用）
							break;
						case 4:
							// 市区町村コード（最初の桁が「0」の場合は除かれている）
							lgcode = token;
							if (lgcode.length() < 5) {
								while (lgcode.length() < 5) {
									lgcode = "0" + lgcode;
								}
							}
							break;
						case 5:
							// 経度（10進法）
							lng = Double.parseDouble(token);
							break;
						case 6:
							// 緯度（10進法）
							lat = Double.parseDouble(token);
							break;
						case 7:
							// 空カラム（未使用）
							break;
						case 8:
							// 空カラム（未使用）
							break;
						case 9:
							// 空カラム（未使用）
							break;
						}
					}
					String geomId;
					geomId = String.valueOf(chimeiId++);
					chimeiStatement.setString(1, geomId);
					chimeiStatement.setString(2, type);
					chimeiStatement.setString(3, title);
					if (lgcode.equals("00000")) {
						chimeiStatement.setNull(4, Types.VARCHAR);
					} else {
						chimeiStatement.setString(4, lgcode);
					}
					chimeiStatement.setDouble(5, lng);
					chimeiStatement.setDouble(6, lat);
					chimeiStatement.execute();
					counter++;
					if (counter % 100 == 0) {
						conn.commit();
					}
				}
				conn.commit();
				return counter;
			}
		}
	}
	/**
	 * 地名検索用データを読み込んだSQLiteデータベースのDataSourceを取得
	 * @param path　地名検索用データのファイルパス
	 * @return SQLiteデータベースのDataSource
	 * @throws SQLException データベースの処理で失敗した時の例外
	 * @throws IOException ファイル操作に失敗した時の例外
	 */
	static public DataSource loadChimeiDataSource(String path) throws SQLException,IOException {
		File inputFile = new File(path);
		SQLiteDataSource dataSource = new SQLiteDataSource();
		String dbURL = "jdbc:sqlite::memory:?cache=shared";
		dataSource.setUrl(dbURL);
		try (Connection connection = dataSource.getConnection()) {
			try (Statement statement = connection.createStatement()) {
				statement.setQueryTimeout(30); // set timeout to 30 sec.
				statement.executeUpdate(CHIMEI_TABLE);
				LOGGER.log(Level.INFO,"Table created.");
				int loaded = ChimeiDataLoader.convert(inputFile, connection);
				LOGGER.log(Level.INFO,String.format("%d records loaded.",loaded));
				statement.executeUpdate(CHIMEI_INDEX_1);
				LOGGER.log(Level.INFO,"Index created.");
				connection.commit();
			}
			return dataSource;
		}
	}
}
