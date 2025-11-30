package com.ibeus.Comanda.Digital.service;

import com.ibeus.Comanda.Digital.dto.OrderDTO;
import com.ibeus.Comanda.Digital.enums.OrderStatus;
import com.ibeus.Comanda.Digital.model.Order;
import com.ibeus.Comanda.Digital.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KitchenService {

    private final OrderRepository repository;

    public KitchenService(OrderRepository repository) {
        this.repository = repository;
    }

    /**
     * Busca todos os pedidos no fluxo de trabalho da cozinha (RECEIVED até ON_THE_WAY).
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> findKitchenOrders() {
        // Status visíveis no painel
        List<OrderStatus> kitchenStatuses = Arrays.asList(
            OrderStatus.RECEIVED,
            OrderStatus.IN_PREPARATION,
            OrderStatus.READY,
            OrderStatus.ON_THE_WAY
        );
        
        List<Order> entities = repository.findByStatusIn(kitchenStatuses);
        
        return entities.stream().map(OrderDTO::new).collect(Collectors.toList());
    }

    /**
     * Avança o pedido uma etapa no fluxo da cozinha (Next Step).
     */
    @Transactional
    public OrderDTO nextStep(Long id) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado"));

        switch (order.getStatus()) {
            case RECEIVED -> order.setStatus(OrderStatus.IN_PREPARATION);
            case IN_PREPARATION -> order.setStatus(OrderStatus.READY);
            case READY -> order.setStatus(OrderStatus.ON_THE_WAY);
            
            case ON_THE_WAY, DELIVERED -> throw new IllegalStateException("O pedido já está fora da área de preparo ou entregue.");
            case DRAFT -> throw new IllegalStateException("O pedido ainda não foi finalizado (status DRAFT).");
        }

        Order updated = repository.save(order);
        return new OrderDTO(updated);
    }

    /**
     * Retrocede o pedido uma etapa no fluxo da cozinha (Previous Step).
     * 🛑 REGRA DE NEGÓCIO: Bloqueia retrocesso a partir de ON_THE_WAY.
     */
    @Transactional
    public OrderDTO previousStep(Long id) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado"));

        switch (order.getStatus()) {
            // 🛑 Bloqueio de retrocesso
            case ON_THE_WAY, DELIVERED -> { 
                throw new IllegalStateException("O pedido não pode ser retrocedido após sair para entrega ou ser entregue.");
            }
            case READY -> order.setStatus(OrderStatus.IN_PREPARATION);
            case IN_PREPARATION -> order.setStatus(OrderStatus.RECEIVED);
            
            // Permite retroceder para rascunho (DRAFT)
            case RECEIVED -> order.setStatus(OrderStatus.DRAFT); 
            case DRAFT -> throw new IllegalStateException("Pedido já está no início (DRAFT).");
        }

        Order updated = repository.save(order);
        return new OrderDTO(updated);
    }
}