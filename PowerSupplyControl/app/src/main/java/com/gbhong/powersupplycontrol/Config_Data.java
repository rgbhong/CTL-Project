package com.gbhong.powersupplycontrol;

public class Config_Data {
    String Cvalue;
    String Clabel;
    int Ctype;
    int Cgrp;
    int Cinx;


    public int getInx() {
        return Cinx;
    }
    public int getGrp() {
        return Cgrp;
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

    public void setInx(int inx) {
        this.Cinx = inx;
    }
    public void setGrp(int grp) {
        this.Cgrp = grp;
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
