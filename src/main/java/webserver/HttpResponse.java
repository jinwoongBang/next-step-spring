package webserver;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import model.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpResponse {

  private static final Logger log = LoggerFactory.getLogger(HttpResponse.class);

  private final DataOutputStream dos;
  private final String WEBAPP_ROOT_PATH = "./webapp";
  private final Map<String, String> headers = new HashMap<>();

  private byte[] body = "".getBytes();
  private HttpStatus httpStatus = HttpStatus.OK;

  public HttpResponse(OutputStream outputStream) {
    this.dos = new DataOutputStream(outputStream);
  }

  public void sendResource(String path) {
    findWebappFile(path);
    setHttpStatus(HttpStatus.OK);
    setHeader("Location", path);
    setHeader("Content-Type", "text/html;charset=utf-8");
    responseHeader(this.body.length, this.httpStatus);
    responseBody(this.body);
  }

  public void forward(String path) {
    findWebappFile(path);
    setHttpStatus(HttpStatus.OK);
    responseHeader(this.body.length, this.httpStatus);
    responseBody(this.body);
  }

  public void sendRedirect(String path) {
    findWebappFile(path);
    setHttpStatus(HttpStatus.REDIRECT);
    setHeader("Location", path);
    setHeader("Content-Type", "text/html;charset=utf-8");
    responseHeader(this.body.length, this.httpStatus);
    responseBody(this.body);
  }

  private void responseHeader(int lengthOfBodyContent, HttpStatus httpStatus) {
    String generalHeader = String.format("HTTP/1.1 %d %s \r\n", httpStatus.getCode(), httpStatus.name());
    String entityHeader = String.format("Content-Length: %d \r\n", lengthOfBodyContent);

    try {
      this.dos.writeBytes(generalHeader);
      this.dos.writeBytes(entityHeader);
      for (Map.Entry<String, String> header : this.headers.entrySet()) {
        String key = header.getKey();
        String value = header.getValue();
        String responseHeader = String.format("%s: %s\r\n", key, value);
        this.dos.writeBytes(responseHeader);
      }
      this.dos.writeBytes("\r\n");
    } catch (IOException e) {
      log.error(e.getMessage());
    }
  }

  private void responseBody(byte[] body) {
    try {
      this.dos.write(body, 0, body.length);
      this.dos.flush();
    } catch (IOException e) {
      log.error(e.getMessage());
    }
  }

  private void findWebappFile(String path) {
    File webappFile = new File(WEBAPP_ROOT_PATH + path);
    boolean isExists = webappFile.exists();
    try {
      if (isExists) {
        this.body = Files.readAllBytes(webappFile.toPath());
      } else {
        this.httpStatus = HttpStatus.NOT_FOUND;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public void setHeader(String key, String value) {
    this.headers.put(key, value);
  }

  public void setHttpStatus(HttpStatus httpStatus) {
    this.httpStatus = httpStatus;
  }
}
