package webserver;

import db.DataBase;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import model.HttpStatus;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;

public class RequestHandler extends Thread {

  private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

  private Socket connection;

  private boolean isLogined = false;

  public RequestHandler(Socket connectionSocket) {
    this.connection = connectionSocket;
  }

  public void run() {
    log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
        connection.getPort());

    try (InputStream in = connection.getInputStream(); OutputStream out = connection
        .getOutputStream()) {
      HttpRequest httpRequest = new HttpRequest(in);
      this.isLogined = Boolean.parseBoolean(httpRequest.getCookie("logined"));
      log.info("Is Logined : {}", this.isLogined);

      HttpResponse httpResponse = new HttpResponse(out);

      String requestURL = httpRequest.getPath();

      if (requestURL.equals("/user/list.html")) {
        if (!this.isLogined) {
          httpResponse.sendRedirect("/user/login.html" );
        }

        Collection<User> users = DataBase.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("<table border='1'>");
        for(User user : users) {
          sb.append("<tr>");
          sb.append("<td>" + user.getUserId() + "</td>");
          sb.append("<td>" + user.getName() + "</td>");
          sb.append("<td>" + user.getEmail() + "</td>");
          sb.append("</tr>");
        }
        sb.append("</table>");
        httpResponse.forwardBody(sb.toString());
      } else if (requestURL.equals("/user/create")) {
        User user = createUser(httpRequest);
        DataBase.addUser(user);
        log.warn("user : {}", user.toString());
        httpResponse.sendRedirect("/index.html");
      } else if (requestURL.equals("/user/login")) {
        String userId = httpRequest.getRequestBody("userId");
        String password = httpRequest.getRequestBody("password");
        User user = DataBase.findUserById(userId);
        this.isLogined = user != null && user.getPassword().equals(password);
        httpResponse.addHeader("Set-Cookie", String.format("logined=%s", this.isLogined));
        httpResponse.sendRedirect(this.isLogined ? "/index.html" : "/user/login_failed.html");
      } else if (requestURL.endsWith(".html")) {
        httpResponse.sendResource(requestURL);
      } else if (requestURL.endsWith(".css")) {
        httpResponse.addHeader("Content-Type", "text/css");
        httpResponse.forward(requestURL);
      }
    } catch (IOException e) {
      log.error(e.getMessage());
    } catch (Exception er) {
      log.error(er.getMessage());
      er.printStackTrace();
    }
  }

  public User createUser(HttpRequest httpRequest) {
    String userId = httpRequest.getRequestBody("userId");
    String password = httpRequest.getRequestBody("password");
    String name = httpRequest.getRequestBody("name");
    String email = httpRequest.getRequestBody("email");
    User user = new User(userId, password, name, email);

    return user;
  }
}
