package com.example.passman.service;

import com.example.passman.model.Credential;
import com.example.passman.repository.CredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CredentialService {

    @Autowired
    private CredentialRepository credentialRepository;

    // Save a new credential
    public Credential saveCredential(Credential credential) {
        return credentialRepository.save(credential);
    }

    // Get all credentials
    public List<Credential> getAllCredentials() {
        return credentialRepository.findAll();
    }

    // Get a credential by ID
    public Optional<Credential> getCredentialById(Long id) {
        return credentialRepository.findById(id);
    }

    // Update an existing credential
    public Credential updateCredential(Long id, Credential updatedCredential) {
        return credentialRepository.findById(id)
                .map(existing -> {
                    existing.setUsername(updatedCredential.getUsername());
                    existing.setPassword(updatedCredential.getPassword());
                    existing.setDescription(updatedCredential.getDescription());
                    return credentialRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Credential not found with id " + id));
    }

    // Delete a credential
    public void deleteCredential(Long id) {
        credentialRepository.deleteById(id);
    }
}
