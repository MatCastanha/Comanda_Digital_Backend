package com.ibeus.Comanda.Digital.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibeus.Comanda.Digital.dto.DishDTO;
import com.ibeus.Comanda.Digital.model.Dish;
import com.ibeus.Comanda.Digital.service.DishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController // Indica que esta classe responde a requisições REST (JSON)
@RequestMapping("/dishes") // Prefixo da URL: localhost:8080/dishes
@CrossOrigin(origins = "http://localhost:4200") // Permite acesso do Angular
public class DishController {

    @Autowired
    private DishService dishService;

    // --- MÉTODOS DE LEITURA (GET) ---

    @GetMapping
    public ResponseEntity<List<DishDTO>> getAllDishes() {
        // Busca Entities, converte para DTOs e retorna lista
        List<DishDTO> list = dishService.findAll().stream()
                .map(DishDTO::fromModel)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DishDTO> getDishById(@PathVariable Long id) {
        return ResponseEntity.ok(DishDTO.fromModel(dishService.findById(id)));
    }

    @GetMapping("/search")
    public ResponseEntity<List<DishDTO>> getDishByName(@RequestParam("name") String name) {
        List<DishDTO> list = dishService.findByName(name).stream()
                .map(DishDTO::fromModel)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<DishDTO>> getDishByCategory(@PathVariable String category) {
        List<DishDTO> list = dishService.findByCategory(category).stream()
                .map(DishDTO::fromModel)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    // --- MÉTODO DE CRIAÇÃO UNIFICADO (POST) ---

    // consumes = "multipart/form-data" permite envio de arquivos + texto
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<DishDTO> createDish(
            // 1. Revertido: Recebe os campos do formulário (name, price, etc.) e tenta mapear diretamente
            @ModelAttribute DishDTO dishDTO,

            // 2. Recebe o arquivo
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        // A Lógica do Service continua a mesma (chama createUnified)
        Dish savedDish = dishService.create(dishDTO, file);
        return ResponseEntity.ok(DishDTO.fromModel(savedDish));
    }

    // --- MÉTODOS DE ATUALIZAÇÃO E DELEÇÃO ---

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<DishDTO> updateDish(
            @PathVariable Long id,
            @ModelAttribute DishDTO dishDTO,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        Dish updatedDish = dishService.update(id, dishDTO, file);
        return ResponseEntity.ok(DishDTO.fromModel(updatedDish));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDish(@PathVariable Long id) {
        dishService.delete(id);
        // Retorna 204 No Content (sucesso sem corpo)
        return ResponseEntity.noContent().build();
    }
}