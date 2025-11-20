package com.ibeus.Comanda.Digital.controller;

import com.ibeus.Comanda.Digital.model.Client;
import com.ibeus.Comanda.Digital.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/client")
@CrossOrigin(origins = "http://localhost:4200")
public class ClientController {

    @Autowired
    private ClientService clientService;

    @GetMapping
    public Client getClient() {
        return clientService.getClient();
    }

    @PostMapping
    public Client saveOrUpdate(@RequestBody Client client) {
        return clientService.saveOrUpdate(client);
    }
}
