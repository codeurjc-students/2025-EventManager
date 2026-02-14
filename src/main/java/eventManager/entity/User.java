package eventManager.entity;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="\"USER\"")
public class User implements UserDetails {

    /** Primary key */
    @Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_id_seq")
    @SequenceGenerator(name = "user_id_seq", sequenceName = "\"USER_USER_ID_seq\"", allocationSize = 1)
    @Column(name="\"USER_ID\"")
    private Integer userId;

    @Column(name="\"EMAIL\"", nullable=false, length=50)
    String email;
    @Column(name="\"USERNAME\"", nullable=false, length=25)
    String username;
    @JsonIgnore
    @Column(name="\"PASSWORD\"", nullable=false, length=100)
    String password;
    @Column(name="\"USER_ROLE\"", nullable=false)
    @Enumerated(EnumType.STRING)
    UserRole role;
    @Column(name="\"FIRST_NAME\"", nullable=false, length=20)
    String firstName;
    @Column(name="\"LAST_NAME\"", nullable=false, length=50)
    String lastName;
    @Column(name="\"PHONE_NUMBER\"", nullable=false, length=15)
    String phoneNumber;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority((role.name())));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
    
}