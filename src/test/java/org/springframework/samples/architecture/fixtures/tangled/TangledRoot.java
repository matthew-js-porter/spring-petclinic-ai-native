package org.springframework.samples.architecture.fixtures.tangled;

import org.springframework.samples.architecture.fixtures.tangled.a.TangledFeature;

/**
 * Part of a root-to-feature cycle with {@link TangledFeature}.
 */
public class TangledRoot {

	TangledFeature feature;

}
