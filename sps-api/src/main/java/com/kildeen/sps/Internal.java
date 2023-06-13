package com.kildeen.sps;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Classes annotated with this are the part of SPS internals. Interacting with SPS should go through Contracts.
 * This class is safe to use per se, but can change completely between SPS versions.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface Internal {
}
