package com.offcn.entity;

import java.io.Serializable;
import java.util.List;

//分页的复合实体类
public class PageResult implements Serializable {
    private List rows;  //分页查询集合
    private Long total; //总记录数

    public PageResult() {
    }

    public PageResult( Long total,List rows) {
        this.rows = rows;
        this.total = total;
    }

    public List getRows() {
        return rows;
    }

    public void setRows(List rows) {
        this.rows = rows;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}
