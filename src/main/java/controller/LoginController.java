package controller;

import db.DataBase;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import webserver.AbstractController;
import webserver.HttpRequest;
import webserver.HttpResponse;

public class LoginController extends AbstractController {

  private static final Logger log = LoggerFactory.getLogger(LoginController.class);

  private boolean isLogined = false;

  @Override
  public void doPost(HttpRequest request, HttpResponse response) {
    String userId = request.getRequestBody("userId");
    String password = request.getRequestBody("password");
    User user = DataBase.findUserById(userId);
    this.isLogined = user != null && user.getPassword().equals(password);
    response.addHeader("Set-Cookie", String.format("logined=%s", this.isLogined));
    response.sendRedirect(this.isLogined ? "/index.html" : "/user/login_failed.html");
  }
}
