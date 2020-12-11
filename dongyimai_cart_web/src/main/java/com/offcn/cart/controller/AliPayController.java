package com.offcn.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.offcn.entity.Result;
import com.offcn.order.service.OrderService;
import com.offcn.pay.service.AliPayService;
import com.offcn.pojo.TbPayLog;
import com.offcn.utils.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/alipay")
public class AliPayController {

    @Reference
    private AliPayService aliPayService;

    @Autowired
    private IdWorker idWorker;

    @Reference
    private OrderService orderService;

    @RequestMapping("/createNative")
    public Map createNative(){
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        TbPayLog tbPayLog = orderService.searchPayLogFromRedis(userId);
        if (tbPayLog!=null){
            return aliPayService.createNative(tbPayLog.getOutTradeNo(),tbPayLog.getTotalFee()+"");
        }else{
            return new HashMap();
        }

    }

    @RequestMapping("/queryPayStatus")
    public Result queryPayStatus(String out_trade_no){

        Result result = null;
        Map map = null;
        int x = 0;
        while (true) {
            try {
                map = aliPayService.queryPayStatus(out_trade_no);

            } catch (Exception e) {
                System.out.println("查询异常");
            }
            if (map==null){
                result = new Result(false,"有内鬼,停止交易");
                break;
            }
            if (map.get("tradeStatus")!=null&&((String)map.get("tradeStatus")).equals("TRADE_SUCCESS")){
                //修改支付成功状态
                orderService.updateOrderStatus(out_trade_no,(String)map.get("tradeNo"));
                result = new Result(true,"支付成功");
                break;
            }
            if (map.get("tradeStatus")!=null&&((String)map.get("tradeStatus")).equals("TRADE_CLOSED")){
                result = new Result(true,"未付款交易超时关闭，或支付完成后全额退款");
                break;
            }
            if (map.get("tradeStatus")!=null&&((String)map.get("tradeStatus")).equals("TRADE_FINISHED")){
                result = new Result(true,"交易结束,不可退款");
                break;
            }

            try {
                x++;
                if (x>=100){   //5分钟后超时
                    result = new Result(false,"二维码超时");
                    break;
                }
                Thread.sleep(3000);     //3秒执行一次循环
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

}






















