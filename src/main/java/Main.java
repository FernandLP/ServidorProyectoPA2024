import java.io.*;
import java.net.*;

public class Main {
    public static void main(String[] args) throws IOException {
        int port = 12345;
        ServerSocket servidorSocket = new ServerSocket(port); // Puerto en el que el servidor escucha las conexiones
        System.out.println("Servidor esperando conexiones...");
        while (true) {
            Socket clienteSocket = servidorSocket.accept(); // Esperar a que un cliente se conecte
            System.out.println("Cliente conectado desde " + clienteSocket.getInetAddress() + ":" + clienteSocket.getPort());

            // Manejar la conexión con el cliente en un hilo aparte
            Thread clienteThread = new Thread(new ClienteHandler(clienteSocket));
            clienteThread.start();
        }
    }
}



