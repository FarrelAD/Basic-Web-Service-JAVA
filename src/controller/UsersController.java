package controller;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.net.Socket;
import java.net.URI;

import model.User;
import model.Users;

public class UsersController {
    private Users data;

    public UsersController(Users data) {
        this.data = data;
    }

    public void getAllUsers(Socket clientSocket) {
        try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html");
            out.println();

            ArrayList<User> dataResult = data.getAllData();
            String result = "<ol>";
            for (User datum : dataResult) {
                result += """
                    <li>
                        <p>Name : """+datum.getName()+"""
                        </p>
                        <p>Job : """+datum.getJob()+"""
                        </p>
                    </li>
                """;
            }
            result += "</ol>";
            
            out.println("""
                <html>
                    <head>
                        <title>Welcome!</title>
                    </head>
                    <body>
                        <h1>This is all users data</h1>
                        """+result+"""
                    </body>
                </html>
                """);
        } catch (IOException e) {
            handleErrorResponse(clientSocket, e);
        }
    }

    public void getUserDataById(Socket clientSocket, String requestLine) {
        try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            String[] arrayRequestLine = requestLine.split(" ");

            int userId = -1;
            try {
                userId = Integer.parseInt(arrayRequestLine[1].substring("/users/".length()));
    
                String result = "";
                if (userId != -1 && userId <= data.getArrayLength()) {
                    result = """
                        <h1>You get data from the server</h1>
                        <p>ID: """+userId+"""
                        </p>
                        <p>Data:
                            <ul>
                                <li>Name: """+data.getUserDataByIndex(userId - 1).getName()+"""
                                </li>
                                <li>Job: """+data.getUserDataByIndex(userId - 1).getJob()+"""
                                </li>
                            </ul>
                        </p>
    
                        <br><br>
                        <div>
                            <h1>Update this user data!</h1>
                            <form id="form-put">
                                <input type="text" name="new-name" id="new-name-input" placeholder="New name"> 
                                <br>
                                <input type="text" name="new-job" id="new-job-input" placeholder="New job">
                                <br>
                                <button type="submit">Submit</button>
                            </form>
                            <br><br>
                            <div>
                                <h1>Delete this user data!</h1>
                                <form id="form-delete">
                                    <button type="submit">Submit</button>
                                </form>
                            </div>
                        </div>
    
                        <script>
                            const newNameInput = document.getElementById('new-name-input')
                            const newJobInput = document.getElementById('new-job-input')
                            const formPut = document.getElementById('form-put')
    
                            formPut.addEventListener('submit', function(event) {
                                event.preventDefault()
    
                                fetch('http://localhost:8000/users/"""+userId+"""
                                ', {
                                    method: 'PUT', 
                                    headers: {
                                        'Content-Type': 'application/json'
                                    },
                                    body: JSON.stringify({
                                        name: newNameInput.value,
                                        job: newJobInput.value
                                    })
                                })
                                .then(response => response.json())
                                .then(data => {
                                    alert('Data has been successfully updated.')
                                    location.reload()
                                })
                                .catch(error => console.error('Error:', error))
                            })
                        </script>
                    """;
                } else {
                    result = "<h1>Data can not be found!</h1>";
                }
    
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: text/html");
                out.println();
                out.println("""
                    <html>
                        <head>
                            <title>User Data</title>
                        </head>
                        <body>
                        """+ result +"""
                        </body>
                    </html>
                """);
            } catch (Exception e) {
                handleErrorResponse(clientSocket, e);
            }
        } catch (IOException e) {
            handleErrorResponse(clientSocket, e);
        }
    }

    public void getUserDataByQuery(Socket clientSocket, String requestLine) {
        try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            String[] arrayRequestLine = requestLine.split(" ");
            URI uri = new URI(arrayRequestLine[1]);

            Map<String, String> queryParams = extractQueryParams(uri.getQuery());

            ArrayList<User> dataResult = data.getUserDataByQuery(queryParams);
            String dataHTMLContent;
            if (!dataResult.isEmpty()) {
                dataHTMLContent = "<ul>";
                for (User datum : dataResult) {
                    dataHTMLContent += """
                        <li>
                            <p>Name: """+datum.getName()+"""
                            </p>
                            <p>Job: """+datum.getJob()+"""
                            </p>
                        </li>
                    """;
                }
                dataHTMLContent += "</ul>";
            } else {
                dataHTMLContent = "<h2>Data not found!</h2>";
            }

            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html");
            out.println();
            out.println("""
                <h1>Search result!</h1>
                """+dataHTMLContent+"""
            """);
        } catch (Exception e) {
            handleErrorResponse(clientSocket, e);
        }
    }

    public void postUserData(Socket clientSocket, BufferedReader inputParams) {
        try (BufferedReader in = inputParams;
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            String inputLine;
            int contentLength = 0;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.isEmpty()) {
                    break;
                }

                if (inputLine.startsWith("Content-Length:")) {
                    contentLength = Integer.parseInt(inputLine.split(":")[1].trim());
                }
            }

            String requestBody = null;
            if (contentLength > 0) {
                char[] body = new char[contentLength];
                in.read(body, 0, contentLength);
                requestBody = new String(body);
            }

            String responseBody = "";
            String newName = extractQueryParams(requestBody).get("name");
            String newJob = extractQueryParams(requestBody).get("job");

            data.addNewData(new User(newName, newJob));
            
            responseBody = """
                <h1>Data successfully submitted to server</h1>
                <a href="/">
                    <button type="submit" >Submit data again</button>
                </a>
            """;
            
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html");
            out.println();
            out.println(responseBody);
        } catch (IOException e) {
            handleErrorResponse(clientSocket, e);
        }
    }

    private void handleErrorResponse(Socket clientSocket, Exception e) {
        try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            out.println("HTTP/1.1 500 Internal Server Error");
            out.println("Content-Type: text/html");
            out.println();
            out.println("Error occured: " + e.getMessage());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private Map<String, String> extractQueryParams(String query) {
        Map<String, String> queryParams = new HashMap<>();
        String[] pairs = query.split("&");

        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                queryParams.put(keyValue[0], keyValue[1]);
            } else if (keyValue.length == 1) {
                queryParams.put(keyValue[0], "");
            }
        }
        
        return queryParams;
    }
}