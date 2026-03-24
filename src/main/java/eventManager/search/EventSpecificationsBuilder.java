package eventManager.search;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import eventManager.entity.Event;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EventSpecificationsBuilder {

    public Specification<Event> build(List<SearchCriteria> params) {
        if (params.isEmpty()) {
            return (root, query, builder) -> builder.conjunction();
        }

        List<Specification<Event>> specs = params.stream()
          .map(EventSpecification::new)
          .collect(Collectors.toList());
        
        Specification<Event> result = specs.get(0);

        for (int i = 1; i < params.size(); i++) {
        	result = Specification.where(result).and(specs.get(i));
        }       
        return result;
    }
	
}
