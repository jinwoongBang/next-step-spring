package webserver;

import controller.CreateUserController;
import controller.LoginController;
import controller.UserListController;
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

  private final Map<String, Controller> controllerMap;

  public RequestHandler(Socket connectionSocket) {
    this.connection = connectionSocket;
    this.controllerMap = new HashMap<String, Controller>();
    this.controllerMap.put("/user/list.html", new UserListController());
    this.controllerMap.put("/user/login", new LoginController());
    this.controllerMap.put("/user/create", new CreateUserController());
  }

  public void run() {
    log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
        connection.getPort());

    try (InputStream in = connection.getInputStream(); OutputStream out = connection
        .getOutputStream()) {
      HttpRequest httpRequest = new HttpRequest(in);

      HttpResponse httpResponse = new HttpResponse(out);

      String requestURL = httpRequest.getPath();

      Controller controller = controllerMap.get(requestURL);

      if (controller == null && requestURL.endsWith(".html")) {
        httpResponse.response200Header(requestURL);
      } else if (controller == null && requestURL.endsWith(".css")) {
        httpResponse.addHeader("Content-Type", "text/css");
        httpResponse.forward(requestURL);
      } else if (controller != null) {
        controller.service(httpRequest, httpResponse);
      }
    } catch (IOException e) {
      log.error(e.getMessage());
    } catch (Exception er) {
      log.error(er.getMessage());
      er.printStackTrace();
    }
  }
}
