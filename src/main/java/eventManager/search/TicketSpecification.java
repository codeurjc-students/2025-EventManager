package eventManager.search;

import eventManager.constant.Constantes;
import eventManager.entity.Ticket;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@NoArgsConstructor
@AllArgsConstructor
public class TicketSpecification implements Specification<Ticket>{

	private static final long serialVersionUID = 1L;
	private SearchCriteria criteria;
	
	@Override
	public Predicate toPredicate(Root<Ticket> root, @Nullable CriteriaQuery<?> query, CriteriaBuilder builder) {
		
		switch (criteria.getOperation()) {
			case Constantes.EQUAL:
				if (criteria.getKey().contains(".")) {
					String[] keys = criteria.getKey().split("\\.");
					return builder.equal(root.get(keys[0]).get(keys[1]), criteria.getValue());
				} else if (criteria.getValue() == null || "null".equalsIgnoreCase(criteria.getValue().toString())) {
					// Búsqueda de valores NULL (para campos booleanos que no sean ni true ni false)
					return builder.isNull(root.get(criteria.getKey()));
				} else {
					if(criteria.getKey().contains("Confirmation")){
						// Convertir el valor a tipo Boolean
						Object convertedValue = convertValue(criteria.getValue());
						return builder.equal(root.get(criteria.getKey()), convertedValue);
					} else {
						return builder.equal(root.get(criteria.getKey()), criteria.getValue());
					}
				}
		}
		throw new IllegalStateException("Operación de búsqueda no soportada: " + criteria.getOperation());
		
	}
	
	private Object convertValue(Object value) {
		String valueStr = value.toString();
		if ("true".equalsIgnoreCase(valueStr)) {
			return Boolean.TRUE;
		} else if ("false".equalsIgnoreCase(valueStr)) {
			return Boolean.FALSE;
		} else {
			return null;
		}
	}

}
