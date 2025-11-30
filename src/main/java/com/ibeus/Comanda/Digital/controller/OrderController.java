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

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    // --- MÉTODOS DE BUSCA GERAL (Mantidos) ---
    @GetMapping
    public ResponseEntity<List<OrderDTO>> findAll() {
        List<OrderDTO> list = service.findAll();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> findById(@PathVariable Long id) {
        OrderDTO dto = service.findById(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderDTO>> findByStatus(@PathVariable OrderStatus status) {
        List<OrderDTO> list = service.findByStatus(status);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/history")
    public ResponseEntity<List<OrderDTO>> getOrderHistory() {
        List<OrderDTO> list = service.findHistory();
        return ResponseEntity.ok(list);
    }

    // --- MÉTODOS DE ESCRITA (FLUXO DO CARRINHO - Mantidos) ---
    @PostMapping
    public ResponseEntity<OrderDTO> create(@RequestBody OrderDTO dto) {
        OrderDTO created = service.create(dto);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/{orderId}/items")
    public ResponseEntity<OrderItemDTO> addItem(
            @PathVariable Long orderId,
            @RequestBody OrderItemInputDTO itemDTO) {

        OrderItemDTO addedItem = service.addItemToOrder(orderId, itemDTO);
        return ResponseEntity.ok(addedItem);
    }

    @PatchMapping("/{orderId}/items/remove")
    public ResponseEntity<OrderDTO> removeItem(
            @PathVariable Long orderId,
            @RequestBody OrderItemInputDTO itemDTO) {

        OrderDTO updatedOrder = service.removeItemFromOrder(orderId, itemDTO);
        return ResponseEntity.ok(updatedOrder);
    }

    @PostMapping("/{orderId}/finalize")
    public ResponseEntity<OrderDTO> finalizeOrder(@PathVariable Long orderId) {
        OrderDTO finalizedOrder = service.finalizeOrder(orderId);
        return ResponseEntity.ok(finalizedOrder);
    }

    // --- MÉTODOS DE ADMINISTRAÇÃO / GERAIS (Mantidos) ---
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {

        OrderDTO updatedOrder = service.updateStatus(id, status);
        return ResponseEntity.ok(updatedOrder);
    }
    
    // 🛑 Os endpoints /next e /previous foram removidos.
}