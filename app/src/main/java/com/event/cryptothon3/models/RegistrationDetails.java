package com.event.cryptothon3.models;

import java.util.ArrayList;

public class RegistrationDetails {
    private boolean wrongTeamCode;
    private boolean registeredSuccessfully;
    private ArrayList<String> registeredDevices;

    public void setRegisteredDevices(ArrayList<String> registeredDevices){

        this.registeredDevices = new ArrayList<>();
        this.registeredDevices.addAll(registeredDevices);
    }

    public ArrayList<String> getRegisteredDevices(){ return registeredDevices; }

    public String getDeviceId1() {
        return deviceId1;
    }

    public void setDeviceId1(String deviceId1) {
        this.deviceId1 = deviceId1;
    }

    public String getDeviceId2() {
        return deviceId2;
    }

    public void setDeviceId2(String deviceId2) {
        this.deviceId2 = deviceId2;
    }

    public String getDeviceId3() {
        return deviceId3;
    }

    public void setDeviceId3(String deviceId3) {
        this.deviceId3 = deviceId3;
    }

    private String deviceId1;
    private String deviceId2;
    private String deviceId3;

    public boolean isWrongTeamCode() {
        return wrongTeamCode;
    }

    public void setWrongTeamCode(boolean wrongTeamCode) {
        this.wrongTeamCode = wrongTeamCode;
    }

    public boolean isRegisteredSuccessfully() {
        return registeredSuccessfully;
    }

    public void setRegisteredSuccessfully(boolean registeredSuccessfully) {
        this.registeredSuccessfully = registeredSuccessfully;
    }
}
