package edu.ucsb.cs.cs184.elicker.eclicker;
public class DeviceIdentifier {
    @Override
    public String toString() {
        return "DeviceIdentifier{" +
                "deviceId='" + deviceId + '\'' +
                ", deviceToken='" + deviceToken + '\'' +
                '}';
    }

    public DeviceIdentifier(String deviceId, String deviceToken){
        this.deviceId = deviceId;
        this.deviceToken = deviceToken;
    }
    public String deviceId;
    public String deviceToken;
}
