package eventManager.repository;

import eventManager.entity.Ticket;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TicketRepository extends JpaRepository<Ticket,Integer>, JpaSpecificationExecutor<Ticket> {

    boolean existsByEventId_EventIdAndUserId_UserId(Integer eventId, Integer userId);

    boolean existsByEventId_EventIdAndUserId_UserIdAndRole(Integer eventId, Integer userId, String role);

    List<Ticket> findByUserId_UserIdAndRole(Integer userId, String role);

    Optional<Ticket> findByEventId_EventIdAndUserId_UserId(Integer eventId, Integer userId);

}