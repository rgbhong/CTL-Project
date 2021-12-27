package com.gbhong.TempHumiMonitor;

public class DryBoxObject {
    //json data와 동일한 이름
    private String DryBox;
    private String Status;
    private String start;
    private String end;
    private int CNT;

    public DryBoxObject(String DryBox, String Status, int CNT, String start, String end) {
        this.DryBox = DryBox;
        this.Status = Status;
        this.start = start;
        this.end = end;
        this.CNT = CNT;
    }


    public String getDryBox() {
        return DryBox;
    }

    public String getStatus() {
        return Status;
    }

    public String getstart() {
        return start;
    }

    public String getend() {
        return end;
    }

    public int getCNT() {
        return CNT;
    }

    public void setDryBox(String DryBox) {
        this.DryBox=DryBox;
    }

    public void setStatus(String Status) {
        this.Status = Status;
    }

    public void setstart(String start) {
        this.start = start;
    }

    public void setend(String end) {
        this.end = end;
    }

    public void setCNT(int CNT) {
        this.CNT = CNT;
    }
}

