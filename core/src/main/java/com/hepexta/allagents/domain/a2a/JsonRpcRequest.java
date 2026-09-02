package com.hepexta.allagents.domain.a2a;

import java.util.Map;

public record JsonRpcRequest(String jsonrpc, Object id, String method, Map<String, Object> params) {
}
