package models;

public class CapturingInfo {

    private String deviceType;
    private String captureType;
    private boolean autoCapture;

    public CapturingInfo(String deviceType, String captureType, boolean autoCapture) {
        this.deviceType = deviceType;
        this.captureType = captureType;
        this.autoCapture = autoCapture;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getCaptureType() {
        return captureType;
    }

    public void setCaptureType(String captureType) {
        this.captureType = captureType;
    }

    public boolean isAutoCapture() {
        return autoCapture;
    }

    public void setAutoCapture(boolean autoCapture) {
        this.autoCapture = autoCapture;
    }
}
