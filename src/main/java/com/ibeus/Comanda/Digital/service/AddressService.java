package com.ibeus.Comanda.Digital.service;


import com.ibeus.Comanda.Digital.model.Address;
import com.ibeus.Comanda.Digital.repository.AddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Service
public class AddressService {

    private final String VIA_CEP_URL = "https://viacep.com.br/ws/{cep}/json/";

    @Autowired
    private AddressRepository addressRepository;

    public Address buscarPorCep(String cep) {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> response = restTemplate.getForObject(VIA_CEP_URL, Map.class, cep);

        if (response == null || response.containsKey("erro")) {
            throw new RuntimeException("CEP não encontrado ou inválido: " + cep);
        }

        Address address = new Address();
        address.setCep(cep);
        address.setLogradouro((String) response.get("logradouro"));
        address.setBairro((String) response.get("bairro"));
        address.setLocalidade((String) response.get("localidade"));
        address.setUf((String) response.get("uf"));
        return address;
    }

    //  Salva ou atualiza o endereço (só existe um)
    public Address salvarOuAtualizarPorCep(String cep) {
        Address novoEndereco = buscarPorCep(cep);

        Optional<Address> existente = addressRepository.findAll().stream().findFirst();

        if (existente.isPresent()) {
            Address address = existente.get();
            address.setCep(novoEndereco.getCep());
            address.setLogradouro(novoEndereco.getLogradouro());
            address.setBairro(novoEndereco.getBairro());
            address.setLocalidade(novoEndereco.getLocalidade());
            address.setUf(novoEndereco.getUf());
            return addressRepository.save(address);
        } else {
            return addressRepository.save(novoEndereco);
        }
    }

    public Address getAddress() {
        return addressRepository.findAll().stream().findFirst().orElse(null);
    }
}