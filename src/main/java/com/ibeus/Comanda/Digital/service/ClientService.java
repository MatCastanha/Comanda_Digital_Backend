package com.ibeus.Comanda.Digital.service;

import com.ibeus.Comanda.Digital.model.Client;
import com.ibeus.Comanda.Digital.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    public Client getClient() {

        return clientRepository.findAll().stream().findFirst().orElse(null);
    }

    public Client saveOrUpdate(Client clientData) {

        Optional<Client> existing = clientRepository.findAll().stream().findFirst();

        if (existing.isPresent()) {
            Client client = existing.get();
            client.setName(clientData.getName());
            client.setMidName(clientData.getMidName());
            client.setAddress(clientData.getAddress());
            client.setAddressNumber(clientData.getAddressNumber());
            client.setPayment(clientData.getPayment());
            return clientRepository.save(client);
        } else {

            return clientRepository.save(clientData);
        }
    }
}
