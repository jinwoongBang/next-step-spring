package webserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import model.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

public class HttpRequest {

  private static final Logger log = LoggerFactory.getLogger(HttpRequest.class);

  private BufferedReader bufferedReader;

  private Map<String, String> headers = new HashMap<>();
  private Map<String, String> body;
  private Map<String, String> queryParams;

  private HttpMethod method;
  private String url;
  private String protocol;

  public HttpRequest(InputStream inputStream) {
    this.bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

    String oneLineHeader = null;
    int headersLineIndex = 0;

    try {
      while (!(oneLineHeader = this.bufferedReader.readLine()).equals("")) {
        log.debug(oneLineHeader);
        if (headersLineIndex == 0) {
          getGeneralHeader(oneLineHeader);
        } else {
          getHeaders(oneLineHeader);
        }
        headersLineIndex++;
      }
    } catch (IOException e) {
      e.printStackTrace();
      log.error(e.getMessage());
    }

    if (this.method.equals(HttpMethod.GET)) {
      getQueryParams();
    } else if (this.method.equals(HttpMethod.POST)) {
      getBody();
    }

    log.info("Method : {}, RequestURL : {}, Protocol : {}, requestBody : {}", this.method, this.url,
        this.protocol, this.body.toString());
  }

  public String getHeader(String key) {
    return this.headers.get(key);
  }

  public String getParameter(String key) {
    return this.queryParams.get(key);
  }

  public String getURL() {
    return this.url;
  }

  public String getRequestBody(String key) {
    return this.body.get(key);
  }

  private void getBody() {
    int contentLength = Integer.parseInt(getHeader("Content-Length"));
    String body = null;
    try {
      body = IOUtils.readData(bufferedReader, contentLength);
      String[] bodyList = body.split("&");

      for (int i = 0; i < bodyList.length; i++) {
        String keyAndValue = bodyList[i];
        String[] keyAndValueList = keyAndValue.split("=");
        this.body.put(keyAndValueList[0], keyAndValueList[1]);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void getQueryParams() {
    boolean hasQueryParams = this.url.contains("?");
    if (hasQueryParams) {
      String[] splitOfQueryParams = this.url.split("\\?");
      this.url = splitOfQueryParams[0];
      String queryParams = splitOfQueryParams[1];
      log.info("Query Params : {}", queryParams);
      this.queryParams = HttpRequestUtils.parseQueryString(queryParams);
    }
  }

  private void getHeaders(String header) {
    String[] keyAndValue = header.split(":\\s");
    String key = keyAndValue[0];
    String value = keyAndValue[1];
    this.headers.put(key, value);
  }

  private void getGeneralHeader(String generalHeader) {
    String[] headerList = generalHeader.split("\\s");
    String method = headerList[0];

    if (HttpMethod.GET.name().equals(method)) {
      this.method = HttpMethod.GET;
    } else if (HttpMethod.POST.name().equals(method)) {
      this.method = HttpMethod.POST;
    }

    this.url = headerList[1];
    this.protocol = headerList[2];
  }


}