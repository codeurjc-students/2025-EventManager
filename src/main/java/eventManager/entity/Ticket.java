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
@Table(name="\"TICKET\"")
public class Ticket implements Serializable {

    private static final long serialVersionUID = 1L;

	/** Primary key */
    @Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ticket_id_seq")
    @SequenceGenerator(name = "ticket_id_seq", sequenceName = "\"TIKCET_TICKET_ID_seq\"", allocationSize = 1)
    @Column(name="\"TICKET_ID\"")
    private Integer ticketId;

    /** Foreign keys **/
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="\"EVENT_ID\"", nullable=false)
    private Event eventId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="\"USER_ID\"", nullable=false)
    private User userId;

    @Column(name="\"ROLE\"", nullable=false, length=5)
    private String role;
    @Column(name="\"GUEST_NUMBER\"", nullable=false)
    private Integer guestNumber;
    @Column(name="\"INVITATION_CONFIRMATION\"", nullable=true)
    private Boolean invitationConfirmation;
    @Column(name="\"ASSIST_CONFIRMATION\"", nullable=true)
    private Boolean assistConfirmation;
    @Column(name="\"NOTES\"", nullable=true, length=500)
    private String notes;

}
