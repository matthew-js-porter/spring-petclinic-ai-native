package org.springframework.samples.architecture.fixtures.bpp.safeparameters;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;

/**
 * An infrastructure bean, safe to inject into a post-processor.
 */
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
class InfrastructureBean {

}
