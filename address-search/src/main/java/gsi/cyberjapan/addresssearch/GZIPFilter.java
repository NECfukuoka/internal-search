package gsi.cyberjapan.addresssearch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.zip.GZIPOutputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * GZIP圧縮して結果を返すFilter
 */
public class GZIPFilter implements Filter {
	/* (non-Javadoc)
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
	 */
	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}

	/* (non-Javadoc)
	 * @see javax.servlet.Filter#destroy()
	 */
	@Override
	public void destroy() {
	}

	/* (non-Javadoc)
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		if (req instanceof HttpServletRequest) {
			HttpServletRequest request = (HttpServletRequest) req;
			HttpServletResponse response = (HttpServletResponse) res;
			String ae = request.getHeader("accept-encoding");
			if (ae != null && ae.indexOf("gzip") != -1) {
				GZIPResponseWrapper wrappedResponse = new GZIPResponseWrapper(response);
				chain.doFilter(req, wrappedResponse);
				wrappedResponse.finishResponse();
				return;
			}
			chain.doFilter(req, res);
		}
	}

	/**
	 * 結果をGZIPをするServletOutputStreamの派生クラス
	 *
	 */
	private class GZIPResponseStream extends ServletOutputStream {
		private ByteArrayOutputStream baos = null;
		private GZIPOutputStream gzipstream = null;
		private boolean closed = false;
		private HttpServletResponse response = null;
		private ServletOutputStream output = null;

		/**
		 * コンストラクタ
		 * @param paramHttpServletResponse
		 * @throws IOException
		 */
		private GZIPResponseStream(HttpServletResponse paramHttpServletResponse) throws IOException {
			this.response = paramHttpServletResponse;
			this.output = paramHttpServletResponse.getOutputStream();
			this.baos = new ByteArrayOutputStream();
			this.gzipstream = new GZIPOutputStream(this.baos);
		}

		/* (non-Javadoc)
		 * @see java.io.OutputStream#close()
		 */
		public void close() throws IOException {
			if (this.closed) {
				throw new IOException("This output stream has already been closed");
			}
			this.gzipstream.finish();
			byte[] arrayOfByte = this.baos.toByteArray();
			this.response.addHeader("Content-Length",Integer.toString(arrayOfByte.length));
			this.response.addHeader("Content-Encoding", "gzip");
			this.output.write(arrayOfByte);
			this.output.flush();
			this.output.close();
			this.closed = true;
		}

		/* (non-Javadoc)
		 * @see java.io.OutputStream#flush()
		 */
		public void flush() throws IOException {
			if (this.closed) {
				throw new IOException("Cannot flush a closed output stream");
			}
			this.gzipstream.flush();
		}

		/* (non-Javadoc)
		 * @see java.io.OutputStream#write(int)
		 */
		public void write(int paramInt) throws IOException {
			if (this.closed) {
				throw new IOException("Cannot write to a closed output stream");
			}
			this.gzipstream.write((byte) paramInt);
		}

		/* (non-Javadoc)
		 * @see java.io.OutputStream#write(byte[])
		 */
		public void write(byte[] paramArrayOfByte) throws IOException {
			write(paramArrayOfByte, 0, paramArrayOfByte.length);
		}

		/* (non-Javadoc)
		 * @see java.io.OutputStream#write(byte[], int, int)
		 */
		public void write(byte[] paramArrayOfByte, int paramInt1, int paramInt2) throws IOException {
			if (this.closed) {
				throw new IOException("Cannot write to a closed output stream");
			}
			this.gzipstream.write(paramArrayOfByte, paramInt1, paramInt2);
		}
	}

	/**
	 * GZIP圧縮の為にHttpServletResponseをWrapするクラス
	 *
	 */
	private class GZIPResponseWrapper extends HttpServletResponseWrapper {
		private HttpServletResponse origResponse = null;
		private ServletOutputStream stream = null;
		private PrintWriter writer = null;

		/**
		 * コンストラクタ
		 * @param paramHttpServletResponse
		 */
		public GZIPResponseWrapper(HttpServletResponse paramHttpServletResponse) {
			super(paramHttpServletResponse);
			this.origResponse = paramHttpServletResponse;
		}

		/**
		 * GZIPResponseStreamを生成
		 * @return
		 * @throws IOException
		 */
		private ServletOutputStream createOutputStream() throws IOException {
			return new GZIPResponseStream(this.origResponse);
		}

		/**
		 * 結果の圧縮処理を終了
		 */
		private void finishResponse() {
			try {
				if (this.writer != null) {
					this.writer.close();
				} else if (this.stream != null) {
					this.stream.close();
				}
			} catch (IOException localIOException) {
			}
		}

		/* (non-Javadoc)
		 * @see javax.servlet.ServletResponseWrapper#flushBuffer()
		 */
		public void flushBuffer() throws IOException {
			this.stream.flush();
		}

		/* (non-Javadoc)
		 * @see javax.servlet.ServletResponseWrapper#getOutputStream()
		 */
		public ServletOutputStream getOutputStream() throws IOException {
			if (this.writer != null) {
				throw new IllegalStateException("getWriter() has already been called!");
			}
			if (this.stream == null) {
				this.stream = createOutputStream();
			}
			return this.stream;
		}

		/* (non-Javadoc)
		 * @see javax.servlet.ServletResponseWrapper#getWriter()
		 */
		public PrintWriter getWriter() throws IOException {
			if (this.writer != null) {
				return this.writer;
			}
			if (this.stream != null) {
				throw new IllegalStateException("getOutputStream() has already been called!");
			}
			this.stream = createOutputStream();
			this.writer = new PrintWriter(new OutputStreamWriter(this.stream, "UTF-8"));
			return this.writer;
		}

	}
}
