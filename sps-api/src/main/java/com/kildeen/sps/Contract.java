package com.kildeen.sps;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Classes annotated with this are the core API of SPS. Interacting with SPS should go through Contracts
 */
@Retention(RetentionPolicy.SOURCE)
public @interface Contract {
}
