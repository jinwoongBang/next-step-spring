package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import model.ContextUser;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {

  private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);
  private static final String REQUEST_METHOD_KEY = "Method";
  private static final String REQUEST_URL_KEY = "URL";
  private static final String REQUEST_PROTOCOL_KEY = "Protocol";

  private static final String HTTP_METHOD_GET = "GET";
  private static final String HTTP_METHOD_POST = "POST";

  private Socket connection;
  private int httpStatus = 200;

  public RequestHandler(Socket connectionSocket) {
    this.connection = connectionSocket;
  }

  public void run() {
    log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
        connection.getPort());
    Map<String, String> requestHeader = new HashMap<>();
    Map<String, String> requestBody = new HashMap<>();
    Map<String, String> queryParamMap = new HashMap<>();

    try (InputStream in = connection.getInputStream(); OutputStream out = connection
        .getOutputStream()) {
      // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
      requestHeader = createRequestHeaderMap(bufferedReader);

      if (requestHeader.isEmpty()) {
        return;
      }

      String method = requestHeader.get(REQUEST_METHOD_KEY);
      String requestURL = requestHeader.get(REQUEST_URL_KEY);
      String protocol = requestHeader.get(REQUEST_PROTOCOL_KEY);
      String contentLength = requestHeader.get("Content-Length");

      switch (method) {
        case HTTP_METHOD_GET:
          boolean hasQueryParams = requestURL.contains("?");
          if (hasQueryParams) {
            String[] splitOfQueryParams = requestURL.split("\\?");
            requestURL = splitOfQueryParams[0];
            String queryParams = splitOfQueryParams[1];
            log.info("Query Params : {}", queryParams);
            queryParamMap = HttpRequestUtils.parseQueryString(queryParams);
          }
          break;
        case HTTP_METHOD_POST:
          if (contentLength != null) {
            String body = IOUtils.readData(bufferedReader, Integer.parseInt(contentLength));
            String[] bodyList = body.split("&");
            for (int i = 0; i < bodyList.length; i++) {
              String keyAndValue = bodyList[i];
              String[] keyAndValueList = keyAndValue.split("=");
              requestBody.put(keyAndValueList[0], keyAndValueList[1]);
            }
          }
      }

      log.info("Method : {}, RequestURL : {}, Protocol : {}, requestBody : {}", method, requestURL,
          protocol, requestBody.toString());

      String redirectURL = requestURL;

      switch (requestURL) {
        case "":
        case "/":
          break;
        case "/user/create": {
          String userId = requestBody.get("userId");
          String password = requestBody.get("password");
          String name = requestBody.get("name");
          String email = requestBody.get("email");
          User user = new User(userId, password, name, email);
          ContextUser.setUser(user);
          log.warn("user : {}, thread user : {}", user.toString(), ContextUser.getUser().toString());
          redirectURL = "/index.html";
          httpStatus = 302;
          break;
        }
        case "/user/login": {
          String userId = requestBody.get("userId");
          String password = requestBody.get("password");
          User user = ContextUser.getUser();
          log.warn("user : {}", user.toString());
          boolean isEqualsUserId = user.getUserId().equals(userId);
          boolean isEqualsPassword = user.getPassword().equals(password);
          if (isEqualsUserId && isEqualsPassword) {
            log.info("Login Success");
            redirectURL = "/index.html";
            httpStatus = 302;
          } else {
            log.info("Login Failure");
            redirectURL = "/user/login_failed.html";
            httpStatus = 302;
          }
        }
        default:
          break;
      }

      File webappFile = new File("./webapp" + redirectURL);
      boolean isExists = webappFile.exists();
      byte[] body = "".getBytes();
      if (isExists) {
        body = Files.readAllBytes(webappFile.toPath());
      } else {
        httpStatus = 404;
      }

      DataOutputStream dos = new DataOutputStream(out);
      switch (httpStatus) {
        case 200:
          response200Header(dos, body.length);
          break;
        case 302:
          response302Header(dos, body.length, redirectURL);
          break;
        case 404:
          body = "404_NOT_FOUND".getBytes();
          response200Header(dos, body.length);
          break;
        default:
          response200Header(dos, body.length);
          break;
      }
      responseBody(dos, body);
    } catch (IOException e) {
      log.error(e.getMessage());
    } catch (Exception er) {
      log.error(er.getMessage());
      er.printStackTrace();
    }
  }

  private Map<String, String> createRequestHeaderMap(BufferedReader bufferedReader)
      throws IOException {
    Map<String, String> headers = new HashMap<>();
    String oneLineHeader = null;
    Pattern pattern = Pattern.compile(":\\s");

    while (!(oneLineHeader = bufferedReader.readLine()).equals("")) {
      log.debug(oneLineHeader);
      Matcher matcher = pattern.matcher(oneLineHeader);
      boolean hasColone = matcher.find();
      headers.putAll(
          hasColone ? createRequestHeaderMap(oneLineHeader) : createRequestURLMap(oneLineHeader));
    }

    return headers;
  }

  private String createRequestBodyMap(BufferedReader bufferedReader)
      throws IOException {
    StringBuffer stringBuffer = new StringBuffer();
    while (bufferedReader.ready()) {
      int ch = bufferedReader.read();
      log.info(String.valueOf((char) ch));
      if ((ch < 0) || (ch == '\n')) {
        break;
      }
      stringBuffer.append((char) ch);
    }
    return stringBuffer.toString();
  }

  private Map<String, String> createRequestHeaderMap(String header) {
    Map<String, String> headerMap = new HashMap<>();
    String[] keyAndValue = header.split(":\\s");
    String key = keyAndValue[0];
    String value = keyAndValue[1];
    headerMap.put(key, value);

    return headerMap;
  }

  private Map<String, String> createRequestURLMap(String header) {
    Map<String, String> headerMap = new HashMap<>();
    if (header.length() == 0) {
      return headerMap;
    }
    String[] keyList = {REQUEST_METHOD_KEY, REQUEST_URL_KEY, REQUEST_PROTOCOL_KEY};
    String[] headerList = header.split("\\s");

    for (int i = 0; i < headerList.length; i++) {
      headerMap.put(keyList[i], headerList[i]);
    }

    return headerMap;
  }

  private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
    try {
      dos.writeBytes("HTTP/1.1 200 OK \r\n");
      dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
      dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
      dos.writeBytes("\r\n");
    } catch (IOException e) {
      log.error(e.getMessage());
    }
  }

  private void response302Header(DataOutputStream dos, int lengthOfBodyContent, String location) {
    try {
      dos.writeBytes("HTTP/1.1 302 OK \r\n");
      dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
      dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
      dos.writeBytes("Location: " + location + "\r\n");
      dos.writeBytes("\r\n");
    } catch (IOException e) {
      log.error(e.getMessage());
    }
  }

  private void responseBody(DataOutputStream dos, byte[] body) {
    try {
      dos.write(body, 0, body.length);
      dos.flush();
    } catch (IOException e) {
      log.error(e.getMessage());
    }
  }
}
