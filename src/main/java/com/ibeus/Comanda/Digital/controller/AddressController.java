package com.ibeus.Comanda.Digital.controller;


import com.ibeus.Comanda.Digital.model.Address;
import com.ibeus.Comanda.Digital.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/address")
@CrossOrigin(origins = "http://localhost:4200")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @GetMapping
    public Address getAddress(){
        return addressService.getAddress();
    }

    // Endpoint GET: apenas consulta o CEP (sem salvar)
    @GetMapping("/{cep}")
    public Address getAddressByCep(@PathVariable String cep) {
        return addressService.buscarPorCep(cep);
    }

    // Endpoint POST: busca o CEP e salva/atualiza no banco automaticamente
    @PostMapping("/{cep}")
    public Address saveOrUpdateByCep(@PathVariable String cep) {
        return addressService.salvarOuAtualizarPorCep(cep);
    }
}