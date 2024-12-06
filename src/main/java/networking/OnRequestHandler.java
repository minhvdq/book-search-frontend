package networking;

public interface OnRequestHandler {
    byte[] requestHandle(byte[] inputData);
    String getEndpoint();
}
