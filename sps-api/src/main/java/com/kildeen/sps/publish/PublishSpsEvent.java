package com.kildeen.sps.publish;

import com.kildeen.sps.SpsEvent;

import java.util.List;
import java.util.Map;

public interface PublishSpsEvent extends SpsEvent {

    String type();

    default List<String> types() {
        return List.of(type());
    }

    Map<String, Object> data();

    default String parentId() {
        return null;
    }

}
