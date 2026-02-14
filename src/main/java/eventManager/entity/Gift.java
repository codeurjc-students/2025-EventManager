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
@Table(name="\"GIFT\"")
public class Gift implements Serializable {

    private static final long serialVersionUID = 1L;

	/** Primary key */
    @Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gift_id_seq")
    @SequenceGenerator(name = "gift_id_seq", sequenceName = "\"GIFT_GIFT_ID_seq\"", allocationSize = 1)
    @Column(name="\"GIFT_ID\"")
    private Integer giftId;

    /** Foreign keys **/
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "\"EVENT_ID\"", nullable = false)
    private Event event;

    @Column(name="\"NAME\"", nullable=false, length=100)
    private String name;
    @Column(name="\"PRICE\"", nullable=false)
    private Double price;
    @Column(name="\"DETAILS\"", nullable=true, length=500)
    private String details;
    @Column(name="\"URL\"", nullable=true, length=500)
    private String url;
    @Column(name="\"IMAGE\"", nullable=true, length=500)
    private String image;
    @Column(name="\"COLLECTED\"", nullable=false)
    private Double collected;
    @Column(name="\"CREATION_USER\"", nullable=false, length=25)
    private String creationUser;
    @Column(name="\"CREATED_BY_HOST\"", nullable=false)
    private Boolean createdByHost;
    @Column(name="\"PAID_IN_FULL\"", nullable=false)
    private Boolean paidInFull;

}
