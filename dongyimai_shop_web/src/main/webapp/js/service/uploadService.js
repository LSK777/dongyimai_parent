app.service('uploadService',function ($http) {

    this.uploadFile = function () {
        //1.获取表单的上传控件的数据
        var formData = new FormData();
        formData.append("file",file.files[0]);
        //2.对上传请求数据配置
        return $http({
            'method':'POST',
            'url':'../upload.do',
            'data': formData,
            'headers':{'Content-Type':undefined},   //默认上传请求数据格式都是JSON,采用数据流的方式
            'transformRequest':angular.identity      //使用angularJS对上传的数据进行序列化
        })
    }
})











