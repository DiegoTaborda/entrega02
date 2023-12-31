package sistemasdistribuidos.servidor;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.mindrot.jbcrypt.BCrypt;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerController {
    private String SECRET_KEY = "AoT3QFTTEkj16rCby/TPVBWvfSQHL3GeEz3zVwEd6LDrQDT97sgDY8HJyxgnH79jupBWFOQ1+7fRPBLZfpuA2lwwHqTgk+NJcWQnDpHn31CVm63Or5c5gb4H7/eSIdd+7hf3v+0a5qVsnyxkHbcxXquqk9ezxrUe93cFppxH4/kF/kGBBamm3kuUVbdBUY39c4U3NRkzSO+XdGs69ssK5SPzshn01axCJoNXqqj+ytebuMwF8oI9+ZDqj/XsQ1CLnChbsL+HCl68ioTeoYU9PLrO4on+rNHGPI0Cx6HrVse7M3WQBPGzOd1TvRh9eWJrvQrP/hm6kOR7KrWKuyJzrQh7OoDxrweXFH8toXeQRD8=";
    @FXML
    private Label welcomeText;

    @FXML
    private TextField serverPort;

    private ExecutorService threadPool = Executors.newCachedThreadPool();

    private ArrayList<User> userList = new ArrayList<>();

    private int idCounter = 0;

    private boolean acceptingConnections = true;

    @FXML
    protected void onHelloButtonClick() {
        ServerService serverService = new ServerService();
        serverService.setOnSucceeded(event -> {
            welcomeText.setText("Servidor Iniciado!");
        });

        initializeUsers();

        serverService.start();
    }

    private void initializeUsers() {
        //int id = idCounter++;
        //senhas 123456 para ambos
        User exampleAdmin = new User(idCounter, "admin", "a", hash("E10ADC3949BA59ABBE56E057F20F883E"), true, "");
        idCounter++;
        User exampleUser = new User(idCounter, "usuario default", "user@email.com", hash("E10ADC3949BA59ABBE56E057F20F883E"), false, "");
        idCounter++;
        userList.add(exampleAdmin);
        userList.add(exampleUser);      
    }
    
    private String getResponse(String request) {
        JSONObject responseJson = new JSONObject();
        
        try {
            JSONObject requestJson = new JSONObject(request);

            // Check if the action is "login"
            if (requestJson.has("action") && requestJson.getString("action").equals("autocadastro-usuario")) {
                responseJson =  getRegisterResponse(requestJson);
            }
            else if (requestJson.has("action") && requestJson.getString("action").equals("login")) {
                responseJson =  getLoginResponse(requestJson);
            }
            else if (requestJson.has("action") && requestJson.getString("action").equals("logout")) {
                responseJson = getLogoutResponse(requestJson);
            }
            else if (requestJson.has("action") && requestJson.getString("action").equals("pedido-proprio-usuario")) {
                responseJson = getLoggedUserData(requestJson);
            }
            else if (requestJson.has("action") && requestJson.getString("action").equals("listar-usuarios")) {
                responseJson = getListUsers(requestJson);
            }
            else if (requestJson.has("action") && (requestJson.getString("action").equals("autoedicao-usuario") || requestJson.getString("action").equals("edicao-usuario"))) {   	
                responseJson = getChangeUser(requestJson);
            }
            else if (requestJson.has("action") && (requestJson.getString("action").equals("excluir-proprio-usuario") || requestJson.getString("action").equals("excluir-usuario"))) {   	
                responseJson = getDeleteUser(requestJson);
            } else {
                responseJson.put("action", requestJson.has("action"));
                responseJson.put("error", true);
                responseJson.put("message", "Ação desconhecida");
            }
        }
        catch(JSONException e) {
            System.out.println(e);
        }
        String response = responseJson.toString();
        //System.out.println("---------------------------------");
        System.out.println("Respondendo ao cliente: " + response);
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------");
        return response;
    }

    private JSONObject getListUsers(JSONObject requestJson) {
        JSONObject responseJson = new JSONObject();
        //System.out.println("Entrei Lista de usuarios");
        try {
        	
        	JSONArray users = new JSONArray();
        	JSONObject data = new JSONObject();
        	
        	//String token = requestJson.getJSONObject("data").getString("token");
        	
        	responseJson.put("message", "Sucesso");
            responseJson.put("error", false);
            responseJson.put("action", "listar-usuarios");
        	
        	for(User user: userList) {
        		JSONObject usuario = new JSONObject();
        		usuario.put("id", user.getId());
        		usuario.put("name", user.getName());
        		usuario.put("type", user.getType());
        		usuario.put("email", user.getEmail());
        		users.put(usuario);
        	}
        	responseJson.put("data", data);
        	data.put("users", users);        
        }
        catch(JSONException e) {
            System.out.println(e.toString());
        }
        return responseJson;
    }
    
    private JSONObject getChangeUser(JSONObject requestJson) {
    	JSONObject responseJson = new JSONObject();
    	System.out.println("Entrei Mudança de usuario");
    	try {
    		
    		int id;
    		JSONObject jsonData = requestJson.getJSONObject("data");
    		
    		if(requestJson.getString("action").equals("edicao-usuario")) {
    			id = Integer.parseInt(jsonData.getString("user_id"));
    			responseJson.put("action",requestJson.getString("action"));
    		} else {
    			id = Integer.parseInt(jsonData.getString("id"));
    			responseJson.put("action",requestJson.getString("action"));
    		}
    		
    		//pesquisar usuario e atualizar
    		for(User user : userList) {
    			if(user.getId() == id) {
    				if(!jsonData.getString("name").isBlank() && !jsonData.isNull("name")) {
    					user.setName(jsonData.getString("name"));
    				}
    				if(!jsonData.getString("email").isBlank() && !jsonData.isNull("email")) {
    					user.setEmail(jsonData.getString("email"));
    				}
    				if(!jsonData.getString("password").isBlank() && !jsonData.isNull("password")) {
    					String hspw = hash(jsonData.getString("password"));
    					user.setPassword(hspw);
    				}
    				
    				responseJson.put("error", false);
    				responseJson.put("message", "Usuário atualizado com sucesso!");
    				
    				return responseJson;
    			}
    		}
    		
    		responseJson.put("error", true);
			responseJson.put("message", "Usuário não encontrado!");
			
			return responseJson;
    		
    	}
    	catch(JSONException e) {
    		System.out.println(e.toString());
    	}
    	
    	return responseJson;
    }
    
    private JSONObject getDeleteUser(JSONObject requestJson) {
    	JSONObject responseJson = new JSONObject();
    	try {
    		if(requestJson.getString("action").equals("excluir-usuario")) {
    			for(User user : userList) {
    				if(user.getId() == Integer.parseInt(requestJson.getJSONObject("data").getString("user_id"))) {
    					userList.remove(user);
    					
    					responseJson.put("error", false);
    					responseJson.put("message", "Usuário removido com sucesso!");
    					responseJson.put("action", "excluir-usuario");
    					
    					return responseJson;
    				}
    			}
    		} else {
    			for(User user : userList) {
    				if(requestJson.getJSONObject("data").getString("token").equals(user.getToken())) {
    					userList.remove(user);
    					
    					responseJson.put("error", false);
    					responseJson.put("message", "Usuário removido com sucesso!");
    					responseJson.put("action", "excluir-proprio-usuario");
    					
    					return responseJson;
    				}
    			}
    		}
    		
    		responseJson.put("error", true);
			responseJson.put("message", "Usuario nao encontrado!");
			responseJson.put("action", "excluir-proprio-usuario");
    		
    	} catch (JSONException e) {
    		System.out.println(e.toString());
    	}
    	return responseJson;
    }
    
    private JSONObject getLogoutResponse(JSONObject requestJson) {
        JSONObject responseJson = new JSONObject();
        try {
            responseJson.put("action", requestJson.has("logout"));
            responseJson.put("error", false);
            responseJson.put("message", "Logout efetuado com sucesso");
        }
        catch(JSONException e) {
            System.out.println(e.toString());
        }
        return responseJson;
    }

    private JSONObject getLoggedUserData(JSONObject requestJson) {
        JSONObject responseJson = new JSONObject();
        try {
            String token = requestJson.getJSONObject("data").getString("token");
            for (User user: userList) {
                if(user.getToken().equals(token)) {
                    JSONObject data = new JSONObject();
                    JSONObject jasonuser = new JSONObject();
                    data.put("user", jasonuser);
                    jasonuser.put("id", user.getId());
                    jasonuser.put("name", user.getName());
                    jasonuser.put("type", user.getType());
                    jasonuser.put("email", user.getEmail());

                    responseJson.put("action", "pedido-proprio-usuario");
                    responseJson.put("error", false);
                    responseJson.put("message", "Sucesso");
                    responseJson.put("data", data);
                    return responseJson;
                }
            }

            responseJson.put("action", "pedido-proprio-usuario");
            responseJson.put("error", true);
            responseJson.put("message", "Sessão falhou!");
        }
        catch (JSONException e) {
            System.out.println(e.toString());
        }
        return responseJson;
    }
    private JSONObject getRegisterResponse(JSONObject requestJson) {
        JSONObject responseJson = new JSONObject();
        try {
            String name = requestJson.getJSONObject("data").getString("name");
            String email = requestJson.getJSONObject("data").getString("email");
            String password = requestJson.getJSONObject("data").getString("password");
            String hashedpw = hash(password);
            int id = idCounter++;
            System.out.println("PW: " + password+ " || HASH: " + hashedpw);

            // Check if there is a user in the userList with the provided email and password
            for (User user : userList) {
                if (user.getEmail().equals(email)) {
                    // Return a successful login response with the user's token
                    JSONObject data = new JSONObject();
                    responseJson.put("action", "autocadastro-usuario");
                    responseJson.put("error", true);
                    responseJson.put("message", "email já cadastrado!");

                    return responseJson;
                }
            }

            // If no matching user is found, register user and return success message
            User newUser = new User(id, name , email, hashedpw, false, generateToken(id,false));
            System.out.println(newUser.toString());
            userList.add(newUser);
            responseJson.put("action", "autocadastro-usuario");
            responseJson.put("error", false);
            responseJson.put("message", "Usuário cadastrado com sucesso");
        }
        catch (JSONException e) {
            System.out.println(e.toString());
        }
        return responseJson;
    }
    
    private JSONObject getLoginResponse(JSONObject requestJson) {
        JSONObject responseJson = new JSONObject();
        try {
            String email = requestJson.getJSONObject("data").getString("email");
            String password = requestJson.getJSONObject("data").getString("password");

            // Check if there is a user in the userList with the provided email and password
            for (User user : userList) {
                if (user.getEmail().equals(email) && BCrypt.checkpw(password,user.getPassword()) ) {
                    // Return a successful login response with the user's token
                    user.setToken(generateToken(user.getId(),user.isAdmin()));
                    JSONObject data = new JSONObject();
                    data.put("token", user.getToken());

                    responseJson.put("action", "login");
                    responseJson.put("error", false);
                    responseJson.put("message", "logado com sucesso");
                    responseJson.put("data", data);

                    return responseJson;
                }
            }

            // If no matching user is found, return an error response
            responseJson.put("action", "login");
            responseJson.put("error", true);
            responseJson.put("message", "Credenciais incorretas");
        }
        catch (JSONException e) {
            System.out.println(e.toString());
        }
        return responseJson;
    }

    public String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public String generateToken(int id, boolean isAdmin) {
        return Jwts.builder()
                .claim("user_id", id)
                .claim("isAdmin", isAdmin)
                .setSubject(Integer.toString(id))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    private class ServerService extends Service<Void> {
        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() {
                    int port = Integer.parseInt(serverPort.getText());
                    try (ServerSocket serverSocket = new ServerSocket(port)) {
                        System.out.println("Servidor na porta " + port);

                        while (acceptingConnections) {
                            Socket clientSocket = serverSocket.accept();
                            System.out.println("Cliente conectou");

                            threadPool.execute(() -> {
                                try {
                                    InputStream in = clientSocket.getInputStream();
                                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                                    String message;
                                    while ((message = reader.readLine()) != null) {
                                        System.out.println("Recebida do cliente: " + message);

                                        String response = getResponse(message);

                                        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                                        writer.println(response);

                                        if (message.equals("EOF")) {
                                            break;
                                        }
                                    }

                                    clientSocket.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
        }
    }

    @FXML
    protected void onStopButtonClick() {
        acceptingConnections = false;
    }
}
