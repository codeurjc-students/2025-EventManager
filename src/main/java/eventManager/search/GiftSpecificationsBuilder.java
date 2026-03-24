package eventManager.search;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import eventManager.entity.Gift;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class GiftSpecificationsBuilder {

    public Specification<Gift> build(List<SearchCriteria> params) {
        if (params.isEmpty()) {
            return (root, query, builder) -> builder.conjunction();
        }

        List<Specification<Gift>> specs = params.stream()
          .map(GiftSpecification::new)
          .collect(Collectors.toList());
        
        Specification<Gift> result = specs.get(0);

        for (int i = 1; i < params.size(); i++) {
        	result = Specification.where(result).and(specs.get(i));
        }       
        return result;
    }
	
}
