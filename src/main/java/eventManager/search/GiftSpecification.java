package eventManager.search;

import eventManager.constant.Constantes;
import eventManager.entity.Gift;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaBuilder.In;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.Arrays;

@NoArgsConstructor
@AllArgsConstructor
public class GiftSpecification implements Specification<Gift>{

	private static final long serialVersionUID = 1L;
	private SearchCriteria criteria;
	
	@Override
	public Predicate toPredicate(Root<Gift> root, @Nullable CriteriaQuery<?> query, CriteriaBuilder builder) {
		
		String value;
		switch (criteria.getOperation()) {
			
			case Constantes.LIKE:
					value = criteria.getValue().toString().substring(1, criteria.getValue().toString().length()-1);
					value="%"+value.toLowerCase()+"%";
					return builder.like(builder.lower(root.get(criteria.getKey())), value);
			
			case Constantes.EQUAL:
				if (criteria.getKey().contains(".")) {
					String[] keys = criteria.getKey().split("\\.");
					return builder.equal(root.get(keys[0]).get(keys[1]), criteria.getValue());
				} else if (criteria.getValue() == null || "null".equalsIgnoreCase(criteria.getValue().toString())) {
					// Búsqueda de valores NULL
					return builder.isNull(root.get(criteria.getKey()));
				} else if (criteria.getValue() instanceof String && criteria.getValue().toString().charAt(0) == '(' && criteria.getValue().toString().charAt(criteria.getValue().toString().length()-1) == ')') {
					//IN
					value = criteria.getValue().toString().substring(1, criteria.getValue().toString().length()-1);
					In<String> in = builder.in(root.get(criteria.getKey()));
					Arrays.asList(value.split(";")).forEach(in::value);
					return in;
				} else {
					// Manejar campos booleanos (paidInFull, createdByHost)
					if (criteria.getKey().equals("paidInFull") || criteria.getKey().equals("createdByHost")) {
						Object convertedValue = convertValue(criteria.getValue());
						return builder.equal(root.get(criteria.getKey()), convertedValue);
					} else {
						return builder.equal(root.get(criteria.getKey()), criteria.getValue());
					}
				}
						
			case Constantes.GREATER_THAN:
				return builder.greaterThan(root.get(criteria.getKey()), (LocalDateTime) criteria.getValue());
		
			case Constantes.GREATER_EQUAL:
				return builder.greaterThanOrEqualTo(root.get(criteria.getKey()), (LocalDateTime) criteria.getValue());
				
			case Constantes.LESS_THAN:
				return builder.lessThan(root.get(criteria.getKey()), (LocalDateTime) criteria.getValue());
			
			case Constantes.LESS_EQUAL:
				return builder.lessThanOrEqualTo(root.get(criteria.getKey()), (LocalDateTime) criteria.getValue());
			
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
