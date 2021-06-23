package controller;

import db.DataBase;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import webserver.AbstractController;
import webserver.HttpRequest;
import webserver.HttpResponse;
import webserver.RequestHandler;

public class CreateUserController extends AbstractController {

  private static final Logger log = LoggerFactory.getLogger(CreateUserController.class);

  @Override
  public void doPost(HttpRequest request, HttpResponse response) {
    User user = createUser(request);
    DataBase.addUser(user);
    log.debug("user : {}", user.toString());
    response.sendRedirect("/index.html");
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
