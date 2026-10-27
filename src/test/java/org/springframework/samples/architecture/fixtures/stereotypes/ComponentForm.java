package org.springframework.samples.architecture.fixtures.stereotypes;

import org.springframework.stereotype.Component;

/**
 * A form registered as a bean.
 */
@Component
record ComponentForm(String name) {

}
