app.service('cartService',function ($http) {
    this.findCartList = function () {
        return $http.get('../cart/findCartList.do')
    }

    this.addGoodsToCartList = function (itemId, num) {
        return $http.get('../cart/addGoodsToCartList.do?itemId='+itemId+'&num='+num);
    }

    this.sum = function (cartList) {
        var totalValue = {'totalNum':0,'totalMoney':0.00};
        for (var i=0;i<cartList.length;i++){
            for (var j=0;j<cartList[i].orderItemList.length;j++){
                var orderItem = cartList[i].orderItemList[j];
                totalValue.totalNum += orderItem.num;   //总购买数
                totalValue.totalMoney += orderItem.totalFee;  //总金额
            }
        }
        return totalValue;
    }

    //查询地址列表
    this.findListByUserId = function () {
        return $http.get('../address/findListByUserId.do');
    }

    //保存订单
    this.submitOrder = function (entity) {
        return $http.post('../order/add.do',entity);
    }
})




















