app.controller('cartController',function ($scope,cartService){

    $scope.findCartList = function () {
        cartService.findCartList().success(
            function (response) {
                $scope.cartList = response;
                //购物车列表更新,总数量也更新
                 $scope.totalValue = cartService.sum($scope.cartList);
        })
    }

    $scope.addGoodsToCartList = function (itemId,num) {
        cartService.addGoodsToCartList(itemId,num).success(
            function (response) {
            if (response.success){
                //刷新列表
                $scope.findCartList();
            }else{
                alert(response.message);
            }

        })
    }

    $scope.findListByUserId = function () {
        cartService.findListByUserId().success(
            function (response) {
            $scope.addressList = response;
            //遍历地址列表,设置默认选中
                for (var i=0;i<$scope.addressList.length;i++){
                    if ($scope.addressList[i].isDefault=='1'){
                        $scope.address = $scope.addressList[i];
                        break;
                    }
                }
        })
    }

    //选中地址
    $scope.selectAddress = function (address) {
        $scope.address = address;
    }

    //判断是否选中
    $scope.isSelect = function (address) {
        if ($scope.address==address){
            return true;
        }else{
            return false;
        }
    }

    //初始化订单的数据结构
    $scope.order = {'paymentType':'1'};
    //设置支付方式
    $scope.setPaymentType = function (type) {
        $scope.order.paymentType = type;
    }

    //提交订单
    $scope.submitOrder = function () {
        $scope.order.receiverAreaName = $scope.address.address; //收货地址
        $scope.order.receiverMobile = $scope.address.mobile;   //收货电话
        $scope.order.receiver = $scope.address.contact;        //收获人

        cartService.submitOrder($scope.order).success(
            function (response) {
            if (response.success){
                if ($scope.order.paymentType=='1'){   //判断支付方式,线上支付跳转到支付页面
                    location.href="pay.html";
                }else{
                    alert(response.message);
                }
            }
        })
    }

})






















