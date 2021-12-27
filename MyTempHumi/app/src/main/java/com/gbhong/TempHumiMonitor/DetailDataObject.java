package com.gbhong.TempHumiMonitor;

public class DetailDataObject {
    //json data와 동일한 이름
    private String DateTime;
    private double temp1;
    private double temp2;
    private double humi1;
    private double humi2;

    public DetailDataObject(String DateTime, double temp1, double temp2, double humi1, double humi2) {
        this.DateTime = DateTime;
        this.temp1 = temp1;
        this.temp2 = temp2;
        this.humi1 = humi1;
        this.humi2 = humi2;
    }


    public String getDateTime() {
        return DateTime;
    }

    public double getTemp1() {
        return temp1;
    }

    public double getTemp2() {
        return temp2;
    }

    public double getHumi1() {
        return humi1;
    }

    public double getHumi2() {
        return humi2;
    }

}

