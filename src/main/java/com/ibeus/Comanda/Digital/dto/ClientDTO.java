        package com.ibeus.Comanda.Digital.dto;

import lombok.Data;
import com.ibeus.Comanda.Digital.model.Client;

@Data
public class ClientDTO {

    private Long id;
    private String cpf;
    private String name;
    private String midName;
    private AddressDTO address; // O Address também deve ser DTO neste nível
    private int addressNumber;
    private String complement;

    /**
     * 🟢 CORREÇÃO: Converte a Entity 'Client' para o DTO 'ClientDTO' (USADO NO GET e POST de retorno)
     * - Este método é 'static', então é chamado diretamente na classe (ClientDTO.fromModel).
     */
    public static ClientDTO fromModel(Client client) {
        if (client == null) {
            return null;
        }

        ClientDTO dto = new ClientDTO();
        dto.setId(client.getId());
        dto.setCpf(client.getCpf());
        dto.setName(client.getName());
        dto.setMidName(client.getMidName());
        dto.setAddressNumber(client.getAddressNumber());
        dto.setComplement(client.getComplement());

        // Converte a Entity Address para o DTO AddressDTO
        // Note que o AddressDTO também precisa ter o seu próprio método fromModel(Address)
        if (client.getAddress() != null) {
            dto.setAddress(AddressDTO.fromModel(client.getAddress()));
        } else {
            dto.setAddress(null);
        }

        return dto;
    }

    /**
     * Converte o DTO 'ClientDTO' para a Entity 'Client' (USADO NA ENTRADA/POST)
     */
    public Client toModel() {
        Client client = new Client();

        // Se o CPF vier no DTO, ele será usado para identificar (se não for gerado)
        client.setId(this.id);
        client.setCpf(this.cpf);
        client.setName(this.name);
        client.setMidName(this.midName);
        client.setAddressNumber(this.addressNumber);
        client.setComplement(this.getComplement());

        // Converte o DTO Address para a Entity Address
        client.setAddress(this.address == null ? null : this.address.toModel());


        return client;
    }
}