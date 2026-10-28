package org.springframework.samples.architecture.fixtures.untangled;

import org.springframework.samples.architecture.fixtures.untangled.a.UntangledFeature;

/**
 * Depends on {@link UntangledFeature}, which does not depend back.
 */
class UntangledRoot {

	UntangledFeature feature;

}
