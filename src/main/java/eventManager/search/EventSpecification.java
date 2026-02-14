package eventManager.search;

import eventManager.constant.Constantes;
import eventManager.entity.Event;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaBuilder.In;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class EventSpecification implements Specification<Event>{

	private static final long serialVersionUID = 1L;
	private SearchCriteria criteria;
	
	/**
	 * Convierte un String a LocalDateTime.
	 * Soporta formatos: "yyyy-MM-ddTHH:mm:ss" o "yyyy-MM-dd"
	 */
	private LocalDateTime parseToLocalDateTime(String dateString) {
		try {
			// Si contiene 'T', es un formato completo con hora
			if (dateString.contains("T")) {
				return LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
			} else {
				// Si es solo fecha, agregar hora 00:00:00
				return LocalDateTime.parse(dateString + "T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Formato de fecha inválido: " + dateString, e);
		}
	}
	
	@Override
	public Predicate toPredicate(Root<Event> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		
		String value;
		switch (criteria.getOperation()) {
			
			case Constantes.LIKE:
					value = criteria.getValue().toString().substring(1, criteria.getValue().toString().length()-1);
					value="%"+value.toLowerCase()+"%";
					return builder.like(builder.lower(root.get(criteria.getKey())), value);
			
			case Constantes.EQUAL:
				if (criteria.getValue() instanceof String && criteria.getValue().toString().charAt(0) == '(' && criteria.getValue().toString().charAt(criteria.getValue().toString().length()-1) == ')') {
					//IN
					value = criteria.getValue().toString().substring(1, criteria.getValue().toString().length()-1);
					In<Integer> in = builder.in(root.get(criteria.getKey()));
					Arrays.asList(value.split(";")).forEach(v -> in.value(Integer.parseInt(v)));
					return in;
				} else {
					return builder.equal(root.get(criteria.getKey()), criteria.getValue());
				}
						
			case Constantes.GREATER_THAN:
				// Convertir el valor String a LocalDateTime
				LocalDateTime dateTimeGT = parseToLocalDateTime(criteria.getValue().toString());
				return builder.greaterThan(root.get(criteria.getKey()), dateTimeGT);
		
			case Constantes.GREATER_EQUAL:
				// Convertir el valor String a LocalDateTime
				LocalDateTime dateTimeGE = parseToLocalDateTime(criteria.getValue().toString());
				return builder.greaterThanOrEqualTo(root.get(criteria.getKey()), dateTimeGE);
				
			case Constantes.LESS_THAN:
				// Convertir el valor String a LocalDateTime
				LocalDateTime dateTimeLT = parseToLocalDateTime(criteria.getValue().toString());
				return builder.lessThan(root.get(criteria.getKey()), dateTimeLT);
			
			case Constantes.LESS_EQUAL:
				// Convertir el valor String a LocalDateTime
				LocalDateTime dateTimeLE = parseToLocalDateTime(criteria.getValue().toString());
				return builder.lessThanOrEqualTo(root.get(criteria.getKey()), dateTimeLE);
			
		}
		
        return null;
		
	}

}
