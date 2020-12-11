package com.offcn.pay.service;

import java.util.Map;

//支付宝接口
public interface AliPayService {

    //生成支付宝支付二维码             订单编号             应付金额
    public Map createNative(String out_trade_no,String total_fee);

    //查询订单状态
    public Map queryPayStatus(String out_trade_no);
}
