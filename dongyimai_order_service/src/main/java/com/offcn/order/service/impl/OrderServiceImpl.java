package com.offcn.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.offcn.entity.PageResult;
import com.offcn.group.Cart;
import com.offcn.mapper.TbOrderItemMapper;
import com.offcn.mapper.TbOrderMapper;
import com.offcn.mapper.TbPayLogMapper;
import com.offcn.order.service.OrderService;
import com.offcn.pojo.TbOrder;
import com.offcn.pojo.TbOrderExample;
import com.offcn.pojo.TbOrderExample.Criteria;
import com.offcn.pojo.TbOrderItem;
import com.offcn.pojo.TbPayLog;
import com.offcn.utils.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;

import java.lang.management.GarbageCollectorMXBean;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 服务实现层
 *
 * @author Administrator
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private TbOrderMapper orderMapper;


    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private TbOrderItemMapper orderItemMapper;


    @Autowired
    private TbPayLogMapper payLogMapper;


    /**
     * 查询全部
     */
    @Override
    public List<TbOrder> findAll() {
        return orderMapper.selectByExample(null);
    }

    /**
     * 按分页查询
     */
    @Override
    public PageResult findPage(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        Page<TbOrder> page = (Page<TbOrder>) orderMapper.selectByExample(null);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 增加
     */
    @Override
    public void add(TbOrder order) {
        //1.根据当前登录人获取缓存中的购物车集合
        List<Cart> cartList = (List<Cart>) redisTemplate.boundHashOps("cartList").get(order.getUserId());

        if (!CollectionUtils.isEmpty(cartList)) {
            double total_fee_double = 0.00;
            List<String> orderList = new ArrayList<String>();

            //2.遍历购物车集合
            for (Cart cart : cartList) {
                //3.保存订单信息
                long orderId = idWorker.nextId();                            //订单ID 通过分布式ID生成器获得
                System.out.println("orderId:" + orderId);
                TbOrder tbOrder = new TbOrder();
                tbOrder.setOrderId(orderId);
                tbOrder.setPaymentType(order.getPaymentType());            //支付方式
                tbOrder.setStatus("1");                                        //状态：1、未付款，2、已付款，3、未发货，4、已发货，5、交易成功，6、交易关闭,7、待评价
                tbOrder.setCreateTime(new Date());                            //创建时间
                tbOrder.setUpdateTime(new Date());                            //更新时间
                tbOrder.setUserId(order.getUserId());                        //用户ID
                tbOrder.setReceiverAreaName(order.getReceiverAreaName());    //收货地址
                tbOrder.setReceiverMobile(order.getReceiverMobile());        //收货电话
                tbOrder.setReceiver(order.getReceiver());                    //收货人
                tbOrder.setSourceType(order.getSourceType());                //订单来源
                tbOrder.setSellerId(cart.getSellerId());                    //商家ID
                double payment = 0.00;
                //4.保存订单详情信息
                for (TbOrderItem orderItem : cart.getOrderItemList()) {
                    orderItem.setId(idWorker.nextId());                     //订单详情ID
                    orderItem.setOrderId(orderId);                           //订单ID
                    payment += orderItem.getTotalFee().doubleValue();         //应付金额
                    orderItemMapper.insert(orderItem);
                }
                tbOrder.setPayment(new BigDecimal(payment));            //订单应付金额
                total_fee_double += tbOrder.getPayment().doubleValue();   //支付日志中的支付总金额（元）
                orderList.add(tbOrder.getOrderId() + "");                  //订单编号集合
                orderMapper.insert(tbOrder);
            }

            //添加支付日志   //线上支付
            if (order.getPaymentType().equals("1")) {

                TbPayLog tbPayLog = new TbPayLog();
                long outTradeNo = idWorker.nextId();
                tbPayLog.setOutTradeNo(outTradeNo + "");                              //支付订单编号
                tbPayLog.setPayType(order.getPaymentType());                        //支付方式
                tbPayLog.setTradeState("0");                                        //交易状态 未支付
                tbPayLog.setUserId(order.getUserId());                              //交易用户
                tbPayLog.setCreateTime(new Date());                                 //交易生成时间

                //元转分
                BigDecimal total_fee_big = new BigDecimal(total_fee_double);
                BigDecimal chs = new BigDecimal(100);
                BigDecimal total_fee = total_fee_big.multiply(chs);
                tbPayLog.setTotalFee(total_fee.longValue());                        //支付总金额（分）

                String orderIds = orderList.toString().replace("[", "").replace("]", "").replace(" ", "");

                tbPayLog.setOrderList(orderIds);
                //支付日志保存到数据库
                payLogMapper.insert(tbPayLog);
                //暂存到缓存中
                redisTemplate.boundHashOps("payLog").put(order.getUserId(), tbPayLog);
            }


            //5.清空缓存中的购物车数据
            redisTemplate.boundHashOps("cartList").delete(order.getUserId());
        }

    }


    /**
     * 修改
     */
    @Override
    public void update(TbOrder order) {
        orderMapper.updateByPrimaryKey(order);
    }

    /**
     * 根据ID获取实体
     *
     * @param id
     * @return
     */
    @Override
    public TbOrder findOne(Long orderId) {
        return orderMapper.selectByPrimaryKey(orderId);
    }

    /**
     * 批量删除
     */
    @Override
    public void delete(Long[] orderIds) {
        for (Long orderId : orderIds) {
            orderMapper.deleteByPrimaryKey(orderId);
        }
    }


    @Override
    public PageResult findPage(TbOrder order, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        TbOrderExample example = new TbOrderExample();
        Criteria criteria = example.createCriteria();

        if (order != null) {
            if (order.getPaymentType() != null && order.getPaymentType().length() > 0) {
                criteria.andPaymentTypeLike("%" + order.getPaymentType() + "%");
            }
            if (order.getPostFee() != null && order.getPostFee().length() > 0) {
                criteria.andPostFeeLike("%" + order.getPostFee() + "%");
            }
            if (order.getStatus() != null && order.getStatus().length() > 0) {
                criteria.andStatusLike("%" + order.getStatus() + "%");
            }
            if (order.getShippingName() != null && order.getShippingName().length() > 0) {
                criteria.andShippingNameLike("%" + order.getShippingName() + "%");
            }
            if (order.getShippingCode() != null && order.getShippingCode().length() > 0) {
                criteria.andShippingCodeLike("%" + order.getShippingCode() + "%");
            }
            if (order.getUserId() != null && order.getUserId().length() > 0) {
                criteria.andUserIdLike("%" + order.getUserId() + "%");
            }
            if (order.getBuyerMessage() != null && order.getBuyerMessage().length() > 0) {
                criteria.andBuyerMessageLike("%" + order.getBuyerMessage() + "%");
            }
            if (order.getBuyerNick() != null && order.getBuyerNick().length() > 0) {
                criteria.andBuyerNickLike("%" + order.getBuyerNick() + "%");
            }
            if (order.getBuyerRate() != null && order.getBuyerRate().length() > 0) {
                criteria.andBuyerRateLike("%" + order.getBuyerRate() + "%");
            }
            if (order.getReceiverAreaName() != null && order.getReceiverAreaName().length() > 0) {
                criteria.andReceiverAreaNameLike("%" + order.getReceiverAreaName() + "%");
            }
            if (order.getReceiverMobile() != null && order.getReceiverMobile().length() > 0) {
                criteria.andReceiverMobileLike("%" + order.getReceiverMobile() + "%");
            }
            if (order.getReceiverZipCode() != null && order.getReceiverZipCode().length() > 0) {
                criteria.andReceiverZipCodeLike("%" + order.getReceiverZipCode() + "%");
            }
            if (order.getReceiver() != null && order.getReceiver().length() > 0) {
                criteria.andReceiverLike("%" + order.getReceiver() + "%");
            }
            if (order.getInvoiceType() != null && order.getInvoiceType().length() > 0) {
                criteria.andInvoiceTypeLike("%" + order.getInvoiceType() + "%");
            }
            if (order.getSourceType() != null && order.getSourceType().length() > 0) {
                criteria.andSourceTypeLike("%" + order.getSourceType() + "%");
            }
            if (order.getSellerId() != null && order.getSellerId().length() > 0) {
                criteria.andSellerIdLike("%" + order.getSellerId() + "%");
            }
        }

        Page<TbOrder> page = (Page<TbOrder>) orderMapper.selectByExample(example);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 从缓存中读取支付日志对象
     *
     * @param userId
     * @return
     */
    public TbPayLog searchPayLogFromRedis(String userId) {

        return (TbPayLog) redisTemplate.boundHashOps("payLog").get(userId);
    }

    /**
     * 支付成功后修改订单状态
     *
     * @param out_trade_no   交易订单编号
     * @param transaction_id 支付宝平台返回的交易流水号
     */
    public void updateOrderStatus(String out_trade_no, String transaction_id) {
        //1.修改支付日志支付状态
        TbPayLog tbPayLog = payLogMapper.selectByPrimaryKey(out_trade_no);
        tbPayLog.setTradeState("1");   //已支付
        tbPayLog.setPayTime(new Date());  //支付时间
        tbPayLog.setTransactionId(transaction_id);   //交易流水号
        payLogMapper.updateByPrimaryKey(tbPayLog);
        //  111,222,333
        String orderIds = tbPayLog.getOrderList();
        String[] ids = orderIds.split(",");
        //2.遍历修改订单的付款状态
        for (String orderId : ids) {
            TbOrder tbOrder = orderMapper.selectByPrimaryKey(Long.parseLong(orderId));
            tbOrder.setStatus("2");   //已付款
            orderMapper.updateByPrimaryKey(tbOrder);
        }
        //3.清空缓存中支付日志
        redisTemplate.boundHashOps("payLog").delete(tbPayLog.getUserId());
    }

}
