package com.dhu.ycl.filter;

import com.dhu.ycl.grace.result.GraceJSONResult;
import com.dhu.ycl.grace.result.ResponseStatusEnum;
import com.google.gson.Gson;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

public class ErrorRenderUtils {

    // 重新包装并且返回错误信息
    public static Mono<Void> renderErrorMsg(ServerWebExchange exchange, ResponseStatusEnum statusEnum) {
        // 1. 获得相应response，设置header为json格式、设置状态码为500(不太合适：尽量不要和浏览器状态码冲突)
        ServerHttpResponse response = exchange.getResponse();
        if (!response.getHeaders().containsKey("Content-Type")) {
            response.getHeaders().add("Content-Type", MimeTypeUtils.APPLICATION_JSON_VALUE);
        }
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);

        // 2. 构建jsonResult，转为String格式，写入response
        GraceJSONResult jsonResult = GraceJSONResult.exception(statusEnum);
        String resultJson = new Gson().toJson(jsonResult);
        DataBuffer buffer = response.bufferFactory().wrap(resultJson.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
