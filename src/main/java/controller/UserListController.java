package controller;

import db.DataBase;
import java.util.Collection;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import webserver.AbstractController;
import webserver.HttpRequest;
import webserver.HttpResponse;
import webserver.RequestHandler;

public class UserListController extends AbstractController {

  private static final Logger log = LoggerFactory.getLogger(UserListController.class);

  private boolean isLogined = false;

  @Override
  public void doGet(HttpRequest request, HttpResponse response) {
    this.isLogined = Boolean.parseBoolean(request.getCookie("logined"));
    log.info("[UserListController] isLogined : {}", isLogined);

    if (!this.isLogined) {
      response.sendRedirect("/user/login.html");
      return;
    }

    Collection<User> users = DataBase.findAll();
    StringBuilder sb = new StringBuilder();
    sb.append("<table border='1'>");
    for (User user : users) {
      sb.append("<tr>");
      sb.append("<td>" + user.getUserId() + "</td>");
      sb.append("<td>" + user.getName() + "</td>");
      sb.append("<td>" + user.getEmail() + "</td>");
      sb.append("</tr>");
    }
    sb.append("</table>");

    response.forwardBody(sb.toString());
  }
}
