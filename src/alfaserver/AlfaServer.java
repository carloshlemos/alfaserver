package alfaserver;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.StringTokenizer;

/**
 * 
 * @author carloshlemos
 *
 */
public class AlfaServer {

	public static void main(String args[]) {
		int port;
		ServerSocket server_socket;
		try {
			port = Integer.parseInt(args[0]);
		} catch (Exception e) {
			port = 8888;
		}
		try {

			server_socket = new ServerSocket(port);
			System.out.println("AlfaServer executando na porta " + server_socket.getLocalPort());

			// servidor com loop infinito
			while (true) {
				Socket socket = server_socket.accept();
				System.out.println("Nova conexão aceita " + socket.getInetAddress() + ":" + socket.getPort());

				// Constroi handler para processar cada HTTP request.
				try {
					httpRequestHandler request = new httpRequestHandler(socket);
					// Cria uma nova thread para cada processo de request.
					Thread thread = new Thread(request);

					// Inicia a thread
					thread.start();
				} catch (Exception e) {
					System.out.println(e);
				}
			}
		} catch (IOException e) {
			System.out.println(e);
		}
	}
}

class httpRequestHandler implements Runnable {
	final static String CRLF = "\r\n";

	Socket socket;

	InputStream input;

	OutputStream output;

	BufferedReader br;

	URL location;

	public httpRequestHandler(Socket socket) throws Exception {
		this.socket = socket;
		this.input = socket.getInputStream();
		this.output = socket.getOutputStream();
		this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.location = AlfaServer.class.getProtectionDomain().getCodeSource().getLocation();
	}

	public void run() {
		try {
			processRequest();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	private void processRequest() throws Exception {
		while (true) {

			String headerLine = br.readLine();
			System.out.println(headerLine);
			if (headerLine.equals(CRLF) || headerLine.equals(""))
				break;

			StringTokenizer s = new StringTokenizer(headerLine);
			String temp = s.nextToken();

			if (temp.equals("GET")) {

				String fileName = s.nextToken();
				fileName = location.getPath() + "web" + fileName;

				FileInputStream fis = null;
				boolean fileExists = true;
				try {
					fis = new FileInputStream(fileName);
				} catch (FileNotFoundException e) {
					fileName = location.getPath() + "web/error404.html";
					fis = new FileInputStream(fileName);
					fileExists = false;
				}
				String serverLine = "AlfaServer: Simples Http Server com Java";
				String statusLine = null;
				String contentTypeLine = null;
				String entityBody = null;
				String contentLengthLine = "error";
				if (fileExists) {
					statusLine = "HTTP/1.0 200 OK" + CRLF;
					contentTypeLine = "Content-type: " + contentType(fileName) + CRLF;
					contentLengthLine = "Content-Length: " + (new Integer(fis.available())).toString() + CRLF;
				} else {
					statusLine = "HTTP/1.0 404 Not Found" + CRLF;
					contentTypeLine = "Content-type: " + contentType(fileName) + CRLF;
					contentLengthLine = "Content-Length: " + (new Integer(fis.available())).toString() + CRLF;
					// contentTypeLine = "text/html";
					// entityBody = "<HTML>" +
					// "<HEAD><TITLE>404 Não Encontrado</TITLE></HEAD>" +
					// "<BODY>404 Não Encontrado"
					// + "<br>usage:http://yourHostName:port/" +
					// "fileName.html</BODY></HTML>";
				}

				// Envia o status.
				output.write(statusLine.getBytes());
				System.out.println(statusLine);

				// Envia a apresentação do servidor.
				output.write(serverLine.getBytes());
				System.out.println(serverLine);

				// Envia o tipo do conteúdo da resposta.
				output.write(contentTypeLine.getBytes());
				System.out.println(contentTypeLine);

				// Envia o tamanho da resposta.
				output.write(contentLengthLine.getBytes());
				System.out.println(contentLengthLine);

				// Envia uma linha em branco pra dizer ao HTTP que encerrou.
				output.write(CRLF.getBytes());
				System.out.println(CRLF);

				// Envia o arquivo solicitado.
				if (fileExists) {
					sendBytes(fis, output);
					fis.close();
				} else {
					// Envia o arquivo de erro, caso não exista o arquivo
					// solicitado.
					sendBytes(fis, output);
				}
			}
		}

		// Fecha todos as saídas.
		try {
			output.close();
			br.close();
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Método para gerar os bytes do arquivo solicitado.
	private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception {

		byte[] buffer = new byte[1024];
		int bytes = 0;

		while ((bytes = fis.read(buffer)) != -1) {
			os.write(buffer, 0, bytes);
		}
	}

	// Método para adquirir o tipo do arquivo.
	private static String contentType(String fileName) {
		if (fileName.endsWith(".htm") || fileName.endsWith(".html") || fileName.endsWith(".txt")) {
			return "text/html";
		} else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
			return "image/jpeg";
		} else if (fileName.endsWith(".gif")) {
			return "image/gif";
		} else {
			return "application/octet-stream";
		}
	}
}
