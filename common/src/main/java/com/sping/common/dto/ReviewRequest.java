package com.sping.common.dto;

public class ReviewRequest {

    private String action;   // APPROVE or REJECT
    private String remark;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
