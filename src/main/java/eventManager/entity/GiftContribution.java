package eventManager.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name="\"GIFT_CONTRIBUTION\"")
public class GiftContribution implements Serializable {

    private static final long serialVersionUID = 1L;

	/** Primary key */
    @Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gift_contribution_id_seq")
    @SequenceGenerator(name = "gift_contribution_id_seq", sequenceName = "\"GIFT_CONTRIBUTION_ID_seq\"", allocationSize = 1)
    @Column(name="\"GIFT_CONTRIBUTION_ID\"")
    private Integer giftContributionId;

    /** Foreign keys **/
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "\"GIFT_ID\"", nullable = false)
    private Gift giftId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="\"USER_ID\"", nullable=false)
    private User userId;

    @Column(name="\"CONTRIBUTION\"", nullable=false)
    private Double contribution;

}
