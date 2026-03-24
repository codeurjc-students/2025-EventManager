package eventManager.search;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import eventManager.entity.Ticket;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TicketSpecificationsBuilder {

    public Specification<Ticket> build(List<SearchCriteria> params) {
        if (params.isEmpty()) {
            return (root, query, builder) -> builder.conjunction();
        }

        List<Specification<Ticket>> specs = params.stream()
          .map(TicketSpecification::new)
          .collect(Collectors.toList());
        
        Specification<Ticket> result = specs.get(0);

        for (int i = 1; i < params.size(); i++) {
        	result = Specification.where(result).and(specs.get(i));
        }       
        return result;
    }
	
}
