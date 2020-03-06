package socotra.model;

import socotra.Client;
import socotra.util.Util;

public class LoginModel {

    /**
     * The error type of the connection.
     */
    private int errorType = 1;

    /**
     * Setter for error type.
     *
     * @param errorType The error type needs to be set.
     */
    public void setErrorType(int errorType) {
        this.errorType = errorType;
    }

    public int handleLogin(String serverName, String username, String password) {
        Client.setClientThread(new ClientThread(Util.isEmpty(serverName) ? "localhost" : serverName, this, username, password));
        Client.getClientThread().start();
        // Wait until the ClientThread notify it.
        synchronized (this) {
            try {
                this.wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return errorType;
    }

}