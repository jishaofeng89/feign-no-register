package com.a360inhands.feign.api;

import com.a360inhands.feign.annotation.FeignApi;
import feign.Headers;
import feign.RequestLine;

@FeignApi(serviceUrl = "http://api.360inhands.com:8080")
public interface IFileService {

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @RequestLine("GET /qiniu_token/file/list")
    public Object get();
}
