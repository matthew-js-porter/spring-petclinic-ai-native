package org.springframework.samples.architecture.fixtures.features.independent;

import org.springframework.samples.architecture.fixtures.features.independent.a.IndependentA;
import org.springframework.samples.architecture.fixtures.features.independent.b.IndependentB;

/**
 * The root package, which may wire the features together.
 */
class IndependentRoot {

	IndependentA a;

	IndependentB b;

}
