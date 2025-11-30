package com.ibeus.Comanda.Digital.controller;

import com.ibeus.Comanda.Digital.dto.OrderDTO;
import com.ibeus.Comanda.Digital.service.KitchenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/kitchen/orders") // 💡 Novo prefixo
@CrossOrigin(origins = "http://localhost:4200")
public class KitchenController {

    private final KitchenService service;

    public KitchenController(KitchenService service) {
        this.service = service;
    }

    // --- MÉTODOS DE VISUALIZAÇÃO ---
    
    // 1. Endpoint para buscar pedidos do painel Drag & Drop
    @GetMapping
    public ResponseEntity<List<OrderDTO>> findKitchenOrders() {
        List<OrderDTO> list = service.findKitchenOrders();
        return ResponseEntity.ok(list);
    }

    // --- MÉTODOS DE TRANSIÇÃO (DRAG & DROP) ---

    // 2. Avançar etapa
    @PostMapping("/{id}/next")
    public ResponseEntity<OrderDTO> nextStep(@PathVariable Long id) {
        return ResponseEntity.ok(service.nextStep(id));
    }

    // 3. Retroceder etapa (Com bloqueio a partir de ON_THE_WAY)
    @PostMapping("/{id}/previous")
    public ResponseEntity<OrderDTO> previousStep(@PathVariable Long id) {
        return ResponseEntity.ok(service.previousStep(id));
    }
}