package org.springframework.samples.architecture.fixtures.features.a;

import org.springframework.samples.architecture.fixtures.features.b.FeatureB;

/**
 * Depends on another feature.
 */
class FeatureA {

	FeatureB other;

}
