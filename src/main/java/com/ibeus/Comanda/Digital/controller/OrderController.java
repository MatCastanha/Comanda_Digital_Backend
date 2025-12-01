package com.ibeus.Comanda.Digital.controller;

import com.ibeus.Comanda.Digital.dto.OrderDTO;
import com.ibeus.Comanda.Digital.dto.OrderItemDTO;
import com.ibeus.Comanda.Digital.dto.OrderItemInputDTO;
import com.ibeus.Comanda.Digital.enums.OrderStatus;
import com.ibeus.Comanda.Digital.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // --- MÉTODOS GET ---
    @GetMapping
    public ResponseEntity<List<OrderDTO>> findAll() {
        List<OrderDTO> list = orderService.findAll();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> findById(@PathVariable Long id) {
        OrderDTO dto = orderService.findById(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderDTO>> findByStatus(@PathVariable OrderStatus status) {
        List<OrderDTO> list = orderService.findByStatus(status);
        return ResponseEntity.ok(list);
    }

    // Busca o histórico de pedidos finalizados
    @GetMapping("/history")
    public ResponseEntity<List<OrderDTO>> getOrderHistory() {
        List<OrderDTO> list = orderService.findHistory();
        return ResponseEntity.ok(list);
    }

    // --- MÉTODOS DE ESCRITA (FLUXO DO CARRINHO) ---

    // 1. Cria o rascunho (DRAFT) vinculado ao Cliente Único
    @PostMapping
    public ResponseEntity<OrderDTO> create(@RequestBody OrderDTO dto) {
        OrderDTO created = orderService.create(dto);
        return ResponseEntity.ok(created);
    }

    // 2. Adiciona/Atualiza Item ao Rascunho (Carrinho)
    @PostMapping("/{orderId}/items")
    public ResponseEntity<OrderItemDTO> addItem(
            @PathVariable Long orderId,
            @RequestBody OrderItemInputDTO itemDTO) {

        OrderItemDTO addedItem = orderService.addItemToOrder(orderId, itemDTO);
        return ResponseEntity.ok(addedItem);
    }

    // --- 🔹 REMOVER OU DIMINUIR ITEM (PATCH) 🔄 ---
    // PATCH é mais adequado, pois pode ser uma diminuição (atualização parcial) ou remoção.
    @PatchMapping("/{orderId}/items/remove")
    public ResponseEntity<OrderDTO> removeItem(
            @PathVariable Long orderId,
            @RequestBody OrderItemInputDTO itemDTO) {

        OrderDTO updatedOrder = orderService.removeItemFromOrder(orderId, itemDTO);
        return ResponseEntity.ok(updatedOrder);
    }

    // 3. Finaliza o Pedido (Muda de DRAFT para RECEIVED)
    @PostMapping("/{orderId}/finalize")
    public ResponseEntity<OrderDTO> finalizeOrder(@PathVariable Long orderId) {
        OrderDTO finalizedOrder = orderService.finalizeOrder(orderId);
        return ResponseEntity.ok(finalizedOrder);
    }

    // --- MÉTODOS DE MUDANÇA DE STATUS ---

    // Atualizar para um status específico (Ex: Drag & Drop)
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {

        OrderDTO updatedOrder = orderService.updateStatus(id, status);
        return ResponseEntity.ok(updatedOrder);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderDTO> cancelOrder(@PathVariable Long id) {
        OrderDTO canceledOrder = orderService.cancelOrder(id);
        return ResponseEntity.ok(canceledOrder);
    }

    // Avançar etapa
    @PostMapping("/{id}/next")
    public ResponseEntity<OrderDTO> nextStep(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.nextStep(id));
    }

    // Voltar etapa
    @PostMapping("/{id}/previous")
    public ResponseEntity<OrderDTO> previousStep(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.previousStep(id));
    }
}