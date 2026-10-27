package org.springframework.samples.architecture.fixtures.cycles;

import org.springframework.samples.architecture.fixtures.cycles.a.CycleFeature;

/**
 * Part of a root-to-feature cycle with {@link CycleFeature}.
 */
public class CycleRoot {

	CycleFeature feature;

}
