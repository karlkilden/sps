package com.kildeen.sps.inlet;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A marker annotation. Classes marked with this is the contract level API:s offered by SPS.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface Contract {
}
