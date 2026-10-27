package org.springframework.samples.architecture.fixtures.cycles.a;

import org.springframework.samples.architecture.fixtures.cycles.CycleRoot;

/**
 * Part of a root-to-feature cycle with {@link CycleRoot}.
 */
public class CycleFeature {

	CycleRoot root;

}
