package com.ibeus.Comanda.Digital.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ibeus.Comanda.Digital.model.Client;


public interface ClientRepository extends JpaRepository<Client, Long> {
}
