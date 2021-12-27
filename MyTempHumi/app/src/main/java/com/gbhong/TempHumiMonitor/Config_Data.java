package com.gbhong.TempHumiMonitor;

public class Config_Data {
    String Cname;
    String Cinx;
    String Cvalue;
    String Clabel;
    int Ctype;

    public String getName() {
        return Cname;
    }

    public String getInx() {
        return Cinx;
    }

    public String getValue() {
        return Cvalue;
    }

    public String getLabel() {
        return Clabel;
    }

    public int getType() {
        return Ctype;
    }

    public void setName(String name) {
        this.Cname = name;
    }

    public void setInx(String inx) {
        this.Cinx = inx;
    }

    public void setValue(String value) {
        this.Cvalue = value;
    }

    public void setLabel(String label) {
        this.Clabel = label;
    }

    public void setType(int type) {
        this.Ctype = type;
    }
}
