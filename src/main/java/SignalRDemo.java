    import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
    import com.microsoft.signalr.HubConnectionState;

    public class SignalRDemo {

    static final String SIGNALR_HUB_URL = "";
    static final String TARGET_NAME = "";

    public static void main(String[] args) {
        HubConnection hubConnection = HubConnectionBuilder.create(SIGNALR_HUB_URL)
                .build();

        if (hubConnection.getConnectionState().compareTo(HubConnectionState.DISCONNECTED) == 1) {
            System.out.println(">>>>>>> hubConnection.start");
            hubConnection.start();
        }


        if (hubConnection.getConnectionState().compareTo(HubConnectionState.CONNECTED) == 1) {
            System.out.println(">>>>>>> hubConnection.stop");
            hubConnection.stop();
        }

        hubConnection.on(TARGET_NAME, () -> {

           System.out.println(">>>>>>>>>>>>>> In " + TARGET_NAME + " on method");
        });

    }
}
