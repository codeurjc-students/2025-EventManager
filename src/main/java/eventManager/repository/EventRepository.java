package eventManager.repository;

import eventManager.entity.Event;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EventRepository extends JpaRepository<Event,Integer>, JpaSpecificationExecutor<Event>{

    Optional<Event> findByEventCode(String eventCode);

    boolean existsByEventCode(String eventCode);

}