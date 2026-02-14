package eventManager.repository;

import eventManager.entity.Gift;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GiftRepository extends JpaRepository<Gift,Integer>, JpaSpecificationExecutor<Gift>{

    boolean existsByNameAndEvent_EventId(String name, Integer eventId);

    Optional<Gift> findByGiftId(Integer giftId);

}