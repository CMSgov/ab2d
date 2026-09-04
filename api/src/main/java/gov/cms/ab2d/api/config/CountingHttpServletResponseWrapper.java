package gov.cms.ab2d.api.config;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Counts the bytes the application writes to the response body so {@code ab2d.api.response.size} can
 * be reported for every response, including the file downloads that stream without a Content-Length.
 *
 * <p>Unlike Spring's {@code ContentCachingResponseWrapper} nothing is buffered: writes are passed
 * straight through to the container and only tallied, which matters because job files are streamed
 * and can be gigabytes. The tally is the number of bytes the application produced; Tomcat's response
 * compression happens below this wrapper, so the count is the uncompressed size.
 */
class CountingHttpServletResponseWrapper extends HttpServletResponseWrapper {

    private CountingServletOutputStream outputStream;
    private PrintWriter writer;
    private long bytesWritten;

    CountingHttpServletResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (outputStream == null) {
            outputStream = new CountingServletOutputStream(super.getOutputStream());
        }
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), charset()), false);
        }
        return writer;
    }

    @Override
    public void flushBuffer() throws IOException {
        flushWriter();
        super.flushBuffer();
    }

    @Override
    public void reset() {
        super.reset();
        bytesWritten = 0;
    }

    @Override
    public void resetBuffer() {
        super.resetBuffer();
        bytesWritten = 0;
    }

    /**
     * Number of body bytes written so far. Flushes a writer obtained through {@link #getWriter()}
     * first so characters still sitting in its buffer are counted.
     */
    long getBytesWritten() {
        flushWriter();
        return bytesWritten;
    }

    private void flushWriter() {
        if (writer != null) {
            // PrintWriter swallows IO errors rather than throwing, which is what we want here: a
            // failure to flush must never turn into a failed request just because we are counting.
            writer.flush();
        }
    }

    private Charset charset() {
        String encoding = getCharacterEncoding();
        if (encoding == null) {
            return StandardCharsets.ISO_8859_1;
        }
        try {
            return Charset.forName(encoding);
        } catch (IllegalArgumentException e) {
            return StandardCharsets.ISO_8859_1;
        }
    }

    private final class CountingServletOutputStream extends ServletOutputStream {

        private final ServletOutputStream delegate;

        private CountingServletOutputStream(ServletOutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(int b) throws IOException {
            delegate.write(b);
            bytesWritten++;
        }

        @Override
        public void write(byte[] b) throws IOException {
            delegate.write(b);
            bytesWritten += b.length;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
            bytesWritten += len;
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            delegate.setWriteListener(writeListener);
        }
    }
}
