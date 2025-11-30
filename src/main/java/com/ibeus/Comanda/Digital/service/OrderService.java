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

    // --- MÉTODOS DE BUSCA GERAL (Mantidos) ---
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

    /**
     * Busca todos os pedidos que atingiram o status final DELIVERED (Histórico).
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> findHistory() {
        List<Order> entities = repository.findByStatus(OrderStatus.DELIVERED);
        
        if (entities.isEmpty()) {
            // A exceção deve ser lançada apenas se realmente for um erro (ex: filtro de status não encontrar nada)
            // Mas para um endpoint de histórico, retornar uma lista vazia (ResponseEntity.ok([])) é frequentemente mais amigável.
            // Para manter a consistência do seu código anterior, mantenho a exceção:
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum pedido finalizado encontrado.");
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
        order.setStatus(OrderStatus.DRAFT);

        Order saved = repository.save(order);
        return new OrderDTO(saved);
    }

    @Transactional
    public OrderItemDTO addItemToOrder(Long orderId, OrderItemInputDTO itemDTO) {
        // ... (lógica de adicionar item) ...
        Order order = repository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado: " + orderId));

        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new IllegalStateException("Só é possível adicionar itens a pedidos no status DRAFT.");
        }

        Dish dish = dishRepository.findById(itemDTO.getDishId())
                .orElseThrow(() -> new EntityNotFoundException("Prato não encontrado: " + itemDTO.getDishId()));

        OrderItem existingItem = order.getItems().stream()
                .filter(item -> item.getDish().getId().equals(dish.getId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + itemDTO.getQuantity();
            existingItem.setQuantity(newQuantity);
            repository.save(order);
            return new OrderItemDTO(existingItem);

        } else {
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

    @Transactional
    public OrderDTO removeItemFromOrder(Long orderId, OrderItemInputDTO itemDTO) {
        // ... (lógica de remover item) ...
        Order order = repository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado: " + orderId));

        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new IllegalStateException("Só é possível remover/diminuir itens em pedidos no status DRAFT.");
        }

        OrderItem existingItem = order.getItems().stream()
                .filter(item -> item.getDish().getId().equals(itemDTO.getDishId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Item não encontrado no pedido para o Prato ID: " + itemDTO.getDishId()));

        int quantityToRemove = itemDTO.getQuantity();

        if (quantityToRemove <= 0) {
            throw new IllegalArgumentException("A quantidade a ser removida deve ser positiva.");
        }

        int currentQuantity = existingItem.getQuantity();

        if (quantityToRemove >= currentQuantity) {
            order.getItems().remove(existingItem);
        } else {
            int newQuantity = currentQuantity - quantityToRemove;
            existingItem.setQuantity(newQuantity);
        }

        Order saved = repository.save(order);
        return new OrderDTO(saved);
    }

    @Transactional
    public OrderDTO finalizeOrder(Long orderId) {
        // ... (lógica de finalização) ...
        Order order = repository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado: " + orderId));

        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new IllegalStateException("Apenas pedidos no status DRAFT podem ser finalizados.");
        }

        Client client = order.getClient();

        if (client == null || client.getAddress() == null) {
            throw new IllegalStateException("Cliente ou Endereço principal não configurado no pedido.");
        }

        order.setClientSnapshotName(client.getName());

        Address address = client.getAddress();

        String fullAddress = String.format("%s, %s, %s - %s/%s. CEP: %s. Complemento: %s",
                address.getLogradouro(),
                client.getAddressNumber(),
                address.getBairro(),
                address.getLocalidade(),
                address.getUf(),
                address.getCep(),
                client.getComplement() != null ? client.getComplement() : ""
        );
        order.setAddressSnapshot(fullAddress);

        order.setStatus(OrderStatus.RECEIVED);
        Order updated = repository.save(order);

        return new OrderDTO(updated);
    }

    // 🔹 Atualiza para um status específico (Mantido para uso Admin/Geral)
    @Transactional
    public OrderDTO updateStatus(Long id, OrderStatus newStatus) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado: " + id));

        order.setStatus(newStatus);
        Order updated = repository.save(order);

        return new OrderDTO(updated);
    }
    
    // 🛑 Os métodos nextStep(Long id) e previousStep(Long id) foram removidos.
}