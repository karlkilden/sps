package com.kildeen.sps;

import com.kildeen.sps.json.JsonProvider;

import java.util.Set;

public class IdWithReceiptsResponse {
    private final int code;
    private final String json;

    public IdWithReceiptsResponse(Receipt allEvents, Set<Receipt> receipts) {

        this.code = allEvents == Receipt.ACK ? 204 : 200;
        this.json =  JsonProvider.json().write(receipts);
    }

    public int code() {
        return code;
    }

    public String json() {
        return json;
    }

    public boolean hasBody() {
        return code != 204;
    }
}
