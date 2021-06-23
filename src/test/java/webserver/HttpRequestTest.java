package webserver;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class HttpRequestTest {

  private String testDirectory = "./src/test/resources/";
  private final String HTTP_GET_RESPONSE_FILE_NAME = "Http_GET.txt";
  private final String HTTP_POST_RESPONSE_FILE_NAME = "Http_POST.txt";

  @DisplayName("Http Request Method GET")
  @Test
  public void request_GET() throws Exception {
    InputStream in = new FileInputStream(new File(testDirectory + HTTP_GET_RESPONSE_FILE_NAME));
    HttpRequest request = new HttpRequest(in);

    assertEquals("GET", request.getMethod().name());
    assertEquals("/user/create", request.getPath());
    assertEquals("keep-alive", request.getHeader("Connection"));
    assertEquals("javajigi", request.getParameter("userId"));
  }

  @DisplayName("Http Request Method POST")
  @Test
  public void request_POST() throws Exception {
    InputStream in = new FileInputStream(new File(testDirectory + HTTP_POST_RESPONSE_FILE_NAME));
    HttpRequest request = new HttpRequest(in);

    assertEquals("POST", request.getMethod().name());
    assertEquals("/user/create", request.getPath());
    assertEquals("keep-alive", request.getHeader("Connection"));
    assertEquals("javajigi", request.getRequestBody("userId"));
  }
}
