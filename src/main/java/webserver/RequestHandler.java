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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());
        Map<String, Object> requestHeader = new HashMap<>();
        Map<String, String> paramMap = new HashMap<>();;
        User user;

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            String temp;
            Pattern pattern = Pattern.compile(":\\s");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));

            while((temp = bufferedReader.readLine()) != null) {
                log.info(temp);
                if (temp == null || temp.equals("")) {
                    break;
                }

                Matcher matcher = pattern.matcher(temp);
                boolean hasColone = matcher.find();
                String[] clientRequest;

                if (hasColone) {
                    clientRequest = temp.split(":\\s");
                    String key = clientRequest[0];
                    key = key.replace("-", "");
                    String value = clientRequest[1];
                    requestHeader.put(key, value);
                } else {
                    clientRequest = temp.split("\\s");
                    String method = clientRequest[0];
                    String requestURL = clientRequest[1];
                    String protocol = clientRequest[2];
                    requestHeader.put("Method", method);
                    requestHeader.put("RequestURL", requestURL);
                    requestHeader.put("Protocol", protocol);
                }
            }
            log.info("Method : {}, RequestURL : {}, Protocol : {}", requestHeader.get("Method"), requestHeader.get("RequestURL"), requestHeader.get("Protocol"));

            Object path = requestHeader.get("RequestURL");
            String requestPath = path != null ? (String) path : "";

            boolean hasQueryParams = requestPath.contains("?");
            String queryParams;
            if (hasQueryParams) {
                String[] splitOfQueryParams = requestPath.split("\\?");
                requestPath = splitOfQueryParams[0];
                queryParams = splitOfQueryParams[1];
                log.info("Query Params : {}", queryParams);
                paramMap = HttpRequestUtils.parseQueryString(queryParams);
            }

            switch(requestPath) {
                case "/user/create":
                    String userId = paramMap.get("userId");
                    String password = paramMap.get("password");
                    String name = paramMap.get("name");
                    String email = paramMap.get("email");
                    user = new User(userId, password, name, email);
                    log.info("user : {}", user.toString());
                    break;
                default:
            }

            File webappFile = new File("./webapp" + requestPath);
            boolean isExists = webappFile.exists();
            byte[] body = "404_NOT_FOUNT".getBytes();
            if (isExists) {
                body = Files.readAllBytes(webappFile.toPath());
            }

            DataOutputStream dos = new DataOutputStream(out);
            response200Header(dos, body.length);
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
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

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
