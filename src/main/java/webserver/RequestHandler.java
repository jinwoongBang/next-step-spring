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

  private Socket connection;
  private int httpStatusCode = 200;
  private String httpStatusMessage = "OK";


  public RequestHandler(Socket connectionSocket) {
    this.connection = connectionSocket;
  }

  public void run() {
    log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
        connection.getPort());

    Map<String, String> responseHeader = new HashMap<>();

    try (InputStream in = connection.getInputStream(); OutputStream out = connection
        .getOutputStream()) {
      // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
      HttpRequest httpRequest = new HttpRequest(in);

      String requestURL = httpRequest.getURL();
      String redirectURL = requestURL;

      switch (requestURL) {
        case "":
        case "/":
        case "/index.html": {
          redirectURL = "/index.html";
          responseHeader.put("Location", redirectURL);
          responseHeader.put("Content-Type", "text/html;charset=utf-8");
          break;
        }
        case "/user/create": {
          String userId = httpRequest.getRequestBody("userId");
          String password = httpRequest.getRequestBody("password");
          String name = httpRequest.getRequestBody("name");
          String email = httpRequest.getRequestBody("email");
          User user = new User(userId, password, name, email);
          ContextUser.setUser(user);
          log.warn("user : {}, thread user : {}", user.toString(),
              ContextUser.getUser().toString());
          redirectURL = "/index.html";
          httpStatusCode = 302;
          httpStatusMessage = "Found";
          responseHeader.put("Location", redirectURL);
          break;
        }
        case "/user/login": {
          String userId = httpRequest.getRequestBody("userId");
          String password = httpRequest.getRequestBody("password");
          User user = ContextUser.getUser();
          log.warn("user : {}", user.toString());
          boolean isEqualsUserId = user.getUserId().equals(userId);
          boolean isEqualsPassword = user.getPassword().equals(password);
          if (isEqualsUserId && isEqualsPassword) {
            log.info("Login Success");
            redirectURL = "/index.html";
            responseHeader.put("Location", redirectURL);
            responseHeader.put("Set-Cookie", "logined=true");
          } else {
            log.info("Login Failure");
            redirectURL = "/user/login_failed.html";
            responseHeader.put("Location", redirectURL);
            responseHeader.put("Set-Cookie", "logined=false");
          }
          httpStatusCode = 302;
          httpStatusMessage = "Redirect";
          responseHeader.put("Content-Type", "text/html;charset=utf-8");
          break;
        }
        case "/user/list": {
          String cookies = httpRequest.getRequestBody("Cookie");
          boolean isLogined = false;
          if (cookies != null) {
            Map<String, String> cookie = HttpRequestUtils.parseCookies(cookies);
            String loginedCookie = cookie.get("logined");
            isLogined = Boolean.parseBoolean(loginedCookie);
          }

          if (isLogined) {
            redirectURL = "/user/list.html";
          } else {
            redirectURL = "/user/login.html";
          }
          responseHeader.put("Content-Type", "text/html;charset=utf-8");
          break;
        }
        default:
          Pattern cssPattern = Pattern.compile(".css");
          Matcher cssMatcher = cssPattern.matcher(requestURL);
          boolean isCss = cssMatcher.find();
          if (isCss) {
            responseHeader.put("Content-Type", "text/css");
          }
          break;
      }

      File webappFile = new File("./webapp" + redirectURL);
      boolean isExists = webappFile.exists();
      byte[] body = "".getBytes();
      if (isExists) {
        body = Files.readAllBytes(webappFile.toPath());
      } else {
        httpStatusCode = 404;
      }

      DataOutputStream dos = new DataOutputStream(out);
      responseHeader(dos, body.length, httpStatusCode, responseHeader);
      responseBody(dos, body);
    } catch (IOException e) {
      log.error(e.getMessage());
    } catch (Exception er) {
      log.error(er.getMessage());
      er.printStackTrace();
    }
  }


  private void responseHeader(DataOutputStream dos, int lengthOfBodyContent, int httpStatusCode,
      Map<String, String> headers) {
    responseHeader(dos, lengthOfBodyContent, httpStatusCode, null, headers);
  }

  private void responseHeader(DataOutputStream dos, int lengthOfBodyContent, int httpStatusCode,
      String httpStatusMessage, Map<String, String> headers) {

    String generalHeader = String.format("HTTP/1.1 %d %s \r\n", httpStatusCode, httpStatusMessage);
    String entityHeader = String.format("Content-Length: %d \r\n", lengthOfBodyContent);

    try {
      dos.writeBytes(generalHeader);
      dos.writeBytes(entityHeader);
      for (Map.Entry<String, String> header : headers.entrySet()) {
        String key = header.getKey();
        String value = header.getValue();
        String responseHeader = String.format("%s: %s\r\n", key, value);
        dos.writeBytes(responseHeader);
      }
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
