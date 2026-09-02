package com.hepexta.allagents.domain.a2a;

public record JsonRpcResponse(String jsonrpc, Object id, Object result, Error error) {

    public record Error(int code, String message) {
    }

    public static JsonRpcResponse ok(Object id, Object result) {
        return new JsonRpcResponse("2.0", id, result, null);
    }

    public static JsonRpcResponse error(Object id, int code, String message) {
        return new JsonRpcResponse("2.0", id, null, new Error(code, message));
    }
}
