package eventManager.repository;

import eventManager.entity.GiftContribution;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GiftContributionRepository extends JpaRepository<GiftContribution,Integer>, JpaSpecificationExecutor<GiftContribution>{

    Optional<GiftContribution> findByGiftId_GiftIdAndUserId_UserId(Integer giftId, Integer userId);

    List<GiftContribution> findByGiftId_GiftId(Integer giftId);

    void deleteByGiftId_GiftId(Integer giftId);

}