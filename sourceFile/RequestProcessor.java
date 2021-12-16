import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.*;

public class RequestProcessor implements Runnable {

  private final static Logger logger = Logger.getLogger(
          RequestProcessor.class.getCanonicalName());

  private File rootDirectory;
  private String indexFileName = "index.html";
  private Socket connection;

  private Map<String, String> users = new HashMap<>();

  public RequestProcessor(File rootDirectory,
                          String indexFileName, Socket connection) {

    if (rootDirectory.isFile()) {
      throw new IllegalArgumentException(
              "rootDirectory must be a directory, not a file");
    }
    try {
      rootDirectory = rootDirectory.getCanonicalFile();
    } catch (IOException ex) {
    }
    this.rootDirectory = rootDirectory;

    if (indexFileName != null) this.indexFileName = indexFileName;
    this.connection = connection;

    users.put("admin", "123456");
    users.put("example", "123456");
  }

  @Override
  public void run() {
    // for security checks
    String root = rootDirectory.getPath();
    try {
      OutputStream raw = new BufferedOutputStream(
              connection.getOutputStream()
      );
      Writer out = new OutputStreamWriter(raw);
      Reader in = new InputStreamReader(
              new BufferedInputStream(
                      connection.getInputStream()
              ),"US-ASCII"
      );
      StringBuilder requestLine = new StringBuilder();
      while (true) {
        int c = in.read();
        if (c == '\r' || c == '\n') break;
        requestLine.append((char) c);
      }

      String get = requestLine.toString();

      logger.info(connection.getRemoteSocketAddress() + " " + get);

      String[] tokens = get.split("\\s+");
      String method = tokens[0];
      String version = "";
      if (method.equals("GET")) {
        // Handle GET Request

        String fileName = tokens[1];
        if (fileName.endsWith("/")) fileName += indexFileName;
        String contentType =
                URLConnection.getFileNameMap().getContentTypeFor(fileName);
        if (tokens.length > 2) {
          version = tokens[2];
        }

        File theFile = new File(rootDirectory,
                fileName.substring(1, fileName.length()));

        if (theFile.canRead()
                // Don't let clients outside the document root
                && theFile.getCanonicalPath().startsWith(root)) {
          byte[] theData = Files.readAllBytes(theFile.toPath());
          if (version.startsWith("HTTP/")) { // send a MIME header
            sendHeader(out, "HTTP/1.0 200 OK", contentType, theData.length);
          }

          // send the file; it may be an image or other binary data
          // so use the underlying output stream
          // instead of the writer
          raw.write(theData);
          raw.flush();
        } else { // can't find the file
          String body = new StringBuilder("<HTML>\r\n")
                  .append("<HEAD><TITLE>File Not Found</TITLE>\r\n")
                  .append("</HEAD>\r\n")
                  .append("<BODY>")
                  .append("<H1>HTTP Error 404: File Not Found</H1>\r\n")
                  .append("</BODY></HTML>\r\n").toString();
          if (version.startsWith("HTTP/")) { // send a MIME header
            sendHeader(out, "HTTP/1.0 404 File Not Found",
                    "text/html; charset=utf-8", body.length());
          }
          out.write(body);
          out.flush();
        }
      }
      else if (method.equals("HEAD")){
        // Handle HEAD request

        String fileName = tokens[1];
        if (fileName.endsWith("/")) fileName += indexFileName;
        String contentType =
                URLConnection.getFileNameMap().getContentTypeFor(fileName);
        if (tokens.length > 2) {
          version = tokens[2];
        }

        File theFile = new File(rootDirectory,
                fileName.substring(1, fileName.length()));

        if (theFile.canRead()
                // Don't let clients outside the document root
                && theFile.getCanonicalPath().startsWith(root)) {
          if (version.startsWith("HTTP/")) { // send a MIME header
            sendHeader(out, "HTTP/1.0 200 OK", contentType, 0);
          }

        } else { // can't find the file
          if (version.startsWith("HTTP/")) { // send a MIME header
            sendHeader(out, "HTTP/1.0 404 File Not Found",
                    "text/html; charset=utf-8", 0);
          }
        }
      }

      else if (method.equals("POST")){ // If the server receives a POST request

        String fileName = tokens[1]; // Get the file name
        if (fileName.endsWith("/")) fileName += indexFileName;
        if (tokens.length > 2) {
          version = tokens[2];
        }

        File theFile = new File(rootDirectory,
                fileName.substring(1, fileName.length()));

        StringBuilder postHeader = new StringBuilder(); // get all request headers

        int count_rn = 0; // check if there are \r\n\r\n pattern

        while (true) {
          int c = in.read();
          if (c == '\r' || c == '\n') {
            count_rn++;
          } else {
            count_rn = 0;
          }

          postHeader.append((char) c);
          if (count_rn >= 4) break;
        }

        // Create a HashMap for all the headers
        Map<String, String> headerMap = new HashMap<>();
        for (String headerLine : postHeader.toString().split("\r\n")){
          headerMap.put(headerLine.split(": ")[0], headerLine.split(": ")[1]);
        }

        // Get the length I need to read from message body
        int contentLength = Integer.parseInt(headerMap.get("Content-Length"));

        StringBuilder entity = new StringBuilder(); // get all message body

        // Read from message body
        for (int i = 0; i < contentLength; i++){
          int c = in.read();
          entity.append((char) c);
        }

        // Create the entity map
        Map<String, String> entityMap = new HashMap<>();
        for (String item : entity.toString().split("&")){
          entityMap.put(item.split("=")[0], item.split("=")[1]);
        }


        if (fileName.equals("/form.html")){ // To deal with POST request from form.html
          if (theFile.canRead()
                  // Don't let clients outside the document root
                  && theFile.getCanonicalPath().startsWith(root)) {


            // Construct the message body of a post request
            String body = new StringBuilder("<HTML>\r\n")
                    .append("<HEAD><TITLE>Handling Post Request</TITLE>\r\n")
                    .append("</HEAD>\r\n")
                    .append("<BODY>")
                    .append("<H1>Hello! " + entityMap.get("name") + " from " + entityMap.get("city") + "</H1>\r\n")
                    .append("<H2>POST Request Received at Server Side (Detailed)</H2>\r\n")
                    .append("<H3>POST Request-Line</H3>\r\n")
                    .append("<P>" + requestLine.toString() + "</P>\r\n")
                    .append("<H3>POST Request Headers</H3>\r\n")
                    .append("<P>" + postHeader.toString().replace("\r\n", "<br>") + "</P>\r\n")
                    .append("<H3>POST Request Message Body</H3>\r\n")
                    .append("<P>" + entity.toString() +  "</P>\r\n")
                    .append("</BODY></HTML>\r\n").toString();

            // Send out
            if (version.startsWith("HTTP/")) { // send a MIME header
              sendHeader(out, "HTTP/1.0 200 OK", "text/html; charset=utf-8", body.length());
            }

            out.write(body);
            out.flush();
          }

        }

        else if (fileName.equals("/index_L.html")){


          Boolean isAdmin = entityMap.get("username").equals("admin");

          String allUsersTrTdInTableString;
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append("<table>\n" +
                  "            <div class=\"table-title\">All Users</div>\n" +
                  "            <tr>\n" +
                  "                <th>Username</th>\n" +
                  "                <th>Password</th>\n" +
                  "            </tr>");
          users.forEach((username, password) -> stringBuilder.append("<tr>\n" +
                  "                <td>" + username + "</td>\n" +
                  "                <td>" + password + "</td>\n" +
                  "            </tr>"));
          stringBuilder.append("</table>");
          allUsersTrTdInTableString = stringBuilder.toString();

          String body = new StringBuilder("<!DOCTYPE html>")
                  .append("<!DOCTYPE html>\n" +
                          "<html lang=\"en\">\n" +
                          "<head>\n" +
                          "    <meta charset=\"UTF-8\">\n" +
                          "    <title>Title</title>\n" +
                          "    <style>\n" +
                          "        *,\n" +
                          "        *::before,\n" +
                          "        *::after {\n" +
                          "            box-sizing: border-box;\n" +
                          "            margin: 0;\n" +
                          "            padding: 0;\n" +
                          "        }\n" +
                          "\n" +
                          "        * {\n" +
                          "            font-family: Arial, sans-serif;\n" +
                          "        }\n" +
                          "\n" +
                          "        .main {\n" +
                          "            width: 100vw;\n" +
                          "            height: 100vh;\n" +
                          "            display: flex;\n" +
                          "            flex-direction: column;\n" +
                          "            align-items: center;\n" +
                          "            justify-content: center;\n" +
                          "        }\n" +
                          "\n" +
                          "        .headline h1 {\n" +
                          "            text-align: center;\n" +
                          "            font-size: 80px;\n" +
                          "            background-image: linear-gradient(110deg, #f36570, #c61be0, #7703f1);\n" +
                          "            -webkit-background-clip: text;\n" +
                          "            -webkit-text-fill-color: transparent;\n" +
                          "        }\n" +
                          "\n" +
                          "        .content {\n" +
                          "            margin-top: 80px;\n" +
                          "        }\n" +
                          "\n" +
                          "        .content table {\n" +
                          "            min-width: 480px;\n" +
                          "            font-size: 18px;\n" +
                          "            border-collapse: collapse;\n" +
                          "        }\n" +
                          "\n" +
                          "        .content .table-title {\n" +
                          "            margin-bottom: 10px;\n" +
                          "            font-size: 24px;\n" +
                          "        }\n" +
                          "\n" +
                          "        .content table td,th {\n" +
                          "            border: 1px solid #dddddd;\n" +
                          "            text-align: left;\n" +
                          "            padding: 10px;\n" +
                          "        }\n" +
                          "\n" +
                          "        .content .content-paragraph {\n" +
                          "            font-size: 20px;\n" +
                          "        }\n" +
                          "\n" +
                          "\n" +
                          "    </style>\n" +
                          "</head>\n" +
                          "<body>\n" +
                          "<div class=\"main\">\n" +
                          "    <div class=\"headline\">\n" +
                          "        <h1>Welcome, " + entityMap.get("username") + "</h1>\n" +
                          "    </div>\n" +
                          "    <div class=\"content\">")
                  .append(isAdmin ? allUsersTrTdInTableString : "<div class=\"content-paragraph\">You have logged in as a normal user.</div>")
                  .append("</div>\n" +
                          "</div>\n" +
                          "</body>\n" +
                          "</html>").toString();

          if (version.startsWith("HTTP/")) { // send a MIME header
            sendHeader(out, "HTTP/1.0 200 OK", "text/html; charset=utf-8", body.length());
          }
          out.write(body);
          out.flush();

        }


        else { // If server doesn't support POST request from this file

          // I still wan to show the POST request I received at the server side
          String body = new StringBuilder("<HTML>\r\n")
                  .append("<HEAD><TITLE>POST Request Not Supported</TITLE>\r\n")
                  .append("</HEAD>\r\n")
                  .append("<BODY>")
                  .append("<H1>Post Request not Supported for " + fileName + " </H1>\r\n")
                  .append("<H2>POST Request Received at Server Side (Detailed)</H2>\r\n")
                  .append("<H3>POST Request-Line</H3>\r\n")
                  .append("<P>" + requestLine.toString() + "</P>\r\n")
                  .append("<H3>POST Request Headers</H3>\r\n")
                  .append("<P>" + postHeader.toString().replace("\r\n", "<br>") + "</P>\r\n")
                  .append("<H3>POST Request Message Body</H3>\r\n")
                  .append("<P>" + entity.toString() +  "</P>\r\n")
                  .append("</BODY></HTML>\r\n").toString();
          if (version.startsWith("HTTP/")) { // send a MIME header
            sendHeader(out, "HTTP/1.0 501 Not Implemented",
                    "text/html; charset=utf-8", body.length());
          }
          out.write(body);
          out.flush();

        }
      }

      else { // method does not equal "GET" / "HEAD" / "POST"
        String body = new StringBuilder("<HTML>\r\n")
                .append("<HEAD><TITLE>File Not Found</TITLE>\r\n")
                .append("</HEAD>\r\n")
                .append("<BODY>")
                .append("<H1>HTTP Error 501: Not Implemented</H1>\r\n")
                .append("</BODY></HTML>\r\n").toString();
        if (version.startsWith("HTTP/")) { // send a MIME header
          sendHeader(out, "HTTP/1.0 501 Not Implemented",
                  "text/html; charset=utf-8", body.length());
        }
        out.write(body);
        out.flush();
      }
    } catch (IOException ex) {
      logger.log(Level.WARNING,
              "Error talking to " + connection.getRemoteSocketAddress(), ex);
    } finally {
      try {
        connection.close();
      }
      catch (IOException ex) {}
    }
  }

  private void sendHeader(Writer out, String responseCode,
                          String contentType, int length)
          throws IOException {
    out.write(responseCode + "\r\n");
    Date now = new Date();
    out.write("Date: " + now + "\r\n");
    out.write("Server: JHTTP 2.0\r\n");
    out.write("Content-length: " + length + "\r\n");
    out.write("Content-type: " + contentType + "\r\n\r\n");
    out.flush();
  }
}