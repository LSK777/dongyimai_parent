package com.offcn.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.offcn.pojo.TbItem;
import com.offcn.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.List;

@Component
public class SearchMessageListenerImpl implements MessageListener {
    @Autowired
    private ItemSearchService itemSearchService;

    @Override
    public void onMessage(Message message) {
        if (message instanceof TextMessage){
            //1.获取消息并做类型转换
            TextMessage textMessage =(TextMessage) message;
            try {
                String itemListStr = textMessage.getText();
                //2.将获取的消息做类型转换成操作对象
                List<TbItem> itemList = JSON.parseArray(itemListStr, TbItem.class);
                //3.执行导入solr
                itemSearchService.importItem(itemList);
                System.out.println("成功接收消息中间件的消息,导入solr成功");
            } catch (JMSException e) {
                e.printStackTrace();
            }

        }

    }
}
