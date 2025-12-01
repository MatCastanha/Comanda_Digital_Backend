package com.ibeus.Comanda.Digital.dto;

import com.ibeus.Comanda.Digital.model.Dish;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Gera Getters e Setters
@NoArgsConstructor // ⚠️ IMPORTANTE: Cria um construtor vazio. Essencial para o Spring converter JSON/Form-data neste objeto sem dar erro.
@AllArgsConstructor // Cria um construtor com todos os argumentos (útil para testes)
public class DishDTO {

    private Long id;
    private String urlImage;
    private String name;
    private String category;
    private String description;
    private Double price;

    public static DishDTO fromModel(Dish dish) {
        if (dish == null) return null; // Segurança contra NullPointerException
        DishDTO dto = new DishDTO();
        dto.setId(dish.getId());
        dto.setUrlImage(dish.getUrlImage());
        dto.setName(dish.getName());
        dto.setCategory(dish.getCategory());
        dto.setDescription(dish.getDescription());
        dto.setPrice(dish.getPrice());
        return dto;
    }

    public Dish toModel() {
        Dish dish = new Dish();
        dish.setId(this.id);
        dish.setUrlImage(this.urlImage);
        dish.setName(this.name);
        dish.setCategory(this.category);
        dish.setDescription(this.description);
        dish.setPrice(this.price);
        return dish;
    }
}