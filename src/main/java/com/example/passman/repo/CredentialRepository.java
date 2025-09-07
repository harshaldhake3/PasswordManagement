
package com.example.passman.repo;

import com.example.passman.model.Credential;
import com.example.passman.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CredentialRepository extends JpaRepository<Credential, Long> {
    List<Credential> findByOwner(AppUser owner);
}
