import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.persistence.*;
import models.TareaEntity;
import models.UsuarioEntity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClienteHandler implements Runnable {

    private final Socket clienteSocket;
    private Gson gson;
    private int idUsuario;

    public ClienteHandler(Socket clienteSocket) {
        gson = new Gson();
        this.clienteSocket = clienteSocket;
    }

    @Override
    public void run() {
        try {
            BufferedReader entradaDesdeCliente = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()));
            PrintWriter salidaACliente = new PrintWriter(clienteSocket.getOutputStream(), true);

            String mensajeDelCliente;
            while ((mensajeDelCliente = entradaDesdeCliente.readLine()) != null) {
                JsonObject jsonObject = gson.fromJson(mensajeDelCliente, JsonObject.class);
                String type = jsonObject.get("type").getAsString();

                switch (type) {
                    case "login":
                        salidaACliente.write(handleLogin(jsonObject) + "\n");
                        salidaACliente.flush();
                        break;
                    case "allTask":
                        String tareasJson = handleAllTask(jsonObject);
                        salidaACliente.write(tareasJson + "\n");
                        salidaACliente.flush();
                        break;
                    case "loadProfile":
                        salidaACliente.write(handleLoadProfile(jsonObject) + "\n");
                        salidaACliente.flush();
                        break;
                    case "saveData":
                        String resultadoGuardar = handleSaveData(jsonObject);
                        salidaACliente.write(resultadoGuardar + "\n");
                        salidaACliente.flush();
                        break;
                    case "signup":
                        salidaACliente.write(handleSigup(jsonObject) + "\n");
                        salidaACliente.flush();
                        break;
                    default:
                        System.out.println("Mensaje desconocido: " + mensajeDelCliente);
                        break;
                }
            }
        }  catch (SocketException e) {
            System.err.println("El cliente se ha desconectado: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error en la comunicación con el cliente: " + e.getMessage());
        } finally {
            try {
                if (clienteSocket != null && !clienteSocket.isClosed()) {
                    clienteSocket.close();
                }
            } catch (IOException e) {
                System.err.println("Error al cerrar el socket: " + e.getMessage());
            }
        }
    }

    private String handleSigup(JsonObject jsonObject) {
        String nombre = jsonObject.get("nombre").getAsString();
        String correo = jsonObject.get("correo").getAsString();
        String contrasenia = jsonObject.get("contrasenia").getAsString();

        // Crear una instancia EntityManagerFactory
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("gestion_tareas");
        // Crear una instancia EntityManager
        EntityManager em = emf.createEntityManager();
        // Obtener una nueva transacción
        EntityTransaction transaction = em.getTransaction();

        try {
            // Comenzar la transacción
            transaction.begin();

            // Verificar si el correo electrónico ya está registrado
            Query query = em.createQuery("SELECT u FROM UsuarioEntity u WHERE u.email = :correo");
            query.setParameter("correo", correo);
            List<UsuarioEntity> usuarios = query.getResultList();

            if (!usuarios.isEmpty()) {
                // El correo electrónico ya está registrado
                // Rollback de la transacción
                transaction.rollback();
                // Cerrar el EntityManager y EntityManagerFactory
                em.close();
                emf.close();
                // Devolver un mensaje de error al cliente
                return "{\"status\":\"failure\", \"message\":\"El correo electrónico ya está registrado.\"}";
            }

            // Crear una nueva entidad de usuario
            UsuarioEntity nuevoUsuario = new UsuarioEntity();
            nuevoUsuario.setNombre(nombre);
            nuevoUsuario.setEmail(correo);
            nuevoUsuario.setContrasena(contrasenia);

            // Persistir el nuevo usuario en la base de datos
            em.persist(nuevoUsuario);

            // Commit la transacción
            transaction.commit();

            // Cerrar el EntityManager y EntityManagerFactory
            em.close();
            emf.close();

            // Devolver un mensaje de éxito al cliente
            return "{\"status\":\"success\", \"message\":\"Usuario registrado correctamente.\"}";
        } catch (Exception e) {
            // Si ocurre un error, hacer rollback de la transacción
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            // Cerrar el EntityManager y EntityManagerFactory
            em.close();
            emf.close();
            // Devolver un mensaje de error al cliente
            return "{\"status\":\"failure\", \"message\":\"Error al registrar el usuario.\"}";
        }
    }

    private String handleSaveData(JsonObject jsonObject) {
        JsonArray tareasJson = jsonObject.getAsJsonArray("tareas");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("gestion_tareas");
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();

        try {
            transaction.begin();

            for (JsonElement tareaElement : tareasJson) {
                JsonObject tareaJson = tareaElement.getAsJsonObject();
                String estado = tareaJson.get("estado").getAsString();

                if (estado.equals("editada")) {
                    int tareaId = tareaJson.get("id").getAsInt();
                    TareaEntity existingTarea = em.find(TareaEntity.class, tareaId);

                    if (existingTarea != null) {
                        existingTarea.setTitulo(tareaJson.get("titulo").getAsString());
                        existingTarea.setDescripcion(tareaJson.get("descripcion").getAsString());
                        try {
                            existingTarea.setFechaLimite(new Date(dateFormat.parse(tareaJson.get("fechaLimite").getAsString()).getTime()));
                        } catch (ParseException e) {
                            // Manejar error de parseo de fecha
                        }
                        JsonElement prioridadElement = tareaJson.get("prioridad");
                        String prioridad = (prioridadElement != null && !prioridadElement.isJsonNull()) ? prioridadElement.getAsString() : null;
                        existingTarea.setPrioridad(prioridad);
                        existingTarea.setColor(tareaJson.get("color").getAsString());
                        existingTarea.setUsuarioId(idUsuario); // Usar el idUsuario global
                        em.merge(existingTarea); // Actualizar la tarea en la base de datos
                    }
                } else if (estado.equals("borrado")) {
                    int tareaId = tareaJson.get("id").getAsInt();
                    TareaEntity existingTarea = em.find(TareaEntity.class, tareaId);

                    if (existingTarea != null) {
                        em.remove(existingTarea); // Eliminar la tarea de la base de datos
                    }
                } else if (estado.equals("nueva")) {
                    TareaEntity nuevaTarea = new TareaEntity();
                    // No establecer ID, dejar que la base de datos lo genere
                    nuevaTarea.setTitulo(tareaJson.get("titulo").getAsString());
                    nuevaTarea.setDescripcion(tareaJson.get("descripcion").getAsString());
                    try {
                        nuevaTarea.setFechaLimite(new Date(dateFormat.parse(tareaJson.get("fechaLimite").getAsString()).getTime()));
                    } catch (ParseException e) {
                        // Manejar error de parseo de fecha
                    }
                    JsonElement prioridadElement = tareaJson.get("prioridad");
                    String prioridad = (prioridadElement != null && !prioridadElement.isJsonNull()) ? prioridadElement.getAsString() : null;
                    nuevaTarea.setPrioridad(prioridad);
                    nuevaTarea.setColor(tareaJson.get("color").getAsString());
                    nuevaTarea.setUsuarioId(idUsuario); // Usar el idUsuario global
                    em.persist(nuevaTarea); // Persistir la nueva tarea en la base de datos
                }
            }

            transaction.commit();
            return "{\"type\":\"saveDataResponse\", \"status\":\"success\"}";
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            // Manejar el error de persistencia
            return "{\"type\":\"saveDataResponse\", \"status\":\"error\", \"message\":\"" + e.getMessage() + "\"}";
        } finally {
            em.close();
            emf.close();
        }
    }

    private String handleLoadProfile(JsonObject jsonObject) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("gestion_tareas");
        EntityManager em = emf.createEntityManager();

        try {
            // Buscar el usuario en la base de datos utilizando el idUsuario
            UsuarioEntity usuario = em.find(UsuarioEntity.class, idUsuario);
            System.out.println(idUsuario);

            if (usuario != null) {
                // Crear el objeto JSON con los datos del usuario
                JsonObject usuarioJson = new JsonObject();
                usuarioJson.addProperty("id", usuario.getId());
                usuarioJson.addProperty("nombre", usuario.getNombre());
                usuarioJson.addProperty("email", usuario.getEmail());
                usuarioJson.addProperty("contrasena", usuario.getContrasena());

                // Convertir el objeto JSON a una cadena y devolverla
                return gson.toJson(usuarioJson);
            } else {
                // Si no se encuentra el usuario, devolver un mensaje de error
                JsonObject errorJson = new JsonObject();
                errorJson.addProperty("error", "Usuario no encontrado");
                return gson.toJson(errorJson);
            }
        } finally {
            em.close();
            emf.close();
        }
    }

    private String handleAllTask(JsonObject jsonObject) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("gestion_tareas");
        EntityManager em = emf.createEntityManager();

        try {
            // Consultar las tareas del usuario
            TypedQuery<TareaEntity> query = em.createQuery("SELECT t FROM TareaEntity t WHERE t.usuarioId = :usuarioId", TareaEntity.class);
            query.setParameter("usuarioId", idUsuario);
            List<TareaEntity> tareas = query.getResultList();

            // Convertir las tareas en formato JSON
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("type", "allTaskResponse");
            responseJson.add("tareas", gson.toJsonTree(tareas));

            return gson.toJson(responseJson);
        } finally {
            em.close();
            emf.close();
        }
    }

    private String handleLogin(JsonObject jsonObject) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("gestion_tareas");
        EntityManager em = emf.createEntityManager();
        String correo = jsonObject.get("usuario").getAsString();
        String contrasena = jsonObject.get("contraseña").getAsString();

        try {
            TypedQuery<UsuarioEntity> query = em.createQuery("SELECT u FROM UsuarioEntity u WHERE u.email = :correo AND u.contrasena = :contrasena", UsuarioEntity.class);
            query.setParameter("correo", correo);
            query.setParameter("contrasena", contrasena);
            UsuarioEntity usuario = query.getResultList().stream().findFirst().orElse(null);
            if (usuario != null) {
                JsonObject loginJson = new JsonObject();
                loginJson.addProperty("validity", "VALIDO");
                loginJson.addProperty("id", usuario.getId());
                loginJson.addProperty("nombre", usuario.getNombre());
                loginJson.addProperty("email", usuario.getEmail());
                loginJson.addProperty("contrasena", usuario.getContrasena());
                String jsonString = gson.toJson(loginJson);
                idUsuario = usuario.getId();
                return jsonString;
            } else {
                JsonObject loginJson = new JsonObject();
                loginJson.addProperty("validity", "INVALIDO");
                String jsonString = gson.toJson(loginJson);
                return jsonString;
            }
        } finally {
            em.close();
            emf.close();
        }
    }
}
