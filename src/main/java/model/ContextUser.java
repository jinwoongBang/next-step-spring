package model;

public class ContextUser {
  private static User contextUser = new User("", "", "", "");

  public static synchronized void setUser(User user) {
    contextUser = user;
  }

  public static synchronized User getUser() {
    return contextUser;
  }
}
