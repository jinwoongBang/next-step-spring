package webserver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

public class HttpRequestTest {

  private String testDirectory = "./src/test/resources";
  private final String HTTP_GET_RESPONSE_FILE_NAME = "Http_Get.txt";

  @Test
  public void request_GET() throws Exception {
    InputStream in = new FileInputStream(new File(testDirectory + HTTP_GET_RESPONSE_FILE_NAME));
    HttpRequest request = new HttpRequest(in);

    assertEquals("GET", request.getMethod());
    assertEquals("/user/create", request.getPath());
    assertEquals("keep-alive", request.getHeader("Connection"));
    assertEquals("javajigi", request.getHeader("userId"));
  }
}
