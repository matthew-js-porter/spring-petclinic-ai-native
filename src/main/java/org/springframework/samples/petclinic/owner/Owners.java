package org.springframework.samples.petclinic.owner;

import java.util.List;

/**
 * JSON body of {@code GET /owners}; follows the {@code vetList} wrapper of {@code GET /vets}.
 */
public record Owners(List<OwnerView> ownerList) {
}
