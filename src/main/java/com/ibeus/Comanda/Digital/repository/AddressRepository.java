package com.ibeus.Comanda.Digital.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ibeus.Comanda.Digital.model.Address;


public interface AddressRepository extends JpaRepository<Address, Long> {
}
