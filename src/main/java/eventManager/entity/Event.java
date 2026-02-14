package eventManager.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name="\"EVENT\"")
public class Event implements Serializable {

    private static final long serialVersionUID = 1L;

	/** Primary key */
    @Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "event_id_seq")
    @SequenceGenerator(name = "event_id_seq", sequenceName = "\"EVENT_EVENT_ID_seq\"", allocationSize = 1)
    @Column(name="\"EVENT_ID\"")
    private Integer eventId;

    @Column(name="\"EVENT_CODE\"", nullable=false, length=6)
    private String eventCode;
    @Column(name="\"EVENT_NAME\"", nullable=false, length=100)
    private String eventName;
    @Column(name="\"DESCRIPTION\"", nullable=true, length=250)
    private String description;
    @Column(name="\"PLACE\"", nullable=false, length=150)
    private String place;
    @Column(name="\"DATE\"", nullable=false)
    private LocalDateTime date;
    @Column(name="\"STATUS\"", nullable=false, length=20)
    private String status;
    
}
