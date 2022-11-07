package com.kildeen.sps.schemagen;


import com.kildeen.sps.SpsEvent;

import java.util.Set;

public record AddSchemaRequest(SpsEvent event, String description, Set<String> tags) {

}