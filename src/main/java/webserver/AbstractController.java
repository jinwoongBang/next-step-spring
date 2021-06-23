package webserver;

import model.HttpMethod;

public abstract class AbstractController implements Controller {
  @Override
  public void service(HttpRequest request, HttpResponse response) {
    HttpMethod method = request.getMethod();

    if (HttpMethod.GET.equals(method)) {
      doGet(request, response);
    } else if (HttpMethod.POST.equals(method)) {
      doPost(request, response);
    }
  }

  public abstract void doGet(HttpRequest request, HttpResponse response);
  public abstract void doPost(HttpRequest request, HttpResponse response);
}
