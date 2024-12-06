package networking;

import cluster.management.ServiceRegistry;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class WebServer {

    private int port;
    private HttpServer server;
    private OnRequestHandler searchHandler;
    private final String HOMEPAGE_ENDPOINT = "/";

    private final String STATUS_ENDPOINT = "/status";
    private final String ASSET_DIRECTORY = "/ui_assets/";
    public WebServer( OnRequestHandler searchHandler, int port ){
        this.searchHandler = searchHandler;
        this.port = port;
    }
    public void startServer() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);

        HttpContext statusCheck = server.createContext(STATUS_ENDPOINT);
        HttpContext searchContext = server.createContext(searchHandler.getEndpoint());

        statusCheck.setHandler(this::handleStatus);
        searchContext.setHandler(this::handleSearch);

        HttpContext assetContext = server.createContext(HOMEPAGE_ENDPOINT);
        assetContext.setHandler(this::handleRequestForAsset);

        server.setExecutor(Executors.newFixedThreadPool(6));
        server.start();

    }

    private void handleRequestForAsset(HttpExchange exchange) throws IOException {
        if(!exchange.getRequestMethod().equalsIgnoreCase("get")){
            exchange.close();
            return;
        }
        byte[] response;
        String asset = exchange.getRequestURI().getPath();
        System.out.println(asset + "is asset");
        if( asset.equals(HOMEPAGE_ENDPOINT)){

            response = readUIFrom( ASSET_DIRECTORY + "index.html");
            System.out.println(ASSET_DIRECTORY + "index.html");
        }else{
            response = readUIFrom( asset );
        }
        addContentType( asset, exchange );
        System.out.println("hehehehe");
        sendResponses(response,exchange);

    }

    private void addContentType(String asset, HttpExchange exchange) {
        String contentType = "text/html";
        if(asset.endsWith("js")){
            contentType = "text/javascript";
        }
        else if(asset.endsWith(("css"))){
            contentType = "text/css";
        }
        System.out.println(contentType + " is content type");
        exchange.getResponseHeaders().add("Content-Type", contentType);
    }

    private byte[] readUIFrom(String asset) throws IOException {
        InputStream response = getClass().getResourceAsStream(asset);
        if(response == null){
            return new byte[] {};
        }
        return response.readAllBytes();

    }

    private void handleSearch(HttpExchange exchange) throws IOException {
        if( !exchange.getRequestMethod().equalsIgnoreCase("post")){
            exchange.close();
            return;
        }
        byte[] responseByte = searchHandler.requestHandle(exchange.getRequestBody().readAllBytes());
        sendResponses(responseByte, exchange);
    }

    private void handleStatus(HttpExchange exchange) {
        if( !exchange.getRequestMethod().equalsIgnoreCase("get")){
            System.out.println("hahahahha");
            exchange.close();
            return;
        }
        String message = "Server is live !";
        byte[] data = message.getBytes();
        try {
            sendResponses(data, exchange);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void sendResponses(byte[] data, HttpExchange exchange ) throws IOException {
        exchange.sendResponseHeaders(200, data.length);

        OutputStream responseBody = exchange.getResponseBody();
        responseBody.write(data);
        responseBody.flush();
        responseBody.close();
    }
}
