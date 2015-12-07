package gsi.cyberjapan.addresssearch;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
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
	static private Logger LOGGER = Logger.getLogger(AddressSearchServlet.class.getName());
	/**
	 * 地名検索用データベースDataSource
	 */
	private DataSource dataSource;
	/**
	 * 最大検索結果件数
	 */
	private int maxRecords;
	
	/* (non-Javadoc)
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
			throw new ServletException("Fail to read " + this.getInitParameter("data") + " is not exists.");
		}
		try {
			this.dataSource = ChimeiDataLoader.loadChimeiDataSource(dataFileName);
		} catch (Exception e) {
			throw new ServletException("Fail to load database.",e);
		}
		try {
			this.maxRecords = Integer.parseInt(this.getInitParameter("maxRecords"));
		} catch (Exception e) {
			throw new ServletException("Fail to read maxRecords parameter.",e);
		}
	}

	/* (non-Javadoc)
	 * @see javax.servlet.GenericServlet#destroy()
	 */
	@Override
	public void destroy() {
		super.destroy();
	}

	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		JSONArray records = new JSONArray();
		try {
			String searchWord = req.getParameter("q");
			if (searchWord.isEmpty()) {
				LOGGER.log(Level.FINER, "Query parameter is empty.");
			} else {
				try (Connection connection = this.dataSource.getConnection()) {
					try (PreparedStatement query = connection.prepareStatement("select title,lgcode,x,y from chimei where title like ? order by datatype")) {
						query.setString(1, "%"+searchWord+"%");
						try (ResultSet resultSet = query.executeQuery()) {
							while (resultSet.next()) {
								if (records.size() >= this.maxRecords) {
									LOGGER.log(Level.WARNING, "Total number of results is over the limit=" + this.maxRecords+".");
									break;
								}
								String title = resultSet.getString("title");
								String lgcode = resultSet.getString("lgcode");
								Double x = resultSet.getDouble("x");
								Double y = resultSet.getDouble("y");
								JSONObject record = new JSONObject();
								record.put("type","Feature");
								JSONObject geometry = new JSONObject();
								geometry.put("type", "Point");
								JSONArray coordinates = new JSONArray();
								coordinates.add(x);
								coordinates.add(y);
								geometry.put("coordinates", coordinates);
								record.put("geometry", geometry);
								JSONObject properties = new JSONObject();
								properties.put("title",title);
								properties.put("addressCode",lgcode);
								record.put("properties",properties);
								records.add(record);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Fail to search address",e);
		}
		resp.setContentType("application/json; charset=utf-8");
		resp.addHeader("Access-Control-Allow-Origin", "*");
		String resultData = records.toJSONString();
		byte[] buff = resultData.getBytes(Charset.forName("UTF-8"));
		resp.getOutputStream().write(buff);
	}
}
