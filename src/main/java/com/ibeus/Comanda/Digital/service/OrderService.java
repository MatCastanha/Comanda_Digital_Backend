package com.ibeus.Comanda.Digital.service;

import com.ibeus.Comanda.Digital.dto.OrderDTO;
import com.ibeus.Comanda.Digital.dto.OrderItemDTO;
import com.ibeus.Comanda.Digital.dto.OrderItemInputDTO;
import com.ibeus.Comanda.Digital.enums.OrderStatus;
import com.ibeus.Comanda.Digital.model.*;
import com.ibeus.Comanda.Digital.repository.ClientRepository;
import com.ibeus.Comanda.Digital.repository.DishRepository;
import com.ibeus.Comanda.Digital.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository repository;
    private final ClientRepository clientRepository;
    private final DishRepository dishRepository;

    public OrderService(OrderRepository repository,
                        ClientRepository clientRepository,
                        DishRepository dishRepository) {
        this.repository = repository;
        this.clientRepository = clientRepository;
        this.dishRepository = dishRepository;
    }

    // --- MÉTODOS DE BUSCA (Buscam o pedido completo) ---
    @Transactional(readOnly = true)
    public List<OrderDTO> findAll() {
        List<Order> entities = repository.findAll();
        return entities.stream().map(OrderDTO::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDTO findById(Long id) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado: " + id));
        return new OrderDTO(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> findByStatus(OrderStatus status) {
        List<Order> entities = repository.findByStatus(status);
        if (entities.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum pedido encontrado com status: " + status);
        }
        return entities.stream().map(OrderDTO::new).collect(Collectors.toList());
    }

    // --- 🔹 CRIAR PEDIDO (CRIA SOMENTE O RASCUNHO/CARRINHO) ---
    @Transactional
    public OrderDTO create(OrderDTO dto) {

        // 🚨 BUSCA O CLIENTE PADRÃO
        List<Client> allClients = clientRepository.findAll();
        if (allClients.isEmpty()) {
            throw new EntityNotFoundException("O cliente não foi encontrado. Crie o registro inicial do cliente.");
        }
        Client defaultClient = allClients.get(0);

        Order order = new Order();
        order.setClient(defaultClient);
        order.setMoment(Instant.now());
        order.setStatus(OrderStatus.DRAFT); // Status inicial como RASCUNHO

        Order saved = repository.save(order);
        return new OrderDTO(saved);
    }

    // --- 🆕 ADICIONAR ITEM AO RASCUNHO (CARRINHO) ---
    @Transactional
    public OrderItemDTO addItemToOrder(Long orderId, OrderItemInputDTO itemDTO) {
        Order order = repository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado: " + orderId));

        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new IllegalStateException("Só é possível adicionar itens a pedidos no status DRAFT.");
        }

        Dish dish = dishRepository.findById(itemDTO.getDishId())
                .orElseThrow(() -> new EntityNotFoundException("Prato não encontrado: " + itemDTO.getDishId()));

        // Tenta encontrar item existente para atualizar a quantidade
        OrderItem existingItem = order.getItems().stream()
                .filter(item -> item.getDish().getId().equals(dish.getId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            // Atualiza a quantidade se o item já existe
            int newQuantity = existingItem.getQuantity() + itemDTO.getQuantity();
            existingItem.setQuantity(newQuantity);
            repository.save(order);
            return new OrderItemDTO(existingItem);

        } else {
            // Cria novo OrderItem
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setDish(dish);
            item.setQuantity(itemDTO.getQuantity());
            item.setPrice(dish.getPrice());

            order.getItems().add(item);
            Order saved = repository.save(order);

            return new OrderItemDTO(item);
        }
    }

    // --- 🔹 REMOVER OU DIMINUIR ITEM DO RASCUNHO (CARRINHO) 🔄 ---
    @Transactional
    public OrderDTO removeItemFromOrder(Long orderId, OrderItemInputDTO itemDTO) {
        Order order = repository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado: " + orderId));

        // 1. Verifica se o pedido está no status DRAFT
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new IllegalStateException("Só é possível remover/diminuir itens em pedidos no status DRAFT.");
        }

        // 2. Localiza o OrderItem pelo dishId
        OrderItem existingItem = order.getItems().stream()
                .filter(item -> item.getDish().getId().equals(itemDTO.getDishId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Item não encontrado no pedido para o Prato ID: " + itemDTO.getDishId()));

        // 3. Define a quantidade a remover/diminuir
        int quantityToRemove = itemDTO.getQuantity();

        if (quantityToRemove <= 0) {
            throw new IllegalArgumentException("A quantidade a ser removida deve ser positiva.");
        }

        int currentQuantity = existingItem.getQuantity();

        if (quantityToRemove >= currentQuantity) {
            // 4A. Remover o item completamente (Quantidade a remover é maior ou igual à atual)
            order.getItems().remove(existingItem);
            // ⚠️ O JPA/Hibernate só remove o item da tabela secundária se você usar o 'orphanRemoval = true'
            // no mapeamento OneToMany da classe Order. (O seu código já tem isso)

        } else {
            // 4B. Diminuir a quantidade (Quantidade a remover é menor que a atual)
            int newQuantity = currentQuantity - quantityToRemove;
            existingItem.setQuantity(newQuantity);
            // O subtotal será recalculado automaticamente ao salvar
        }

        // 5. Salva o pedido atualizado (com item removido ou quantidade alterada)
        Order saved = repository.save(order);
        return new OrderDTO(saved);
    }


    // --- 🆕 FINALIZAR PEDIDO (Mudar de DRAFT para RECEIVED) ---
    @Transactional
    public OrderDTO finalizeOrder(Long orderId) {
        Order order = repository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado: " + orderId));

        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new IllegalStateException("Apenas pedidos no status DRAFT podem ser finalizados.");
        }

        // Garante que o objeto Client e Address estejam carregados para a captura
        Client client = order.getClient();

        if (client == null || client.getAddress() == null) {
            throw new IllegalStateException("Cliente ou Endereço principal não configurado no pedido.");
        }

        // 💡 1. CAPTURAR E CONGELAR OS DADOS DO CLIENTE E ENDEREÇO (SNAPSHOT)
        order.setClientSnapshotName(client.getName());

        // --- 🎯 CORREÇÃO AQUI: Usando os campos exatos do seu Address (logradouro, localidade, uf) ---
        Address address = client.getAddress();

        // Constrói uma string completa do endereço para o histórico, usando os campos da sua Entidade Address
        String fullAddress = String.format("%s, %s, %s - %s/%s. CEP: %s. Complemento: %s",
                address.getLogradouro(),         // Ex: Rua das Flores
                client.getAddressNumber(),       // Ex: 10
                address.getBairro(),             // Ex: Centro
                address.getLocalidade(),         // Ex: São Paulo
                address.getUf(),                 // Ex: SP
                address.getCep(),                // Ex: 01001-000
                client.getComplement() != null ? client.getComplement() : ""
        );
        order.setAddressSnapshot(fullAddress);
        // -----------------------------------------------------------------------------------------

        // 2. Altera o status e salva.
        order.setStatus(OrderStatus.RECEIVED);
        Order updated = repository.save(order);

        // O DTO de resposta (OrderDTO) precisará ser atualizado para exibir esses novos campos.
        return new OrderDTO(updated);
    }

    // 🔹 Atualiza para um status específico (Drag & Drop)
    @Transactional
    public OrderDTO updateStatus(Long id, OrderStatus newStatus) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado: " + id));

        order.setStatus(newStatus);
        Order updated = repository.save(order);

        return new OrderDTO(updated);
    }

    // 🔹 Avançar automaticamente por etapas
    @Transactional
    public OrderDTO nextStep(Long id) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado"));

        switch (order.getStatus()) {
            case DRAFT -> order.setStatus(OrderStatus.RECEIVED); // Permite pular direto do DRAFT
            case RECEIVED -> order.setStatus(OrderStatus.IN_PREPARATION);
            case IN_PREPARATION -> order.setStatus(OrderStatus.READY);
            case READY -> order.setStatus(OrderStatus.DELIVERED);
            case DELIVERED -> throw new IllegalStateException("Pedido já foi entregue!");
        }

        Order updated = repository.save(order);
        return new OrderDTO(updated);
    }

    // 🔹 Retroceder automaticamente
    @Transactional
    public OrderDTO previousStep(Long id) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado"));

        switch (order.getStatus()) {
            case DELIVERED -> order.setStatus(OrderStatus.READY);
            case READY -> order.setStatus(OrderStatus.IN_PREPARATION);
            case IN_PREPARATION -> order.setStatus(OrderStatus.RECEIVED);
            case RECEIVED -> order.setStatus(OrderStatus.DRAFT); // Permite voltar para o DRAFT
            case DRAFT -> throw new IllegalStateException("Pedido já está no início!");
        }

        Order updated = repository.save(order);
        return new OrderDTO(updated);
    }
}