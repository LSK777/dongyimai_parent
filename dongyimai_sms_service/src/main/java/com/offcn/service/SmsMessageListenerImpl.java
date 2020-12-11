package com.offcn.service;

import com.offcn.utils.SmsUtil;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.swing.text.StyleContext;

@Component
public class SmsMessageListenerImpl implements MessageListener {

    @Autowired
    private SmsUtil smsUtil;

    @Override
    public void onMessage(Message message) {
        if (message instanceof MapMessage){
            MapMessage mapMessage = (MapMessage) message;
            try {
                String mobile = mapMessage.getString("mobile");
                String code = mapMessage.getString("code");
                //执行发送短信
                HttpResponse response = smsUtil.sendSms(mobile, code);
                System.out.println(EntityUtils.toString(response.getEntity()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
















