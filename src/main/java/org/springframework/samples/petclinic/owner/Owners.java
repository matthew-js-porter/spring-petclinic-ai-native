package org.springframework.samples.petclinic.owner;

import java.util.List;

/**
 * JSON body of {@code GET /owners}; follows the {@code vetList} wrapper of
 * {@code GET /vets}.
 *
 * @param ownerList the owners, in id order
 * @author Matthew Porter
 */
public record Owners(List<OwnerView> ownerList) {
}
