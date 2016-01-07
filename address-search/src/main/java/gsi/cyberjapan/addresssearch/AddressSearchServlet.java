package gsi.cyberjapan.addresssearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * 地名検索
 *
 */
public class AddressSearchServlet extends HttpServlet {
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -6294373173183174105L;
	/**
	 * ロガー
	 */
	static private Logger LOGGER = Logger.getLogger(AddressSearchServlet.class
			.getName());
	/**
	 * 地名検索用データベースDataSource
	 */
	private DataSource dataSource;
	/**
	 * 最大検索結果件数
	 */
	private int maxRecords;

	/**
	 * 東京大学CSIS シンプルジオコーディング実験サービスへ問い合わせを行うPHPのURL
	 */
	private String csisURL;
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.GenericServlet#init()
	 */
	@Override
	public void init() throws ServletException {
		super.init();
		String dataFileName = this.getInitParameter("data");
		if (dataFileName.isEmpty()) {
			throw new ServletException("Fail to read data parameter.");
		}
		dataFileName = this.getServletContext().getRealPath(dataFileName);
		if (!new File(dataFileName).exists()) {
			throw new ServletException("Fail to read "
					+ this.getInitParameter("data") + " is not exists.");
		}
		try {
			this.dataSource = ChimeiDataLoader
					.loadChimeiDataSource(dataFileName);
		} catch (Exception e) {
			throw new ServletException("Fail to load database.", e);
		}
		try {
			this.maxRecords = Integer.parseInt(this
					.getInitParameter("maxRecords"));
		} catch (Exception e) {
			throw new ServletException("Fail to read maxRecords parameter.", e);
		}
		this.csisURL = this.getInitParameter("csisURL");
		if (this.csisURL == null) {
			throw new ServletException("Fail to read csisURL parameter.");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.GenericServlet#destroy()
	 */
	@Override
	public void destroy() {
		super.destroy();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest
	 * , javax.servlet.http.HttpServletResponse)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		JSONArray records = new JSONArray();
		try {
			String searchWord = req.getParameter("q");
			if (searchWord.isEmpty()) {
				LOGGER.log(Level.FINER, "Query parameter is empty.");
			} else {
				// CSISを検索
				JSONArray csisRecords = this.searchFromCSIS(searchWord);
				for (Object csisRecord : csisRecords) {
					if (records.size() >= this.maxRecords) {
						LOGGER.log(Level.WARNING,
								"Total number of results is over the limit="
										+ this.maxRecords + ".");
						break;
					}
					records.add(csisRecord);
				}
				if (records.size() < this.maxRecords) {
					// SQLiteを検索
					this.searchFromSQLite(searchWord, records);
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Fail to search address", e);
		}
		resp.setContentType("application/json; charset=utf-8");
		resp.addHeader("Access-Control-Allow-Origin", "*");
		String resultData = records.toJSONString();
		byte[] buff = resultData.getBytes(Charset.forName("UTF-8"));
		resp.getOutputStream().write(buff);
	}
	/**
	 * SQLiteのデータを検索
	 * @param searchWord
	 * @param records
	 */
	private void searchFromSQLite(String searchWord,JSONArray records) {
		try (Connection connection = this.dataSource.getConnection()) {
			try (PreparedStatement query = connection
					.prepareStatement("select title,lgcode,x,y from chimei where title like ? order by datatype desc,case when lgcode is null then 1 else 0 end,lgcode,title")) {
				query.setString(1, "%" + searchWord + "%");
				try (ResultSet resultSet = query.executeQuery()) {
					while (resultSet.next()) {
						if (records.size() >= this.maxRecords) {
							LOGGER.log(Level.WARNING,
									"Total number of results is over the limit="
											+ this.maxRecords + ".");
							break;
						}
						String title = resultSet.getString("title");
						String lgcode = resultSet.getString("lgcode");
						Double x = resultSet.getDouble("x");
						Double y = resultSet.getDouble("y");
						JSONObject record = new JSONObject();
						record.put("type", "Feature");
						JSONObject geometry = new JSONObject();
						geometry.put("type", "Point");
						JSONArray coordinates = new JSONArray();
						coordinates.add(x);
						coordinates.add(y);
						geometry.put("coordinates", coordinates);
						record.put("geometry", geometry);
						JSONObject properties = new JSONObject();
						properties.put("title", title);
						if (lgcode != null) {
							properties.put("addressCode", lgcode);
						}
						record.put("properties", properties);
						records.add(record);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Fail to search address from SQLite.", e);
		}
	}
	/**
	 * PHPを介して、東京大学CSIS シンプルジオコーディング実験サービスに問い合わせる
	 * @param searchWord
	 * @return
	 */
	private JSONArray searchFromCSIS(String searchWord) {
		HttpURLConnection connection = null;
		try {
			String encodedSearchWord = URLEncoder.encode(searchWord, "UTF-8");
			URL url = new URL(String.format("%s?q=%s",this.csisURL,encodedSearchWord));
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				try (InputStreamReader isr = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
					BufferedReader reader = new BufferedReader(isr)) {
					JSONParser jsonParser = new JSONParser();
					JSONArray result = (JSONArray)jsonParser.parse(reader);
					return result;
				}
			} else {
				LOGGER.log(Level.WARNING, "Fail to search address from CSIS.");
				return new JSONArray();
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Fail to search address from CSIS.",e);
			return new JSONArray();
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
}
