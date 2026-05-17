package me.sathish.runs_app.rifl;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.GONE)
public class StaleRpcException extends RuntimeException {
    public StaleRpcException(long clientId) {
        super("STALE_RPC: lease expired or unknown for clientId=" + clientId);
    }
}
