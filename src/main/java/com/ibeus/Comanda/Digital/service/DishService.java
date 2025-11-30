package com.ibeus.Comanda.Digital.service;

import com.ibeus.Comanda.Digital.dto.DishDTO;
import com.ibeus.Comanda.Digital.model.Dish;
import com.ibeus.Comanda.Digital.repository.DishRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DishService {

    @Autowired
    private DishRepository dishRepository;

    @Autowired
    private StorageService storageService; // Injeta o serviço que sabe salvar arquivos

    // --- Buscas ---

    public List<Dish> findAll() {
        return dishRepository.findAll();
    }

    public Dish findById(Long id) {
        return dishRepository.findById(id)
                // Se não achar, lança erro 404 (Not Found) em vez de erro genérico
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prato não encontrado: " + id));
    }

    public List<Dish> findByFavorites() {
        return dishRepository.findByFavoriteTrue();
    }

    public List<Dish> findByName(String name) {
        return dishRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Dish> findByCategory(String category) {
        List<Dish> dishes = dishRepository.findByCategoryIgnoreCase(category);
        if (dishes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum prato nesta categoria: " + category);
        }
        return dishes;
    }

    @Transactional
    public Dish toggleFavorite(Long id) {
        Dish existingDish = findById(id);
        
        // Inverte o valor booleano atual
        existingDish.setFavorite(!existingDish.isFavorite()); 
        
        return dishRepository.save(existingDish);
    }

    // --- Criação Unificada (Lógica Principal) ---

    public Dish create(DishDTO dishDTO, MultipartFile file) {
        try {
            // 1. Verifica se o usuário enviou uma imagem
            if (file != null && !file.isEmpty()) {
                // Se enviou, chama o StorageService para salvar no disco
                String imageUrl = storageService.store(file);
                // Atualiza o DTO com o caminho da nova imagem
                dishDTO.setUrlImage(imageUrl);
            }
            // Se file for null, ele mantém a URL que talvez já tenha vindo no DTO (ou fica null)

            // 2. Converte DTO -> Entity e salva no banco
            return dishRepository.save(dishDTO.toModel());

        } catch (Exception e) {
            // Captura erros e devolve um 400 Bad Request
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Erro ao processar dados do prato", e);
        }
    }

    // --- Atualização ---

    // --- Atualização Unificada (Novo Método) 🔄 ---
    /**
     * Atualiza o prato e a imagem opcionalmente.
     * @param id ID do prato.
     * @param dishDTO Dados textuais do prato.
     * @param file Novo arquivo de imagem (opcional).
     * @return O prato atualizado.
     */
    @Transactional // Garante que tudo seja revertido se o upload falhar
    public Dish update(Long id, DishDTO dishDTO, MultipartFile file) {
        Dish existingDish = findById(id); // 1. Garante que o prato existe

        try {
            // 2. Atualiza campos textuais
            existingDish.setName(dishDTO.getName());
            existingDish.setPrice(dishDTO.getPrice());
            existingDish.setCategory(dishDTO.getCategory());
            existingDish.setDescription(dishDTO.getDescription());

            // 3. Lógica de Atualização/Substituição da Imagem
            if (file != null && !file.isEmpty()) {

                // Opcional: Adicione a lógica para DELETAR a imagem antiga do storage aqui
                // if (existingDish.getUrlImage() != null && !existingDish.getUrlImage().isEmpty()) {
                //     storageService.delete(existingDish.getUrlImage());
                // }

                // Faz o upload do NOVO arquivo e obtém a URL
                String newImageUrl = storageService.store(file);

                // Atualiza a URL no prato
                existingDish.setUrlImage(newImageUrl);

            } else if (dishDTO.getUrlImage() != null && dishDTO.getUrlImage().isEmpty()) {
                // Caso especial: O front-end enviou explicitamente a URL vazia para remover a imagem
                existingDish.setUrlImage(null);
                // Opcional: Lógica para deletar a imagem do storage (se existir)
            }
            // Se 'file' for nulo e 'dishDTO.getUrlImage()' for igual ao valor antigo, nada muda.

            // 4. Salva o prato com todas as alterações
            return dishRepository.save(existingDish);

        } catch (Exception e) {
            // Captura erros (ex: falha no upload) e devolve um 400 Bad Request
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Erro ao atualizar prato ou imagem", e);
        }
    }

    // --- Deleção ---

    public void delete(Long id) {
        if (!dishRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prato não encontrado para deletar");
        }
        dishRepository.deleteById(id);
    }
}