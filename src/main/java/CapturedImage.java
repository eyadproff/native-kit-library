public class CapturedImage {

    private String deviceName;
    private String base64String;

    public CapturedImage(String deviceName, String base64String) {
        this.deviceName = deviceName;
        this.base64String = base64String;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getBase64String() {
        return base64String;
    }

    public void setBase64String(String base64String) {
        this.base64String = base64String;
    }
}
