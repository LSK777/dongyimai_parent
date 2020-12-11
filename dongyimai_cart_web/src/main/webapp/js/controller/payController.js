app.controller('payController',function ($scope,$location,payService) {

    $scope.createNative = function () {
        payService.createNative().success(
            function (response) {
            $scope.outTradeNo = response.out_trade_no;  //订单号
                $scope.totalFee = (response.total_fee / 100).toFixed(2);
                var qr = new QRious({
                    'element': document.getElementById("erweima"),    //读取表单控件
                    'size': 250,                                        //设置二维码尺寸
                    'level': 'H',                                   //设置容错级别
                    'value': response.qrCode                //二维码内容
                })
                queryPayStatus(response.out_trade_no);
        })
    }

    queryPayStatus = function (out_trade_no) {
        payService.queryPayStatus(out_trade_no).success(
            function (response) {
            if (response.success){
                location.href = "paysuccess.html#?money="+$scope.totalFee;
            }else{
                if (response.message=='二维码超时'){
                    document.getElementById("timeout").innerHTML = '二维码已过期，刷新页面重新获取二维码。';
                }else{
                    location.href = "payfail.html";
                }

            }
        })
    }

    $scope.getMoney = function () {
        return $location.search()['money'];
    }

})
















